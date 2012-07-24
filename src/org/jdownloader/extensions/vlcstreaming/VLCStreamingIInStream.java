package org.jdownloader.extensions.vlcstreaming;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.io.streamingio.Streaming;
import org.appwork.utils.io.streamingio.StreamingInputStream;

public class VLCStreamingIInStream implements IInStream {

    protected Streaming            streaming;
    protected AtomicLong           currentPosition    = new AtomicLong(0);
    protected StreamingInputStream currentInputStream = null;

    public VLCStreamingIInStream(Streaming streaming) {
        this.streaming = streaming;
    }

    @Override
    public synchronized int read(byte[] abyte0) throws SevenZipException {
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
        close();
        switch (i) {
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
