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
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import jd.parser.Regex;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;

class RarOpener implements IArchiveOpenVolumeCallback, IArchiveOpenCallback, ICryptoGetTextPassword, Closeable {

    private final class RandomAcessFileStats {

        private final RandomAccessFile raf;
        private long                   bytesRead = 0;
        private long                   bytesSeek = 0;

        private RandomAcessFileStats(RandomAccessFile raf) {
            this.raf = raf;
        }

    }

    private final Map<String, RandomAcessFileStats> openedRandomAccessFileList = new HashMap<String, RandomAcessFileStats>();
    private String                                  name                       = null;
    private final String                            password;
    private final Archive                           archive;
    private String                                  firstName;
    private final LogInterface                      logger;

    RarOpener(Archive archive, LogInterface logger) {
        this(archive, null, logger);
    }

    RarOpener(Archive archive, String password, LogInterface logger) {
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

    /*
     * additional dots before .rar confuses 7zip
     */
    private final String fixInternalName(final String name) {
        if (name != null && name.matches(("(?i).*\\.pa?r?t?\\.?\\d+\\D+\\.rar$"))) {
            return name.replaceFirst("(\\d+)(\\D+\\.rar)$", "$1.rar");
        } else {
            return name;
        }
    }

    private void init() {
        // support for test.part01-blabla.tat archives.
        // we have to create a rename matcher map in this case because 7zip cannot handle this type
        if (logger != null) {
            logger.info("Init Map:");
        }
        final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        if (firstArchiveFile.getFilePath().matches("(?i).*\\.pa?r?t?\\.?\\d+\\D.*?\\.rar$")) {
            for (ArchiveFile af : archive.getArchiveFiles()) {
                final String rest = new Regex(af.getFilePath(), ".*pa?r?t?\\.?\\d+(\\D.*?)\\.rar$").getMatch(0);
                final String name;
                if (rest == null) {
                    name = archive.getName() + "." + new Regex(af.getFilePath(), ".*(pa?r?t?\\d+)").getMatch(0) + ".rar";
                } else {
                    name = archive.getName() + "." + new Regex(af.getFilePath(), ".*(pa?r?t?\\d+)").getMatch(0) + rest + ".rar";
                }
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

    private final HashMap<String, ArchiveFile> map = new HashMap<String, ArchiveFile>();

    public IInStream getStream(final String fileName) throws SevenZipException {
        ArchiveFile af = null;
        try {
            RandomAcessFileStats tracker = openedRandomAccessFileList.get(fileName);
            if (tracker == null) {
                af = map.get(fileName);
                if (af == null) {
                    af = archive.getBestArchiveFileMatch(fileName);
                    if (af != null) {
                        map.put(fileName, af);
                    }
                }
                final File file;
                if (af != null) {
                    file = new File(af.getFilePath());
                } else {
                    file = new File(fileName);
                }
                tracker = new RandomAcessFileStats(IO.open(file, "r"));
                logger.info("OpenFile->Filename:+" + fileName + "|ArchiveFile:" + af + "|Filename(onDisk)" + file.getAbsolutePath());
                openedRandomAccessFileList.put(fileName, tracker);
            }
            if (name == null) {
                name = fixInternalName(fileName);
            }
            final RandomAcessFileStats finalTracker = tracker;
            finalTracker.raf.seek(0);
            return new RandomAccessFileInStream(finalTracker.raf) {
                @Override
                public int read(byte[] abyte0) throws SevenZipException {
                    final int read = super.read(abyte0);
                    if (read > 0) {
                        finalTracker.bytesRead += read;
                    }
                    return read;
                }

                @Override
                public synchronized long seek(long arg0, int arg1) throws SevenZipException {
                    final long seek = super.seek(arg0, arg1);
                    if (seek > 0) {
                        finalTracker.bytesSeek += seek;
                    }
                    return seek;
                }

            };
        } catch (IOException e) {
            logger.log(e);
            if (af != null) {
                throw new SevenZipException(e);
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.log(e);
            throw new SevenZipException(e);
        }
    }

    public void close() throws IOException {
        final Iterator<Entry<String, RandomAcessFileStats>> it = openedRandomAccessFileList.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, RandomAcessFileStats> next = it.next();
            try {
                logger.info("CloseFile->Filename:" + next.getKey() + "|BytesRead:" + next.getValue().bytesRead + "|BytesSeek:" + next.getValue().bytesSeek);
                next.getValue().raf.close();
            } catch (final Throwable e) {
                logger.log(e);
            } finally {
                it.remove();
            }
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