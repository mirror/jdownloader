package org.jdownloader.keyevent;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class GlobalEventStore implements AWTEventListener {
    private static final GlobalEventStore INSTANCE = new GlobalEventStore();

    /**
     * get the only existing instance of GlobalKeyListener. This is a singleton
     * 
     * @return
     */
    public static GlobalEventStore getInstance() {
        return GlobalEventStore.INSTANCE;
    }

    private KeyEvent   latestKeyEvent;
    private MouseEvent latestMouseEvent;

    public MouseEvent getLatestMouseEvent() {
        return latestMouseEvent;
    }

    /**
     * Create a new instance of GlobalKeyListener. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private GlobalEventStore() {
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);

    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (event instanceof KeyEvent) {
            latestKeyEvent = (KeyEvent) event;

        } else if (event instanceof MouseEvent) {
            latestMouseEvent = (MouseEvent) event;
        }
    }

    public KeyEvent getLatestKeyEvent() {
        return latestKeyEvent;
    }

}
