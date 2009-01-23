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

package jd.http.download;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.http.Request;
import jd.nutils.jobber.JDRunnable;
import jd.utils.JDLocale;

public class DownloadChunk implements JDRunnable {

    private static final int MAXBUFFER_SIZE = 1024 * 128;
    private static final int TIMEOUT_READ = 20000;
    private static final int TIMEOUT_CONNECT = 20000;
    private static final int SPEEDMETER_INTERVAL = 3000;
    private Request request;
    /**
     * chunkStart. The first Byte that contains to the chunk. border included
     */
    private long chunkStart = 0;
    /**
     * The last Byte that contains to the chunk. Border included
     */
    private long chunkEnd = 0;
    private HTTPConnection connection;
    private InputStream inputStream;
    private long writePosition = 0;

    public long getWritePosition() {
        return this.chunkStart + writePosition;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String toString() {
        return "Chunk [" + this.chunkStart + " - " + this.chunkEnd + "]";
    }

    private ReadableByteChannel channel;
    private boolean connectionRequested = false;
    private HTTPDownload owner;
    private long bytesLoaded = 0;
    private boolean alive;
    private TwoLayerSpeedMeter speedmeter;
    private ByteBuffer buffer;

    public HTTPDownload getOwner() {
        return owner;
    }

    public ReadableByteChannel getChannel() {
        return channel;
    }

    public DownloadChunk(HTTPDownload owner, long start, long end) {
        this.owner = owner;
        this.chunkStart = start;
        this.chunkEnd = end;
        if (start > end) {
            System.out.println("HUHUHU");
            // throw new BrowserException("range error");
        }
        request = owner.getRequest();
    }

    public DownloadChunk(HTTPDownload owner) {
        this.owner = owner;
        request = owner.getRequest();
    }

    public long getChunkStart() {
        return chunkStart;
    }

    public void setChunkStart(long chunkStart) {
        this.chunkStart = chunkStart;
    }

    public long getChunkEnd() {
        return chunkEnd;
    }

    public void setChunkEnd(long chunkEnd) {
        System.out.println(this + " Chunkend to " + chunkEnd);
        this.chunkEnd = chunkEnd;
    }

    public void connect() throws IOException, BrowserException {
        if (connectionRequested) throw new IllegalStateException("Already Connected");
        this.connectionRequested = true;
        if (request.getHttpConnection() == null) {
            request.connect();
            this.connection = request.getHttpConnection();
        } else {
            HTTPConnection connection = request.getHttpConnection();

            Browser br = new Browser();

            br.setDebug(true);
            br.setReadTimeout(request.getReadTimeout());
            br.setConnectTimeout(request.getConnectTimeout());

            Map<String, List<String>> request = connection.getRequestProperties();

            if (request != null) {
                Set<Entry<String, List<String>>> requestEntries = request.entrySet();
                Iterator<Entry<String, List<String>>> it = requestEntries.iterator();
                String value;
                while (it.hasNext()) {
                    Entry<String, List<String>> next = it.next();

                    value = next.getValue().toString();
                    br.getHeaders().put(next.getKey(), value.substring(1, value.length() - 1));
                }
            }
            if (chunkEnd < 0) {
                br.getHeaders().put("Range", "bytes=" + chunkStart + "-");
            } else {
                br.getHeaders().put("Range", "bytes=" + chunkStart + "-" + this.chunkEnd);
            }
            HTTPConnection con;
            if (connection.getHTTPURLConnection().getDoOutput()) {
                con = br.openPostConnection(connection.getURL() + "", connection.getPostData());
            } else {
                con = br.openGetConnection(connection.getURL() + "");
            }
            if (!con.isOK()) { throw new BrowserException(JDLocale.L("exceptions.browserexception.chunkcopyerror.badrequest", "Unexpected chunkcopy error"), BrowserException.TYPE_BADREQUEST);

            }
            if (con.getHeaderField("Location") != null) { throw new BrowserException(JDLocale.L("exceptions.browserexception.redirecterror", "Unexpected chunkcopy error: Redirect"), BrowserException.TYPE_REDIRECT);

            }
            this.connection = con;

            long[] range = this.connection.getRange();

            if (range[0] != this.chunkStart) { throw new BrowserException(JDLocale.L("exceptions.browserexception.rangeerror", "Chunkload error"), BrowserException.TYPE_RANGE);

            }

            if (chunkEnd > 0 && range[1] < this.chunkEnd) { throw new BrowserException(JDLocale.L("exceptions.browserexception.rangeerror", "Chunkload error"), BrowserException.TYPE_RANGE);

            }

        }

        if (!this.connection.isOK()) throw new BrowserException(JDLocale.LF("exceptions.browserexception.badrequest", "Bad Request: %s(%s)", connection.getHTTPURLConnection().getResponseMessage(), connection.getResponseCode() + ""), BrowserException.TYPE_BADREQUEST);

        connection.setReadTimeout(TIMEOUT_READ);
        connection.setConnectTimeout(TIMEOUT_CONNECT);
        inputStream = connection.getInputStream();
        channel = Channels.newChannel(inputStream);

    }

    public boolean isConnectionRequested() {
        return connectionRequested;
    }

    public void setRange(long start, long end) {
        this.chunkStart = start;
        this.chunkEnd = end;

    }

    public long getChunkBytes() {
        return bytesLoaded;
    }

    public long getRemainingChunkBytes() {
        long end = chunkEnd;
        if (end < 0) {
            end = owner.getFileSize() - 1;
            System.out.println("FIlesize: " + end);
        }
        return Math.max(0, end - this.chunkStart - bytesLoaded + 1);

    }

    public void go() throws Exception {
        alive = true;
        this.speedmeter = new TwoLayerSpeedMeter(SPEEDMETER_INTERVAL);
        try {
            if (!this.isConnected()) this.connect();
            download();
            System.out.println("F2 " + this);
        } finally {
            System.out.println("F3 " + this);
            this.setChunkEnd(this.getWritePosition() - 1);

            alive = false;
        }

    }

    public boolean isAlive() {
        return alive;
    }

    private void download() throws Exception, InterruptedException {
        this.bytesLoaded = 0l;
        buffer = ByteBuffer.allocateDirect(MAXBUFFER_SIZE);
        ByteBuffer miniBuffer = ByteBuffer.allocateDirect(1024 * 10);
        miniBuffer.clear();
        long loadUntil = 0;
        int miniRead = 0;
        long limit = -1;

        buffer.clear();

        try {

            main: while (true) {

                if (this.getChunkEnd() > 0 && this.getWritePosition() + buffer.limit() > this.getChunkEnd()) {
                    try {
                        buffer.limit((int) (this.getChunkEnd() - this.getWritePosition() + 1));
                    } catch (Exception e) {

                    }
                }
                loadUntil = System.currentTimeMillis() + 250;
                limit = owner.getChunkBandwidth(this);
                if (limit > 0) {
                    // System.out.println("limi " + limit);
                    limit /= 4;
                    limit = Math.max(1, limit);

                    limit = Math.min(buffer.capacity(), limit);

                    if (limit > buffer.capacity()) {
                        HTTPDownload.debug("Buffer auf " + (limit * 1.1));
                        buffer = ByteBuffer.allocateDirect((int) (limit * 1.1));
                    }

                    buffer.limit((int) limit);
                }
                while (buffer.hasRemaining() && System.currentTimeMillis() < loadUntil) {
                    miniBuffer.clear();
                    if (miniBuffer.remaining() > buffer.remaining()) {
                        miniBuffer.limit(buffer.remaining());
                    }
                    try {
                        miniRead = this.channel.read(miniBuffer);
                    } finally {
                        miniBuffer.flip();
                        buffer.put(miniBuffer);
                        if (miniRead > 0) bytesLoaded += miniRead;
                    }
                    if (miniRead == -1) {
                        if (buffer.position() == 0) break main;
                        break;
                    }
                    synchronized (speedmeter) {
                        speedmeter.update(miniRead);
                    }
                    // System.out.println("Speed " + this + "  : " +
                    // speedmeter.getSpeed());
                    // owner.updateSpeed(miniRead);
                    // addPartBytes(miniblock);
                    // addToTotalLinkBytesLoaded(miniblock);
                    // addChunkBytesLoaded(miniblock);

                }

                // deltaTime = Math.max(System.currentTimeMillis() - startTime,
                // 1);
                
                //Hier werden alle CHunks mit dem writer synchronisiert. Die Schreibfunktionen kann jeweils nur 1 Chunk betreten. Alle anderen Warten,
                System.out.println("write " + this + " " + buffer.position());
                //owner.setChunkToWrite(this);
                HDWriter.getWriter().writeAndWait(this.getBuffer(),this.owner.getOutputChannel(),this.getWritePosition());
               // owner.waitForWriter(this);
                System.out.println("Buffer written.. continue " + this);

                // owner.writeBytes(this, buffer);
                this.writePosition += buffer.limit();
                buffer.clear();
                if (miniRead == -1) break main;
                if (this.getChunkEnd() > 0 && this.getWritePosition() > this.getChunkEnd()) {
                    System.out.println("Overhead interrupt " + (this.getChunkEnd() - this.getWritePosition() + 1));

                    return;
                }
                long restWait = (loadUntil - System.currentTimeMillis());
                if (limit <= 0 && restWait > 100) {

                    HTTPDownload.debug("Buffer auf " + (buffer.capacity() * 1.5));
                    buffer = ByteBuffer.allocateDirect((int) (buffer.capacity() * 1.5));
                }
                if (limit > 0 && restWait > 0) Thread.sleep(restWait);
            }

            miniRead = 0;
        } finally {
            System.out.println("F1 " + this);
            if (buffer.position() > 0) {
                try {
                    owner.setChunkToWrite(this);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.writePosition += buffer.limit();
                buffer.clear();
            }

            this.disconnect();

        }
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public long getSpeed() {
        if (speedmeter == null || !this.isAlive()) return 0;
        return this.speedmeter.getSpeed();
    }

    private void disconnect() throws IOException {
        System.out.println(this + " CLOSE & Disconnect");
        channel.close();
        this.inputStream.close();
        connection.disconnect();

    }

    public boolean isConnected() {

        return connection != null;
    }

    public void resetSpeedMeter() {

        this.speedmeter = new TwoLayerSpeedMeter(SPEEDMETER_INTERVAL);

    }

}
