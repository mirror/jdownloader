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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;

class MultiOpener implements IArchiveOpenVolumeCallback, IArchiveOpenCallback, ICryptoGetTextPassword, Closeable {
    private final class RandomAcessFileStats {
        private final RandomAccessFile raf;
        private long                   bytesRead = 0;
        private long                   bytesSeek = 0;

        private RandomAcessFileStats(RandomAccessFile raf) {
            this.raf = raf;
        }
    }

    private final Map<String, RandomAcessFileStats> openedRandomAccessFileList = new HashMap<String, RandomAcessFileStats>();
    private final HashMap<String, ArchiveFile>      map                        = new HashMap<String, ArchiveFile>();
    private final String                            password;
    private String                                  name                       = null;
    private final Archive                           archive;
    private final LogInterface                      logger;

    MultiOpener(Archive archive, LogInterface logger) {
        this(archive, null, logger);
    }

    MultiOpener(Archive archive, String password, LogInterface logger) {
        if (password == null) {
            /* password null will crash jvm */
            password = "";
        }
        this.logger = logger;
        this.password = password;
        this.archive = archive;
    }

    public Object getProperty(PropID propID) throws SevenZipException {
        switch (propID) {
        case NAME:
            return name;
        }
        return null;
    }

    public IInStream getStream(ArchiveFile archiveFile) throws SevenZipException {
        return getStream(archiveFile.getFilePath());
    }

    private final String fixInternalName(final String name) {
        return name;
    }

    public IInStream getStream(final String fileName) throws SevenZipException {
        ArchiveFile af = null;
        try {
            RandomAcessFileStats tracker = openedRandomAccessFileList.get(fileName);
            if (tracker == null) {
                af = map.get(fileName);
                if (af == null) {
                    af = archive.getBestArchiveFileMatch(fileName);
                    if (af != null) {
                        if (!map.values().contains(af)) {
                            map.put(fileName, af);
                        } else {
                            // don't open the same file twice
                            throw new FileNotFoundException(fileName);
                        }
                    }
                }
                final File file;
                if (af != null) {
                    file = new File(af.getFilePath());
                } else {
                    file = new File(fileName);
                }
                logger.info("OpenFile->Filename:" + fileName + "|ArchiveFile:" + af + "|Filename(onDisk)" + file.getAbsolutePath());
                tracker = new RandomAcessFileStats(IO.open(file, "r"));
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
        } catch (FileNotFoundException e) {
            if (af != null) {
                logger.log(e);
                throw new SevenZipException(e);
            } else {
                return null;
            }
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
        map.clear();
    }

    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }

    @Override
    public void setCompleted(Long arg0, Long arg1) throws SevenZipException {
    }

    @Override
    public void setTotal(Long arg0, Long arg1) throws SevenZipException {
    }
}