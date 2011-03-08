package org.jdownloader.update;

import org.appwork.shutdown.ShutdownController;

public class RestartController {
    private static final RestartController INSTANCE = new RestartController();

    /**
     * get the only existing instance of RestartController. This is a singleton
     * 
     * @return
     */
    public static RestartController getInstance() {
        return RestartController.INSTANCE;
    }

    /**
     * Create a new instance of RestartController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private RestartController() {

    }

    public void restartViaUpdater() {

        ShutdownController.getInstance().removeShutdownEvent(SilentUpdaterEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartDirectEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartViaUpdaterEvent.getInstance());
        ShutdownController.getInstance().addShutdownEvent(RestartViaUpdaterEvent.getInstance());
        ShutdownController.getInstance().requestShutdown();
    }

    public void exitViaUpdater() {

        ShutdownController.getInstance().removeShutdownEvent(SilentUpdaterEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartDirectEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartViaUpdaterEvent.getInstance());
        ShutdownController.getInstance().addShutdownEvent(SilentUpdaterEvent.getInstance());

        // ShutdownController.getInstance().requestShutdown();

    }
}
