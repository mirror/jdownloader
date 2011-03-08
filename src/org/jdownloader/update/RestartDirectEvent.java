package org.jdownloader.update;

import org.appwork.shutdown.ShutdownEvent;

public class RestartDirectEvent extends ShutdownEvent {
    private static final RestartDirectEvent INSTANCE = new RestartDirectEvent();

    /**
     * get the only existing instance of RestartDirectEvent. This is a singleton
     * 
     * @return
     */
    public static RestartDirectEvent getInstance() {
        return RestartDirectEvent.INSTANCE;
    }

    /**
     * Create a new instance of RestartDirectEvent. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private RestartDirectEvent() {

    }

    @Override
    public void run() {
    }

}
