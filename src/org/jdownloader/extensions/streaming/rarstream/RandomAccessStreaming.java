package org.jdownloader.extensions.streaming.rarstream;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.io.streamingio.Streaming;
import org.appwork.utils.io.streamingio.StreamingInputStream;
import org.jdownloader.extensions.extraction.ArchiveFile;

public class RandomAccessStreaming implements IInStream {

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

    public RandomAccessStreaming(ArchiveFile archiveFile, String filename, RarStreamProvider rarOpener, Streaming streaming) throws SevenZipException {
        this.archiveFile = archiveFile;
        this.filename = filename;
        owner = rarOpener;
        this.streaming = streaming;

    }

    protected Streaming            streaming;
    protected AtomicLong           currentPosition    = new AtomicLong(0);
    protected StreamingInputStream currentInputStream = null;
    protected RarStreamProvider    owner              = null;
    private int                    seekMode;

    @Override
    public synchronized int read(byte[] abyte0) throws SevenZipException {

        owner.setLatestAccessedStream(this);

        if (abyte0.length == 0) return 0;

        StreamingInputStream lcurrentInputStream = currentInputStream;
        try {
            int ret = -1;
            if (lcurrentInputStream != null) {
                ret = lcurrentInputStream.read(abyte0);
            } else {
                currentInputStream = streaming.getInputStream(currentPosition.get(), -1);
                ret = currentInputStream.read(abyte0);

            }
            if (ret > 0) {
                // if (!owner.isReadyForExtract()) {
                // StringBuffer hexString = new StringBuffer();
                // for (int i = 0; i < ret; i++) {
                // String hex = Integer.toHexString(0xFF & abyte0[i]);
                // if (hex.length() == 1) hexString.append("0");
                // hexString.append(hex);
                // }
                // owner.getLogger().finer("Read at " + currentPosition + " " + ret + " bytes: " + hexString);
                // if (ret > 100) {
                // owner.getLogger().finer(URLEncoder.encode(new String(abyte0), "UTF-8"));
                // }
                // }
                currentPosition.addAndGet(ret);

            }
            if (ret >= 0) {
                currentPosition.set(currentInputStream.getCurrentPosition());
            }
            if (ret <= 0) {
                /* map EOF to 0, according to http://sevenzipjbind.sourceforge.net/javadoc/net/sf/sevenzipjbinding/ISequentialInStream.html */
                return 0;
            } else {
                return ret;
            }
        } catch (IOException e) {
            throw new SevenZipException(e);
        }
    }

    @Override
    public synchronized long seek(long l, int i) throws SevenZipException {
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

            currentPosition.set(streaming.getFinalFileSize() + l);
            break;
        default:
            throw new RuntimeException((new StringBuilder()).append("Seek: unknown origin: ").append(i).toString());
        }
        // if (!owner.isReadyForExtract()) {
        // owner.getLogger().finer("Seek " + currentPosition.get() + " " + filename + " i " + i + " L " + l);
        // }
        return currentPosition.get();
    }

    public synchronized void close() {
        StreamingInputStream lcurrentInputStream = currentInputStream;
        currentInputStream = null;
        if (lcurrentInputStream != null) {
            lcurrentInputStream.close();
        }
    }

}
