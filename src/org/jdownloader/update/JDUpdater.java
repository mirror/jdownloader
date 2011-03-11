package org.jdownloader.update;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
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
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;

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
        getGUI().reset();
        gui = getGUI();
        try {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    gui.setVisible(true);
                }
            };
            // ask to restart if there are updates left in the
            ArrayList<File> filesToInstall = updater.getFilesToInstall();
            setWaitingUpdates(filesToInstall.size());

            final UpdatePackage updates = updater.getUpdates();
            setWaitingUpdates(filesToInstall.size() + updates.size());
            if (updates.size() > 0) {

                // gui.doDownloadNow(new Runnable() {
                //
                // public void run() {
                // // try {
                // // updater.downloadUpdates();
                // //
                // // setWaitingUpdates(updater.getFilesToInstall().size());
                // // updater.getStateMachine().forceState(StateApp.DONE);
                // //
                // // gui.installNow();
                // // } catch (HTTPIOException e1) {
                // // gui.onException(e1);
                // // } catch (ParseException e1) {
                // // gui.onException(e1);
                // // } catch (InterruptedException e1) {
                // // gui.onException(e1);
                // // } catch (UpdateException e1) {
                // // gui.onException(e1);
                // // } catch (IOException e1) {
                // // gui.onException(e1);
                // // } catch (RuntimeException e1) {
                // // gui.onException(e1);
                // //
                // // }
                //
                // }
                // }, new Runnable() {
                //
                // public void run() {
                // }
                // });
                Log.L.finer(updater.getBranch().getName());
                Log.L.finer("Files to update: " + updates);

            } else if (filesToInstall.size() > 0) {

                // updater.getStateMachine().forceState(StateApp.DONE);
                //
                // gui.installNow();

            } else {
                Log.L.finer("No Updates available");

            }

        } catch (Throwable e) {
            // gui.onException(e);
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

            setWaitingUpdates(filesToInstall.size() + updates.size());

            if (filesToInstall.size() > 0) {
                ConfirmDialog dialog = new ConfirmDialog(Dialog.LOGIC_COUNTDOWN, "Update!", "XYZ are ready for Installation.\r\nDo you want to run the update now?", ImageProvider.getImageIcon("logo", 32, 32), "Yes", null);
                dialog.setLeftActions(new AbstractAction("Ask me later") {

                    /**
                     * 
                     */
                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(ActionEvent e) {
                        RestartController.getInstance().exitViaUpdater();
                    }

                });
                Dialog.getInstance().showDialog(dialog);
                RestartController.getInstance().restartViaUpdater();

                // updater.getStateMachine().forceState(StateApp.DONE);
                //

            } else {
                Log.L.finer("No Updates available");

            }

        } catch (Throwable e) {
            Log.exception(e);
        } finally {

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
