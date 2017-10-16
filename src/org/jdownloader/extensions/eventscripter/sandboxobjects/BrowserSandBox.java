package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.IOException;

import jd.http.Browser;

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
