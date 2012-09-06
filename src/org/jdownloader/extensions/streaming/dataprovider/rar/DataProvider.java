package org.jdownloader.extensions.streaming.dataprovider.rar;

import java.io.IOException;
import java.io.InputStream;

import org.jdownloader.extensions.extraction.ArchiveFile;

public interface DataProvider<T> {

    boolean canHandle(ArchiveFile archiveFile, DataProvider[] dataProviders);

    void close() throws IOException;

    InputStream getInputStream(ArchiveFile archiveFile, long l, int i);

    long getFinalFileSize(ArchiveFile archiveFile);

}
