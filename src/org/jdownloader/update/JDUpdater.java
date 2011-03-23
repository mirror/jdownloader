package org.jdownloader.update;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.swing.JFrame;

import jd.JDInitFlags;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.update.exchange.UpdatePackage;
import org.appwork.update.updateclient.InstalledFile;
import org.appwork.update.updateclient.Updater;
import org.appwork.update.updateclient.UpdaterState;
import org.appwork.update.updateclient.event.UpdaterEvent;
import org.appwork.update.updateclient.event.UpdaterListener;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.update.gui.UpdateFoundDialog;

public class JDUpdater extends Updater implements Runnable, ControlListener {
    private static final JDUpdater INSTANCE = new JDUpdater();

    /**
     * get the only existing instance of JDUpdater. This is a singleton
     * 
     * @return
     */
    public static JDUpdater getInstance() {
        return JDUpdater.INSTANCE;
    }

    public static final String    PARAM_BRANCH    = "BRANCH";
    public static final String    BRANCHINUSE     = "BRANCHINUSE";
    protected static final String UPDATE_INTERVAL = "UPDATEINTERVAL";
    private JSonWrapper           storage;

    private UpdaterGUI            gui;
    private boolean               silentCheck;
    private int                   waitingUpdates  = 0;
    private boolean               updateRunning   = false;
    private Thread                updaterThread;
    private Thread                updateChecker;

    /**
     * unsynched access to gui. may return null
     * 
     * @return
     */
    private UpdaterGUI getExistingGUI() {
        return gui;
    }

    private UpdaterGUI getGUI() {
        synchronized (this) {
            if (gui == null) {
                gui = new EDTHelper<UpdaterGUI>() {

                    @Override
                    public UpdaterGUI edtRun() {
                        gui = new UpdaterGUI();

                        return gui;

                    }
                }.getReturnValue();
            }
        }
        return gui;
    }

    /**
     * Create a new instance of JDUpdater. This is a singleton class. Access the
     * only existing instance by using {@link #getInstance()}.
     */
    private JDUpdater() {
        super(new UpdaterHttpClientImpl(), new Options());
        getOptions().setDebug(JDInitFlags.SWITCH_DEBUG);
        storage = JSonWrapper.get("WEBUPDATE");

        JDUtilities.getController().addControlListener(this);
        this.getEventSender().addListener(new UpdaterListener() {

            public void onUpdaterModuleStart(UpdaterEvent arg0) {
            }

            public void onUpdaterModuleProgress(UpdaterEvent arg0, int arg1) {
            }

            public void onUpdaterModuleEnd(UpdaterEvent arg0) {
            }

            public void onUpdaterEvent(UpdaterEvent arg0) {
            }

            public void onStateExit(UpdaterState arg0) {
                if (arg0 == stateFilter) {
                    if (getOptions().isDebug()) {
                        Log.L.info("Files to Install:");
                        Log.L.info(JSonStorage.toString(getFilesToInstall()));
                        Log.L.info("Files to Download:");
                        Log.L.info(JSonStorage.toString(getUpdates()));

                        Log.L.info("Files to Remove:");
                        Log.L.info(JSonStorage.toString(getFilesToRemove()));

                    }
                }
            }

            public void onStateEnter(UpdaterState arg0) {
            }
        });

    }

    public void start() throws Exception {
        try {
            super.start();
        } catch (Exception e) {
            if (isInterrupted()) { throw new InterruptedException(e.getMessage()); }
            throw e;
        }
    }

    public void setBranchInUse(String branch) {
        storage.setProperty(PARAM_BRANCH, branch);

        storage.setProperty(BRANCHINUSE, branch);
        storage.save();
    }

    public void startUpdate(final boolean silentCheck) {
        if (updateRunning) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {

                    if (getExistingGUI() != null) {
                        getExistingGUI().setVisible(true);
                        getExistingGUI().toFront();
                        getExistingGUI().flash();
                        getExistingGUI().setExtendedState(JFrame.NORMAL);
                    } else {
                        Dialog.getInstance().showMessageDialog("Update already running");
                    }

                }

            };

            return;
        }
        updateRunning = true;
        this.silentCheck = silentCheck;

        updaterThread = new Thread(this);
        this.setThread(updaterThread);
        updaterThread.start();

    }

    public void run() {
        synchronized (this) {

            final Updater updater = JDUpdater.getInstance();
            try {
                updater.reset();

                if (silentCheck) {
                    runSilent(updater);
                } else {
                    runGUI(updater);
                }

            } finally {

                updateRunning = false;
            }
        }
    }

    private void runGUI(final Updater updater) {

        Log.L.finer("Start GUI Updatecheck");
        Log.L.finer("Start Silent Updatecheck");
        getGUI().reset();
        gui = getGUI();
        try {
            // ask to restart if there are updates left in the
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    gui.setVisible(true);
                }
            };
            updater.setBreakBeforePoint(updater.stateWaitForUnlock);
            updater.start();
            final UpdatePackage updates = updater.getUpdates();
            ArrayList<File> filesToInstall = updater.getFilesToInstall();
            ArrayList<InstalledFile> filesToRemove = updater.getFilesToRemove();
            setWaitingUpdates(filesToInstall.size() + updates.size() + filesToRemove.size());

            if (filesToInstall.size() > 0 || filesToRemove.size() > 0) {
                // UpdateFoundDialog dialog = new UpdateFoundDialog(new
                // Runnable() {
                //
                // public void run() {
                // // user clicked "Later"
                // RestartController.getInstance().exitViaUpdater();
                // }
                //
                // }, new Runnable() {
                //
                // public void run() {
                // // user clicked "NOW"
                // RestartController.getInstance().restartViaUpdater();
                // }
                //
                // }, updater);
                // try {
                // Dialog.getInstance().showDialog(dialog);
                // user clicked "INstall now"
                //
                // return;
                // } catch (DialogClosedException e) {
                //
                // } catch (DialogCanceledException e) {
                // if (e.isCausedByTimeout()) {
                // // no user interaction.
                // // ask again in next update cycle
                // return;
                // }
                // }
                // user clicked cancel
                // do not ask user unless he clicks update manually
                // stopChecker();

            } else {
                Log.L.finer("No Updates available");

            }

        } catch (Throwable e) {
            Log.exception(Level.WARNING, e);
        } finally {

        }

    }

    private void setWaitingUpdates(int size) {
        if (size == waitingUpdates) return;
        waitingUpdates = size;
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                SwingGui.getInstance().getMainFrame().setTitle(JDUtilities.getJDTitle());

            }
        };
    }

    private void runSilent(final Updater updater) {
        Log.L.finer("Start Silent Updatecheck");
        getGUI().reset();
        gui = getGUI();
        try {
            // ask to restart if there are updates left in the

            updater.setBreakBeforePoint(updater.stateWaitForUnlock);
            updater.start();
            final UpdatePackage updates = updater.getUpdates();
            ArrayList<File> filesToInstall = updater.getFilesToInstall();
            ArrayList<InstalledFile> filesToRemove = updater.getFilesToRemove();
            setWaitingUpdates(filesToInstall.size() + updates.size() + filesToRemove.size());

            if (filesToInstall.size() > 0 || filesToRemove.size() > 0) {
                UpdateFoundDialog dialog = new UpdateFoundDialog(new Runnable() {

                    public void run() {
                        // user clicked "Later"
                        RestartController.getInstance().exitViaUpdater();
                    }

                }, new Runnable() {

                    public void run() {
                        // user clicked "NOW"
                        RestartController.getInstance().restartViaUpdater();
                    }

                }, updater);
                try {
                    Dialog.getInstance().showDialog(dialog);
                    // user clicked "INstall now"

                    return;
                } catch (DialogClosedException e) {

                } catch (DialogCanceledException e) {
                    if (e.isCausedByTimeout()) {
                        // no user interaction.
                        // ask again in next update cycle
                        return;
                    }
                }
                // user clicked cancel
                // do not ask user unless he clicks update manually
                stopChecker();

            } else {
                Log.L.finer("No Updates available");

            }

        } catch (Throwable e) {
            Log.exception(Level.WARNING, e);
        } finally {

        }
    }

    private synchronized void stopChecker() {
        if (updateChecker != null) {
            updateChecker.interrupt();
            try {
                updateChecker.join(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updateChecker = null;
        }
    }

    public int getWaitingUpdates() {
        return waitingUpdates;
    }

    public void interrupt() {
        if (updaterThread != null) {

            updaterThread.interrupt();
            super.requestExit();
        }
    }

    public void startChecker() {
        updateChecker = new Thread("UpdateChecker") {
            public void run() {
                startUpdate(true);
                while (true) {
                    try {
                        Thread.sleep(storage.getIntegerProperty(UPDATE_INTERVAL, 30 * 60000));
                    } catch (InterruptedException e) {
                        return;
                    }
                    startUpdate(true);
                }
            }
        };
        updateChecker.start();
    }

    public void controlEvent(ControlEvent event) {
        // interrupt updater if jd exists
        if (ControlEvent.CONTROL_SYSTEM_EXIT == event.getEventID()) {
            interrupt();
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (gui != null) gui.setVisible(false);
                }
            };

        }
    }

    public boolean hasWaitingUpdates() {
        return getUpdates().size() > 0 || getFilesToInstall().size() > 0 || getFilesToRemove().size() > 0;
    }

}
