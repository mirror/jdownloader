package org.jdownloader.update;

import java.awt.HeadlessException;
import java.io.IOException;

import jd.controlling.JDController;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.utils.JDUtilities;

import org.appwork.update.exchange.UpdatePackage;
import org.appwork.update.updateclient.AppNotFoundException;
import org.appwork.update.updateclient.ParseException;
import org.appwork.update.updateclient.UpdateException;
import org.appwork.update.updateclient.Updater;
import org.appwork.update.updateclient.http.HTTPIOException;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
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

    public static final String PARAM_BRANCH   = "BRANCH";
    public static final String BRANCHINUSE    = "BRANCHINUSE";
    private JSonWrapper        storage;

    private UpdaterGUI         gui;
    private boolean            silentCheck;
    private int                waitingUpdates = 0;

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
        super(new JDUpdateOptions());
        storage = JSonWrapper.get("WEBUPDATE");
    }

    public void setBranchInUse(String branch) {
        storage.setProperty(PARAM_BRANCH, branch);

        storage.setProperty(BRANCHINUSE, branch);
        storage.save();
    }

    public void startUpdate(final boolean silentCheck) {
        this.silentCheck = silentCheck;

        new Thread(this).start();

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
            } catch (AppNotFoundException e) {
                e.printStackTrace();
                Dialog.getInstance().showExceptionDialog("Update Error", e.getClass().getSimpleName(), e);
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
            }
        }
    }

    private void runGUI(Updater updater) throws HTTPIOException, ParseException, InterruptedException, UpdateException, IOException {
        getGUI().reset();
        gui = getGUI();
        setWaitingUpdates(updater.getFilesToInstall().size());

        // ask to restart if there are updates left in the

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                gui.setVisible(true);
                // SwingGui.getInstance().getMainFrame().setTitle(JDUtilities.getJDTitle());

            }
        };
        final UpdatePackage updates = updater.getUpdates();
        setWaitingUpdates(updater.getFilesToInstall().size() + updates.size());
        if (updates.size() > 0) {

            Log.L.finer(updater.getBranch().getName());
            Log.L.finer("Files to update: " + updates);

            updater.downloadUpdates();

        } else {
            updater.downloadUpdates();
        }
        setWaitingUpdates(updater.getFilesToInstall().size());
        if (updater.getFilesToInstall().size() > 0) {

            if (gui.installNow()) {

                JDUtilities.restartJDandWait();
                return;
            }
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

    private void runSilent(Updater updater) {
    }

    public int getWaitingUpdates() {
        return waitingUpdates;
    }
}
