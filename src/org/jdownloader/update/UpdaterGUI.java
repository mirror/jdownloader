package org.jdownloader.update;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import jd.gui.swing.jdgui.JDGui;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.update.updateclient.AppNotFoundException;
import org.appwork.update.updateclient.ParseException;
import org.appwork.update.updateclient.gui.UpdaterCoreGui;
import org.appwork.update.updateclient.http.HTTPIOException;
import org.appwork.update.updateclient.translation.T;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;

public class UpdaterGUI extends JFrame {
    private UpdaterCoreGui panel;
    private JButton        ok;
    private JButton        cancel;
    private JButton        ok2;

    public void setVisible(boolean b) {
        super.setVisible(b);
        setLocation(SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), this));
        ok.setVisible(false);
        ok2.setVisible(false);

    }

    public UpdaterGUI() {
        super("Updater");
        panel = new UpdaterCoreGui(JDUpdater.getInstance());
        panel.expand();
        JDUpdater.getInstance().getEventSender().addListener(panel);
        JDUpdater.getInstance().getStateMachine().addListener(panel);
        addWindowListener(new WindowListener() {

            public void windowActivated(final WindowEvent arg0) {
            }

            public void windowClosed(final WindowEvent arg0) {
            }

            public void windowClosing(final WindowEvent arg0) {
                setVisible(false);
            }

            public void windowDeactivated(final WindowEvent arg0) {
            }

            public void windowDeiconified(final WindowEvent arg0) {

            }

            public void windowIconified(final WindowEvent arg0) {
            }

            public void windowOpened(final WindowEvent arg0) {
            }

        });
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
                if (isVisible()) {
                    toFront();
                    setExtendedState(JFrame.NORMAL);
                }
            }
        });
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // set appicon
        setIconImages(JDGui.getInstance().getMainFrame().getIconImages());

        layoutGUI();

        setLocation(SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), this));

        pack();

    }

    private void layoutGUI() {
        this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill][]"));
        this.add(panel);
        ok = new JButton();
        ok2 = new JButton();
        cancel = new JButton(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.cancel", "Cancel"));
        panel.add(Box.createHorizontalGlue(), "spanx,pushx,split 4,newline");
        panel.add(ok, "hidemode 3,sg bt,tag ok");
        panel.add(ok2, "hidemode 3,sg bt,tag ok");

        panel.add(cancel, "hidemode 3,sg bt,tag cancel");
        ok.setVisible(false);
        ok2.setVisible(false);
    }

    public void reset() {

        panel.reset();
    }

    public boolean installNow() throws AppNotFoundException, HTTPIOException, ParseException {

        final ArrayList<File> installedFiles = JDUpdater.getInstance().getFilesToInstall();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                panel.log(T._.readyToInstallFiles(installedFiles.size()));
                panel.getProgressLogo().setProgress(1.0f);
                if (installedFiles.size() > 0) {
                    panel.getBar().setValue(100);
                    panel.getSubBar().setValue(100);

                    panel.getSubBar().setString(T._.readyToInstallFiles(installedFiles.size()));
                    panel.getBar().setString(T._.UpdateServer_UpdaterGui_runInEDT_successfull());
                } else {
                    panel.getBar().setValue(100);
                    panel.getSubBar().setValue(100);

                    panel.getSubBar().setString(T._.readyToInstallFiles(installedFiles.size()));
                    panel.getBar().setString(T._.UpdateServer_UpdaterGui_runInEDT_finished());
                }
                panel.getBar().setIndeterminate(false);
                panel.getSubBar().setIndeterminate(false);
                ok.setText(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.installnow", "Install now!"));
                ok2.setText(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.installlater", "Install later!"));
                ok.setVisible(true);
                ok2.setVisible(true);
            }
        };
        return false;
    }

}