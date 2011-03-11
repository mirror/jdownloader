package org.jdownloader.update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.appwork.controlling.StateEvent;
import org.appwork.update.exchange.UpdatePackage;
import org.appwork.update.updateclient.ParseException;
import org.appwork.update.updateclient.UpdaterState;
import org.appwork.update.updateclient.event.UpdaterEvent;
import org.appwork.update.updateclient.event.UpdaterListener;
import org.appwork.update.updateclient.gui.UpdaterCoreGui;
import org.appwork.update.updateclient.http.HTTPIOException;
import org.appwork.update.updateclient.translation.T;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.windowflasher.WindowFlasher;

public class UpdaterGUI extends JFrame implements ActionListener, UpdaterListener {
    private UpdaterCoreGui panel;
    private JButton        ok;
    private JButton        cancel;
    private JButton        ok2;
    private WindowFlasher  flasher;
    private UpdatePackage  updates;

    public void setVisible(boolean b) {
        super.setVisible(b);
        setLocation(SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), this));
        ok.setVisible(false);
        ok2.setVisible(false);

    }

    public void onStateUpdate(StateEvent event) {
    }

    public void onStateChange(StateEvent event) {

        flash();
    }

    public UpdaterGUI() {
        super("Updater");
        panel = new UpdaterCoreGui(JDUpdater.getInstance());
        panel.expand();

        flasher = new WindowFlasher(this);
        JDUpdater.getInstance().getEventSender().addListener(panel);

        JDUpdater.getInstance().getEventSender().addListener(this);
        addWindowListener(new WindowListener() {

            public void windowActivated(final WindowEvent arg0) {
                // sets dialog parent to updateframe if active
                Dialog.getInstance().setParentOwner(UpdaterGUI.this);
            }

            public void windowClosed(final WindowEvent arg0) {
            }

            public void windowClosing(final WindowEvent arg0) {
                cancel();

            }

            public void windowDeactivated(final WindowEvent arg0) {
                // resets dialog root
                Dialog.getInstance().setParentOwner(JDGui.getInstance().getMainFrame());
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
                Dialog.getInstance().setParentOwner(JDGui.getInstance().getMainFrame());
            }
        });
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // set appicon
        setIconImages(JDGui.getInstance().getMainFrame().getIconImages());

        layoutGUI();

        setLocation(SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), this));

        pack();

    }

    protected void cancel() {
        try {
            Dialog.getInstance().showConfirmDialog(0, T._.dialog_rly_cancel());
            JDUpdater.getInstance().interrupt();
            setVisible(false);

        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    private void layoutGUI() {
        this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill][]"));
        this.add(panel);
        ok = new JButton();
        ok2 = new JButton();
        cancel = new JButton(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.cancel", "Cancel"));
        cancel.addActionListener(this);

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

    private void installNow() throws HTTPIOException, ParseException, InterruptedException {
        flash();
        final ArrayList<File> installedFiles = JDUpdater.getInstance().getFilesToInstall();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setVisible(true);
                panel.log(T._.readyToInstallFiles(installedFiles.size()));
                panel.getProgressLogo().setProgress(1.0f);

                panel.getBar().setValue(100);
                panel.getSubBar().setValue(100);

                panel.getSubBar().setString(T._.readyToInstallFiles(installedFiles.size()));
                panel.getBar().setString(T._.udpates_found());

                panel.getBar().setIndeterminate(false);
                panel.getSubBar().setIndeterminate(false);
                ok.setText(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.installnow", "Install now!"));
                ok2.setText(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.installlater", "Install later!"));
                ok.setVisible(true);
                ok2.setVisible(true);
                for (ActionListener al : ok.getActionListeners())
                    ok.removeActionListener(al);
                ok.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        RestartController.getInstance().restartViaUpdater();
                    }
                });

                for (ActionListener al : ok2.getActionListeners())
                    ok2.removeActionListener(al);
                ok2.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        RestartController.getInstance().exitViaUpdater();
                        setVisible(false);
                    }
                });
            }
        };

    }

    public void flash() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // if (isActive()) {
                // toFront();
                // setExtendedState(JFrame.NORMAL);
                // }
                flasher.start();
            }
        };
    }

    private void onException(Throwable e) {
        // panel.onException(e);

    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == cancel) {

            if (JDUpdater.getInstance().isInterrupted() || JDUpdater.getInstance().isFinal() || JDUpdater.getInstance().isBreakPointed()) {
                setVisible(false);
                return;
            }
            cancel();

        }
    }

    public void onUpdaterEvent(UpdaterEvent event) {

        switch (event.getType()) {

        // case EXIT_REQUEST:
        // cancel();
        // break;
        }
    }

    public void onUpdaterModuleEnd(UpdaterEvent event) {
    }

    public void onUpdaterModuleProgress(UpdaterEvent event, int parameter) {
    }

    public void onUpdaterModuleStart(UpdaterEvent event) {
    }

    private void onNoUpdates() {
        cancel.setText(T._.exit());
        // this.panel.onFinished(null);
    }

    private void doDownloadNow(final Runnable downloadNow, final Runnable downloadLater) {

        flash();

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setVisible(true);
                UpdatePackage files;
                try {
                    files = JDUpdater.getInstance().getUpdates();
                } catch (Throwable e) {
                    e.printStackTrace();
                    files = new UpdatePackage();
                }
                panel.log(T._.downloadUpdatesNow(files.size()));
                // panel.getProgressLogo().setProgress(1.0f);

                panel.getBar().setString(T._.readyToDownloadUpdates(files.size()));
                panel.getSubBar().setString(T._.readyToDownloadUpdatesDetailed(files.size()));

                panel.getBar().setIndeterminate(false);
                panel.getSubBar().setIndeterminate(false);
                ok.setText(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.downloadnow", "Download now!"));
                ok2.setText(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.downloadlater", "Download later!"));
                ok.setVisible(true);
                ok2.setVisible(true);
                for (ActionListener al : ok.getActionListeners())
                    ok.removeActionListener(al);
                ok.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new Thread(downloadNow).start();
                    }

                });
                for (ActionListener al : ok2.getActionListeners())
                    ok2.removeActionListener(al);
                ok2.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new Thread(downloadLater).start();
                    }

                });
            }
        };
    }

    public void onStateEnter(UpdaterState state) {
    }

    public void onStateExit(UpdaterState event) {
    }

}