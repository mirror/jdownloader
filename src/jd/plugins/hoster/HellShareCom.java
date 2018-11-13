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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.UserIO;
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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hellshare.cz" }, urls = { "https?://(download\\.|www\\.)?(sk|cz|en)?hellshare\\.(com|sk|hu|de|cz|pl)/[a-z0-9\\-/]+/\\d+" })
public class HellShareCom extends PluginForHost {
    /*
     * Sister sites: hellshare.cz, (and their other domains), hellspy.cz (and their other domains), using same dataservers but slightly
     * different script
     */
    /* Czech VPN required!! */
    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -5;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 2;
    private static final String  LIMITREACHED                 = "(You have exceeded today´s free download limit|You exceeded your today\\'s limit for free download|<strong>Dnešní limit free downloadů jsi vyčerpal\\.</strong>)";
    // edt: added 2 constants for testing, in normal work - waittime when server
    // is 100% load should be 2-5 minutes
    private static final String  COOKIE_HOST                  = "http://hellshare.com";
    private final long           WAITTIME100PERCENT           = 60 * 1000l;
    private final long           WAITTIMEDAILYLIMIT           = 4 * 3600 * 1000l;
    private static final boolean ALL_PREMIUMONLY              = true;
    private static Object        LOCK                         = new Object();

    public HellShareCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.en.hellshare.com/register");
        /* Especially for premium - don't make too many requests in a short time or we'll get 503 responses. */
        this.setStartIntervall(2000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.en.hellshare.com/terms";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        final String numbers = new Regex(link.getPluginPatternMatcher(), "hellshare\\.com/(\\d+)").getMatch(0);
        if (numbers == null) {
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceAll("https?.*?//.*?/", "https://www.hellshare.cz/"));
        }
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "hellshare.com".equals(host)) {
            return "hellshare.cz";
        }
        return super.rewriteHost(host);
    }

    private Browser prepBrowser(final Browser prepBr) {
        prepBr.setCustomCharset("utf-8");
        prepBr.getHeaders().put("Accept-Language", "en-gb;q=0.9, en;q=0.8");
        prepBr.addAllowedResponseCodes(502);
        return prepBr;
    }

    /** TODO: Improve overall errorhandling. */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        prepBrowser(br);
        /* To prefer english page UPDATE: English does not work anymore */
        changeToEnglish();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 502) {
            link.getLinkStatus().setStatusText("We are sorry, but HellShare is unavailable in your country");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML(">File not found|>File was deleted|>The file is private and can only be downloaded owner") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Folderlinks are unsupported! */
        if (br.containsHTML("id=\"snippet\\-\\-FolderList\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filesize = br.getRegex("FileSize_master\">(.*?)</strong>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("\"The content.*?with a size of (.*?) has been uploaded").getMatch(0);
        }
        String filename = br.getRegex("\"FileName_master\">(.*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"The content (.*?) with a size").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) – Download").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("hidden\">Downloading file (.*?)</h1>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("keywords\" content=\"HellShare, (.*?)\"").getMatch(0);
                    }
                }
            }
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace("&nbsp;", "")));
        link.setUrlDownload(br.getURL());
        return AvailableStatus.TRUE;
    }

    private String getDownloadOverview(final String fileID) {
        String freePage = br.getRegex("\"(/[^/\"\\'<>]+/[^/\"\\'<>]+/" + fileID + "/\\?do=relatedFileDownloadButton\\-" + fileID + ".*?)\"").getMatch(0);
        if (freePage == null) {
            freePage = br.getRegex("\"(/([^/\"\\'<>]+/)?[^/\"\\'<>]+/" + fileID + "/\\?do=relatedFileDownloadButton\\-" + fileID + ".*?)\"").getMatch(0);
        }
        if (freePage == null) {
            freePage = br.getRegex("\"(/[^/\"\\'<>]+/" + fileID + "/\\?do=fileDownloadButton\\-showDownloadWindow)\"").getMatch(0);
            if (freePage == null) {
                freePage = br.getRegex("\\s+<a href=\"([^\\s]+)\" class=\"ajax button button-devil\"").getMatch(0);
            }
        }
        if (freePage != null) {
            if (!freePage.startsWith("http")) {
                freePage = "http://download.hellshare.com" + freePage;
            }
        }
        return freePage;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.getHttpConnection().getResponseCode() == 502) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "We are sorry, but HellShare is unavailable in your country", 4 * 60 * 60 * 1000l);
        }
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        if (ALL_PREMIUMONLY) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (br.containsHTML(LIMITREACHED)) {
            // edt: to support bug when server load = 100% and daily limit
            // reached are simultaneously displayed
            if (br.containsHTML("Current load 100%") || br.containsHTML("Server load: 100%")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.HellShareCom.error.DailyLimitReached", "Daily Limit for free downloads reached"), WAITTIMEDAILYLIMIT);
            }
        }
        br.setFollowRedirects(false);
        final String changetocz = br.getRegex("lang=\"cz\" xml:lang=\"cz\" href=\"(http://download\\.cz\\.hellshare\\.com/.*?/\\d+)\"").getMatch(0);
        if (changetocz == null) {
            // Do NOT throw an exeption here as this part isn't that important
            // but it's bad that the plugin breaks just because of this regex
            logger.warning("Language couldn't be changed. This will probably cause trouble...");
        } else {
            br.getPage(changetocz);
            if (br.containsHTML("No htmlCode read")) {
                br.getPage(downloadLink.getDownloadURL());
            }
        }
        br.setDebug(true);
        // edt: new string for server 100% load
        if (br.containsHTML("Current load 100%") || br.containsHTML("Server load: 100%")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.CurrentLoadIs100Percent", "The current serverload is 100%"), WAITTIME100PERCENT);
        }
        // edt: added more logging info
        if (br.containsHTML(LIMITREACHED)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.HellShareCom.error.DailyLimitReached", "Daily Limit for free downloads reached"), WAITTIMEDAILYLIMIT);
        }
        final String fileId = new Regex(downloadLink.getDownloadURL(), "/(\\d+)(/)?$").getMatch(0);
        if (fileId == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean secondWay = true;
        String freePage = getDownloadOverview(fileId);
        if (freePage == null) {
            // edt: seems that URL for download is already final for all the
            // links, so br.getURL doesn't need to be changed, secondWay always
            // works good
            freePage = br.getURL(); // .replace("hellshare.com/serialy/",
            // "hellshare.com/").replace("/pop/",
            // "/").replace("filmy/", "");
            secondWay = false;
        }
        // edt: if we got response then secondWay works for all the links
        // br.getPage(freePage);
        if (!br.containsHTML("No htmlCode read")) {// (br != null) {
            secondWay = true;
        }
        if (br.containsHTML("The server is under the maximum load")) {
            logger.info(JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"));
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"), 10 * 60 * 1000l);
        }
        if (br.containsHTML("You are exceeding the limitations on this download")) {
            logger.info("You are exceeding the limitations on this download");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        }
        // edt: added more logging info
        if (br.containsHTML(LIMITREACHED)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.HellShareCom.error.DailyLimitReached", "Daily Limit for free downloads reached"), WAITTIMEDAILYLIMIT);
        }
        if (br.containsHTML("<h1>File not found</h1>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (secondWay) {
            Form captchaForm = null;
            final Form[] allForms = br.getForms();
            if (allForms != null && allForms.length != 0) {
                for (final Form aForm : allForms) {
                    if (aForm.containsHTML("captcha-img")) {
                        captchaForm = aForm;
                        break;
                    }
                }
            }
            if (captchaForm == null) {
                logger.warning("captchaform equals null!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String captchaLink = captchaForm.getRegex("src=\"(.*?)\"").getMatch(0);
            if (captchaLink == null) {
                captchaLink = br.getRegex("\"(http://(www\\.)?hellshare\\.com/captcha\\?sv=.*?)\"").getMatch(0);
            }
            if (captchaLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            try {
                final String code = getCaptchaCode(Encoding.htmlDecode(captchaLink), downloadLink);
                captchaForm.put("captcha", code);
            } catch (Exception e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "reCaptcha aborted!");
            }
            br.setFollowRedirects(true);
            br.setReadTimeout(120 * 1000);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, FREE_RESUME, FREE_MAXCHUNKS);
        } else {
            Form form = br.getForm(1);
            if (form == null) {
                form = br.getForm(0);
            }
            final String captcha = "http://www.en.hellshare.com/antispam.php?sv=FreeDown:" + fileId;
            final String code = getCaptchaCode(captcha, downloadLink);
            form.put("captcha", Encoding.urlEncode(code));
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, FREE_RESUME, FREE_MAXCHUNKS);
        }
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            if (br.getURL().contains("errno=404")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.HellShareCom.error.404", "404 Server error. File might not be available for your country!"));
            }
            if (br.containsHTML("<h1>File not found</h1>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // edt: new string for server 100% load
            if (br.containsHTML("The server is under the maximum load") || br.containsHTML("Server load: 100%")) {
                logger.info(JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"));
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"), WAITTIME100PERCENT);
            }
            if (br.containsHTML("(Incorrectly copied code from the image|Opište barevný kód z obrázku)") || br.getURL().contains("error=405")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (br.containsHTML("You are exceeding the limitations on this download")) {
                logger.info("You are exceeding the limitations on this download");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            }
            if (this.br.toString().length() < 30) {
                /* E.g. empty page */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // downloadLink.setProperty("free_directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String dllink = null;
        requestFileInformation(downloadLink);
        if (br.getHttpConnection().getResponseCode() == 502) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "We are sorry, but HellShare is unavailable in your country", 4 * 60 * 60 * 1000l);
        }
        login(account, false);
        dllink = checkDirectLink(downloadLink, "account_premium_directlink");
        if (dllink == null) {
            changeToEnglish();
            if (account.getBooleanProperty("free", false)) {
                logger.info("Handling free account download");
                doFree(downloadLink);
                return;
            }
            br.getPage(downloadLink.getDownloadURL());
            br.setFollowRedirects(false);
            final String filedownloadbutton = br.getURL() + "?do=fileDownloadButton-showDownloadWindow";
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(filedownloadbutton);
                if (con.getContentType().contains("html")) {
                    br.followConnection();
                    dllink = br.getRedirectLocation();
                    if (dllink == null) {
                        dllink = PluginJSonUtils.getJsonValue(br, "redirect");
                    }
                    if (dllink == null) {
                        if (br.containsHTML("button-download-full-nocredit")) {
                            logger.info("not enough credits to download");
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        }
                        if (br.containsHTML("Daily limit exceeded")) {
                            logger.info("hellshare: Daily limit exceeded!");
                            UserIO.getInstance().requestMessageDialog(0, "Hellshare.com Premium Error", "Daily limit exceeded!\r\nPremium disabled, will continue downloads as Free User");
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                            // throw new PluginException(LinkStatus.ERROR_PREMIUM, "Daily limit exceeded!");
                        }
                        // Hellshare Premium sharing not allowed!
                        if (br.containsHTML("HellShare not allowed to share the login information to the accounts\\.")) {
                            UserIO.getInstance().requestMessageDialog(0, "Hellshare.com Premium Error", "HellShare not allowed to share the login information to the accounts!");
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        }
                        if (this.br.toString().length() < 30) {
                            /* E.g. empty page */
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
                        }
                        logger.warning("dllink (premium) is null...");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else {
                    dllink = filedownloadbutton;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("account_premium_directlink", dllink);
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            ai.setStatus("Login failed");
            account.setValid(false);
            throw e;
        }
        final String trafficleft = br.getRegex("id=\"info_credit\" class=\"va-middle\">[\n\t\r ]+<strong>(.*?)</strong>").getMatch(0);
        String premiumActive = br.getRegex("<div class=\"icon-timecredit icon\">[\n\t\r]+<h4>Premium account</h4>[\n\t\r]+(.*)[\n\t\r]+<br />[\n\t\r]+<a href=\"/credit/time\">Buy</a>").getMatch(0);
        if (premiumActive == null) {
            // Premium User
            premiumActive = br.getRegex("<div class=\"icon-timecredit icon\">[\n\t\r]+<h4>Premium account</h4>[\n\t\r]+(.*)<br />[\n\t\r]+<a href=\"/credit/time\">Buy</a>").getMatch(0);
        }
        if (premiumActive == null) {
            // User with Credits
            premiumActive = br.getRegex("<div class=\"icon-credit icon\">[\n\t\r]+<h4>(.*)</h4>[\n\t\r]+<table>+[\n\t\r]+<tr>[\n\t\r]+<th>Current:</th>[\n\t\r]+<td>(.*?)</td>[\n\t\r]+</tr>").getMatch(0);
        }
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            ai.setValidUntil(-1);
            ai.setStatus("Account with credits");
            account.setValid(true);
            account.setProperty("free", false);
        } else if (premiumActive == null) {
            ai.setStatus("Invalid/Unknown");
            account.setValid(false);
        } else if (premiumActive.contains("Inactive")) {
            ai.setStatus("Free User");
            // for inactive - set traffic left to 0
            ai.setTrafficLeft(0l);
            account.setValid(true);
            account.setProperty("free", true);
        } else if (premiumActive.contains("Active")) {
            String validUntil = premiumActive.substring(premiumActive.indexOf(":") + 1);
            // page only displays full day, so JD fails in the last day of Premium
            // added time as if the account is Premium until the midnight
            validUntil += " 23:59:59";
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd.MM.yyyy HH:mm:ss", null));
            ai.setStatus("Premium accoount");
            ai.setExpired(false);
            account.setValid(true);
            account.setProperty("free", false);
        }
        return ai;
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
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
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.setDebug(true);
                try {
                    changeToEnglish();
                } catch (final Throwable e) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage("http://www.hellshare.com/?do=login-showLoginWindow");
                br.postPage("/?do=login-loginBoxForm-submit", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&login=P%C5%99ihl%C3%A1sit+registrovan%C3%A9ho+u%C5%BEivatele&perm_login=on");
                /*
                 * this will change account language to eng,needed because language is saved in profile
                 */
                final String permLogin = br.getCookie(br.getURL(), "permlogin");
                if (permLogin == null || br.containsHTML("zadal jsi špatné uživatelské")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage("/members/");
                if (br.containsHTML("Wrong user name or wrong password\\.") || !br.containsHTML("credit for downloads") || br.containsHTML("Špatně zadaný login nebo heslo uživatele") || br.containsHTML("zadal jsi špatné uživatelské")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
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

    /* Changes the language to English. */
    private void changeToEnglish() throws PluginException, IOException {
        br.getPage("http://www.hellshare.cz");
        if (br.getHttpConnection().getResponseCode() == 502) {
            // connection is blocked.
            return;
        }
        final String cookie = br.getCookie(br.getURL(), "PHPSESSID");
        if (cookie == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("http://www.hellshare.com/--" + cookie + "-/");
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
        return false;
    }
}