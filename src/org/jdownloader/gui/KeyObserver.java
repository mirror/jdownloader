package org.jdownloader.gui;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

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

    private static int CLEAR_OLD_MASK = ~(KeyEvent.SHIFT_MASK & KeyEvent.ALT_MASK & KeyEvent.CTRL_MASK & KeyEvent.META_MASK & KeyEvent.ALT_GRAPH_MASK & KeyEvent.BUTTON1_MASK);

    /**
     * Returns whether or not the Control modifier is down on this event.
     */
    public boolean isControlDown(boolean nootherKey) {
        return isModifierPressed(KeyEvent.CTRL_DOWN_MASK, nootherKey);
    }

    /**
     * Returns whether or not the Meta modifier is down on this event.
     */
    public boolean isMetaDown(boolean nootherKey) {

        return isModifierPressed(KeyEvent.META_DOWN_MASK, nootherKey);
    }

    public boolean isModifierPressed(int modifier, boolean nootherKey) {

        int currentMod = fixModifiers(modifiers);
        modifier = fixModifiers(modifier);
        // currentMod &= CLEAR_OLD_MASK;
        if (nootherKey) {

        return currentMod == modifier; }

        int and = (currentMod & modifier);
        return and == modifier;
    }

    /**
     * @param currentMod
     * @return
     */
    public int fixModifiers(int currentMod) {
        if ((currentMod & KeyEvent.SHIFT_MASK) != 0) {
            currentMod |= KeyEvent.SHIFT_DOWN_MASK;

        }
        if ((currentMod & KeyEvent.ALT_MASK) != 0) {
            currentMod |= KeyEvent.ALT_DOWN_MASK;

        }
        if ((currentMod & KeyEvent.CTRL_MASK) != 0) {
            currentMod |= KeyEvent.CTRL_DOWN_MASK;

        }
        if ((currentMod & KeyEvent.META_MASK) != 0) {
            currentMod |= KeyEvent.META_DOWN_MASK;

        }
        if ((currentMod & KeyEvent.ALT_GRAPH_MASK) != 0) {
            currentMod |= KeyEvent.ALT_GRAPH_DOWN_MASK;

        }
        if ((currentMod & KeyEvent.BUTTON1_MASK) != 0) {
            currentMod |= KeyEvent.BUTTON1_DOWN_MASK;

        }

        if ((currentMod & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            currentMod |= KeyEvent.SHIFT_MASK;
        }
        if ((currentMod & KeyEvent.ALT_DOWN_MASK) != 0) {
            currentMod |= KeyEvent.ALT_MASK;
        }
        if ((currentMod & KeyEvent.CTRL_DOWN_MASK) != 0) {
            currentMod |= KeyEvent.CTRL_MASK;
        }
        if ((currentMod & KeyEvent.META_DOWN_MASK) != 0) {
            currentMod |= KeyEvent.META_MASK;
        }
        if ((currentMod & KeyEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            currentMod |= KeyEvent.ALT_GRAPH_MASK;
        }
        if ((currentMod & KeyEvent.BUTTON1_DOWN_MASK) != 0) {
            currentMod |= KeyEvent.BUTTON1_MASK;
        }
        return currentMod;
    }

    private int      modifiers;
    private KeyEvent kEvent;

    /**
     * Create a new instance of KeyObserver. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private KeyObserver() {
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);

    }

    @Override
    public void eventDispatched(AWTEvent event) {

        kEvent = (KeyEvent) event;
        int modifier = kEvent.getModifiersEx();

        if (modifier != this.modifiers) {
            System.out.println("Modifier: " + KeyStroke.getKeyStroke(0, modifier));
            this.modifiers = modifier;

            GUIEventSender.getInstance().fireEvent(new GUIEvent(this, GUIEvent.Type.KEY_MODIFIER, modifier));

        }

    }

}
