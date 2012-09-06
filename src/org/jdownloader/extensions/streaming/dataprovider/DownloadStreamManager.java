package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;

import jd.plugins.DownloadLink;

import org.appwork.utils.io.streamingio.Streaming;
import org.jdownloader.extensions.streaming.StreamingExtension;

public class DownloadStreamManager extends AbstractStreamManager<DownloadLink> {

    public DownloadStreamManager(StreamingExtension extension) {
        super(extension);
    }

    protected StreamFactoryInterface createNewFactory(DownloadLink link) throws IOException {
        Streaming streamProvider = getExtension().getStreamProvider().getStreamingProvider(link);
        return new DownloadStreamFactory(streamProvider);
    }

    public StreamFactoryInterface getStreamFactory(DownloadLink dlink) throws IOException {
        StreamFactoryInterface streaming = map.get(dlink);

        if (streaming == null || streaming.isClosed()) {
            streaming = createNewFactory(dlink);
            map.put(dlink, streaming);
        }

        return streaming;
    }

}
