package org.jdownloader.swt.browser.djnativeswing.event;

import java.util.EventListener;

import org.jdownloader.swt.browser.djnativeswing.DJWebBrowser;

public interface DJWebBrowserListener extends EventListener {

    void onBrowserWindowRequested(DJWebBrowser DJWebBrowser, String location);

    void onUrlChanged(DJWebBrowser DJWebBrowser, String location);

    void onUrlChanging(DJWebBrowser DJWebBrowser, String location);

    void onLoadingComplete(DJWebBrowser DJWebBrowser);

    void onLoading(DJWebBrowser DJWebBrowser, double d);

    void onJavaScriptCallback(DJWebBrowser DJWebBrowser, String functionName, Object[] arguments);

}