//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.utils;

import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class DynByteBuffer {

    private ByteBuffer buffer;

    public DynByteBuffer(int l) {
        this.buffer = ByteBuffer.allocateDirect(l);
    }

    public void put(byte[] buffer, int read) {
        checkBufferSize(read);
        this.buffer.put(buffer);
    }

    public void clear() {
        this.buffer.clear();
    }

    public String toString() {
        
        try {
            return new String(this.getLast(buffer.position()),Executer.CODEPAGE);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new String(this.getLast(buffer.position()));
        }
    }

    public int capacity() {
        return this.buffer.capacity();
    }

    public int limit() {
        return this.buffer.limit();
    }

    public int position() {
        return this.buffer.position();
    }

    private void checkBufferSize(int read) {
        if (this.buffer.remaining() < read) {
            ByteBuffer newbuffer = ByteBuffer.allocateDirect(this.buffer.capacity() * 2);
            this.buffer.flip();
            newbuffer.put(this.buffer);
            this.buffer = newbuffer;

        }

    }

    public byte get() {
        return buffer.get();
    }

    public Buffer flip() {
        return this.buffer.flip();
    }

    public ByteBuffer compact() {
        return this.buffer.compact();
    }

    public byte[] getLast(int num) {
        int posi = buffer.position();
        num = Math.min(posi, num);
        buffer.position(posi - num);
        byte[] b = new byte[num];
        buffer.get(b);
        buffer.position(posi);
        return b;

    }

}
