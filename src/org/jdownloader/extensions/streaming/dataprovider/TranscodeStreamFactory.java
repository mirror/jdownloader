package org.jdownloader.extensions.streaming.dataprovider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.appwork.utils.Application;
import org.appwork.utils.io.streamingio.StreamingChunk;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.streaming.dataprovider.transcode.Transcoder;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.upnp.remotedevices.RendererInfo;

public class TranscodeStreamFactory extends AbstractStreamFactory {

    private StreamFactoryInterface streamfactory;
    private MediaItem              mediaItem;
    private RendererInfo           callingDevice;
    private Profile                profile;
    private int                    port;

    public enum TranscoderState {
        IDLE,
        RUNNING,
        SUCCESS,
        FAILED
    }

    private TranscoderState transcoderState = TranscoderState.IDLE;
    private StreamingChunk  streamingChunk;

    public TranscodeStreamFactory(StreamFactoryInterface streamfactory, MediaItem mediaItem, RendererInfo callingDevice, Profile dlnaProfile) {
        this.streamfactory = streamfactory;
        this.mediaItem = mediaItem;
        this.callingDevice = callingDevice;
        this.profile = dlnaProfile;
        contentLength = callingDevice.getHandler().estimateTranscodedContentLength(mediaItem, dlnaProfile);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public InputStream createInputStream(long start, long end) throws IOException {
        synchronized (this) {
            switch (transcoderState) {
            case IDLE:
                start();
                break;
            case FAILED:
                return null;
            case RUNNING:
            case SUCCESS:

            }

        }
        return new PrintSpeedInputStream(new ListenerInputstreamWrapper(streamingChunk.getInputStream(start, end), 5000, null), "Transcoder");
    }

    private Transcoder transcoder;
    protected long     contentLength = -1;
    private boolean    closed        = false;

    private void start() throws FileNotFoundException {
        transcoderState = TranscoderState.RUNNING;
        String id = new UniqueAlltimeID().toString();
        File tmp = Application.getResource("/tmp/streaming/transcode_" + id);
        tmp.deleteOnExit();
        FileCreationManager.getInstance().mkdir(tmp.getParentFile());

        streamingChunk = new StreamingChunk(tmp, 0);
        streamingChunk.setCanGrow(true);
        Thread thread = new Thread("Transcoder Thread") {

            public void run() {
                try {

                    transcoder = new Transcoder(port) {

                        @Override
                        protected void write(byte[] buffer, int length) throws IOException {
                            streamingChunk.write(buffer, 0, length);
                        }

                        @Override
                        protected String[] getFFMpegCommandLine(String string) {
                            return callingDevice.getHandler().getFFMpegTranscodeCommandline(mediaItem, profile, string);

                        }

                        @Override
                        protected StreamFactoryInterface getStreamFactory() {
                            return streamfactory;
                        }

                        @Override
                        protected void addException(Exception e) {
                            TranscodeStreamFactory.this.addException(e);
                        }

                    };

                    transcoder.run();
                    contentLength = streamingChunk.getAvailableChunkSize();
                } catch (Exception e) {
                    addException(e);

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
    public long getGuaranteedContentLength() {
        return (streamingChunk == null || streamingChunk.getAvailableChunkSize() == 0) ? 1 : streamingChunk.getAvailableChunkSize();
    }

    public void setTransportPort(int port) {
        this.port = port;
    }

    @Override
    public void close() {
        closed = true;
        transcoderState = TranscoderState.IDLE;
        if (transcoder != null) transcoder.close();
        streamingChunk.close();

    }

}
