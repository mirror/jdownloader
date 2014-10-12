package org.jdownloader.swt.browser.djnativeswing.event;

import org.jdownloader.swt.browser.djnativeswing.DJWebBrowser;

public interface JavaScriptEventListener {

    Object onJavaScriptCallback(DJWebBrowser jWebBrowser, String functionName, Object[] arguments);

}
