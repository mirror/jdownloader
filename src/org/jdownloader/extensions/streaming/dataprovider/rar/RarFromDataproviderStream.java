package org.jdownloader.extensions.streaming.dataprovider.rar;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.streaming.dataprovider.DataProvider;

public class RarFromDataproviderStream implements IInStream {

    private ArchiveFile               archiveFile;
    private String                    filename;
    private DataProvider<ArchiveFile> dataProvider;

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

    public RarFromDataproviderStream(ArchiveFile archiveFile, String filename, RarArchiveDataProvider rarFileProvider, DataProvider<ArchiveFile> dp) {
        this.archiveFile = archiveFile;
        this.filename = filename;
        owner = rarFileProvider;
        this.dataProvider = dp;

    }

    protected AtomicLong             currentPosition = new AtomicLong(0);

    protected RarArchiveDataProvider owner           = null;
    private int                      seekMode;
    private InputStream              inputStream;

    @Override
    public synchronized int read(byte[] abyte0) throws SevenZipException {
        try {
            owner.setLatestAccessedStream(this);

            if (abyte0.length == 0) return 0;

            if (inputStream == null) {
                inputStream = dataProvider.getInputStream(archiveFile, currentPosition.get(), -1);
            }

            int ret = inputStream.read(abyte0);
            System.out.println("REad " + ret + " at " + currentPosition.get());
            if (ret > 0) {
                currentPosition.addAndGet(ret);

            }
            // if (ret >= 0) {
            // currentPosition.set(inputStream.);
            // }
            if (ret <= 0) {
                /*
                 * map EOF to 0, according to
                 * http://sevenzipjbind.sourceforge.net
                 * /javadoc/net/sf/sevenzipjbinding/ISequentialInStream.html
                 */
                return 0;
            } else {
                return ret;
            }
        } catch (IOException e) {
            e.printStackTrace();
            owner.setException(e);
            throw new SevenZipException(e);
        }
    }

    @Override
    public synchronized long seek(long l, int i) throws SevenZipException {
        try {
            owner.setLatestAccessedStream(this);
            close();
            seekMode = i;
            switch (seekMode) {
            case SEEK_SET:
                currentPosition.set(l);
                break;
            case SEEK_CUR:
                currentPosition.addAndGet(l);
                break;
            case SEEK_END:

                currentPosition.set(dataProvider.getFinalFileSize(archiveFile) + l);
                break;
            default:
                throw new RuntimeException((new StringBuilder()).append("Seek: unknown origin: ").append(i).toString());
            }
            // if (!owner.isReadyForExtract()) {
            // owner.getLogger().finer("Seek " + currentPosition.get() + " " +
            // filename + " i " + i + " L " + l);
            // }
            return currentPosition.get();

        } catch (IOException e) {
            throw new SevenZipException(e);
        }
    }

    public synchronized void close() throws IOException {
        InputStream lcurrentInputStream = inputStream;
        inputStream = null;
        if (lcurrentInputStream != null) {
            lcurrentInputStream.close();
        }
    }

}
