package org.jdownloader.gui;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;

import org.jdownloader.gui.event.GUIEvent;
import org.jdownloader.gui.event.GUIEventSender;

public class KeyObserver implements AWTEventListener {
    private static final KeyObserver INSTANCE = new KeyObserver();

    /**
     * get the only existing instance of KeyObserver. This is a singleton
     * 
     * @return
     */
    public static KeyObserver getInstance() {
        return KeyObserver.INSTANCE;
    }

    /**
     * Returns whether or not the Shift modifier is down on this event.
     */
    public boolean isShiftDown() {
        return (modifiers & KeyEvent.SHIFT_MASK) != 0;
    }

    /**
     * Returns whether or not the Control modifier is down on this event.
     */
    public boolean isControlDown() {
        return (modifiers & KeyEvent.CTRL_MASK) != 0;
    }

    /**
     * Returns whether or not the Meta modifier is down on this event.
     */
    public boolean isMetaDown() {
        return (modifiers & KeyEvent.META_MASK) != 0;
    }

    /**
     * Returns whether or not the Alt modifier is down on this event.
     */
    public boolean isAltDown() {
        return (modifiers & KeyEvent.ALT_MASK) != 0;
    }

    /**
     * Returns whether or not the AltGraph modifier is down on this event.
     */
    public boolean isAltGraphDown() {
        return (modifiers & KeyEvent.ALT_GRAPH_MASK) != 0;
    }

    private int modifiers;

    /**
     * Create a new instance of KeyObserver. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private KeyObserver() {
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);

    }

    @Override
    public void eventDispatched(AWTEvent event) {

        KeyEvent kEvent = (KeyEvent) event;
        int modifier = kEvent.getModifiers();
        if (modifier != this.modifiers) {
            this.modifiers = modifier;

            GUIEventSender.getInstance().fireEvent(new GUIEvent(this, GUIEvent.Type.KEY_MODIFIER, modifier));

        }

    }
}
