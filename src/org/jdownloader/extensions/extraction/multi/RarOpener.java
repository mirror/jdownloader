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

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
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
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import org.appwork.exceptions.WTFException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;

/**
 * Used to join the separated rar files.
 *
 * @author botzi
 *
 */
class RarOpener implements IArchiveOpenVolumeCallback, IArchiveOpenCallback, ICryptoGetTextPassword, Closeable {
    private final Map<String, OpenerAccessTracker> openedRandomAccessFileList = new HashMap<String, OpenerAccessTracker>();
    private String                                 name                       = null;
    private final String                           password;
    private final Archive                          archive;
    private final HashMap<String, ArchiveFile>     map                        = new HashMap<String, ArchiveFile>();
    private String                                 firstName;
    private final Logger                           logger;
    private long                                   accessCounter              = 0l;

    RarOpener(Archive archive, Logger logger) {
        this(archive, null, logger);
    }

    RarOpener(Archive archive, String password, Logger logger) {
        if (password == null) {
            /* password null will crash jvm */
            this.password = "";
        } else {
            this.password = password;
        }
        this.archive = archive;
        this.logger = logger;
        init();
    }

    public void resetTracker() {
        for (OpenerAccessTracker tracker : getTrackedFiles()) {
            tracker.setAccessIndex(0);
        }
    }

    private void init() {
        // support for test.part01-blabla.tat archives.
        // we have to create a rename matcher map in this case because 7zip cannot handle this type
        if (logger != null) {
            logger.info("Init Map:");
        }
        ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        if (firstArchiveFile.getFilePath().matches("(?i).*\\.pa?r?t?\\.?\\d+\\D.*?\\.rar$")) {
            for (ArchiveFile af : archive.getArchiveFiles()) {
                String name = archive.getName() + "." + new Regex(af.getFilePath(), ".*(part\\d+)").getMatch(0) + ".rar";

                if (logger != null) {
                    logger.info(af.getFilePath() + " name: " + name);
                }
                if (af == firstArchiveFile) {
                    firstName = name;
                    if (logger != null) {
                        logger.info(af.getFilePath() + " FIRSTNAME name: " + name);
                    }
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

    public IInStream getStream(ArchiveFile firstArchiveFile) throws SevenZipException {
        return getStream(firstName == null ? firstArchiveFile.getFilePath() : firstName);
    }

    public IInStream getStream(String filename) throws SevenZipException {
        try {
            OpenerAccessTracker tracker = openedRandomAccessFileList.get(filename);
            if (tracker == null) {
                ArchiveFile af = map.get(filename);
                filename = af == null ? filename : af.getFilePath();
                tracker = new OpenerAccessTracker(filename, new RandomAccessFile(filename, "r"));
                openedRandomAccessFileList.put(filename, tracker);
            }
            if (name == null) {
                name = filename;
            }
            final OpenerAccessTracker finalTracker = tracker;
            finalTracker.getRandomAccessFile().seek(0);
            return new RandomAccessFileInStream(finalTracker.getRandomAccessFile()) {
                @Override
                public int read(byte[] abyte0) throws SevenZipException {
                    finalTracker.setAccessIndex(++accessCounter);
                    return super.read(abyte0);
                }

                @Override
                public long seek(long l, int i) throws SevenZipException {
                    finalTracker.setAccessIndex(++accessCounter);
                    return super.seek(l, i);
                }

            };
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
    public void close() throws IOException {
        Iterator<Entry<String, OpenerAccessTracker>> it = openedRandomAccessFileList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, OpenerAccessTracker> next = it.next();
            try {
                next.getValue().getRandomAccessFile().close();
            } catch (final Throwable e) {
            }
            it.remove();
        }
    }

    public Collection<OpenerAccessTracker> getTrackedFiles() {
        return openedRandomAccessFileList.values();
    }

    public void setCompleted(Long files, Long bytes) throws SevenZipException {

    }

    public void setTotal(Long files, Long bytes) throws SevenZipException {
    }

    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }

}