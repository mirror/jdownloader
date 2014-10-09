package org.jdownloader.swt.browser;

public interface JavaScriptEventListener {

    Object onJavaScriptCallback(JWebBrowser jWebBrowser, String functionName, Object[] arguments);

}
