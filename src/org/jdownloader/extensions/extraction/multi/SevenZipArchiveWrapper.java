package org.jdownloader.extensions.extraction.multi;

import java.util.Date;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;

public interface SevenZipArchiveWrapper {

    int getNumberOfItems();

    boolean isSlowDownWorkaroundNeeded();

    ArchiveFormat getArchiveFormat();

    Boolean isEncrypted(int index);

    Boolean isFolder(int index);

    Long getSize(int index);

    Long getPackedSize(int index);

    String getPath(int index);

    String getMethod(int index);

    Integer getAttributes(int index);

    Date getLastWriteTime(int index);

    void close() throws SevenZipException;

    void extract(int[] indices, boolean testMode, IArchiveExtractCallback extractCallback) throws SevenZipException;

    ISimpleInArchive getSimpleInterface();

    Object getArchiveProperty(PropID propID);
}
