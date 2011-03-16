package jd.http.ext;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;

import org.appwork.utils.logging.Log;
import org.lobobrowser.html.HttpRequest;
import org.lobobrowser.html.ReadyStateChangeListener;
import org.w3c.dom.Document;

public class ExtHTTPRequest implements HttpRequest {

    private ExtBrowser browser;

    private ArrayList<ReadyStateChangeListener> listener;

    private int readyState = NetworkRequest.STATE_UNINITIALIZED;

    private Request request;

    private boolean asyncFlag;

    private Browser br;

    public ExtHTTPRequest(ExtBrowser browser) {
        this.browser = browser;
        listener = new ArrayList<ReadyStateChangeListener>();
    }

    public void abort() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void addReadyStateChangeListener(ReadyStateChangeListener listener) {
        this.listener.add(listener);

    }

    public String getAllResponseHeaders() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    private void changeReadyState(int newState) {
        boolean dif = newState > 0 && newState != readyState;
        if (newState > 0) {
            this.readyState = newState;
        }
        if (dif) {
            for (ReadyStateChangeListener l : listener) {
                l.readyStateChanged();
            }
        }

    }

    public int getReadyState() {

        return readyState;

    }

    public byte[] getResponseBytes() {
        return request.getResponseBytes();

    }

    public String getResponseHeader(String headerName) {
        return request.getResponseHeader(headerName);

    }

    public Image getResponseImage() {
        return request.getResponseImage();

    }

    public String getResponseText() {
        try {
            return request.getHtmlCode();
        } catch (CharacterCodingException e) {
            Log.exception(e);
            return null;
        }

    }

    public Document getResponseXML() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public int getStatus() {
        if (request == null || request.getHttpConnection() == null) return 403;
        return request.getHttpConnection().getResponseCode();
    }

    public String getStatusText() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void open(String method, String url) throws IOException {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void open(String method, URL url) throws IOException {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void open(String method, URL url, boolean asyncFlag) throws IOException {
        open(method, url + "", asyncFlag);

    }

    public void open(final String method, final String url, final boolean asyncFlag) throws IOException {
        // TODO use threadpool for asynch

        this.asyncFlag = asyncFlag;
        if (method.equalsIgnoreCase("GET")) {
            br = browser.getCommContext().cloneBrowser();
            request = br.createGetRequest(url);

        } else {
            RuntimeException e = new RuntimeException("Not implemented");
            Log.exception(e);
            throw e;
        }

        this.changeReadyState(NetworkRequest.STATE_LOADING);

    }

    public void open(String method, URL url, boolean asyncFlag, String userName) throws IOException {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void open(String method, URL url, boolean asyncFlag, String userName, String password) throws IOException {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void send(String content) throws IOException {
        if (browser.getBrowserEnviroment().doLoadContent(request)) {
            if (asyncFlag) {
                // use pool
                new Thread("Asynchloader") {
                    public void run() {
                        try {
                            br.openRequestConnection(request);

                            br.loadConnection(null);
                            browser.getBrowserEnviroment().prepareContents(br.getRequest());
                            changeReadyState(NetworkRequest.STATE_LOADED);
                            changeReadyState(NetworkRequest.STATE_INTERACTIVE);
                            changeReadyState(NetworkRequest.STATE_COMPLETE);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                }.start();
            } else {
                br.openRequestConnection(request);

                changeReadyState(NetworkRequest.STATE_LOADED);

                String htmlCode = read(request.getHttpConnection());
                request.setHtmlCode(htmlCode);
                browser.getBrowserEnviroment().prepareContents(br.getRequest());

                changeReadyState(NetworkRequest.STATE_INTERACTIVE);
                changeReadyState(NetworkRequest.STATE_COMPLETE);

            }

        }

    }

    public static String read(URLConnectionAdapter con) throws IOException {
        BufferedReader rd;
        InputStreamReader isr;
        InputStream is = null;
        if (con.getHeaderField("Content-Encoding") != null && con.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            if (con.getInputStream() != null) is = new GZIPInputStream(con.getInputStream());
        } else {
            if (con.getInputStream() != null) is = con.getInputStream();
        }
        if (is == null) return null;
        String cs = con.getCharset();
        if (cs == null) {
            /* default encoding ist ISO-8859-1, falls nicht anders angegeben */
            isr = new InputStreamReader(is, "ISO-8859-1");
        } else {
            cs = cs.toUpperCase();
            try {
                isr = new InputStreamReader(is, cs);
            } catch (Exception e) {
                // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,
                // "Could not Handle Charset " + cs, e);
                try {
                    isr = new InputStreamReader(is, cs.replace("-", ""));
                } catch (Exception e2) {
                    // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,
                    // "Could not Handle Charset " + cs, e);
                    isr = new InputStreamReader(is);
                }
            }
        }
        rd = new BufferedReader(isr);
        String line;
        StringBuilder htmlCode = new StringBuilder();
        /* workaround for premature eof */
        try {
            while ((line = rd.readLine()) != null) {
                htmlCode.append(line + "\r\n");
            }
        } catch (EOFException e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Try workaround for ", e);
        } catch (IOException e) {
            if (e.toString().contains("end of ZLIB") || e.toString().contains("Premature")) {
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Try workaround for ", e);
            } else
                throw e;
        } finally {
            try {
                rd.close();
            } catch (Exception e) {
            }
        }
        return htmlCode.toString();
    }

}
