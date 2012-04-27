package org.jdownloader.controlling;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import org.appwork.shutdown.ShutdownController;
import org.appwork.storage.config.JsonConfig;
import org.appwork.update.inapp.RestartController;
import org.appwork.update.inapp.RestartViaUpdaterEvent;
import org.appwork.update.inapp.SilentUpdaterEvent;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.update.JDUpdater;

public class JDRestartController extends RestartController {
    private static final JDRestartController INSTANCE = new JDRestartController();

    /**
     * get the only existing instance of JDRestartController. This is a
     * singleton
     * 
     * @return
     */
    public static JDRestartController getInstance() {
        return JDRestartController.INSTANCE;
    }

    public void onShutdown() {

        if (!JsonConfig.create(GeneralSettings.class).isSilentUpdateEnabled()) {
            super.onShutdown();
        } else {
            if (JDUpdater.getInstance().hasWaitingUpdates() && !ShutdownController.getInstance().hasShutdownEvent(RestartViaUpdaterEvent.getInstance())) {
                SilentUpdaterEvent.getInstance().setBootstrappath(JDUpdater.getInstance().getTmpUpdateDirectory().getAbsolutePath());
                runUpdaterOnAppExit();
            }
        }

    }

    private volatile long lastMouseActivity;

    /**
     * Create a new instance of JDRestartController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private JDRestartController() {
        super();
        lastMouseActivity = System.currentTimeMillis();
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {

            protected void dispatchEvent(AWTEvent event) {
                if (event != null && event instanceof MouseEvent) {
                    lastMouseActivity = System.currentTimeMillis();
                }
                super.dispatchEvent(event);
            }
        });
    }

    public void bootstrapRestartASAP() {

        new Thread("Wait For Restart") {
            public void run() {
                if (System.currentTimeMillis() - lastMouseActivity > 20000) {
                    // wait until there is no mousevent
                    RestartViaUpdaterEvent.getInstance().setBootstrappath(JDUpdater.getInstance().getTmpUpdateDirectory().getAbsolutePath());
                    setSilentShutDownEnabled(true);
                    restartViaUpdater(false);
                    // setSilentShutDownEnabled(false);
                    // RestartViaUpdaterEvent.getInstance().setBootstrappath(null);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
