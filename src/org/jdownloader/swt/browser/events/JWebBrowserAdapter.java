package org.jdownloader.swt.browser.events;

import org.jdownloader.swt.browser.JWebBrowser;

public abstract class JWebBrowserAdapter implements JWebBrowserListener {

    @Override
    public void onInitComplete(JWebBrowser jWebBrowser) {
    }

    @Override
    public void onBrowserWindowRequested(JWebBrowser jWebBrowser, String location) {
    }

    @Override
    public void onUrlChanged(JWebBrowser jWebBrowser, String location) {
    }

    @Override
    public void onUrlChanging(JWebBrowser jWebBrowser, String location) {
    }

    @Override
    public void onLoadingComplete(JWebBrowser jWebBrowser) {
    }

    @Override
    public void onLoading(JWebBrowser jWebBrowser, double d) {
    }

}
