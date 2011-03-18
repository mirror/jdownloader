package org.jdownloader.update;

import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.swing.JFrame;

import jd.controlling.JDController;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.utils.JDUtilities;

import org.appwork.update.exchange.UpdatePackage;
import org.appwork.update.updateclient.ParseException;
import org.appwork.update.updateclient.UpdateException;
import org.appwork.update.updateclient.Updater;
import org.appwork.update.updateclient.http.HTTPIOException;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class JDUpdater extends Updater implements Runnable {
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
        storage = JSonWrapper.get("WEBUPDATE");

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
            final String id = JDController.requestDelayExit("doUpdateCheck");
            final Updater updater = JDUpdater.getInstance();
            try {
                updater.reset();

                if (silentCheck) {
                    runSilent(updater);
                } else {
                    runGUI(updater);
                }

            } catch (HeadlessException e) {
                e.printStackTrace();

            } catch (HTTPIOException e) {
                e.printStackTrace();
                Dialog.getInstance().showExceptionDialog("Update Error", e.getClass().getSimpleName(), e);
            } catch (ParseException e) {
                e.printStackTrace();
                Dialog.getInstance().showExceptionDialog("Update Error", e.getClass().getSimpleName(), e);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Dialog.getInstance().showExceptionDialog("Update Error", e.getClass().getSimpleName(), e);
            } catch (UpdateException e) {
                e.printStackTrace();
                Dialog.getInstance().showExceptionDialog("Update Error", e.getClass().getSimpleName(), e);
            } catch (IOException e) {
                e.printStackTrace();
                Dialog.getInstance().showExceptionDialog("Update Error", e.getClass().getSimpleName(), e);
            } finally {

                JDController.releaseDelayExit(id);
                updateRunning = false;
            }
        }
    }

    private void runGUI(final Updater updater) throws HTTPIOException, ParseException, InterruptedException, UpdateException, IOException {

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
            ArrayList<File> filesToRemove = updater.getFilesToRemove();
            setWaitingUpdates(filesToInstall.size() + updates.size() + filesToRemove.size());

            if (filesToInstall.size() > 0 && filesToRemove.size() > 0) {
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

                }, filesToInstall.size() + filesToRemove.size());
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
                stopChecker();

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
            ArrayList<File> filesToRemove = updater.getFilesToRemove();
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

                }, filesToInstall.size() + filesToRemove.size());
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

}
