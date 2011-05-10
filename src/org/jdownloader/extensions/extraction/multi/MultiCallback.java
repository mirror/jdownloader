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
import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

import org.jdownloader.extensions.extraction.CPUPriority;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;

/**
 * Gets the decrypted bytes and writes it into the file.
 * 
 * @author botzi
 * 
 */
class MultiCallback implements ISequentialOutStream {
    private FileOutputStream     fos = null;
    private ExtractionController con;
    private CPUPriority          priority;

    MultiCallback(File file, ExtractionController con, CPUPriority priority2, boolean shouldCrc) throws FileNotFoundException {
        this.con = con;
        this.priority = priority2;

        fos = new FileOutputStream(file, true);
    }

    public int write(byte[] data) throws SevenZipException {
        try {
            fos.write(data);

            con.getArchiv().setExtracted(con.getArchiv().getExtracted() + data.length);

            if (priority != null) {
                try {
                    if (priority == CPUPriority.NORMAL) {
                        Thread.sleep(100);
                    } else if (priority == CPUPriority.LOW) {
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    con.setExeption(e);
                    con.getArchiv().setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                }
            }
        } catch (FileNotFoundException e) {
            con.setExeption(e);
            con.getArchiv().setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
        } catch (IOException e) {
            con.setExeption(e);
            con.getArchiv().setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
        }

        return data.length;
    }

    /**
     * Closes the unpacking.
     * 
     * @throws IOException
     */
    void close() throws IOException {
        try {
            if (fos != null) fos.flush();
        } catch (Throwable e) {
        }
        try {
            if (fos != null) fos.close();
        } catch (Throwable e) {
        }

    }
}