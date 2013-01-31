package org.jdownloader.controlling;

import org.jdownloader.updatev2.RestartController;

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

    // private boolean silentRestartAllowed = true;

    // public void onShutdown(boolean silent) {
    //
    // if (!JsonConfig.create(GeneralSettings.class).isSilentUpdateEnabled()) {
    // super.onShutdown(silent);
    // } else {
    // if (UpdateController.getInstance().hasWaitingUpdates() &&
    // !ShutdownController.getInstance().hasShutdownEvent(RestartViaUpdaterEvent.getInstance())) {
    // // if (UpdateController.getInstance().getBranch() != null && !UpdateController.getInstance().isSelfUpdateRequested()) {
    // // SilentUpdaterEvent.getInstance().setBootstrappath(UpdateController.getInstance().getTmpUpdateDirectory().getAbsolutePath());
    // // }
    // if (ShutdownController.getInstance().hasShutdownEvent(RestartDirectEvent.getInstance()) ||
    // ShutdownController.getInstance().hasShutdownEvent(RestartViaUpdaterEvent.getInstance())) {
    // restartViaUpdater(true);
    // } else {
    // runUpdaterOnAppExit();
    // }
    // }
    // }
    //
    // }

    /**
     * Create a new instance of JDRestartController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private JDRestartController() {
        super();

    }

    public void directAsynchRestart(final String... strings) {
        new Thread("restarter") {
            public void run() {
                directRestart(strings);
            }
        }.start();
    }

    // public void bootstrapRestartASAP() {
    //
    // new Thread("Wait For Restart") {
    // public void run() {
    // while (true) {
    // long sinceStartup = System.currentTimeMillis() - CopyCutPasteHandler.getInstance().getStartupTime();
    // if ((CopyCutPasteHandler.getInstance().getLastMouseEvent() < 0 && sinceStartup > 20000 && silentRestartAllowed) ||
    // (CopyCutPasteHandler.getInstance().getLastMouseEvent() > 0 && System.currentTimeMillis() -
    // CopyCutPasteHandler.getInstance().getLastMouseEvent() > 20000 && silentRestartAllowed)) {
    // // wait until there is no mousevent
    // // if (UpdateController.getInstance().getBranch() != null &&
    // // !UpdateController.getInstance().isSelfUpdateRequested()) {
    // // RestartViaUpdaterEvent.getInstance().setBootstrappath(UpdateController.getInstance().getTmpUpdateDirectory().getAbsolutePath());
    // // }
    // JsonConfig.create(GeneralSettings.class).setSilentRestart(true);
    // setSilentShutDownEnabled(true);
    // restartViaUpdater(false);
    // return;
    // // setSilentShutDownEnabled(false);
    // // RestartViaUpdaterEvent.getInstance().setBootstrappath(null);
    // }
    // try {
    // Thread.sleep(5000);
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }
    // }
    // }
    // }.start();
    //
    // }
    //
    // public void setSilentRestartAllowed(boolean b) {
    // silentRestartAllowed = b;
    // }

}
