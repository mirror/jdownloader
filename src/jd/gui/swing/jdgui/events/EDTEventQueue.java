package jd.gui.swing.jdgui.events;

import java.awt.Toolkit;

import org.appwork.app.gui.copycutpaste.CopyCutPasteHandler;

public class EDTEventQueue {

    public static void initEventQueue() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new CopyCutPasteHandler());

    }

}
