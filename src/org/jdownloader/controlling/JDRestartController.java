package org.jdownloader.controlling;

import org.appwork.app.gui.copycutpaste.CopyCutPasteHandler;
import org.appwork.shutdown.ShutdownController;
import org.appwork.storage.config.JsonConfig;
import org.appwork.update.inapp.RestartController;
import org.appwork.update.inapp.RestartDirectEvent;
import org.appwork.update.inapp.RestartViaUpdaterEvent;
import org.appwork.update.inapp.SilentUpdaterEvent;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.update.JDUpdater;

public class JDRestartController extends RestartController {
    private static final JDRestartController INSTANCE = new JDRestartController();

    /**
     * get the only existing instance of JDRestartController. This is a singleton
     * 
     * @return
     */
    public static JDRestartController getInstance() {
        return JDRestartController.INSTANCE;
    }

    private boolean silentRestartAllowed = true;

    public void onShutdown(boolean silent) {

        if (!JsonConfig.create(GeneralSettings.class).isSilentUpdateEnabled()) {
            super.onShutdown(silent);
        } else {
            if (JDUpdater.getInstance().hasWaitingUpdates() && !ShutdownController.getInstance().hasShutdownEvent(RestartViaUpdaterEvent.getInstance())) {
                if (JDUpdater.getInstance().getBranch() != null && !JDUpdater.getInstance().isSelfUpdateRequested()) {
                    SilentUpdaterEvent.getInstance().setBootstrappath(JDUpdater.getInstance().getTmpUpdateDirectory().getAbsolutePath());
                }
                if (ShutdownController.getInstance().hasShutdownEvent(RestartDirectEvent.getInstance()) || ShutdownController.getInstance().hasShutdownEvent(RestartViaUpdaterEvent.getInstance())) {
                    restartViaUpdater(true);
                } else {
                    runUpdaterOnAppExit();
                }
            }
        }

    }

    /**
     * Create a new instance of JDRestartController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private JDRestartController() {
        super();

    }

    public void bootstrapRestartASAP() {

        new Thread("Wait For Restart") {
            public void run() {
                while (true) {
                    long sinceStartup = System.currentTimeMillis() - CopyCutPasteHandler.getInstance().getStartupTime();
                    if ((CopyCutPasteHandler.getInstance().getLastMouseEvent() < 0 && sinceStartup > 20000 && silentRestartAllowed) || (CopyCutPasteHandler.getInstance().getLastMouseEvent() > 0 && System.currentTimeMillis() - CopyCutPasteHandler.getInstance().getLastMouseEvent() > 20000 && silentRestartAllowed)) {
                        // wait until there is no mousevent
                        if (JDUpdater.getInstance().getBranch() != null && !JDUpdater.getInstance().isSelfUpdateRequested()) {
                            RestartViaUpdaterEvent.getInstance().setBootstrappath(JDUpdater.getInstance().getTmpUpdateDirectory().getAbsolutePath());
                        }
                        JsonConfig.create(GeneralSettings.class).setSilentRestart(true);
                        setSilentShutDownEnabled(true);
                        restartViaUpdater(false);
                        return;
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

    public void setSilentRestartAllowed(boolean b) {
        silentRestartAllowed = b;
    }

}
