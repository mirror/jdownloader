//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ByteBufferController;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.PasswordListController;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.gui.JDLookAndFeelManager;
import jd.gui.UIInterface;
import jd.gui.UserIO;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.http.Browser;
import jd.http.Encoding;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 */

public class JDInit {

    private static final boolean TEST_INSTALLER = false;

    private static Logger logger = jd.controlling.JDLogger.getLogger();

    private boolean installerVisible = false;

    public JDInit() {
    }

    public void checkUpdate() {
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
                logger.info("Detected that JD just got updated");
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_FINISH));
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

    public void init() {
        Browser.init();
    }

    public void initControllers() {
        DownloadController.getInstance();
        PasswordListController.getInstance();
        DownloadController.getInstance().addListener(PasswordListController.getInstance());
        AccountController.getInstance();
        ByteBufferController.getInstance();
        // LinkGrabberController.getInstance();
    }

    public UIInterface initGUI(JDController controller) {

        UIInterface uiInterface = SimpleGUI.createGUI();
        controller.setUiInterface(uiInterface);
        controller.addControlListener(uiInterface);
        return uiInterface;
    }

    public void initPlugins() {
        try {
            loadPluginForDecrypt();
            loadPluginForHost();
            loadCPlugins();

            loadPluginOptional();

            for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
                if (plg.isLoaded()) {
                    try {
                        if (plg.isEnabled() && !plg.getPlugin().initAddon()) {
                            logger.severe("Error loading Optional Plugin:" + plg.getClassName());
                        }
                    } catch (Throwable e) {
                        logger.severe("Error loading Optional Plugin: " + e.getMessage());
                        JDLogger.exception(e);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean installerWasVisible() {
        return installerVisible;
    }

    public Configuration loadConfiguration() {
        Object obj = JDUtilities.getDatabaseConnector().getData("jdownloaderconfig");

        if (obj == null) {
            System.out.println("Fresh install?");
            // File file = JDUtilities.getResourceFile(JDUtilities.CONFIG_PATH);
            // if (file.exists()) {
            // logger.info("Wrapping jdownloader.config");
            // obj = JDIO.loadObject(null, file, Configuration.saveAsXML);
            // System.out.println(obj.getClass().getName());
            // JDUtilities.getDatabaseConnector().saveConfiguration(
            // "jdownloaderconfig",
            // obj);
            // }
        }

        if (!TEST_INSTALLER && obj != null && ((Configuration) obj).getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) != null) {

            Configuration configuration = (Configuration) obj;
            JDUtilities.setConfiguration(configuration);
            jd.controlling.JDLogger.getLogger().setLevel(configuration.getGenericProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getStringProperty(SimpleGuiConstants.PARAM_THEME, "default"));

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
            jd.controlling.JDLogger.getLogger().setLevel(configuration.getGenericProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getStringProperty(SimpleGuiConstants.PARAM_THEME, "default"));

            JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", JDUtilities.getConfiguration());
            installerVisible = true;
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_FINISH));
            /**
             * Workaround to enable JGoodies for MAC oS
             */

            JDLookAndFeelManager.setUIManager();
            Installer inst = new Installer();

            if (!inst.isAborted()) {

                File home = JDUtilities.getResourceFile(".");
                if (home.canWrite() && !JDUtilities.getResourceFile("noupdate.txt").exists()) {

                    // try {
                    // new WebUpdate().doWebupdate(true);
                    // JDUtilities.getConfiguration().save();
                    // JDUtilities.getDatabaseConnector().shutdownDatabase();
                    // logger.info(JDUtilities.runCommand("java", new String[] {
                    // "-jar", "jdupdate.jar", "/restart", "/rt" +
                    // JDUtilities.RUNTYPE_LOCAL_JARED },
                    // home.getAbsolutePath(), 0));
                    // System.exit(0);
                    // } catch (Exception e) {
                    // jd.controlling.JDLogger.getLogger().log(java.util.logging.
                    // Level.SEVERE,"Exception occurred",e);
                    // // System.exit(0);
                    // }

                }
                if (!home.canWrite()) {
                    logger.severe("INSTALL abgebrochen");

                    UserIO.getInstance().requestMessageDialog(JDLocale.L("installer.error.noWriteRights", "Error. You do not have permissions to write to the dir"));

                    JDIO.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                    System.exit(1);
                }

            } else {
                logger.severe("INSTALL abgebrochen2");

                UserIO.getInstance().requestMessageDialog(JDLocale.L("installer.abortInstallation", "Error. User aborted installation."));

                JDIO.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                System.exit(0);

            }
        }

        return JDUtilities.getConfiguration();
    }

    public void loadCPlugins() {
        new CPluginWrapper("linkbackup", "B", ".+\\.linkbackup");
        new CPluginWrapper("ccf", "C", ".+\\.ccf");
        new CPluginWrapper("rsdf", "R", ".+\\.rsdf");
        new CPluginWrapper("dlc", "D", ".+\\.dlc");
    }

    public void loadPluginForDecrypt() {
        new DecryptPluginWrapper("Serienjunkies.org", "Serienjunkies", "http://[\\w\\.]*?serienjunkies\\.org.*(rc[_-]|rs[_-]|nl[_-]|u[tl][_-]|ff[_-]|p=[\\d]+|cat=[\\d]+).*");
        new DecryptPluginWrapper("jdloader", "JDLoader", "(jdlist://.+)|((dlc|rsdf|ccf)://.*/.+)", PluginWrapper.LOAD_ON_INIT);
        new DecryptPluginWrapper("charts4you.org", "Charts4You", "http://[\\w\\.]*?charts4you\\.org/\\?id=\\d+");
        new DecryptPluginWrapper("alpha-link.eu", "AlphaLink", "http://[\\w\\.]*?alpha\\-link\\.eu/\\?id=[a-fA-F0-9]+");
        new DecryptPluginWrapper("protectbox.in", "ProtectBoxIn", "http://[\\w\\.]*?protectbox\\.in/.*");
        new DecryptPluginWrapper("animea.net", "AnimeANet", PluginPattern.DECRYPTER_ANIMEANET_PLUGIN);
        new DecryptPluginWrapper("anime-loads.org", "AnimeLoadsorg", "http://[\\w\\.]*?anime-loads\\.org/crypt.php\\?cryptid=[\\w]+|http://[\\w\\.]*?anime-loads\\.org/page.php\\?id=[0-9]+");
        new DecryptPluginWrapper("bat5.com", "URLCash", "http://.+bat5\\.com");
        new DecryptPluginWrapper("counterstrike.de", "CounterstrikeDe", "http://[\\w\\.]*?4players\\.de/\\S*/download/[0-9]+/([01]/)?index\\.html?");
        new DecryptPluginWrapper("googlegroups.com", "GoogleGroups", "http://groups.google.com/group/[^/]+/files/?");
        new DecryptPluginWrapper("best-movies.us", "BestMovies", "http://crypt\\.(best-movies\\.us|capcrypt\\.info)/go\\.php\\?id\\=\\d+");
        new DecryptPluginWrapper("blog-xx.net", "BlogXXNet", "http://[\\w\\.]*?blog-xx\\.net/wp/(.*?)/");
        new DecryptPluginWrapper("bm4u.in", "Bm4uin", "http://[\\w\\.]*?bm4u\\.in/index\\.php\\?do=show_download&id=\\d+");
        new DecryptPluginWrapper("cine.to", "CineTo", "http://[\\w\\.]*?cine\\.to/index\\.php\\?do=show_download&id=[\\w]+|http://[\\w\\.]*?cine\\.to/index\\.php\\?do=protect&id=[\\w]+|http://[\\w\\.]*?cine\\.to/pre/index\\.php\\?do=show_download&id=[\\w]+|http://[\\w\\.]*?cine\\.to/pre/index\\.php\\?do=protect&id=[\\w]+");
        new DecryptPluginWrapper("clipfish.de", "ClipfishDe", "http://[\\w\\.]*?clipfish\\.de/(.*?channel/\\d+/video/\\d+|video/\\d+(/.+)?)");
        new DecryptPluginWrapper("collectr.net", "Collectr", "http://[\\w\\.]*?collectr\\.net/(out/(\\d+/)?\\d+|links/\\w+)");
        new DecryptPluginWrapper("crypting.it", "CryptingIt", "http://[\\w\\.]*?crypting\\.it/(s/[\\w]+|index\\.php\\?p=show(usrfolders)?(&user=.+)?&id=[\\w]+)");
        new DecryptPluginWrapper("all-stream.info", "AllStreamInfo", "http://[\\w\\.]*?all-stream.info/\\?id=\\d+.*");
        new DecryptPluginWrapper("link-protection.org", "LinkProtectionOrg", "http://[\\w\\.]*?link-protection\\.org/.*");
        new DecryptPluginWrapper("crystal-warez.in", "CrystalWarezIN", "http://[\\w\\.]*?crystal-(warez|board)\\.in//?(show_download|protect)/.*?\\.html");
        new DecryptPluginWrapper("protector.to", "ProtectorTO", "http://[\\w\\.]*?protector\\.to/.*");
        new DecryptPluginWrapper("newsurl.de", "NewsUrlDe", "http://[\\w\\.]*?newsurl.de/.*");
        new DecryptPluginWrapper("crypt-it.com", "CryptItCom", "(http|ccf)://[\\w\\.]*?crypt-it\\.com/(s|e|d|c)/[\\w]+");
        new DecryptPluginWrapper("cryptlink.ws", "Cryptlinkws", "http://[\\w\\.]*?cryptlink\\.ws/\\?file=[\\w]+|http://[\\w\\.]*?cryptlink\\.ws/crypt\\.php\\?file=[0-9]+");
        new DecryptPluginWrapper("crypt-me.com", "CryptMeCom", "http://[\\w\\.]*?crypt-me\\.com/folder/[\\w]+\\.html");
        new DecryptPluginWrapper("ddl-music.org", "DDLMusicOrg", PluginPattern.DECRYPTER_DDLMSC_PLUGIN);
        new DecryptPluginWrapper("ddl-warez.org", "DDLWarez", "http://[\\w\\.]*?ddl-warez\\.org/detail\\.php\\?id=.+&cat=[^/]+");
        new DecryptPluginWrapper("3dl.am", "DreiDlAm", PluginPattern.DECRYPTER_3DLAM_PLUGIN);
        new DecryptPluginWrapper("1kh.de", "EinsKhDe", "http://[\\w\\.]*?1kh\\.de/f/[0-9/]+|http://[\\w\\.]*?1kh\\.de/[0-9]+");
        new DecryptPluginWrapper("falinks.com", "FalinksCom", "http://[\\w\\.]*?falinks\\.com/(\\?fa=link&id=\\d+|link/\\d+/?)");
        new DecryptPluginWrapper("filefactory.com", "FileFactoryFolder", "http://[\\w\\.]*?filefactory\\.com(/|//)f/[\\w]+");
        new DecryptPluginWrapper("filer.net", "Filer", "http://[\\w\\.]*?filer.net/folder/.+/.*");
        new DecryptPluginWrapper("freakshare.net", "FreakShareFolder", "http://[\\w\\.]*?freakshare.net/folder/\\d+/");
        new DecryptPluginWrapper("File-Upload.net", "FileUploadnet", "http://[\\w\\.]*?member\\.file-upload\\.net/(.*?)/(.*)");
        new DecryptPluginWrapper("filmatorium.cn", "FilmatoriumCn", "http://[\\w\\.]*?filmatorium\\.cn/\\?p=\\d+");
        new DecryptPluginWrapper("frozen-roms.in", "FrozenRomsIn", "http://[\\w\\.]*?frozen-roms\\.in/(details_[0-9]+|get_[0-9]+_[0-9]+)\\.html");
        new DecryptPluginWrapper("gwarez.cc", "Gwarezcc", "http://[\\w\\.]*?gwarez\\.cc/(\\d+|mirror/\\d+/checked/game/\\d+/|mirror/\\d+/parts/game/\\d+/|download/dlc/\\d+/)");
        new DecryptPluginWrapper("Hider.ath.cx", "HiderAthCx", "http://[\\w\\.]*?hider\\.ath\\.cx/\\d+");
        new DecryptPluginWrapper("hideurl.biz", "Hideurlbiz", "http://[\\w\\.]*?hideurl\\.biz/[\\w]+");
        new DecryptPluginWrapper("hubupload.com", "Hubuploadcom", "http://[\\w\\.]*?hubupload\\.com/files/[\\w]+/[\\w]+/(.*)");
        new DecryptPluginWrapper("iload.to", "ILoadTo", "http://iload\\.to/go/\\d+/|http://iload\\.to/(view|title|release)/.*?/");
        new DecryptPluginWrapper("imagefap.com", "ImagefapCom", "http://[\\w\\.]*?imagefap\\.com/(gallery\\.php\\?gid=.+|gallery/.+)");
        new DecryptPluginWrapper("joke-around.org", "JokeAroundOrg", "http://[\\w\\.]*?joke-around\\.org/.+");
        new DecryptPluginWrapper("knoffl.com", "KnofflCom", "http://[\\w\\.]*?knoffl\\.com/(u/[\\w-]+|[\\w-]+)");
        new DecryptPluginWrapper("LinkBank.eu", "LinkBankeu", "http://[\\w\\.]*?linkbank\\.eu/show\\.php\\?show=\\d+");
        new DecryptPluginWrapper("linkbase.biz", "LinkbaseBiz", "http://[\\w\\.]*?linkbase\\.biz/\\?v=[\\w]+");
        new DecryptPluginWrapper("linkbucks.com", "LinkBucks", "http://[\\w\\.]*?linkbucks\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?");
        new DecryptPluginWrapper("qvvo.com", "LinkBucks", "http://[\\w\\.]*?qvvo\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?");
        new DecryptPluginWrapper("baberepublic.com", "LinkBucks", "http://[\\w\\.]*?baberepublic\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?");
        new DecryptPluginWrapper("blahetc.com", "LinkBucks", "http://[\\w\\.]*?blahetc\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?");
        new DecryptPluginWrapper("linkcrypt.ws", "LinkCryptWs", "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+");
        new DecryptPluginWrapper("linkprotect.in", "LinkProtectIn", "http://[\\w\\.]*?linkprotect\\.in/index.php\\?site=folder&id=[\\w]{1,50}");
        new DecryptPluginWrapper("linkcrypt.com", "LinkcryptCom", "http://[\\w\\.]*?linkcrypt\\.com/[a-zA-Z]-[\\w]+");
        new DecryptPluginWrapper("sealed.in", "SealedIn", "http://[\\w\\.]*?sealed\\.in/[a-zA-Z]-[\\w]+");
        new DecryptPluginWrapper("crypting.cc", "CryptingCC", "http://[\\w\\.]*?crypting\\.cc/[a-zA-Z]-[\\w]+");
        new DecryptPluginWrapper("link-protector.com", "LinkProtectorCom", "http://[\\w\\.]*?link-protector\\.com/(x-)?\\d+");
        new DecryptPluginWrapper("linkr.at|rapidblogger.com", "LinkrAt", "http://[\\w\\.]*?(linkr\\.at/\\?p=|rapidblogger\\.com/link/)\\w+");
        new DecryptPluginWrapper("linksafe.ws", "LinkSafeWs", "http://[\\w\\.]*?linksafe\\.ws/files/[\\w]{4}-[\\d]{5}-[\\d]");
        new DecryptPluginWrapper("Linksave.in", "LinksaveIn", "http://[\\w\\.]*?linksave\\.in/[\\w]+");
        new DecryptPluginWrapper("link-share.org", "LinkShareOrg", "http://[\\w\\.]*?link-share\\.org/view.php\\?url=[\\w]{32}");
        new DecryptPluginWrapper("linkshield.com", "Linkshield", "http://[\\w\\.]*?linkshield\\.com/[sc]/[\\d]+_[\\d]+");
        new DecryptPluginWrapper("lix.in", "Lixin", "http://[\\w\\.]*?lix\\.in/[-]{0,1}[\\w]{6,10}");
        new DecryptPluginWrapper("mediafire.com", "MediafireFolder", "http://[\\w\\.]*?mediafire\\.com/\\?sharekey=.+");
        new DecryptPluginWrapper("redirect.musicalmente.info", "MusicalmenteInfo", "http://[\\w\\.]*?redirect\\.musicalmente\\.info/.+");
        new DecryptPluginWrapper("music-base.ws", "MusicBaseWs", "http://[\\w\\.]*?music-base\\.ws/dl\\.php.*?c=[\\w]+");
        new DecryptPluginWrapper("myref.de", "MyRef", "http://[\\w\\.]*?myref\\.de(/){0,1}\\?\\d{0,10}");
        new DecryptPluginWrapper("myup.cc", "Myupcc", "http://[\\w\\.]*?myup\\.cc/link-[\\w]+\\.html");
        new DecryptPluginWrapper("myvideo.de", "MyvideoDe", "http://[\\w\\.]*?myvideo\\.de/watch/[0-9]+/");
        new DecryptPluginWrapper("netfolder.in", "NetfolderIn", "http://[\\w\\.]*?netfolder\\.in/folder\\.php\\?folder_id\\=[\\w]{7}|http://[\\w\\.]*?netfolder\\.in/[\\w]{7}/.*?");
        new DecryptPluginWrapper("newzfind.com", "NewzFindCom", "http://[\\w\\.]*?newzfind\\.com/(video|music|games|software|mac|graphics|unix|magazines|e-books|xxx|other)/.+");
        new DecryptPluginWrapper("outlinkr.com", "OutlinkrCom", "http://[\\w\\.]*?outlinkr\\.com/(files|cluster)/[0-9]+/.+");
        new DecryptPluginWrapper("1gabba.com", "OneGabbaCom", "http://[\\w\\.]*?1gabba\\.com/node/[\\d]{4}");
        new DecryptPluginWrapper("Paylesssofts.net", "PaylesssoftsNet", "http://[\\w\\.]*?paylesssofts\\.net/((rs/\\?id\\=)|(\\?))[\\w]+");
        /* Protect.Tehparadox.com: old links still work */
        new DecryptPluginWrapper("Protect.Tehparadox.com", "ProtectTehparadoxcom", "http://[\\w\\.]*?protect\\.tehparadox\\.com/[\\w]+\\!");
        new DecryptPluginWrapper("raidrush.org", "RaidrushOrg", "http://[\\w\\.]*?raidrush\\.org/ext/\\?fid\\=[\\w]+");
        new DecryptPluginWrapper("rapidfolder.com", "RapidFolderCom", "http://[\\w\\.]*?rapidfolder\\.com/\\?\\w+");
        new DecryptPluginWrapper("rapidlayer.in", "Rapidlayerin", "http://[\\w\\.]*?rapidlayer\\.in/go/[\\w]+");
        new DecryptPluginWrapper("rapidsafe.de", "RapidsafeDe", "http://.+rapidsafe\\.de");
        new DecryptPluginWrapper("rapidsafe.net", "Rapidsafenet", "http://[\\w\\.]*?rapidsafe\\.net/r.-?[\\w]{11}/.*");
        new DecryptPluginWrapper("rapidshare.com", "RapidshareComFolder", "http://[\\w\\.]*?rapidshare.com/users/.+");
        new DecryptPluginWrapper("rapidshare.mu", "RapidshareMu", "http://[\\w\\.]*?rapidshare.mu/[\\w]+");
        new DecryptPluginWrapper("Rapidshark.net", "rapidsharknet", "http://[\\w\\.]*?rapidshark\\.net/(safe\\.php\\?id=)?.+");
        new DecryptPluginWrapper("rapidspread.com", "RapidSpreadCom", "http://[\\w\\.]*?rapidspread\\.com/file\\.jsp\\?id=\\w+");
        new DecryptPluginWrapper("r-b-a.de", "RbaDe", "http://[\\w\\.]*?r-b-a\\.de/(index\\.php\\?ID=4101&(amp;)?BATTLE=\\d+(&sid=\\w+)?)|http://[\\w\\.]*?r-b-a\\.de/index\\.php\\?ID=4100(&direction=last)?&MEMBER=\\d+(&sid=\\w+)?");
        new DecryptPluginWrapper("Redirect Services", "Redirecter", PluginPattern.decrypterPattern_Redirecter_Plugin());
        new DecryptPluginWrapper("relink.us", "RelinkUs", "http://[\\w\\.]*?relink\\.us/(go\\.php\\?id=[\\w]+|f/[\\w]+)");
        new DecryptPluginWrapper("relinka.net", "RelinkaNet", "http://[\\w\\.]*?relinka\\.net/folder/[a-z0-9]{8}-[a-z0-9]{4}");
        new DecryptPluginWrapper("rlslog.net", "Rlslog", "(http://[\\w\\.]*?rlslog\\.net(/.+/.+/#comments|/.+/#comments|/.+/.*))");
        new DecryptPluginWrapper("zerosec.ws", "ZeroSecWs", "(http://[\\w\\.]*?zerosec\\.ws(/.+/.+/#comments|/.+/#comments|/.+/.*))");
        new DecryptPluginWrapper("RnB4U.in", "RnB4Uin", "http://[\\w\\.]*?rnb4u\\.in/download\\.php\\?action=kategorie&kat_id=\\d+|http://[\\w\\.]*?rnb4u\\.in/download\\.php\\?action=popup&kat_id=\\d+&fileid=\\d+");
        new DecryptPluginWrapper("rock-house.in", "RockHouseIn", "http://[\\w\\.]*?rock-house\\.in/warez/warez_download\\.php\\?id=\\d+");
        new DecryptPluginWrapper("romhustler.net", "RomHustlerNet", "(http://[\\w\\.]*?romhustler\\.net/rom/.*?/\\d+/.+)|(/rom/.*?/\\d+/.+)");
        new DecryptPluginWrapper("roms.zophar.net", "RomsZopharNet", "http://[\\w\\.]*?roms\\.zophar\\.net/(.+)/(.+\\.7z)");
        new DecryptPluginWrapper("romscentral.com", "RomscentralCom", "(http://[\\w\\.]*?romscentral\\.com/(.+)/(.+\\.htm))|(onclick=\"return popitup\\('(.+\\.htm)'\\))", PluginWrapper.ACCEPTONLYSURLSFALSE);
        new DecryptPluginWrapper("rs.hoerbuch.in", "RsHoerbuchin", "http://rs\\.hoerbuch\\.in/com-[\\w]{11}/.*|http://rs\\.hoerbuch\\.in/de-[\\w]{11}/.*|http://rs\\.hoerbuch\\.in/u[\\w]{6}.html");
        new DecryptPluginWrapper("rs-layer.com", "RsLayerCom", "http://[\\w\\.]*?rs-layer\\.com/(.+)\\.html");
        new DecryptPluginWrapper("rsprotect.com", "RsprotectCom", "http://[\\w\\.]*?rsprotect\\.com/r[sc]-[\\w]{11}/.*");
        new DecryptPluginWrapper("rs-protect.freehoster.ch", "Rsprotectfreehosterch", "http://[\\w\\.]*?rs-protect\\.freehoster\\.ch/r[sc]-[\\w]{11}/.*");
        new DecryptPluginWrapper("rs.xxx-blog.org", "RsXXXBlog", "http://[\\w\\.]*?xxx-blog\\.org/[\\w]{1,4}-[\\w]{10,40}/.*");
        new DecryptPluginWrapper("saug.us", "SAUGUS", "http://[\\w\\.]*?saug\\.us/folder.?-[\\w\\-]{30,50}\\.html|http://[\\w\\.]*?saug\\.us/go.+\\.php");
        new DecryptPluginWrapper("save.raidrush.ws", "SaveRaidrushWs", "http://[\\w\\.]*?save\\.raidrush\\.ws/\\?id\\=[\\w]+");
        new DecryptPluginWrapper("scum.in", "ScumIn", "http://[\\w\\.]*?scum\\.in/index\\.php\\?id=\\d+");
        new DecryptPluginWrapper("sdx.cc", "SdxCc", "http://[\\w\\.]*?sdx\\.cc/infusions/(pro_download_panel|user_uploads)/download\\.php\\?did=\\d+");
        new DecryptPluginWrapper("secured.in", "Secured", "http://[\\w\\.]*?secured\\.in/download-[\\d]+-[\\w]{8}\\.html");
        new DecryptPluginWrapper("sexuria.com", "Sexuriacom", "http://[\\w\\.]*?sexuria\\.com/Pornos_Kostenlos_.+?_(\\d+)\\.html|http://[\\w\\.]*?sexuria\\.com/dl_links_\\d+_(\\d+)\\.html|http://[\\w\\.]*?sexuria\\.com/out.php\\?id=([0-9]+)&part=[0-9]+&link=[0-9]+");
        new DecryptPluginWrapper("sharebank.ws", "SharebankWs", "http://[\\w\\.]*?(mygeek|sharebank)\\.ws/\\?(v|go)=[\\w]+");
        new DecryptPluginWrapper("sharebee.com", "ShareBeeCom", "http://[\\w\\.]*?sharebee\\.com/[\\w]{8}");
        new DecryptPluginWrapper("spiegel.de", "SpiegelDe", "(http://[\\w\\.]*?spiegel\\.de/video/video-\\d+.html|http://[\\w\\.]*?spiegel\\.de/fotostrecke/fotostrecke-\\d+(-\\d+)?.html)");
        new DecryptPluginWrapper("Stealth.to", "Stealth", "http://[\\w\\.]*?stealth\\.to/(\\?id\\=[\\w]+|index\\.php\\?id\\=[\\w]+|\\?go\\=captcha&id=[\\w]+)");
        new DecryptPluginWrapper("technorocker.info", "TechnorockerInfo", "http://[\\w\\.]*?technorocker\\.info/download/[0-9]+/.*");
        new DecryptPluginWrapper("usercash.com", "UserCashCom", "http://[\\w\\.]*?usercash\\.com/");
        new DecryptPluginWrapper("Underground CMS", "UCMS", PluginPattern.decrypterPattern_UCMS_Plugin());
        new DecryptPluginWrapper("uploadjockey.com", "UploadJockeycom", "http://[\\w\\.]*?uploadjockey\\.com/download/[\\w]+/(.*)");
        new DecryptPluginWrapper("up.picoasis.net", "UpPicoasisNet", "http://up\\.picoasis\\.net/[\\d]+");
        new DecryptPluginWrapper("urlcash.net", "URLCash", "(http://[\\w\\.]*?" + PluginPattern.URLCASH + "/.+)|(http://[\\w\\-]{5,16}\\." + PluginPattern.URLCASH + ")");
        new DecryptPluginWrapper("urlshield.net", "UrlShieldnet", "http://[\\w\\.]*?urlshield\\.net/l/[\\w]+");
        new DecryptPluginWrapper("uu.canna.to", "UUCannaTo", "http://[uu\\.canna\\.to|85\\.17\\.36\\.224]+/cpuser/links\\.php\\?action=[cp_]*?popup&kat_id=[\\d]+&fileid=[\\d]+");
        new DecryptPluginWrapper("vetax.in", "VetaXin", "http://[\\w\\.]*?vetax\\.in/view/\\d+|http://[\\w\\.]*?vetax\\.in/(dload|mirror)/[\\w]+");
        new DecryptPluginWrapper("web06.de", "Web06de", "http://[\\w\\.]*?web06\\.de/\\?user=\\d+site=(.*)");
        new DecryptPluginWrapper("wii-reloaded.ath.cx", "Wiireloaded", "http://wii-reloaded\\.ath\\.cx/protect/get\\.php\\?i=.+");
        new DecryptPluginWrapper("chaoz.ws", "Woireless6xTo", "http://[\\w\\.]*?chaoz\\.ws/woireless/page/album_\\d+\\.html");
        new DecryptPluginWrapper("Wordpress Parser", "Wordpress", PluginPattern.decrypterPattern_Wordpress_Plugin());
        new DecryptPluginWrapper("xaili.com", "Xailicom", "http://[\\w\\.]*?xaili\\.com/\\?site=protect&id=[0-9]+");
        new DecryptPluginWrapper("xenonlink.net", "XenonLinkNet", "http://[\\w\\.]*?xenonlink\\.net/");
        new DecryptPluginWrapper("xink.it", "XinkIt", "http://[\\w\\.]*?xink\\.it/f-[\\w]+");
        new DecryptPluginWrapper("xlice.net", "XliceNet", "http://[\\w\\.]*?xlice\\.net/download/[a-z0-9]+");
        new DecryptPluginWrapper("xrl.us", "XrlUs", "http://[\\w\\.]*?xrl\\.us/[\\w]+");
        new DecryptPluginWrapper("xup.in", "XupInFolder", "http://[\\w\\.]*?xup\\.in/a,[0-9]+(/.+)?(/(list|mini))?");
        new DecryptPluginWrapper("youporn.com", "YouPornCom", "http://[\\w\\.]*?youporn\\.com/watch/\\d+/?.+/?");
        new DecryptPluginWrapper("yourfiles.biz", "YourFilesBizFolder", "http://[\\w\\.]*?yourfiles\\.biz/.*/folders/[0-9]+/.+\\.html");
        new DecryptPluginWrapper("youtube.com", "YouTubeCom", "http://[\\w\\.]*?youtube\\.com/(watch\\?v=[a-z-_A-Z0-9]+|view_play_list\\?p=[a-z-_A-Z0-9]+)");
        new DecryptPluginWrapper("megaupload.com folder", "MegauploadComFolder", "http://[\\w\\.]*?megaupload\\.com/.*?\\?f=[\\w]+");
        new DecryptPluginWrapper("rsmonkey.com", "RsMonkeyCom", "http://[\\w\\.]*?rsmonkey\\.com/\\d+");
        new DecryptPluginWrapper("savefile.com Project", "SavefileComProject", "http://[\\w\\.]*?savefile\\.com/projects/[0-9]+");
        new DecryptPluginWrapper("badongo.com", "BadongoCom", "http://[\\w\\.]*?badongo\\.com/.*(file|vid|audio)/[0-9]+");
        new DecryptPluginWrapper("wrzuta.pl", "WrzutaPl", "http://[\\w\\.]*?wrzuta\\.pl/katalog/\\w+.+");
        new DecryptPluginWrapper("rs43.com", "Rs43Com", "http://[\\w\\.]*?rs43\\.com/(Share\\.Mirror\\.Service/\\?|\\?)/.+");
        new DecryptPluginWrapper("relink-it.com", "RelinkItCom", "http://[\\w\\.]*?relink-it\\.com(/\\w\\w/|/)\\?.+");
        new DecryptPluginWrapper("protectlinks.com", "ProtectLinksCom", "http://[\\w\\.]*?protectlinks\\.com/\\d+");
        new DecryptPluginWrapper("remixshare.com", "RemixShareComFolder", "http://[\\w\\.]*?remixshare\\.com/container/\\?id=[a-z0-9].+");
        new DecryptPluginWrapper("zero10.us", "Zero10Us", "http://[\\w\\.]*?zero10\\.us/\\d+");
        new DecryptPluginWrapper("h-link.us", "Zero10Us", "http://[\\w\\.]*?h-link\\.us/\\d+");
        new DecryptPluginWrapper("jamendo.com", "JamendoCom", "http://[\\w\\.]*?jamendo\\.com/.?.?/?(album/\\d+|artist/.+)");
        new DecryptPluginWrapper("fdnlinks.com", "FDNLinksCom", "http://[\\w\\.]*?fdnlinks\\.com/link/[\\w]+");
        new DecryptPluginWrapper("sprezer.com", "SprezerCom", "http://[\\w\\.]*?sprezer\\.com/file-.+");
        new DecryptPluginWrapper("sharebase.to folder", "ShareBaseToFolder", "http://[\\w\\.]*?sharebase\\.to/ordner/.+");
        new DecryptPluginWrapper("linkbee.com", "LinkBeeCom", "http://[\\w\\.]*?linkbee\\.com/[\\w]+");
        new DecryptPluginWrapper("superuploader.net", "SuperUploaderNet", "http://[\\w\\.]*?superuploader\\.net/.*?\\.html");
        new DecryptPluginWrapper("easy-share.com", "EasyShareFolder", "http://[\\w\\.]*?easy-share\\.com/f/\\d+/");
        new DecryptPluginWrapper("mp3link.org", "Mp3LinkOrg", "http://[\\w\\.]*?mp3link\\.org/.*?/(song|album).+");
        new DecryptPluginWrapper("4shared.com", "FourSharedFolder", "http://[\\w\\.]*?4shared\\.com/dir/\\d+/[\\w]+/?");
        new DecryptPluginWrapper("Hex.io", "HexIo", "http://[\\w\\.]*?hex\\.io/[\\w]+");
        new DecryptPluginWrapper("HyperLinkCash.com", "HyperLinkCashCom", "http://[\\w\\.]*?hyperlinkcash\\.com/link\\.php\\?r=[\\w%]+(&k=[\\w%]+)?");
        new DecryptPluginWrapper("rom-news.org", "RomNewsOrg", "http://[\\w\\.]*?download\\.rom-news\\.org/[\\w]+");
        new DecryptPluginWrapper("quicklink.me", "QuickLinkMe", "http://[\\w\\.]*?quicklink\\.me/\\?l=[\\w]+");
        new DecryptPluginWrapper("short.redirect.am", "ShortRedirectAm", "http://short\\.redirect\\.am/\\?[\\w]+");
        new DecryptPluginWrapper("h-url.in", "HurlIn", "http://[\\w\\.]*?h-url\\.in/[\\w]+");
        new DecryptPluginWrapper("takemyfile.com", "TakemyfileCom", "http://[\\w\\.]*?(tmf\\.myegy\\.com|takemyfile.com)/(.*?id=)?\\d+");
        new DecryptPluginWrapper("raubkopierer.ws", "RaubkopiererWs", "http://[\\w\\.]*?raubkopierer\\.(ws|cc)/\\w+/[\\w/]*?\\d+/.+");
        new DecryptPluginWrapper("yourref.de", "YourRefDe", "http://[\\w\\.]*?yourref\\.de/\\?\\d+");
        new DecryptPluginWrapper("pspisos.org", "PspIsosOrg", "http://[\\w\\.]*?pspisos\\.org/(d\\d\\d?wn|d3741l5)/.+(/\\d+)?");
        new DecryptPluginWrapper("fileducky.com", "Fileducky", "http://[\\w\\.]*?fileducky\\.com/[\\w]+/?");
        new DecryptPluginWrapper("megarotic.com", "MegaRoticCom", "http://[\\w\\.]*?(megarotic|sexuploader)\\.com/.*?(\\?|&)d=[\\w]+");
        new DecryptPluginWrapper("foxlink.info", "FoxLinkInfo", "http://[\\w\\.]*?(fox|vip)link\\.info/\\d+");
        new DecryptPluginWrapper("referhush.com", "ReferHushCom", "http://[\\w\\.]*?referhush\\.com/\\?rh=[a-f0-9]+");
        new DecryptPluginWrapper("amigura.co.uk", "AmiguraCoUk", "http://[\\w\\.]*?amigura\\.co\\.uk/(s\\d+|send_file)\\.php\\?d=(\\d+(-|/)[A-Z0-9]+(-|/)\\d+(-|/)|\\d+/).+");
        new DecryptPluginWrapper("hotfile.com", "HotfileCom", "http://[\\w\\.]*?hotfile\\.com/list/\\d+/[\\w]+");
        new DecryptPluginWrapper("adf.ly", "AdfLy", "http://[\\w\\.]*?adf\\.ly/[\\w]+");
        new DecryptPluginWrapper("uploadr.eu", "UploadrEu", "http://[\\w\\.]*?uploadr\\.eu/(link/[\\w]+|folder/\\d+/)");
        new DecryptPluginWrapper("seriousurls.com", "LinkBucks", "http://[\\w\\.]*?seriousurls\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("thesegalleries.com", "LinkBucks", "http://[\\w\\.]*?thesegalleries\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("ubucks.net", "LinkBucks", "http://[\\w\\.]*?ubucks\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("seriousfiles.com", "LinkBucks", "http://[\\w\\.]*?seriousfiles\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("viraldatabase.com", "LinkBucks", "http://[\\w\\.]*?viraldatabase\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("placepictures.com", "LinkBucks", "http://[\\w\\.]*?placepictures\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("urlpulse.net", "LinkBucks", "http://[\\w\\.]*?urlpulse\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("thesefiles.com", "LinkBucks", "http://[\\w\\.]*?thesefiles\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("linkgalleries.net", "LinkBucks", "http://[\\w\\.]*?linkgalleries\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("youfap.com", "LinkBucks", "http://[\\w\\.]*?youfap\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("realfiles.net", "LinkBucks", "http://[\\w\\.]*?realfiles\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?");
        new DecryptPluginWrapper("urlcut.com", "UrlCutCom", "http://[\\w\\.]*?urlcut\\.com/[0-9a-zA-Z]+");
        new DecryptPluginWrapper("short.3arabforest.com", "ThreeArabForestCom", "http://[\\w\\.]*?short\\.3arabforest\\.com/\\d+");
        new DecryptPluginWrapper("youcrypt.com", "YouCryptCom", "http://[\\w\\.]*?youcrypt\\.com/[0-9a-zA-Z]+\\.html");
        new DecryptPluginWrapper("zomgupload.com", "ZomgUploadCom", "(?!http://[\\w\\.]*?zomgupload\\.com/.+[/0-9a-zA-Z/-]+.html)http://[\\w\\.]*?zomgupload\\.com/.+[/0-9a-zA-Z/-]");
        new DecryptPluginWrapper("rsd-store.com", "RsdStore", "http://[\\w\\.]*?rsd-store\\.com/\\d+\\.html");
        new DecryptPluginWrapper("movietown.info", "MovieTown", "http://[\\w\\.]*?movietown\\.info/index/download\\.php\\?id=\\d+");
        new DecryptPluginWrapper("nareey.com", "NareeyCom", "http://[\\w\\.]*?nareey\\.com/(\\d\\.php\\?\\d+|\\d+/)");
        new DecryptPluginWrapper("q1q1q.com", "Q1Q1QCom", "http://[\\w\\.]*?q1q1q\\.com/\\d+");
        new DecryptPluginWrapper("minyurl.net", "MinyUrlNet", "http://[\\w\\.]*?minyurl\\.net/[^\\s^/]+");
        new DecryptPluginWrapper("linksafe.info","LinksafeInfo","http://[\\w\\.]*?linksafe\\.info/[^\\s^/]+");
        // Decrypter from Extern
        new DecryptPluginWrapper("rapidlibrary.com", "RapidLibrary", "http://rapidlibrary\\.com/download_file_i\\.php\\?.+");
    }

    public void loadPluginForHost() {
        // Premium Hoster
        new HostPluginWrapper("RapidShare.com", "Rapidshare", "http://[\\w\\.]*?rapidshare\\.com/files/[\\d]{3,9}/?.+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Uploaded.to", "Uploadedto", "((http://[\\w\\.]*?uploaded\\.to/.*?(file/|\\?id=|&id=)[\\w]+/?)|(http://[\\w\\.]*?ul\\.to/[\\w\\-]+/?))", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("BlueHost.to", "BluehostTo", "http://[\\w\\.]*?bluehost\\.to/(\\?dl=|dl=|file/).*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("DepositFiles.com", "DepositFiles", "http://[\\w\\.]*?depositfiles\\.com(/\\w{1,3})?/files/[\\w]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("FileFactory.com", "FileFactory", "http://[\\w\\.]*?filefactory\\.com(/|//)file/[\\w]+/?", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Filer.net", "FilerNet", "http://[\\w\\.]*?filer.net/(file[\\d]+|get|dl)/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("MegaShares.Com", "MegasharesCom", "http://[\\w\\.]*?(d[0-9]{2}\\.)?megashares\\.com/(.*\\?d[0-9]{2}=[0-9a-f]{7}|.*?dl/[0-9a-f]+/)", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("MegaUpload.com", "Megauploadcom", "http://[\\w\\.]*?(megaupload)\\.com/.*?(\\?|&)d=[\\w]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("MegaPorn.com", "MegaPornCom", "http://[\\w\\.]*?(megaporn)\\.com/.*?(\\?|&)d=[\\w]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("MeinUpload.com", "MeinUpload", "(http://[\\w\\.]*?meinupload\\.com/{1,}dl/.+/.+)|(http://[\\w\\.]*?meinupload\\.com/\\?d=.*)|http://[\\w\\.]*?mein-upload\\.com/[\\w]+(\\.html|/)", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Badongo.com", "BadongoCom", "http://[\\w\\.]*?badongo\\.viajd.*/.*(file|vid)/[0-9]+/?\\d?/?\\w?\\w?", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Mooshare.net", "Moosharenet", "http://[\\w\\.]*?mooshare\\.net/files/\\d+/.*?\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("NetLoad.in", "Netloadin", "http://[\\w\\.]*?netload\\.in/.+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("QShare.Com", "QshareCom", "http://[\\w\\.]*?qshare\\.com/get/[0-9]{1,20}/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("RapidShare.De", "RapidShareDe", "http://[\\w\\.]*?rapidshare\\.de/files/[\\d]{3,9}/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("ShareBase.to", "ShareBaseTo", "http://[\\w\\.]*?sharebase\\.(de|to)/files/[\\w]+\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("YouTube.com", "Youtube", "http://[\\w\\.]*?youtube\\.com/get_video\\?video_id=.+&t=.+(&fmt=\\d+)?", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Share-Online.Biz", "ShareOnlineBiz", "http://[\\w\\.]*?share\\-online\\.biz/download.php\\?id\\=[\\w]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("FileHostMe.com", "FileHostMecom", "http://[\\w\\.]*?filehostme\\.com/[\\w]+\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("GigaSize.com", "GigaSizeCom", "http://[\\w\\.]*?gigasize\\.com/get\\.php.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("LetItBit.net", "LetitBitNet", "http://[\\w\\.]*?letitbit\\.net/download/[\\w]+?/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Speedy-Share.com", "Speedy_ShareCom", "http://[\\w\\.]*?speedy\\-share\\.com/[\\w]+/(.*)");
        new HostPluginWrapper("Shragle.com", "ShragleCom", "http://[\\w\\.]*?shragle\\.(com|de)/files/[\\w]+/.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Vip-File.com", "Vipfilecom", "http://[\\w\\.]*?vip-file\\.com/download/[\\w]+/(.*?)\\.html", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Easy-Share.com", "EasyShareCom", "http://[\\w\\d\\.]*?easy-share\\.com/\\d{6}.*", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Uploader.pl", "UploaderPl", "http://[\\w\\.]*?uploader\\.pl/\\?d=[A-F0-9]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("CZShare.com", "CZShareCom", "http://[\\w\\.]*?czshare\\.com/(files/\\d+/[\\w_]+|\\d+/[\\w_]+/[^\\s]+|download_file\\.php\\?id=\\d+&file=[^\\s]+)", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("4FreeLoad.net", "FourFreeLoadNet", "http://[\\w\\.]*?4freeload\\.net/download\\.php\\?id=[A-Fa-f0-9]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("HotFile.com", "HotFileCom", "http://[\\w\\.]*?hotfile\\.com/dl/\\d+/[0-9a-zA-Z]+/", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Data.hu", "DataHu", "http://[\\w\\.]*?data.hu/get/.+/.+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("OdSiebie.com", "Odsiebiecom", "http://[\\w\\.]*?odsiebie\\.com/(pokaz|pobierz)/\\d+---[\\w]+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("Uploading.com", "UploadingCom", "http://[\\w\\.]*?uploading\\.com/files/\\w+/.+", PluginWrapper.LOAD_ON_INIT);
        new HostPluginWrapper("SendSpace.com", "SendspaceCom", "http://[\\w\\.]*?sendspace\\.com/file/[0-9a-zA-Z]+", PluginWrapper.LOAD_ON_INIT);

        // Free Hoster
        new HostPluginWrapper("FreakShare.net", "Freaksharenet", "http://[\\w\\.]*?freakshare\\.net/file(s/|/)[\\w]+/(.*)");
        new HostPluginWrapper("Share-Now.net", "ShareNownet", "http://[\\w\\.]*?share-now\\.net/{1,}files/\\d+-(.*?)\\.html");
        new HostPluginWrapper("Archiv.to", "ArchivTo", "http://[\\w\\.]*?archiv\\.to/\\?Module\\=Details&HashID\\=.*");
        new HostPluginWrapper("AxiFile.com", "AxiFileCom", "http://[\\w\\.]*?axifile\\.com/\\?\\d+");
        new HostPluginWrapper("CShare.de", "SwoopshareCom", "http://[\\w\\.]*?cshare.de/file/.*");
        new HostPluginWrapper("ClipFish.de", "ClipfishDe", "http://[\\w\\.]*?pg\\d+\\.clipfish\\.de/media/.+?\\.flv");
        new HostPluginWrapper("DataUp.de", "Dataupde", "http://[\\w\\.]*?dataup\\.de/\\d+/(.*)");
        new HostPluginWrapper("Dump.ru", "DumpRu", "http://[\\w\\.]*?dump\\.ru/file/[0-9]+");
        new HostPluginWrapper("4Shared.com", "FourSharedCom", "http://[\\w\\.]*?4shared.com/file/\\d+?/.*");
        new HostPluginWrapper("FastShare.org", "FastShareorg", "http://[\\w\\.]*?fastshare\\.org/download/(.*)");
        new HostPluginWrapper("FileBase.To", "FileBaseTo", "http://[\\w\\.]*?filebase\\.to/files/\\d{1,}/.*");
        new HostPluginWrapper("Files.To", "FilesTo", "http://[\\w\\.]*?files\\.to/get/[0-9]+/[\\w]+");
        new HostPluginWrapper("File-Upload.net", "FileUploadnet", "((http://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html)|(http://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html))|(http://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)))");
        new HostPluginWrapper("GoogleGroups.com", "GoogleGroups", "http://[\\w\\.]*?googlegroups.com/web/.*");
        new HostPluginWrapper("HTTP Links", "HTTPAllgemein", "https?viajd://[\\d\\w\\.:\\-@]*/.*" + PluginPattern.ENDINGS);
        new HostPluginWrapper("ImageFap.com", "ImageFap", "http://[\\w\\.]*?imagefap.com/image.php\\?id=.*(&pgid=.*&gid=.*&page=.*)?");
        new HostPluginWrapper("MediaFire.Com", "MediafireCom", "http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?.+|\\?.+|file/.+)");
        new HostPluginWrapper("MyVideo.de", "MyVideo", "http://[\\w\\.]*?myvideo.*?/.*?/\\d+\\.flv");
        new HostPluginWrapper("Przeslij.net", "Przeslijnet", "http://www[\\d]?\\.przeslij\\.net/download\\.php\\?file=(.*)");
        new HostPluginWrapper("R-b-a.De", "RbaDe", "http://[\\w\\.]*?r-b-a\\.de/download\\.php\\?FILE=(\\d+)-(\\d)\\.mp3&PATH=(\\d)");
        new HostPluginWrapper("Roms.Zophar.Net", "RomsZopharNet", "http://[\\w\\.]*?roms\\.zophar\\.net/download-file/[0-9]{1,}");
        new HostPluginWrapper("RomHustler.Net", "RomHustlerNet", "http://[\\w\\.]*?romhustler\\.net/download/.*?/\\d+");
        new HostPluginWrapper("SharePlace.com", "Shareplacecom", "http://[\\w\\.]*?shareplace\\.com/\\?[\\w]+(/.*?)?");
        new HostPluginWrapper("Datei.in", "Shareplacecom", "http://[\\w\\.]*?datei\\.in/\\?[\\w]+(/.*?)?");
        new HostPluginWrapper("Spiegel.de", "SpiegelDe", "http://video\\.spiegel\\.de/flash/.+?\\.flv|http://video\\.promobil2spiegel\\.netbiscuits\\.com/.+?\\.(3gp|mp4)|http://www.spiegel.de/img/.+?(\\.\\w+)");
        new HostPluginWrapper("SwoopShare.com", "SwoopshareCom", "http://[\\w\\.]*?swoopshare\\.com/file/.*");
        new HostPluginWrapper("2shared.com", "TwoSharedCom", "http://[\\w\\.]*?2shared\\.com/file/\\d+/[\\w]+");
        new HostPluginWrapper("UploadService.info", "UploadServiceinfo", "http://[\\w\\.]*?uploadservice\\.info/file/[\\w]+\\.html");
        new HostPluginWrapper("UploadStube.de", "UploadStube", "http://[\\w\\.]*?uploadstube\\.de/download\\.php\\?file=.*");
        new HostPluginWrapper("Xup.In", "XupIn", "http://[\\w\\.]*?xup\\.in/dl,\\d+/?.+?");
        new HostPluginWrapper("YouPorn.Com", "YouPornCom", "http://download\\.youporn\\.com/download/\\d+.*");
        new HostPluginWrapper("YourFiles.Biz", "YourFilesBiz", "http://[\\w\\.]*?yourfiles\\.(biz|to)/\\?d=[\\w]+");
        new HostPluginWrapper("YourFileSender.com", "YourFileSendercom", "http://[\\w\\.]*?yourfilesender\\.com/v/\\d+/(.*?\\.html)");
        new HostPluginWrapper("ZippyShare.com", "Zippysharecom", "http://www\\d{0,}\\.zippyshare\\.com/(v/\\d+/file\\.html|.*?key=\\d+)");
        new HostPluginWrapper("ZShare.net", "ZShareNet", "http://[\\w\\.]*?zshare\\.net/(download|video|image|audio|flash)/.*");
        new HostPluginWrapper("SendSpace.pl", "SendSpacePl", "http://[\\w\\.]*?sendspace.pl/file/[\\w]+/?");
        new HostPluginWrapper("dl.free.fr", "DlFreeFr", "http://[\\w\\.]*?dl\\.free\\.fr/(getfile\\.pl\\?file=/[\\w]+|[\\w]+/?)");
        new HostPluginWrapper("EgoShare.com", "EgoshareCom", "http://[\\w\\.]*?egoshare\\.com/download\\.php\\?id=[\\w]+");
        new HostPluginWrapper("SpeedShare.org", "SpeedShareOrg", "http://[\\w\\.]*?speedshare\\.org/download\\.php\\?id=[\\w]+");
        new HostPluginWrapper("Vimeo.com", "VimeoCom", "http://[\\w\\.]*?vimeo\\.com/[0-9]+");
        new HostPluginWrapper("Self-Load.com", "SelfLoadCom", "http://[\\w\\.]*?self-load\\.com/\\d+/.+");
        new HostPluginWrapper("Plikus.pl", "PlikusPl", "http://[\\w\\.]*?plikus\\.pl/zobacz_plik-.*?-\\d+\\.html");
        new HostPluginWrapper("KewlShare.com", "KewlshareCom", "http://[\\w\\.]*?kewlshare\\.com/dl/[\\w]+/.*");
        new HostPluginWrapper("ShareBomb.com", "ShareBombCom", "http://[\\w\\.]*?sharebomb\\.com/[0-9]+.*");
        new HostPluginWrapper("Load.to", "LoadTo", "http://(\\w*\\.)?load\\.to/[\\?d=]?[\\w]+.*");
        new HostPluginWrapper("FileStore.to", "FilestoreTo", "http://[\\w\\.]*?filestore\\.to/\\?d=[\\w]+");
        new HostPluginWrapper("ZetShare.com", "ZetshareCom", "http://[\\w\\.]*?zetshare\\.com/(download/|url/)?download\\.php\\?file=[0-9a-zA-Z]+");
        new HostPluginWrapper("Filezzz.com", "FilezzzCom", "http://[\\w\\.]*?filezzz\\.com/download/[0-9]+/");
        new HostPluginWrapper("SaveFile.com", "SavefileCom", "http://[\\w\\.]*?savefile\\.com/files/[0-9]+");
        new HostPluginWrapper("SpeedyShare.com", "SpeedyShareCom", "http://[\\w\\.]*?speedyshare\\.com/[0-9]+.*");
        new HostPluginWrapper("FileShaker.com", "FileshakerCom", "http://[\\w\\.]*?fileshaker\\.com/.+");
        new HostPluginWrapper("ADrive.com", "AdriveCom", "http://[\\w\\.].*?adrive\\.com/public/[0-9a-zA-Z]+.*");
        new HostPluginWrapper("Przeklej.pl", "PrzeklejPl", "http://[\\w\\.]*?przeklej\\.pl/(d/\\w+/|\\d+|plik/)[^\\s]+");
        new HostPluginWrapper("Wrzuta.pl", "WrzutaPl", "http://[\\w\\.]*?wrzuta\\.pl/(audio|film|obraz)/\\w+.+");
        new HostPluginWrapper("FileFactory.pl", "FileFactoryPl", "http://[\\w\\.]*?filefactory\\.pl/showfile-\\d+.+");
        new HostPluginWrapper("WyslijTo.pl", "WyslijToPl", "http://[\\w\\.]*?wyslijto\\.pl/(download|files/download|files/pre_download|plik)/\\w+");
        new HostPluginWrapper("Wrzucaj.com", "WrzucajCom", "http://[\\w\\.]*?wrzucaj\\.com/\\d+");
        new HostPluginWrapper("HostPlik.com", "HostPlikCom", "http://[\\w\\.]*?hostplik\\.com/\\d+.*");
        new HostPluginWrapper("Upload-Drive.com", "UploadDriveCom", "http://[\\w\\.]*?upload-drive\\.com/\\d+/.+");
        new HostPluginWrapper("Pliczek.net", "PliczekNet", "http://[\\w\\.]*?pliczek\\.net/index\\.php\\?p=\\d+");
        new HostPluginWrapper("Plikos.pl", "PlikosPl", "http://[\\w\\.]*?plikos\\.pl/\\w+/.+");
        new HostPluginWrapper("RemixShare.com", "RemixShareCom", "http://[\\w\\.]*?remixshare\\.com/.*?\\?file=[a-z0-9]+");
        new HostPluginWrapper("FileSend.net", "FileSendNet", "http://[\\w\\.]*?filesend\\.net/download\\.php\\?f=[a-z0-9]+");
        new HostPluginWrapper("UploadBox.com", "UploadBoxCom", "http://[\\w\\.]*?uploadbox\\.com/.*?files/[0-9a-zA-Z]+");
        new HostPluginWrapper("Jamendo.com", "JamendoCom", "http://[\\w\\.]*?jamendo\\.com/.*.*/?(track|download/album)/\\d+");
        new HostPluginWrapper("FileLoad.us", "FileloadUs", "http://[\\w\\.]*?fileload\\.us/[\\w]+/?");
        new HostPluginWrapper("EnterUpload.com", "EnteruploadCom", "http://[\\w\\.]*?enterupload\\.com/[\\w]+/?");
        new HostPluginWrapper("6Giga.com", "SixGigaCom", "http://[\\w\\.]*?6giga\\.com/.*");
        new HostPluginWrapper("Filelobster.com", "FilelobsterCom", "http://[\\w\\.]*?filelobster\\.com/[\\w]+/?");
        new HostPluginWrapper("Sharearound.com", "SharearoundCom", "http://[\\w\\.]*?sharearound\\.com/[\\w]+/.*");
        new HostPluginWrapper("Only4files.com", "OnlyFourFilesCom", "http://[\\w\\.]*?only4files\\.com/[\\w]+/.*");
        new HostPluginWrapper("Fileuploadshark.com", "FileuploadsharkCom", "http://[\\w\\.]*?fileuploadshark\\.com/[\\w]+/?");
        new HostPluginWrapper("Xshareware.com", "XsharewareCom", "http://[\\w\\.]*?xshareware\\.com/[\\w]+/.*");
        new HostPluginWrapper("Savefile.ro", "SavefileRo", "http://[\\w\\.]*?savefile\\.ro/[\\w]+/?");
        new HostPluginWrapper("Pixelhit.com", "PixelhitCom", "http://[\\w\\.]*?pixelhit\\.com/[\\w]+/?");
        new HostPluginWrapper("Bagruj.cz", "BagrujCz", "http://[\\w\\.]*?bagruj\\.cz/.*");
        new HostPluginWrapper("Uploadline.com", "UploadlineCom", "http://[\\w\\.]*?uploadline\\.com/\\d+/.+");
        new HostPluginWrapper("Biggerupload.com", "BiggeruploadCom", "http://[\\w\\.]*?biggerupload\\.com/[\\w]+/?.*");
        new HostPluginWrapper("Ziddu.com", "ZidduCom", "http://[\\w\\.]*?ziddu\\.com/((download/\\d+/.*?\\.html)|(download\\.php\\?uid=[\\w%]+)|downloadfile/\\d+/.*?\\.html)");
        new HostPluginWrapper("UpMusic.in", "UpMusicIn", "http://[\\w\\.]*?upmusic\\.in/[a-z0-9]+.*?");
        new HostPluginWrapper("SuperShare.pl", "SuperSharePl", "http://[\\w\\.]*?supershare\\.pl/\\?d=[A-F0-9]+");
        new HostPluginWrapper("FileFront.com", "FileFrontCom", "http://files.filefront\\.com/[^\\s]+/;\\d+(;/fileinfo.html|;)");
        new HostPluginWrapper("FileQube.com", "FileQubeCom", "http://[\\w\\.]*?fileqube\\.com/(file|shared)/[\\w]+");
        new HostPluginWrapper("MegaShare.com", "MegaShareCom", "http://[\\w\\.]*?megashare\\.com/[0-9]+");
        new HostPluginWrapper("RapidShark.pl", "RapidSharkPl", "http://[\\w\\.]*?rapidshark\\.pl/.*?[\\w]+/?");
        new HostPluginWrapper("MilleDrive.com", "MilleDriveCom", "http://[\\w\\.]*?milledrive\\.com/(music|files|videos|files/video|files/music)/\\d+/.*");
        new HostPluginWrapper("Up-File.com", "UpFileCom", "http://[\\w\\.]*?up-file\\.com/download/[a-z0-9]+");
        new HostPluginWrapper("LeteckaPosta.cz", "LeteckaPostaCz", "http://[\\w\\.]*?leteckaposta\\.cz/[0-9]+.");
        new HostPluginWrapper("ShareGadget.com", "ShareGadgetCom", "http://[\\w\\.]*?sharegadget\\.com/[0-9]+.");
        new HostPluginWrapper("Uptal.com", "UptalCom", "http://[\\w\\.]*?uptal\\.com/\\?d=[A-Fa-f0-9]+");
        new HostPluginWrapper("IFile.it", "IFileIt", "http://[\\w\\.]*?ifile\\.it/[\\w]+/?");
        new HostPluginWrapper("Mihd.net", "IFileIt", "http://[\\w\\.]*?mihd\\.net/[\\w]+/?");
        new HostPluginWrapper("BitRoad.net", "BitRoadNet", "http://[\\w\\.]*?bitroad\\.net/download/[A-Fa-f0-9]+");
        new HostPluginWrapper("FriendlyFiles.net", "BitRoadNet", "http://[\\w\\.]*?friendlyfiles\\.net/download/[A-Fa-f0-9]+");
        new HostPluginWrapper("MaxUpload.eu", "MaxUploadEu", "http://[\\w\\.]*?maxupload\\.eu/../\\d+");
        new HostPluginWrapper("NetGull.com", "NetGullCom", "http://[\\w\\.]*?netgull\\.com/\\?d=[0-9a-fA-F]+");
        new HostPluginWrapper("MyUpload.dk", "MyuploadDK", "http://[\\w\\.]*?myupload\\.dk/showfile/[0-9a-fA-F]+");
        new HostPluginWrapper("IndoWebster.com", "Indowebster", "http://[\\w\\.]*?indowebster\\.com/[^\\s]+\\.html");
        new HostPluginWrapper("QuickShare.cz", "QuickShareCz", "http://[\\w\\.]*?quickshare\\.cz/stahnout-soubor/\\d+:[^\\s]+");
        new HostPluginWrapper("RuTube.ru", "RuTubeRu", "http://[\\w\\.]*?rutube\\.ru/tracks/\\d+\\.html");
        new HostPluginWrapper("FileFlyer.com", "FileFlyerCom", "http://[\\w\\.]*?fileflyer\\.com/view/[\\w]+");
        new HostPluginWrapper("BizHat.com", "BizHatCom", "http://[\\w\\.]*?uploads\\.bizhat\\.com/file/[0-9]+");
        new HostPluginWrapper("MegaFTP.com", "MegaFtpCom", "http://[\\w\\.]*?megaftp\\.com/[0-9]+");
        new HostPluginWrapper("DuckLoad.com", "DuckLoad", "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/download/\\d+/.+");
        new HostPluginWrapper("HetrixLoad.com", "HetrixLoadCom", "http://[\\w\\.]*?hetrixload\\.com/files/[A-Za-z0-9]+/.+\\.html");
        new HostPluginWrapper("UserShare.net", "Usershare", "http://[\\w\\.]*?usershare\\.net/[0-9a-zA-Z.]+");
        new HostPluginWrapper("SlingFile.com", "SlingFileCom", "http://[\\w\\.]*?slingfile\\.com/(file|audio|video)/.+");
        new HostPluginWrapper("HyperFileShare.com", "HyperFileShareCom", "http://[\\w\\.]*?hyperfileshare\\.com/(d/|download\\.php\\?code=)[a-fA-F0-9]+");
        new HostPluginWrapper("zomgupload.com", "ZomgUploadCom", "http://[\\w\\.]*?zomgupload\\.com/.+[/0-9a-zA-Z]+.html");
        new HostPluginWrapper("hostuje.net", "HostUjeNet", "http://[\\w\\.]*?hostuje\\.net/file\\.php\\?id=.+");

        // Hoster from Extern
        new HostPluginWrapper("iFolder.ru", "IfolderRu", "http://[\\-\\w\\.]*?ifolder\\.ru/\\d+");
        new HostPluginWrapper("ExtraShare.us", "ExtraShareUs", "http://[\\w\\.]*?extrashare.us/file/.+/.+");
        new HostPluginWrapper("Addat.hu", "AddatHu", "http://[\\w\\.]*?addat.hu/.+/.+");
        new HostPluginWrapper("LinkFile.de", "LinkFileDe", "http://[\\w\\.]*?linkfile.de/download-[\\w]+\\.php");


    }

    public void loadPluginOptional() {

        if (JDUtilities.getJavaVersion() >= 1.6) new OptionalPluginWrapper("jdtrayicon.JDLightTray", 1.6, "trayicon", JDLocale.L("plugins.optional.trayicon.name", "Tray Icon (Minimizer)"));
        new OptionalPluginWrapper("webinterface.JDWebinterface", 1.5, "webinterface", JDLocale.L("plugins.optional.webinterface.name", "WebInterface"));
        new OptionalPluginWrapper("schedule.Schedule", 1.5, "scheduler", JDLocale.L("addons.schedule.name", "Schedule"));
        new OptionalPluginWrapper("JDFolderWatch", 1.5, "folderwatch", JDLocale.L("plugins.optional.folderwatch.name", "JDFolderWatch"));
        new OptionalPluginWrapper("JDShutdown", 1.5, "shutdown", JDLocale.L("plugins.optional.jdshutdown.name", "JDShutdown"));
        new OptionalPluginWrapper("JDRemoteControl", 1.5, "remotecontrol", JDLocale.L("plugins.optional.remotecontrol.name", "RemoteControl"));
        new OptionalPluginWrapper("jdchat.JDChat", 1.5, "chat", JDLocale.L("plugins.optional.jdchat.name2", "JD Support Chat"));
        new OptionalPluginWrapper("Newsfeeds", 1.5, "newsfeed", JDLocale.L("plugins.optional.newsfeeds.pluginTitle", "Newsfeed Check"));
        new OptionalPluginWrapper("JDInfoFileWriter", 1.5, "infofilewriter", JDLocale.L("plugins.optional.infoFileWriter.name", "Info File Writer"));
        new OptionalPluginWrapper("langfileeditor.LangFileEditor", 1.5, "langfileditor", JDLocale.L("plugins.optional.langfileeditor.name", "Language File Editor"));
        new OptionalPluginWrapper("jdunrar.JDUnrar", 1.5, "unrar", JDLocale.L("plugins.optional.jdunrar.name", "JD-Unrar"));
        new OptionalPluginWrapper("hjsplit.JDHJSplit", 1.5, "hjsplit", JDLocale.L("plugins.optional.jdhjsplit.name", "JD-HJMerge"));
        new OptionalPluginWrapper("JDPremiumCollector", 1.5, "premcol", JDLocale.L("plugins.optional.premiumcollector.name", "PremiumCollector"));
        if (OSDetector.isMac()) new OptionalPluginWrapper("JDGrowlNotification", 1.5, "growl", JDLocale.L("plugins.optional.jdgrowlnotification.name", "JDGrowlNotification"));
        new OptionalPluginWrapper("HTTPLiveHeaderScripter", 1.5, "livescripter", JDLocale.L("plugins.optional.httpliveheaderscripter.name", "HTTPLiveHeaderScripter"));

    }

    public void removeFiles() {
        String[] remove = Regex.getLines(JDIO.getLocalFile(JDUtilities.getResourceFile("outdated.dat")));
        String homedir = JDUtilities.getJDHomeDirectoryFromEnvironment().toString();
        if (remove != null) {
            for (String file : remove) {
                if (file.length() == 0) continue;
                if (!file.matches(".*?" + File.separator + "?\\.+" + File.separator + ".*?")) {
                    File delete = new File(homedir, file);
                    if (JDIO.removeDirectoryOrFile(delete)) logger.warning("Removed " + file);
                }
            }
        }
    }

}
