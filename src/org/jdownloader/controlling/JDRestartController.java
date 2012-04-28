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
import org.appwork.utils.swing.EDTRunner;
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

    public void onShutdown(boolean silent) {

        if (!JsonConfig.create(GeneralSettings.class).isSilentUpdateEnabled()) {
            super.onShutdown(silent);
        } else {
            if (JDUpdater.getInstance().hasWaitingUpdates() && !ShutdownController.getInstance().hasShutdownEvent(RestartViaUpdaterEvent.getInstance())) {
                if (JDUpdater.getInstance().getBranch() != null && !JDUpdater.getInstance().isSelfUpdateRequested()) {
                    SilentUpdaterEvent.getInstance().setBootstrappath(JDUpdater.getInstance().getTmpUpdateDirectory().getAbsolutePath());
                }
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

    }

    public void bootstrapRestartASAP() {
        System.out.println(1);
        new Thread("Wait For Restart") {
            public void run() {
                while (true) {
                    if (System.currentTimeMillis() - lastMouseActivity > 20000) {
                        // wait until there is no mousevent
                        if (JDUpdater.getInstance().getBranch() != null && !JDUpdater.getInstance().isSelfUpdateRequested()) {
                            RestartViaUpdaterEvent.getInstance().setBootstrappath(JDUpdater.getInstance().getTmpUpdateDirectory().getAbsolutePath());
                        }
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
            }
        }.start();

    }

    public void initMouseObserver() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {

                    protected void dispatchEvent(AWTEvent event) {
                        if (event != null && event instanceof MouseEvent) {
                            lastMouseActivity = System.currentTimeMillis();

                        }

                        super.dispatchEvent(event);
                    }
                });
            }
        };
    }
}
