package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;
import java.io.InputStream;

import org.appwork.utils.io.streamingio.Streaming;

public class DownloadStreamFactory extends AbstractStreamFactory {
    private Streaming streamProvider;

    public DownloadStreamFactory(Streaming streamProvider) {
        this.streamProvider = streamProvider;

    }

    public boolean isClosed() {
        return streamProvider.isClosed();
    }

    @Override
    public long getContentLength() {
        return streamProvider.getFinalFileSize();
    }

    @Override
    public long getGuaranteedContentLength() {

        return getContentLength();

    }

    @Override
    public void close() {

        streamProvider.close();

    }

    @Override
    protected InputStream createInputStream(long start, long end) throws IOException {
        return streamProvider.getInputStream(start, end);
    }

}
