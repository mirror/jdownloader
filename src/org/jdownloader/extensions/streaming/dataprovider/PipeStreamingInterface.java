package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;
import java.io.InputStream;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.streaming.StreamingInterface;

public class PipeStreamingInterface implements StreamingInterface {

    private DataProvider dataProvider;
    private DownloadLink link;

    public PipeStreamingInterface(DownloadLink dlink, DataProvider rarProvider) {
        this.dataProvider = rarProvider;
        this.link = dlink;
    }

    @Override
    public boolean isRangeRequestSupported() {
        return dataProvider.isRangeRequestSupported(link);
    }

    @Override
    public long getFinalFileSize() {
        return dataProvider.getFinalFileSize(link);
    }

    @Override
    public InputStream getInputStream(long startPosition, long stopPosition) throws IOException {
        return dataProvider.getInputStream(link, startPosition, stopPosition);
    }

    @Override
    public void close() throws IOException {
        dataProvider.close();
    }

}
