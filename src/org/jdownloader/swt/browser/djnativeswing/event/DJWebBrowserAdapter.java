package org.jdownloader.swt.browser.djnativeswing.event;

import org.jdownloader.swt.browser.djnativeswing.DJWebBrowser;

public abstract class DJWebBrowserAdapter implements DJWebBrowserListener {

    @Override
    public void onBrowserWindowRequested(DJWebBrowser jWebBrowser, String location) {
    }

    @Override
    public void onJavaScriptCallback(DJWebBrowser jWebBrowser, String functionName, Object[] arguments) {
    }

    @Override
    public void onUrlChanged(DJWebBrowser jWebBrowser, String location) {
    }

    @Override
    public void onUrlChanging(DJWebBrowser jWebBrowser, String location) {
    }

    @Override
    public void onLoadingComplete(DJWebBrowser jWebBrowser) {
    }

    @Override
    public void onLoading(DJWebBrowser jWebBrowser, double d) {
    }

}
