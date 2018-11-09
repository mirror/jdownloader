//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.jdownloader.extensions.extraction.multi;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.download.raf.FileBytesCache;
import jd.plugins.download.raf.FileBytesCacheFlusher;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.IO;
import org.jdownloader.extensions.extraction.CPUPriority;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.FLUSH_MODE;

/**
 * Gets the decrypted bytes and writes it into the file.
 *
 * @author botzi
 *
 */
class MultiCallback implements ISequentialOutStream, FileBytesCacheFlusher {
    protected final RandomAccessFile   fos;
    protected final CPUPriority        priority;
    protected final AtomicLong         fileWritePosition = new AtomicLong(0);
    protected final AtomicLong         flushedBytes      = new AtomicLong(0);
    protected final File               file;
    private final FileBytesCache       cache;
    private final AtomicBoolean        fileOpen          = new AtomicBoolean(true);
    private volatile IOException       ioException       = null;
    private final ExtractionController con;
    private final FLUSH_MODE           flushMode;

    MultiCallback(File file, ExtractionController con, ExtractionConfig config) throws IOException {
        final CPUPriority priority = config.getCPUPriority();
        if (priority == null || CPUPriority.HIGH.equals(priority)) {
            this.priority = null;
        } else {
            this.priority = priority;
        }
        final FLUSH_MODE flushMode = config.getFlushMode();
        if (flushMode == null) {
            this.flushMode = FLUSH_MODE.NONE;
        } else {
            this.flushMode = flushMode;
        }
        this.con = con;
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

    public int write(final byte[] data) throws SevenZipException {
        if (fileOpen.get()) {
            cache.write(this, fileWritePosition.get(), data, data.length);
            fileWritePosition.addAndGet(data.length);
            waitCPUPriority();
            return data.length;
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
        return flushedBytes.get();
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
                            cache.flushIfContains(MultiCallback.this);
                        } finally {
                            fileOpen.set(false);
                        }
                    }
                }
            });
        } finally {
            try {
                if (fos != null) {
                    try {
                        switch (flushMode) {
                        case FULL:
                            fos.getChannel().force(true);
                            break;
                        case DATA:
                            fos.getChannel().force(false);
                            break;
                        default:
                            break;
                        }
                    } finally {
                        fos.close();
                    }
                }
            } catch (Throwable e) {
                con.getLogger().log(e);
            }
        }
    }

    @Override
    public void flush(byte[] writeCache, int writeCachePosition, int length, long fileWritePosition) {
        if (fileOpen.get()) {
            try {
                fos.seek(fileWritePosition);
                fos.write(writeCache, writeCachePosition, length);
                flushedBytes.addAndGet(length);
            } catch (final IOException e) {
                con.getLogger().log(e);
                ioException = e;
                fileOpen.set(false);
            }
        }
    }

    @Override
    public void flushed() {
    }
}