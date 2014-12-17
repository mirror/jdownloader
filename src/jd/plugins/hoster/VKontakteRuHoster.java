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
import java.util.regex.Pattern;

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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

//Links are coming from a decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "http://(vkontaktedecrypted\\.ru/(picturelink/(\\-)?[\\d\\-]+_[\\d\\-]+(\\?tag=[\\d\\-]+)?|audiolink/[\\d\\-]+_[\\d\\-]+|videolink/[\\d\\-]+)|vk\\.com/doc[\\d\\-]+_[\\d\\-]+(\\?hash=[a-z0-9]+)?)" }, flags = { 2 })
public class VKontakteRuHoster extends PluginForHost {

    private static final String DOMAIN                      = "http://vk.com";
    private static Object       LOCK                        = new Object();
    private String              finalUrl                    = null;
    private static final String AUDIOLINK                   = "http://vkontaktedecrypted\\.ru/audiolink/[\\d\\-]+_[\\d\\-]+";
    private static final String VIDEOLINK                   = "http://vkontaktedecrypted\\.ru/videolink/[\\d\\-]+";
    private static final String DOCLINK                     = "http://vk\\.com/doc[\\d\\-]+_[\\d\\-]+(\\?hash=[a-z0-9]+)?";
    private int                 MAXCHUNKS                   = 1;
    private static final String TEMPORARILYBLOCKED          = jd.plugins.decrypter.VKontakteRu.TEMPORARILYBLOCKED;
    /* Settings stuff */
    private static final String USECOOKIELOGIN              = "USECOOKIELOGIN";
    private static final String FASTLINKCHECK_VIDEO         = "FASTLINKCHECK_VIDEO";
    private static final String FASTLINKCHECK_PICTURES      = "FASTLINKCHECK_PICTURES";
    private static final String FASTLINKCHECK_AUDIO         = "FASTLINKCHECK_AUDIO";
    private static final String ALLOW_BEST                  = "ALLOW_BEST";
    private static final String ALLOW_240P                  = "ALLOW_240P";
    private static final String ALLOW_360P                  = "ALLOW_360P";
    private static final String ALLOW_480P                  = "ALLOW_480P";
    private static final String ALLOW_720P                  = "ALLOW_720P";
    private static final String VKWALL_GRAB_ALBUMS          = "VKWALL_GRAB_ALBUMS";
    private static final String VKWALL_GRAB_PHOTOS          = "VKWALL_GRAB_PHOTOS";
    private static final String VKWALL_GRAB_AUDIO           = "VKWALL_GRAB_AUDIO";
    private static final String VKWALL_GRAB_VIDEO           = "VKWALL_GRAB_VIDEO";
    private static final String VKWALL_GRAB_LINK            = "VKWALL_GRAB_LINK";
    private static final String VKVIDEO_USEIDASPACKAGENAME  = "VKVIDEO_USEIDASPACKAGENAME";
    private static final String VKPHOTO_CORRECT_FINAL_LINKS = "VKPHOTO_CORRECT_FINAL_LINKS";

    public VKontakteRuHoster(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (downloadLink.getDownloadURL().matches(VKontakteRuHoster.DOCLINK)) {
            if (this.br.containsHTML("This document is available only to its owner\\.")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This document is available only to its owner");
            }
        }
        br.getHeaders().put("Accept-Encoding", "identity");
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, this.finalUrl, true, this.MAXCHUNKS);
        final URLConnectionAdapter con = this.dl.getConnection();
        if (con.getResponseCode() == 416) {
            con.disconnect();
            this.logger.info("Resume failed --> Retrying from zero");
            downloadLink.setChunksProgress(null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (con.getContentType().contains("html")) {
            this.logger.info("vk.com: Plugin broken after download-try");
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            if (this.getPluginConfig().getBooleanProperty(this.USECOOKIELOGIN, false)) {
                this.logger.info("Logging in with cookies.");
                this.login(this.br, account, false);
                this.logger.info("Logged in successfully with cookies...");
            } else {
                this.logger.info("Logging in without cookies (forced login)...");
                this.login(this.br, account, true);
                this.logger.info("Logged in successfully without cookies (forced login)!");
            }
        } catch (final PluginException e) {
            this.logger.info("Login failed!");
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    /* Same function in hoster and decrypterplugin, sync it!! */
    private LinkedHashMap<String, String> findAvailableVideoQualities() {
        /* Find needed information */
        this.br.getRequest().setHtmlCode(this.br.toString().replace("\\", ""));
        final String[][] qualities = { { "url720", "720p" }, { "url480", "480p" }, { "url360", "360p" }, { "url240", "240p" } };
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        for (final String[] qualityInfo : qualities) {
            final String finallink = this.getJson(qualityInfo[0]);
            if (finallink != null) {
                foundQualities.put(qualityInfo[1], finallink);
            }
        }
        return foundQualities;
    }

    private void generalErrorhandling() throws PluginException {
        if (this.br.containsHTML(VKontakteRuHoster.TEMPORARILYBLOCKED)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many requests in a short time", 60 * 1000l);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://vk.com/help.php?page=terms";
    }

    private String getJson(final String key) {
        return getJson(this.br.toString(), key);
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.\\-]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /* Handle all kinds of stuff that disturbs the downloadflow */
    private void getPageSafe(final Account acc, final DownloadLink dl, final String page) throws Exception {
        this.br.getPage(page);
        if (acc != null && this.br.getRedirectLocation() != null && this.br.getRedirectLocation().contains("login.vk.com/?role=fast")) {
            this.logger.info("Avoiding 'https://login.vk.com/?role=fast&_origin=' security check by re-logging in...");
            // Force login
            this.login(this.br, acc, true);
            this.br.getPage(page);
        } else if (acc != null && this.br.toString().length() < 100 && this.br.toString().trim().matches("\\d+<\\!><\\!>\\d+<\\!>\\d+<\\!>\\d+<\\!>[a-z0-9]+")) {
            this.logger.info("Avoiding possible outdated cookie/invalid account problem by re-logging in...");
            // Force login
            this.login(this.br, acc, true);
            this.br.getPage(page);
        }
        this.generalErrorhandling();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        /* Doc-links and other links with permission can be downloaded without login */
        if (downloadLink.getDownloadURL().matches(VKontakteRuHoster.DOCLINK)) {
            this.requestFileInformation(downloadLink);
            this.doFree(downloadLink);
        } else if (downloadLink.getBooleanProperty("nologin", false)) {
            this.requestFileInformation(downloadLink);
            this.doFree(downloadLink);
        } else {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Download only possible with account!");
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.requestFileInformation(link);
        this.login(this.br, account, false);
        this.doFree(link);
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */

    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    /**
     * Checks a given directlink for content. Sets finalfilename as final filename if finalfilename != null - else sets server filename as
     * final filename.
     * 
     * @return <b>true</b>: Link is valid and can be downloaded <b>false</b>: Link leads to HTML, times out or other problems occured - link
     *         is not downloadable!
     */
    private boolean linkOk(final DownloadLink downloadLink, final String finalfilename) throws IOException {
        final Browser br2 = this.br.cloneBrowser();
        /* In case the link redirects to the finallink */
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            br2.getHeaders().put("Accept-Encoding", "identity");
            try {
                con = br2.openGetConnection(this.finalUrl);
            } catch (final Throwable e) {
                return false;
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                if (finalfilename == null) {
                    downloadLink.setFinalFileName(Encoding.htmlDecode(Plugin.getFileNameFromHeader(con)));
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
            } catch (final Throwable e) {
            }
        }
    }

    /** TODO: Maybe add login via API: https://vk.com/dev/auth_mobile */
    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (VKontakteRuHoster.LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                this.prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(VKontakteRuHoster.DOMAIN, key, value);
                        }
                        return;
                    }
                }
                br.clearCookies("http://vk.com/login.php");
                br.setFollowRedirects(true);
                br.getPage("http://vk.com/login.php");
                String damnIPH = br.getRegex("name=\"ip_h\" value=\"(.*?)\"").getMatch(0);
                if (damnIPH == null) {
                    damnIPH = br.getRegex("\\{loginscheme: \\'https\\', ip_h: \\'(.*?)\\'\\}").getMatch(0);
                }
                if (damnIPH == null) {
                    damnIPH = br.getRegex("loginscheme: \\'https\\'.*?ip_h: \\'(.*?)\\'").getMatch(0);
                }
                if (damnIPH == null) {
                    this.logger.info("damnIPH String is null, marking account as invalid...");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.postPage("https://login.vk.com/", "act=login&success_url=&fail_url=&try_to_login=1&to=&vk=1&al_test=3&from_host=vk.com&from_protocol=http&ip_h=" + damnIPH + "&email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&expire=");
                if (br.getCookie(VKontakteRuHoster.DOMAIN, "remixsid") == null) {
                    this.logger.info("remixsid cookie is null, marking account as invalid...");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Finish login */
                final Form lol = br.getFormbyProperty("name", "login");
                if (lol != null) {
                    lol.put("email", Encoding.urlEncode(account.getUser()));
                    lol.put("pass", Encoding.urlEncode(account.getPass()));
                    lol.put("expire", "0");
                    br.submitForm(lol);
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(VKontakteRuHoster.DOMAIN);
                for (final Cookie c : add.getCookies()) {
                    if ("deleted".equalsIgnoreCase(c.getValue())) {
                        continue;
                    }
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

    private void postPageSafe(final Account acc, final DownloadLink dl, final String page, final String postData) throws Exception {
        this.br.postPage(page, postData);
        if (acc != null && this.br.getRedirectLocation() != null && this.br.getRedirectLocation().contains("login.vk.com/?role=fast")) {
            this.logger.info("Avoiding 'https://login.vk.com/?role=fast&_origin=' security check by re-logging in...");
            // Force login
            this.login(this.br, acc, true);
            this.br.postPage(page, postData);
        } else if (acc != null && this.br.toString().length() < 100 && this.br.toString().trim().matches("\\d+<\\!><\\!>\\d+<\\!>\\d+<\\!>\\d+<\\!>[a-z0-9]+")) {
            this.logger.info("Avoiding possible outdated cookie/invalid account problem by re-logging in...");
            // Force login
            this.login(this.br, acc, true);
            this.br.postPage(page, postData);
        }
        this.generalErrorhandling();
    }

    private void prepBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0");
        // Set english language
        br.setCookie("http://vk.com/", "remixlang", "3");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* Check if offline was set via decrypter */
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        this.finalUrl = null;
        this.setBrowserExclusive();

        this.br.setFollowRedirects(false);
        // Login required to check/download
        if (link.getDownloadURL().matches(VKontakteRuHoster.DOCLINK)) {
            this.MAXCHUNKS = 0;
            this.br.getPage(link.getDownloadURL());
            if (this.br.containsHTML("File deleted")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (this.br.containsHTML("This document is available only to its owner\\.")) {
                link.getLinkStatus().setStatusText("This document is available only to its owner");
                link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
                return AvailableStatus.TRUE;
            }
            String filename = this.br.getRegex("title>([^<>\"]*?)</title>").getMatch(0);
            this.finalUrl = this.br.getRegex("var src = \\'(http://[^<>\"]*?)\\';").getMatch(0);
            if (filename == null || this.finalUrl == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Sometimes filenames on site are cut - finallink usually contains the full filenames */
            final String betterFilename = new Regex(this.finalUrl, "docs/[a-z0-9]+/([^<>\"]*?)\\?extra=.+").getMatch(0);
            if (betterFilename != null) {
                filename = Encoding.htmlDecode(betterFilename).trim();
            } else {
                filename = Encoding.htmlDecode(filename.trim());
            }
            if (!this.linkOk(link, filename)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            // Only log in if needed
            final boolean noLogin = link.getBooleanProperty("nologin", false);
            Account aa = null;
            if (!noLogin) {
                aa = AccountController.getInstance().getValidAccount(this);
                if (aa == null) {
                    link.getLinkStatus().setStatusText("Only downlodable via account!");
                    return AvailableStatus.UNCHECKABLE;
                }
                this.login(this.br, aa, false);
            }
            if (link.getDownloadURL().matches(VKontakteRuHoster.AUDIOLINK)) {
                final String audioID = link.getStringProperty("owner_id", null) + "_" + link.getStringProperty("content_id", null) + "1";
                String finalFilename = link.getFinalFileName();
                if (finalFilename == null) {
                    finalFilename = link.getName();
                }
                this.finalUrl = link.getStringProperty("directlink", null);
                if (!this.linkOk(link, finalFilename)) {
                    this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    String post = "act=get_wall_playlist&al=1&local_id=" + link.getProperty("postID") + "&oid=" + link.getProperty("fromId") + "&wall_type=own";
                    br.postPage("https://vk.com/audio", post);
                    String url = br.getRegex("\"0\"\\:\"" + Pattern.quote(link.getProperty("owner_id") + "") + "\"\\,\"1\"\\:\"" + Pattern.quote(link.getProperty("content_id") + "") + "\"\\,\"2\"\\:(\"[^\"]+\")").getMatch(0);
                    // Decodes the json String
                    url = (String) DummyScriptEnginePlugin.jsonToJavaObject(url);
                    this.finalUrl = url;
                    if (this.finalUrl == null) {
                        this.logger.info("vk.com: FINALLINK is null in availablecheck --> Probably file is offline");
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }

                    if (!this.linkOk(link, finalFilename)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    link.setProperty("directlink", this.finalUrl);
                }
            } else if (link.getDownloadURL().matches(VKontakteRuHoster.VIDEOLINK)) {
                this.MAXCHUNKS = 0;
                this.br.setFollowRedirects(true);
                this.finalUrl = link.getStringProperty("directlink", null);
                // Check if directlink is expired
                if (!this.linkOk(link, link.getFinalFileName())) {
                    final String oid = link.getStringProperty("userid", null);
                    final String id = link.getStringProperty("videoid", null);
                    final String embedhash = link.getStringProperty("embedhash", null);
                    this.br.getPage("http://vk.com/video.php?act=a_flash_vars&vid=" + oid + "_" + id);
                    if (br.containsHTML("This video has been removed from public access")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final LinkedHashMap<String, String> availableQualities = this.findAvailableVideoQualities();
                    if (availableQualities == null) {
                        this.logger.info("vk.com: Couldn't find any available qualities for videolink");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    this.finalUrl = availableQualities.get(link.getStringProperty("selectedquality", null));
                    if (this.finalUrl == null) {
                        this.logger.warning("Could not find new link for selected quality...");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (!this.linkOk(link, link.getStringProperty("directfilename", null))) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                this.finalUrl = link.getStringProperty("picturedirectlink", null);
                if (this.finalUrl == null) {
                    // For photos which are actually offline but their directlinks still exist
                    String directLinks = link.getStringProperty("directlinks", null);
                    if (directLinks != null) {
                        getHighestQualityPic(link, directLinks);
                    }
                    if (this.finalUrl == null) {
                        final String photoID = new Regex(link.getDownloadURL(), "vkontaktedecrypted\\.ru/picturelink/((\\-)?[\\d\\-]+_[\\d\\-]+)").getMatch(0);
                        String albumID = link.getStringProperty("albumid");
                        if (albumID == null) {
                            this.getPageSafe(aa, link, "http://vk.com/photo" + photoID);
                            if (this.br.containsHTML("Unknown error|Unbekannter Fehler")) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            if (this.br.containsHTML("Access denied")) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            albumID = this.br.getRegex("class=\"active_link\">[\t\n\r ]+<a href=\"/(.*?)\"").getMatch(0);
                            if (albumID == null) {
                                this.logger.info("vk.com: albumID is null");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        this.br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                        this.postPageSafe(aa, link, "http://vk.com/al_photos.php", "act=show&al=1&module=photos&list=" + albumID + "&photo=" + photoID);
                        if (this.br.containsHTML(">Unfortunately, this photo has been deleted")) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        final String correctedBR = this.br.toString().replace("\\", "");
                        final String id_source = new Regex(correctedBR, "\\{(\"id\":\"" + photoID + ".*?)(\"id\"|\\}\\})").getMatch(0);
                        getHighestQualityPic(link, id_source);
                    }
                }
                if (this.finalUrl == null) {
                    this.logger.warning("vk.com: Finallink is null!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                photo_correctLink();
                link.setProperty("picturedirectlink", this.finalUrl);
            }
        }
        return AvailableStatus.TRUE;
    }

    /**
     * Try to get best quality and test links till a working link is found as it can happen that the found link is offline but others are
     * online
     * 
     * @throws IOException
     */
    private void getHighestQualityPic(final DownloadLink dl, String source) throws Exception {
        if (source == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        source = Encoding.htmlDecode(source).replace("\\", "");
        /* New way */
        if (source.contains("\"type\":\"photo\"")) {
            final String[] qualitylinks = new Regex(source, "\"photo_\\d+\":\"(http[^<>\"]*?)\"").getColumn(0);
            if (qualitylinks != null && qualitylinks.length > 0) {
                this.finalUrl = qualitylinks[qualitylinks.length - 1];
                /* Do this to set the final filename and get size */
                this.linkOk(dl, null);
            }
        } else {
            /* Old way */
            final String[] qs = { "w_", "z_", "y_", "x_", "m_" };
            final String base = new Regex(source, "base(\\')?:(\"|\\')(http://[^<>\"]*?)(\"|\\')").getMatch(2);
            for (final String q : qs) {
                /* large image */
                if (this.finalUrl == null || this.finalUrl != null && !this.linkOk(dl, null)) {
                    if (base == null) {
                        this.finalUrl = new Regex(source, q + "(\\')?:\\[(\"|\\')([^<>\"]*?)(\"|\\')").getMatch(2);
                        if (this.finalUrl != null) {
                            this.finalUrl += ".jpg";
                        } else {
                            /* Other source has complete links */
                            this.finalUrl = new Regex(source, "\"" + q + "src\":\"(http[^<>\"]*?)\"").getMatch(0);
                        }
                    } else {
                        final String linkPart = new Regex(source, q + "(\\')?:\\[(\"|\\')([^<>\"]*?)(\"|\\')").getMatch(2);
                        if (linkPart != null) {
                            this.finalUrl = base + linkPart + ".jpg";
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Changes server of picture links if wished by user - if not it will change them back to their "original" format. On error (server does
     * not match expected) it won't touch the current finallink at all! Only use this for photo links!
     */
    private void photo_correctLink() {
        if (this.getPluginConfig().getBooleanProperty(VKPHOTO_CORRECT_FINAL_LINKS, false)) {
            logger.info("VKPHOTO_CORRECT_FINAL_LINKS enabled --> Correcting finallink");
            /* Correct server to get files that are otherwise inaccessible */
            final String oldserver = new Regex(this.finalUrl, "(https?://cs\\d+\\.vk\\.me/)").getMatch(0);
            final String serv_id = new Regex(this.finalUrl, "cs(\\d+)\\.vk\\.me/").getMatch(0);
            if (oldserver != null && serv_id != null) {
                final String newserver = "https://pp.vk.me/c" + serv_id + "/";
                this.finalUrl = this.finalUrl.replace(oldserver, newserver);
                logger.info("VKPHOTO_CORRECT_FINAL_LINKS enabled --> SUCCEEDED to correct finallink");
            } else {
                logger.warning("VKPHOTO_CORRECT_FINAL_LINKS enabled --> FAILED to correct finallink");
            }
        } else {
            logger.info("VKPHOTO_CORRECT_FINAL_LINKS DISABLED --> changing link back to standard");
            /* Correct links to standard format */
            final Regex dataregex = new Regex(this.finalUrl, "(https?://pp\\.vk\\.me/c)(\\d+)/v(\\d+)/");
            final String serv_id = dataregex.getMatch(1);
            final String oldserver = dataregex.getMatch(0) + serv_id + "/";
            if (oldserver != null && serv_id != null) {
                final String newserver = "http://cs" + serv_id + ".vk.me/";
                this.finalUrl = this.finalUrl.replace(oldserver, newserver);
                logger.info("VKPHOTO_CORRECT_FINAL_LINKS DISABLE --> SUCCEEDED to revert corrected finallink");
            } else {
                logger.warning("VKPHOTO_CORRECT_FINAL_LINKS enabled --> FAILED to revert corrected finallink");
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public String getDescription() {
        return "JDownloader's Vk Plugin helps downloading all sorts of media from vk.com.";
    }

    private static final boolean default_USECOOKIELOGIN                     = false;
    private static final boolean default_fastlinkcheck_FASTLINKCHECK        = true;
    private static final boolean default_fastlinkcheck_FASTPICTURELINKCHECK = true;
    private static final boolean default_fastlinkcheck_FASTAUDIOLINKCHECK   = true;
    private static final boolean default_ALLOW_BEST                         = false;
    private static final boolean default_ALLOW_240p                         = true;
    private static final boolean default_ALLOW_360p                         = true;
    private static final boolean default_ALLOW_480p                         = true;
    private static final boolean default_ALLOW_720p                         = true;
    private static final boolean default_WALL_ALLOW_albums                  = true;
    private static final boolean default_WALL_ALLOW_photo                   = true;
    private static final boolean default_WALL_ALLOW_audio                   = true;
    private static final boolean default_WALL_ALLOW_video                   = true;
    private static final boolean default_WALL_ALLOW_links                   = false;
    private static final boolean default_VKVIDEO_USEIDASPACKAGENAME         = false;
    private static final boolean default_VKPHOTO_CORRECT_FINAL_LINKS        = false;

    public void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "General settings:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), USECOOKIELOGIN, JDL.L("plugins.hoster.vkontakteruhoster.alwaysUseCookiesForLogin", "Always use cookies for login (this can cause out of date errors)")).setDefaultValue(default_USECOOKIELOGIN));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Linkcheck settings:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), FASTLINKCHECK_VIDEO, JDL.L("plugins.hoster.vkontakteruhoster.fastLinkcheck", "Fast linkcheck for video links (filesize won't be shown in linkgrabber)?")).setDefaultValue(default_fastlinkcheck_FASTLINKCHECK));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), FASTLINKCHECK_PICTURES, JDL.L("plugins.hoster.vkontakteruhoster.fastPictureLinkcheck", "Fast linkcheck for picture links (filesize won't be shown in linkgrabber)?")).setDefaultValue(default_fastlinkcheck_FASTPICTURELINKCHECK));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), FASTLINKCHECK_AUDIO, JDL.L("plugins.hoster.vkontakteruhoster.fastAudioLinkcheck", "Fast linkcheck for audio links (filesize won't be shown in linkgrabber)?")).setDefaultValue(default_fastlinkcheck_FASTAUDIOLINKCHECK));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Video settings:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_BEST, JDL.L("plugins.hoster.vkontakteruhoster.checkbest", "Only grab the best available resolution")).setDefaultValue(default_ALLOW_BEST);
        this.getConfig().addEntry(hq);
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_240P, JDL.L("plugins.hoster.vkontakteruhoster.check240", "Grab 240p MP4/FLV?")).setDefaultValue(default_ALLOW_240p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_360P, JDL.L("plugins.hoster.vkontakteruhoster.check360", "Grab 360p MP4?")).setDefaultValue(default_ALLOW_360p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_480P, JDL.L("plugins.hoster.vkontakteruhoster.check480", "Grab 480p MP4?")).setDefaultValue(default_ALLOW_480p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_720P, JDL.L("plugins.hoster.vkontakteruhoster.check720", "Grab 720p MP4?")).setDefaultValue(default_ALLOW_720p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/wall-numbers' and 'vk.com/wall-numbers_numbers' links:\r\n NOTE: You can't turn off all types. If you do that, JD will decrypt all instead!"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_ALBUMS, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckalbums", "Grab album links ('vk.com/album')?")).setDefaultValue(default_WALL_ALLOW_albums));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_PHOTOS, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckphotos", "Grab photo links ('vk.com/photo')?")).setDefaultValue(default_WALL_ALLOW_photo));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_AUDIO, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckaudio", "Grab audio links (.mp3 directlinks)?")).setDefaultValue(default_WALL_ALLOW_audio));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_VIDEO, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckvideo", "Grab video links ('vk.com/video')?")).setDefaultValue(default_WALL_ALLOW_video));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_LINK, JDL.L("plugins.hoster.vkontakteruhoster.wallchecklink", "Grab links ('vk.com/xxxx')?")).setDefaultValue(default_WALL_ALLOW_links));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/video' links:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKVIDEO_USEIDASPACKAGENAME, JDL.L("plugins.hoster.vkontakteruhoster.videoUseIdAsPackagename", "Use video-ID as packagename ('videoXXXX_XXXX' or 'video-XXXX_XXXX')?")).setDefaultValue(default_VKVIDEO_USEIDASPACKAGENAME));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/photo' links:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKPHOTO_CORRECT_FINAL_LINKS, JDL.L("plugins.hoster.vkontakteruhoster.correctFinallinks", "Change final downloadlinks from 'https?://csXXX.vk.me/vXXX/...' to 'https://pp.vk.me/cXXX/vXXX/...' (forces HTTPS)?")).setDefaultValue(default_VKPHOTO_CORRECT_FINAL_LINKS));
    }

}