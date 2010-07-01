package jd.http.ext;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;

import jd.http.Browser;
import jd.http.Request;

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
        try {
            if (request == null || request.getHttpConnection() == null) return 403;
            return request.getHttpConnection().getResponseCode();
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
                br.loadConnection(null);
                browser.getBrowserEnviroment().prepareContents(br.getRequest());

                changeReadyState(NetworkRequest.STATE_INTERACTIVE);
                changeReadyState(NetworkRequest.STATE_COMPLETE);

            }

        }

    }

}
