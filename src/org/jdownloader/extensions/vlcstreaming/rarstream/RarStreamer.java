package org.jdownloader.extensions.vlcstreaming.rarstream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import jd.Launcher;
import jd.nutils.Executer;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.utils.Application;
import org.appwork.utils.io.streamingio.StreamingChunk;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionException;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.vlcstreaming.StreamingInterface;
import org.jdownloader.extensions.vlcstreaming.VLCStreamingExtension;
import org.jdownloader.extensions.vlcstreaming.VLCStreamingProvider;
import org.jdownloader.logging.LogController;

public class RarStreamer implements Runnable, StreamingInterface {

    private Archive               archive;
    private ISevenZipInArchive    rarArchive;
    private LogSource             logger;
    private VLCStreamingProvider  streamProvider;
    private VLCStreamingExtension extension;
    private ISimpleInArchiveItem  bestItem = null;
    private RarStreamProvider     rarStreamProvider;
    private StreamingChunk        streamingChunk;
    private Thread                extractionThread;

    public RarStreamer(Archive archive, VLCStreamingExtension extension) {
        this.archive = archive;
        this.extension = extension;
        this.streamProvider = extension.getStreamProvider();
        logger = LogController.CL();
    }

    public void start() {
        new Thread(this, "RarStreamer").start();
    }

    @Override
    public void run() {
        try {
            open();
        } catch (ExtractionException e) {
            logger.log(e);
        }
    }

    private void updateContentView(ISimpleInArchive simpleInterface) {
        try {
            ContentView newView = new ContentView();
            for (ISimpleInArchiveItem item : simpleInterface.getArchiveItems()) {
                try {
                    if (item.getPath().trim().equals("")) continue;
                    newView.add(new PackedFile(item.isFolder(), item.getPath(), item.getSize()));
                } catch (SevenZipException e) {
                    logger.log(e);
                }
            }
            archive.setContentView(newView);
        } catch (SevenZipException e) {
            logger.log(e);
        }
    }

    private void open() throws ExtractionException {
        String ID = "extract" + archive.getFactory().getID();
        StreamingInterface streamingInterface = extension.getVlcstreamingAPI().getStreamingInterface(ID);
        if (streamingInterface != null) return;
        rarStreamProvider = new RarStreamProvider(archive, "serienjunkies.org", this.streamProvider);
        try {
            IInStream rarStream = rarStreamProvider.getStream(archive.getFirstArchiveFile());
            rarArchive = SevenZip.openInArchive(ArchiveFormat.RAR, rarStream, rarStreamProvider);
            for (ISimpleInArchiveItem item : rarArchive.getSimpleInterface().getArchiveItems()) {
                if (bestItem == null && !item.isFolder()) {
                    bestItem = item;
                } else if (!item.isFolder() && bestItem != null && bestItem.getSize() < item.getSize()) {
                    bestItem = item;
                }
                if (item.isEncrypted()) {
                    archive.setProtected(true);
                }
            }
            updateContentView(rarArchive.getSimpleInterface());
            System.out.println("best item: " + bestItem.getPath() + " size: " + bestItem.getSize());
            extension.getVlcstreamingAPI().addHandler(ID, this);
            extract();
            Executer exec = new Executer(extension.getVLCBinary());
            exec.setLogger(LogController.CL());
            exec.addParameters(new String[] { "http://127.0.0.1:" + RemoteAPIController.getInstance().getApiPort() + "/vlcstreaming/play?id=" + ID });
            exec.setRunin(Application.getRoot(Launcher.class));
            exec.setWaitTimeout(0);
            exec.start();
        } catch (SevenZipException e) {
            logger.log(e);
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("No password was provided")) {

                // file password protected: net.sf.sevenzipjbinding.SevenZipException: HRESULT: 0x80004005 (Fail). Archive file (format:
                // Rar)
                // can't be opened
                // There are password protected multipart rar files
                archive.setProtected(true);

            } else {
                throw new ExtractionException(e, streamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null);
            }
        } catch (Throwable e) {
            logger.log(e);
            throw new ExtractionException(e, streamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null);
        }
    }

    public void extract() throws IOException, SevenZipException {
        File tmp = Application.getResource("/tmp/streaming/extract" + System.currentTimeMillis());
        tmp.getParentFile().mkdirs();
        streamingChunk = new StreamingChunk(tmp, 0);
        extractionThread = new Thread() {
            public void run() {
                try {
                    streamingChunk.setCanGrow(true);
                    rarArchive.extractSlow(bestItem.getItemIndex(), new ISequentialOutStream() {

                        @Override
                        public int write(byte[] abyte0) throws SevenZipException {
                            if (abyte0.length == 0) return 0;
                            try {
                                streamingChunk.write(abyte0, 0, abyte0.length);
                                return abyte0.length;
                            } catch (IOException e) {
                                throw new SevenZipException(e);
                            }
                        }
                    });
                } catch (final Throwable e) {
                    e.printStackTrace();
                } finally {
                    streamingChunk.setCanGrow(false);
                }
            };
        };
        extractionThread.start();

    }

    public void close() {
        try {
            rarArchive.close();
        } catch (final Throwable e) {
        }
        try {
            rarStreamProvider.close();
        } catch (final Throwable e) {
        }
    }

    @Override
    public boolean isRangeRequestSupported() {
        return true;
    }

    @Override
    public long getFinalFileSize() {
        try {
            return bestItem.getSize();
        } catch (final Throwable e) {
            return -1;
        }
    }

    @Override
    public InputStream getInputStream(final long startPosition, final long stopPosition) throws IOException {
        return new InputStream() {
            long   currentPosition = startPosition;
            byte[] bufferByte      = new byte[1];

            @Override
            public int read() throws IOException {
                if (currentPosition != 0 && currentPosition > streamingChunk.getAvailableChunkSize()) return -1;
                int ret = read(bufferByte, 0, 1);
                if (ret == 1) return bufferByte[1];
                return -1;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (currentPosition != 0 && currentPosition > streamingChunk.getAvailableChunkSize()) return -1;
                try {
                    int ret;
                    ret = streamingChunk.read(b, off, len, currentPosition);
                    if (ret >= 0) currentPosition += ret;
                    return ret;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return -1;
                }

            }

            @Override
            public boolean markSupported() {
                return false;
            }
        };
    }

}
