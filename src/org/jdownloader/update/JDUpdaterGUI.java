package org.jdownloader.update;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.update.inapp.UpdaterGUI;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;

public class JDUpdaterGUI extends UpdaterGUI {

    public JDUpdaterGUI() {
        super(JDUpdater.getInstance());

        JDGui.getInstance().getMainFrame().addWindowListener(new WindowListener() {

            public void windowOpened(WindowEvent e) {
                if (isVisible()) {
                    toFront();
                    setExtendedState(JFrame.NORMAL);
                }
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
                if (isVisible()) {
                    toFront();
                    setExtendedState(JFrame.NORMAL);
                }
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
                Dialog.getInstance().setParentOwner(JDGui.getInstance().getMainFrame());
            }
        });

    }

    @Override
    protected void updateLocation() {
        setLocation(SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), this));
    }

}