package jd.http.download;

import java.io.IOException;
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

    public DownloadChunk(long start, long end) {
        this.chunkStart = start;
        this.chunkEnd = end;
    }

    public DownloadChunk() {
        // TODO Auto-generated constructor stub
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

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request orgRequest) {
        this.request = orgRequest;

    }

    public void connect() throws IOException, BrowserException {
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

            br.getHeaders().put("Range", "bytes=" + chunkStart + "-" + this.chunkEnd);
            HTTPConnection con;
            if (connection.getHTTPURLConnection().getDoOutput()) {
                con = br.openPostConnection(connection.getURL() + "", connection.getPostData());
            } else {
                con = br.openGetConnection(connection.getURL() + "");
            }
            if (!con.isOK()) { throw new BrowserException(JDLocale.L("exceptions.browserexception.chunkcopyerror.badrequest", "Unexpected chunkcopy error"));

            }
            if (con.getHeaderField("Location") != null) { throw new BrowserException(JDLocale.L("exceptions.browserexception.redirecterror", "Unexpected chunkcopy error: Redirect"));

            }
            connection = con;
        }

    }

    public void setRange(long start, long end) {
        this.chunkStart = start;
        this.chunkEnd = end;

    }

    public void go()throws Exception {

        this.connect();

    }

}
