package jd.controlling;

import java.util.logging.Logger;

import jd.http.BrowserSettings;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class BrowserSettingsThread extends Thread implements BrowserSettings {

    private HTTPProxy proxy;
    private boolean   debug;
    private boolean   verbose;
    private Logger    logger;

    public BrowserSettingsThread(Runnable r) {
        super(r);
        copySettings();
    }

    private void copySettings() {
        final Thread currentThread = Thread.currentThread();
        /**
         * use BrowserSettings from current thread if available
         */
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            this.proxy = settings.getCurrentProxy();
            this.debug = settings.isDebug();
            this.verbose = settings.isVerbose();
            this.logger = settings.getLogger();
        }
    }

    public BrowserSettingsThread(Runnable r, String name) {
        super(r, name);
        copySettings();
    }

    public BrowserSettingsThread(String name) {
        super(name);
        copySettings();
    }

    public HTTPProxy getCurrentProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setCurrentProxy(HTTPProxy proxy) {
        this.proxy = proxy;
    }

    public void setDebug(boolean b) {
        this.debug = b;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

}
