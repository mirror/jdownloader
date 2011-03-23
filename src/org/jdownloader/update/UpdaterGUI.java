package org.jdownloader.update;

import java.awt.Image;
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
import net.miginfocom.swing.MigLayout;

import org.appwork.controlling.StateEvent;
import org.appwork.update.exchange.UpdatePackage;
import org.appwork.update.updateclient.UpdaterState;
import org.appwork.update.updateclient.event.UpdaterEvent;
import org.appwork.update.updateclient.event.UpdaterListener;
import org.appwork.update.updateclient.gui.UpdaterCoreGui;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.locale.APPWORKUTILS;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.windowflasher.WindowFlasher;
import org.jdownloader.update.gui.JDStandaloneUpdaterGui;
import org.jdownloader.update.translate.T;

public class UpdaterGUI extends JFrame implements ActionListener, UpdaterListener {

    private static final long serialVersionUID = -8479103857143982495L;
    private UpdaterCoreGui    panel;
    private JButton           ok;
    private JButton           cancel;
    private JButton           ok2;
    private WindowFlasher     flasher;
    private UpdatePackage     updates;

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
        final ArrayList<Image> list = new ArrayList<Image>();

        list.add(IconIO.getImage(JDStandaloneUpdaterGui.class.getResource("resource/updaterIcon128.png")));
        list.add(IconIO.getImage(JDStandaloneUpdaterGui.class.getResource("resource/updaterIcon64.png")));
        list.add(IconIO.getImage(JDStandaloneUpdaterGui.class.getResource("resource/updaterIcon32.png")));
        list.add(IconIO.getImage(JDStandaloneUpdaterGui.class.getResource("resource/updaterIcon16.png")));
        setIconImages(list);
        layoutGUI();

        setLocation(SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), this));

        pack();

    }

    protected void cancel() {
        try {
            if (JDUpdater.getInstance().isBreakPointed() || JDUpdater.getInstance().isFinal()) {
                setVisible(false);
                return;
            }
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
        cancel = new JButton();
        cancel.addActionListener(this);

        panel.add(Box.createHorizontalGlue(), "spanx,pushx,split 4,newline");
        panel.add(ok, "hidemode 3,sg bt,tag ok");
        panel.add(ok2, "hidemode 3,sg bt,tag ok");

        panel.add(cancel, "hidemode 3,sg bt,tag cancel");
        ok.setVisible(false);
        ok2.setVisible(false);

    }

    public void reset() {
        cancel.setText(APPWORKUTILS.T.ABSTRACTDIALOG_BUTTON_CANCEL());
        panel.reset();
        setLocation(SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), this));
        ok.setVisible(false);
        ok2.setVisible(false);
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

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                cancel.setText(T._.exit());
            }
        };
        // this.panel.onFinished(null);
    }

    // private void doDownloadNow(final Runnable downloadNow, final Runnable
    // downloadLater) {
    //
    // flash();
    //
    // new EDTRunner() {
    //
    // @Override
    // protected void runInEDT() {
    // setVisible(true);
    // UpdatePackage files;
    // try {
    // files = JDUpdater.getInstance().getUpdates();
    // } catch (Throwable e) {
    // e.printStackTrace();
    // files = new UpdatePackage();
    // }
    // panel.log(T._.downloadUpdatesNow(files.size()));
    // // panel.getProgressLogo().setProgress(1.0f);
    //
    // panel.getBar().setString(T._.readyToDownloadUpdates(files.size()));
    // panel.getSubBar().setString(T._.readyToDownloadUpdatesDetailed(files.size()));
    //
    // panel.getBar().setIndeterminate(false);
    // panel.getSubBar().setIndeterminate(false);
    // ok.setText(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.downloadnow",
    // "Download now!"));
    // ok2.setText(JDL.L("org.jdownloader.update.UpdaterGUI.layoutGUI.downloadlater",
    // "Download later!"));
    // ok.setVisible(true);
    // ok2.setVisible(true);
    // for (ActionListener al : ok.getActionListeners())
    // ok.removeActionListener(al);
    // ok.addActionListener(new ActionListener() {
    //
    // public void actionPerformed(ActionEvent e) {
    // new Thread(downloadNow).start();
    // }
    //
    // });
    // for (ActionListener al : ok2.getActionListeners())
    // ok2.removeActionListener(al);
    // ok2.addActionListener(new ActionListener() {
    //
    // public void actionPerformed(ActionEvent e) {
    // new Thread(downloadLater).start();
    // }
    //
    // });
    // }
    // };
    // }

    public void onStateEnter(UpdaterState state) {

    }

    public void onStateExit(UpdaterState state) {
        System.out.println("        ::: " + state);
        if (JDUpdater.getInstance().isBreakPointed()) {
            // downloaded updates
            onInstallRequest();
        } else if (JDUpdater.getInstance().isFinal() && !JDUpdater.getInstance().isFailed()) {
            // error or done
            System.out.println("done");
            final ArrayList<File> installedFiles = JDUpdater.getInstance().getFilesToInstall();
            if (installedFiles.size() > 0 || JDUpdater.getInstance().getFilesToRemove().size() > 0) {
                onInstallRequest();
            } else {
                onNoUpdates();
            }

        }
    }

    private void onInstallRequest() {

        flash();
        final ArrayList<File> installedFiles = JDUpdater.getInstance().getFilesToInstall();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // setVisible(true);
                panel.log(T._.updates_are_ready_for_install_now(installedFiles.size() + JDUpdater.getInstance().getFilesToRemove().size()));
                panel.getProgressLogo().setProgress(1.0f);
                cancel.setText(T._.exit());
                panel.getBar().setValue(100);
                panel.getSubBar().setValue(100);

                panel.getSubBar().setString(T._.updates_ready_for_install(installedFiles.size() + JDUpdater.getInstance().getFilesToRemove().size()));
                panel.getBar().setString(T._.udpates_found());

                panel.getBar().setIndeterminate(false);
                panel.getSubBar().setIndeterminate(false);
                ok.setText(T._.install_updates_now());
                ok2.setText(T._.install_updates_later());
                ok.setVisible(true);
                ok2.setVisible(true);
                for (ActionListener al : ok.getActionListeners()) {
                    ok.removeActionListener(al);
                }
                ok.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        RestartController.getInstance().restartViaUpdater();
                        setVisible(false);
                    }
                });

                for (ActionListener al : ok2.getActionListeners()) {
                    ok2.removeActionListener(al);
                }
                ok2.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        RestartController.getInstance().exitViaUpdater();
                        setVisible(false);
                    }
                });
            }
        };
    }

}