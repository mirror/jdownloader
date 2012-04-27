package org.jdownloader.update;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.storage.config.JsonConfig;
import org.appwork.update.inapp.SilentUpdaterEvent;
import org.appwork.update.inapp.UpdaterGUI;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.JDRestartController;
import org.jdownloader.settings.GeneralSettings;

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

    protected void onInstallRequest() {

        if (JsonConfig.create(GeneralSettings.class).isSilentUpdateEnabled()) {
            if (JsonConfig.create(GeneralSettings.class).isSilentUpdateWithRestartEnabled()) {

                JDRestartController.getInstance().bootstrapRestartASAP();

            } else {

                SilentUpdaterEvent.getInstance().setBootstrappath(JDUpdater.getInstance().getTmpUpdateDirectory().getAbsolutePath());

            }
        } else {
            super.onInstallRequest();
        }
    }

    @Override
    protected void updateLocation() {
        setLocation(SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), this));
    }

}