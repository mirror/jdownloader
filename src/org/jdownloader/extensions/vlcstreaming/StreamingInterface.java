package org.jdownloader.extensions.vlcstreaming;

import java.io.IOException;
import java.io.InputStream;

public interface StreamingInterface {

    boolean isRangeRequestSupported();

    long getFinalFileSize();

    InputStream getInputStream(long startPosition, long stopPosition) throws IOException;

    public void close();

}
