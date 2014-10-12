package org.jdownloader.swt.browser.events;

import org.jdownloader.swt.browser.JWebBrowser;

public interface JavaScriptEventListener {

    Object onJavaScriptCallback(JWebBrowser jWebBrowser, String functionName, Object[] arguments);

}
