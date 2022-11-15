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
package jd.nutils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.appwork.utils.JDK8BufferHelper;
import org.jdownloader.logging.LogController;

public class DynByteBuffer {
    private ByteBuffer buffer;

    public DynByteBuffer(int l) {
        this.buffer = ByteBuffer.allocateDirect(l);
    }

    public void put(byte[] buffer, int read) {
        checkBufferSize(read);
        this.buffer.put(buffer);
    }

    public void put(byte b) {
        checkBufferSize(1);
        this.buffer.put(b);
    }

    public void put(byte[] bytes, int off, int len) {
        checkBufferSize(len);
        this.buffer.put(bytes, off, len);
    }

    public void clear() {
        JDK8BufferHelper.clear(buffer);
    }

    public String toString() {
        /* new String(byte) correct as we have seperate toString */
        return new String(this.getLast(buffer.position()));
    }

    public byte[] toByteArray() {
        return this.getLast(buffer.position());
    }

    public String toString(String codepage) {
        try {
            return new String(this.getLast(buffer.position()), codepage);
        } catch (UnsupportedEncodingException e) {
            LogController.CL().log(e);
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
            /* first we try to double capactiy */
            ByteBuffer newbuffer = ByteBuffer.allocateDirect(this.buffer.capacity() * 2);
            JDK8BufferHelper.flip(buffer);
            newbuffer.put(this.buffer);
            this.buffer = newbuffer;
        }
        if (this.buffer.remaining() < read) {
            /* still not enough, so lets increase even more */
            ByteBuffer newbuffer = ByteBuffer.allocateDirect(this.buffer.capacity() + read);
            // jdk8:flip is in Buffer -> jdk9+:flip override in actual implementation
            JDK8BufferHelper.flip(buffer);
            newbuffer.put(this.buffer);
            this.buffer = newbuffer;
        }
    }

    public byte get() {
        return buffer.get();
    }

    public Buffer flip() {
        return JDK8BufferHelper.flip(buffer);
    }

    public ByteBuffer compact() {
        return this.buffer.compact();
    }

    public byte[] getLast(int num) {
        final int posi = buffer.position();
        num = Math.min(posi, num);
        JDK8BufferHelper.position(buffer, posi - num);
        buffer.position();
        final byte[] b = new byte[num];
        buffer.get(b);
        JDK8BufferHelper.position(buffer, posi);
        return b;
    }

    public byte[] getSub(int start, int end) {
        int posi = buffer.position();
        JDK8BufferHelper.position(buffer, start);
        byte[] b = new byte[end - start];
        buffer.get(b);
        JDK8BufferHelper.position(buffer, posi);
        return b;
    }

    public static PrintStream PrintStreamforDynByteBuffer(int l) {
        final OutputStream buf = OutputStreamforDynByteBuffer(l);
        return new PrintStream(buf) {
            public synchronized String toString() {
                return buf.toString();
            }
        };
    }

    public static OutputStream OutputStreamforDynByteBuffer(int l) {
        final DynByteBuffer buf = new DynByteBuffer(l);
        return new OutputStream() {
            public synchronized void write(int b) throws IOException {
                buf.put((byte) b);
            }

            public synchronized void write(byte[] bytes, int off, int len) throws IOException {
                buf.put(bytes, off, len);
            }

            public synchronized String toString() {
                return buf.toString();
            }
        };
    }
}
