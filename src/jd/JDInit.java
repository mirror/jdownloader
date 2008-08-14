package jd;

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

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.BackupLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.update.PackageData;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import sun.misc.Service;

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
        logger.info("hallo " + JDUtilities.getResourceFile("links.linkbackup"));
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

                logger.finer("Init Webupdater");
                final WebUpdater updater = new WebUpdater(JDUtilities.getSubConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_BETA", false) ? "http://jdbetaupdate.ath.cx" : null);

                updater.setCid(oldCid);
                logger.finer("Get available files");
                Vector<Vector<String>> files = updater.getAvailableFiles();
                // logger.info(files + "");
                updater.filterAvailableUpdates(files, JDUtilities.getResourceFile("."));
                // if(JDUtilities.getSubConfig("JAC").getBooleanProperty(
                // Configuration.USE_CAPTCHA_EXCHANGE_SERVER,
                // false)){
                // for (int i = files.size() - 1; i >= 0; i--) {
                //                  
                // // if
                //(files.get(i).get(0).startsWith("jd/captcha/methods/")&&files.
                // get(i).get(0).endsWith("mth"))
                // {
                // // logger.info("Autotrain active. ignore
                // "+files.get(i).get(0));
                // // files.remove(i);
                // // }
                // }
                // }
                if (files != null) {
                    JDUtilities.getController().setWaitingUpdates(files);
                }

                cid = updater.getCid();
                if (getCid() > 0 && getCid() != JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.CID, getCid());
                    JDUtilities.saveConfig();
                }
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
                        Browser.download(JDUtilities.getResourceFile("webupdater.jar"), "http://jdownloaderwebupdate.ath.cx");

                        JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                        logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                        System.exit(0);
                    } else {

                        try {
                            JHelpDialog d = new JHelpDialog(JDUtilities.getGUI() != null ? ((SimpleGUI) JDUtilities.getGUI()).getFrame() : null, "Update!", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + (files.size() + packages.size()) + " update(s) available. Start Webupdater now?" + "</font>");
                            d.getBtn3().setText("Cancel");
                            d.getBtn1().setText("Show changes");
                            d.getBtn2().setText(JDLocale.L("gui.dialogs.helpDialog.btn.ok", "Update now!"));
                            d.setAction1(d.new Action() {
                                public boolean doAction() {

                                    try {
                                        JLinkButton.openURL(JDLocale.L("system.update.changelogurl", "http://jdownloader.org/changes?toolmode=1"));
                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }

                                    return true;
                                }
                            });
                            d.showDialog();

                            if (d.getStatus() == JHelpDialog.STATUS_ANSWER_2) {
                                Browser.download(JDUtilities.getResourceFile("webupdater.jar"), "http://jdownloaderwebupdate.ath.cx");

                                JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                                logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
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

        }.start();
    }

    public int getCid() {
        return cid;
    }

    void init() {
        CookieHandler.setDefault(null);

    }

    public JDController initController() {
        // try {
        // splashScreen.setText("init Controller");
        // splashScreen.increase(2);
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        return new JDController();
    }

    public UIInterface initGUI(JDController controller) {

        UIInterface uiInterface = new SimpleGUI();
        controller.setUiInterface(uiInterface);
        controller.addControlListener(uiInterface);
        return uiInterface;
    }

    public void initPlugins() {
        try {
            logger.info("Lade Plugins");
            // try {
            // splashScreen.setText("Load Plugins");
            // splashScreen.increase(3);
            // } catch (Exception e) {
            // // TODO: handle exception
            // }
            // JDController controller = JDUtilities.getController();
            JDUtilities.setPluginForDecryptList(loadPluginForDecrypt());
            JDUtilities.setPluginForHostList(loadPluginForHost());
            JDUtilities.setPluginForContainerList(loadPluginForContainer());
            try {
                JDUtilities.setPluginOptionalList(loadPluginOptional());
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            HashMap<String, PluginOptional> pluginsOptional = JDUtilities.getPluginsOptional();

            Iterator<String> iterator = pluginsOptional.keySet().iterator();
            String key;

            while (iterator.hasNext()) {
                key = iterator.next();
                PluginOptional plg = pluginsOptional.get(key);
                if (JDUtilities.getConfiguration().getBooleanProperty("OPTIONAL_PLUGIN_" + plg.getPluginName(), false)) {
                    try {
                        if (!pluginsOptional.get(key).initAddon()) {
                            logger.severe("Error loading Optional Plugin: FALSE");
                        }

                    } catch (Throwable e2) {
                        logger.severe("Error loading Optional Plugin: " + e2.getMessage());
                        e2.printStackTrace();
                    }
                }
            }
        } catch (Throwable e2) {
            logger.severe("Error loading Optional Plugin: " + e2.getMessage());
            e2.printStackTrace();
        }
    }

    public boolean installerWasVisible() {

        return installerVisible;
    }

    public Configuration loadConfiguration() {
        // boolean allOK = true;

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

                    Browser.download(new File(home, "webupdater.jar"), "http://jdownloaderwebupdate.ath.cx");

                    JDUtilities.saveConfig();
                    logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.RUNTYPE_LOCAL_JARED }, home.getAbsolutePath(), 0));
                    System.exit(0);

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
        // try {
        // splashScreen.setText("Configuration loaded");
        // splashScreen.increase(5);
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
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
        // try {
        // splashScreen.increase();
        // splashScreen.setText("Load images");
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        File dir = JDUtilities.getResourceFile("jd/img/");

        String[] images = dir.list();
        if (images == null || images.length == 0) {
            logger.severe("Could not find the img directory");
            return;
        }
        for (String element : images) {
            if (element.toLowerCase().endsWith(".png") || element.toLowerCase().endsWith(".gif")) {
                File f = new File(element);
                // try {
                // splashScreen.increase(2);
                // } catch (Exception e) {
                // // TODO: handle exception
                // }
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
    public Vector<PluginForContainer> loadPluginForContainer() {
        Vector<PluginForContainer> plugins = new Vector<PluginForContainer>();

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        logger.finer("Load Container Plugins");
        Iterator iterator = Service.providers(PluginForContainer.class, jdClassLoader);

        while (iterator.hasNext()) {
            try {
                PluginForContainer p = (PluginForContainer) iterator.next();
                logger.finer("Load " + p);
                plugins.add(p);
            } catch (Exception e) {
                e.printStackTrace();
                logger.info("caught");
            }
        }
        return plugins;
    }

    @SuppressWarnings("unchecked")
    public Vector<PluginForDecrypt> loadPluginForDecrypt() {
        Vector<PluginForDecrypt> plugins = new Vector<PluginForDecrypt>();

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        logger.finer("Load Decrypt Plugins");
        Iterator iterator = Service.providers(PluginForDecrypt.class, jdClassLoader);

        while (iterator.hasNext()) {
            try {
                PluginForDecrypt p = (PluginForDecrypt) iterator.next();
                logger.finer("Load " + p);
                plugins.add(p);
            } catch (Exception e) {
                logger.info("caught");
                e.printStackTrace();
            }
        }
        return plugins;
    }

    @SuppressWarnings("unchecked")
    public Vector<PluginForHost> loadPluginForHost() {
        Vector<PluginForHost> plugins = new Vector<PluginForHost>();

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        logger.finer("Load Host Plugins");
        Iterator iterator = Service.providers(PluginForHost.class, jdClassLoader);

        while (iterator.hasNext()) {
            try {
                PluginForHost p = (PluginForHost) iterator.next();
                logger.finer("Load " + p);
                plugins.add(p);
            } catch (Exception e) {
                logger.info("caught");
                e.printStackTrace();
            }
        }
        return plugins;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, PluginOptional> loadPluginOptional() {
        // try {
        // splashScreen.setText("Load Optional Plugins");
        // splashScreen.increase();
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        HashMap<String, PluginOptional> pluginsOptional = new HashMap<String, PluginOptional>();
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
        Iterator<optionalPluginsVersions> iter = optionalPluginsVersionsArray.iterator();
        while (iter.hasNext()) {
            optionalPluginsVersions cl = (optionalPluginsVersions) iter.next();
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

                        PluginOptional p = (PluginOptional) con.newInstance(new Object[] {});
                        pluginsOptional.put(p.getPluginName(), p);
                        // try {
                        // splashScreen.setText(p.getPluginName());
                        // splashScreen.increase(2);
                        // } catch (Exception e) {
                        // // TODO: handle exception
                        // }
                        logger.finer("Successfull!. Loaded " + cl);
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
        String[] remove = null;
        remove = Regex.getLines(JDUtilities.getLocalFile(JDUtilities.getResourceFile("outdated.dat")));

        if (remove != null) {
            for (String file : remove) {
                if (JDUtilities.removeDirectoryOrFile(JDUtilities.getResourceFile(file.trim()))) {
                    logger.warning("Removed " + file);
                }

            }
        }

    }

}
