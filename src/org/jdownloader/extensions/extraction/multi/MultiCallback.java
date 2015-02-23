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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.plugins.download.raf.FileBytesCache;
import jd.plugins.download.raf.FileBytesCacheFlusher;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.extraction.CPUPriority;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;

/**
 * Gets the decrypted bytes and writes it into the file.
 *
 * @author botzi
 *
 */
class MultiCallback implements ISequentialOutStream, FileBytesCacheFlusher {
    protected final RandomAccessFile fos;
    protected final CPUPriority      priority;
    protected volatile long          fileWritePosition = 0;
    protected volatile long          flushedBytes      = 0;
    protected final File             file;
    private final FileBytesCache     cache;
    private AtomicBoolean            fileOpen          = new AtomicBoolean(true);
    private volatile IOException     ioException       = null;

    MultiCallback(File file, ExtractionController con, ExtractionConfig config, boolean shouldCrc) throws FileNotFoundException {
        final CPUPriority priority = config.getCPUPriority();
        if (priority == null || CPUPriority.HIGH.equals(priority)) {
            this.priority = null;
        } else {
            this.priority = priority;
        }
        this.file = file;
        RandomAccessFile fos = null;
        try {
            fos = new RandomAccessFile(file, "rw");
        } catch (final FileNotFoundException e) {
            if (CrossSystem.isWindows()) {
                /**
                 * too fast file opening/extraction (eg image gallery) can result in "access denied" exception
                 */
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    throw e;
                }
                fos = new RandomAccessFile(file, "rw");
            } else {
                throw e;
            }
        }
        this.fos = fos;
        cache = con.getFileBytesCache();
    }

    protected void waitCPUPriority() throws SevenZipException {
        if (priority != null && !CPUPriority.HIGH.equals(priority)) {
            synchronized (this) {
                try {
                    wait(priority.getTime());
                } catch (InterruptedException e) {
                    throw new MultiSevenZipException(e, ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                }
            }
        }
    }

    public int write(byte[] data) throws SevenZipException {
        if (fileOpen.get()) {
            cache.write(this, fileWritePosition, data, data.length);
            fileWritePosition += data.length;
            waitCPUPriority();
            return data.length;
        } else {
            throw new MultiSevenZipException(ioException, ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
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
                            cache.flushIfContains(MultiCallback.this);
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