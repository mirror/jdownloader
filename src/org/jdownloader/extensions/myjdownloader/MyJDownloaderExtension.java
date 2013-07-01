package org.jdownloader.extensions.myjdownloader;

import jd.plugins.AddonPanel;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.txtresource.TranslateInterface;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class MyJDownloaderExtension extends AbstractExtension<MyDownloaderExtensionConfig, TranslateInterface> implements ShutdownVetoListener {

    private MyJDownloaderConfigPanel   configPanel;
    private MyJDownloaderConnectThread thread = null;

    @Override
    protected void stop() throws StopException {
        MyJDownloaderConnectThread lThread = thread;
        thread = null;

        if (lThread != null && lThread.isAlive()) {
            lThread.disconnect();
        }
    }

    public synchronized void setEnabled(boolean enabled) throws StartException, StopException {
        super.setEnabled(enabled);
    }

    @Override
    public String getIconKey() {
        return "logo/jd_logo_54_54";
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
        ShutdownController.getInstance().addShutdownVetoListener(this);
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

    @Override
    public void onShutdown(ShutdownRequest request) {
        try {
            stop();
        } catch (final Throwable e) {
        }
    }

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

}
