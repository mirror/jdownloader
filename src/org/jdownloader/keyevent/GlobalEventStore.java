package org.jdownloader.keyevent;

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.appwork.swing.event.AWTEventListener;
import org.appwork.swing.event.AWTEventQueueLinker;

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
        AWTEventQueueLinker.getInstance().getEventSender().addListener(this);
    }

    @Override
    public void onAWTEventAfterDispatch(AWTEvent parameter) {
    }

    @Override
    public void onAWTEventBeforeDispatch(AWTEvent parameter) {
        if (parameter instanceof KeyEvent) {
            latestKeyEvent = (KeyEvent) parameter;

        } else if (parameter instanceof MouseEvent) {
            latestMouseEvent = (MouseEvent) parameter;
        }
    }

    public KeyEvent getLatestKeyEvent() {
        return latestKeyEvent;
    }
}
