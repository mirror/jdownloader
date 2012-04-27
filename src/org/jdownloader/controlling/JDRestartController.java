package org.jdownloader.controlling;

import org.appwork.shutdown.ShutdownController;
import org.appwork.storage.config.JsonConfig;
import org.appwork.update.inapp.RestartController;
import org.appwork.update.inapp.RestartViaUpdaterEvent;
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
                runUpdaterOnAppExit();
            }
        }

    }

    /**
     * Create a new instance of JDRestartController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private JDRestartController() {
        super();
    }

    public void bootstrapRestartASAP() {

        new Thread("Wait For Restart") {
            public void run() {
                RestartViaUpdaterEvent.getInstance().setBootstrappath(JDUpdater.getInstance().getTmpUpdateDirectory().getAbsolutePath());
                setSilentShutDownEnabled(true);
                restartViaUpdater(false);
                setSilentShutDownEnabled(false);
                RestartViaUpdaterEvent.getInstance().setBootstrappath(null);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
