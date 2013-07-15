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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.parser.Regex;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.exceptions.WTFException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;

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
    private Archive                       archive;
    private HashMap<String, ArchiveFile>  map;
    private String                        firstName;
    private Logger                        logger;
    private ExtRandomAccessFileInStream   latestAccessedStream;

    RarOpener(Archive archive) {
        this.password = "";
        this.archive = archive;
        init();
    }

    RarOpener(Archive archive, String password) {
        this.password = password;
        this.archive = archive;
        init();
    }

    private void init() {
        map = new HashMap<String, ArchiveFile>();
        // support for test.part01-blabla.tat archives.
        // we have to create a rename matcher map in this case because 7zip cannot handle this type
        if (logger != null) logger.info("Init Map:");
        if (archive.getFirstArchiveFile().getFilePath().matches("(?i).*\\.pa?r?t?\\.?\\d+\\D.*?\\.rar$")) {
            for (ArchiveFile af : archive.getArchiveFiles()) {
                String name = archive.getName() + "." + new Regex(af.getFilePath(), ".*(part\\d+)").getMatch(0) + ".rar";

                if (logger != null) logger.info(af.getFilePath() + " name: " + name);
                if (af == archive.getFirstArchiveFile()) {
                    firstName = name;
                    if (logger != null) logger.info(af.getFilePath() + " FIRSTNAME name: " + name);
                }
                if (map.put(name, af) != null) {
                    //
                    throw new WTFException("Cannot handle " + af.getFilePath());
                }
            }

        }
    }

    public Object getProperty(PropID propID) throws SevenZipException {
        switch (propID) {
        case NAME:
            return name;
        }
        return null;
    }

    public boolean isStreamOpen(String filename) {
        return openedRandomAccessFileList.containsKey(filename);
    }

    public IInStream getStream(ArchiveFile firstArchiveFile) throws SevenZipException {
        return getStream(firstName == null ? firstArchiveFile.getFilePath() : firstName);
    }

    public IInStream getStream(String filename) throws SevenZipException {
        try {

            if (logger != null) logger.info("Stream request: " + filename);
            RandomAccessFile randomAccessFile = openedRandomAccessFileList.get(filename);
            ArchiveFile af = map.get(filename);
            if (randomAccessFile != null) {
                randomAccessFile.seek(0);
                name = filename;
                return new ExtRandomAccessFileInStream(randomAccessFile, af == null ? archive.getArchiveFileByPath(filename) : af, filename, this);
            }

            randomAccessFile = new RandomAccessFile(af == null ? filename : af.getFilePath(), "r");
            openedRandomAccessFileList.put(filename, randomAccessFile);
            if (logger != null) logger.info("New RandomAccess: " + (af == null ? filename : af.getFilePath()));
            name = filename;
            return new ExtRandomAccessFileInStream(randomAccessFile, af == null ? archive.getArchiveFileByPath(filename) : af, filename, this);
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
        Iterator<Entry<String, RandomAccessFile>> it = openedRandomAccessFileList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RandomAccessFile> next = it.next();
            try {
                next.getValue().close();
            } catch (final Throwable e) {
            }
            it.remove();
        }
    }

    public void setCompleted(Long files, Long bytes) throws SevenZipException {

    }

    public void setTotal(Long files, Long bytes) throws SevenZipException {
        System.out.println(2);
    }

    public String cryptoGetTextPassword() throws SevenZipException {

        return password;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setLatestAccessedStream(ExtRandomAccessFileInStream extRandomAccessFileInStream) {
        if (extRandomAccessFileInStream != latestAccessedStream) {

            latestAccessedStream = extRandomAccessFileInStream;
            onStreamUpdate(extRandomAccessFileInStream);

        }
    }

    protected void onStreamUpdate(ExtRandomAccessFileInStream extRandomAccessFileInStream) {

        logger.info("Extract from: " + extRandomAccessFileInStream.getFilename());
    }

    public ExtRandomAccessFileInStream getLatestAccessedStream() {
        return latestAccessedStream;
    }

}