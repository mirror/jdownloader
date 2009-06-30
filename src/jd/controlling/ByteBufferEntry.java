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

import java.nio.ByteBuffer;

public class ByteBufferEntry {
    public ByteBuffer buffer = null;
    private int size = 0;
    private boolean unused = true;

    public static ByteBufferEntry getByteBufferEntry(int size) {
        ByteBufferEntry ret = ByteBufferController.getInstance().getByteBufferEntry(size);
        if (ret != null) {
            return ret.getbytebufferentry(size);
        } else {
            return new ByteBufferEntry(size).getbytebufferentry(size);
        }
    }

    private ByteBufferEntry(int size) {
        this.size = size;
        buffer = ByteBuffer.allocateDirect(size);
        System.out.println("buffer is direct " + buffer.isDirect());
        clear();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public void clear() {
        buffer.clear();
        buffer.limit(size);
    }

    public void clear(int size) {
        this.size = size;
        buffer.clear();
        buffer.limit(size);
    }

    public int size() {
        return size;
    }

    public void limit(int size) {
        this.size = size;
        buffer.limit(size);
    }

    protected ByteBufferEntry getbytebufferentry(int size) {
        unused = false;
        this.size = size;
        clear();
        return this;
    }

    /*
     * may be called only once in lifetime of the bytebufferentry!, please call
     * this only at the end of usage, because buffer is instantly available for
     * others to use
     */
    public void setUnused() {
        if (unused) return;
        unused = true;
        ByteBufferController.getInstance().putByteBufferEntry(this);
    }

}