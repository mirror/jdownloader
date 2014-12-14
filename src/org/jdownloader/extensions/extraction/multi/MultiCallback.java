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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

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
class MultiCallback implements ISequentialOutStream {
    protected final FileOutputStream     fos;
    protected final CPUPriority          priority;
    protected final BufferedOutputStream bos;
    protected long                       written = 0;
    protected final File                 file;

    MultiCallback(File file, ExtractionController con, ExtractionConfig config, boolean shouldCrc) throws FileNotFoundException {
        final CPUPriority priority = config.getCPUPriority();
        if (priority == null || CPUPriority.HIGH.equals(priority)) {
            this.priority = null;
        } else {
            this.priority = priority;
        }
        final int maxbuffersize = Math.max(config.getBufferSize() * 1024, 10240);
        this.file = file;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, false);
        } catch (final FileNotFoundException e) {
            /**
             * too fast file opening/extraction (eg image gallery) can result in "access denied" exception
             */
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                throw e;
            }
            fos = new FileOutputStream(file, false);
        }
        this.fos = fos;
        bos = new BufferedOutputStream(this.fos, maxbuffersize);
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
        try {
            bos.write(data);
            written += data.length;
            waitCPUPriority();
        } catch (IOException e) {
            throw new MultiSevenZipException(e, ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
        }
        return data.length;
    }

    public File getFile() {
        return file;
    }

    public long getWritten() {
        return written;
    }

    /**
     * Closes the unpacking.
     * 
     * @throws IOException
     */
    void close() throws IOException {
        try {
            bos.flush();
        } catch (Throwable e) {
        }
        try {
            bos.close();
        } catch (Throwable e) {
        }
        try {
            fos.flush();
        } catch (Throwable e) {
        }
        try {
            fos.close();
        } catch (Throwable e) {
        }
    }
}