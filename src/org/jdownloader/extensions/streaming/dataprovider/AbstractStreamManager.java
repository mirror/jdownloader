package org.jdownloader.extensions.streaming.dataprovider;

import java.util.HashMap;

import org.jdownloader.extensions.streaming.StreamingExtension;

public abstract class AbstractStreamManager<LinkType> {

    private StreamingExtension extension;

    public StreamingExtension getExtension() {
        return extension;
    }

    protected HashMap<Object, StreamFactoryInterface> map;

    public AbstractStreamManager(StreamingExtension extension) {

        this.extension = extension;
        map = new HashMap<Object, StreamFactoryInterface>();
    }

}
