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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sendspace.com" }, urls = { "http://(www\\.)?(beta\\.)?sendspace\\.com/(file|pro/dl)/[0-9a-zA-Z]+" }, flags = { 2 })
public class SendspaceCom extends PluginForHost {

    public SendspaceCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.sendspace.com/joinpro_pay.html");
        setStartIntervall(5000l);
    }

    private final String  JDOWNLOADERAPIKEY = "T1U5ODVNT1FDTQ==";
    // private static final String JDUSERNAME = "cHNwem9ja2Vyc2NlbmVqZA==";
    private String        CURRENTERRORCODE;
    private String        SESSIONTOKEN;
    private String        SESSIONKEY;
    private static Object LOCK              = new Object();
    private String        COOKIE_HOST       = "http://sendspace.com";

    // TODO: Add handling for password protected files for handle premium,
    // actually it only works for handle free
    /**
     * For premium we use their API: http://www.sendspace.com/dev_method.html
     */
    /**
     * Usage: create a token, log in and then use the functions via method "apiRequest(String url, String data)
     * */
    @Override
    public String getAGBLink() {
        return "http://www.sendspace.com/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("beta.sendspace.com/", "sendspace.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser();
        // createSessToken();
        // final Account aa =
        // AccountController.getInstance().getValidAccount(this);
        // if (aa != null) {
        // apiLogin(aa.getUser(), JDHash.getMD5(SESSIONTOKEN +
        // JDHash.getMD5(aa.getPass()).toLowerCase()));
        // } else {
        // apiLogin(Encoding.Base64Decode(JDUSERNAME),
        // JDHash.getMD5(SESSIONTOKEN +
        // JDHash.getMD5("JD Account Password").toLowerCase()));
        // }
        // apiRequest("http://api.sendspace.com/rest/?method=files.getinfo",
        // "&session_key=" + SESSIONKEY + "&file_id=" +
        // getFid(downloadLink));
        // if (br.containsHTML("<error code=\"9\"")) throw new
        // PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // final String filename =
        // br.getRegex("name=\"([^<>\"]*?)\"").getMatch(0);
        // final String filesize =
        // br.getRegex("file_size=\"(\\d+)\"").getMatch(0);
        // if (filename != null && filename != null) {
        // downloadLink.setFinalFileName(filename);
        // downloadLink.setDownloadSize(Long.parseLong(filesize));
        // return AvailableStatus.TRUE;
        // } else {
        // return handleOldAvailableStatus(downloadLink);
        // }
        return handleOldAvailableStatus(downloadLink);

    }

    private AvailableStatus handleOldAvailableStatus(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        String url = downloadLink.getDownloadURL();
        if (!url.contains("/pro/dl/")) {
            br.getPage(url);
            if (br.containsHTML("The page you are looking for is  not available\\. It has either been moved") && url.contains("X")) {
                url = url.replaceAll("X", "x");
                downloadLink.setUrlDownload(url);
                return requestFileInformation(downloadLink);
            }
            if (!br.containsHTML("the file you requested is not available")) {
                String[] infos = br.getRegex("<b>Name:</b>(.*?)<br><b>Size:</b>(.*?)<br>").getRow(0);/* old */
                if (infos == null) infos = br.getRegex("Download: <strong>(.*?)<.*?strong> \\((.*?)\\)<").getRow(0);/* new1 */
                if (infos == null) infos = br.getRegex("Download <b>(.*?)<.*?File Size: (.*?)<").getRow(0);/* new2 */
                if (infos != null) {
                    /* old format */
                    downloadLink.setName(Encoding.htmlDecode(infos[0]).trim());
                    downloadLink.setDownloadSize(SizeFormatter.getSize(infos[1].trim().replaceAll(",", "\\.")));
                    return AvailableStatus.TRUE;
                } else {
                    String filename = br.getRegex("<title>Download ([^<>/\"]*?) from Sendspace\\.com \\- send big files the easy way</title>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<h2 class=\"bgray\"><b>(.*?)</b></h2>").getMatch(0);
                        if (filename == null) filename = br.getRegex("title=\"download (.*?)\">Click here to start").getMatch(0);
                    }
                    String filesize = br.getRegex("<b>File Size:</b> (.*?)</div>").getMatch(0);
                    if (filename != null) {
                        downloadLink.setName(Encoding.htmlDecode(filename).trim());
                        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim().replaceAll(",", ".")));
                        return AvailableStatus.TRUE;
                    }
                }
                if (br.containsHTML("No htmlCode read")) {
                    // No html content??? maybe server problem
                    // seems like a firewall block.
                    Thread.sleep(90000);
                    return requestFileInformation(downloadLink);
                }
                //
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(url);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                downloadLink.setName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                downloadLink.setDownloadSize(con.getLongContentLength());
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
    }

    private void handleErrors(boolean plugindefect) throws PluginException {
        String error = br.getRegex("<div class=\"errorbox-bad\".*?>(.*?)</div>").getMatch(0);
        if (error == null) error = br.getRegex("<div class=\"errorbox-bad\".*?>.*?>(.*?)</>").getMatch(0);
        if (error == null && !plugindefect) return;
        if (error == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sendspacecom.errors.servererror", "Unknown server error"), 5 * 60 * 1000l);
        logger.severe("Error: " + error);
        if (error.contains("You cannot download more than one file at a time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "A download is still in progress", 10 * 60 * 1000l);
        if (error.contains("You may now download the file")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 30 * 1000l); }
        if (error.contains("full capacity")) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sendspacecom.errors.serverfull", "Free service capacity full"), 5 * 60 * 1000l); }
        if (error.contains("this connection has reached the")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000);
        if (error.contains("reached daily download") || error.contains("reached your daily download")) {
            int wait = 60;
            String untilh = br.getRegex("again in (\\d+)h:(\\d+)m").getMatch(0);
            String untilm = br.getRegex("again in (\\d+)h:(\\d+)m").getMatch(1);
            if (untilh != null) wait = Integer.parseInt(untilh) * 60;
            if (untilm != null && untilh != null) wait = wait + Integer.parseInt(untilm);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached your daily download limit", wait * 60 * 1000l);
        }
        if (br.containsHTML("(>The file is not currently available|Our support staff have been notified and we hope to resolve the problem shortly)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sendspacecom.errors.temporaryunavailable", "This file is not available at the moment!"));
        if (plugindefect) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        if (!downloadLink.getDownloadURL().contains("/pro/dl/")) {
            // Re-use old directlinks to avoid captchas, especially good after
            // reconnects
            String linkurl = checkDirectLink(downloadLink, "savedlink");
            if (linkurl == null) {
                br.getPage(downloadLink.getDownloadURL());
                if (br.containsHTML("You have reached your daily download limit")) {
                    int minutes = 0, hours = 0;
                    String tmphrs = br.getRegex("again in.*?(\\d+)h:.*?m or").getMatch(0);
                    if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                    String tmpmin = br.getRegex("again in.*?h:(\\d+)m or").getMatch(0);
                    if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                    int waittime = ((3600 * hours) + (60 * minutes) + 1) * 1000;
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
                }
                // Password protected links handling
                String passCode = null;
                if (br.containsHTML("name=\"filepassword\"")) {
                    logger.info("This link seems to be püassword protected...");
                    for (int i = 0; i < 2; i++) {
                        Form pwform = br.getFormbyKey("filepassword");
                        if (pwform == null) pwform = br.getForm(0);
                        if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        if (downloadLink.getStringProperty("pass", null) == null) {
                            passCode = Plugin.getUserInput("Password?", downloadLink);
                        } else {
                            /* gespeicherten PassCode holen */
                            passCode = downloadLink.getStringProperty("pass", null);
                        }
                        pwform.put("filepassword", passCode);
                        br.submitForm(pwform);
                        if (br.containsHTML("(name=\"filepassword\"|Incorrect Password)")) {
                            continue;
                        }
                        break;
                    }
                    if (br.containsHTML("(name=\"filepassword\"|Incorrect Password)")) throw new PluginException(LinkStatus.ERROR_FATAL, "Wrong Password");
                }
                /* handle captcha */
                if (br.containsHTML(">Please prove that you are a human being")) {
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.parse();
                    rc.load();
                    String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                    rc.setId(id);
                    for (int i = 0; i <= 5; i++) {
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, downloadLink);
                        rc.setCode(c);
                        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                            rc.reload();
                            continue;
                        }
                        break;
                    }
                    if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                handleErrors(false);
                /* Link holen */
                linkurl = br.getRegex("<a id=\"download_button\" href=\"(http://.*?)\"").getMatch(0);
                if (linkurl == null) linkurl = br.getRegex("\"(http://fs\\d+n\\d+\\.sendspace\\.com/dl/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
                if (linkurl == null) {
                    if (br.containsHTML("has reached the 300MB hourly download")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
                    logger.warning("linkurl equals null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (passCode != null) {
                    downloadLink.setProperty("pass", passCode);
                }
            }
            /* Datei herunterladen */
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 1);
            URLConnectionAdapter con = dl.getConnection();
            if (con.getURL().toExternalForm().contains("?e=") || con.getContentType().contains("html")) {
                br.followConnection();
                handleErrors(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (con.getResponseCode() == 416) {
                // HTTP/1.1 416 Requested Range Not Satisfiable
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
            }
            if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
            downloadLink.setProperty("savedlink", linkurl);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 0);
            URLConnectionAdapter con = dl.getConnection();
            if (con.getContentType().contains("html")) {
                br.followConnection();
                handleErrors(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        try {
            apiRequest("http://api.sendspace.com/rest/?method=download.getinfo", "&session_key=" + SESSIONKEY + "&file_id=" + Encoding.urlEncode(link.getDownloadURL()));
        } catch (final Exception e) {
            logger.info("Unexpected error while trying to download, maybe old sessionkey, logging in again...");
            login(account, true);
            apiRequest("http://api.sendspace.com/rest/?method=download.getinfo", "&session_key=" + SESSIONKEY + "&file_id=" + Encoding.urlEncode(link.getDownloadURL()));
        }
        String linkurl = br.getRegex("url=\"(http[^<>\"]*?)\"").getMatch(0);
        if (linkurl == null) {
            logger.warning("Final downloadlink couldn't be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, linkurl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("Received html code, stopping...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public void init() {
        br.setRequestIntervalLimit(getHost(), 750);
    }

    public void login(final Account account, final boolean force) throws IOException, PluginException {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        SESSIONTOKEN = account.getStringProperty("sessiontoken", null);
                        SESSIONKEY = account.getStringProperty("sessionkey", null);
                        return;
                    }
                }
                createSessToken();
                apiLogin(account.getUser(), account.getPass());
                if ("Lite".equals(get("membership_type"))) {
                    logger.info("This is a free account, JDownloader doesn't support sendspace.com free accounts!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                account.setProperty("sessiontoken", SESSIONTOKEN);
                account.setProperty("sessionkey", SESSIONKEY);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                account.setProperty("sessiontoken", Property.NULL);
                account.setProperty("sessionkey", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        final String left = get("bandwidth_left");
        if (left != null) {
            ai.setTrafficLeft(Long.parseLong(left));
        } else {
            apiFailure("bandwidth_left");
            account.setValid(false);
            return ai;
        }
        final String expires = get("membership_ends");
        if (expires != null) {
            ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(expires));
        } else {
            apiFailure("membership_ends");
            account.setValid(false);
            return ai;
        }
        String spaceUsed = get("diskspace_used");
        if (spaceUsed != null) {
            if (spaceUsed.equals("")) spaceUsed = "0";
            ai.setUsedSpace(Long.parseLong(spaceUsed));
        }
        ai.setStatus("Account type: " + get("membership_type"));
        account.setValid(true);
        return ai;
    }

    private void prepBrowser() {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
    }

    private String get(final String parameter) {
        return br.getRegex("<" + parameter + ">([^<>\"]*?)</" + parameter + ">").getMatch(0);
    }

    private String getErrorcode() {
        return br.getRegex("<error code=\"(\\d+)\"").getMatch(0);
    }

    private void apiFailure(final String parameter) {
        logger.warning("API failure: " + parameter + " is null");
    }

    private void createSessToken() throws IOException, PluginException {
        apiRequest("http://api.sendspace.com/rest/", "?method=auth.createtoken&api_key=" + Encoding.Base64Decode(JDOWNLOADERAPIKEY) + "&api_version=1.0&response_format=xml&app_version=0.1");
        SESSIONTOKEN = get("token");
        if (SESSIONTOKEN == null) {
            logger.warning("sessiontoken could not be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private boolean sessionOk() {
        boolean failed = false;
        try {
            apiRequest("http://api.sendspace.com/rest/", "?method=auth.checksession&session_key=" + SESSIONKEY);
        } catch (final Exception e) {
            failed = true;
        }
        if (!"ok".equals(get("session")) || failed)
            return false;
        else
            return true;
    }

    private void apiRequest(final String parameter, final String data) throws IOException, PluginException {
        br.getPage(parameter + data);
        handleAPIErrors();
    }

    private void apiLogin(final String username, final String password) throws IOException, PluginException {
        apiRequest("http://api.sendspace.com/rest/", "?method=auth.login&token=" + SESSIONTOKEN + "&user_name=" + username + "&tokened_password=" + JDHash.getMD5(SESSIONTOKEN + JDHash.getMD5(password).toLowerCase()));
        SESSIONKEY = get("session_key");
        if (SESSIONKEY == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleAPIErrors() throws PluginException {
        CURRENTERRORCODE = getErrorcode();
        if (CURRENTERRORCODE != null) {
            final int error = Integer.parseInt(CURRENTERRORCODE);
            switch (error) {
            case 5:
                logger.warning("API_ERROR_BAD_API_VERSION");
            case 6:
                logger.warning("API_ERROR_SESSION_BAD");
            case 7:
                logger.warning("Session not authenticated");
            case 8:
                logger.warning("API_ERROR_AUTHENTICATION_FAILURE");
            case 9:
                logger.info("API_ERROR_FILE_NOT_FOUND");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 11:
                logger.warning("API_ERROR_PERMISSION_DENIED");
            case 12:
                logger.warning("API_ERROR_DOWNLOAD_TEMP_ERROR");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            case 18:
                logger.warning("Unknown API key");
            case 19:
                logger.warning("API_ERROR_PRO_EXPIRED");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 22:
                logger.info("API_ERROR_BAD_PASSWORD");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Link is password protected");
            case 23:
                logger.info("API_ERROR_BANDWIDTH_LIMIT");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 25:
                logger.warning("API_ERROR_OUTDATED_VERSION");
            case 26:
                logger.warning("API_ERROR_INVALID_FILE_URL");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 30:
                logger.warning("API ERROR 30: Too many sessions open for account (re-login required, temp. disabling account)");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            default:
                logger.warning("Unknown API errorcode: " + CURRENTERRORCODE);
                logger.warning("HTML code: " + br.toString());
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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
        return false;
    }
}