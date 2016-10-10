package org.jdownloader.extensions.extraction.multi;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.plugins.download.raf.FileBytesCache;
import jd.plugins.download.raf.FileBytesCacheFlusher;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.IO;
import org.jdownloader.extensions.extraction.CPUPriority;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;

public class FilesBytesCacheWriter implements FileBytesCacheFlusher {
    protected final RandomAccessFile fos;
    protected final CPUPriority      priority;
    protected volatile long          fileWritePosition = 0;
    protected volatile long          flushedBytes      = 0;
    protected final File             file;
    private final FileBytesCache     cache;
    private final AtomicBoolean      fileOpen          = new AtomicBoolean(true);
    private volatile IOException     ioException       = null;

    public FilesBytesCacheWriter(File file, ExtractionController con, ExtractionConfig config) throws IOException {
        final CPUPriority priority = config.getCPUPriority();
        if (priority == null || CPUPriority.HIGH.equals(priority)) {
            this.priority = null;
        } else {
            this.priority = priority;
        }
        this.file = file;
        this.fos = IO.open(file, "rw");
        cache = con.getFileBytesCache();
    }

    protected void waitCPUPriority() throws SevenZipException {
        if (priority != null) {
            synchronized (this) {
                try {
                    wait(priority.getTime());
                } catch (InterruptedException e) {
                    throw new MultiSevenZipException(e, ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                }
            }
        }
    }

    public int write(final byte[] data, final int length) throws SevenZipException {
        if (fileOpen.get()) {
            cache.write(this, fileWritePosition, data, length);
            fileWritePosition += length;
            waitCPUPriority();
            return length;
        } else {
            final IOException lIoException = ioException;
            if (lIoException != null) {
                throw new MultiSevenZipException(lIoException, ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            } else {
                throw new MultiSevenZipException(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            }
        }
    }

    public File getFile() {
        return file;
    }

    public long getWritten() {
        return flushedBytes;
    }

    /**
     * Closes the unpacking.
     *
     * @throws IOException
     */
    void close() throws IOException {
        try {
            cache.execute(new Runnable() {
                @Override
                public void run() {
                    if (fileOpen.get()) {
                        try {
                            cache.flushIfContains(FilesBytesCacheWriter.this);
                        } finally {
                            fileOpen.set(false);
                        }
                    }
                }
            });
        } finally {
            try {
                try {
                    if (fos != null) {
                        fos.getChannel().force(true);
                    }
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void flush(byte[] writeCache, int writeCachePosition, int length, long fileWritePosition) {
        if (fileOpen.get()) {
            try {
                fos.seek(fileWritePosition);
                fos.write(writeCache, writeCachePosition, length);
                flushedBytes += length;
            } catch (final IOException e) {
                ioException = e;
                fileOpen.set(false);
            }
        }
    }

    @Override
    public void flushed() {
    }

}
