/*
 * Copyright (C) 2002 - 2005 Leonardo Ferracci
 *
 * This file is part of JAxe.
 *
 * JAxe is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * JAxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with JAxe; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.  Or, visit http://www.gnu.org/copyleft/gpl.html
 */

package jd.plugins.optional.extraction.hjsplit.jaxe;

import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends InputStream {
    private int iLastRead = 0;
    private InputStream is;
    private long lRead = 0;
    private long lTotal = -1;

    public CountingInputStream(InputStream input) {
        is = input;
    }

    //@Override
    public int available() throws IOException {
        return is.available();
    }

    //@Override
    public void close() throws IOException {
        is.close();
    }

    public long getDiff() {
        return lTotal - lRead;
    }

    public int getLastRead() {
        return iLastRead;
    }

    public int getLastReadAndReset() {
        int i = iLastRead;

        iLastRead = 0;
        return i;
    }

    public long getRead() {
        return lRead;
    }

    //@Override
    public void mark(int i) {
        is.mark(i);
    }

    //@Override
    public boolean markSupported() {
        return is.markSupported();
    }

    //@Override
    public int read() throws IOException {
        lRead++;
        iLastRead = 1;
        return is.read();
    }

    //@Override
    public int read(byte[] ba) throws IOException {
        iLastRead = is.read(ba);

        lRead += iLastRead;
        return iLastRead;
    }

    //@Override
    public int read(byte[] ba, int off, int len) throws IOException {
        iLastRead = is.read(ba, off, len);

        lRead += iLastRead;
        return iLastRead;
    }

    //@Override
    public void reset() throws IOException {
        is.reset();
    }

    public void setTotal(long l) {
        lTotal = l;
    }

    //@Override
    public long skip(long l) throws IOException {
        return is.skip(l);
    }
}
