package org.jdownloader.extensions.vlcstreaming.rarstream;

import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.jdownloader.extensions.extraction.ArchiveFile;

public class RandomAccessStream implements IInStream {

    private ArchiveFile archiveFile;
    private String      filename;

    public ArchiveFile getArchiveFile() {
        return archiveFile;
    }

    public void setArchiveFile(ArchiveFile archiveFile) {
        this.archiveFile = archiveFile;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    private RarStreamProvider owner;

    public RandomAccessStream(ArchiveFile archiveFile, String filename, RarStreamProvider rarOpener) {

        this.archiveFile = archiveFile;
        this.filename = filename;
        owner = rarOpener;
    }

    @Override
    public long seek(long l, int i) throws SevenZipException {
        System.out.println("SEEK " + filename + " l" + l + " i" + i);
        owner.setLatestAccessedStream(this);
        return 0;
    }

    @Override
    public int read(byte[] abyte0) throws SevenZipException {
        System.out.println("READ " + filename + " abyte0 " + abyte0.length);
        owner.setLatestAccessedStream(this);

        return 0;

    }

    public void close() {
    }

}
