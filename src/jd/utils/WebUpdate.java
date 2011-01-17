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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import jd.HostPluginWrapper;
import jd.JDInit;
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
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
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
    private static final Logger LOG                      = JDLogger.getLogger();
    private static int          waitingUpdates           = 0;
    //
    //
    private static boolean      DYNAMIC_PLUGINS_FINISHED = false;
    private static boolean      UPDATE_IN_PROGRESS       = false;

    public static void dynamicPluginsFinished() {
        DYNAMIC_PLUGINS_FINISHED = true;
    }

    private static String getUpdaterMD5(final int trycount) {
        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + "jdupdate.jar.md5";
    }

    private static String getUpdater(final int trycount) {
        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + "jdupdate.jar";
    }

    public static boolean updateUpdater() {
        final ProgressController progress = new ProgressController(JDL.L("wrapper.webupdate.updatenewupdater", "Downloading new jdupdate.jar"), "gui.images.update");
        progress.increase(1);
        final Thread ttmp = new Thread() {
            public void run() {
                while (true) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (progress.getValue() > 95) {
                        progress.setStatus(10);
                    }
                    progress.increase(1);
                }

            }
        };
        WebUpdater.randomizeMirrors();
        ttmp.start();
        final Browser browser = new Browser();
        browser.setReadTimeout(20 * 1000);
        browser.setConnectTimeout(10 * 1000);
        final File file = JDUtilities.getResourceFile("jdupdate.jar");
        final String fileAbsolutePath = file.getAbsolutePath();
        String localHash = JDHash.getMD5(file);
        String remoteHash = null;
        for (int trycount = 0; trycount < 10; trycount++) {
            if (remoteHash == null) {
                try {
                    remoteHash = browser.getPage(getUpdaterMD5(trycount) + "?t=" + System.currentTimeMillis()).trim();
                } catch (Exception e) {
                    remoteHash = null;
                    errorWait();
                    continue;
                }
            }
            if (localHash != null && remoteHash != null && remoteHash.equalsIgnoreCase(localHash)) {
                ttmp.interrupt();
                progress.doFinalize();
                LOG.info("Updater is still up2date!");
                return true;
            }
            if (localHash == null || !remoteHash.equalsIgnoreCase(localHash)) {
                LOG.info("Download " + fileAbsolutePath + "");
                URLConnectionAdapter con = null;
                try {
                    con = browser.openGetConnection(getUpdater(trycount) + "?t=" + System.currentTimeMillis());
                    if (con.isOK()) {
                        final File tmp = new File(fileAbsolutePath + ".tmp");
                        Browser.download(tmp, con);
                        localHash = JDHash.getMD5(tmp);
                        if (remoteHash.equalsIgnoreCase(localHash)) {
                            if ((!file.exists() || file.delete()) && tmp.renameTo(file)) {
                                ttmp.interrupt();
                                progress.doFinalize(2000);
                                LOG.info("Update of " + fileAbsolutePath + " successfull");
                                return true;
                            } else {
                                ttmp.interrupt();
                                LOG.severe("Rename error: jdupdate.jar");
                                progress.setColor(Color.RED);
                                progress.setStatusText(JDL.LF("wrapper.webupdate.updateUpdater.error_rename", "Could not rename jdupdate.jar.tmp to jdupdate.jar"));
                                progress.doFinalize(5000);
                                return false;
                            }
                        } else {
                            LOG.severe("CRC Error while downloading jdupdate.jar");
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
                new File(fileAbsolutePath + ".tmp").delete();
            }
        }
        ttmp.interrupt();
        progress.setColor(Color.RED);
        progress.setStatusText(JDL.LF("wrapper.webupdate.updateUpdater.error_reqeust2", "Could not download new jdupdate.jar"));
        progress.doFinalize(5000);
        LOG.info("Update of " + fileAbsolutePath + " failed");
        return false;
    }

    private static void errorWait() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param forceguiCall
     *            : Updatemeldung soll erscheinen, auch wenn user updates
     *            deaktiviert hat
     */
    public static synchronized void doUpdateCheck(final boolean forceguiCall) {
        if (UPDATE_IN_PROGRESS) {
            LOG.info("Update is already running");
            Balloon.show(JDL.L("jd.utils.webupdate.ballon.title", "Update"), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), JDL.L("jd.utils.webupdate.ballon.message.updateinprogress", "There is already an update in progress."));
            return;
        }
        final ProgressController guiPrgs;
        if (forceguiCall) {
            guiPrgs = new ProgressController(ProgressController.Type.DIALOG, JDL.L("init.webupdate.progress.0_title", "Webupdate"), JDL.L("init.webupdate.progress.0_title", "Webupdate"), 9, "gui.images.update");
            guiPrgs.setStatus(3);
        } else {
            guiPrgs = null;
        }
        UPDATE_IN_PROGRESS = true;

        final String id = JDController.requestDelayExit("doUpdateCheck");

        final WebUpdater updater = new WebUpdater();

        updater.ignorePlugins(false);
        LOG.finer("Checking for available updates");

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

                    boolean coreUp2Date = true;
                    final ArrayList<FileUpdate> tmpfiles = new ArrayList<FileUpdate>();
                    for (FileUpdate f : files) {
                        // check if jdownloader.jar is up2date
                        if (f.getLocalFile().equals(JDUtilities.getResourceFile("JDownloader.jar"))) {
                            coreUp2Date = false;
                        }
                        if (f.getLocalFile().getName().endsWith(".class")) {
                            tmpfiles.add(f);
                        }

                    }
                    if (coreUp2Date && !checkIfRestartRequired(tmpfiles)) {
                        doPluginUpdate(updater, tmpfiles);
                        for (FileUpdate f : tmpfiles) {
                            if (f.equals()) {
                                files.remove(f);
                            }
                        }
                    }
                    WebUpdate.setWaitingUpdates(files.size());

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
                if (HostPluginWrapper.getHostWrapper().size() > 50 && !JDInitFlags.SWITCH_RETURNED_FROM_UPDATE && !forceguiCall && JSonWrapper.get("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
                    LOG.severe("Webupdater disabled");
                    /*
                     * autostart downloads if not autostarted yet and
                     * autowebupdate is also enabled
                     */
                    if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                        JDController.getInstance().autostartDownloadsonStartup();
                    }
                    JDController.releaseDelayExit(id);
                    UPDATE_IN_PROGRESS = false;
                    return;
                }

                if (files == null || files.isEmpty()) {

                    // ask to restart if there are updates left in the /update/
                    // folder
                    final File[] updates = JDUtilities.getResourceFile("update").listFiles();
                    if (updates != null && updates.length > 0) {
                        final int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("jd.update.Main.error.title.old", "Updates found!"), JDL.L("jd.update.Main.error.message.old", "There are uninstalled updates. Install them now?"), null, null, null);
                        if (JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)) {
                            JDController.releaseDelayExit(id);
                            JDUtilities.restartJDandWait();
                            return;
                        }
                    }
                    if (updater.getBetaBranch() != null && !JSonWrapper.get("WEBUPDATE").getBooleanProperty(updater.getBetaBranch(), false)) {
                        JSonWrapper.get("WEBUPDATE").setProperty(updater.getBetaBranch(), true);
                        JSonWrapper.get("WEBUPDATE").save();

                        final int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, JDL.L("updater.newbeta.title", "New BETA available"), JDL.L("updater.newbeta.message", "Do you want to try the new BETA?\r\nClick OK to get more Information."));
                        if (UserIO.isOK(ret)) {
                            try {
                                LocalBrowser.openDefaultURL(new URL("http://jdownloader.org/beta"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    LOG.severe("Webupdater offline or nothing to update");
                    /*
                     * autostart downloads if not autostarted yet and
                     * autowebupdate is also enabled
                     */
                    if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                        JDController.getInstance().autostartDownloadsonStartup();
                    }
                    JDController.releaseDelayExit(id);
                    UPDATE_IN_PROGRESS = false;
                    return;
                }
                int org;

                if (files.size() > 0) {
                    final ProgressController progress = new ProgressController(JDL.L("init.webupdate.progress.0_title", "Webupdate"), 100, "gui.images.update");
                    updater.getBroadcaster().addListener(messageListener = new MessageListener() {
                        public void onMessage(final MessageEvent event) {
                            progress.setStatusText(event.getCaller() + ": " + event.getMessage());
                        }
                    });

                    progress.setRange(org = files.size());
                    progress.setStatusText(JDL.L("init.webupdate.progress.1_title", "Update Check"));
                    progress.setStatus(org - (files.size()));
                    LOG.finer(updater.getBranch());
                    LOG.finer("Files to update: " + files);

                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                        UserIO.setCountdownTime(5);
                        final int answer = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDL.L("init.webupdate.auto.countdowndialog2", "Automatic update."), JDL.LF("jd.utils.webupdate.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s) available. Install now?</font>", files.size()), JDTheme.II("gui.images.update", 32, 32), null, null);
                        UserIO.setCountdownTime(-1);

                        if (JDFlags.hasSomeFlags(answer, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                            doUpdate(updater, files);
                        } else {
                            UPDATE_IN_PROGRESS = false;
                        }
                    } else {
                        try {
                            final String html = JDL.L("jd.utils.webupdate.whatchangedlink", "<hr/><a href='http://jdownloader.org/latestchanges'>What has changed?</a>");
                            int answer = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDL.L("system.dialogs.update", "Updates available"), JDL.LF("jd.utils.webupdate.message2", "<font size=\"4\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s) available. Install now?</font>", files.size()) + html, JDTheme.II("gui.images.update", 32, 32), null, null);

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
                if (messageListener != null) {
                    updater.getBroadcaster().removeListener(messageListener);
                }
                /*
                 * autostart downloads if not autostarted yet and autowebupdate
                 * is also enabled
                 */
                if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                    JDController.getInstance().autostartDownloadsonStartup();
                }
                JDController.releaseDelayExit(id);
            }
        }.start();
    }

    /**
     * checks the files to update and returns true if we can do a silent
     * pluginupdate.
     * 
     * @param tmpfiles
     * @return
     */
    protected static boolean checkIfRestartRequired(ArrayList<FileUpdate> tmpfiles) {
        for (FileUpdate f : tmpfiles) {
            String path = f.getLocalPath();
            if (path.endsWith(".class")) {
                if (classIsLoaded(path.replace("/", ".").substring(1, path.length() - 6))) return true;
            }

        }
        return false;
    }

    /**
     * Checks if the class (a plugin) already has been loaded)
     * 
     * @param clazz
     * @return
     */
    private static boolean classIsLoaded(String clazz) {

        java.lang.reflect.Method m;
        try {

            m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });

            m.setAccessible(true);

            Object test1 = m.invoke(JDInit.getPluginClassLoader(), clazz);
            return test1 != null;

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void doPluginUpdate(final WebUpdater updater, final ArrayList<FileUpdate> files) {
        final ProgressController pc = new ProgressController("", files.size(), "gui.images.update");

        try {
            updater.getBroadcaster().addListener(new MessageListener() {

                public void onMessage(MessageEvent event) {
                    pc.setStatusText(event.getMessage() + " " + JDL.L("jd.utils.WebUpdate.doPluginUpdate", "[Restart on plugin out of date errors]"));
                }

            });
            System.out.println("Update: " + files);

            updater.updateFiles(files, pc);// copies plugins

            // please check:
            boolean restart = false;
            for (Iterator<FileUpdate> it = files.iterator(); it.hasNext();) {
                final FileUpdate f = it.next();
                final File localFile = f.getLocalFile();
                final File localTmpFile = f.getLocalTmpFile();

                // try to rename NOW
                if (!((!localFile.exists() || localFile.delete()) && (localTmpFile.renameTo(localFile)))) {
                    restart = true;
                    // has not been updated
                    // it.remove();
                } else {
                    File parent = localTmpFile.getParentFile();
                    while (parent.listFiles() != null && parent.listFiles().length < 1) {
                        parent.delete();
                        parent = parent.getParentFile();
                    }
                }
            }
            if (restart) {
                Balloon.show(JDL.L("jd.utils.WebUpdate.doPluginUpdate.title", "Restart recommended"), null, JDL.L("jd.utils.WebUpdate.doPluginUpdate.message", "Some Plugins have been updated\r\nYou should restart JDownloader."));
            }
        } catch (Exception e) {
            System.err.println("EXCEPTION");
            JDLogger.exception(e);
            e.printStackTrace();
        }
        pc.doFinalize();

    }

    private static void doUpdate(final WebUpdater updater, final ArrayList<FileUpdate> files) {

        new Thread() {
            public void run() {
                final String id = JDController.requestDelayExit("doUpdate");
                try {
                    int secs = 0;
                    while (!DYNAMIC_PLUGINS_FINISHED) {
                        try {
                            Thread.sleep(1000);
                            secs++;
                            LOG.severe("Waiting on DynamicPlugins since " + secs + " secs!");
                        } catch (InterruptedException e) {
                        }
                    }

                    // DownloadController dlc =
                    // DownloadController.getInstance();
                    // if (dlc != null) {
                    if (DownloadController.getInstance() != null) {
                        JDUpdateUtils.backupDataBase();
                    } else {
                        LOG.severe("Could not backup. downloadcontroller=null");
                    }

                    // if (!WebUpdate.updateUpdater()) {
                    //
                    // }
                    WebUpdate.updateUpdater();

                    final ProgressController pc = new ProgressController(JDL.L("jd.utils.webupdate.progresscontroller.text", "Update is running"), 10, "gui.images.update");

                    try {

                        updater.getBroadcaster().addListener(new MessageListener() {

                            public void onMessage(final MessageEvent event) {
                                pc.setStatusText(event.getCaller().toString() + ": " + event.getMessage());

                            }

                        });
                        pc.increase(10);

                        System.out.println("Update: " + files);
                        updater.cleanUp();

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

    private static void setWaitingUpdates(final int i) {
        waitingUpdates = Math.max(0, i);
    }

    public static int getWaitingUpdates() {
        return waitingUpdates;
    }

}
