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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

/**
 * Used to join the separated HJSplit and 7z files.
 * 
 * @author botzi
 * 
 */
class MultiOpener implements IArchiveOpenVolumeCallback, ICryptoGetTextPassword {
    private Map<String, OpenerAccessTracker> openedRandomAccessFileList = new HashMap<String, OpenerAccessTracker>();
    private final String                     password;
    private final AtomicLong                 accessCounter              = new AtomicLong(0);

    MultiOpener() {
        this(null);
    }

    MultiOpener(String password) {
        if (password == null) {
            /* password null will crash jvm */
            password = "";
        }
        this.password = password;
    }

    public Object getProperty(PropID propID) throws SevenZipException {
        return null;
    }

    public boolean isStreamOpen(String filename) {
        return openedRandomAccessFileList.containsKey(filename);
    }

    public IInStream getStream(String filename) throws SevenZipException {
        try {
            OpenerAccessTracker tracker = openedRandomAccessFileList.get(filename);
            if (tracker == null) {
                tracker = new OpenerAccessTracker(filename, new RandomAccessFile(filename, "r"));
                openedRandomAccessFileList.put(filename, tracker);
            }
            final OpenerAccessTracker finalTracker = tracker;
            finalTracker.getRandomAccessFile().seek(0);
            return new RandomAccessFileInStream(finalTracker.getRandomAccessFile()) {
                @Override
                public int read(byte[] abyte0) throws SevenZipException {
                    finalTracker.setAccessIndex(accessCounter.incrementAndGet());
                    return super.read(abyte0);
                }

                @Override
                public long seek(long l, int i) throws SevenZipException {
                    finalTracker.setAccessIndex(accessCounter.incrementAndGet());
                    return super.seek(l, i);
                }
            };
        } catch (FileNotFoundException fileNotFoundException) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void resetTracker() {
        for (OpenerAccessTracker tracker : getTrackedFiles()) {
            tracker.setAccessIndex(0);
        }
    }

    public Collection<OpenerAccessTracker> getTrackedFiles() {
        return openedRandomAccessFileList.values();
    }

    /**
     * Closes all open files.
     * 
     * @throws IOException
     */
    void close() throws IOException {
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

    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }
}