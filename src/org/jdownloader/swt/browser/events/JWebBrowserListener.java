package org.jdownloader.swt.browser.events;

import java.util.EventListener;

import org.jdownloader.swt.browser.JWebBrowser;

public interface JWebBrowserListener extends EventListener {

    void onInitComplete(JWebBrowser jWebBrowser);

    void onBrowserWindowRequested(JWebBrowser jWebBrowser, String location);

    void onUrlChanged(JWebBrowser jWebBrowser, String location);

    void onUrlChanging(JWebBrowser jWebBrowser, String location);

    void onLoadingComplete(JWebBrowser jWebBrowser);

    void onLoading(JWebBrowser jWebBrowser, double d);

}