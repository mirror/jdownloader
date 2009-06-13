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
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import jd.Main;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.interaction.PackageManager;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDFlags;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.update.FileUpdate;
import jd.update.PackageData;
import jd.update.WebUpdater;

public class WebUpdate implements ControlListener {
    private static Logger logger = JDLogger.getLogger();
    private static boolean JDInitialized = false;
    private static boolean DynamicPluginsFinished = false;
    private static boolean ListenerAdded = false;
    private static boolean updateinprogress = false;

    public static void DynamicPluginsFinished() {
        DynamicPluginsFinished = true;
    }

    private static String getUpdaterMD5(int trycount) {

        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + "jdupdate.jar.md5";
    }

    private static String getUpdater(int trycount) {
        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + "jdupdate.jar";
    }

    public static boolean updateUpdater() {
        final ProgressController progress = new ProgressController(JDLocale.LF("wrapper.webupdate.updatenewupdater", "Downloading new jdupdate.jar"));
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
                try {
                    URLConnectionAdapter con = br.openGetConnection(getUpdater(trycount) + "?t=" + System.currentTimeMillis());
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
                                progress.setStatusText(JDLocale.LF("wrapper.webupdate.updateUpdater.error_rename", "Could not rename jdupdate.jar.tmp to jdupdate.jar"));
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
                }
                new File(file.getAbsolutePath() + ".tmp").delete();
            }
        }
        ttmp.interrupt();
        progress.setColor(Color.RED);
        progress.setStatusText(JDLocale.LF("wrapper.webupdate.updateUpdater.error_reqeust2", "Could not download new jdupdate.jar"));
        progress.finalize(5000);
        logger.info("Update of " + file.getAbsolutePath() + " failed");
        return false;
    }

    public synchronized void doWebupdate(final boolean guiCall) {
        if (!JDInitialized && !ListenerAdded) {
            if (JDUtilities.getController() != null) {
                JDUtilities.getController().addControlListener(this);
                ListenerAdded = true;
            }
        }
        // SubConfiguration cfg = WebUpdater.getConfig("WEBUPDATE");
        // cfg.setProperty(Configuration.PARAM_WEBUPDATE_DISABLE,
        // JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE,
        // false));
        // cfg.setProperty("PLAF",
        // JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).getStringProperty("PLAF"));
        // cfg.save();

        logger.finer("Init Webupdater");

        final ProgressController progress = new ProgressController(JDLocale.L("init.webupdate.progress.0_title", "Webupdate"), 100);

        // LASTREQUEST = System.currentTimeMillis();
        final WebUpdater updater = new WebUpdater();
        if (SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
            updater.ignorePlugins(false);
        }
        logger.finer("Get available files");
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
            return;
        }
        new Thread() {
            public void run() {
                PackageManager pm = new PackageManager();
                final ArrayList<PackageData> packages = pm.getDownloadedPackages();
                updater.filterAvailableUpdates(files);
                if (files != null) {
                    JDUtilities.getController().setWaitingUpdates(files);
                }
                if ((!guiCall && SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) || Main.isBeta()) {
                    logger.severe("Webupdater disabled");
                    progress.finalize();
                    return;
                }
                if (files == null && packages.size() == 0) {
                    logger.severe("Webupdater offline");
                    progress.finalize();
                    return;
                }
                int org;
                progress.setRange(org = files.size());
                logger.finer("Files found: " + files);
                logger.finer("init progressbar");
                progress.setStatusText(JDLocale.L("init.webupdate.progress.1_title", "Update Check"));
                if (files.size() > 0 || packages.size() > 0) {
                    progress.setStatus(org - (files.size() + packages.size()));
                    logger.finer("Files to update: " + files);
                    logger.finer("JDUs to update: " + packages.size());
                    while (JDInitialized == false) {
                        int i = 0;
                        try {
                            Thread.sleep(1000);
                            i++;
                            logger.severe("Waiting on JD-Init-Complete since " + i + " secs!");
                        } catch (InterruptedException e) {
                        }
                    }
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {

                            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {

                                int answer = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDLocale.L("init.webupdate.auto.countdowndialog", "Automatic update."), JDLocale.LF("system.dialogs.update.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s)  and %s package(s) or addon(s) available. Install now?</font>", files.size(), packages.size()), JDTheme.II("gui.splash.update", 32, 32), null, null);

                                if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) {
                                    doUpdate();
                                }
                            } else {
                                try {
                                    int answer = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDLocale.L("system.dialogs.update", "Updates available"), JDLocale.LF("system.dialogs.update.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s)  and %s package(s) or addon(s) available. Install now?</font>", files.size(), packages.size()), JDTheme.II("gui.splash.update", 32, 32), null, null);

                                    if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) {
                                        doUpdate();
                                    }
                                } catch (HeadlessException e) {
                                    JDLogger.exception(e);
                                }
                            }
                        }
                    });
                }
                progress.finalize();
            }
        }.start();
    }

    private static void doUpdate() {
        if (updateinprogress == true) return;
        new Thread() {
            public void run() {
                updateinprogress = true;
                while (JDInitialized == false) {
                    int i = 0;
                    try {
                        Thread.sleep(1000);
                        i++;
                        logger.severe("Waiting on JD-Init-Complete since " + i + " secs!");
                    } catch (InterruptedException e) {
                    }
                }

                while (DynamicPluginsFinished == false) {
                    int i = 0;
                    try {
                        Thread.sleep(1000);
                        i++;
                        logger.severe("Waiting on DynamicPlugins since " + i + " secs!");
                    } catch (InterruptedException e) {
                    }
                }

                DownloadController.getInstance().backupDownloadLinksSync();

                if (!WebUpdate.updateUpdater()) {
                    updateinprogress = false;
                    return;
                }
                JDIO.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "jdupdate.jar", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                if (JDUtilities.getController() != null) JDUtilities.getController().prepareShutdown();
                updateinprogress = false;
                System.exit(0);
            }
        }.start();
    }

    public void controlEvent(ControlEvent event) {

        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            JDInitialized = true;
            JDUtilities.getController().removeControlListener(this);
        }
    }
}
