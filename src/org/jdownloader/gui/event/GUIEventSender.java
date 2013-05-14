package org.jdownloader.gui.event;

import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.utils.event.Eventsender;

public class GUIEventSender extends Eventsender<GUIListener, GUIEvent> {
    private static final GUIEventSender INSTANCE = new GUIEventSender();

    /**
     * get the only existing instance of GUIEventSender. This is a singleton
     * 
     * @return
     */
    public static GUIEventSender getInstance() {
        return GUIEventSender.INSTANCE;
    }

    /**
     * Create a new instance of GUIEventSender. This is a singleton class. Access the only existing instance by using {@link #getInstance()}
     * .
     */
    private GUIEventSender() {

    }

    @Override
    protected void fireEvent(GUIListener listener, GUIEvent event) {
        switch (event.getType()) {
        case TAB_SWITCH:
            listener.onGuiMainTabSwitch((View) event.getParameter(0), (View) event.getParameter(1));
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}