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
import jd.utils.JDLocale;
import jd.utils.jobber.JDRunnable;

public class DownloadChunk implements JDRunnable {

    private static final int MAXBUFFER_SIZE = 1024 * 128;
    private static final int TIMEOUT_READ = 20000;
    private static final int TIMEOUT_CONNECT = 20000;
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

    public HTTPDownload getOwner() {
        return owner;
    }

    public ReadableByteChannel getChannel() {
        return channel;
    }

    public DownloadChunk(HTTPDownload owner, long start, long end)  {
        this.owner = owner;
        this.chunkStart = start;
        this.chunkEnd = end;
        if(start>end){
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

    public void setRange(long start, long end) {
        this.chunkStart = start;
        this.chunkEnd = end;

    }

    public long getChunkBytes() {
        return bytesLoaded;
    }

    public long getRemainingChunkBytes() {
        long end = chunkEnd;
        if (end < 0){
            end = owner.getFileSize() - 1;
            System.out.println("FIlesize: "+end);
        }
        return Math.max(0, end - this.chunkStart - bytesLoaded + 1);

    }

    public void go() throws Exception {
        if (!this.isConnected()) this.connect();
        download();

    }

    private void download() throws IOException {
        this.bytesLoaded = 0l;
        ByteBuffer buffer = ByteBuffer.allocateDirect(MAXBUFFER_SIZE);
        ByteBuffer miniBuffer = ByteBuffer.allocateDirect(1024 * 10);
        miniBuffer.clear();
        long loadUntil = 0;
        int miniRead = 0;

        long startTime = 0;
        buffer.clear();

        main: while (true) {
            startTime = System.currentTimeMillis();
            loadUntil = getNextWriteTime();
            if (this.getChunkEnd() > 0 && this.getWritePosition() + buffer.limit() > this.getChunkEnd()) {
                try {
                    buffer.limit((int) (this.getChunkEnd() - this.getWritePosition() + 1));
                } catch (Exception e) {

                }
            }
            while (buffer.hasRemaining() && System.currentTimeMillis() < loadUntil) {
                miniBuffer.clear();
                if (miniBuffer.remaining() > buffer.remaining()) {
                    miniBuffer.limit(buffer.remaining());
                }
                miniRead = this.channel.read(miniBuffer);
                miniBuffer.flip();
                buffer.put(miniBuffer);

                if (miniRead == -1) {
                    if (buffer.position() == 0) break main;
                    break;
                }
                bytesLoaded += miniRead;
                // addPartBytes(miniblock);
                // addToTotalLinkBytesLoaded(miniblock);
                // addChunkBytesLoaded(miniblock);

            }

            // deltaTime = Math.max(System.currentTimeMillis() - startTime, 1);

            owner.writeBytes(this, buffer);
            this.writePosition += buffer.limit();
            buffer.clear();
            if (miniRead == -1) break main;
            if (this.getChunkEnd() > 0 && this.getWritePosition() > this.getChunkEnd()) {
                System.out.println("Overhead interrupt " + (this.getChunkEnd() - this.getWritePosition() + 1));
                connection.disconnect();
                return;
            }
        }

        miniRead = 0;
    }

    private long getNextWriteTime() {
        // TODO Auto-generated method stub
        return System.currentTimeMillis() + 1000;
    }

    public boolean isConnected() {

        return connection != null;
    }

}
