//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

//Links are coming from a decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "http://(vkontaktedecrypted\\.ru/(picturelink/(\\-)?\\d+_\\d+(\\?tag=\\d+)?|audiolink/\\d+|videolink/\\d+)|vk\\.com/doc\\d+_\\d+\\?hash=[a-z0-9]+)" }, flags = { 2 })
public class VKontakteRuHoster extends PluginForHost {

    private static final String DOMAIN                     = "http://vk.com";
    private static Object       LOCK                       = new Object();
    private String              FINALLINK                  = null;
    private static final String AUDIOLINK                  = "http://vkontaktedecrypted\\.ru/audiolink/\\d+";
    private static final String VIDEOLINK                  = "http://vkontaktedecrypted\\.ru/videolink/\\d+";
    private static final String DOCLINK                    = "http://vk\\.com/doc\\d+_\\d+\\?hash=[a-z0-9]+";
    private int                 MAXCHUNKS                  = 1;
    private static final String TEMPORARILYBLOCKED         = "You tried to load the same page more than once in one second|Sie haben versucht die Seite mehrfach innerhalb einer Sekunde zu laden";
    /** Settings stuff */
    private final String        USECOOKIELOGIN             = "USECOOKIELOGIN";
    private final String        FASTLINKCHECK              = "FASTLINKCHECK";
    private final String        FASTPICTURELINKCHECK       = "FASTPICTURELINKCHECK";
    private final String        FASTAUDIOLINKCHECK         = "FASTAUDIOLINKCHECK";
    private static final String ALLOW_BEST                 = "ALLOW_BEST";
    private static final String ALLOW_240P                 = "ALLOW_240P";
    private static final String ALLOW_360P                 = "ALLOW_360P";
    private static final String ALLOW_480P                 = "ALLOW_480P";
    private static final String ALLOW_720P                 = "ALLOW_720P";
    private static final String VKWALL_GRAB_ALBUMS         = "VKWALL_GRAB_ALBUMS";
    private static final String VKWALL_GRAB_PHOTOS         = "VKWALL_GRAB_PHOTOS";
    private static final String VKWALL_GRAB_AUDIO          = "VKWALL_GRAB_AUDIO";
    private static final String VKWALL_GRAB_VIDEO          = "VKWALL_GRAB_VIDEO";
    private static final String VKVIDEO_USEIDASPACKAGENAME = "VKVIDEO_USEIDASPACKAGENAME";

    public VKontakteRuHoster(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://vk.com/help.php?page=terms";
    }

    @SuppressWarnings("unchecked")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        FINALLINK = null;
        this.setBrowserExclusive();

        br.setFollowRedirects(false);
        // Login required to check/download
        if (link.getDownloadURL().matches(DOCLINK)) {
            MAXCHUNKS = 0;
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("File deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("This document is available only to its owner\\.")) {
                link.getLinkStatus().setStatusText("This document is available only to its owner");
                link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
                return AvailableStatus.TRUE;
            }
            String filename = br.getRegex("title>([^<>\"]*?)</title>").getMatch(0);
            FINALLINK = br.getRegex("var src = \\'(http://[^<>\"]*?)\\';").getMatch(0);
            if (filename == null || FINALLINK == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = Encoding.htmlDecode(filename.trim());
            if (!linkOk(link, filename)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            // Only log in if needed
            boolean noLogin = link.getBooleanProperty("nologin", false);
            Account aa = null;
            if (!noLogin) {
                aa = AccountController.getInstance().getValidAccount(this);
                if (aa == null) {
                    link.getLinkStatus().setStatusText("Only downlodable via account!");
                    return AvailableStatus.UNCHECKABLE;
                }
                login(br, aa, false);
            }
            if (link.getDownloadURL().matches(AUDIOLINK)) {
                String finalFilename = link.getFinalFileName();
                if (finalFilename == null) finalFilename = link.getName();
                final String audioID = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
                FINALLINK = link.getStringProperty("directlink", null);
                if (!linkOk(link, finalFilename)) {
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.postPage("http://vk.com/audio", link.getStringProperty("postdata", null));
                    FINALLINK = br.getRegex("\\'" + audioID + "\\',\\'(http://cs\\d+\\.[a-z0-9]+\\.[a-z]{2,4}/u\\d+/audios?/[a-z0-9]+\\.mp3)\\'").getMatch(0);
                    if (FINALLINK == null) {
                        logger.info("vk.com: FINALLINK is null in availablecheck");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (!linkOk(link, finalFilename)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    link.setProperty("directlink", FINALLINK);
                }
            } else if (link.getDownloadURL().matches(VIDEOLINK)) {
                if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                MAXCHUNKS = 0;
                br.setFollowRedirects(true);
                FINALLINK = link.getStringProperty("directlink", null);
                // Check if directlink is expired
                if (!linkOk(link, link.getFinalFileName())) {
                    final String oid = link.getStringProperty("userid", null);
                    final String id = link.getStringProperty("videoid", null);
                    final String embedhash = link.getStringProperty("embedhash", null);
                    if (link.getBooleanProperty("videospecial", false)) {
                        br.getPage("http://vk.com/video.php?act=a_flash_vars&vid=" + oid + "_" + id);
                    } else {
                        br.getPage("http://vk.com/video_ext.php?oid=" + oid + "&id=" + id + "&hash=" + embedhash);
                    }
                    final LinkedHashMap<String, String> availableQualities = findAvailableVideoQualities();
                    if (availableQualities == null) {
                        logger.info("vk.com: Couldn't find any available qualities for videolink");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    FINALLINK = availableQualities.get(link.getStringProperty("selectedquality", null));
                    if (FINALLINK == null) {
                        logger.warning("Could not find new link for selected quality...");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (!linkOk(link, link.getStringProperty("directfilename", null))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                FINALLINK = link.getStringProperty("picturedirectlink", null);
                if (FINALLINK == null) {
                    final String[] qs = { "w_", "z_", "y_", "x_", "m_" };
                    // For photos which are actually offline but their directlinks still exist
                    String directLinks = link.getStringProperty("directlinks", null);
                    if (directLinks != null) {
                        directLinks = Encoding.htmlDecode(directLinks).replace("\\", "");
                        /**
                         * Try to get best quality and test links till a working link is found as it can happen that the found link is
                         * offline but others are online
                         */
                        final String base = new Regex(directLinks, "base(\\')?:(\"|\\')(http://[^<>\"]*?)(\"|\\')").getMatch(2);
                        for (String q : qs) {
                            /* large image */
                            if (FINALLINK == null || (FINALLINK != null && !linkOk(link, null))) {
                                if (base == null) {
                                    FINALLINK = new Regex(directLinks, q + "(\\')?:\\[(\"|\\')([^<>\"]*?)(\"|\\')").getMatch(2);
                                    if (FINALLINK != null) FINALLINK += ".jpg";
                                } else {
                                    final String linkPart = new Regex(directLinks, q + "(\\')?:\\[(\"|\\')([^<>\"]*?)(\"|\\')").getMatch(2);
                                    if (linkPart != null) {
                                        FINALLINK = base + linkPart + ".jpg";
                                    }
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    if (FINALLINK == null) {
                        final String photoID = new Regex(link.getDownloadURL(), "vkontaktedecrypted\\.ru/picturelink/((\\-)?\\d+_\\d+)").getMatch(0);
                        String albumID = link.getStringProperty("albumid");
                        if (albumID == null) {
                            getPageSafe(aa, link, "http://vk.com/photo" + photoID);
                            if (br.containsHTML("Unknown error|Unbekannter Fehler")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            if (br.containsHTML("Access denied")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            albumID = br.getRegex("class=\"active_link\">[\t\n\r ]+<a href=\"/(.*?)\"").getMatch(0);
                            if (albumID == null) {
                                logger.info("vk.com: albumID is null");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                        postPageSafe(aa, link, "http://vk.com/al_photos.php", "act=show&al=1&module=photos&list=" + albumID + "&photo=" + photoID);
                        if (br.containsHTML(">Unfortunately, this photo has been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        final String correctedBR = br.toString().replace("\\", "");
                        /**
                         * Try to get best quality and test links till a working link is found as it can happen that the found link is
                         * offline but others are online
                         */
                        for (String q : qs) {
                            /* large image */
                            if (FINALLINK == null || (FINALLINK != null && !linkOk(link, null))) {
                                String base = new Regex(correctedBR, "\"id\":\"" + photoID + "\",\"base\":\"(http://.*?)\"").getMatch(0);
                                if (base == null) base = "";
                                final String section = new Regex(correctedBR, "(\\{\"id\":\"" + photoID + "\",\"base\":\"" + base + ".*?)((,\\{)|$)").getMatch(0);
                                if (base != null) {
                                    FINALLINK = new Regex(section, "\"id\":\"" + photoID + "\",\"base\":\"" + base + "\".*?\"" + q + "src\":\"(" + base + ".*?)\"").getMatch(0);
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
                if (FINALLINK == null) {
                    logger.warning("vk.com: Finallink is null!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setProperty("picturedirectlink", FINALLINK);
            }
        }
        return AvailableStatus.TRUE;

    }

    private void generalErrorhandling() throws PluginException {
        if (br.containsHTML(TEMPORARILYBLOCKED)) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many requests in a short time", 60 * 1000l); }
    }

    private boolean linkOk(final DownloadLink downloadLink, final String finalfilename) throws IOException {
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(FINALLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                if (finalfilename == null) {
                    downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                } else {
                    downloadLink.setFinalFileName(finalfilename);
                }
            } else {
                return false;
            }
            return true;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        // Doc-links and other links with permission can be downloaded without login
        if (downloadLink.getDownloadURL().matches(DOCLINK)) {
            requestFileInformation(downloadLink);
            doFree(downloadLink);
        } else if (downloadLink.getBooleanProperty("nologin", false)) {
            requestFileInformation(downloadLink);
            doFree(downloadLink);
        } else {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Download only possible with account!");
        }
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (downloadLink.getDownloadURL().matches(DOCLINK)) {
            if (br.containsHTML("This document is available only to its owner\\.")) { throw new PluginException(LinkStatus.ERROR_FATAL, "This document is available only to its owner"); }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, FINALLINK, true, MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.info("vk.com: Plugin broken after download-try");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            if (this.getPluginConfig().getBooleanProperty(USECOOKIELOGIN, false)) {
                logger.info("Logging in with cookies.");
                login(br, account, false);
                logger.info("Logged in successfully with cookies...");
            } else {
                logger.info("Logging in without cookies (forced login)...");
                login(br, account, true);
                logger.info("Logged in successfully without cookies (forced login)!");
            }
        } catch (PluginException e) {
            logger.info("Login failed!");
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(br, account, false);
        doFree(link);
    }

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(DOMAIN, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://vk.com/login.php");
                String damnIPH = br.getRegex("name=\"ip_h\" value=\"(.*?)\"").getMatch(0);
                if (damnIPH == null) damnIPH = br.getRegex("\\{loginscheme: \\'https\\', ip_h: \\'(.*?)\\'\\}").getMatch(0);
                if (damnIPH == null) damnIPH = br.getRegex("loginscheme: \\'https\\'.*?ip_h: \\'(.*?)\\'").getMatch(0);
                if (damnIPH == null) {
                    logger.info("damnIPH String is null, marking account as invalid...");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.postPage("https://login.vk.com/", "act=login&success_url=&fail_url=&try_to_login=1&to=&vk=1&al_test=3&from_host=vk.com&from_protocol=http&ip_h=" + damnIPH + "&email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&expire=");
                if (br.getCookie(DOMAIN, "remixsid") == null) {
                    logger.info("remixsid cookie is null, marking account as invalid...");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Finish login
                final Form lol = br.getFormbyProperty("name", "login");
                if (lol != null) {
                    lol.put("email", Encoding.urlEncode(account.getUser()));
                    lol.put("pass", Encoding.urlEncode(account.getPass()));
                    lol.put("expire", "0");
                    br.submitForm(lol);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(DOMAIN);
                for (final Cookie c : add.getCookies()) {
                    if ("deleted".equals(c.getValue())) continue;
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private void prepBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
        // Set english language
        br.setCookie("http://vk.com/", "remixlang", "3");
    }

    /** Same function in hoster and decrypterplugin, sync it!! */
    private LinkedHashMap<String, String> findAvailableVideoQualities() {
        /** Find needed information */
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        final String[][] qualities = { { "url720", "720p" }, { "url480", "480p" }, { "url360", "360p" }, { "url240", "240p" } };
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        for (final String[] qualityInfo : qualities) {
            final String finallink = getJson(qualityInfo[0]);
            if (finallink != null) {
                foundQualities.put(qualityInfo[1], finallink);
            }
        }
        return foundQualities;
    }

    private String getJson(final String key) {
        return br.getRegex("\"" + key + "\":\"(http:[^<>\"]*?)\"").getMatch(0);
    }

    // Handle all kinds of stuff that disturbs the downloadflow
    private void getPageSafe(final Account acc, final DownloadLink dl, final String page) throws Exception {
        br.getPage(page);
        if (acc != null && br.getRedirectLocation() != null && br.getRedirectLocation().contains("login.vk.com/?role=fast")) {
            logger.info("Avoiding 'https://login.vk.com/?role=fast&_origin=' security check by re-logging in...");
            // Force login
            login(br, acc, true);
            br.getPage(page);
        } else if (acc != null && br.toString().length() < 100 && br.toString().trim().matches("\\d+<\\!><\\!>\\d+<\\!>\\d+<\\!>\\d+<\\!>[a-z0-9]+")) {
            logger.info("Avoiding possible outdated cookie/invalid account problem by re-logging in...");
            // Force login
            login(br, acc, true);
            br.getPage(page);
        }
        generalErrorhandling();
    }

    private void postPageSafe(final Account acc, final DownloadLink dl, final String page, final String postData) throws Exception {
        br.postPage(page, postData);
        if (acc != null && br.getRedirectLocation() != null && br.getRedirectLocation().contains("login.vk.com/?role=fast")) {
            logger.info("Avoiding 'https://login.vk.com/?role=fast&_origin=' security check by re-logging in...");
            // Force login
            login(br, acc, true);
            br.postPage(page, postData);
        } else if (acc != null && br.toString().length() < 100 && br.toString().trim().matches("\\d+<\\!><\\!>\\d+<\\!>\\d+<\\!>\\d+<\\!>[a-z0-9]+")) {
            logger.info("Avoiding possible outdated cookie/invalid account problem by re-logging in...");
            // Force login
            login(br, acc, true);
            br.postPage(page, postData);
        }
        generalErrorhandling();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getDescription() {
        return "JDownloader's Vk Plugin helps downloading videoclips from vk.com. Vk provides different video formats and qualities.";
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "General settings:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USECOOKIELOGIN, JDL.L("plugins.hoster.vkontakteruhoster.alwaysUseCookiesForLogin", "Always use cookies for login (this can cause out of date errors)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Linkcheck settings:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, JDL.L("plugins.hoster.vkontakteruhoster.fastLinkcheck", "Fast linkcheck for video links (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTPICTURELINKCHECK, JDL.L("plugins.hoster.vkontakteruhoster.fastPictureLinkcheck", "Fast linkcheck for picture links (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTAUDIOLINKCHECK, JDL.L("plugins.hoster.vkontakteruhoster.fastAudioLinkcheck", "Fast linkcheck for audio links (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Video settings:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.vkontakteruhoster.checkbest", "Only grab the best available resolution")).setDefaultValue(false);
        getConfig().addEntry(hq);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_240P, JDL.L("plugins.hoster.vkontakteruhoster.check240", "Grab 240p MP4/FLV?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_360P, JDL.L("plugins.hoster.vkontakteruhoster.check360", "Grab 360p MP4?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480P, JDL.L("plugins.hoster.vkontakteruhoster.check480", "Grab 480p MP4?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720P, JDL.L("plugins.hoster.vkontakteruhoster.check720", "Grab 720p MP4?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/wall-numbers' and 'vk.com/wall-numbers_numbers' links:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), VKWALL_GRAB_ALBUMS, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckalbums", "Grab album links ('vk.com/album')?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), VKWALL_GRAB_PHOTOS, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckphotos", "Grab photo links ('vk.com/photo')?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), VKWALL_GRAB_AUDIO, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckaudio", "Grab audio links (.mp3 directlinks)?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), VKWALL_GRAB_VIDEO, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckvideo", "Grab video links ('vk.com/video')?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/video' links: "));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), VKVIDEO_USEIDASPACKAGENAME, JDL.L("plugins.hoster.vkontakteruhoster.videoUseIdAsPackagename", "Use video-ID as packagename ('videoXXXX_XXXX' or 'video-XXXX_XXXX')?")).setDefaultValue(false));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}