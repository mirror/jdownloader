package jd.gui.swing.jdgui.events;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

public class EDTEventQueue extends EventQueue {

    public static void initEventQueue() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTEventQueue());
    }

    private final ContextMenu cm;

    private EDTEventQueue() {
        super();

        cm = new ContextMenu();
    }

    @Override
    protected void dispatchEvent(AWTEvent e) {
        super.dispatchEvent(e);

        if (e instanceof MouseEvent) cm.dispatchMouseEvent((MouseEvent) e);
    }

}
