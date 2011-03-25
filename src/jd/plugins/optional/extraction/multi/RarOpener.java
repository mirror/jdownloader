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

package jd.plugins.optional.extraction.multi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

/**
 * Used to join the separated rar files.
 * 
 * @author botzi
 * 
 */
class RarOpener implements IArchiveOpenVolumeCallback, IArchiveOpenCallback, ICryptoGetTextPassword {
    private Map<String, RandomAccessFile> openedRandomAccessFileList = new HashMap<String, RandomAccessFile>();
    private String                        name;
    private String                        password;

    RarOpener() {
        this.password = "";
    }

    RarOpener(String password) {
        this.password = password;
    }

    public Object getProperty(PropID propID) throws SevenZipException {
        switch (propID) {
        case NAME:
            return name;
        }
        return null;
    }

    public IInStream getStream(String filename) throws SevenZipException {
        try {
            RandomAccessFile randomAccessFile = openedRandomAccessFileList.get(filename);
            if (randomAccessFile != null) {
                randomAccessFile.seek(0);
                name = filename;
                return new RandomAccessFileInStream(randomAccessFile);
            }

            randomAccessFile = new RandomAccessFile(filename, "r");

            openedRandomAccessFileList.put(filename, randomAccessFile);

            name = filename;
            return new RandomAccessFileInStream(randomAccessFile);
        } catch (FileNotFoundException fileNotFoundException) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes open files.
     * 
     * @throws IOException
     */
    void close() throws IOException {
        for (RandomAccessFile file : openedRandomAccessFileList.values()) {
            file.close();
        }
    }

    public void setCompleted(Long files, Long bytes) throws SevenZipException {
    }

    public void setTotal(Long files, Long bytes) throws SevenZipException {
    }

    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }
}