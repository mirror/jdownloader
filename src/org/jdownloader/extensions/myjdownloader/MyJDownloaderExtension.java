package org.jdownloader.extensions.myjdownloader;

import jd.plugins.AddonPanel;

import org.appwork.txtresource.TranslateInterface;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class MyJDownloaderExtension extends AbstractExtension<MyDownloaderExtensionConfig, TranslateInterface> {

    private MyJDownloaderConfigPanel   configPanel;
    private MyJDownloaderConnectThread thread = null;

    @Override
    protected void stop() throws StopException {
        MyJDownloaderConnectThread lThread = thread;
        thread = null;

        if (lThread != null && lThread.isAlive()) lThread.interruptConnectThread();
    }

    public synchronized void setEnabled(boolean enabled) throws StartException, StopException {
        super.setEnabled(enabled);
    }

    @Override
    protected void start() throws StartException {
        thread = new MyJDownloaderConnectThread(this);
        thread.start();
    }

    protected MyJDownloaderConnectThread getConnectThread() {
        return thread;
    }

    @Override
    protected void initExtension() throws StartException {
        setTitle("my.jdownloader.org");

        configPanel = new MyJDownloaderConfigPanel(this, getSettings());
    }

    public LogSource getLogger() {
        return logger;
    }

    @Override
    public ExtensionConfigPanel<?> getConfigPanel() {
        return configPanel;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getDescription() {
        return "my.jdownloader.org";
    }

    @Override
    public AddonPanel<? extends AbstractExtension<MyDownloaderExtensionConfig, TranslateInterface>> getGUI() {
        return null;
    }

}
