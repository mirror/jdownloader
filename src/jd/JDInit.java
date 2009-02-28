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

package jd;

import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.config.CFGConfig;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.gui.skins.simple.JDLookAndFeelManager;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.http.Encoding;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.BackupLink;
import jd.plugins.DownloadLink;
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
            // http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html
            // für evtl authentifizierung:
            // http://www.softonaut.com/2008/06/09/using-javanetauthenticator-for
            // -proxy-authentication/
            // nonProxy Liste ist unnötig, da ja eh kein reconnect möglich
            // wäre
            String host = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_HOST, "");
            String port = new Integer(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PROXY_PORT, 8080)).toString();
            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS, "");
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_HOST, host);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_PROXY, true);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PORT, port);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_USER, user);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PASS, pass);
        } else {
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_PROXY, false);
        }
    }

    public static void setupSocks() {
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_SOCKS, false)) {
            // http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html
            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER_SOCKS, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS_SOCKS, "");
            String host = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.SOCKS_HOST, "");
            String port = new Integer(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.SOCKS_PORT, 1080)).toString();
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.SOCKS_HOST, host);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_SOCKS, true);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.SOCKS_PORT, port);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_USER_SOCKS, user);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PASS_SOCKS, pass);
        } else {
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_SOCKS, false);
        }
    }

    private boolean installerVisible = false;

    private SplashScreen splashScreen;

    // private static long LASTREQUEST = 0;

    // private Vector<Vector<String>> files;

    public JDInit() {
        this(null);
    }

    public JDInit(SplashScreen splashScreen) {
        this.splashScreen = splashScreen;
    }

    public void checkUpdate() {
        File updater = JDUtilities.getResourceFile("webupdater.jar");
        if (updater.exists()) {
            if (!updater.delete()) {
                logger.severe("Webupdater.jar could not be deleted. PLease remove JDHOME/webupdater.jar to ensure a proper update");
            }
        }
        if (JDUtilities.getResourceFile("webcheck.tmp").exists() && JDIO.getLocalFile(JDUtilities.getResourceFile("webcheck.tmp")).indexOf("(Revision" + JDUtilities.getRevision() + ")") > 0) {
            JDUtilities.getController().getUiInterface().showTextAreaDialog("Error", "Failed Update detected!", "It seems that the previous webupdate failed.\r\nPlease ensure that your java-version is equal- or above 1.5.\r\nMore infos at http://www.syncom.org/projects/jdownloader/wiki/FAQ.\r\n\r\nErrorcode: \r\n" + JDIO.getLocalFile(JDUtilities.getResourceFile("webcheck.tmp")));
            JDUtilities.getResourceFile("webcheck.tmp").delete();
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false);
        } else {

            Interaction.handleInteraction(Interaction.INTERACTION_APPSTART, false);
        }
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
            String old = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "");
            if (!old.equals(JDUtilities.getRevision())) {
                logger.info("Returned from Update");

                if (splashScreen != null) {
                    splashScreen.finish();
                }

                SimpleGUI.showChangelogDialog();

            }
        }
        submitVersion();
    }

    private void submitVersion() {
        new Thread(new Runnable() {
            public void run() {
                if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                    String os = "unk";
                    if (OSDetector.isLinux()) {
                        os = "lin";
                    } else if (OSDetector.isMac()) {
                        os = "mac";
                    } else if (OSDetector.isWindows()) {
                        os = "win";
                    }
                    String tz = System.getProperty("user.timezone");
                    if (tz == null) tz = "unknown";
                    Browser br = new Browser();
                    br.setConnectTimeout(15000);
                    if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "").equals(JDUtilities.getRevision())) {
                        try {
                            String prev = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "");
                            if (prev == null || prev.length() < 3) {
                                prev = "0";
                            } else {
                                prev = prev.replaceAll(",|\\.", "");
                            }
                            br.postPage("http://service.jdownloader.org/tools/s.php", "v=" + JDUtilities.getRevision().replaceAll(",|\\.", "") + "&p=" + prev + "&os=" + os + "&tz=" + Encoding.urlEncode(tz));
                            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_VERSION, JDUtilities.getRevision());
                            JDUtilities.getConfiguration().save();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }).start();
    }

    public synchronized static void createQueueBackup() {
        Vector<DownloadLink> links = JDUtilities.getController().getDownloadLinks();
        Iterator<DownloadLink> it = links.iterator();
        ArrayList<BackupLink> ret = new ArrayList<BackupLink>();
        while (it.hasNext()) {
            DownloadLink next = it.next();
            BackupLink bl;
            if (next.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                bl = (new BackupLink(new File(next.getContainerFile()), next.getContainerIndex(), next.getContainer()));

            } else {
                bl = (new BackupLink(next.getDownloadURL()));
            }
            bl.setProperty("downloaddirectory", next.getFilePackage().getDownloadDirectory());
            bl.setProperty("packagename", next.getFilePackage().getName());
            bl.setProperty("plugin", next.getPlugin().getClass().getSimpleName());
            bl.setProperty("name", new File(next.getFileOutput()).getName());
            bl.setProperty("properties", next.getProperties());
            bl.setProperty("enabled", next.isEnabled());

            ret.add(bl);
        }
        if (ret.size() == 0) return;
        File file = JDUtilities.getResourceFile("backup/links.linkbackup");
        if (file.exists()) {
            File old = JDUtilities.getResourceFile("backup/links_" + file.lastModified() + ".linkbackup");

            file.getParentFile().mkdirs();
            if (file.exists()) {
                file.renameTo(old);
            }
            file.delete();
        } else {
            file.getParentFile().mkdirs();
        }
        JDIO.saveObject(null, ret, file, "links.linkbackup", "linkbackup", false);
    }

    void init() {
        Browser.init();
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
        loadPluginForDecrypt();
        loadPluginForHost();
        loadCPlugins();
        loadPluginOptional();
        for (OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!plg.isLoaded()) continue;
            try {
                if (plg.isEnabled() && !plg.getPlugin().initAddon()) {
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
            System.out.println("Fresh install?");
//            File file = JDUtilities.getResourceFile(JDUtilities.CONFIG_PATH);
//            if (file.exists()) {
//                logger.info("Wrapping jdownloader.config");
//                obj = JDIO.loadObject(null, file, Configuration.saveAsXML);
//                System.out.println(obj.getClass().getName());
//                JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", obj);
//            }
        }

        if (obj != null && ((Configuration) obj).getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) != null) {

            Configuration configuration = (Configuration) obj;
            JDUtilities.setConfiguration(configuration);
            JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_THEME, "default"));
            JDSounds.setSoundTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(JDSounds.PARAM_CURRENTTHEME, "noSounds"));

        } else {

            File cfg = JDUtilities.getResourceFile("config");
            if (!cfg.exists()) {

                if (!cfg.mkdirs()) {
                    System.err.println("Could not create configdir");
                    return null;
                }
                if (!cfg.canWrite()) {
                    System.err.println("Cannot write to configdir");
                    return null;
                }
            }
            Configuration configuration = new Configuration();
            JDUtilities.setConfiguration(configuration);
            JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_THEME, "default"));
            JDSounds.setSoundTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(JDSounds.PARAM_CURRENTTHEME, "noSounds"));

            JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", JDUtilities.getConfiguration());
            installerVisible = true;
            try {
                splashScreen.finish();
            } catch (Exception e) {
            }
            JDLookAndFeelManager.setUIManager();
            Installer inst = new Installer();

            if (!inst.isAborted()) {

                File home = JDUtilities.getResourceFile(".");
                if (home.canWrite() && !JDUtilities.getResourceFile("noupdate.txt").exists()) {

                    JOptionPane.showMessageDialog(null, JDLocale.L("installer.welcome", "Welcome to jDownloader. Download missing files."));

                    try {
                        Browser.download(new File(home, "webupdater.jar"), "http://service.jdownloader.org/update/webupdater.jar");
                        JDUtilities.getConfiguration().save();
                        JDUtilities.getDatabaseConnector().shutdownDatabase();
                        logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.RUNTYPE_LOCAL_JARED }, home.getAbsolutePath(), 0));
                        System.exit(0);
                    } catch (java.net.ConnectException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, JDLocale.L("installer.refused", "Your connection got refused. It seems that any security software (e.g. firewalls) block JD. See http://jdownloader.org/knowledge/wiki/faq."));
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        // System.exit(0);
                    }

                }
                if (!home.canWrite()) {
                    logger.info("INSTALL abgebrochen");
                    JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.error.noWriteRights", "Error. You do not have permissions to write to the dir"));
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

        return JDUtilities.getConfiguration();
    }

    public void loadDownloadQueue() {
        JDUtilities.getController().initDownloadLinks();
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

    public void loadCPlugins() {

        new CPluginWrapper("linkbackup", "B", ".+\\.linkbackup");
        new CPluginWrapper("ccf", "C", ".+\\.ccf");
        new CPluginWrapper("rsdf", "R", ".+\\.rsdf");
        new CPluginWrapper("dlc", "D", ".+\\.dlc");

    }

    public void loadPluginForDecrypt() {
        new DecryptPluginWrapper("charts4you.org", "Charts4You", "http://[\\w\\.]*?charts4you\\.org\\/\\?id=\\d+");
        new DecryptPluginWrapper("alpha-link.eu", "AlphaLink", "http://[\\w\\.]*?alpha\\-link\\.eu\\/\\?id=[a-fA-F0-9]+");
        new DecryptPluginWrapper("protectbox.in", "ProtectBoxIn", "http://[\\w\\.]*?protectbox\\.in\\/\\?id=[a-fA-F0-9]+");
        new DecryptPluginWrapper("animea.net", "AnimeANet", PluginPattern.decrypterPattern_AnimeANet_Plugin);
        new DecryptPluginWrapper("anime-loads.org", "AnimeLoadsorg", "http://[\\w\\.]*?anime-loads\\.org/Crypt-it/([^/]*)/[a-zA-Z0-9]+\\.html");
        new DecryptPluginWrapper("baberepublic.com", "LinkBucks", "http://[\\w\\.]*?baberepublic\\.com/link/[0-9a-zA-Z]+(/\\d+)?");
        new DecryptPluginWrapper("bat5.com", "URLCash", "http://.+bat5\\.com");
        new DecryptPluginWrapper("counterstrike.de", "CounterstrikeDe", "http://[\\w\\.]*?4players\\.de/\\S*/download/[0-9]+/([01]/)?index\\.html?");
        new DecryptPluginWrapper("googlegroups.com", "GoogleGroups", "http://groups.google.com/group/[^/]+/files/?");
        new DecryptPluginWrapper("best-movies.us", "BestMovies", "http://crypt\\.(best-movies\\.us|capcrypt\\.info)/go\\.php\\?id\\=\\d+");
        new DecryptPluginWrapper("blog-xx.net", "BlogXXNet", "http://[\\w\\.]*?blog-xx\\.net/wp/(.*?)/");
        new DecryptPluginWrapper("bm4u.in", "Bm4uin", "http://[\\w\\.]*?bm4u\\.in/index\\.php\\?do=show_download&id=\\d+");
        new DecryptPluginWrapper("brazil-series.com", "BrazilSeriesCom", "http://[\\w\\.]*?brazil-series\\.com/(\\w+/\\w+/\\w+_ep\\d+\\.htm|\\w+/\\d+t/\\w+_intro.htm|\\w+/\\w+_intro.htm)");
        new DecryptPluginWrapper("cine.to", "CineTo", "http://[\\w\\.]*?cine\\.to/index\\.php\\?do=show_download\\&id=[a-zA-Z0-9]+|http://[\\w\\.]*?cine\\.to/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+|http://[\\w\\.]*?cine\\.to/pre/index\\.php\\?do=show_download\\&id=[a-zA-Z0-9]+|http://[\\w\\.]*?cine\\.to/pre/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+");
        new DecryptPluginWrapper("clipfish.de", "ClipfishDe", "http://[\\w\\.]*?clipfish\\.de/n\\.php/channel/\\d+/video/(\\d+)|http://[\\w\\.]*?clipfish\\.de/video/(\\d+)(/.+)?");
        new DecryptPluginWrapper("collectr.net", "Collectr", "http://[\\w\\.]*?collectr\\.net/(out/(\\d+/)?\\d+|links/\\w+)");
        new DecryptPluginWrapper("crypting.it", "CryptingIt", "http://[\\w.]*?crypting\\.it/index\\.php\\?p=show&id=\\d+");
        new DecryptPluginWrapper("protector.it", "ProtectorIT", "http://[\\w.]*?protect-it\\.org/.*");
        new DecryptPluginWrapper("crypt-it.com", "CryptItCom", "(http|ccf)://[\\w\\.]*?crypt-it\\.com/(s|e|d|c)/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("cryptlink.ws", "Cryptlinkws", "http://[\\w\\.]*?cryptlink\\.ws/\\?file=[a-zA-Z0-9]+|http://[\\w\\.]*?cryptlink\\.ws/crypt\\.php\\?file=[0-9]+");
        new DecryptPluginWrapper("crypt-me.com", "CryptMeCom", "http://[\\w\\.]*?crypt-me\\.com/folder/[a-zA-Z0-9]+\\.html");
        new DecryptPluginWrapper("ddl-music.org", "DDLMusicOrg", PluginPattern.decrypterPattern_DDLMusic_Plugin);
        new DecryptPluginWrapper("ddl-warez.org", "DDLWarez", "http://[\\w\\.]*?ddl-warez\\.org/detail\\.php\\?id=.+&cat=[^/]+");
        new DecryptPluginWrapper("doperoms.net", "DoperomsCom", "http://[\\w.]*?doperoms\\.com/roms/(.+)/(.+).html");
        new DecryptPluginWrapper("downloads.pes-arena.com", "DownloadsPesArenacom", "http://downloads\\.pes-arena\\.com/\\?id=(\\d+)");
        new DecryptPluginWrapper("3dl.am", "DreiDlAm", PluginPattern.decrypterPattern_DreiDlAm_Plugin);
        new DecryptPluginWrapper("1kh.de", "EinsKhDe", "http://[\\w\\.]*?1kh\\.de/f/[0-9/]+|http://[\\w\\.]*?1kh\\.de/[0-9]+");
        new DecryptPluginWrapper("falinks.com", "FalinksCom", "http://[\\w\\.]*?falinks\\.com/\\?fa=link&id=\\d+");
        new DecryptPluginWrapper("filefactory.com", "FileFactoryFolder", "http://[\\w\\.]*?filefactory\\.com(/|//)f/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("filehost.it", "FilehostIt", "http://[\\w\\.]*?filehost\\.it/(multi|live)link/checklinks\\.php\\?links=[\\d]+");
        new DecryptPluginWrapper("filer.net", "Filer", "http://[\\w\\.]*?filer.net/folder/.+/.*");
        new DecryptPluginWrapper("File-Upload.net", "FileUploadnet", "http://[\\w\\.]*?member\\.file-upload\\.net/(.*?)/(.*)");
        new DecryptPluginWrapper("filmatorium.cn", "FilmatoriumCn", "http://[\\w\\.]*?filmatorium\\.cn/\\?p=\\d+");
        new DecryptPluginWrapper("flyload.net", "FlyLoadnet", "http://[\\w\\.]*?flyload\\.net/safe\\.php\\?id=[a-zA-Z0-9]+|http://[\\w\\.]*?flyload\\.net/request_window\\.php\\?(\\d+)|http://[\\w\\.]*?flyload\\.net/download\\.php\\?view\\.(\\d+)");
        new DecryptPluginWrapper("frozen-roms.in", "FrozenRomsIn", "http://[\\w\\.]*?frozen-roms\\.in/(details_[0-9]+|get_[0-9]+_[0-9]+)\\.html");
        new DecryptPluginWrapper("ftp2share.net", "ftp2share", "http://[\\w\\.]*?ftp2share\\.net/folder/[a-zA-Z0-9\\-]+/(.*?)|http://[\\w\\.]*?ftp2share\\.net/file/[a-zA-Z0-9\\-]+/(.*?)");
        new DecryptPluginWrapper("gapping.org", "GappingOrg", "http://[\\w\\.]*?gapping\\.org/index\\.php\\?folderid=\\d+|http://[\\w\\.]*?gapping\\.org/f/\\d+\\.html|http://[\\w\\.]*?gapping\\.org/file\\.php\\?id=.+|http://[\\w\\.]*?gapping\\.org/g.*?\\.html|http://[\\w\\.]*?gapping\\.org/d/.*\\.html");
        new DecryptPluginWrapper("gwarez.cc", "Gwarezcc", "http://[\\w\\.]*?gwarez\\.cc/\\d{1,}\\#details|http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/check/\\d{1,}/|http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/parts/\\d{1,}/|http://[\\w\\.]*?gwarez\\.cc/download/dlc/\\d{1,}/");
        new DecryptPluginWrapper("Hider.ath.cx", "HiderAthCx", "http://[\\w\\.]*?hider\\.ath\\.cx/\\d+");
        new DecryptPluginWrapper("hideurl.biz", "Hideurlbiz", "http://[\\w\\.]*?hideurl\\.biz/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("hubupload.com", "Hubuploadcom", "http://[\\w\\.]*?hubupload\\.com/files/[a-zA-Z0-9]+/[a-zA-Z0-9]+/(.*)");
        new DecryptPluginWrapper("iload.to", "ILoadTo", "http://iload\\.to/go/\\d+/|http://iload\\.to/(view|title|release)/.*?/");
        new DecryptPluginWrapper("imagefap.com", "ImagefapCom", "http://[\\w\\.]*?imagefap\\.com/(gallery\\.php\\?gid=.+|gallery/.+)");
        new DecryptPluginWrapper("joke-around.org", "JokeAroundOrg", "http://[\\w\\.]*?joke-around\\.org/.+");
        new DecryptPluginWrapper("knoffl.com", "KnofflCom", "http://[\\w\\.]*?knoffl\\.com/(u/[a-zA-Z0-9-]+|[a-zA-Z0-9-]+)");
        new DecryptPluginWrapper("leecher.ws", "LeecherWs", "http://[\\w\\.]*?leecher\\.ws/(folder/.+|out/.+/[0-9]+)");
        new DecryptPluginWrapper("LinkBank.eu", "LinkBankeu", "http://[\\w\\.]*?linkbank\\.eu/show\\.php\\?show=\\d+");
        new DecryptPluginWrapper("linkbase.biz", "LinkbaseBiz", "http://[\\w\\.]*?linkbase\\.biz/\\?v=[a-zA-Z0-9]+");
        new DecryptPluginWrapper("linkbucks.com", "LinkBucks", "http://[\\w\\.]*?linkbucks\\.com/link/[0-9a-zA-Z]+(/\\d+)?");
        new DecryptPluginWrapper("linkcrypt.ws", "LinkCryptWs", "http://[\\w\\.]*?linkcrypt\\.ws/dir/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("linkprotect.in", "LinkProtectIn", "http://[\\w\\.]*?linkprotect\\.in/index.php\\?site=folder&id=[a-zA-Z0-9]{1,50}");
        new DecryptPluginWrapper("linkcrypt.com", "LinkcryptCom", "http://[\\w\\.]*?linkcrypt\\.com\\/[a-zA-Z]-[a-zA-z0-9]+");
                                                      
       
        new DecryptPluginWrapper("link-protector.com", "LinkProtectorCom", "http://[\\w\\.]*?link-protector\\.com/[\\d]{6}.*");
        new DecryptPluginWrapper("linkr.at|rapidblogger.com", "LinkrAt", "http://[\\w\\.]*?(linkr\\.at/\\?p=|rapidblogger\\.com/link/)\\w+");
        new DecryptPluginWrapper("linksafe.ws", "LinkSafeWs", "http://[\\w\\.]*?linksafe\\.ws/files/[a-zA-Z0-9]{4}-[\\d]{5}-[\\d]");
        new DecryptPluginWrapper("Linksave.in", "LinksaveIn", "http://[\\w\\.]*?linksave\\.in/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("link-share.org", "LinkShareOrg", "http://[\\w\\.]*?link-share\\.org/view.php\\?url=[a-zA-Z0-9]{32}");
        new DecryptPluginWrapper("linkshield.com", "Linkshield", "http://[\\w\\.]*?linkshield\\.com/[sc]/[\\d]+_[\\d]+");
        new DecryptPluginWrapper("lix.in", "Lixin", "http://[\\w\\.]*?lix\\.in/[-]{0,1}[a-zA-Z0-9]{6,10}");
        new DecryptPluginWrapper("mediafire.com", "MediafireFolder", "http://[\\w\\.]*?mediafire\\.com/\\?sharekey=.+");
        new DecryptPluginWrapper("mirrorit.de", "MirrorItDe", "http://[\\w\\.]*?mirrorit\\.de/\\?id=[a-zA-Z0-9]{16}");
        new DecryptPluginWrapper("music-base.ws", "MusicBaseWs", "http://[\\w\\.]*?music-base\\.ws/dl\\.php.*?c=[\\w]+");
        new DecryptPluginWrapper("myref.de", "MyRef", "http://[\\w\\.]*?myref\\.de(\\/){0,1}\\?\\d{0,10}");
        new DecryptPluginWrapper("myspace.com", "MySpaceCom", "http://[\\w\\.]*?myspace\\.(com|de)/.+");
        new DecryptPluginWrapper("myup.cc", "Myupcc", "http://[\\w\\.]*?myup\\.cc/link-[a-zA-Z0-9]+\\.html");
        new DecryptPluginWrapper("myvideo.de", "MyvideoDe", "http://[\\w\\.]*?myvideo\\.de/watch/[0-9]+/");
        new DecryptPluginWrapper("netfolder.in", "NetfolderIn", "http://[\\w\\.]*?netfolder\\.in/folder\\.php\\?folder_id\\=[a-zA-Z0-9]{7}|http://[\\w\\.]*?netfolder\\.in/[a-zA-Z0-9]{7}/.*?");
        new DecryptPluginWrapper("newzfind.com", "NewzFindCom", "http://[\\w\\.]*?newzfind\\.com/(video|music|games|software|mac|graphics|unix|magazines|e-books|xxx|other)/.+");
        new DecryptPluginWrapper("outlinkr.com", "OutlinkrCom", "http://[\\w\\.]*?outlinkr\\.com/(files|cluster)/[0-9]+/.+");
        new DecryptPluginWrapper("1gabba.com", "OneGabbaCom", "http://[\\w\\.]*?1gabba\\.com/node/[\\d]{4}");
        new DecryptPluginWrapper("Protect.Tehparadox.com", "ProtectTehparadoxcom", "http://[\\w\\.]*?protect\\.tehparadox\\.com\\/[a-zA-Z0-9]+\\!");
        new DecryptPluginWrapper("qvvo.com", "LinkBucks", "http://[\\w\\.]*?qvvo\\.com/link/[0-9a-zA-Z]+(/\\d+)?");
        new DecryptPluginWrapper("raidrush.org", "RaidrushOrg", "http://[\\w\\.]*?raidrush\\.org/ext/\\?fid\\=[a-zA-Z0-9]+");
        new DecryptPluginWrapper("rapidfolder.com", "RapidFolderCom", "http://[\\w\\.]*?rapidfolder\\.com/\\?\\w+");
        new DecryptPluginWrapper("rapidlayer.in", "Rapidlayerin", "http://[\\w\\.]*?rapidlayer\\.in/go/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("rapidsafe.de", "RapidsafeDe", "http://.+rapidsafe\\.de");
        new DecryptPluginWrapper("rapidsafe.net", "Rapidsafenet", "http://[\\w\\.]*?rapidsafe\\.net/r.-?[a-zA-Z0-9]{11}/.*");
        new DecryptPluginWrapper("rapidshare.com", "RapidshareComFolder", "http://[\\w\\.]*?rapidshare.com/users/.+");
        new DecryptPluginWrapper("rapidshare.mu", "RapidshareMu", "http://[\\w\\.]*?rapidshare.mu/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("Rapidshark.net", "rapidsharknet", "http://[\\w\\.]*?rapidshark\\.net/(safe\\.php\\?id=)?.+");
        new DecryptPluginWrapper("rapidspread.com", "RapidSpreadCom", "http://[\\w\\.]*?rapidspread\\.com/file\\.jsp\\?id=\\w+");
        new DecryptPluginWrapper("rappers.in", "RappersIn", "http://[\\w\\.]*?rappers\\.in/([\\w-]+|artist\\.php\\?.+)");
        new DecryptPluginWrapper("r-b-a.de", "RbaDe", "http://[\\w\\.]*?r-b-a\\.de/(index\\.php\\?ID=4101&(amp;)?BATTLE=\\d+(&sid=\\w+)?)|http://[\\w\\.]*?r-b-a\\.de/index\\.php\\?ID=4100(&direction=last)?&MEMBER=\\d+(&sid=\\w+)?");
        new DecryptPluginWrapper("Redirect Services", "Redirecter", PluginPattern.decrypterPattern_Redirecter_Plugin());
        new DecryptPluginWrapper("relink.us", "RelinkUs", "http://[\\w\\.]*?relink\\.us\\/(go\\.php\\?id=[a-zA-Z0-9]+|f/[a-zA-Z0-9]+)");

        new DecryptPluginWrapper("relinka.net", "RelinkaNet", "http://[\\w\\.]*?relinka\\.net\\/folder\\/[a-z0-9]{8}-[a-z0-9]{4}");
        new DecryptPluginWrapper("rlslog.net", "Rlslog", "(http://[\\w\\.]*?rlslog\\.net(/.+/.+/#comments|/.+/#comments|/.+/.*))");
        new DecryptPluginWrapper("zerosec.ws", "ZeroSecWs", "(http://[\\w\\.]*?zerosec\\.ws(/.+/.+/#comments|/.+/#comments|/.+/.*))");
        new DecryptPluginWrapper("RnB4U.in", "RnB4Uin", "http://[\\w\\.]*?rnb4u\\.in/download\\.php\\?action=kategorie&kat_id=\\d+|http://[\\w\\.]*?rnb4u\\.in/download\\.php\\?action=popup&kat_id=\\d+&fileid=\\d+");
        new DecryptPluginWrapper("rock-house.in", "RockHouseIn", "http://[\\w\\.]*?rock-house\\.in/warez/warez_download\\.php\\?id=\\d+");
        new DecryptPluginWrapper("romhustler.net", "RomHustlerNet", "(http://[\\w.]*?romhustler\\.net/rom/.*?/\\d+/.+)|(/rom/.*?/\\d+/.+)");
        new DecryptPluginWrapper("roms.zophar.net", "RomsZopharNet", "http://[\\w.]*?roms\\.zophar\\.net/(.+)/(.+\\.7z)");
        new DecryptPluginWrapper("romscentral.com", "RomscentralCom", "(http://[\\w.]*?romscentral\\.com/(.+)/(.+\\.htm))|(onclick=\"return popitup\\('(.+\\.htm)'\\))", PluginWrapper.ACCEPTONLYSURLSFALSE);
        new DecryptPluginWrapper("rs.hoerbuch.in", "RsHoerbuchin", "http://rs\\.hoerbuch\\.in/com-[\\w]{11}/.*|http://rs\\.hoerbuch\\.in/de-[\\w]{11}/.*|http://rs\\.hoerbuch\\.in/u[\\w]{6}.html");
        new DecryptPluginWrapper("rs-layer.com", "RsLayerCom", "http://[\\w\\.]*?rs-layer\\.com/(.+)\\.html");
        new DecryptPluginWrapper("rsprotect.com", "RsprotectCom", "http://[\\w\\.]*?rsprotect\\.com/r[sc]-[a-zA-Z0-9]{11}/.*");
        new DecryptPluginWrapper("rs-protect.freehoster.ch", "Rsprotectfreehosterch", "http://[\\w\\.]*?rs-protect\\.freehoster\\.ch/r[sc]-[a-zA-Z0-9]{11}/.*");
        new DecryptPluginWrapper("rs.xxx-blog.org", "RsXXXBlog", "http://[\\w\\.]*?xxx-blog\\.org/[a-zA-Z0-9]{1,4}-[a-zA-Z0-9]{10,40}/.*");
        new DecryptPluginWrapper("rurl.de", "RurlDe", "http://[\\w\\.]*?rurl\\.de/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("saug.us", "SAUGUS", "http://[\\w\\.]*?saug\\.us/folder.?-[a-zA-Z0-9\\-]{30,50}\\.html|http://[\\w\\.]*?saug\\.us/go.+\\.php");
        new DecryptPluginWrapper("save.raidrush.ws", "SaveRaidrushWs", "http://[\\w\\.]*?save\\.raidrush\\.ws/\\?id\\=[a-zA-Z0-9]+");
        new DecryptPluginWrapper("scum.in", "ScumIn", "http://[\\w\\.]*?scum\\.in/index\\.php\\?id=\\d+");
        new DecryptPluginWrapper("sdx.cc", "SdxCc", "http://[\\w\\.]*?sdx\\.cc/infusions/(pro_download_panel|user_uploads)/download\\.php\\?did=\\d+");
        new DecryptPluginWrapper("secured.in", "Secured", "http://[\\w\\.]*?secured\\.in/download-[\\d]+-[a-zA-Z0-9]{8}\\.html");
        new DecryptPluginWrapper("se-cur.net", "SeCurNet", "http://[\\w\\.]*?se-cur\\.net/q\\.php\\?d=.+");
        new DecryptPluginWrapper("Serienjunkies.org", "Serienjunkies", new String(), PluginWrapper.LOAD_ON_INIT);
        new DecryptPluginWrapper("sexuria.com", "Sexuriacom", "http://[\\w\\.]*?sexuria\\.com/Pornos_Kostenlos_.+?_(\\d+)\\.html|http://[\\w\\.]*?sexuria\\.com/dl_links_\\d+_(\\d+)\\.html|http://[\\w\\.]*?sexuria\\.com/out.php\\?id=([0-9]+)\\&part=[0-9]+\\&link=[0-9]+");
        new DecryptPluginWrapper("sharebank.ws", "SharebankWs", "http://[\\w\\.]*?sharebank\\.ws/\\?(v|go)=[a-zA-Z0-9]+");
        new DecryptPluginWrapper("sharebee.com", "SharebeeCom", "http://[\\w\\.]*?sharebee\\.com/[a-zA-Z0-9]{8}");
        new DecryptPluginWrapper("shareonall.com", "ShareOnAll", "http://[\\w\\.]*?shareonall\\.com/(.*?)\\.htm");
        new DecryptPluginWrapper("shareprotect.t-w.at", "ShareProtect", "http://shareprotect\\.t-w\\.at/\\?id\\=[a-zA-Z0-9\\-]{3,10}");
        new DecryptPluginWrapper("share.rockt.es", "ShareRocktEs", "http://[\\w\\.]*?share\\.rockt\\.es/\\?v=\\w+|http://[\\w\\.]*?share\\.rockt\\.es/\\?go=(\\w+)");
        new DecryptPluginWrapper("spiegel.de", "SpiegelDe", "(http://[\\w\\.]*?spiegel\\.de/video/video-\\d+.html|http://[\\w\\.]*?spiegel\\.de/fotostrecke/fotostrecke-\\d+(-\\d+)?.html)");
        new DecryptPluginWrapper("stacheldraht.to", "StacheldrahtTo", "http://[\\w\\.]*?stacheldraht\\.to/index\\.php\\?folder=.+");
        new DecryptPluginWrapper("Stealth.to", "Stealth", "http://[\\w\\.]*?stealth\\.to/(\\?id\\=[a-zA-Z0-9]+|index\\.php\\?id\\=[a-zA-Z0-9]+|\\?go\\=captcha&id=[a-zA-Z0-9]+)");
        new DecryptPluginWrapper("technorocker.info", "TechnorockerInfo", "http://[\\w\\.]*?technorocker\\.info/download/[0-9]+/.*");
        new DecryptPluginWrapper("tinyload.com", "TinyLoadCom", "http://[\\w\\.]*?tinyload\\.com/\\w+");
        new DecryptPluginWrapper("ucash.in", "UCashin", "http://[\\w\\.]*?ucash\\.in/([a-zA-Z0-9]+)");
        new DecryptPluginWrapper("usercash.com", "UserCashCom", "http://[\\w\\.]*?usercash\\.com/");
        new DecryptPluginWrapper("Underground CMS", "UCMS", PluginPattern.decrypterPattern_UCMS_Plugin());
        new DecryptPluginWrapper("uploadjockey.com", "UploadJockeycom", "http://[\\w\\.]*?uploadjockey\\.com/download/[a-zA-Z0-9]+/(.*)");
        new DecryptPluginWrapper("up.picoasis.net", "UpPicoasisNet", "http://up\\.picoasis\\.net/[\\d]+");
        new DecryptPluginWrapper("urlcash.net", "URLCash", "http://[a-zA-Z0-9\\-]{5,16}\\.(urlcash\\.net|urlcash\\.org|clb1\\.com|urlgalleries\\.com|celebclk\\.com|smilinglinks\\.com|peekatmygirlfriend\\.com|looble\\.net)");
        new DecryptPluginWrapper("urlshield.net", "UrlShieldnet", "http://[\\w\\.]*?urlshield\\.net/l/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("uu.canna.to", "UUCannaTo", "http://uu\\.canna\\.to/cpuser/links\\.php\\?action=popup&kat_id=[\\d]+&fileid=[\\d]+");
        new DecryptPluginWrapper("vetax.in", "VetaXin", "http://[\\w\\.]*?vetax\\.in/view/\\d+|http://[\\w\\.]*?vetax\\.in/(dload|mirror)/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("web06.de", "Web06de", "http://[\\w\\.]*?web06\\.de/\\?user=\\d+site=(.*)");
        new DecryptPluginWrapper("wii-reloaded.ath.cx", "Wiireloaded", "http://wii-reloaded\\.ath\\.cx/protect/get\\.php\\?i=.+");
        new DecryptPluginWrapper("chaoz.ws", "Woireless6xTo", "http://[\\w.]*?chaoz\\.ws/woireless/page/album_\\d+\\.html");
        new DecryptPluginWrapper("Wordpress Parser", "Wordpress", PluginPattern.decrypterPattern_Wordpress_Plugin());
        new DecryptPluginWrapper("xaili.com", "Xailicom", "http://[\\w\\.]*?xaili\\.com/\\?site=protect\\&id=[0-9]+");
        new DecryptPluginWrapper("xink.it", "XinkIt", "http://[\\w\\.]*?xink\\.it/f-[a-zA-Z0-9]+");
        new DecryptPluginWrapper("xlice.net", "XliceNet", "http://[\\w\\.]*?xlice\\.net/download/[a-z0-9]+");
        new DecryptPluginWrapper("xlink.in", "Xlinkin", "http://[\\w\\.]*?xlink\\.in/\\?v=[a-zA-Z0-9]+");
        new DecryptPluginWrapper("xrl.us", "XrlUs", "http://[\\w\\.]*?xrl\\.us/[a-zA-Z0-9]+");
        new DecryptPluginWrapper("xup.in", "XupInFolder", "http://[\\w\\.]*?xup\\.in/a,[0-9]+(/.+)?(/(list|mini))?");
        new DecryptPluginWrapper("youporn.com", "YouPornCom", "http://[\\w\\.]*?youporn\\.com/watch/\\d+/?.+/?");
        new DecryptPluginWrapper("yourlayer.com", "YourLayercom", "http://[\\w\\.]*?yourlayer\\.com/go\\.php\\?uid=[a-zA-Z0-9]+(&part=\\d+)?");
        new DecryptPluginWrapper("yourfiles.biz", "YourFilesBizFolder", "http://[\\w\\.]*?yourfiles\\.biz/.*/folders/[0-9]+/.+\\.html");
        new DecryptPluginWrapper("youtube.com", "YouTubeCom", "http://[\\w\\.]*?youtube\\.com/watch\\?v=[a-z-_A-Z0-9]+|\\< streamingshare=\"youtube\\.com\" name=\".*?\" dlurl=\".*?\" brurl=\".*?\" convertto=\".*?\" comment=\".*?\" \\>");
        new DecryptPluginWrapper("megaupload.com folder", "MegauploadComFolder", "http://[\\w\\.]*?megaupload\\.com/.*?\\?f=[a-zA-Z0-9]+");
        new DecryptPluginWrapper("rsmonkey.com", "RsMonkeyCom", "http://[\\w\\.]*?rsmonkey\\.com/\\d+");
        new DecryptPluginWrapper("savefile.com Project", "SavefileComProject", "http://[\\w\\.]*?savefile\\.com/projects/[0-9]+");
        new DecryptPluginWrapper("badongo.com", "BadongoCom", "http://[\\w\\.]*?badongo\\.com/.*(file|vid)/[0-9]+");
        new DecryptPluginWrapper("wrzuta.pl", "WrzutaPl", "http://[\\w\\.]*?wrzuta\\.pl/katalog/\\w+.+");
        new DecryptPluginWrapper("rs43.com", "Rs43Com", "http://[\\w\\.]*?rs43\\.com/(Share\\.Mirror\\.Service/\\?|\\?)/.+");
        new DecryptPluginWrapper("relink-it.com", "RelinkItCom", "http://[\\w\\.]*?relink-it\\.com(/\\w\\w/|/)\\?.+");
        new DecryptPluginWrapper("protectlinks.com", "ProtectLinksCom", "http://[\\w\\.]*?protectlinks\\.com/\\d+");
        new DecryptPluginWrapper("remixshare.com", "RemixShareComFolder", "http://[\\w\\.]*?remixshare\\.com/container/\\?id=[a-z0-9].+");
        new DecryptPluginWrapper("zero10.us", "Zero10Us", "http://[\\w\\.]*?zero10\\.us/\\d+");
        new DecryptPluginWrapper("h-link.us", "Zero10Us", "http://[\\w\\.]*?h-link\\.us/\\d+");
        new DecryptPluginWrapper("jdloader", "JDLoader", "(jdlist://.+)|((dlc|rsdf|ccf)://.*/.+)", PluginWrapper.LOAD_ON_INIT);
        new DecryptPluginWrapper("jamendo.com", "JamendoCom", "http://[\\w\\.]*?jamendo\\.com/.?.?/?(album/\\d+|artist/.+)");

        //extern
        new DecryptPluginWrapper("rapidlibrary.com", "RapidLibrary", "http://rapidlibrary\\.com/download_file_i\\.php\\?.+");
    }

    public void loadPluginForHost() {
        // Premium Hoster
        new HostPluginWrapper("RapidShare.com", "Rapidshare", "http://[\\w\\.]*?rapidshare\\.com/files/[\\d]{3,9}/?.+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Uploaded.to", "Uploadedto", "http://[\\w\\.]*?uploaded\\.to/.*?(file/|\\?id=|&id=)[a-zA-Z0-9]+/?", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("bluehost.to", "BluehostTo", "http://[\\w\\.]*?bluehost\\.to/(\\?dl=|dl=|file/).*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("depositfiles.com", "DepositFiles", "http://[\\w\\.]*?depositfiles\\.com(/\\w{1,3})?/files/[a-zA-Z0-9]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("FileFactory.com", "FileFactory", "http://[\\w\\.]*?filefactory\\.com(/|//)file/[a-zA-Z0-9]+/?", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Filer.net", "Filer", "http://[\\w\\.]*?filer.net/(file[\\d]+|get)/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Freakshare.net", "Freaksharenet", "http://[\\w\\.]*?freakshare\\.net/files/\\d+/(.*)", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Megashares.Com", "MegasharesCom", "http://[\\w\\.]*?(d[0-9]{2}\\.)?megashares\\.com/.*\\?d[0-9]{2}=[0-9a-f]{7}", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Megaupload.com", "Megauploadcom", "http://[\\w\\.]*?(megaupload)\\.com/.*?\\?d=[a-zA-Z0-9]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("MeinUpload.com", "MeinUpload", "(http://[\\w\\.]*?meinupload\\.com/{1,}dl/.+/.+)|(http://[\\w\\.]*?meinupload\\.com/\\?d=.*)|http://[\\w\\.]*?mein-upload\\.com/[a-zA-Z0-9]+(\\.html|/)", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("badongo.com", "BadongoCom", "http://[\\w\\.]*?badongo\\.viajd.*/.*(file|vid)/[0-9]+\\??[0-9]*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Mooshare.net", "Moosharenet", "http://[\\w\\.]*?mooshare\\.net/files/\\d+/.*?\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Netload.in", "Netloadin", "http://[\\w\\.]*?netload\\.in/.+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Qshare.Com", "QshareCom", "http://[\\w\\.]*?qshare\\.com\\/get\\/[0-9]{1,20}\\/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("RapidShare.De", "RapidShareDe", "http://[\\w\\.]*?rapidshare\\.de/files/[\\d]{3,9}/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("sharebase.to", "ShareBaseTo", "http://[\\w\\.]*?sharebase\\.(de|to)/files/[a-zA-Z0-9]+\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Youtube.com", "Youtube", "http://[\\w\\.]*?youtube\\.com/get_video\\?video_id=.+&t=.+(&fmt=\\d+)?", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Share-Online.Biz", "ShareOnlineBiz", "http://[\\w\\.]*?share\\-online\\.biz/download.php\\?id\\=[a-zA-Z0-9]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("filehostme.com", "FileHostMecom", "http://[\\w\\.]*?filehostme\\.com/[a-zA-Z0-9]+\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("gigasize.com", "GigaSizeCom", "http://[\\w\\.]*?gigasize\\.com/get\\.php.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("letitbit.net", "LetitBitNet", "http://[\\w\\.]*?letitbit\\.net/download/[a-zA-Z0-9]+?/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Megarotic.com", "MegaroticCom", "http://[\\w\\.]*?(megarotic|sexuploader)\\.com/.*?\\?d=[a-zA-Z0-9]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Share-Now.net", "ShareNownet", "http://[\\w\\.]*?share-now\\.net/{1,}files/\\d+-(.*?)\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Speedy-Share.com", "Speedy_ShareCom", "http://[\\w\\.]*?speedy\\-share\\.com/[a-zA-Z0-9]+/(.*)");
        new HostPluginWrapper("shragle.com", "ShragleCom", "http://[\\w\\.]*?shragle\\.(com|de)/files/[a-zA-Z0-9]+/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Vip-file.com", "Vipfilecom", "http://[\\w\\.]*?vip-file\\.com/download/[a-zA-z0-9]+/(.*?)\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("easy-share.com", "EasyShareCom", "http://[\\w\\d\\.]*?easy-share\\.com/\\d{6}.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("uploader.pl", "UploaderPl", "http://[\\w\\.]*?uploader\\.pl/\\?d=[A-F0-9]+", PluginWrapper.LOAD_ON_INIT);

        // Free Hoster
        new HostPluginWrapper("archiv.to", "ArchivTo", "http://[\\w\\.]*?archiv\\.to/\\?Module\\=Details\\&HashID\\=.*");
        new HostPluginWrapper("axifile.com", "AxiFileCom", "http://[\\w\\.]*?axifile\\.com/\\?\\d+");
        new HostPluginWrapper("cshare.de", "SwoopshareCom", "http://[\\w\\.]*?cshare.de/file/.*");
        new HostPluginWrapper("clipfish.de", "ClipfishDe", "http://[\\w\\.]*?pg\\d+\\.clipfish\\.de/media/.+?\\.flv");
        new HostPluginWrapper("data.hu", "DataHu", "http://[\\w\\.]*?data.hu/get/.+/.+");
        new HostPluginWrapper("dataup.de", "Dataupde", "http://[\\w\\.]*?dataup\\.de/\\d+/(.*)");
        new HostPluginWrapper("datengigant.com", "DatenGigantCom", "http://[\\w\\.]*?datengigant\\.com/\\w+?/file/\\d+/.+?\\.html");
        new HostPluginWrapper("dump.ru", "DumpRu", "http://[\\w\\.]*?dump\\.ru/file/[0-9]+");
        new HostPluginWrapper("4share.com", "FourShareCom", "http://[\\w\\.]*?4shared.com/file/\\d+?/.*?/.*");
        new HostPluginWrapper("fastshare.org", "FastShareorg", "http://[\\w\\.]*?fastshare\\.org/download/(.*)");
        new HostPluginWrapper("FileBase.To", "FileBaseTo", "http://[\\w\\.]*?filebase\\.to/files/\\d{1,}/.*");
        new HostPluginWrapper("FileMojo.Com", "FileMojoCom", "http://[\\w\\.]*?filemojo\\.com/(\\d+(/.+)?|l\\.php\\?flink=\\d+)");
        new HostPluginWrapper("Files.To", "FilesTo", "http://[\\w\\.]*?files\\.to/get/[0-9]+/[a-zA-Z0-9]+");
        new HostPluginWrapper("File-Upload.net", "FileUploadnet", "((http://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html)|(http://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html))|(http://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)))");
        new HostPluginWrapper("googlegroups.com", "GoogleGroups", "http://.*?\\.googlegroups.com/web/.*");
        new HostPluginWrapper("HTTP Links", "HTTPAllgemein", "httpviajd://[\\d\\w\\.:\\-@]*/.*\\.(otrkey|ac3|3gp|7zip|7z|aiff|aif|aifc|au|avi|bin|bz2|ccf|cue|divx|dlc|doc|docx|dot|exe|flv|gif|gz|iso|java|jpg|jpeg|mkv|mp2|mp3|mp4|mov|movie|mpe|mpeg|mpg|msi|png|pdf|ppt|pptx|pps|ppz|pot|qt|rar|rsdf|rtf|snd|tar|tif|tiff|viv|vivo|wav|wmv|xla|xls|zip|ts)");
        new HostPluginWrapper("ImageFap.com", "ImageFap", "http://[\\w\\.]*?imagefap.com/image.php\\?id=.*(&pgid=.*&gid=.*&page=.*)?");
        new HostPluginWrapper("Mediafire.Com", "MediafireCom", "http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?.+|\\?.+|file/.+)");
        new HostPluginWrapper("MySpace.Com", "MySpaceCom", "myspace://.+");
        new HostPluginWrapper("MyVideo.de", "MyVideo", "http://[\\w\\.]*?myvideo.*?\\.llnwd\\.net/d[\\d]+/(movie[\\d]+/.+/|movies/.+/)[\\d]+\\.flv");
        new HostPluginWrapper("Odsiebie.com", "Odsiebiecom", "http://[\\w\\.]*?odsiebie\\.com/pokaz/\\d+---[a-zA-Z0-9]+.html");
        new HostPluginWrapper("Przeslij.net", "Przeslijnet", "http://www[\\d]?\\.przeslij\\.net/download\\.php\\?file=(.*)");
        new HostPluginWrapper("rappers.in", "RappersIn", "(httpRappersIn://[\\w\\.]*?.+)");
        new HostPluginWrapper("R-b-a.De", "RbaDe", "http://[\\w\\.]*?r-b-a\\.de/download\\.php\\?FILE=(\\d+)-(\\d)\\.mp3&PATH=(\\d)");
        new HostPluginWrapper("Roms.Zophar.Net", "RomsZopharNet", "http://[\\w.]*?roms\\.zophar\\.net/download-file/[0-9]{1,}");
        new HostPluginWrapper("RomHustler.Net", "RomHustlerNet", "http://[\\w.]*?romhustler\\.net/download/.*?/\\d+");
        new HostPluginWrapper("SharedZilla.com", "SharedZillacom", "http://[\\w\\.]*?sharedzilla\\.com/(en|ru)/get\\?id=\\d+");
        new HostPluginWrapper("Shareplace.com", "Shareplacecom", "http://[\\w\\.]*?shareplace\\.com/\\?[a-zA-Z0-9]+(/.*?)?");
        new HostPluginWrapper("Shareplace.com", "Shareplacecom", "http://[\\w\\.]*?datei\\.in/\\?[a-zA-Z0-9]+(/.*?)?");
        new HostPluginWrapper("Spiegel.de", "SpiegelDe", "http://video\\.spiegel\\.de/flash/.+?\\.flv|http://video\\.promobil2spiegel\\.netbiscuits\\.com/.+?\\.(3gp|mp4)|http://www.spiegel.de/img/.+?(\\.\\w+)");
        new HostPluginWrapper("swoopshare.com", "SwoopshareCom", "http://[\\w\\.]*?swoopshare\\.com/file/.*");
        new HostPluginWrapper("2shared.com", "TwoSharedCom", "http://[\\w\\.]*?2shared\\.com/file/\\d+/[a-zA-Z0-9]+");
        new HostPluginWrapper("UploadService.info", "UploadServiceinfo", "http://[\\w\\.]*?uploadservice\\.info/file/[a-zA-Z0-9]+\\.html");
        new HostPluginWrapper("UploadStube.de", "UploadStube", "http://[\\w\\.]*?uploadstube\\.de/download\\.php\\?file=.*");
        new HostPluginWrapper("Upshare.net", "Upsharenet", "http://[\\w\\.]*?upshare\\.(net|eu)/download\\.php\\?id=[a-zA-Z0-9]+");
        new HostPluginWrapper("Xup.In", "XupIn", "http://[\\w\\.]*?xup\\.in/dl,\\d+/?.+?");
        new HostPluginWrapper("xup.raidrush.ws", "XupInRaidrush", "http://xup.raidrush.ws/.*?/");
        new HostPluginWrapper("YouPorn.Com", "YouPornCom", "http://download\\.youporn\\.com/download/\\d+.*");
        new HostPluginWrapper("YourFiles.Biz", "YourFilesBiz", "http://[\\w\\.]*?yourfiles\\.biz/\\?d\\=[a-zA-Z0-9]+");
        new HostPluginWrapper("YourFileSender.com", "YourFileSendercom", "http://[\\w\\.]*?yourfilesender\\.com/v/\\d+/(.*?\\.html)");
        new HostPluginWrapper("Zippyshare.com", "Zippysharecom", "http://www\\d{0,}\\.zippyshare\\.com/v/\\d+/file\\.html");
        new HostPluginWrapper("zshare.net", "ZShareNet", "http://[\\w\\.]*?zshare\\.net/(download|video|image|audio|flash)/.*");
        new HostPluginWrapper("sendspace.pl", "SendSpacePl", "http://[\\w\\.]*?sendspace.pl/file/[a-zA-Z0-9]+/?");
        new HostPluginWrapper("dl.free.fr", "DlFreeFr", "http://[\\w\\.]*?dl\\.free\\.fr/(getfile\\.pl\\?file=/[a-zA-Z0-9]+|[a-zA-Z0-9]+/?)");
        new HostPluginWrapper("dosyakaydet.com", "DosyakaydetCom", "http://[\\w\\.]*?dosyakaydet\\.com/(download/[a-zA-Z0-9]+/?|index/p_download/hash_[a-zA-Z0-9]+/)");
        new HostPluginWrapper("egoshare.com", "EgoshareCom", "http://[\\w\\.]*?egoshare\\.com/download\\.php\\?id=[a-zA-Z0-9]+");
        new HostPluginWrapper("speedshare.org", "SpeedShareOrg", "http://[\\w\\.]*?speedshare\\.org/download\\.php\\?id=[a-zA-Z0-9]+");
        new HostPluginWrapper("vimeo.com", "VimeoCom", "http://[\\w\\.]*?vimeo\\.com/[0-9]+");
        new HostPluginWrapper("self-load.com", "SelfLoadCom", "http://[\\w\\.]*?self-load\\.com/\\d+/.+");
        new HostPluginWrapper("plikus.pl", "PlikusPl", "http://[\\w\\.]*?plikus\\.pl/zobacz_plik-.*?-\\d+\\.html");
        new HostPluginWrapper("uploading.com", "UploadingCom", "http://[\\w\\.]*?uploading\\.com/files/\\w+/.+");
        new HostPluginWrapper("kewlshare.com", "KewlshareCom", "http://[\\w\\.]*?kewlshare\\.com/dl/[a-zA-Z0-9]+/.*");
        new HostPluginWrapper("sharebomb.com", "ShareBombCom", "http://[\\w\\.]*?sharebomb\\.com/[0-9]+.*");
        new HostPluginWrapper("load.to", "LoadTo", "http://[\\w\\.]*?load\\.to/[\\?d=]?[a-zA-Z0-9]+.*");
        new HostPluginWrapper("filestore.to", "FilestoreTo", "http://[\\w\\.]*?filestore\\.to/\\?d=[a-zA-Z0-9]+");
        new HostPluginWrapper("zetshare.com", "ZetshareCom", "http://[\\w\\.]*?zetshare\\.com/(download/|url/)?download\\.php\\?file=[0-9a-zA-Z]+");
        new HostPluginWrapper("filezzz.com", "FilezzzCom", "http://[\\w\\.]*?filezzz\\.com/download/[0-9]+/.*");
        new HostPluginWrapper("savefile.com", "SavefileCom", "http://[\\w\\.]*?savefile\\.com/files/[0-9]+");
        new HostPluginWrapper("sendspace.com", "SendspaceCom", "http://[\\w\\.]*?sendspace\\.com/file/[0-9a-zA-Z]+");
        new HostPluginWrapper("speedyshare.com", "SpeedyShareCom", "http://[\\w\\.]*?speedyshare\\.com/[0-9]+.*");
        new HostPluginWrapper("fileshaker.com", "FileshakerCom", "http://[\\w\\.]*?fileshaker\\.com/.+");
        new HostPluginWrapper("adrive.com", "AdriveCom", "http://[\\w\\.].*?adrive\\.com/public/[0-9a-zA-Z]+.*");
        new HostPluginWrapper("przeklej.pl", "PrzeklejPl", "http://[\\w\\.]*?przeklej\\.pl/d/\\w+/.+");
        new HostPluginWrapper("wrzuta.pl", "WrzutaPl", "http://[\\w\\.]*?wrzuta\\.pl/(audio|film|obraz)/\\w+.+");
        new HostPluginWrapper("filefactory.pl", "FileFactoryPl", "http://[\\w\\.]*?filefactory\\.pl/showfile-\\d+.+");
        new HostPluginWrapper("wyslijto.pl", "WyslijToPl", "http://[\\w\\.]*?wyslijto\\.pl/(download|files/download|files/pre_download|plik)/\\w+");
        new HostPluginWrapper("wrzucaj.com", "WrzucajCom", "http://[\\w\\.]*?wrzucaj\\.com/\\d+");
        new HostPluginWrapper("hostplik.com", "HostPlikCom", "http://[\\w\\.]*?hostplik\\.com/.*");
        new HostPluginWrapper("upload-drive.com", "UploadDriveCom", "http://[\\w\\.]*?upload-drive\\.com/\\d+/.+");
        new HostPluginWrapper("pliczek.net", "PliczekNet", "http://[\\w\\.]*?pliczek\\.net/index\\.php\\?p=\\d+");
        new HostPluginWrapper("plikos.pl", "PlikosPl", "http://[\\w\\.]*?plikos\\.pl/\\w+/.+");
        //http://remixshare.com/?file=e1rt7319a9
        new HostPluginWrapper("remixshare.com", "RemixShareCom", "http://[\\w\\.]*?remixshare\\.com/.*?\\?file=[a-z0-9]+");
        new HostPluginWrapper("filesend.net", "FileSendNet", "http://[\\w\\.]*?filesend\\.net/download\\.php\\?f=[a-z0-9]+");
        new HostPluginWrapper("filelocity.com", "FilelocityCom", "http://[\\w\\.]*?filelocity\\.com/\\?act=download&file=[a-z0-9]+");
        new HostPluginWrapper("uploadbox.com", "UploadBoxCom", "http://[\\w\\.]*?uploadbox\\.com/.*?files/[0-9a-zA-Z]+");
        new HostPluginWrapper("jamendo.com", "JamendoCom", "http://[\\w\\.]*?jamendo\\.com/.*.*/?(track|download/album)/\\d+");
        new HostPluginWrapper("fileload.us", "FileloadUs", "http://[\\w\\.]*?fileload\\.us/.*");

        
        //extern
        new HostPluginWrapper("ifolder.ru", "IfolderRu", "http://[\\w\\.]*?ifolder\\.ru/\\d+");
    }

    public void loadPluginOptional() {

        new OptionalPluginWrapper("JDTrayIcon", 1.6);
        new OptionalPluginWrapper("JDLightTray", 1.6);
        new OptionalPluginWrapper("webinterface.JDWebinterface", 1.5);
        new OptionalPluginWrapper("schedule.Schedule", 1.5);
        new OptionalPluginWrapper("JDFolderWatch", 1.5);
        new OptionalPluginWrapper("JDShutdown", 1.5);
        new OptionalPluginWrapper("JDRemoteControl", 1.5);
        new OptionalPluginWrapper("JDLowSpeed", 1.5);
        new OptionalPluginWrapper("HTTPLiveHeaderScripter", 1.5);
        new OptionalPluginWrapper("jdchat.JDChat", 1.5);
        new OptionalPluginWrapper("Newsfeeds", 1.5);
        new OptionalPluginWrapper("JDInfoFileWriter", 1.5);
        new OptionalPluginWrapper("StreamingShareTool", 1.5);
        new OptionalPluginWrapper("LangFileEditor", 1.5);
        new OptionalPluginWrapper("jdunrar.JDUnrar", 1.5);
        new OptionalPluginWrapper("hjsplit.JDHJSplit", 1.5);
        new OptionalPluginWrapper("JDPremiumCollector", 1.5);
        new OptionalPluginWrapper("JDGrowlNotification", 1.5);
    }

    public void removeFiles() {
        String[] remove = Regex.getLines(JDIO.getLocalFile(JDUtilities.getResourceFile("outdated.dat")));
        String homedir = JDUtilities.getJDHomeDirectoryFromEnvironment().toString();
        if (remove != null) {
            for (String file : remove) {
                if (file.length() == 0) continue;
                if (!file.matches(".*?" + File.separator + "?\\.+" + File.separator + ".*?")) {
                    File delete = new File(homedir, file);
                    if (JDUtilities.removeDirectoryOrFile(delete)) logger.warning("Removed " + file);
                }
            }
        }
    }

}
