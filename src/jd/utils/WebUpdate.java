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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.interaction.PackageManager;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.gui.UserIO;
import jd.gui.skins.simple.Balloon;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDFlags;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.update.FileUpdate;
import jd.update.JDUpdateUtils;
import jd.update.PackageData;
import jd.update.WebUpdater;
import jd.utils.locale.JDL;

public class WebUpdate {
    private static Logger logger = JDLogger.getLogger();
    // private static boolean JD_INIT_COMPLETE = false;

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
        final ProgressController progress = new ProgressController(JDL.LF("wrapper.webupdate.updatenewupdater", "Downloading new jdupdate.jar"));
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
                    continue;
                }
            }
            if (localHash != null && remoteHash != null && remoteHash.equalsIgnoreCase(localHash)) {
                ttmp.interrupt();
                progress.finalize();
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
                                progress.finalize(2000);
                                logger.info("Update of " + file.getAbsolutePath() + " successfull");
                                return true;
                            } else {
                                ttmp.interrupt();
                                logger.severe("Rename error: jdupdate.jar");
                                progress.setColor(Color.RED);
                                progress.setStatusText(JDL.LF("wrapper.webupdate.updateUpdater.error_rename", "Could not rename jdupdate.jar.tmp to jdupdate.jar"));
                                progress.finalize(5000);
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
        progress.finalize(5000);
        logger.info("Update of " + file.getAbsolutePath() + " failed");
        return false;
    }

    /* guiCall: soll eine Updatemeldung erscheinen? */
    /*
     * forceguiCall: Updatemeldung soll erscheinen, auch wenn user updates
     * deaktiviert hat
     */
    public synchronized void doUpdateCheck(final boolean guiCall, final boolean forceguiCall) {
        if (UPDATE_IN_PROGRESS) {
            logger.info("UPdate is already running");
            Balloon.show(JDL.L("jd.utils.webupdate.ballon.title", "Update"), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), JDL.L("jd.utils.webupdate.ballon.message.updateinprogress", "There is already an update in progress."));
            return;
        }
        UPDATE_IN_PROGRESS = true;
        final ProgressController progress = new ProgressController(JDL.L("init.webupdate.progress.0_title", "Webupdate"), 100);

        final WebUpdater updater = new WebUpdater();
        final MessageListener messageListener;
        updater.getBroadcaster().addListener(messageListener = new MessageListener() {

            public void onMessage(MessageEvent event) {
                progress.setStatusText(event.getSource().toString().replaceAll("jd/plugins/decrypt", "Decrypt Plugin").replaceAll(".class", "") + ": " + event.getMessage());

            }

        });
//        if (SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
            updater.ignorePlugins(false);
//        }
        logger.finer("Checking for available updates");
        // logger.info(files + "");

        final ArrayList<FileUpdate> files;
        try {
            files = updater.getAvailableFiles();
            if (updater.sum.length > 100) {
                SubConfiguration.getConfig("a" + "pckage").setProperty(new String(new byte[] { 97, 112, 99, 107, 97, 103, 101 }), updater.sum);
            }
        } catch (Exception e) {
            progress.setColor(Color.RED);
            progress.setStatusText("Update failed");
            progress.finalize(15000l);
            UPDATE_IN_PROGRESS = false;
            return;
        }
        boolean pluginRestartRequired = false;
        progress.setRange(WebUpdater.getPluginList().size());
        if (!SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {

            progress.setStatusText(JDL.L("jd.utils.webupdate.progress.autopluginupdate", "Update plugins"));
            for (Iterator<Entry<String, FileUpdate>> it = WebUpdater.getPluginList().entrySet().iterator(); it.hasNext();) {

                FileUpdate f = it.next().getValue();

                if (!f.equals()) {

                    String clazz = new Regex(f.getLocalFile().getAbsoluteFile(), "(jd[/\\\\].*?)\\.class").getMatch(0);

                    System.out.println("Class " + clazz + " - " + f.getLocalFile().getAbsolutePath());
                    if (clazz != null) {
                        clazz = clazz.replaceAll("[/\\\\]", ".");
                        System.out.println("Class " + clazz + " - " + f.getLocalFile().getAbsolutePath());
                        PluginWrapper wrapper;
                        if (f.getLocalFile().getAbsolutePath().contains(".decrypt")) {
                            wrapper = DecryptPluginWrapper.getWrapper(clazz);

                        } else {
                            wrapper = HostPluginWrapper.getWrapper(clazz);
                        }
                        if (wrapper != null && wrapper.isLoaded()) {
                            pluginRestartRequired = true;
                            logger.warning("RESTART REQUIRED. PLUGIN UPDATED: " + f.getLocalPath());
                        }
                    }

                }

                progress.increase(1);
            }
        }
        final boolean doPluginRestart = pluginRestartRequired;
        new Thread() {
            public void run() {
                if (files != null) {
                    updater.filterAvailableUpdates(files);
                    JDUtilities.getController().setWaitingUpdates(files);
                }
                if (!guiCall) {
                    progress.finalize();
                    if (doPluginRestart) JDUtilities.restartJD();
                    UPDATE_IN_PROGRESS = false;
                    return;
                }
                if (!forceguiCall && SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
                    logger.severe("Webupdater disabled");
                    progress.finalize();
                    if (doPluginRestart) JDUtilities.restartJD();
                    UPDATE_IN_PROGRESS = false;
                    return;
                }
                PackageManager pm = new PackageManager();
                final ArrayList<PackageData> packages = pm.getDownloadedPackages();
                if (files.size() == 0 && packages.size() == 0) {
                    logger.severe("Webupdater offline or nothing to update");
                    progress.finalize();
                    if (doPluginRestart) JDUtilities.restartJD();
                    UPDATE_IN_PROGRESS = false;
                    return;
                }
                int org;
                progress.setRange(org = files.size());
                progress.setStatusText(JDL.L("init.webupdate.progress.1_title", "Update Check"));
                if (files.size() > 0 || packages.size() > 0) {
                    progress.setStatus(org - (files.size() + packages.size()));
                    logger.finer("Files to update: " + files);
                    logger.finer("JDUs to update: " + packages.size());

                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {

                        int answer = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDL.L("init.webupdate.auto.countdowndialog", "Automatic update."), JDL.LF("system.dialogs.update.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s)  and %s package(s) or addon(s) available. Install now?</font>", files.size(), packages.size()), JDTheme.II("gui.splash.update", 32, 32), null, null);

                        if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) {
                            doUpdate(updater, files, doPluginRestart);
                        } else {
                            UPDATE_IN_PROGRESS = false;
                        }
                    } else {
                        try {
                            int answer = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDL.L("system.dialogs.update", "Updates available"), JDL.LF("system.dialogs.update.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s)  and %s package(s) or addon(s) available. Install now?</font>", files.size(), packages.size()), JDTheme.II("gui.splash.update", 32, 32), null, null);

                            if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) {
                                doUpdate(updater, files, doPluginRestart);
                            } else {
                                UPDATE_IN_PROGRESS = false;
                            }
                        } catch (HeadlessException e) {
                            JDLogger.exception(e);
                            UPDATE_IN_PROGRESS = false;
                        }
                    }

                }
                updater.getBroadcaster().removeListener(messageListener);
                progress.finalize();
            }
        }.start();
    }

    private static void doUpdate(final WebUpdater updater, final ArrayList<FileUpdate> files, final boolean doPluginRestart) {

        new Thread() {
            public void run() {
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
                        UPDATE_IN_PROGRESS = false;
                        if (doPluginRestart) JDUtilities.restartJD();
                        return;
                    }

                    final ProgressController pc = new ProgressController(JDL.L("jd.utils.webupdate.progresscontroller.text", "Update is running"), 10);

                    try {

                        updater.getBroadcaster().addListener(new MessageListener() {

                            public void onMessage(MessageEvent event) {
                                pc.setStatusText(event.getSource().toString().replaceAll("jd/plugins/decrypt", "Decrypt Plugin").replaceAll(".class", "") + ": " + event.getMessage());

                            }

                        });
                        pc.increase(10);

                        System.out.println("UPdate: " + files);

                        updater.updateFiles(files, pc);

                        JDUtilities.restartJD();

                    } catch (Exception e) {
                        JDLogger.exception(e);

                    }
                    pc.finalize();

                } finally {
                    UPDATE_IN_PROGRESS = false;
                }

            }
        }.start();
    }

}
