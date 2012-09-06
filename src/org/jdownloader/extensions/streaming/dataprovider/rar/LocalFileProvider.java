package org.jdownloader.extensions.streaming.dataprovider.rar;

import java.io.File;
import java.io.InputStream;

public interface LocalFileProvider {

    InputStream getInputStream(File file, long startPosition, long stopPosition);

    void close();

}
