package org.jdownloader.extensions.streaming.dataprovider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.io.streamingio.StreamingChunk;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.streaming.dataprovider.transcode.Transcoder;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.upnp.remotedevices.RendererInfo;
import org.jdownloader.extensions.streaming.upnp.remotedevices.handlers.AbstractDeviceHandler;
import org.jdownloader.logging.LogController;

public class TranscodeDataProvider implements DataProvider {

    private DataProvider          dataProvider;
    private MediaItem             mediaItem;
    private AbstractDeviceHandler handler;
    private boolean               transcode;
    private Profile               profile;
    private TranscoderState       transcoderState = TranscoderState.IDLE;
    private StreamingChunk        streamingChunk;

    private int                   port;
    private LogSource             logger;

    public TranscodeDataProvider(int port, MediaItem mediaItem, RendererInfo callingDevice, boolean isTranscodeRequired, Profile dlnaProfile, DataProvider dp) {
        super();
        this.dataProvider = dp;
        logger = LogController.getInstance().getLogger(TranscodeDataProvider.class.getName());
        this.mediaItem = mediaItem;

        transcode = isTranscodeRequired;
        profile = dlnaProfile;
        this.handler = callingDevice.getHandler();
        this.port = port;
    }

    @Override
    public boolean canHandle(Object link, DataProvider... dataProviders) {
        if (!transcode) {
            //
            return dataProvider.canHandle(link, dataProviders);
        }
        return false;
    }

    @Override
    public boolean isRangeRequestSupported(Object link) {
        if (!transcode) {
            //
            return dataProvider.isRangeRequestSupported(link);
        }
        return true;
    }

    @Override
    public long getFinalFileSize(Object link) {
        if (!transcode) {
            //
            return dataProvider.getFinalFileSize(link);
        }
        return -1;
    }

    public enum TranscoderState {
        IDLE,
        RUNNING,
        SUCCESS,
        FAILED
    }

    @Override
    public InputStream getInputStream(Object link, final long startPosition, long stopPosition) throws IOException {
        if (!transcode) {
            //
            return dataProvider.getInputStream(link, startPosition, stopPosition);
        }
        switch (transcoderState) {
        case IDLE:
            start();
            break;
        case FAILED:
            return null;
        case RUNNING:
        case SUCCESS:

        }
        logger.info("New Stream: " + startPosition + " -> " + stopPosition);
        return new TranscodedInputStream() {
            long   currentPosition = startPosition;
            byte[] bufferByte      = new byte[1];

            public String toString() {
                return "TranscodedInputStream<<" + transcoder;
            }

            @Override
            public int read() throws IOException {
                if (exception != null) {
                    //
                    throw new IOException(exception);
                }
                if (currentPosition != 0 && currentPosition > streamingChunk.getAvailableChunkSize()) return -1;
                int ret = read(bufferByte, 0, 1);
                if (ret == 1) return bufferByte[1];
                return -1;
            }

            @Override
            public void close() throws IOException {
                super.close();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (exception != null) {
                    //
                    throw new IOException(exception);
                }
                logger.info(" Read: " + len + " bytes at " + currentPosition);

                if (currentPosition != 0 && currentPosition > streamingChunk.getAvailableChunkSize()) {
                    System.out.println("-1 answer");
                    return -1;
                }
                try {
                    int ret;

                    ret = streamingChunk.read(b, off, len, currentPosition);
                    if (ret >= 0) currentPosition += ret;
                    if (exception != null) {
                        //
                        throw new IOException(exception);
                    }
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

    protected Exception exception;

    public String toString() {
        return "TransCodeDataProvide<<" + dataProvider;
    }

    protected void setException(Exception e) {
        if (this.exception == null) {
            // logger.log(e);
            exception = e;
        }
    }

    private Transcoder transcoder;

    private void start() throws FileNotFoundException {
        transcoderState = TranscoderState.RUNNING;
        String id = new UniqueAlltimeID().toString();
        File tmp = Application.getResource("/tmp/streaming/transcode_" + id);
        tmp.deleteOnExit();
        tmp.getParentFile().mkdirs();

        streamingChunk = new StreamingChunk(tmp, 0);
        streamingChunk.setCanGrow(true);
        Thread thread = new Thread("Transcoder Thread") {

            public void run() {
                try {

                    transcoder = new Transcoder(port) {

                        @Override
                        protected DataProvider getDataProvider() {
                            return dataProvider;
                        }

                        @Override
                        protected DownloadLink getDownloadLink() {
                            return mediaItem.getDownloadLink();
                        }

                        @Override
                        protected void write(byte[] buffer, int length) throws IOException {
                            streamingChunk.write(buffer, 0, length);
                        }

                        @Override
                        protected String[] getFFMpegCommandLine(String string) {
                            return handler.getFFMpegTranscodeCommandline(mediaItem, profile, string);

                        }

                    };

                    try {
                        transcoder.run();
                    } catch (Exception e) {
                        setException(e);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    streamingChunk.setCanGrow(false);
                    transcoderState = TranscoderState.SUCCESS;
                    try {
                        transcoder.close();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }

                }
            };
        };
        thread.start();

    }

    @Override
    public void close() throws IOException {
        transcoderState = TranscoderState.IDLE;
        if (transcoder != null) transcoder.close();
        exception = null;
        streamingChunk.close();
        dataProvider.close();
    }

}
