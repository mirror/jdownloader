package org.jdownloader.plugins.js;

import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import org.jdownloader.scripting.envjs.EnvJSBrowser;

public class EnvJsBrowserWrapper implements BrowserWrapperInterface {

    private EnvJSBrowser envJs;

    public EnvJsBrowserWrapper(EnvJSBrowser envJs) {
        this.envJs = envJs;
    }

    public void loadPage(final String url) {
        Thread th = new Thread("Load Page") {
            @Override
            public void run() {
                envJs.getPage(url);
            }
        };
        try {
            th.start();
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Object getDocument() {
        return ScriptableObject.getProperty(envJs.getScope(), "document");
    }

}
