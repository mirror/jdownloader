package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;
import java.io.InputStream;

public interface StreamFactoryInterface {
    public boolean isClosed();

    long getContentLength();

    InputStream getInputStream(long start, long end) throws IOException;

    long getGuaranteedContentLength();

    void close();

}
