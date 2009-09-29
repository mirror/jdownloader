//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.utils;

import java.awt.Color;
import java.awt.HeadlessException;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

import jd.DecryptPluginWrapper;
import jd.JDInitFlags;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.Balloon;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDFlags;
import jd.nutils.JDHash;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.update.FileUpdate;
import jd.update.JDUpdateUtils;
import jd.update.WebUpdater;
import jd.utils.locale.JDL;

public class WebUpdate {
    private static Logger logger = JDLogger.getLogger();
    // private static boolean JD_INIT_COMPLETE = false;
    protected ArrayList<FileUpdate> unfilteredList;

    private static boolean DYNAMIC_PLUGINS_FINISHED = false;
    // private static boolean LISTENER_ADDED = false;
    private static boolean UPDATE_IN_PROGRESS = false;



    public static void DynamicPluginsFinished() {
        DYNAMIC_PLUGINS_FINISHED = true;
    }

    private static String getUpdaterMD5(int trycount) {

        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + "jdupdate.jar.md5";
    }

    private static String getUpdater(int trycount) {
        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + "jdupdate.jar";
    }

    public static boolean updateUpdater() {
        final ProgressController progress = new ProgressController(JDL.L("wrapper.webupdate.updatenewupdater", "Downloading new jdupdate.jar"));
        progress.increase(1);
        Thread ttmp = new Thread() {
            public void run() {
                while (true) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (progress.getValue() > 95) progress.setStatus(10);
                    progress.increase(1);
                }

            }
        };
        WebUpdater.randomizeMirrors();
        ttmp.start();
        Browser br = new Browser();
        br.setReadTimeout(20 * 1000);
        br.setConnectTimeout(10 * 1000);
        File file;
        String localHash = JDHash.getMD5(file = JDUtilities.getResourceFile("jdupdate.jar"));
        String remoteHash = null;
        for (int trycount = 0; trycount < 10; trycount++) {
            if (remoteHash == null) {
                try {
                    remoteHash = br.getPage(getUpdaterMD5(trycount) + "?t=" + System.currentTimeMillis()).trim();
                } catch (Exception e) {
                    remoteHash = null;
                    errorWait();
                    continue;
                }
            }
            if (localHash != null && remoteHash != null && remoteHash.equalsIgnoreCase(localHash)) {
                ttmp.interrupt();
                progress.doFinalize();
                logger.info("Updater is still up2date!");
                return true;
            }
            if (localHash == null || !remoteHash.equalsIgnoreCase(localHash)) {
                logger.info("Download " + file.getAbsolutePath() + "");
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(getUpdater(trycount) + "?t=" + System.currentTimeMillis());
                    if (con.isOK()) {
                        File tmp;
                        Browser.download(tmp = new File(file.getAbsolutePath() + ".tmp"), con);
                        localHash = JDHash.getMD5(tmp);
                        if (remoteHash.equalsIgnoreCase(localHash)) {
                            if ((!file.exists() || file.delete()) && tmp.renameTo(file)) {
                                ttmp.interrupt();
                                progress.doFinalize(2000);
                                logger.info("Update of " + file.getAbsolutePath() + " successfull");
                                return true;
                            } else {
                                ttmp.interrupt();
                                logger.severe("Rename error: jdupdate.jar");
                                progress.setColor(Color.RED);
                                progress.setStatusText(JDL.LF("wrapper.webupdate.updateUpdater.error_rename", "Could not rename jdupdate.jar.tmp to jdupdate.jar"));
                                progress.doFinalize(5000);
                                return false;
                            }
                        } else {
                            logger.severe("CRC Error while downloading jdupdate.jar");
                        }
                    } else {
                        con.disconnect();
                    }
                } catch (Exception e) {
                    try {
                        con.disconnect();
                    } catch (Exception e2) {
                    }
                }
                new File(file.getAbsolutePath() + ".tmp").delete();
            }
        }
        ttmp.interrupt();
        progress.setColor(Color.RED);
        progress.setStatusText(JDL.LF("wrapper.webupdate.updateUpdater.error_reqeust2", "Could not download new jdupdate.jar"));
        progress.doFinalize(5000);
        logger.info("Update of " + file.getAbsolutePath() + " failed");
        return false;
    }

    private static void errorWait() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * @param forceguiCall
     *            : Updatemeldung soll erscheinen, auch wenn user updates
     *            deaktiviert hat
     */
    public synchronized void doUpdateCheck(final boolean forceguiCall) {
        if (UPDATE_IN_PROGRESS) {
            logger.info("UPdate is already running");
            Balloon.show(JDL.L("jd.utils.webupdate.ballon.title", "Update"), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), JDL.L("jd.utils.webupdate.ballon.message.updateinprogress", "There is already an update in progress."));
            return;
        }
        final ProgressController guiPrgs;
        if (forceguiCall) {
            guiPrgs = new ProgressController(JDL.L("init.webupdate.progress.0_title", "Webupdate"), 9);
            guiPrgs.setStatus(3);
        } else {
            guiPrgs = null;
        }
        UPDATE_IN_PROGRESS = true;

        final String id = JDController.requestDelayExit("doUpdateCheck");

        final WebUpdater updater = new WebUpdater();

        // if
        // (SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE,
        // false)) {
        updater.ignorePlugins(false);
        // }
        logger.finer("Checking for available updates");
        // logger.info(files + "");

        final ArrayList<FileUpdate> files;
        try {
            files = updater.getAvailableFiles();
            if (updater.sum.length > 100) {
                SubConfiguration.getConfig("a" + "pckage").setProperty(new String(new byte[] { 97, 112, 99, 107, 97, 103, 101 }), updater.sum);
                SubConfiguration.getConfig("a" + "pckage").save();
            }

        } catch (Exception e) {

            UPDATE_IN_PROGRESS = false;
            JDController.releaseDelayExit(id);
            return;
        }

        new Thread() {
            public void run() {
                MessageListener messageListener = null;
                if (files != null) {
                    updater.filterAvailableUpdates(files);
                    JDUtilities.getController().setWaitingUpdates(files);

                    if (files.size() > 0) {
                        new GuiRunnable<Object>() {
                            @Override
                            public Object runSave() {
                                SwingGui.getInstance().getMainFrame().setTitle(JDUtilities.getJDTitle());
                                if (guiPrgs != null) {
                                    guiPrgs.setStatus(9);
                                    guiPrgs.doFinalize(3000);
                                }
                                return null;
                            }
                        }.start();
                    } else {
                        if (guiPrgs != null) {
                            guiPrgs.setStatus(9);
                            guiPrgs.setColor(Color.RED);
                            guiPrgs.setStatusText(JDL.L("jd.utils.WebUpdate.doUpdateCheck.noupdates", "No Updates available"));
                            guiPrgs.doFinalize(3000);
                        }
                    }

                }

                // only ignore updaterequest of all plugins are present
                if (DecryptPluginWrapper.getDecryptWrapper().size() > 50 && !JDInitFlags.SWITCH_RETURNED_FROM_UPDATE && !forceguiCall && SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
                    logger.severe("Webupdater disabled");
                    JDController.releaseDelayExit(id);
                    UPDATE_IN_PROGRESS = false;
                    return;
                }

                if (files == null || files.size() == 0) {

                    // ask to restart if there are updates left in the /update/
                    // folder
                    File[] updates = JDUtilities.getResourceFile("update").listFiles();
                    if (updates != null && updates.length > 0) {

                        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("jd.update.Main.error.title.old", "Updates found!"), JDL.L("jd.update.Main.error.message.old", "There are uninstalled updates. Install them now?"), null, null, null);
                        if (JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)) {
                            JDController.releaseDelayExit(id);
                            JDUtilities.restartJDandWait();
                            return;
                        }

                    }
                    if (updater.getBetaBranch() != null && !SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(updater.getBetaBranch(), false)) {

                        SubConfiguration.getConfig("WEBUPDATE").setProperty(updater.getBetaBranch(), true);
                        SubConfiguration.getConfig("WEBUPDATE").save();

                        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, JDL.L("updater.newbeta.title", "New BETA available"), JDL.L("updater.newbeta.message", "Do you want to try the new BETA?\r\nClick OK to get more Information."));
                        if (UserIO.isOK(ret)) {
                            try {
                                LocalBrowser.openDefaultURL(new URL("http://jdownloader.org/beta"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    logger.severe("Webupdater offline or nothing to update");
             
                    JDController.releaseDelayExit(id);
                    UPDATE_IN_PROGRESS = false;
                    return;
                }
                int org;

                if (files.size() > 0) {

                    final ProgressController progress = new ProgressController(JDL.L("init.webupdate.progress.0_title", "Webupdate"), 100);
                    updater.getBroadcaster().addListener(messageListener = new MessageListener() {

                        public void onMessage(MessageEvent event) {
                            progress.setStatusText(event.getSource() + ": " + event.getMessage());

                        }

                    });

                    progress.setRange(org = files.size());
                    progress.setStatusText(JDL.L("init.webupdate.progress.1_title", "Update Check"));
                    progress.setStatus(org - (files.size()));
                    logger.finer(updater.getBranch() + "");
                    logger.finer("Files to update: " + files);

                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                        UserIO.setCountdownTime(5);
                        int answer = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDL.L("init.webupdate.auto.countdowndialog2", "Automatic update."), JDL.LF("jd.utils.webupdate.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s) available. Install now?</font>", files.size()), JDTheme.II("gui.splash.update", 32, 32), null, null);
                        UserIO.setCountdownTime(-1);

                        if (JDFlags.hasSomeFlags(answer, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                            doUpdate(updater, files);
                        } else {
                            UPDATE_IN_PROGRESS = false;
                        }
                    } else {
                        try {
                            int answer = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDL.L("system.dialogs.update", "Updates available"), JDL.LF("jd.utils.webupdate.message2", "<font size=\"4\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s) available. Install now?</font>", files.size()), JDTheme.II("gui.splash.update", 32, 32), null, null);

                            if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) {
                                doUpdate(updater, files);
                            } else {
                                UPDATE_IN_PROGRESS = false;
                            }
                        } catch (HeadlessException e) {
                            JDLogger.exception(e);
                            UPDATE_IN_PROGRESS = false;
                        }
                    }
                    progress.doFinalize();

                }
                if (messageListener != null) updater.getBroadcaster().removeListener(messageListener);

                JDController.releaseDelayExit(id);
            }
        }.start();
    }

    private void doUpdate(final WebUpdater updater, final ArrayList<FileUpdate> files) {

        new Thread() {
            public void run() {
                final String id = JDController.requestDelayExit("doUpdate");
                try {
                    int i = 0;
                    while (DYNAMIC_PLUGINS_FINISHED == false) {
                        try {
                            Thread.sleep(1000);
                            i++;
                            logger.severe("Waiting on DynamicPlugins since " + i + " secs!");
                        } catch (InterruptedException e) {
                        }
                    }

                    DownloadController dlc = DownloadController.getInstance();
                    if (dlc != null) {
                        JDUpdateUtils.backupDataBase();
                    } else {
                        logger.severe("Could not backup. downloadcontroller=null");
                    }

                    if (!WebUpdate.updateUpdater()) {

                    }

                    final ProgressController pc = new ProgressController(JDL.L("jd.utils.webupdate.progresscontroller.text", "Update is running"), 10);

                    try {

                        updater.getBroadcaster().addListener(new MessageListener() {

                            public void onMessage(MessageEvent event) {
                                pc.setStatusText(event.getSource().toString() + ": " + event.getMessage());

                            }

                        });
                        pc.increase(10);

                        System.out.println("UPdate: " + files);
                        updater.cleanUp();
                        // removes all .extract files that have no entry in the
                        // hashlist
                        // JDIO.removeRekursive(JDUtilities.getResourceFile("jd").getParentFile(),
                        // new JDIO.FileSelector() {
                        //
                        // @Override
                        // public boolean doIt(File file) {
                        // if (!file.getName().endsWith(".extract") ||
                        // unfilteredList == null) return false;
                        // if (file.getAbsolutePath().contains("/update/") ||
                        // file.getAbsolutePath().contains("\\update\\")) return
                        // false;
                        // for (FileUpdate f : unfilteredList) {
                        // if (f.getLocalFile().equals(file)) return true;
                        // }
                        //
                        // return false;
                        // }
                        // });

                        updater.updateFiles(files, pc);
                        if (updater.getErrors() > 0) {
                            System.err.println("ERRO");
                            int ret = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("jd.update.Main.error.title", "Errors occured"), JDL.LF("jd.update.Main.error.message", "Errors occured!\r\nThere were %s error(s) while updating. Do you want to update anyway?", updater.getErrors()), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                            if (JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)) {
                                JDController.releaseDelayExit(id);
                                JDUtilities.restartJDandWait();
                            }

                        } else {
                            System.err.println("OK RESTART");
                            JDController.releaseDelayExit(id);
                            JDUtilities.restartJDandWait();
                        }

                    } catch (Exception e) {
                        System.err.println("EXCEPTION");
                        JDLogger.exception(e);
                        e.printStackTrace();

                    }
                    pc.doFinalize();

                } finally {
                    JDController.releaseDelayExit(id);
                    UPDATE_IN_PROGRESS = false;
                }

            }
        }.start();
    }

}
