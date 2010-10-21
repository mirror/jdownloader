//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.controlling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jd.nutils.Formatter;

public class ByteBufferController {

    private final ArrayList<ByteBufferEntry> bufferpool;

    public final static String MAXBUFFERSIZE = "MAX_BUFFER_SIZE_V3";

    private static final ByteBufferController INSTANCE = new ByteBufferController();

    private final Comparator<ByteBufferEntry> bytebuffercomp = new Comparator<ByteBufferEntry>() {
        public int compare(final ByteBufferEntry a, final ByteBufferEntry b) {
            final int acapacity = a.capacity();
            final int bcapacity = b.capacity();
            return acapacity == bcapacity ? 0 : acapacity > bcapacity ? 1 : -1;
        }
    };

    protected long bufferEntries = 0;

    public static ByteBufferController getInstance() {
        return INSTANCE;
    }

    public void printDebug() {
        long free = 0;
        synchronized (bufferpool) {
            for (final ByteBufferEntry entry : bufferpool) {
                free += entry.capacity();
            }
        }
        JDLogger.getLogger().info("ByteBufferController: Used: " + Formatter.formatReadable(bufferEntries - free) + " Free: " + Formatter.formatReadable(free));
    }

    private ByteBufferController() {
        bufferpool = new ArrayList<ByteBufferEntry>();
        new Thread("ByteBuffer debugger") {
            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(1000 * 60 * 10);
                    } catch (final InterruptedException e) {
                        break;
                    }
                    ByteBufferController.getInstance().printDebug();
                }
            }
        }.start();
    }

    protected ByteBufferEntry getByteBufferEntry(final int size) {
        ByteBufferEntry ret = null;
        synchronized (bufferpool) {
            for (final ByteBufferEntry entry : bufferpool) {
                if (entry.capacity() >= size) {
                    ret = entry;
                    bufferpool.remove(entry);
                    return ret.getbytebufferentry(size);
                }
            }
        }
        bufferEntries += size;
        return null;
    }

    protected void putByteBufferEntry(final ByteBufferEntry entry) {
        synchronized (bufferpool) {
            if (!bufferpool.contains(entry)) {
                bufferpool.add(entry);
            }
            Collections.sort(bufferpool, bytebuffercomp);
        }
    }
}
