package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.IOException;

import jd.http.Browser;
import jd.http.Request;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.extensions.eventscripter.EnvironmentException;

public class BrowserSandBox {
    private final Browser br;

    public BrowserSandBox() {
        this.br = new Browser();
    }

    private BrowserSandBox(Browser br) {
        if (br != null) {
            this.br = br;
        } else {
            this.br = new Browser();
        }
    }

    @Override
    public int hashCode() {
        if (br != null) {
            return br.hashCode();
        } else {
            return super.hashCode();
        }
    }

    public void setCookie(String host, String key, String value) {
        br.setCookie(host, key, value);
    }

    public String getCookie(String host, String key) {
        return br.getCookie(host, key);
    }

    public void setHeader(String field, String value) {
        br.setHeader(field, value);
    }

    public String getResponseHeader(String field) {
        final Request request = br.getRequest();
        return request != null ? request.getResponseHeader(field) : null;
    }

    public int getResponseCode() {
        return br.getHttpConnection().getResponseCode();
    }

    public String getRequestMethod() {
        return br.getHttpConnection().getRequestMethod().toString();
    }

    public long getContentLength() {
        return br.getHttpConnection().getContentLength();
    }

    public long getRequestTime() {
        return br.getHttpConnection().getRequestTime();
    }

    public String getContentType() {
        return br.getHttpConnection().getContentType();
    }

    public boolean isSSLTrustALL() {
        return br.getHttpConnection().isSSLTrustALL();
    }

    public int getConnectTimeout() {
        return br.getConnectTimeout();
    }

    public void setConnectTimeout(final int connectTimeout) {
        br.setConnectTimeout(connectTimeout);
    }

    public int getReadTimeout() {
        return br.getReadTimeout();
    }

    public void setReadTimeout(final int readTimeout) {
        br.setReadTimeout(readTimeout);
    }

    public void setFollowRedirects(final boolean b) {
        br.setFollowRedirects(b);
    }

    public boolean isFollowingRedirects() {
        return br.isFollowingRedirects();
    }

    public String getRedirectLocation() {
        return br.getRedirectLocation();
    }

    public void setDefaultSSLTrustALL(Boolean defaultSSLTrustALL) {
        br.setDefaultSSLTrustALL(defaultSSLTrustALL);
    }

    public boolean getDefaultSSLTrustALL() {
        return br.getDefaultSSLTrustALL();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BrowserSandBox) {
            return ((BrowserSandBox) obj).br == br;
        } else {
            return super.equals(obj);
        }
    }

    public String getPage(final String url) throws EnvironmentException {
        try {
            return br.getPage(url);
        } catch (IOException e) {
            throw new EnvironmentException(e);
        }
    }

    public BrowserSandBox cloneBrowser() {
        return new BrowserSandBox(br.cloneBrowser());
    }

    public boolean setProxy(final String proxyString) {
        final HTTPProxy proxy = HTTPProxy.parseHTTPProxy(proxyString);
        if (proxy != null) {
            br.setProxy(proxy);
            return true;
        } else {
            return false;
        }
    }

    public String postPage(final String url, final String postData) throws EnvironmentException {
        try {
            return br.postPage(url, postData);
        } catch (IOException e) {
            throw new EnvironmentException(e);
        }
    }

    public String getURL() {
        return br.getURL();
    }

    public String getHTML() {
        return br.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        final Browser br = this.br;
        if (br != null) {
            br.disconnect();
        }
    }
}
