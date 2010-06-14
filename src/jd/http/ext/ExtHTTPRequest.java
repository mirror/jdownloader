package jd.http.ext;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;

import jd.http.Browser;
import jd.http.Request;

import org.appwork.utils.logging.Log;
import org.lobobrowser.html.HttpRequest;
import org.lobobrowser.html.ReadyStateChangeListener;
import org.w3c.dom.Document;

public class ExtHTTPRequest implements HttpRequest {

    private ExtBrowser browser;

    private Browser br;

    public ExtHTTPRequest(ExtBrowser browser) {
        this.browser = browser;
    }

    public void abort() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void addReadyStateChangeListener(ReadyStateChangeListener listener) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public String getAllResponseHeaders() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public int getReadyState() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public byte[] getResponseBytes() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public String getResponseHeader(String headerName) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public Image getResponseImage() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public String getResponseText() {
        return br.getRequest().getHtmlCode();

    }

    public Document getResponseXML() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public int getStatus() {
        try {
            if (br == null) return 403;
            return br.getRequest().getHttpConnection().getResponseCode();
        } catch (IOException e) {
            Log.exception(e);
            return 511;
        }

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

    public void open(String method, String url, boolean asyncFlag) throws IOException {
        Request request = null;
        if (method.equalsIgnoreCase("GET")) {
            br = browser.getCommContext().cloneBrowser();
            request = br.createGetRequest(url);

        } else {
            RuntimeException e = new RuntimeException("Not implemented");
            Log.exception(e);
            throw e;
        }
        if (request != null) {
            if (browser.getUserAgent().doLoadContent(request)) {
                br.openGetConnection(url);
                return;
            }
        }
        br = null;

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

        if (br != null) {
            br.loadConnection(null);
            browser.getUserAgent().prepareContents(br.getRequest());
        }

    }

}
