//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapidgator.net" }, urls = { "https?://(www\\.)?(rapidgator\\.net|rg\\.to)/file/([a-z0-9]{32}(/[^/<>]+\\.html)?|\\d+(/[^/<>]+\\.html)?)" })
public class RapidGatorNet extends antiDDoSForHost {
    public RapidGatorNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://rapidgator.net/article/premium");
        this.setConfigElements();
    }

    private static final String            MAINPAGE                        = "https://rapidgator.net/";
    private static Object                  LOCK                            = new Object();
    private static final String            PREMIUMONLYTEXT                 = "This file can be downloaded by premium only</div>";
    private static final String            PREMIUMONLYUSERTEXT             = JDL.L("plugins.hoster.rapidgatornet.only4premium", "Only downloadable for premium users!");
    private final String                   EXPERIMENTALHANDLING            = "EXPERIMENTALHANDLING";
    private final String                   EXPERIMENTAL_ENFORCE_SSL        = "EXPERIMENTAL_ENFORCE_SSL";
    private final String                   DISABLE_API_PREMIUM             = "DISABLE_API_PREMIUM";
    private final String                   apiURL                          = "https://rapidgator.net/api/";
    private final String[]                 IPCHECK                         = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };
    private static AtomicBoolean           hasAttemptedDownloadstart       = new AtomicBoolean(false);
    private static AtomicLong              timeBefore                      = new AtomicLong(0);
    private static final String            PROPERTY_LASTDOWNLOAD_TIMESTAMP = "rapidgatornet_lastdownload_timestamp";
    private final String                   LASTIP                          = "LASTIP";
    private static AtomicReference<String> lastIP                          = new AtomicReference<String>();
    private final Pattern                  IPREGEX                         = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static final long              FREE_RECONNECTWAIT_GENERAL      = 2 * 60 * 60 * 1000L;
    private static final long              FREE_RECONNECTWAIT_DAILYLIMIT   = 3 * 60 * 60 * 1000L;
    private static final long              FREE_RECONNECTWAIT_OTHERS       = 30 * 60 * 1000L;
    private static final long              FREE_CAPTCHA_EXPIRE_TIME        = 105 * 1000L;

    @Override
    public String getAGBLink() {
        return "https://rapidgator.net/article/terms";
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "rapidgator.net".equals(host) || "rg.to".equals(host)) {
            return "rapidgator.net";
        }
        return super.rewriteHost(host);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        if (link.getDownloadURL().contains("rg.to/")) {
            String url = link.getDownloadURL();
            url = url.replaceFirst("rg.to/", "rapidgator.net/");
            link.setUrlDownload(url);
        }
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
        final String linkID = new Regex(link.getPluginPatternMatcher(), "/file/([a-z0-9]{32}|\\d+)").getMatch(0);
        if (linkID != null) {
            link.setLinkID(getHost() + "://" + linkID);
        }
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (!Account.AccountType.PREMIUM.equals(acc.getType())) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    @Override
    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            // prepBr.setRequestIntervalLimit("http://rapidgator.net/", 319 * (int) Math.round(Math.random() * 3 + Math.random() * 3));
            prepBr.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
            prepBr.getHeaders().put("Accept-Language", "en-US,en;q=0.8");
            prepBr.getHeaders().put("Cache-Control", null);
            prepBr.getHeaders().put("Pragma", null);
            prepBr.setCookie("http://rapidgator.net/", "lang", "en");
            prepBr.setCustomCharset("UTF-8");
            prepBr.setReadTimeout(1 * 60 * 1000);
            prepBr.setConnectTimeout(1 * 60 * 1000);
            // for the api
            prepBr.addAllowedResponseCodes(401, 402, 501, 423);
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.containsHTML("400 Bad Request") && link.getDownloadURL().contains("%")) {
            link.setUrlDownload(link.getDownloadURL().replace("%", ""));
            getPage(link.getDownloadURL());
        }
        if (br.containsHTML("File not found")) {
            final String filenameFromURL = new Regex(link.getDownloadURL(), ".+/(.+)\\.html").getMatch(0);
            if (filenameFromURL != null) {
                link.setName(filenameFromURL);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String freedlsizelimit = br.getRegex("'You can download files up to ([\\d\\.]+ ?(MB|GB)) in free mode<").getMatch(0);
        if (freedlsizelimit != null) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.rapidgatornet.only4premium", "This file is restricted to Premium users only"));
        }
        final String md5 = br.getRegex(">MD5: ([A-Fa-f0-9]{32})</label>").getMatch(0);
        String filename = br.getRegex("Downloading:[\t\n\r ]+</strong>([^<>\"]+)</p>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Download file ([^<>\"]+)</title>").getMatch(0);
        }
        final String filesize = br.getRegex("File size:[\t\n\r ]+<strong>([^<>\"]+)</strong>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename.startsWith(".") && /* effectively unix based filesystems */!CrossSystem.isWindows()) {
            /* Temp workaround for hidden files */
            filename = filename.substring(1);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        br.setFollowRedirects(false);
        // Only show message if user has no active premium account
        if (br.containsHTML(RapidGatorNet.PREMIUMONLYTEXT) && AccountController.getInstance().getValidAccount(this) == null) {
            link.getLinkStatus().setStatusText(RapidGatorNet.PREMIUMONLYUSERTEXT);
        }
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink downloadLink) throws Exception {
        // experimental code - raz
        // so called 15mins between your last download, ends up with your IP blocked for the day..
        // Trail and error until we find the sweet spot.
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        final boolean useExperimentalHandling = this.getPluginConfig().getBooleanProperty(EXPERIMENTALHANDLING, false);
        final String currentIP = getIP();
        if (useExperimentalHandling) {
            logger.info("New Download: currentIP = " + currentIP);
            if (ipChanged(currentIP, downloadLink) == false) {
                long lastdownload_timestamp = timeBefore.get();
                if (lastdownload_timestamp == 0) {
                    lastdownload_timestamp = getPluginSavedLastDownloadTimestamp();
                }
                final long passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload_timestamp;
                logger.info("Wait time between downloads to prevent your IP from been blocked for 1 Day!");
                if (passedTimeSinceLastDl < FREE_RECONNECTWAIT_GENERAL) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait time between download session", FREE_RECONNECTWAIT_GENERAL - passedTimeSinceLastDl);
                }
            }
        }
        if (br.containsHTML(RapidGatorNet.PREMIUMONLYTEXT)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        try {
            // end of experiment
            if (br.containsHTML("You have reached your daily downloads limit\\. Please try")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached your daily downloads limit", FREE_RECONNECTWAIT_DAILYLIMIT);
            } else if (br.containsHTML(">[\\r\n ]+You have reached your hourly downloads limit\\.")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached your hourly downloads limit", FREE_RECONNECTWAIT_GENERAL);
            }
            if (br.containsHTML("(You can`t download not more than 1 file at a time in free mode\\.<|>Wish to remove the restrictions\\?)")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You can't download more than one file within a certain time period in free mode", FREE_RECONNECTWAIT_OTHERS);
            }
            final String freedlsizelimit = br.getRegex("'You can download files up to ([\\d\\.]+ ?(MB|GB)) in free mode<").getMatch(0);
            if (freedlsizelimit != null && !freedlsizelimit.equalsIgnoreCase("2 GB")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            final String reconnectWait = br.getRegex("Delay between downloads must be not less than (\\d+) min\\.<br>Don`t want to wait\\? <a style=\"").getMatch(0);
            if (reconnectWait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(reconnectWait) + 1) * 60 * 1000l);
            }
            final String fid = br.getRegex("var fid = (\\d+);").getMatch(0);
            if (fid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // far as I can tell it's not needed.
            simulateBrowser();
            int wait = 30;
            final String waittime = br.getRegex("var secs = (\\d+);").getMatch(0);
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            getPage(br2, "https://rapidgator.net/download/AjaxStartTimer?fid=" + fid);
            final String sid = br2.getRegex("sid\":\"([a-zA-Z0-9]{32})").getMatch(0);
            String state = br2.getRegex("state\":\"([^\"]+)").getMatch(0);
            if (!"started".equalsIgnoreCase(state)) {
                if (br2.toString().equals("No htmlCode read")) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Unknown server error", 2 * 60 * 1000l);
                }
                logger.info(br2.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (sid == null) {
                logger.info(br2.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            sleep((wait + 5) * 1001l, downloadLink);
            /* needed so we have correct referrer ;) (back to original br) */
            br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage(br2, "https://rapidgator.net/download/AjaxGetDownloadLink?sid=" + sid);
            state = br2.getRegex("state\":\"(.*?)\"").getMatch(0);
            if (!"done".equalsIgnoreCase(state)) {
                if (br2.containsHTML("wait specified time")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerIssue", 5 * 60 * 1000l);
                }
                logger.info(br2.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final URLConnectionAdapter con1 = openAntiDDoSRequestConnection(br, br.createGetRequest("https://rapidgator.net/download/captcha"));
            if (con1.getResponseCode() == 302) {
                try {
                    con1.disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerIssue", 5 * 60 * 1000l);
            } else if (con1.getResponseCode() == 403) {
                try {
                    con1.disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (con1.getResponseCode() == 500) {
                try {
                    con1.disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Downloading is not possible at the moment", FREE_RECONNECTWAIT_OTHERS);
            }
            // wasn't needed for raz, but psp said something about a redirect)
            br.followConnection();
            final long timeBeforeCaptchaInput = System.currentTimeMillis();
            if (br.containsHTML("(api\\.recaptcha\\.net/|google\\.com/recaptcha/api/)")) {
                final Recaptcha rc = new Recaptcha(br, this);
                for (int i = 0; i <= 5; i++) {
                    rc.parse();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    checkForExpiredCaptcha(timeBeforeCaptchaInput);
                    rc.getForm().put("DownloadCaptchaForm%5Bcaptcha%5D", "");
                    rc.setCode(c);
                    if (br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/)")) {
                        continue;
                    }
                    break;
                }
            } else {
                if (br.containsHTML("//api\\.solvemedia\\.com/papi|//api-secure\\.solvemedia\\.com/papi|//api\\.adscapchta\\.com/")) {
                    final Form captcha = br.getFormbyProperty("id", "captchaform");
                    if (captcha == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    captcha.put("DownloadCaptchaForm[captcha]", "");
                    String code = null, challenge = null;
                    final Browser capt = br.cloneBrowser();
                    if (br.containsHTML("//api\\.solvemedia\\.com/papi|//api-secure\\.solvemedia\\.com/papi")) {
                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new SolveMedia(br);
                        final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        code = getCaptchaCode(cf, downloadLink);
                        checkForExpiredCaptcha(timeBeforeCaptchaInput);
                        final String chid = sm.getChallenge(code);
                        // if (chid == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        captcha.put("adcopy_challenge", chid);
                        captcha.put("adcopy_response", Encoding.urlEncode(code));
                    } else if (br.containsHTML("//api\\.adscapchta\\.com/")) {
                        final String captchaAdress = captcha.getRegex("<iframe src=\'(https?://api\\.adscaptcha\\.com/NoScript\\.aspx\\?CaptchaId=\\d+&PublicKey=[^\'<>]+)").getMatch(0);
                        final String captchaType = new Regex(captchaAdress, "CaptchaId=(\\d+)&").getMatch(0);
                        if (captchaAdress == null || captchaType == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (!"3017".equals(captchaType)) {
                            logger.warning("ADSCaptcha: Captcha type not supported!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        getPage(capt, captchaAdress);
                        challenge = capt.getRegex("<img src=\"(https?://api\\.adscaptcha\\.com//Challenge\\.aspx\\?cid=[^\"]+)").getMatch(0);
                        code = capt.getRegex("class=\"code\">([0-9a-f\\-]+)<").getMatch(0);
                        if (challenge == null || code == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        challenge = getCaptchaCode(challenge, downloadLink);
                        checkForExpiredCaptcha(timeBeforeCaptchaInput);
                        captcha.put("adscaptcha_response_field", challenge);
                        captcha.put("adscaptcha_challenge_field", Encoding.urlEncode(code));
                    }
                    submitForm(captcha);
                }
            }
            final String redirect = br.getRedirectLocation();
            // Set-Cookie: failed_on_captcha=1; path=/ response if the captcha expired.
            if ("1".equals(br.getCookie("http://rapidgator.net", "failed_on_captcha")) || br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/|//api\\.solvemedia\\.com/papi|//api\\.adscaptcha\\.com)") || (redirect != null && redirect.matches("https?://rapidgator\\.net/file/[a-z0-9]+"))) {
                invalidateLastChallengeResponse();
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                validateLastChallengeResponse();
            }
            String dllink = br.getRegex("'(https?://[A-Za-z0-9\\-_]+\\.rapidgator\\.net//\\?r=download/index&session_id=[A-Za-z0-9]+)'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("'(https?://[A-Za-z0-9\\-_]+\\.rapidgator\\.net//\\?r=download/index&session_id=[A-Za-z0-9]+)'").getMatch(0);
            }
            // Old regex
            if (dllink == null) {
                dllink = br.getRegex("location\\.href = '(https?://.*?)'").getMatch(0);
            }
            if (dllink == null) {
                logger.info(br.toString());
                if (br.getRegex("location\\.href = '/\\?r=download/index&session_id=[A-Za-z0-9]+'").matches()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                handleErrorsBasic();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (this.getPluginConfig().getBooleanProperty(EXPERIMENTAL_ENFORCE_SSL, false)) {
                dllink = dllink.replaceFirst("^http://", "https://");
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                final URLConnectionAdapter con = dl.getConnection();
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404 (session expired?)", 30 * 60 * 1000l);
                } else if (con.getResponseCode() == 416) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 10 * 60 * 1000l);
                }
                br.followConnection();
                if (br.containsHTML("<div class=\"error\">[\r\n ]+Error\\. Link expired. You have reached your daily limit of downloads\\.")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Link expired, or You've reached your daily limit ", FREE_RECONNECTWAIT_DAILYLIMIT);
                } else if (br.containsHTML("<div class=\"error\">[\r\n ]+File is already downloading</div>")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download session in progress", FREE_RECONNECTWAIT_OTHERS);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            RapidGatorNet.hasAttemptedDownloadstart.set(true);
            dl.startDownload();
        } finally {
            try {
                if (RapidGatorNet.hasAttemptedDownloadstart.get()) {
                    RapidGatorNet.timeBefore.set(System.currentTimeMillis());
                    this.getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD_TIMESTAMP, System.currentTimeMillis());
                }
                setIP(currentIP, downloadLink);
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * If users need more than X seconds to enter the captcha and we actually send the captcha input after this time has passed, rapidgator
     * will 'ban' the IP of the user for at least 60 minutes. This function is there to avoid this case. Instead of sending the captcha it
     * throws a retry exception, avoiding the 60+ minutes IP 'ban'.
     */
    private void checkForExpiredCaptcha(final long timeBefore) throws PluginException {
        final long passedTime = System.currentTimeMillis() - timeBefore;
        if (passedTime >= (FREE_CAPTCHA_EXPIRE_TIME - 1000)) {
            /*
             * Do NOT throw a captcha Exception here as it is not the users' fault that we cannot download - he simply took too much time to
             * enter the captcha!
             */
            throw new PluginException(LinkStatus.ERROR_RETRY, "Captcha session expired");
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        account.setProperty("PROPERTY_TEMP_DISABLED_TIMEOUT", Property.NULL);
        final AccountInfo ai = new AccountInfo();
        synchronized (RapidGatorNet.LOCK) {
            if (this.getPluginConfig().getBooleanProperty(DISABLE_API_PREMIUM, false)) {
                return fetchAccountInfo_web(account, ai);
            } else {
                return fetchAccountInfo_api(account, ai);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public AccountInfo fetchAccountInfo_api(final Account account, final AccountInfo ai) throws Exception {
        synchronized (RapidGatorNet.LOCK) {
            try {
                final String sid = login_api(account);
                if (sid != null) {
                    account.setValid(true);
                    /* premium account */
                    final String expire_date = PluginJSonUtils.getJsonValue(br, "expire_date");
                    final String traffic_left = PluginJSonUtils.getJsonValue(br, "traffic_left");
                    final String reset_in = PluginJSonUtils.getJsonValue(br, "reset_in");
                    if (expire_date != null && traffic_left != null) {
                        /*
                         * expire date and traffic left are available, so it is a premium account, add one day extra to prevent it from
                         * expiring too early
                         */
                        ai.setValidUntil(Long.parseLong(expire_date) * 1000 + (24 * 60 * 60 * 1000l));
                        final long left = Long.parseLong(traffic_left);
                        ai.setTrafficLeft(left);
                        final long TB = 1024 * 1024 * 1024 * 1024l;
                        if (left > 3 * TB) {
                            ai.setTrafficMax(12 * TB);
                        } else if (left <= 3 * TB && left > TB) {
                            ai.setTrafficMax(3 * TB);
                        } else {
                            ai.setTrafficMax(TB);
                        }
                        if (!ai.isExpired()) {
                            account.setType(AccountType.PREMIUM);
                            /* account still valid */
                            account.setMaxSimultanDownloads(-1);
                            account.setConcurrentUsePossible(true);
                            if (account.getAccountInfo() == null) {
                                account.setAccountInfo(ai);
                            }
                            if (reset_in != null) {
                                // this is pointless, when traffic == 0 == core automatically sets ai.settraffic("No Traffic Left")
                                // ai.setStatus("Traffic exceeded " + reset_in);
                                // account.setAccountInfo(ai);
                                // is reset_in == seconds, * 1000 back into ms.
                                final Long resetInTimestamp = Long.parseLong(reset_in) * 1000;
                                account.setProperty("PROPERTY_TEMP_DISABLED_TIMEOUT", resetInTimestamp);
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                            }
                            return ai;
                        }
                    }
                    account.setType(AccountType.FREE);
                    account.setMaxSimultanDownloads(1);
                    account.setConcurrentUsePossible(false);
                    return ai;
                }
                account.setType(null);
                account.setProperty("session_id", Property.NULL);
                account.setValid(false);
                return ai;
            } catch (final PluginException e) {
                if (e.getLinkStatus() != 256) {
                    account.setType(null);
                    account.setProperty("session_id", Property.NULL);
                    account.setValid(false);
                }
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public AccountInfo fetchAccountInfo_web(final Account account, final AccountInfo ai) throws Exception {
        try {
            login_web(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (Account.AccountType.FREE.equals(account.getType())) {
            // free accounts still have captcha.
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            ai.setUnlimitedTraffic();
        } else {
            getPage("/profile/index");
            String availableTraffic = br.getRegex(">Bandwith available</td>\\s+<td>\\s+([^<>\"]*?) of").getMatch(0);
            final String availableTrafficMax = br.getRegex(">Bandwith available</td>\\s+<td>\\s+[^<>\"]*? of (\\d+(\\.\\d+)? (?:MB|GB|TB))").getMatch(0);
            logger.info("availableTraffic = " + availableTraffic);
            if (availableTraffic != null) {
                Long avtr = SizeFormatter.getSize(availableTraffic.trim());
                if (avtr == 0) {
                    availableTraffic = "1024 GB"; // SizeFormatter can't handle TB (Temporary workaround)
                }
                ai.setTrafficLeft(SizeFormatter.getSize(availableTraffic.trim()));
                if (availableTrafficMax != null) {
                    ai.setTrafficMax(SizeFormatter.getSize(availableTrafficMax));
                }
            } else {
                /* Probably not true but our errorhandling for empty traffic should work well */
                ai.setUnlimitedTraffic();
            }
            String expireDate = br.getRegex("Premium services will end on ([^<>\"]*?)\\.<br").getMatch(0);
            if (expireDate == null) {
                expireDate = br.getRegex("login-open1.*Premium till (\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            }
            if (expireDate == null) {
                /**
                 * eg subscriptions
                 */
                getPage("/Payment/Payment");
                // expireDate = br.getRegex("style=\"width:60px;\">\\d+</td><td>([^<>\"]*?)</td>").getMatch(0);
                expireDate = br.getRegex("style=\"width.*?style=\"width.*?style=\"width.*?>([^<>\"]*?)<").getMatch(0);
            }
            if (expireDate == null) {
                logger.warning("Could not find expire date!");
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd", Locale.ENGLISH) + 24 * 60 * 60 * 1000l);
            }
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        }
        account.setValid(true);
        return ai;
    }

    private Cookies login_web(final Account account, final boolean force) throws Exception {
        synchronized (RapidGatorNet.LOCK) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && account.isValid()) {
                    /*
                     * Make sure that we're logged in. Doing this for every downloadlink might sound like a waste of server capacity but
                     * really it doesn't hurt anybody.
                     */
                    br.setCookies(getHost(), cookies);
                    /* Even if login is forced, use cookies and check if they are still valid to avoid the captcha below */
                    br.setFollowRedirects(true);
                    avoidBlock(br);
                    if (br.containsHTML("<a href=\"/auth/logout\"")) {
                        setAccountTypeWebsite(account, br);
                        account.saveCookies(br.getCookies(getHost()), "");
                        return cookies;
                    }
                }
                br = new Browser();
                br.setFollowRedirects(true);
                avoidBlock(br);
                for (int i = 1; i <= 3; i++) {
                    logger.info("Site login attempt " + i + " of 3");
                    getPage("https://rapidgator.net/auth/login");
                    String loginPostData = "LoginForm%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass());
                    final Form loginForm = br.getFormbyProperty("id", "login");
                    final String captcha_url = br.getRegex("\"(/auth/captcha/v/[a-z0-9]+)\"").getMatch(0);
                    String code = null;
                    if (captcha_url != null) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", "rapidgator.net", "http://rapidgator.net", true);
                        code = getCaptchaCode(captcha_url, dummyLink);
                        loginPostData += "&LoginForm%5BverifyCode%5D=" + Encoding.urlEncode(code);
                    }
                    if (loginForm != null) {
                        String user = loginForm.getBestVariable("email");
                        String pass = loginForm.getBestVariable("password");
                        if (user == null) {
                            user = "LoginForm%5Bemail%5D";
                        }
                        if (pass == null) {
                            pass = "LoginForm%5Bpassword%5D";
                        }
                        loginForm.put(user, Encoding.urlEncode(account.getUser()));
                        loginForm.put(pass, Encoding.urlEncode(account.getPass()));
                        if (captcha_url != null) {
                            loginForm.put("LoginForm%5BverifyCode%5D", Encoding.urlEncode(code));
                        }
                        submitForm(loginForm);
                        loginPostData = loginForm.getPropertyString();
                    } else {
                        postPage("/auth/login", loginPostData);
                    }
                    if (br.getCookie(RapidGatorNet.MAINPAGE, "user__") == null) {
                        continue;
                    }
                    break;
                }
                if (br.getCookie(RapidGatorNet.MAINPAGE, "user__") == null) {
                    logger.info("disabled because of" + br.toString());
                    final String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                setAccountTypeWebsite(account, br);
                account.saveCookies(br.getCookies(getHost()), "");
                return cookies;
            } catch (final PluginException e) {
                account.setType(null);
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
    }

    private void setAccountTypeWebsite(final Account account, final Browser br) {
        if (br.containsHTML("Account:\\&nbsp;<a href=\"/article/premium\">Free</a>")) {
            account.setType(AccountType.FREE);
        } else {
            account.setType(AccountType.PREMIUM);
        }
    }

    private String login_api(final Account account) throws Exception {
        URLConnectionAdapter con = null;
        synchronized (RapidGatorNet.LOCK) {
            try {
                avoidBlock(br);
                con = openAntiDDoSRequestConnection(br, br.createGetRequest(apiURL + "user/login?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass())));
                handleErrors_api(null, null, account, con);
                if (con.getResponseCode() == 200) {
                    br.followConnection();
                    final String session_id = PluginJSonUtils.getJsonValue(br, "session_id");
                    if (session_id != null) {
                        boolean isPremium = false;
                        final String expire_date = PluginJSonUtils.getJsonValue(br, "expire_date");
                        final String traffic_left = PluginJSonUtils.getJsonValue(br, "traffic_left");
                        if (expire_date != null && traffic_left != null) {
                            /*
                             * expire date and traffic left are available, so its a premium account, add one day extra to prevent it from
                             * expiring too early
                             */
                            final AccountInfo ai = new AccountInfo();
                            ai.setValidUntil(Long.parseLong(expire_date) * 1000 + (24 * 60 * 60 * 1000l));
                            isPremium = !ai.isExpired();
                        }
                        if (isPremium) {
                            account.setType(Account.AccountType.PREMIUM);
                        } else {
                            account.setType(Account.AccountType.FREE);
                        }
                        account.setProperty("session_id", session_id);
                    }
                    return session_id;
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable ignore) {
                }
            }
        }
    }

    private void avoidBlock(Browser br) throws Exception {
        getPage(br, "https://rapidgator.net/");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        correctDownloadLink(link);
        if (this.getPluginConfig().getBooleanProperty(DISABLE_API_PREMIUM, false)) {
            requestFileInformation(link);
            handlePremium_web(link, account);
        } else {
            handlePremium_api(link, account);
        }
    }

    public static String readErrorStream(final URLConnectionAdapter con) throws UnsupportedEncodingException, IOException {
        BufferedReader f = null;
        try {
            con.setAllowedResponseCodes(new int[] { con.getResponseCode() });
            final InputStream es = con.getErrorStream();
            if (es == null) {
                throw new IOException("No errorstream!");
            }
            f = new BufferedReader(new InputStreamReader(es, "UTF8"));
            String line;
            final StringBuilder ret = new StringBuilder();
            final String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }
                ret.append(line);
            }
            return ret.toString();
        } finally {
            try {
                f.close();
            } catch (final Throwable e) {
            }
        }
    }

    private void handleErrors_api(final String session_id, final DownloadLink link, final Account account, final URLConnectionAdapter con) throws PluginException, UnsupportedEncodingException, IOException {
        if (link != null) {
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (con.getResponseCode() == 416) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
            }
            if (con.getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 60 * 60 * 1000l);
            }
        }
        if (con.getResponseCode() != 200) {
            synchronized (RapidGatorNet.LOCK) {
                final String lang = System.getProperty("user.language");
                final String errorMessage = RapidGatorNet.readErrorStream(con);
                logger.info("ErrorMessage: " + errorMessage);
                if (link != null && errorMessage.contains("Exceeded traffic")) {
                    final AccountInfo ac = new AccountInfo();
                    ac.setTrafficLeft(0);
                    account.setAccountInfo(ac);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                final boolean sessionReset = session_id != null && session_id.equals(account.getStringProperty("session_id", null));
                if (errorMessage.contains("Please wait")) {
                    if (link == null) {
                        /* we are inside fetchAccountInfo */
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Server says: 'Please wait ...'", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    } else {
                        /* we are inside handlePremium */
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server says: 'Please wait ...'", 10 * 60 * 1000l);
                    }
                } else if (errorMessage.contains("User is not PREMIUM") || errorMessage.contains("This file can be downloaded by premium only") || errorMessage.contains("You can download files up to")) {
                    if (sessionReset) {
                        logger.info("SessionReset:" + sessionReset);
                        account.setProperty("session_id", Property.NULL);
                    }
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else if (errorMessage.contains("Login or password is wrong") || errorMessage.contains("Error: Error e-mail or password")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (errorMessage.contains("Password cannot be blank")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDas Passwortfeld darf nicht leer sein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nThe password field cannot be blank!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (errorMessage.contains("User is FROZEN")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount ist gesperrt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount is banned!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (StringUtils.containsIgnoreCase(errorMessage, "Error: ACCOUNT LOCKED FOR VIOLATION OF OUR TERMS. PLEASE CONTACT SUPPORT.")) {
                    // most likely account sharing as result of shared account dbs.
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount Locked! Violation of Terms of Service!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (errorMessage.contains("Parameter login or password is missing")) {
                    /*
                     * Unusual case but this may also happen frequently if users use strange chars as usernme/password so simply treat this
                     * as "login/password wrong"!
                     */
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (errorMessage.contains("Session not exist")) {
                    if (sessionReset) {
                        logger.info("SessionReset:" + sessionReset);
                        account.setProperty("session_id", Property.NULL);
                    }
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else if (errorMessage.contains("\"Error: Error e\\-mail or password")) {
                    /* Usually comes with response_status 401 --> Not exactly sure what it means but probably some kind of account issue. */
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (errorMessage.contains("Error: You requested login to your account from unusual Ip address")) {
                    /* User needs to confirm his current IP. */
                    String statusMessage;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        statusMessage = "\r\nBitte bestätige deine aktuelle IP Adresse über den Bestätigungslink per E-Mail um den Account wieder nutzen zu können.";
                    } else {
                        statusMessage = "\r\nPlease confirm your current IP adress via the activation link you got per mail to continue using this account.";
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                if (con.getResponseCode() == 503 || errorMessage.contains("Service Temporarily Unavailable")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Service Temporarily Unavailable", 5 * 60 * 1000l);
                }
                if (con.getResponseCode() == 401 || errorMessage.contains("Wrong e-mail or password")) {
                    final String userName = account.getUser();
                    if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Username must be an e-mail", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Wrong e-mail or password", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (link != null) {
                    // disable api?
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void handlePremium_api(final DownloadLink link, final Account account) throws Exception {
        String session_id = null;
        boolean isPremium = false;
        synchronized (RapidGatorNet.LOCK) {
            session_id = account.getStringProperty("session_id", null);
            if (session_id == null) {
                session_id = login_api(account);
            }
            isPremium = Account.AccountType.PREMIUM.equals(account.getType());
        }
        if (isPremium == false) {
            handleFree(link);
            return;
        }
        if (session_id == null) {
            // disable api?
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        URLConnectionAdapter con = null;
        String fileName = link.getFinalFileName();
        if (fileName == null) {
            /* no final filename yet, do linkcheck */
            try {
                con = openAntiDDoSRequestConnection(br, br.createGetRequest(apiURL + "file/info?sid=" + session_id + "&url=" + Encoding.urlEncode(link.getDownloadURL())));
                handleErrors_api(session_id, link, account, con);
                if (con.getResponseCode() == 200) {
                    br.followConnection();
                    fileName = PluginJSonUtils.getJsonValue(br, "filename");
                    final String fileSize = PluginJSonUtils.getJsonValue(br, "size");
                    final String fileHash = PluginJSonUtils.getJsonValue(br, "hash");
                    if (fileName != null) {
                        link.setFinalFileName(fileName);
                    }
                    if (fileSize != null) {
                        final long size = Long.parseLong(fileSize);
                        try {
                            link.setVerifiedFileSize(size);
                        } catch (final Throwable not09581) {
                            link.setDownloadSize(size);
                        }
                    }
                    if (fileHash != null) {
                        link.setMD5Hash(fileHash);
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable ignore) {
                }
            }
        }
        String url = null;
        try {
            con = openAntiDDoSRequestConnection(br, br.createGetRequest(apiURL + "file/download?sid=" + session_id + "&url=" + Encoding.urlEncode(link.getDownloadURL())));
            handleErrors_api(session_id, link, account, con);
            if (con.getResponseCode() == 200) {
                br.followConnection();
                url = PluginJSonUtils.getJsonValue(br, "url");
                if (url != null) {
                    url = url.replace("\\", "");
                    url = url.replace("//?", "/?");
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable ignore) {
            }
        }
        if ("false".equals(url)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (url == null) {
            // disable api?
            // {"response":{"url":false},"response_status":200,"response_details":null}
            /*
             * This can happen if links go offline in the moment when the user is trying to download them - I (psp) was not able to
             * reproduce this so this is just a bad workaround! Correct server response would be:
             * 
             * {"response":null,"response_status":404,"response_details":"Error: File not found"}
             * 
             * TODO: Maybe move this info handleErrors_api
             */
            if (br.containsHTML("\"response_details\":null")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (this.getPluginConfig().getBooleanProperty(EXPERIMENTAL_ENFORCE_SSL, false)) {
            url = url.replaceFirst("^http://", "https://");
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, true, maxPremChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            handleErrors_api(session_id, link, account, dl.getConnection());
            // so we can see errors maybe proxy errors etc.
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private final int maxPremChunks = -5; // 21.11.16, check highest that can be handled without server issues

    @SuppressWarnings("deprecation")
    public void handlePremium_web(final DownloadLink link, final Account account) throws Exception {
        logger.info("Performing cached login sequence!!");
        Cookies cookies = login_web(account, false);
        final int repeat = 2;
        for (int i = 0; i <= repeat; i++) {
            br.setFollowRedirects(false);
            getPage(br, link.getDownloadURL());
            if (br.getCookie(RapidGatorNet.MAINPAGE, "user__") == null && i + 1 != repeat) {
                // lets login fully again, as hoster as removed premium cookie for some unknown reason...
                logger.info("Performing full login sequence!!");
                br = new Browser();
                cookies = login_web(account, true);
                continue;
            } else if (br.getCookie(RapidGatorNet.MAINPAGE, "user__") == null && i + 1 == repeat) {
                // failure
                logger.warning("handlePremium Failed! Please report to JDownloader Development Team.");
                synchronized (RapidGatorNet.LOCK) {
                    if (cookies == null) {
                        account.setProperty("cookies", Property.NULL);
                    }
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                break;
            }
        }
        if (Account.AccountType.FREE.equals(account.getType())) {
            doFree(link);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                dllink = br.getRegex("var premium_download_link = '(https?://[^<>\"']+)';").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("'(https?://pr_srv\\.rapidgator\\.net//\\?r=download/index&session_id=[A-Za-z0-9]+)'").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("'(https?://pr\\d+\\.rapidgator\\.net//\\?r=download/index&session_id=[A-Za-z0-9]+)'").getMatch(0);
                        if (dllink == null) {
                            if (br.containsHTML("You have reached quota|You have reached daily quota of downloaded information for premium accounts")) {
                                logger.info("You've reached daily download quota for " + account.getUser() + " account");
                                final AccountInfo ac = new AccountInfo();
                                ac.setTrafficLeft(0);
                                account.setAccountInfo(ac);
                                throw new PluginException(LinkStatus.ERROR_RETRY);
                            }
                            if (br.getCookie(RapidGatorNet.MAINPAGE, "user__") == null) {
                                logger.info("Account seems to be invalid!");
                                // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                                account.setProperty("cookies", Property.NULL);
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            logger.warning("Could not find 'dllink'. Please report to JDownloader Development Team");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
            }
            if (this.getPluginConfig().getBooleanProperty(EXPERIMENTAL_ENFORCE_SSL, false)) {
                dllink = dllink.replaceFirst("^http://", "https://");
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, Encoding.htmlDecode(dllink), true, maxPremChunks);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                handleErrors_api(null, link, account, dl.getConnection());
                // so we can see errors maybe proxy errors etc.
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void simulateBrowser() {
        final String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
        for (final String link : sitelinks) {
            if (link.matches("(.+\\.(js|css))")) {
                URLConnectionAdapter con = null;
                try {
                    final Browser rb = br.cloneBrowser();
                    con = openAntiDDoSRequestConnection(rb, rb.createGetRequest(link));
                } catch (final Throwable e) {
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Exception e) {
                    }
                }
            }
        }
    }

    private void handleErrorsBasic() throws PluginException {
        if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404 (session expired?)", 30 * 60 * 1000l);
        }
    }

    private String getIP() throws PluginException {
        final Browser ip = new Browser();
        String currentIP = null;
        final ArrayList<String> checkIP = new ArrayList<String>(Arrays.asList(IPCHECK));
        Collections.shuffle(checkIP);
        for (final String ipServer : checkIP) {
            if (currentIP == null) {
                try {
                    ip.getPage(ipServer);
                    currentIP = ip.getRegex(IPREGEX).getMatch(0);
                    if (currentIP != null) {
                        break;
                    }
                } catch (final Throwable e) {
                }
            }
        }
        if (currentIP == null) {
            logger.warning("firewall/antivirus/malware/peerblock software is most likely is restricting accesss to JDownloader IP checking services");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return currentIP;
    }

    private boolean ipChanged(final String IP, final DownloadLink link) throws PluginException {
        String currentIP = null;
        if (IP != null && new Regex(IP, IPREGEX).matches()) {
            currentIP = IP;
        } else {
            currentIP = getIP();
        }
        if (currentIP == null) {
            return false;
        }
        String lastIP = link.getStringProperty(LASTIP, null);
        if (lastIP == null) {
            lastIP = RapidGatorNet.lastIP.get();
        }
        return !currentIP.equals(lastIP);
    }

    private boolean setIP(final String IP, final DownloadLink link) throws PluginException {
        synchronized (IPCHECK) {
            if (IP != null && !new Regex(IP, IPREGEX).matches()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (ipChanged(IP, link) == false) {
                // Static IP or failure to reconnect! We don't change lastIP
                logger.warning("Your IP hasn't changed since last download");
                return false;
            } else {
                final String lastIP = IP;
                link.setProperty(LASTIP, lastIP);
                RapidGatorNet.lastIP.set(lastIP);
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
    }

    private long getPluginSavedLastDownloadTimestamp() {
        return getLongProperty(getPluginConfig(), PROPERTY_LASTDOWNLOAD_TIMESTAMP, 0);
    }

    private static long getLongProperty(final Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), EXPERIMENTALHANDLING, JDL.L("plugins.hoster.rapidgatornet.useExperimentalWaittimeHandling", "Activate experimental waittime handling to prevent 24-hours IP ban from rapidgator?")).setDefaultValue(false));
        // Some users always get server error 500 via API
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), DISABLE_API_PREMIUM, JDL.L("plugins.hoster.rapidgatornet.disableAPIPremium", "Disable API for premium downloads (use web download)?")).setDefaultValue(false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), EXPERIMENTAL_ENFORCE_SSL, JDL.L("plugins.hoster.rapidgatornet.useExperimentalEnforceSSL", "Activate experimental forced SSL for downloads?")).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    public void getPage(final String page) throws Exception {
        super.getPage(br, page);
    }
}