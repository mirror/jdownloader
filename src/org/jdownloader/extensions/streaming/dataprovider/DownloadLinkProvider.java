package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import jd.plugins.DownloadLink;

import org.appwork.utils.io.streamingio.Streaming;
import org.jdownloader.extensions.streaming.StreamingExtension;

public class DownloadLinkProvider implements DataProvider<DownloadLink> {

    private StreamingExtension               streamingExtension;
    private HashMap<DownloadLink, Streaming> map;

    public DownloadLinkProvider(StreamingExtension streamingExtension) {
        this.streamingExtension = streamingExtension;
        map = new HashMap<DownloadLink, Streaming>();
    }

    @Override
    public boolean isRangeRequestSupported(DownloadLink link) {
        return true;
    }

    @Override
    public long getFinalFileSize(DownloadLink link) {
        return link.getDownloadSize();
    }

    public String toString() {
        return getClass().getSimpleName() + "<< Downloadlink Streaming";
    }

    @Override
    public InputStream getInputStream(DownloadLink link, long startPosition, long stopPosition) throws IOException {
        Streaming streaming = map.get(link);

        if (streaming == null || streaming.isClosed()) {
            streaming = streamingExtension.getStreamProvider().getStreamingProvider(link);

            map.put(link, streaming);
        }

        return streaming.getInputStream(startPosition, stopPosition);
    }

    @Override
    public void close() throws IOException {
        for (Streaming s : map.values()) {
            s.close();
        }
    }

    @Override
    public boolean canHandle(DownloadLink link, DataProvider<?>... dataProviders) {
        return link instanceof DownloadLink;
    }

}
