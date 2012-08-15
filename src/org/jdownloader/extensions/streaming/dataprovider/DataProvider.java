package org.jdownloader.extensions.streaming.dataprovider;

import java.io.IOException;
import java.io.InputStream;

public interface DataProvider<T> {

    boolean canHandle(T link, DataProvider<?>... dataProviders);

    boolean isRangeRequestSupported(T link);

    long getFinalFileSize(T link);

    InputStream getInputStream(T link, long startPosition, long stopPosition) throws IOException;

    public void close() throws IOException;

}
