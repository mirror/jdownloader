//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "https?://(?:www\\.)?(facebookdecrypted\\.com/(video\\.php\\?v=|photo\\.php\\?fbid=|download/)\\d+|facebook\\.com/download/\\d+)" })
public class FaceBookComVideos extends PluginForHost {
    private String              FACEBOOKMAINPAGE      = "http://www.facebook.com";
    private String              PREFERHD              = "PREFERHD";
    private static final String TYPE_SINGLE_PHOTO     = "https?://(www\\.)?facebook\\.com/photo\\.php\\?fbid=\\d+";
    private static final String TYPE_SINGLE_VIDEO_ALL = "https?://(www\\.)?facebook\\.com/video\\.php\\?v=\\d+";
    private static final String TYPE_DOWNLOAD         = "https?://(www\\.)?facebook\\.com/download/\\d+";
    private final String        regexFileExtension    = "\\.[a-z0-9]{3}";
    private static final String REV_2                 = jd.plugins.decrypter.FaceBookComGallery.REV_2;
    private static final String REV_3                 = jd.plugins.decrypter.FaceBookComGallery.REV_3;
    // five minutes, not 30seconds! -raztoki20160309
    private static final long   trust_cookie_age      = 300000l;
    private static Object       LOCK                  = new Object();
    private String              dllink                = null;
    private boolean             loggedIN              = false;
    private boolean             accountNeeded         = false;
    private int                 maxChunks             = 0;
    private boolean             is_private            = false;

    public FaceBookComVideos(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.facebook.com/r.php");
        setConfigElements();
        /*
         * to prevent all downloads starting and finishing together (quite common with small image downloads), login, http request and json
         * task all happen at same time and cause small hangups and slower download speeds. raztoki20160309
         */
        setStartIntervall(200l);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("facebookdecrypted.com/", "facebook.com/"));
        String thislink = link.getDownloadURL();
        String videoID = new Regex(thislink, "facebook\\.com/video\\.php\\?v=(\\d+)").getMatch(0);
        if (videoID != null) {
            thislink = "https://facebook.com/video.php?v=" + videoID;
        }
        link.setUrlDownload(thislink);
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = new Browser();
        is_private = link.getBooleanProperty("is_private", false);
        dllink = link.getStringProperty("directlink", null);
        final String lid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (!link.isNameSet()) {
            link.setName(lid);
        }
        br.setCookie("http://www.facebook.com", "locale", "en_GB");
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null && aa.isValid()) {
            login(aa, br);
            loggedIN = true;
        }
        String filename = null;
        URLConnectionAdapter con = null;
        if (link.getDownloadURL().matches(TYPE_SINGLE_PHOTO) && is_private) {
            accountNeeded = true;
            if (!loggedIN) {
                return AvailableStatus.UNCHECKABLE;
            }
            br.getPage(FACEBOOKMAINPAGE);
            final String user = getUser(br);
            final String image_id = getPICID(link);
            if (user == null || image_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String getdata = "?photo_id=" + image_id + "&__pc=EXP1%3ADEFAULT&__user=" + user + "&__a=1&__dyn=" + jd.plugins.decrypter.FaceBookComGallery.getDyn() + "&__req=11&__rev=" + jd.plugins.decrypter.FaceBookComGallery.getRev(this.br);
            br.getPage("https://www.facebook.com/mercury/attachments/photo/" + getdata);
            br.getRequest().setHtmlCode(this.br.toString().replace("\\", ""));
            dllink = br.getRegex("\"(https?://[^/]+\\.fbcdn\\.net/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https?://[^<>\"]+\\&dl=1)").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            maxChunks = 1;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    filename = Encoding.htmlDecode(getFileNameFromHeader(con));
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else if (link.getDownloadURL().matches(TYPE_DOWNLOAD)) {
            maxChunks = 1;
            try {
                dllink = link.getDownloadURL();
                con = br.openGetConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    filename = Encoding.htmlDecode(getFileNameFromHeader(con));
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else { // Video and TYPE_SINGLE_PHOTO !is_private go here
            br.getPage(link.getDownloadURL());
            /* 2016-05-31: Removed this errorhandling as it caused false-positive-offline urls again and again! */
            // if (!br.containsHTML("class=\"uiStreamPrivacy inlineBlock fbStreamPrivacy fbPrivacyAudienceIndicator") && !loggedIN) {
            // accountNeeded = true;
            // /*
            // * Actually we cannot know whether the video is online or not but even when we're logged in we can get the message similar
            // * to this: "This video is offline or you cannot view it due to someone elses privacy settings" --> Basically we can guess
            // * that most of such URLs added by our users are offline and it is NOT a privacy settings / rights issue!
            // */
            // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // }
            String getThisPage = br.getRegex("window\\.location\\.replace\\(\"(http:.*?)\"").getMatch(0);
            if (getThisPage != null) {
                br.getPage(getThisPage.replace("\\", ""));
            }
            if (this.br.getHttpConnection().getResponseCode() == 404 || isOffline(this.br)) {
                link.setFinalFileName(link.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (loggedIN) {
                filename = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"mtm mbs mrs fsm fwn fcg\">[A-Za-z0-9:]+</span>([^<>\"]*?)</div>").getMatch(0);
                }
            } else {
                filename = br.getRegex("id=\"pageTitle\">([^<>\"]*?)\\| Facebook</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
                }
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = Encoding.htmlDecode(filename.trim());
            // ive seen new lines within filename!
            filename = filename.replaceAll("[\r\n]+", " ");
            if (br.containsHTML(">You must log in to continue")) {
                accountNeeded = true;
                if (!loggedIN) {
                    logger.info("You must log in to continue");
                    return AvailableStatus.UNCHECKABLE;
                }
            }
            if (link.getDownloadURL().matches(TYPE_SINGLE_PHOTO)) { // /photo.php?fbid=\\d+
                // Try if a downloadlink is available
                dllink = br.getRegex("href=\"(https?://[^<>\"]*?(\\?|\\&amp;)dl=1)\"").getMatch(0);
                // Try to find original quality link
                String setID = br.getRegex("\"set\":\"([^<>\"]*?)\"").getMatch(0);
                if (setID == null) {
                    // with loggedIN it's not presented... all setids are the same with this page -raztoki20160119
                    setID = br.getRegex("set=([a-z\\.0-9]+)").getMatch(0);
                }
                final String user = getUser(this.br);
                final String ajaxpipe_token = getajaxpipeToken();
                /*
                 * If no downloadlink is there, simply try to find the fullscreen link to the picture which is located on the "theatre view"
                 * page
                 */
                final String fbid = getPICID(link);
                if (setID != null && user != null && ajaxpipe_token != null && dllink == null) {
                    try {
                        logger.info("Trying to get original quality image");
                        final String data = "{\"type\":\"1\",\"fbid\":\"" + fbid + "\",\"set\":\"" + setID + "\",\"firstLoad\":true,\"ssid\":0,\"av\":\"0\"}";
                        final Browser br2 = br.cloneBrowser();
                        final String theaterView = "https://www.facebook.com/ajax/pagelet/generic.php/PhotoViewerInitPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=7n8ajEyl2qm9udDgDxyF4EihUtCxO4p9GgSmEZ9LFwxBxCuUWdDx2ubhHximmey8OdUS8w&__req=jsonp_3&__rev=" + REV_2 + "&__adt=3";
                        br2.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                        br2.getPage(theaterView);
                        // NOTE: lid isn't always within the url, thus is now bad way to determine dllink! -raztoki20160119
                        final String filter = br2.getRegex("\"image\":\\{\"" + lid + "\".*?\\}{3}").getMatch(-1);
                        if (filter == null) {
                            logger.warning("Filter could not be found.");
                        } else {
                            dllink = new Regex(filter, "\"url\":\"(http[^<>\"]*?_o" + regexFileExtension + "[^\"]*)\"").getMatch(0);
                            if (dllink == null) {
                                dllink = new Regex(filter, "\"url\":\"(http[^<>\"]*?_n" + regexFileExtension + "[^\"]*)\"").getMatch(0);
                            }
                            dllink = dllink.replace("\\", "");
                            checkDllink(dllink);
                            if (dllink == null) {
                                // dllink = new Regex(filter, "\"smallurl\":\"(.*?)\"").getMatch(0);
                                dllink = PluginJSonUtils.getJsonValue(filter, "smallurl");
                            }
                        }
                    } catch (final Throwable e) {
                    }
                }
                if (setID == null && user != null && ajaxpipe_token != null && dllink == null) {
                    // another way -raztoki20160122
                    try {
                        logger.info("Trying to get original quality image");
                        final String data = "{\"type\":\"1\",\"fbid\":\"" + fbid + "\",\"firstLoad\":true,\"ssid\":" + System.currentTimeMillis() + ",\"av\":\"" + user + "\"}";
                        final Browser br2 = br.cloneBrowser();
                        final String theaterView = "https://www.facebook.com/ajax/pagelet/generic.php/PhotoViewerInitPagelet?__pc=EXP1%3ADEFAULT&ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=7AmajEzUGBym5Q9UrEwlg9pEbEKAdy9VQC-C26m6oKezob4q68K5UcU-2CEau48vEwy3eEjzUyVWxeUlwzx2bwYDDBBwDK4VqCgS2O7E&__req=jsonp_2&__rev=" + REV_3 + "&__adt=2";
                        br2.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                        br2.getPage(theaterView);
                        // NOTE: lid isn't always within the url, thus is now bad way to determine dllink! -raztoki20160119
                        final String filter = br2.getRegex("\"image\":\\{\"" + lid + "\".*?\\}{3}").getMatch(-1);
                        if (filter == null) {
                            logger.warning("Filter could not be found.");
                        } else {
                            dllink = new Regex(filter, "\"url\":\"(http[^\"]+_o" + regexFileExtension + "[^\"]*)\"").getMatch(0);
                            if (dllink == null) {
                                dllink = new Regex(filter, "\"url\":\"(http[^\"]+_n" + regexFileExtension + "[^\"]*)\"").getMatch(0);
                            }
                        }
                    } catch (final Throwable e) {
                    }
                }
                // not sure what this is used for... -raztoki
                if (dllink == null) {
                    dllink = br.getRegex("id=\"fbPhotoImage\" src=\"(https?://[^<>\"]*?)\"").getMatch(0);
                }
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = dllink.replace("\\", "");
                dllink = Encoding.htmlDecode(dllink);
                // Try to change it to HD
                final Regex urlSplit = new Regex(dllink, "(https?://[a-z0-9\\-\\.]+/hphotos\\-ak\\-[a-z0-9]+)/(q\\d+/)?s\\d+x\\d+(/.+)");
                final String partOne = urlSplit.getMatch(0);
                final String partTwo = urlSplit.getMatch(2);
                if (partOne != null && partTwo != null) {
                    // Usual part
                    dllink = partOne + partTwo;
                }
                try {
                    con = br.openGetConnection(dllink);
                    if (!con.getContentType().contains("html") && !con.getContentType().contains("text")) {
                        link.setDownloadSize(con.getLongContentLength());
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (this.getPluginConfig().getBooleanProperty(USE_ALBUM_NAME_IN_FILENAME, false)) {
                        filename = filename + "_" + lid + getFileNameExtensionFromString(dllink);
                    } else {
                        filename = Encoding.htmlDecode(getFileNameFromHeader(con)).trim();
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            } else { // Video link is handled by handleVideo
                filename = filename + "_" + lid + ".mp4";
            }
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    private String checkDllink(final String flink) throws Exception {
        URLConnectionAdapter con = null;
        final Browser br3 = br.cloneBrowser();
        br3.setFollowRedirects(true);
        try {
            con = br3.openHeadConnection(flink);
            if (!con.getContentType().contains("text")) {
                dllink = flink;
            } else {
                dllink = null;
            }
        } catch (final Exception e) {
        } finally {
            try {
                con.disconnect();
            } catch (final Exception e) {
            }
        }
        return dllink;
    }

    /**
     * Checks for offline html in all kinds of supported urls. Keep in mind that these "offline" message can also mean that access is
     * restricted only for certain users. Basically user would at least need an account and even when he has one but not the rights to view
     * the content we get the same message. in over 90% of all cases, content will be offline so we should simply treat it as offline.
     */
    public static boolean isOffline(final Browser br) {
        /* TODO: Add support for more languages here */
        /* Example: https://www.facebook.com/photo.php?fbid=624011957634791 */
        return br.containsHTML(">The link you followed may have expired|>Leider ist dieser Inhalt derzeit nicht");
    }

    @SuppressWarnings("deprecation")
    private String getPICID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, br);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setStatus("Valid Facebook account is active");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.facebook.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (dllink != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxChunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            if (accountNeeded && !this.loggedIN) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                handleVideo(downloadLink);
            }
        }
    }

    private String getHigh() {
        final String result = PluginJSonUtils.getJsonValue(br, "hd_src");
        return result;
    }

    private String getLow() {
        final String result = PluginJSonUtils.getJsonValue(br, "sd_src");
        return result;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            handleVideo(downloadLink);
        }
    }

    public void handleVideo(final DownloadLink downloadLink) throws Exception {
        boolean preferHD = getPluginConfig().getBooleanProperty(PREFERHD, true);
        if (preferHD) {
            dllink = getHigh();
            if (dllink == null || "null".equals(dllink)) {
                dllink = getLow();
            }
        } else {
            dllink = getLow();
            if (dllink == null || "null".equals(dllink)) {
                dllink = getHigh();
            }
        }
        if (dllink == null || "null".equals(dllink)) {
            logger.warning("Final downloadlink \"dllink\" is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        final String Vollkornkeks = downloadLink.getDownloadURL().replace(FACEBOOKMAINPAGE, "");
        br.setCookie(FACEBOOKMAINPAGE, "x-referer", Encoding.urlEncode(FACEBOOKMAINPAGE + Vollkornkeks + "#" + Vollkornkeks));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String LOGINFAIL_GERMAN  = "\r\nEvtl. ungültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!\r\nBedenke, dass die Facebook Anmeldung per JD nur funktioniert, wenn Facebook\r\nkeine zusätzlichen Sicherheitsabfragen beim Login deines Accounts verlangt.\r\nPrüfe das und versuchs erneut!";
    private static final String LOGINFAIL_ENGLISH = "\r\nMaybe invalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!\r\nNote that the Facebook login via JD will only work if there are no additional\r\nsecurity questions when logging in your account.\r\nCheck that and try again!";

    private void setHeaders(Browser br) {
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.setCookie("http://www.facebook.com", "locale", "en_GB");
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, Browser br) throws Exception {
        synchronized (LOCK) {
            try {
                setHeaders(br);
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(FACEBOOKMAINPAGE, cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    final boolean follow = br.isFollowingRedirects();
                    try {
                        br.setFollowRedirects(true);
                        br.getPage(FACEBOOKMAINPAGE);
                    } finally {
                        br.setFollowRedirects(follow);
                    }
                    if (br.containsHTML("id=\"logoutMenu\"")) {
                        /* Save cookies to save new valid cookie timestamp */
                        account.saveCookies(br.getCookies(FACEBOOKMAINPAGE), "");
                        return;
                    }
                    /* Get rid of old cookies / headers */
                    br = new Browser();
                    br.setCookiesExclusive(true);
                    setHeaders(br);
                }
                br.setFollowRedirects(true);
                final boolean prefer_mobile_login = false;
                // better use the website login. else the error handling below might be broken.
                if (prefer_mobile_login) {
                    /* Mobile login = no crypto crap */
                    br.getPage("https://m.facebook.com/");
                    final Form loginForm = br.getForm(0);
                    if (loginForm == null) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    loginForm.remove(null);
                    loginForm.put("email", Encoding.urlEncode(account.getUser()));
                    loginForm.put("pass", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginForm);
                    br.getPage("https://www.facebook.com/");
                } else {
                    br.getPage("https://www.facebook.com/login.php");
                    final String lang = System.getProperty("user.language");
                    final Form loginForm = br.getForm(0);
                    if (loginForm == null) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    loginForm.remove("persistent");
                    loginForm.put("persistent", "1");
                    loginForm.remove(null);
                    loginForm.remove("login");
                    loginForm.remove("trynum");
                    loginForm.remove("profile_selector_ids");
                    loginForm.remove("legacy_return");
                    loginForm.remove("enable_profile_selector");
                    loginForm.remove("display");
                    String _js_datr = br.getRegex("\"_js_datr\"\\s*,\\s*\"([^\"]+)").getMatch(0);
                    br.setCookie("https://facebook.com", "_js_datr", _js_datr);
                    br.setCookie("https://facebook.com", "_js_reg_fb_ref", Encoding.urlEncode("https://www.facebook.com/login.php"));
                    br.setCookie("https://facebook.com", "_js_reg_fb_gate", Encoding.urlEncode("https://www.facebook.com/login.php"));
                    loginForm.put("email", Encoding.urlEncode(account.getUser()));
                    loginForm.put("pass", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginForm);
                }
                /**
                 * Facebook thinks we're an unknown device, now we prove we're not ;)
                 */
                if (br.containsHTML(">Your account is temporarily locked")) {
                    final String nh = br.getRegex("name=\"nh\" value=\"([a-z0-9]+)\"").getMatch(0);
                    final String dstc = br.getRegex("name=\"fb_dtsg\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (nh == null || dstc == null) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_GERMAN, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_ENGLISH, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&submit%5BContinue%5D=Continue");
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "facebook.com", "http://facebook.com", true);
                    String achal = br.getRegex("name=\"achal\" value=\"([a-z0-9]+)\"").getMatch(0);
                    final String captchaPersistData = br.getRegex("name=\"captcha_persist_data\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (captchaPersistData == null || achal == null) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_GERMAN, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_ENGLISH, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    // Normal captcha handling
                    for (int i = 1; i <= 3; i++) {
                        String captchaLink = br.getRegex("\"(https?://(www\\.)?facebook\\.com/captcha/tfbimage\\.php\\?captcha_challenge_code=[^<>\"]*?)\"").getMatch(0);
                        if (captchaLink == null) {
                            break;
                        }
                        captchaLink = Encoding.htmlDecode(captchaLink);
                        String code;
                        try {
                            code = getCaptchaCode(captchaLink, dummyLink);
                        } catch (final Exception e) {
                            continue;
                        }
                        br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&captcha_response=" + Encoding.urlEncode(code) + "&achal=" + achal + "&submit%5BSubmit%5D=Submit");
                    }
                    // reCaptcha handling
                    for (int i = 1; i <= 3; i++) {
                        final String rcID = br.getRegex("\"recaptchaPublicKey\":\"([^<>\"]*?)\"").getMatch(0);
                        if (rcID == null) {
                            break;
                        }
                        final String extraChallengeParams = br.getRegex("name=\"extra_challenge_params\" value=\"([^<>\"]*?)\"").getMatch(0);
                        final String captchaSession = br.getRegex("name=\"captcha_session\" value=\"([^<>\"]*?)\"").getMatch(0);
                        if (extraChallengeParams == null || captchaSession == null) {
                            break;
                        }
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.setId(rcID);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c;
                        try {
                            c = getCaptchaCode("recaptcha", cf, dummyLink);
                        } catch (final Exception e) {
                            continue;
                        }
                        br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&captcha_session=" + Encoding.urlEncode(captchaSession) + "&extra_challenge_params=" + Encoding.urlEncode(extraChallengeParams) + "&recaptcha_type=password&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&captcha_response=" + Encoding.urlEncode(c) + "&achal=1&submit%5BSubmit%5D=Submit");
                    }
                    for (int i = 1; i <= 3; i++) {
                        if (br.containsHTML(">To confirm your identity, please enter your birthday")) {
                            achal = br.getRegex("name=\"achal\" value=\"([a-z0-9]+)\"").getMatch(0);
                            if (achal == null) {
                                break;
                            }
                            String birthdayVerificationAnswer;
                            try {
                                birthdayVerificationAnswer = getUserInput("Enter your birthday (dd:MM:yyyy)", dummyLink);
                            } catch (final Exception e) {
                                continue;
                            }
                            final String[] bdSplit = birthdayVerificationAnswer.split(":");
                            if (bdSplit == null || bdSplit.length != 3) {
                                continue;
                            }
                            int bdDay = 0, bdMonth = 0, bdYear = 0;
                            try {
                                bdDay = Integer.parseInt(bdSplit[0]);
                                bdMonth = Integer.parseInt(bdSplit[1]);
                                bdYear = Integer.parseInt(bdSplit[2]);
                            } catch (final Exception e) {
                                continue;
                            }
                            br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&birthday_captcha_month=" + bdMonth + "&birthday_captcha_day=" + bdDay + "&birthday_captcha_year=" + bdYear + "&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&achal=" + achal + "&submit%5BSubmit%5D=Submit");
                        } else {
                            break;
                        }
                    }
                    if (br.containsHTML("/captcha/friend_name_image\\.php\\?")) {
                        // unsupported captcha challange.
                        logger.warning("Unsupported captcha challenge.");
                    }
                } else if (br.containsHTML("/checkpoint/")) {
                    br.getPage("https://www.facebook.com/checkpoint/");
                    final String postFormID = br.getRegex("name=\"post_form_id\" value=\"(.*?)\"").getMatch(0);
                    final String nh = br.getRegex("name=\"nh\" value=\"(.*?)\"").getMatch(0);
                    if (nh == null) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_GERMAN, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_ENGLISH, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&submit%5BContinue%5D=Weiter&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&submit%5BThis+is+Okay%5D=Das+ist+OK&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&machine_name=&submit%5BDon%27t+Save%5D=Nicht+speichern&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&machine_name=&submit%5BDon%27t+Save%5D=Nicht+speichern&nh=" + nh);
                }
                if (br.getCookie(FACEBOOKMAINPAGE, "c_user") == null || br.getCookie(FACEBOOKMAINPAGE, "xs") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_GERMAN, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_ENGLISH, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save cookies */
                account.saveCookies(br.getCookies(FACEBOOKMAINPAGE), "");
                account.setValid(true);
                synchronized (LOCK) {
                    checkFeatureDialog();
                }
            } catch (PluginException e) {
                if (e.getLinkStatus() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                    account.removeProperty("");
                }
                throw e;
            }
        }
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "JDownloader's Facebook Plugin helps downloading videoclips and photo galleries. Facebook provides two different video qualities.";
    }

    public static final String  FASTLINKCHECK_PICTURES         = "FASTLINKCHECK_PICTURES";
    public static final boolean FASTLINKCHECK_PICTURES_DEFAULT = true;
    private static final String USE_ALBUM_NAME_IN_FILENAME     = "USE_ALBUM_NAME_IN_FILENAME";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFERHD, JDL.L("plugins.hoster.facebookcomvideos.preferhd", "Videos: Prefer HD quality")).setDefaultValue(true));
        // fast add all the time! Due to volume of decrypting and distributed results, it becomes multithreaded and hundreds of not
        // thousands of results can return...
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK_PICTURES,
        // JDL.L("plugins.hoster.facebookcomvideos.fastlinkcheckpictures", "Photos: Enable fast linkcheck (filesize won't be shown in
        // linkgrabber)?")).setDefaultValue(FASTLINKCHECK_PICTURES_DEFAULT));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_ALBUM_NAME_IN_FILENAME, JDL.L("plugins.hoster.facebookcomvideos.usealbumnameinfilename", "Photos: Use album name in filename [note that filenames change once the download starts]?")).setDefaultValue(true));
    }

    private static String getUser(final Browser br) {
        return jd.plugins.decrypter.FaceBookComGallery.getUser(br);
    }

    private String getajaxpipeToken() {
        return PluginJSonUtils.getJsonValue(br, "ajaxpipe_token");
    }

    private void checkFeatureDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog_Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog_Shown2") == null) {
                    showFeatureDialogAll();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog_Shown", Boolean.TRUE);
                config.setProperty("featuredialog_Shown2", "shown");
                config.save();
            }
        }
    }

    private static void showFeatureDialogAll() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Facebook.com Plugin";
                        final String lang = System.getProperty("user.language");
                        if ("de".equalsIgnoreCase(lang)) {
                            message += "Du benutzt deinen Facebook Account zum ersten mal in JDownloader.\r\n";
                            message += "Da JDownloader keine Facebook App ist loggt er sich genau wie du per Browser ein.\r\n";
                            message += "Es gibt also keinen Austausch (privater) Facebook Daten mit JD.\r\n";
                            message += "Wir wahren deine Privatsphäre!";
                        } else {
                            message += "You're using your Facebook account in JDownloader for the first time.\r\n";
                            message += "Because JDownloader is not a Facebook App it logs in Facebook just like you via browser.\r\n";
                            message += "There is no (private) data exchange between JD and Facebook!\r\n";
                            message += "We respect your privacy!";
                        }
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}