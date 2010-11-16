package jd.plugins.optional.remoteserv.test;

import java.io.IOException;
import java.net.URL;

import jd.http.Browser;
import jd.plugins.optional.remoteserv.remotecall.HttpClient;
import jd.plugins.optional.remoteserv.remotecall.client.RemoteCallClient;

public class RemoteCallClientImpl extends RemoteCallClient {
    private static final RemoteCallClientImpl INSTANCE = new RemoteCallClientImpl();

    public static RemoteCallClientImpl getInstance() {
        return RemoteCallClientImpl.INSTANCE;

    }

    private Browser    browser;

    private HttpClient brHttpClient;

    private RemoteCallClientImpl() {
        super("localhost:8080");
        this.browser = new Browser();
        // adapter
        this.brHttpClient = new HttpClient() {

            public String post(final URL url, final String data) throws IOException {
                return RemoteCallClientImpl.this.browser.postPage(url + "", data);
            }

        };

    }

    public HttpClient getHTTPClient() {
        return this.brHttpClient;
    }
}
