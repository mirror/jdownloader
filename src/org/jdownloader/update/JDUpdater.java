package org.jdownloader.update;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.ZipException;

import javax.swing.JFrame;

import jd.JDInitFlags;
import jd.gui.swing.SwingGui;
import jd.parser.Regex;
import jd.utils.JDUtilities;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.update.exchange.UpdateFile;
import org.appwork.update.exchange.UpdatePackage;
import org.appwork.update.updateclient.InstalledFile;
import org.appwork.update.updateclient.RestartEvent;
import org.appwork.update.updateclient.UpdateHttpClient;
import org.appwork.update.updateclient.Updater;
import org.appwork.update.updateclient.UpdaterState;
import org.appwork.update.updateclient.event.UpdaterEvent;
import org.appwork.update.updateclient.event.UpdaterListener;
import org.appwork.update.updateclient.http.ClientUpdateRequiredException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.zip.ZipIOException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.update.gui.UpdateFoundDialog;

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

    protected static final String UPDATE_INTERVAL = "UPDATEINTERVAL";

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

    @Override
    public boolean canUnInstallDirect(File localFile, InstalledFile ifile) {
        return super.canUnInstallDirect(localFile, ifile);
    }

    @Override
    public boolean canInstallDirect(File next, UpdateFile uf) {
        String p = next.getAbsolutePath();
        String[] matches = new Regex(p, ".*[\\\\/]jd[\\\\/]plugins[\\\\/](.*?)[\\\\/](.+?)\\.class").getRow(0);
        if (matches != null && "hoster".equalsIgnoreCase(matches[0])) {
            try {
                String name = matches[1];

                boolean loaded;

                loaded = PluginClassLoader.getInstance().isClassLoaded("jd.plugins.hoster." + name);

                // int index;
                // while ((index = name.indexOf("$")) > 0) {
                // name = name.substring(0, index);
                // loaded |=
                // PluginClassLoader.getInstance().isClassLoaded("jd.plugins.hoster."
                // + name);
                // }

                //
                return !loaded;
            } catch (IllegalAccessException e) {
            } catch (IllegalArgumentException e) {
            } catch (InvocationTargetException e) {
            }
        } else if (matches != null && "decrypter".equalsIgnoreCase(matches[0])) {
            try {
                String name = matches[1];

                boolean loaded;

                loaded = PluginClassLoader.getInstance().isClassLoaded("jd.plugins.decrypter." + name);

                // int index;
                // while ((index = name.indexOf("$")) > 0) {
                // name = name.substring(0, index);
                // loaded |=
                // PluginClassLoader.getInstance().isClassLoaded("jd.plugins.hoster."
                // + name);
                // }

                //
                return !loaded;
            } catch (IllegalAccessException e) {
            } catch (IllegalArgumentException e) {
            } catch (InvocationTargetException e) {
            }
        }
        return super.canInstallDirect(next, uf);
    }

    public boolean installDirectFilesEnabled() {
        return true;
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

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                if (updaterThread != null && updaterThread.isAlive()) {
                    Log.L.warning("Interrupt Updater Thread because JDownloader exists");
                    updaterThread.interrupt();
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (gui != null) gui.setVisible(false);
                        }
                    };
                }
            }
        });
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
                } else if (arg0 == stateError) {
                    // Exception exc = getException();

                }

            }

            public void onStateEnter(UpdaterState arg0) {

            }

            public void onDirectInstalls(ArrayList<File> parameter) {
                boolean hasHostPlugins = false;
                boolean hasCrawlPlugins = false;
                for (File f : parameter) {
                    String[] matches = new Regex(f.getAbsolutePath(), ".*[\\\\/]jd[\\\\/]plugins[\\\\/](.*?)[\\\\/](.+?)\\.class").getRow(0);
                    if (matches != null && "hoster".equalsIgnoreCase(matches[0])) {
                        hasHostPlugins = true;
                        if (hasCrawlPlugins) break;
                    } else if (matches != null && "decrypter".equalsIgnoreCase(matches[0])) {
                        hasCrawlPlugins = true;
                        if (hasHostPlugins) break;
                    }
                }
                if (hasHostPlugins) {

                    HostPluginController.getInstance().init(true);

                }
                if (hasCrawlPlugins) {

                    CrawlerPluginController.getInstance().init(true);

                }

            }
        });

    }

    /**
     * getVersion should be -1 here. Usually the server checks the version
     * number and blocks the call of we call with an outdated version. This is
     * used for the standalon updater. the standalone updater will update itself
     * in this case. JDUpdater is used in JD, not as standalon. We do not want
     * to get updater exceptions, so we set version to -1 to ignore this.
     */
    @Override
    public int getVersion() {
        return -1;

    }

    //
    // protected int getProtocolVersion() {
    // return 4;
    // }

    public void runStateApp() throws Exception {

        try {
            super.runStateApp();
        } catch (ClientUpdateRequiredException e) {
            UpdaterGUI myGui = getExistingGUI();
            if (myGui != null) {
                myGui.setVisible(false);
            }
            try {
                Dialog.getInstance().showConfirmDialog(0, _GUI._.JDUpdater_start_updater_update_title(), _GUI._.JDUpdater_start_updater_update_msg(), NewTheme.I().getIcon("puzzle", 32), _GUI._.JDUpdater_start_restart_update_now_(), null);
                doUpdaterUpdate(e);
            } catch (DialogNoAnswerException e1) {

            }
        } catch (Exception e) {

            if (isInterrupted()) { throw new InterruptedException(e.getMessage()); }
            throw e;
        }
    }

    private void doUpdaterUpdate(ClientUpdateRequiredException e) {
        try {

            File updaterUpdatesFolder = downloadSelfUpdate(e);
            final File bootStrapper = Application.getResource("tbs.jar");
            bootStrapper.delete();
            IO.writeToFile(bootStrapper, IO.readURL(Application.getRessourceURL("tbs.jar")));
            ShutdownController.getInstance().addShutdownEvent(new RestartEvent(updaterUpdatesFolder, new String[] {}) {
                protected String getRestartingJar() {
                    return "Updater.jar";
                }
            });
            ShutdownController.getInstance().requestShutdown();

        } catch (ZipException e1) {
            e1.printStackTrace();
        } catch (ZipIOException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void setBranchInUse(String branch) {

        JsonConfig.create(WebupdateSettings.class).setBranch(branch);
        JsonConfig.create(WebupdateSettings.class).setBranchInUse(branch);

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

        updaterThread = new Thread(this) {

            @Override
            public void interrupt() {
                super.interrupt();
                /*
                 * make sure thread can interupt and does not block in any io
                 * operation
                 */
                UpdateHttpClient client = getHttpClient();
                if (client != null) {
                    client.interrupt();
                }
            }

        };
        updaterThread.setName("UpdaterThread");
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
            updater.runStateApp();
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

    private void setWaitingUpdates(final int size) {
        if (size == waitingUpdates) return;
        waitingUpdates = size;
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                SwingGui.getInstance().getMainFrame().setTitle(JDUtilities.getJDTitle(size));

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
            updater.runStateApp();
            final UpdatePackage updates = updater.getUpdates();
            ArrayList<File> filesToInstall = updater.getFilesToInstall();
            ArrayList<InstalledFile> filesToRemove = updater.getFilesToRemove();
            setWaitingUpdates(filesToInstall.size() + updates.size() + filesToRemove.size());

            if (filesToInstall.size() > 0 || filesToRemove.size() > 0) {
                // gui is visible, because user clicked manual update in the
                // meantime
                if (gui.isVisible()) return;
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

                        Thread.sleep(JsonConfig.create(WebupdateSettings.class).getUpdateInterval());
                    } catch (InterruptedException e) {
                        return;
                    }
                    startUpdate(true);
                }
            }
        };
        updateChecker.start();
    }

    public boolean hasWaitingUpdates() {
        return getUpdates().size() > 0 || getFilesToInstall().size() > 0 || getFilesToRemove().size() > 0;
    }

}
