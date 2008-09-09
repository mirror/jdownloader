//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.config.CFGConfig;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.BackupLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginOptional;
import jd.plugins.PluginsC;
import jd.update.PackageData;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 */

public class JDInit {

    private static Logger logger = JDUtilities.getLogger();

    public static void setupProxy() {
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_PROXY, false)) {
            //http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html
            // für evtl authentifizierung:
            //http://www.softonaut.com/2008/06/09/using-javanetauthenticator-for
            // -proxy-authentication/
            // nonProxy Liste ist unnötig, da ja eh kein reconnect möglich wäre
            String host = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_HOST, "");
            String port = new Integer(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PROXY_PORT, 8080)).toString();
            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS, "");
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_HOST, host);

            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_PROXY, true);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PORT, port);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_USER, user);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PASS, pass);

            System.setProperty("http.proxySet", "true");
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", port);
            logger.info("http-proxy: enabled " + user + ":" + pass + "@" + host + ":" + port);

            System.setProperty("http.proxyUserName", user);
            System.setProperty("http.proxyPassword", pass);

        } else {
            System.setProperty("http.proxyHost", "");
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_PROXY, false);

            System.setProperty("http.proxySet", "false");
            logger.info("http-proxy: disabled");

        }
    }

    public static void setupSocks() {
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_SOCKS, false)) {
            //http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html

            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER_SOCKS, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS_SOCKS, "");
            String host = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.SOCKS_HOST, "");
            String port = new Integer(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.SOCKS_PORT, 1080)).toString();

            System.setProperty("socksProxySet", "true");
            System.setProperty("socksProxyHost", host);
            System.setProperty("socksProxyPort", port);
            System.setProperty("socksProxyUserName", user);
            System.setProperty("socksProxyPassword", pass);
            System.setProperty("socks.useProxy", "true");
            System.setProperty("socks.proxyHost", host);
            System.setProperty("socks.proxyPort", port);
            System.setProperty("socks.proxyUserName", user);
            System.setProperty("socks.proxyPassword", pass);

            logger.info("socks-proxy: enabled");

            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.SOCKS_HOST, host);

            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_SOCKS, true);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.SOCKS_PORT, port);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_USER_SOCKS, user);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PASS_SOCKS, pass);

        } else {
            System.setProperty("socksProxySet", "false");
            System.setProperty("socks.useProxy", "false");
            System.setProperty("socks.proxyHost", "");
            System.setProperty("socksProxyHost", "");

            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_SOCKS, false);

            logger.info("socks-proxy: disabled");
        }
    }

    private int cid = -1;

    private boolean installerVisible = false;

    private SplashScreen splashScreen;

    public JDInit() {
        this(null);
    }

    public JDInit(SplashScreen splashScreen) {
        this.splashScreen = splashScreen;
    }

    private void afterConfigIsLoaded() {

    }

    public void checkUpdate() {
        File updater = JDUtilities.getResourceFile("webupdater.jar");
        if (updater.exists()) {
            if (!updater.delete()) {
                logger.severe("Webupdater.jar could not be deleted. PLease remove JDHOME/webupdater.jar to ensure a proper update");
            }
        }
        if (JDUtilities.getResourceFile("webcheck.tmp").exists() && JDUtilities.getLocalFile(JDUtilities.getResourceFile("webcheck.tmp")).indexOf("(Revision" + JDUtilities.getRevision() + ")") > 0) {
            JDUtilities.getController().getUiInterface().showTextAreaDialog("Error", "Failed Update detected!", "It seems that the previous webupdate failed.\r\nPlease ensure that your java-version is equal- or above 1.5.\r\nMore infos at http://www.syncom.org/projects/jdownloader/wiki/FAQ.\r\n\r\nErrorcode: \r\n" + JDUtilities.getLocalFile(JDUtilities.getResourceFile("webcheck.tmp")));
            JDUtilities.getResourceFile("webcheck.tmp").delete();
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false);
        } else {

            Interaction.handleInteraction(Interaction.INTERACTION_APPSTART, false);
        }

        String hash = "";

        if (JDUtilities.getResourceFile("updatemessage.html").exists()) {
            hash = JDUtilities.getLocalHash(JDUtilities.getResourceFile("updatemessage.html"));
        }

        JDUtilities.getRunType();
        if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_HASH, "").equals(hash)) {
            logger.info("Returned from Update");

            if (splashScreen != null) {
                splashScreen.finish();
            }

            SimpleGUI.showChangelogDialog();

        }
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_HASH, hash);
        JDUtilities.saveConfig();
    }

    protected void createQueueBackup() {
        Vector<DownloadLink> links = JDUtilities.getController().getDownloadLinks();
        Iterator<DownloadLink> it = links.iterator();
        Vector<BackupLink> ret = new Vector<BackupLink>();
        while (it.hasNext()) {
            DownloadLink next = it.next();
            if (next.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                ret.add(new BackupLink(new File(next.getContainerFile()), next.getContainerIndex(), next.getContainer()));
            } else {
                ret.add(new BackupLink(next.getDownloadURL()));
            }

        }

        JDUtilities.getResourceFile("links.linkbackup").delete();
        JDUtilities.saveObject(null, ret, JDUtilities.getResourceFile("links.linkbackup"), "links.linkbackup", "linkbackup", false);
    }

    public void doWebupdate(final int oldCid, final boolean guiCall) {
        CFGConfig cfg = CFGConfig.getConfig("WEBUPDATE");

        cfg.setProperty("PLAF", JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty("PLAF"));
        cfg.save();

        new Thread() {

            public void run() {
                ProgressController progress = new ProgressController(JDLocale.L("init.webupdate.progress.0_title", "Webupdate"), 100);
                PackageManager pm = new PackageManager();
                ArrayList<PackageData> packages = pm.getDownloadedPackages();
                checkMessage();

                logger.finer("Init Webupdater");
                final WebUpdater updater = new WebUpdater(null);
                updater.setCid(oldCid);
                logger.finer("Get available files");
                // logger.info(files + "");
                Vector<Vector<String>> files;
                try {
                    files = updater.getAvailableFiles();
                } catch (Exception e) {
                    progress.setColor(Color.RED);
                    progress.setStatusText("Update failed");
                    progress.finalize(15000l);
                    return;
                }
                updater.filterAvailableUpdates(files, JDUtilities.getResourceFile("."));

                if (files != null) {
                    JDUtilities.getController().setWaitingUpdates(files);
                }

                cid = updater.getCid();
                if (getCid() > 0 && getCid() != JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.CID, getCid());
                    JDUtilities.saveConfig();
                }
                JDUtilities.getSubConfig("GUI").setProperty(new String(new byte[] { 112, 97, 99, 107, 97, 103, 101 }), updater.sum);

                if (!guiCall && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
                    logger.severe("Webupdater disabled");
                    progress.finalize();
                    return;
                }

                if (files == null && packages.size() == 0) {
                    logger.severe("Webupdater offline");
                    progress.finalize();
                    return;
                }
                if (files == null) {
                    files = new Vector<Vector<String>>();
                }
                int org;
                progress.setRange(org = files.size());
                logger.finer("Files found: " + files);

                logger.finer("init progressbar");
                progress.setStatusText(JDLocale.L("init.webupdate.progress.1_title", "Update Check"));
                if (files.size() > 0 || packages.size() > 0) {

                    progress.setStatus(org - (files.size() + packages.size()));
                    logger.finer("FIles to update: " + files);
                    logger.finer("JDUs to update: " + packages.size());

                    createQueueBackup();

                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                        CountdownConfirmDialog ccd = new CountdownConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(), JDLocale.LF("init.webupdate.auto.countdowndialog", "Automatic update."), 10, true, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);
                        if (ccd.result) {

                            try {
                                Browser.download(JDUtilities.getResourceFile("webupdater.jar"), "http://service.jdownloader.org/update/webupdater.jar");
                            } catch (IOException e) {
                                progress.setStatusText(JDLocale.L("init.webupdate.error.could_not_load_webupdater", "Could not load Webupdater"));
                                progress.setColor(Color.RED);
                                progress.finalize(10000l);
                                return;
                            }
                            // createDLCBackup();
                            JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                            logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                            System.exit(0);
                        }
                    } else {

                        try {

                            CountdownConfirmDialog ccd = new CountdownConfirmDialog(JDUtilities.getGUI() != null ? ((SimpleGUI) JDUtilities.getGUI()).getFrame() : null, JDLocale.L("system.dialogs.update", "Updates available"), JDLocale.LF("system.dialogs.update.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s)  and %s package(s) or addon(s) available. Install now?</font>", files.size() + "", packages.size() + ""), 20, false, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);

                            if (ccd.result) {
                                try {
                                    Browser.download(JDUtilities.getResourceFile("webupdater.jar"), "http://service.jdownloader.org/update/webupdater.jar");
                                } catch (IOException e) {
                                    progress.setStatusText(JDLocale.L("init.webupdate.error.could_not_load_webupdater", "Could not load Webupdater"));
                                    progress.setColor(Color.RED);
                                    progress.finalize(10000l);
                                    return;
                                }
                                // createDLCBackup();
                                JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                                logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                                System.exit(0);
                            }
                        } catch (HeadlessException e) {

                            e.printStackTrace();
                        }

                    }

                }

                progress.finalize();
                if (getCid() > 0 && getCid() != JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.CID, getCid());
                    JDUtilities.saveConfig();
                }
            }

            private void checkMessage() {
                File res = JDUtilities.getResourceFile("message.html");
                String hash = JDUtilities.getLocalHash(res);
                try {
                    Browser.download(JDUtilities.getResourceFile("message.html"), "http://service.jdownloader.org/html/message.html");
                } catch (IOException e) {
                    return;
                }
                String hash2 = JDUtilities.getLocalHash(res);
                if (hash2 != null && !hash2.equals(hash)) {
                    String message = JDUtilities.getLocalFile(res);
                    if (message != null && message.trim().length() > 0) {
                        CountdownConfirmDialog ccd = new CountdownConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(), JDLocale.L("sys.warning.newMessage", "New Systemmessage"), message, 45, false, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_STOP_COUNTDOWN);
                        if (!ccd.result) {
                            res.delete();
                            res.deleteOnExit();
                        }
                    }
                }

            }

            // private void createDLCBackup() {
            // ProgressController p = new
            // ProgressController(JDLocale.L("init.backup.progress",
            // "Queue backup"), 3);
            //
            // File file = JDUtilities.getResourceFile("container/backup_" +
            // System.currentTimeMillis() + ".dlc");
            // p.increase(1);
            // JDUtilities.getController().saveDLC(file);
            // p.increase(2);
            // CFGConfig.getConfig("WEBUPDATE").setProperty("LAST_BACKUP",
            // file);
            // CFGConfig.getConfig("WEBUPDATE").save();
            // p.finalize(2000);
            //
            // }

        }.start();
    }

    public int getCid() {
        return cid;
    }

    void init() {
        CookieHandler.setDefault(null);

    }

    public JDController initController() {
        return new JDController();
    }

    public UIInterface initGUI(JDController controller) {

        UIInterface uiInterface = new SimpleGUI();
        controller.setUiInterface(uiInterface);
        controller.addControlListener(uiInterface);
        return uiInterface;
    }

    public void initPlugins() {
        logger.info("Lade Plugins");

        JDUtilities.setPluginForDecryptList(loadPluginForDecrypt());
        JDUtilities.setPluginForHostList(loadPluginForHost());
        JDUtilities.setPluginForContainer(loadCPlugins());
        JDUtilities.setPluginOptionalList(loadPluginOptional());

        for (PluginOptional plg : JDUtilities.getPluginsOptional()) {
            try {
                if (JDUtilities.getConfiguration().getBooleanProperty("OPTIONAL_PLUGIN_" + plg.getPluginName(), false) && !plg.initAddon()) {
                    logger.severe("Error loading Optional Plugin: FALSE");
                }
            } catch (Throwable e) {
                logger.severe("Error loading Optional Plugin: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public boolean installerWasVisible() {
        return installerVisible;
    }

    public Configuration loadConfiguration() {
        Object obj = JDUtilities.getDatabaseConnector().getData("jdownloaderconfig");

        if (obj == null) {
            File file = JDUtilities.getResourceFile(JDUtilities.CONFIG_PATH);
            if (file.exists()) {
                logger.info("Wrapping jdownloader.config");
                obj = JDUtilities.loadObject(null, file, Configuration.saveAsXML);
                System.out.println(obj.getClass().getName());
                JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", obj);
            }
        }

        if (obj != null && ((Configuration) obj).getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) != null) {

            Configuration configuration = (Configuration) obj;
            JDUtilities.setConfiguration(configuration);
            JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_THEME, "default"));
            JDSounds.setSoundTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(JDSounds.PARAM_CURRENTTHEME, "default"));

        } else {
            Configuration configuration = new Configuration();
            JDUtilities.setConfiguration(configuration);
            JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_THEME, "default"));
            JDSounds.setSoundTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(JDSounds.PARAM_CURRENTTHEME, "default"));

            JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", JDUtilities.getConfiguration());
            installerVisible = true;
            try {
                splashScreen.finish();
            } catch (Exception e) {
            }
            SimpleGUI.setUIManager();
            Installer inst = new Installer();

            if (!inst.isAborted()) {

                File home = JDUtilities.getResourceFile(".");
                if (home.canWrite() && !JDUtilities.getResourceFile("noupdate.txt").exists()) {

                    JOptionPane.showMessageDialog(null, JDLocale.L("installer.welcome", "Welcome to jDownloader. Download missing files."));

                    try {
                        Browser.download(new File(home, "webupdater.jar"), "http://service.jdownloader.org/update/webupdater.jar");

                        JDUtilities.saveConfig();
                        logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.RUNTYPE_LOCAL_JARED }, home.getAbsolutePath(), 0));
                        System.exit(0);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
                if (!home.canWrite()) {
                    logger.info("INSTALL abgebrochen");
                    JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.error.noWriteRights", "Error. You do not have permissions to write to " + home));
                    JDUtilities.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                    System.exit(1);
                }

            } else {
                logger.info("INSTALL abgebrochen2");
                JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.abortInstallation", "Error. User aborted installation."));

                JDUtilities.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                System.exit(0);

            }
        }

        afterConfigIsLoaded();
        return JDUtilities.getConfiguration();
    }

    public void loadDownloadQueue() {
        if (!JDUtilities.getController().initDownloadLinks()) {
            File links = JDUtilities.getResourceFile("links.dat");

            if (links != null && links.exists()) {
                File newFile = new File(links.getAbsolutePath() + ".bup");
                newFile.delete();
                links.renameTo(newFile);
                JDUtilities.getController().getUiInterface().showMessageDialog(JDLocale.L("sys.warning.linklist.incompatible", "Linkliste inkompatibel. \r\nBackup angelegt."));
            }
        }

    }

    /**
     * Bilder werden dynamisch aus dem Homedir geladen.
     */
    public void loadImages() {
        ClassLoader cl = JDUtilities.getJDClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        File dir = JDUtilities.getResourceFile("jd/img/");

        String[] images = dir.list();
        if (images == null || images.length == 0) {
            logger.severe("Could not find the img directory");
            return;
        }
        for (String element : images) {
            if (element.toLowerCase().endsWith(".png") || element.toLowerCase().endsWith(".gif")) {
                File f = new File(element);

                logger.finer("Loaded image: " + f.getName().split("\\.")[0] + " from " + cl.getResource("jd/img/" + f.getName()));
                JDUtilities.addImage(f.getName().split("\\.")[0], toolkit.getImage(cl.getResource("jd/img/" + f.getName())));
            }

        }

    }

    public void loadModules() {
        logger.finer("create Module: Unrar");
        JDUtilities.getController().setUnrarModule(Unrar.getInstance());
        logger.finer("create Module: InfoFileWriter");

    }

    @SuppressWarnings("unchecked")
    public ArrayList<PluginsC> loadCPlugins() {
        ArrayList<PluginsC> plugins = new ArrayList<PluginsC>();
        ArrayList<String> containerTypes = new ArrayList<String>();
        containerTypes.add("D");
        containerTypes.add("R");
        // containerTypes.add("DLC");
        containerTypes.add("B");
        containerTypes.add("C");

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();

        for (String cl : containerTypes) {

            logger.finer("Try to initialize " + cl);
            try {

                Class plgClass = jdClassLoader.loadClass("jd.plugins.a." + cl);
                if (plgClass == null) {
                    logger.info("PLUGIN NOT FOUND!");
                    continue;
                }
                Class[] classes = new Class[] {};
                Constructor con = plgClass.getConstructor(classes);

                plugins.add((PluginsC) con.newInstance(new Object[] {}));
                logger.finer("Successfully loaded " + cl);

            } catch (Throwable e) {
                logger.info("Plugin Exception!");
                e.printStackTrace();
            }
        }
        return plugins;

    }

    public ArrayList<DecryptPluginWrapper> loadPluginForDecrypt() {
        ArrayList<DecryptPluginWrapper> plwrappers = new ArrayList<DecryptPluginWrapper>();
        plwrappers.add(new DecryptPluginWrapper("animea.net", "AnimeANet", "http://[\\w\\.]*?animea\\.net/download/[\\d]+/(.*?)\\.html|http://[\\w\\.]*?animea\\.net/download/[\\d]+-[\\d]+/(.*?)\\.html"));
        plwrappers.add(new DecryptPluginWrapper("anime-loads.org", "AnimeLoadsorg", "http://[\\w\\.]*?anime-loads\\.org/Crypt-it/([^/]*)/[a-zA-Z0-9]+\\.html"));
        plwrappers.add(new DecryptPluginWrapper("best-movies.us", "BestMovies", "http://crypt\\.best-movies\\.us/go\\.php\\?id\\=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("bm4u.in", "Bm4uin", "http://[\\w\\.]*?bm4u\\.in/index\\.php\\?do=show_download&id=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("cine.to", "CineTo", "http://[\\w\\.]*?cine\\.to/index\\.php\\?do=show_download\\&id=[a-zA-Z0-9]+|http://[\\w\\.]*?cine\\.to/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("collectr.net", "Collectr", "http://[\\w\\.]*?collectr\\.net/out/[0-9]*[/]{0,1}[\\d]*"));
        plwrappers.add(new DecryptPluginWrapper("crypt-it.com", "CryptItCom", "(http|ccf)://[\\w\\.]*?crypt-it\\.com/(s|e|d|c)/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("crypting.it", "CryptingIt", "http://[\\w.]*?crypting\\.it/index\\.php\\?p=show&id=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("cryptlink.ws", "Cryptlinkws", "http://[\\w\\.]*?cryptlink\\.ws/\\?file=[a-zA-Z0-9]+|http://[\\w\\.]*?cryptlink\\.ws/crypt\\.php\\?file=[0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("clipfish.de", "ClipfishDe", "http://[\\w\\.]*?clipfish\\.de/n\\.php/channel/\\d+/video/(\\d+)|http://[\\w\\.]*?clipfish\\.de/video/(\\d+)(/.+)?"));
        plwrappers.add(new DecryptPluginWrapper("daten-bank.org", "Datenbankorg", "http://daten-bank\\.org/go/\\d+/|http://daten-bank\\.org/view/.*?/"));
        plwrappers.add(new DecryptPluginWrapper("ddl-music.org", "DDLMusicOrg", "http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+|http://[\\w\\.]*?ddl-music\\.org/ddlm_cr\\.php\\?\\d+\\?\\d+"));
        plwrappers.add(new DecryptPluginWrapper("ddl-warez.org", "DDLWarez", "http://[\\w\\.]*?ddl-warez\\.org/detail\\.php\\?id=.+&cat=.+"));
        plwrappers.add(new DecryptPluginWrapper("doperoms.net", "DoperomsCom", "http://[\\w.]*?doperoms\\.com/roms/(.+)/(.+).html"));
        plwrappers.add(new DecryptPluginWrapper("downloads.pes-arena.com", "DownloadsPesArenacom", "http://downloads\\.pes-arena\\.com/\\?id=(\\d+)"));
        plwrappers.add(new DecryptPluginWrapper("3dl.am", "DreiDlAm", "http://[\\w\\.]*?3dl\\.am/link/[a-zA-Z0-9]+|http://[\\w\\.]*?3dl\\.am/download/start/[0-9]+/|http://[\\w\\.]*?3dl\\.am/download/[0-9]+/.+\\.html"));
        plwrappers.add(new DecryptPluginWrapper("1kh.de", "EinsKhDe", "http://[\\w\\.]*?1kh\\.de/f/[0-9/]+|http://[\\w\\.]*?1kh\\.de/[0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("falinks.com", "FalinksCom", "http://[\\w\\.]*?falinks\\.com/\\?fa=link&id=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("filefactory.com", "FileFactoryFolder", "http://[\\w\\.]*?filefactory\\.com(/|//)f/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("filehost.it", "FilehostIt", "http://[\\w\\.]*?filehost\\.it/(multi|live)link/checklinks\\.php\\?links=[\\d]+"));
        plwrappers.add(new DecryptPluginWrapper("filer.net", "Filer", "http://[\\w\\.]*?filer.net/folder/.+/.*"));
        plwrappers.add(new DecryptPluginWrapper("File-Upload.net", "FileUploadnet", "http://[\\w\\.]*?member\\.file-upload\\.net/(.*?)/(.*)"));
        plwrappers.add(new DecryptPluginWrapper("flyload.net", "FlyLoadnet", "http://[\\w\\.]*?flyload\\.net/safe\\.php\\?id=[a-zA-Z0-9]+|http://[\\w\\.]*?flyload\\.net/request_window\\.php\\?(\\d+)|http://[\\w\\.]*?flyload\\.net/download\\.php\\?view\\.(\\d+)"));
        plwrappers.add(new DecryptPluginWrapper("frozen-roms.in", "FrozenRomsIn", "http://[\\w\\.]*?frozen-roms\\.in/(details_[0-9]+|get_[0-9]+_[0-9]+)\\.html"));
        plwrappers.add(new DecryptPluginWrapper("ftp2share.net", "ftp2share", "http://[\\w\\.]*?ftp2share\\.net/folder/[a-zA-Z0-9\\-]+/(.*?)|http://[\\w\\.]*?ftp2share\\.net/file/[a-zA-Z0-9\\-]+/(.*?)"));
        plwrappers.add(new DecryptPluginWrapper("fucktheindustry.ru", "FuckTheIndustryRu", "http://[\\w\\.]*?92\\.241\\.164\\.63/file\\.php\\?id=[\\d]+"));
        plwrappers.add(new DecryptPluginWrapper("fyad.org", "Fyadorg", "http://[\\w\\.]*?fyad\\.org/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("Gamez.am", "Gamezam", "javascript:laden\\('include/infos\\.php\\?id=(\\d+)',1\\)"));
        plwrappers.add(new DecryptPluginWrapper("gapping.org", "GappingOrg", "http://[\\w\\.]*?gapping\\.org/index\\.php\\?folderid=\\d+|http://[\\w\\.]*?gapping\\.org/f/\\d+\\.html|http://[\\w\\.]*?gapping\\.org/file\\.php\\?id=.+|http://[\\w\\.]*?gapping\\.org/g.*?\\.html"));
        plwrappers.add(new DecryptPluginWrapper("gwarez.cc", "Gwarezcc", "http://[\\w\\.]*?gwarez\\.cc/\\d{1,}\\#details|http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/check/\\d{1,}/|http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/parts/\\d{1,}/|http://[\\w\\.]*?gwarez\\.cc/download/dlc/\\d{1,}/"));
        plwrappers.add(new DecryptPluginWrapper("Hider.ath.cx", "HiderAthCx", "http://[\\w\\.]*?hider\\.ath\\.cx/\\d+"));
        plwrappers.add(new DecryptPluginWrapper("hideurl.biz", "Hideurlbiz", "http://[\\w\\.]*?hideurl\\.biz/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("hubupload.com", "Hubuploadcom", "http://[\\w\\.]*?hubupload\\.com/files/[a-zA-Z0-9]+/[a-zA-Z0-9]+/(.*)"));
        plwrappers.add(new DecryptPluginWrapper("imagefap.com", "ImagefapCom", "http://[\\w\\.]*?imagefap\\.com/(gallery\\.php\\?gid=.+|gallery/.+)"));
        plwrappers.add(new DecryptPluginWrapper("is.gd", "IsGd", "http://[\\w\\.]*?is\\.gd/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("knoffl.com", "KnofflCom", "http://[\\w\\.]*?knoffl\\.com/u/\\w+"));
        plwrappers.add(new DecryptPluginWrapper("leecher.ws", "LeecherWs", "http://[\\w\\.]*?leecher\\.ws/(folder/.+|out/.+/[0-9]+)"));
        plwrappers.add(new DecryptPluginWrapper("LinkBank.eu", "LinkBankeu", "http://[\\w\\.]*?linkbank\\.eu/show\\.php\\?show=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("linkbucks.com", "LinkBucks", "http://[\\w\\.]*?linkbucks\\.com/link/[0-9a-zA-Z]+/\\d+"));
        plwrappers.add(new DecryptPluginWrapper("link-protector.com", "LinkProtectorCom", "http://[\\w\\.]*?link-protector\\.com/[\\d]{6}.*"));
        plwrappers.add(new DecryptPluginWrapper("linkprotect.in", "LinkProtectIn", "http://[\\w\\.]*?linkprotect\\.in/index.php\\?site=folder&id=[a-zA-Z0-9]{1,50}"));
        plwrappers.add(new DecryptPluginWrapper("linksafe.ws", "LinkSafeWs", "http://[\\w\\.]*?linksafe\\.ws/files/[a-zA-Z0-9]{4}-[\\d]{5}-[\\d]"));
        plwrappers.add(new DecryptPluginWrapper("Linksave.in", "LinksaveIn", "http://[\\w\\.]*?linksave\\.in/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("www.linkshield.com", "Linkshield", "http://[\\w\\.]*?linkshield\\.com/[sc]/[\\d]+_[\\d]+"));
        plwrappers.add(new DecryptPluginWrapper("lix.in", "Lixin", "http://[\\w\\.]*?lix\\.in/[-]{0,1}[a-zA-Z0-9]{6,10}"));
        plwrappers.add(new DecryptPluginWrapper("loading.cc", "LoadingCc", "http://.*?loading\\.cc/detail\\.php\\?id=[0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("mediafire.com", "MediafireFolder", "http://[\\w\\.]*?mediafire\\.com/\\?sharekey=.+"));
        plwrappers.add(new DecryptPluginWrapper("mirrorit.de", "MirrorItDe", "http://[\\w\\.]*?mirrorit\\.de/\\?id=[a-zA-Z0-9]{16}"));
        plwrappers.add(new DecryptPluginWrapper("myref.de", "MyRef", "http://[\\w\\.]*?myref\\.de(\\/){0,1}\\?\\d{0,10}"));
        plwrappers.add(new DecryptPluginWrapper("myspace.com", "MySpaceCom", "http://[\\w\\.]*?myspace\\.(com|de)/.+"));
        plwrappers.add(new DecryptPluginWrapper("myup.cc", "Myupcc", "http://[\\w\\.]*?myup\\.cc/link-[a-zA-Z0-9]+\\.html"));
        plwrappers.add(new DecryptPluginWrapper("myvideo.de", "MyvideoDe", "http://[\\w\\.]*?myvideo\\.de/watch/[0-9]+/"));
        plwrappers.add(new DecryptPluginWrapper("netfolder.in", "NetfolderIn", "http://[\\w\\.]*?netfolder\\.in/folder\\.php\\?folder_id\\=[a-zA-Z0-9]{7}|http://[\\w\\.]*?netfolder\\.in/[a-zA-Z0-9]{7}/.*?"));
        plwrappers.add(new DecryptPluginWrapper("newzfind.com", "NewzFindCom", "http://[\\w\\.]*?newzfind\\.com/(video|music|games|software|mac|graphics|unix|magazines|e-books|xxx|other)/.+"));
        plwrappers.add(new DecryptPluginWrapper("Protect.Tehparadox.com", "ProtectTehparadoxcom", "http://[\\w\\.]*?protect\\.tehparadox\\.com\\/[a-zA-Z0-9]+\\!"));
        plwrappers.add(new DecryptPluginWrapper("raidrush.org", "RaidrushOrg", "http://[\\w\\.]*?raidrush\\.org/ext/\\?fid\\=[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("rapidlayer.in", "Rapidlayerin", "http://[\\w\\.]*?rapidlayer\\.in/go/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("rapidsafe.de", "RapidsafeDe", "http://.+rapidsafe\\.de"));
        plwrappers.add(new DecryptPluginWrapper("rapidsafe.net", "Rapidsafenet", "http://[\\w\\.]*?rapidsafe\\.net/r.-?[a-zA-Z0-9]{11}/.*"));
        plwrappers.add(new DecryptPluginWrapper("rapidshare.com", "RapidshareComFolder", "http://[\\w\\.]*?rapidshare.com/users/.+"));
        plwrappers.add(new DecryptPluginWrapper("Rapidshark.net", "rapidsharknet", "http://[\\w\\.]*?rapidshark\\.net/(?!safe\\.php\\?id=)[a-zA-Z0-9]+|http://[\\w\\.]*?rapidshark\\.net/safe\\.php\\?id=[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("rapidspread.com", "RapidSpreadCom", "http://[\\w\\.]*?rapidspread\\.com/file\\.jsp\\?id=\\w+"));
        plwrappers.add(new DecryptPluginWrapper("r-b-a.de", "RbaDe", "http://[\\w\\.]*?r-b-a\\.de/(index\\.php\\?ID=4101&(amp;)?BATTLE=\\d+(&sid=\\w+)?)|http://[\\w\\.]*?r-b-a\\.de/index\\.php\\?ID=4100(&direction=last)?&MEMBER=\\d+(&sid=\\w+)?"));
        plwrappers.add(new DecryptPluginWrapper("redirect.wayaround.org", "RedirectWayaroundorg", "http://[\\w\\.]*?redirect\\.wayaround\\.org/[a-zA-Z0-9]+/(.*)"));
        plwrappers.add(new DecryptPluginWrapper("relink.us", "RelinkUs", "http://[\\w\\.]*?relink\\.us\\/go\\.php\\?id=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("rlslog.net", "Rlslog", "(http://[\\w\\.]*?rlslog\\.net(/.+/.+/#comments|/.+/#comments|/.+/))"));
        plwrappers.add(new DecryptPluginWrapper("RnB4U.in", "RnB4Uin", "http://[\\w\\.]*?rnb4u\\.in/download\\.php\\?action=kategorie&kat_id=\\d+|http://[\\w\\.]*?rnb4u\\.in/download\\.php\\?action=popup&kat_id=\\d+&fileid=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("rock-house.in", "RockHouseIn", "http://[\\w\\.]*?rock-house\\.in/warez/warez_download\\.php\\?id=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("romhustler.net", "RomHustlerNet", "(http://[\\w.]*?romhustler\\.net/rom/.*?/\\d+/.+)|(/rom/.*?/\\d+/.+)"));
        plwrappers.add(new DecryptPluginWrapper("roms.zophar.net", "RomsZopharNet", "http://[\\w.]*?roms\\.zophar\\.net/(.+)/(.+\\.7z)"));
        plwrappers.add(new DecryptPluginWrapper("romscentral.com", "RomscentralCom", "(http://[\\w.]*?romscentral\\.com/(.+)/(.+\\.htm))|(onclick=\"return popitup\\('(.+\\.htm)'\\))"));
        plwrappers.add(new DecryptPluginWrapper("rs.hoerbuch.in", "RsHoerbuchin", "http://rs\\.hoerbuch\\.in/com-[\\w]{11}/.*|http://rs\\.hoerbuch\\.in/de-[\\w]{11}/.*|http://rs\\.hoerbuch\\.in/u[\\w]{6}.html"));
        plwrappers.add(new DecryptPluginWrapper("rs-layer.com", "RsLayerCom", "http://[\\w\\.]*?rs-layer\\.com/(.+)\\.html"));
        plwrappers.add(new DecryptPluginWrapper("rsprotect.com", "RsprotectCom", "http://[\\w\\.]*?rsprotect\\.com/r[sc]-[a-zA-Z0-9]{11}/.*"));
        plwrappers.add(new DecryptPluginWrapper("rs-protect.freehoster.ch", "Rsprotectfreehosterch", "http://[\\w\\.]*?rs-protect\\.freehoster\\.ch/r[sc]-[a-zA-Z0-9]{11}/.*"));
        plwrappers.add(new DecryptPluginWrapper("rs.xxx-blog.org", "RsXXXBlog", "http://[\\w\\.]*?xxx-blog\\.org/[a-zA-Z0-9]{1,4}-[a-zA-Z0-9]{10,40}/.*"));
        plwrappers.add(new DecryptPluginWrapper("rurl.de", "RurlDe", "http://[\\w\\.]*?rurl\\.de/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("rurl.org", "RurlOrg", "http://[\\w\\.]*?rurl\\.org/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("saug.us", "SAUGUS", "http://[\\w\\.]*?saug\\.us/folder.?-[a-zA-Z0-9\\-]{30,50}\\.html|http://[\\w\\.]*?saug\\.us/go.+\\.php"));
        plwrappers.add(new DecryptPluginWrapper("save.raidrush.ws", "SaveRaidrushWs", "http://[\\w\\.]*?save\\.raidrush\\.ws/\\?id\\=[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("scum.in", "ScumIn", "http://[\\w\\.]*?scum\\.in/index\\.php\\?id=\\d+"));
        plwrappers.add(new DecryptPluginWrapper("secured.in", "Secured", "http://[\\w\\.]*?secured\\.in/download-[\\d]+-[a-zA-Z0-9]{8}\\.html"));
        plwrappers.add(new DecryptPluginWrapper("se-cur.net", "SeCurNet", "http://[\\w\\.]*?se-cur\\.net/q\\.php\\?d=.+"));
        plwrappers.add(new DecryptPluginWrapper("Serienjunkies.org", "Serienjunkies", null, PluginWrapper.LOAD_ON_INIT));
        plwrappers.add(new DecryptPluginWrapper("sexuria.com", "Sexuriacom", "http://[\\w\\.]*?sexuria\\.com/Pornos_Kostenlos_.+?_(\\d+)\\.html|http://[\\w\\.]*?sexuria\\.com/dl_links_\\d+_(\\d+)\\.html|http://[\\w\\.]*?sexuria\\.com/out.php\\?id=([0-9]+)\\&part=[0-9]+\\&link=[0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("sharebee.com", "SharebeeCom", "http://[\\w\\.]*?sharebee\\.com/[a-zA-Z0-9]{8}"));
        plwrappers.add(new DecryptPluginWrapper("shareonall.com", "ShareOnAll", "http://[\\w\\.]*?shareonall\\.com/(.*?)\\.htm"));
        plwrappers.add(new DecryptPluginWrapper("shareprotect.t-w.at", "ShareProtect", "http://shareprotect\\.t-w\\.at/\\?id\\=[a-zA-Z0-9\\-]{3,10}"));
        plwrappers.add(new DecryptPluginWrapper("share.rockt.es", "ShareRocktEs", "http://[\\w\\.]*?share\\.rockt\\.es/\\?v=\\w+|http://[\\w\\.]*?share\\.rockt\\.es/\\?go=(\\w+)"));
        plwrappers.add(new DecryptPluginWrapper("stacheldraht.to", "StacheldrahtTo", "http://[\\w\\.]*?stacheldraht\\.to/index\\.php\\?folder=.+"));
        plwrappers.add(new DecryptPluginWrapper("Stealth.to", "Stealth", "http://[\\w\\.]*?stealth\\.to/(\\?id\\=[a-zA-Z0-9]+|index\\.php\\?id\\=[a-zA-Z0-9]+|\\?go\\=captcha&id=[a-zA-Z0-9]+)"));
        plwrappers.add(new DecryptPluginWrapper("technorocker.info", "TechnorockerInfo", "http://[\\w\\.]*?technorocker\\.info/opentrack\\.php\\?id=[0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("tinyload.com", "TinyLoadCom", "http://[\\w\\.]*?tinyload\\.com/\\w+"));
        plwrappers.add(new DecryptPluginWrapper("tinyurl.com", "Tinyurl", "http://[\\w\\.]*?tinyurl\\.com/[a-zA-Z0-9\\-]+"));
        plwrappers.add(new DecryptPluginWrapper("ucash.in", "UCashin", "http://[\\w\\.]*?ucash\\.in/([a-zA-Z0-9]+)"));

        plwrappers.add(new DecryptPluginWrapper("Underground CMS", "UCMS", PluginPattern.decrypterPattern_UCMS().pattern()));

        plwrappers.add(new DecryptPluginWrapper("uploadjockey.com", "UploadJockeycom", "http://[\\w\\.]*?uploadjockey\\.com/download/[a-zA-Z0-9]+/(.*)"));
        plwrappers.add(new DecryptPluginWrapper("up.picoasis.net", "UpPicoasisNet", "http://up\\.picoasis\\.net/[\\d]+"));
        plwrappers.add(new DecryptPluginWrapper("urlcash.net", "URLCash", "http://[a-zA-Z0-9\\-]{5,16}\\.(urlcash\\.net|urlcash\\.org|clb1\\.com|urlgalleries\\.com|celebclk\\.com|smilinglinks\\.com|peekatmygirlfriend\\.com|looble\\.net)"));
        plwrappers.add(new DecryptPluginWrapper("urlshield.net", "UrlShieldnet", "http://[\\w\\.]*?urlshield\\.net/l/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("vetax.in", "VetaXin", "http://[\\w\\.]*?vetax\\.in/view/\\d+|http://[\\w\\.]*?vetax\\.in/(dload|mirror)/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("web06.de", "Web06de", "http://[\\w\\.]*?web06\\.de/\\?user=\\d+site=(.*)"));
        plwrappers.add(new DecryptPluginWrapper("wii-reloaded.ath.cx", "Wiireloaded", "http://wii-reloaded\\.ath\\.cx/protect/get\\.php\\?i=.+"));
        plwrappers.add(new DecryptPluginWrapper("chaoz.ws", "Woireless6xTo", "http://[\\w.]*?chaoz\\.ws/woireless/page/album_\\d+\\.html"));
        plwrappers.add(new DecryptPluginWrapper("Wordpress Parser", "Wordpress", "http://[\\w\\.]*?(hd-area\\.org/\\d{4}/\\d{2}/\\d{2}/.+|movie-blog\\.org/\\d{4}/\\d{2}/\\d{2}/.+|hoerbuch\\.in/blog\\.php\\?id=[\\d]+|doku\\.cc/\\d{4}/\\d{2}/\\d{2}/.+|xxx-blog\\.org/blog\\.php\\?id=[\\d]+|sky-porn\\.info/blog/\\?p=[\\d]+|best-movies\\.us/\\?p=[\\d]+|game-blog\\.us/game-.+\\.html|pressefreiheit\\.ws/[\\d]+/.+\\.html).*"));
        plwrappers.add(new DecryptPluginWrapper("xaili.com", "Xailicom", "http://[\\w\\.]*?xaili\\.com/\\?site=protect\\&id=[0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("xink.it", "XinkIt", "http://[\\w\\.]*?xink\\.it/f-[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("xlice.net", "XliceNet", "http://[\\w\\.]*?xlice\\.net/download/[a-z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("xlink.in", "Xlinkin", "http://[\\w\\.]*?xlink\\.in/\\?v=[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("xrl.us", "XrlUs", "http://[\\w\\.]*?xrl\\.us/[a-zA-Z0-9]+"));
        plwrappers.add(new DecryptPluginWrapper("xup.in", "XupInFolder", "http://[\\w\\.]*?xup\\.in/a,[0-9]+(/.+)?(/(list|mini))?"));
        plwrappers.add(new DecryptPluginWrapper("youporn.com", "YouPornCom", "http://[\\w\\.]*?youporn\\.com/watch/\\d+/?.+/?|http://[\\w\\.]*?youporn\\.com/(.*?page=\\d+)"));
        plwrappers.add(new DecryptPluginWrapper("yourfiles.biz", "YourFilesBizFolder", "http://[\\w\\.]*?yourfiles\\.biz/.*/folders/[0-9]+/.+\\.html"));
        plwrappers.add(new DecryptPluginWrapper("youtube.com", "YouTubeCom", "http://[\\w\\.]*?youtube\\.com/watch\\?v=[a-z-_A-Z0-9]+|\\< streamingshare=\"youtube\\.com\" name=\".*?\" dlurl=\".*?\" brurl=\".*?\" convertto=\".*?\" comment=\".*?\" \\>"));

        return plwrappers;
    }

    public ArrayList<HostPluginWrapper> loadPluginForHost() {
        ArrayList<HostPluginWrapper> hpl = new ArrayList<HostPluginWrapper>();

        hpl.add(new HostPluginWrapper("archiv.to", "ArchiveTo", "http://[\\w\\.]*?archiv\\.to/\\?Module\\=Details\\&HashID\\=.*"));
        hpl.add(new HostPluginWrapper("bluehost.to", "BluehostTo", "http://[\\w\\.]*?bluehost\\.to/(\\?dl=|dl=|file/).*"));
        hpl.add(new HostPluginWrapper("cocoshare.cc", "Cocosharecc", "http://[\\w\\.]*?cocoshare\\.cc/\\d+/(.*)"));
        hpl.add(new HostPluginWrapper("clipfish.de", "ClipfishDe", "http://[\\w\\.]*?pg\\d+\\.clipfish\\.de/media/.+?\\.flv"));
        hpl.add(new HostPluginWrapper("data.hu", "DataHu", "http://[\\w\\.]*?data.hu/get/.+/.+"));
        hpl.add(new HostPluginWrapper("dataup.de", "Dataupde", "http://[\\w\\.]*?dataup\\.de/\\d+/(.*)"));
        hpl.add(new HostPluginWrapper("depositfiles.com", "DepositFiles", "http://[\\w\\.]*?depositfiles\\.com(/en/|/de/|/ru/|/)files/[0-9]+", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("fast-load.net", "FastLoadNet", "http://[\\w\\.]*?fast-load\\.net(/|//)index\\.php\\?pid=[a-zA-Z0-9]+"));
        hpl.add(new HostPluginWrapper("fastshare.org", "FastShareorg", "http://[\\w\\.]*?fastshare\\.org/download/(.*)"));
        hpl.add(new HostPluginWrapper("FileBase.To", "FileBaseTo", "http://[\\w\\.]*?filebase\\.to/files/\\d{1,}/.*"));
        hpl.add(new HostPluginWrapper("FileFactory.com", "FileFactory", "sjdp://filefactory\\.com.*|http://[\\w\\.]*?filefactory\\.com(/|//)file/.{6}/?", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("FileMojo.Com", "FileMojoCom", "http://[\\w\\.]*?filemojo\\.com/\\d+(/.+)?"));
        hpl.add(new HostPluginWrapper("Filer.net", "Filer", "http://[\\w\\.]*?filer.net/(file[\\d]+|get)/.*"));
        hpl.add(new HostPluginWrapper("Files.To", "FilesTo", "http://[\\w\\.]*?files\\.to/get/[0-9]+/[a-zA-Z0-9]+"));
        hpl.add(new HostPluginWrapper("File-Upload.net", "FileUploadnet", "((http://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html)|(http://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html))|(http://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)))"));
        hpl.add(new HostPluginWrapper("Freakshare.net", "Freaksharenet", "http://[\\w\\.]*?freakshare\\.net/files/\\d+/(.*)"));
        hpl.add(new HostPluginWrapper("HTTP Links", "HTTPAllgemein", "httpviajd://[\\w\\.]*/.*?\\.(dlc|ccf|rsdf|zip|mp3|mp4|avi|iso|mov|wmv|mpg|rar|mp2|7z|pdf|flv|jpg|exe|3gp|wav|mkv|tar|bz2)"));
        hpl.add(new HostPluginWrapper("ImageFap.com", "ImageFap", "http://[\\w\\.]*?imagefap.com/image.php\\?id=.*(&pgid=.*&gid=.*&page=.*)?"));
        hpl.add(new HostPluginWrapper("Mediafire.Com", "MediafireCom", "http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?.+|\\?.+)"));
        hpl.add(new HostPluginWrapper("Megashares.Com", "MegasharesCom", "http://[\\w\\.]*?megashares\\.com/\\?d.*"));
        hpl.add(new HostPluginWrapper("Megaupload.com", "Megauploadcom", "http://[\\w\\.]*?(megaupload|megarotic|sexuploader)\\.com/.*?\\?d\\=.{8}", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("MeinUpload.com", "MeinUpload", "(http://[\\w\\.]*?meinupload\\.com/{1,}dl/.+/.+)|(http://[\\w\\.]*?meinupload\\.com/\\?d=.*)", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("Mooshare.de", "Moosharede", "http://[\\w\\.]*?mooshare\\.de/index\\.php\\?pid\\=[a-zA-Z0-9]+"));
        hpl.add(new HostPluginWrapper("MySpace.Com", "MySpaceCom", "myspace://.+"));
        hpl.add(new HostPluginWrapper("MyVideo.de", "MyVideo", "http://[\\w\\.]*?myvideo.*?\\.llnwd\\.net/d[\\d]+/movie[\\d]+/.+/[\\d]+\\.flv"));
        hpl.add(new HostPluginWrapper("Netload.in", "Netloadin", "sjdp://[\\w\\.]*?netload\\.in.*|(http://[\\w\\.]*?netload\\.in/(?!index\\.php).*)", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("Odsiebie.com", "Odsiebiecom", "http://[\\w\\.]*?odsiebie\\.com/pokaz/\\d+---[a-zA-Z0-9]+.html"));
        hpl.add(new HostPluginWrapper("Przeslij.net", "Przeslijnet", "http://www2\\.przeslij\\.net/download.php\\?file=(.*)"));
        hpl.add(new HostPluginWrapper("Qshare.Com", "QshareCom", "http://[\\w\\.]*?qshare\\.com\\/get\\/[0-9]{1,20}\\/.*", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("rapidshare.com", "Rapidshare", "sjdp://rapidshare\\.com.*|http://[\\w\\.]*?rapidshare\\.com/files/[\\d]{3,9}/.*", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("RapidShare.De", "RapidShareDe", "sjdp://rapidshare\\.de.*|http://[\\w\\.]*?rapidshare\\.de/files/[\\d]{3,9}/.*", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("R-b-a.De", "RbaDe", "http://[\\w\\.]*?r-b-a\\.de/download\\.php\\?FILE=(\\d+)-(\\d)\\.mp3&PATH=(\\d)"));
        hpl.add(new HostPluginWrapper("Megashares.Com", "MegasharesCom", "http://[\\w\\.]*?megashares\\.com/\\?d.*"));
        hpl.add(new HostPluginWrapper("Roms.Zophar.Net", "RomsZopharNet", "http://[\\w.]*?roms\\.zophar\\.net/download-file/[0-9]{1,}"));
        hpl.add(new HostPluginWrapper("RomHustler.Net", "RomHustlerNet", "http://[\\w.]*?romhustler\\.net/download/.*?/\\d+"));
        hpl.add(new HostPluginWrapper("Serienjunkies.org", "Serienjunkies", "http://[\\w\\.]*?sjdownload.org.*"));
        hpl.add(new HostPluginWrapper("ShareBase.De", "ShareBaseDe", "http://[\\w\\.]*?sharebase\\.de/files/[a-zA-Z0-9]+\\.html"));
        hpl.add(new HostPluginWrapper("SharedZilla.com", "SharedZillacom", "http://[\\w\\.]*?sharedzilla\\.com/(en|ru)/get\\?id=\\d+"));
        hpl.add(new HostPluginWrapper("Share-Now.net", "ShareNownet", "http://[\\w\\.]*?share-now\\.net/{1,}files/\\d+-(.*?)\\.html"));
        hpl.add(new HostPluginWrapper("Share-Online.Biz", "ShareOnlineBiz", "http://[\\w\\.]*?share\\-online\\.biz/download.php\\?id\\=[a-zA-Z0-9]+"));
        hpl.add(new HostPluginWrapper("Shareplace.com", "Shareplacecom", "http://[\\w\\.]*?shareplace\\.com/\\?[a-zA-Z0-9]+/.*?"));
        hpl.add(new HostPluginWrapper("SiloFiles.com", "SiloFilescom", "http://[\\w\\.]*?silofiles\\.com/file/\\d+/.*?"));
        hpl.add(new HostPluginWrapper("Speedy-Share.com", "SpeedySharecom", "http://[\\w\\.]*?speedy\\-share\\.com/[a-zA-Z0-9]+/(.*)"));
        hpl.add(new HostPluginWrapper("Uploaded.to", "Uploadedto", "sjdp://uploaded\\.to.*|http://[\\w\\.]*?uploaded\\.to/(file/|\\?id\\=)[a-zA-Z0-9]{6}", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("UploadService.info", "UploadServiceinfo", "http://[\\w\\.]*?uploadservice\\.info/file/[a-zA-Z0-9]+\\.html"));
        hpl.add(new HostPluginWrapper("UploadStube.de", "UploadStube", "http://[\\w\\.]*?uploadstube\\.de/download\\.php\\?file=.*"));
        hpl.add(new HostPluginWrapper("Upshare.net", "Upsharenet", "http://[\\w\\.]*?upshare\\.(net|eu)/download\\.php\\?id=[a-zA-Z0-9]+"));
        hpl.add(new HostPluginWrapper("Vip-file.com", "Vipfilecom", "http://[\\w\\.]*?vip-file\\.com/download/[a-zA-z0-9]+/(.*?)\\.html"));
        hpl.add(new HostPluginWrapper("Xup.In", "XupIn", "http://[\\w\\.]*?xup\\.in/dl,\\d+/?.+?"));
        hpl.add(new HostPluginWrapper("xup.raidrush.ws", "XupInRaidrush", "http://xup.raidrush.ws/.*?/"));
        hpl.add(new HostPluginWrapper("YouPorn.Com", "YouPornCom", "http://download\\.youporn\\.com/download/\\d+/flv/.*"));
        hpl.add(new HostPluginWrapper("YourFiles.Biz", "YourFilesBiz", "http://[\\w\\.]*?yourfiles\\.biz/\\?d\\=[a-zA-Z0-9]+"));
        hpl.add(new HostPluginWrapper("YourFileSender.com", "YourFileSendercom", "http://[\\w\\.]*?yourfilesender\\.com/v/\\d+/(.*?\\.html)"));
        hpl.add(new HostPluginWrapper("Youtube.com", "Youtube", "http://[\\w\\.]*?youtube\\.com/get_video\\?video_id=.+&t=.+(&fmt=\\d+)?", PluginWrapper.LOAD_ON_INIT));
        hpl.add(new HostPluginWrapper("Zippyshare.com", "Zippysharecom", "http://www\\d{0,}\\.zippyshare\\.com/v/\\d+/file\\.html"));
        hpl.add(new HostPluginWrapper("zshare.net", "Zippysharecom", "http://www\\d{0,}\\.zippyshare\\.com/v/\\d+/file\\.html"));

        return hpl;
    }

    @SuppressWarnings("unchecked")
    public Vector<PluginOptional> loadPluginOptional() {

        Vector<PluginOptional> pluginsOptional = new Vector<PluginOptional>();
        class optionalPluginsVersions {
            public String name;
            public double version;

            public optionalPluginsVersions(String name, double version) {
                this.name = name;
                this.version = version;
            }

            public String toString() {
                return name;
            }
        }
        ArrayList<optionalPluginsVersions> optionalPluginsVersionsArray = new ArrayList<optionalPluginsVersions>();
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDTrayIcon", 1.6));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDLightTray", 1.6));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("webinterface.JDWebinterface", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("schedule.Schedule", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDFolderWatch", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDShutdown", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDRemoteControl", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDLowSpeed", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("HTTPLiveHeaderScripter", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("jdchat.JDChat", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("Newsfeeds", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDInfoFileWriter", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("StreamingShareTool", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("LangFileEditor", 1.5));

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();

        Double version = JDUtilities.getJavaVersion();
        for (optionalPluginsVersions cl : optionalPluginsVersionsArray) {
            if (version < cl.version) {
                logger.finer("Plugin " + cl + " requires Java Version " + cl.version + " your Version is: " + version);
                continue;
            }
            logger.finer("Try to initialize " + cl);
            try {

                Class plgClass = jdClassLoader.loadClass("jd.plugins.optional." + cl);
                if (plgClass == null) {
                    logger.info("PLUGIN NOT FOUND!");
                    continue;
                }
                Class[] classes = new Class[] {};
                Constructor con = plgClass.getConstructor(classes);

                try {

                    Method f = plgClass.getMethod("getAddonInterfaceVersion", new Class[] {});

                    int id = (Integer) f.invoke(null, new Object[] {});

                    if (id != PluginOptional.ADDON_INTERFACE_VERSION) {
                        logger.severe("Addon " + cl + " is outdated and incompatible. Please update(Packagemanager) :Addon:" + id + " : Interface: " + PluginOptional.ADDON_INTERFACE_VERSION);
                    } else {
                        pluginsOptional.add((PluginOptional) con.newInstance(new Object[] {}));
                        logger.finer("Successfully loaded " + cl);
                    }
                } catch (Exception e) {
                    logger.severe("Addon " + cl + " is outdated and incompatible. Please update(Packagemanager) :" + e.getLocalizedMessage());
                }

            } catch (Throwable e) {
                logger.info("Plugin Exception!");
                e.printStackTrace();
            }
        }

        return pluginsOptional;

    }

    public void removeFiles() {
        String[] remove = Regex.getLines(JDUtilities.getLocalFile(JDUtilities.getResourceFile("outdated.dat")));
        String homedir = JDUtilities.getJDHomeDirectoryFromEnvironment().toString();
        if (remove != null) {
            for (String file : remove) {
                if (file.length() == 0) continue;
                File delete = new File(homedir, file);
                if (!delete.toString().matches(".*?" + File.separator + "?\\.+" + File.separator + ".*?") && delete.toString().contains(homedir)) {
                    if (JDUtilities.removeDirectoryOrFile(delete)) logger.warning("Removed " + file);
                }
            }
        }
    }

}
