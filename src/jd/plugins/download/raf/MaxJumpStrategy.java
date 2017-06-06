package jd.plugins.download.raf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jd.http.URLConnectionAdapter;
import jd.plugins.download.raf.HTTPChunk.ERROR;
import jd.plugins.download.raf.HTTPDownloader.STATEFLAG;

import org.appwork.exceptions.WTFException;

public class MaxJumpStrategy implements ChunkStrategy {

    private final HTTPDownloader           downloadInterface;
    private final long                     maxJumpLength;
    private final long                     minChunkSplitSize;
    private final ArrayList<HTTPChunk>     chunkHistory            = new ArrayList<HTTPChunk>();
    private boolean                        initialConnectionUsed   = false;

    protected final Comparator<ChunkRange> unMarkedAreaOrderSorter = new Comparator<ChunkRange>() {

        private int compare(long x, long y) {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }

        @Override
        public int compare(ChunkRange o1, ChunkRange o2) {
            return compare(o1.getFrom(), o2.getFrom());
        }
    };

    protected final Comparator<ChunkRange> unMarkedAreaSizeSorter  = new Comparator<ChunkRange>() {

        private int compare(long x, long y) {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }

        @Override
        public int compare(ChunkRange o1, ChunkRange o2) {
            return -compare(o1.getLength(), o2.getLength());
        }
    };

    public MaxJumpStrategy(HTTPDownloader downloadInterface, long maxJumpLength, long minChunkSplitSize) {
        this.downloadInterface = downloadInterface;
        this.maxJumpLength = maxJumpLength;
        this.minChunkSplitSize = minChunkSplitSize;
    }

    protected URLConnectionAdapter getInitialConnection(ChunkRange chunkRange) {
        if (initialConnectionUsed) {
            return null;
        }
        URLConnectionAdapter initialConnection = downloadInterface.getRequest().getHttpConnection();
        long[] initialRange = initialConnection.getRange();
        if (initialRange != null) {
            if (chunkRange.getFrom() == initialRange[0]) {
                if (chunkRange.getLength() >= 0 && chunkRange.getTo() == initialRange[1] || initialRange[2] == downloadInterface.getVerifiedFileSize()) {
                    initialConnectionUsed = true;
                    return initialConnection;
                }
            }
        } else {
            if (chunkRange.getFrom() == 0 && chunkRange.getLength() < 0 || chunkRange.getLength() >= 0 && chunkRange.getLength() == downloadInterface.getVerifiedFileSize()) {
                downloadInterface.getLogger().info("Reuse initialConnection for Chunk: " + chunkRange);
                initialConnectionUsed = true;
                return initialConnection;
            }
        }
        return null;
    }

    protected int processFinishedChunks(List<HTTPChunk> finishedChunks) {
        int maxChunks = -1;
        for (HTTPChunk chunk : finishedChunks) {
            ERROR error = chunk.getError();
            downloadInterface.addError(error);
            /* TODO: better handling */
            switch (error) {
            case RANGE:
            case INVALID_CONTENT:
            case INVALID_RESPONSE:
            case REDIRECT:
                maxChunks = downloadInterface.setMaxChunksNum(downloadInterface.getActiveChunks());
                break;
            case CONNECTING:
            case DOWNLOADING:
                /* TODO: auto reset/increase */
                maxChunks = downloadInterface.setMaxChunksNum(downloadInterface.getActiveChunks());
            break;
            case ABORT:
            case FLUSHING:
            case NONE:
                break;
            }
        }
        return maxChunks;
    }

    @Override
    public List<HTTPChunk> getNextChunks(List<HTTPChunk> finishedChunks) {
        final int maxChunks = processFinishedChunks(finishedChunks);
        chunkHistory.addAll(finishedChunks);
        for (final HTTPChunk finishedChunk : finishedChunks) {
            switch (finishedChunk.getError()) {
            case NONE:
                final ChunkRange chunkRange = finishedChunk.getChunkRange();
                if (chunkRange.isValidLoaded() && chunkRange.getTo() == null && chunkRange.getFrom() == 0 && chunkRange.getLoaded() >= 0 && downloadInterface.getCacheMap().getFinalSize() < 0) {
                    /* eg downloading content without known finalSize */
                    downloadInterface.getLogger().info("Use contentLength from Chunk as finalFileSize: " + chunkRange);
                    downloadInterface.getCacheMap().setFinalSize(chunkRange.getLoaded());
                }
                break;
            default:
                System.out.println(finishedChunk);
                break;
            }
        }
        if (downloadInterface.getStateFlag().get() != STATEFLAG.RUN) {
            return null;
        }
        if (downloadInterface.getActiveChunks() >= downloadInterface.getChunkNum()) {
            return null;
        }
        if (maxChunks == 0 && downloadInterface.getActiveChunks() == 0) {
            return null;
        }
        final List<HTTPChunk> ret = new ArrayList<HTTPChunk>();
        final List<ChunkRange> unMarkedAreas = getUnMarkedAreas();
        if (unMarkedAreas.size() > 0) {
            if (downloadInterface.tryRangeRequest()) {
                if (downloadInterface.getCacheMap().getFinalSize() >= 0) {
                    long minChunkSize = 10 * 1024 * 1024l;
                    long chunkJumpPosition = 0;
                    Collections.sort(unMarkedAreas, unMarkedAreaSizeSorter);
                    while (ret.size() == 0) {
                        boolean skipChunk = false;
                        for (ChunkRange unMarkedArea : unMarkedAreas) {
                            if (unMarkedArea.getFrom() > chunkJumpPosition) {
                                continue;
                            }
                            if (unMarkedArea.getLength() < 0) {
                                throw new WTFException("FIXME: unknown length");
                            }
                            long chunkSize = Math.min(chunkJumpPosition + maxJumpLength, unMarkedArea.getLength());
                            ChunkRange nextChunk = null;
                            if (downloadInterface.getChunksInArea(unMarkedArea) > 0) {
                                long newChunkSize = chunkSize / 2;
                                if (newChunkSize <= minChunkSize) {
                                    skipChunk = true;
                                    continue;
                                }
                                nextChunk = new ChunkRange(unMarkedArea.getFrom() + Math.min(maxJumpLength, newChunkSize), unMarkedArea.getTo());
                            } else {
                                nextChunk = unMarkedArea;
                            }
                            if (nextChunk != null) {
                                final HTTPChunk chunk = new HTTPChunk(nextChunk, getInitialConnection(nextChunk), downloadInterface, downloadInterface.getDownloadable());
                                ret.add(chunk);
                                break;
                            }
                        }
                        chunkJumpPosition += maxJumpLength;
                        if (skipChunk) {
                            if (chunkJumpPosition >= downloadInterface.getVerifiedFileSize()) {
                                minChunkSize = Math.max(minChunkSplitSize, minChunkSize / 2);
                                chunkJumpPosition = 0;
                                if (minChunkSize == minChunkSplitSize) {
                                    break;
                                }
                            }
                        }
                    }
                } else if (downloadInterface.getActiveChunks() == 0) {
                    ChunkRange nextChunk = new ChunkRange(unMarkedAreas.get(0).getFrom(), null);
                    final HTTPChunk chunk = new HTTPChunk(nextChunk, getInitialConnection(nextChunk), downloadInterface, downloadInterface.getDownloadable());
                    ret.add(chunk);
                }
            } else if (downloadInterface.getActiveChunks() == 0) {
                ChunkRange nextChunk = new ChunkRange(0, null);
                final HTTPChunk chunk = new HTTPChunk(nextChunk, getInitialConnection(nextChunk), downloadInterface, downloadInterface.getDownloadable());
                ret.add(chunk);
            }
        }
        return ret;
    }

    @Override
    public List<ChunkRange> getUnMarkedAreas() {
        List<ChunkRange> ret = new ArrayList<ChunkRange>();
        for (Long[] unMarkedArea : downloadInterface.getCacheMap().getUnMarkedAreas()) {
            ret.add(new ChunkRange(unMarkedArea[0], unMarkedArea[1]));
        }
        return ret;
    }
}
