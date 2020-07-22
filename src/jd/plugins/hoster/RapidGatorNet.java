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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.DispositionHeader;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.RapidGatorConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapidgator.net" }, urls = { "https?://(?:www\\.)?(?:rapidgator\\.net|rapidgator\\.asia|rg\\.to)/file/([a-z0-9]{32}(?:/[^/<>]+\\.html)?|\\d+(?:/[^/<>]+\\.html)?)" })
public class RapidGatorNet extends antiDDoSForHost {
    public RapidGatorNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://rapidgator.net/article/premium");
    }

    private static final String            MAINPAGE                                   = "https://rapidgator.net/";
    private static final String            PREMIUMONLYTEXT                            = "This file can be downloaded by premium only</div>";
    /*
     * 2019-12-14: Rapidgator API has a bug which will return invalid offline status. Do NOT trust this status anymore! Wait and retry
     * instead. If the file is offline, availableStatus will find that correct status eventually! This may happen in two cases: 1.
     * Free/Expired premium account tries to download via API.
     */
    private final boolean                  API_TRUST_404_FILE_OFFLINE                 = false;
    /* Old V1 endpoint */
    // private final String API_BASEv1 = "https://rapidgator.net/api/";
    /* https://rapidgator.net/article/api/index */
    private final String                   API_BASEv2                                 = "https://rapidgator.net/api/v2/";
    /* Enforce new session once current one is older than X minutes. 0 or -1 = never refresh session_id unless it is detected as invalid. */
    private final long                     API_SESSION_ID_REFRESH_TIMEOUT_MINUTES     = 45;
    /*
     * 2020-01-07: Use 120 minutes for the website login for now. Consider disabling this on negative feedback as frequent website logins
     * may lead to login-captchas!
     */
    private final long                     WEBSITE_SESSION_ID_REFRESH_TIMEOUT_MINUTES = 1;
    private final String[]                 IPCHECK                                    = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };
    private static AtomicBoolean           hasAttemptedDownloadstart                  = new AtomicBoolean(false);
    private static AtomicLong              timeBefore                                 = new AtomicLong(0);
    private static final String            PROPERTY_LASTDOWNLOAD_TIMESTAMP            = "rapidgatornet_lastdownload_timestamp";
    private static final String            PROPERTY_sessionid                         = "session_id";
    private static final String            PROPERTY_timestamp_session_create_api      = "session_create";
    private static final String            PROPERTY_timestamp_session_create_website  = "session_create_website";
    private final String                   LASTIP                                     = "LASTIP";
    private final String                   HOTLINK                                    = "HOTLINK";
    private static AtomicReference<String> lastIP                                     = new AtomicReference<String>();
    private final Pattern                  IPREGEX                                    = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    /* 2019-12-12: Lowered from 2 to 1 hour */
    private static final long              FREE_RECONNECTWAIT_GENERAL                 = 1 * 60 * 60 * 1001L;
    private static final long              FREE_RECONNECTWAIT_DAILYLIMIT              = 3 * 60 * 60 * 1000L;
    private static final long              FREE_RECONNECTWAIT_OTHERS                  = 30 * 60 * 1000L;
    private static final long              FREE_CAPTCHA_EXPIRE_TIME                   = 105 * 1000L;
    // CONTENT-DISPOSITION header is missing encoding
    private static final boolean           FIX_FILENAMES                              = true;

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

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        if (link.getPluginPatternMatcher().contains("rg.to/")) {
            String url = link.getPluginPatternMatcher();
            url = url.replaceFirst("rg.to/", "rapidgator.net/");
            link.setPluginPatternMatcher(url);
        }
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("http://", "https://"));
        final String linkID = getFID(link);
        if (linkID != null) {
            link.setLinkID(getHost() + "://" + linkID);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/file/([a-z0-9]{32}|\\d+)").getMatch(0);
    }

    private String getURLFilename(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), ".+/(.+)\\.html$").getMatch(0);
    }

    @Override
    public String filterPackageID(String packageIdentifier) {
        return packageIdentifier.replaceAll("([^a-zA-Z0-9]+)", "");
    }

    private char[] FILENAMEREPLACES = new char[] { ' ', '_' };

    @Override
    public char[] getFilenameReplaceMap() {
        return FILENAMEREPLACES;
    }

    @Override
    public boolean isHosterManipulatesFilenames() {
        return true;
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
            prepBr.setCookie("https://rapidgator.net/", "lang", "en");
            prepBr.setCustomCharset("UTF-8");
            /*
             * 2020-04-09: According to user he has timeout issues which do not happen in browser thus let's test a higher readtimeout:
             * https://board.jdownloader.org/showthread.php?t=83764
             */
            final int customReadTimeoutSeconds = PluginJsonConfig.get(RapidGatorConfig.class).getReadTimeout();
            prepBr.setReadTimeout(customReadTimeoutSeconds * 1000);
            prepBr.setConnectTimeout(1 * 60 * 1000);
            // for the api
            prepBr.addAllowedResponseCodes(401, 402, 501, 423);
        }
        return prepBr;
    }

    private String hotLinkURL = null;

    @Override
    public void clean() {
        try {
            super.clean();
        } finally {
            synchronized (INVALIDSESSIONMAP) {
                // remove weak references
                INVALIDSESSIONMAP.size();
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        hotLinkURL = null;
        correctDownloadLink(link);
        setBrowserExclusive();
        br.setFollowRedirects(false);
        final String custom_referer = PluginJsonConfig.get(RapidGatorConfig.class).getReferer();
        if (!StringUtils.isEmpty(custom_referer)) {
            /*
             * 2019-12-14: According to users, some special Referer will remove the captcha in free mode (I was unable to confirm) and lower
             * the waittime between downloads from 120 to 60 minutes.
             */
            br.getHeaders().put("Referer", custom_referer);
        }
        getPage(link.getPluginPatternMatcher());
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            br.setFollowRedirects(true);
            if (redirect.matches(".*?\\?r=download/index&session_id=[A-Za-z0-9]+")) {
                final URLConnectionAdapter con = openAntiDDoSRequestConnection(br, br.createHeadRequest(redirect));
                try {
                    if (con.isOK() && con.isContentDisposition()) {
                        if (con.getLongContentLength() > 0) {
                            link.setVerifiedFileSize(con.getLongContentLength());
                        }
                        if (link.getFinalFileName() == null) {
                            final DispositionHeader header = Plugin.parseDispositionHeader(con);
                            if (header != null && StringUtils.isNotEmpty(header.getFilename())) {
                                if (header.getEncoding() != null) {
                                    link.setFinalFileName(header.getFilename());
                                } else {
                                    final String fileName;
                                    if (FIX_FILENAMES) {
                                        fileName = URLEncode.decodeURIComponent(header.getFilename(), "UTF-8", true);
                                    } else {
                                        fileName = header.getFilename();
                                    }
                                    link.setFinalFileName(fileName);
                                }
                            }
                        }
                        link.setProperty(HOTLINK, Boolean.TRUE);
                        hotLinkURL = con.getURL().toString();
                        return AvailableStatus.TRUE;
                    } else {
                        link.removeProperty(HOTLINK);
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } finally {
                    con.disconnect();
                }
            } else {
                getPage(redirect);
            }
        }
        link.removeProperty(HOTLINK);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("File not found")) {
            String filenameFromURL = getURLFilename(link);
            if (filenameFromURL != null) {
                final String fileName;
                if (FIX_FILENAMES) {
                    fileName = URLEncode.decodeURIComponent(filenameFromURL, "UTF-8", true);
                } else {
                    fileName = filenameFromURL;
                }
                link.setName(fileName);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String freedlsizelimit = br.getRegex("'You can download files up to\\s*([\\d\\.]+ ?(MB|GB))\\s*in free mode<").getMatch(0);
        if (freedlsizelimit != null) {
            link.getLinkStatus().setStatusText("This file is restricted to Premium users only");
        }
        final String md5 = br.getRegex(">\\s*MD5\\s*:\\s*([A-Fa-f0-9]{32})<").getMatch(0);
        String filename = br.getRegex("Downloading\\s*:\\s*</strong>([^<>\"]+)</p>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Download file\\s*([^<>\"]+)</title>").getMatch(0);
        }
        final String filesize = br.getRegex("File size:\\s*<strong>([^<>\"]+)</strong>").getMatch(0);
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, null);
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink link, final Account account) throws Exception {
        // experimental code - raz
        // so called 15mins between your last download, ends up with your IP blocked for the day..
        // Trail and error until we find the sweet spot.
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        final String currentIP = getIP();
        String finalDownloadURL = null;
        if (!StringUtils.isEmpty(hotLinkURL)) {
            logger.info("Seems to be a hotlink file:" + hotLinkURL);
            finalDownloadURL = hotLinkURL;
        } else {
            finalDownloadURL = checkDirectLink(link, account);
            if (StringUtils.isEmpty(finalDownloadURL)) {
                final boolean useExperimentalHandling = PluginJsonConfig.get(RapidGatorConfig.class).isActivateExperimentalWaittimeHandling();
                if (useExperimentalHandling) {
                    logger.info("New Download: currentIP = " + currentIP);
                    if (ipChanged(currentIP, link) == false) {
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
            }
        }
        try {
            if (finalDownloadURL == null) {
                // end of experiment
                handleErrorsWebsite(this.br, link, null);
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
                getPage(br2, "https://" + this.getHost() + "/download/AjaxStartTimer?fid=" + fid);
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
                sleep((wait + 5) * 1001l, link);
                /* needed so we have correct referrer ;) (back to original br) */
                br2 = br.cloneBrowser();
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                getPage(br2, "https://" + this.getHost() + "/download/AjaxGetDownloadLink?sid=" + sid);
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
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerIssue", 5 * 60 * 1000l);
                } else if (con1.getResponseCode() == 403) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (con1.getResponseCode() == 500) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Downloading is not possible at the moment", FREE_RECONNECTWAIT_OTHERS);
                }
                // wasn't needed for raz, but psp said something about a redirect)
                br.followConnection();
                final long timeBeforeCaptchaInput = System.currentTimeMillis();
                Form captcha = null;
                if (br.containsHTML("data-sitekey")) {
                    captcha = br.getFormbyProperty("id", "captchaform");
                    if (captcha == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                    final String recaptchaV2Response = rc2.getToken();
                    if (recaptchaV2Response == null) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    captcha.put("DownloadCaptchaForm[verifyCode]", recaptchaV2Response);
                } else if (br.containsHTML("//api\\.solvemedia\\.com/papi|//api-secure\\.solvemedia\\.com/papi")) {
                    captcha = br.getFormbyProperty("id", "captchaform");
                    if (captcha == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        captcha.put("DownloadCaptchaForm[captcha]", "");
                    }
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new SolveMedia(br);
                    final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    final String code = getCaptchaCode(cf, link);
                    checkForExpiredCaptcha(timeBeforeCaptchaInput);
                    final String chid = sm.getChallenge(code);
                    // if (chid == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    captcha.put("adcopy_challenge", chid);
                    captcha.put("adcopy_response", Encoding.urlEncode(code));
                } else if (br.containsHTML("//api\\.adscapchta\\.com/")) {
                    captcha = br.getFormbyProperty("id", "captchaform");
                    if (captcha == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        captcha.put("DownloadCaptchaForm[captcha]", "");
                    }
                    final String captchaAdress = captcha.getRegex("<iframe src=\'(https?://api\\.adscaptcha\\.com/NoScript\\.aspx\\?CaptchaId=\\d+&PublicKey=[^\'<>]+)").getMatch(0);
                    final String captchaType = new Regex(captchaAdress, "CaptchaId=(\\d+)&").getMatch(0);
                    if (captchaAdress == null || captchaType == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (!"3017".equals(captchaType)) {
                        logger.warning("ADSCaptcha: Captcha type not supported!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Browser adsCaptcha = br.cloneBrowser();
                    getPage(adsCaptcha, captchaAdress);
                    String challenge = adsCaptcha.getRegex("<img src=\"(https?://api\\.adscaptcha\\.com//Challenge\\.aspx\\?cid=[^\"]+)").getMatch(0);
                    if (StringUtils.isEmpty(challenge)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String code = adsCaptcha.getRegex("class=\"code\">([0-9a-f\\-]+)<").getMatch(0);
                    if (StringUtils.isEmpty(code)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    challenge = getCaptchaCode(challenge, link);
                    if (StringUtils.isEmpty(challenge)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    checkForExpiredCaptcha(timeBeforeCaptchaInput);
                    captcha.put("adscaptcha_response_field", challenge);
                    captcha.put("adscaptcha_challenge_field", Encoding.urlEncode(code));
                }
                if (captcha != null) {
                    submitForm(captcha);
                }
                final String redirect = br.getRedirectLocation();
                // Set-Cookie: failed_on_captcha=1; path=/ response if the captcha expired.
                if ("1".equals(br.getCookie("http://rapidgator.net", "failed_on_captcha")) || br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/|//api\\.solvemedia\\.com/papi|//api\\.adscaptcha\\.com)") || (redirect != null && redirect.matches("https?://rapidgator\\.net/file/[a-z0-9]+"))) {
                    invalidateLastChallengeResponse();
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    validateLastChallengeResponse();
                }
                finalDownloadURL = br.getRegex("'(https?://[A-Za-z0-9\\-_]+\\.rapidgator\\.net//\\?r=download/index&session_id=[A-Za-z0-9]+)'").getMatch(0);
                if (finalDownloadURL == null) {
                    // Old regex
                    finalDownloadURL = br.getRegex("location\\.href = '(https?://.*?)'").getMatch(0);
                }
                if (finalDownloadURL == null) {
                    /* 2020-02-06 */
                    finalDownloadURL = br.getRegex("(https?://[^/]+/download/[^<>\"\\']+)").getMatch(0);
                }
                if (finalDownloadURL == null) {
                    logger.info(br.toString());
                    if (br.getRegex("location\\.href = '/\\?r=download/index&session_id=[A-Za-z0-9]+'").matches()) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    handleErrorsBasic();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (PluginJsonConfig.get(RapidGatorConfig.class).isExperimentalEnforceSSL()) {
                finalDownloadURL = finalDownloadURL.replaceFirst("^http://", "https://");
            }
            // 2020-05-27, rapidgator now advertises that it doesn't support resume for free accounts
            // 2020-07-14: Resume works in free mode for most of all files. For some, server may return an "X-Error" header with the content
            // "Unexpected range request" - see code below.
            final boolean resume = link.getBooleanProperty(DownloadLink.PROPERTY_RESUMEABLE, true);
            /* E.g. when directurl was re-used successfully, download is already ready to be started! */
            if (dl == null) {
                if (!resume) {
                    logger.info("Resume disabled for this download");
                }
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finalDownloadURL, resume, getMaxChunks(account));
            }
            /* 2020-03-17: Content-Disposition should always be given */
            if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text") || !dl.getConnection().isContentDisposition() || !dl.getConnection().isOK()) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                final URLConnectionAdapter con = dl.getConnection();
                final int responsecode = con.getResponseCode();
                if (responsecode == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404 (session expired?)", 30 * 60 * 1000l);
                } else if (responsecode == 416) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 10 * 60 * 1000l);
                }
                String json_errormsg = PluginJSonUtils.getJson(br, "error");
                final String error = con.getRequest().getResponseHeader("X-Error");
                if ("Unexpected range request".equalsIgnoreCase(error)) {
                    /* Resume impossible */
                    if (!resume) {
                        /* Resume was already disabled? Then we cannot do anything about it --> Wait and retry later */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown resume related server error");
                    }
                    /* Reset progress and try again */
                    logger.info("Resume impossible, disabling it for the next try");
                    /*
                     * Special: Save directurl although we have an error --> It should be 're-usable' because this failed attempt does not
                     * count as download attempt serverside.
                     */
                    link.setProperty(getDownloadModeDirectlinkProperty(account), finalDownloadURL);
                    /* Disable resume on next attempt */
                    link.setProperty(DownloadLink.PROPERTY_RESUMEABLE, Boolean.valueOf(false));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (br.containsHTML("<div class=\"error\">\\s*Error\\. Link expired\\. You have reached your daily limit of downloads\\.")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Link expired, or You've reached your daily limit ", FREE_RECONNECTWAIT_DAILYLIMIT);
                } else if (br.containsHTML("<div class=\"error\">\\s*File is already downloading</div>")) {
                    /*
                     * 2020-03-11: Do not throw ERROR_IP_BLOCKED error here as this error will usually only show up for 30-60 seconds
                     * between downloads or upon instant retry of an e.g. interrupted free download --> Reconnect is not required
                     */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 1 * 60 * 1000l);
                } else {
                    logger.info("Unknown error happened");
                    if (StringUtils.isEmpty(json_errormsg)) {
                        json_errormsg = "Unknown server error";
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, json_errormsg);
                }
            }
            /*
             * Always allow resume again for next attempt as a "failed" attempt will still get us a usable direct-URL thus no time- or
             * captcha attempt gets wasted!
             */
            if (resume && link.getBooleanProperty(DownloadLink.PROPERTY_RESUMEABLE, false) == false) {
                link.setProperty(DownloadLink.PROPERTY_RESUMEABLE, Boolean.valueOf(true));
            }
            link.setProperty(getDownloadModeDirectlinkProperty(account), finalDownloadURL);
            RapidGatorNet.hasAttemptedDownloadstart.set(true);
            dl.startDownload();
        } finally {
            try {
                if (RapidGatorNet.hasAttemptedDownloadstart.get()) {
                    RapidGatorNet.timeBefore.set(System.currentTimeMillis());
                    this.getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD_TIMESTAMP, System.currentTimeMillis());
                }
                setIP(currentIP, link);
            } catch (final Throwable e) {
            }
        }
    }

    protected static String getDownloadModeDirectlinkProperty(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return "freelink2";
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return "premlink";
        } else {
            /* Free(anonymous) and unknown account type */
            return "freelink";
        }
    }

    private int getMaxChunks(final Account account) {
        if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            // 21.11.16, check highest that can be handled without server issues
            return -5;
        } else {
            /* Free & Free account */
            return 1;
        }
    }

    private String checkDirectLink(final DownloadLink link, final Account account) throws InterruptedException, PluginException {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        final String dllink = link.getStringProperty(directlinkproperty);
        if (dllink != null) {
            final boolean resume = this.isResumeable(link, account);
            final int maxchunks = this.getMaxChunks(account);
            final Browser br2 = this.br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            boolean valid = false;
            try {
                this.dl = jd.plugins.BrowserAdapter.openDownload(br2, link, dllink, resume, maxchunks);
                con = dl.getConnection();
                if (!con.isOK() || StringUtils.containsIgnoreCase(con.getContentType(), "text") || con.getLongContentLength() == -1) {
                    link.setProperty(directlinkproperty, Property.NULL);
                    return null;
                } else {
                    valid = true;
                    return dllink;
                }
            } catch (final InterruptedException e) {
                throw e;
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(directlinkproperty, Property.NULL);
            } finally {
                if (!valid) {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                    this.dl = null;
                }
            }
        }
        return null;
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
            if (true) {
                logger.warning("Captcha session expired?!:" + TimeFormatter.formatMilliSeconds(passedTime, 0));
            } else {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Captcha session expired");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            if (PluginJsonConfig.get(RapidGatorConfig.class).isEnableAPIPremium()) {
                return fetchAccountInfo_api(account, ai);
            } else {
                return fetchAccountInfo_web(account, ai);
            }
        }
    }

    public AccountInfo fetchAccountInfo_api(final Account account, final AccountInfo ai) throws Exception {
        synchronized (account) {
            try {
                login_api(account, false);
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                String expire_date = PluginJSonUtils.getJsonValue(br, "expire_date");
                if (StringUtils.isEmpty(expire_date)) {
                    /* APIv2 */
                    expire_date = PluginJSonUtils.getJsonValue(br, "premium_end_time");
                }
                boolean is_premium = false;
                if (entries.containsKey("is_premium")) {
                    is_premium = ((Boolean) entries.get("is_premium")).booleanValue();
                }
                /*
                 * E.g. "traffic":{"total":null,"left":null} --> Free Account
                 */
                /*
                 * 2019-12-16: Traffic is valid for the complete runtime of a premium package. If e.g. user owns a 1-year-account and
                 * traffic is down to 0 after one week, account is still a premium account but worthless. Not even free downloads are
                 * possible with such accounts!
                 */
                /*
                 * 2019-12-17: They might also have an unofficial daily trafficlimit of 50-100GB. After this the user will first get a new
                 * password via E-Mail and if he continues to download 'too much', account might get temporarily banned.
                 */
                Object traffic_leftO = PluginJSonUtils.getJsonValue(br, "traffic_left");
                if (traffic_leftO == null) {
                    traffic_leftO = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "response/user/traffic/left"), 0);
                }
                long traffic_max = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "response/user/traffic/total"), 0);
                if (!StringUtils.isEmpty(expire_date) || is_premium) {
                    if (!StringUtils.isEmpty(expire_date) && expire_date.matches("\\d+")) {
                        /*
                         * 2019-12-23: Premium accounts expire too early if we just set the expire-date. Using their Android App even they
                         * will display the wrong expire date there. We have to add 24 hours to correct this.
                         */
                        ai.setValidUntil(Long.parseLong(expire_date) * 1000 + (24 * 60 * 60 * 1000l), br);
                    }
                    long traffic_left = 0;
                    if (traffic_leftO != null) {
                        if (traffic_leftO instanceof Number) {
                            traffic_left = ((Number) traffic_leftO).longValue();
                        } else {
                            traffic_left = Long.parseLong((String) traffic_leftO);
                        }
                    }
                    ai.setTrafficLeft(traffic_left);
                    if (traffic_max > 0) {
                        /* APIv2 */
                        ai.setTrafficMax(traffic_max);
                    } else {
                        /* APIv1/Fallback/Hardcoded */
                        final long TB = 1024 * 1024 * 1024 * 1024l;
                        if (traffic_left > 3 * TB) {
                            ai.setTrafficMax(12 * TB);
                        } else if (traffic_left <= 3 * TB && traffic_left > TB) {
                            ai.setTrafficMax(3 * TB);
                        } else {
                            ai.setTrafficMax(TB);
                        }
                    }
                    if (!ai.isExpired()) {
                        account.setType(AccountType.PREMIUM);
                        /* Premium account valid */
                        account.setMaxSimultanDownloads(-1);
                        account.setConcurrentUsePossible(true);
                        if (account.getAccountInfo() == null) {
                            account.setAccountInfo(ai);
                        }
                        return ai;
                    }
                }
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
                /* API returns null value for trafficleft for free accounts --> Display them as unlimited traffic! */
                ai.setUnlimitedTraffic();
                return ai;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.setType(null);
                    account.setProperty(PROPERTY_sessionid, Property.NULL);
                }
                throw e;
            }
        }
    }

    public AccountInfo fetchAccountInfo_web(final Account account, final AccountInfo ai) throws Exception {
        login_web(account, true);
        if (Account.AccountType.FREE.equals(account.getType())) {
            // free accounts still have captcha.
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            ai.setUnlimitedTraffic();
        } else {
            getPage("/profile/index");
            /*
             * 2019-12-16: Traffic is valid for the complete runtime of a premium package. If e.g. user owns a 1-year-account and traffic is
             * down to 0 after one week, account is still a premium account but worthless. Not even free downloads are possible with such
             * accounts!
             */
            String availableTraffic = br.getRegex(">Bandwith available</td>\\s*<td>\\s*([^<>\"]*?) of").getMatch(0);
            final String availableTrafficMax = br.getRegex(">Bandwith available</td>\\s*<td>\\s*[^<>\"]*? of (\\d+(\\.\\d+)? (?:MB|GB|TB))").getMatch(0);
            logger.info("availableTraffic = " + availableTraffic);
            if (availableTraffic != null) {
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
                 * Eg subscriptions
                 */
                getPage("/Payment/Payment");
                expireDate = br.getRegex("\\d+\\s*</td>\\s*<td style=\"width.*?>(\\d{4}-\\d{2}-\\d{2})\\s*<").getMatch(0);
            }
            if (expireDate == null) {
                logger.warning("Could not find expire date!");
            } else {
                /*
                 * 2019-12-18: Rapidgator accounts do have precise expire timestamps but we can only get them via API, see
                 * fetchAccountInfo_api. In website mode we set it like this to make sure that the user can use his account the whole last
                 * day no matter which exact time of the day it expires.
                 */
                expireDate += " 23:59:59";
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
            }
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    /**
     * @param validateCookies
     *            true = Check whether stored cookies are still valid, if not, perform full login <br/>
     *            false = Set stored cookies and trust them if they're not older than 300000l
     *
     */
    private boolean login_web(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            /* Keep followRedirects information */
            final boolean ifr = br.isFollowingRedirects();
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * Make sure that we're logged in. Doing this for every downloadlink might sound like a waste of server capacity but
                     * really it doesn't hurt anybody.
                     */
                    br.setCookies(getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !validateCookies) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust login-cookies without checking as they should still be fresh");
                        return false;
                    }
                    /* Even if login is forced, use cookies and check if they are still valid to avoid the captcha below */
                    br.setFollowRedirects(true);
                    accessMainpage(br);
                    if (isLoggedINWebsite()) {
                        logger.info("Successfully validated last session");
                        if (sessionReUseAllowed(account, PROPERTY_timestamp_session_create_website, WEBSITE_SESSION_ID_REFRESH_TIMEOUT_MINUTES)) {
                            setAccountTypeWebsite(account, br);
                            account.saveCookies(br.getCookies(getHost()), "");
                            return true;
                        }
                    }
                }
                br = new Browser();
                br.setFollowRedirects(true);
                accessMainpage(br);
                for (int i = 1; i <= 3; i++) {
                    logger.info("Website login attempt " + i + " of 3");
                    getPage("https://" + this.getHost() + "/auth/login");
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
                    if (isLoggedINWebsite()) {
                        continue;
                    }
                    break;
                }
                if (!isLoggedINWebsite()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                setAccountTypeWebsite(account, br);
                account.saveCookies(br.getCookies(getHost()), "");
                account.setProperty(PROPERTY_timestamp_session_create_website, System.currentTimeMillis());
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.setType(null);
                    account.setProperty("cookies", Property.NULL);
                }
                throw e;
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
    }

    /**
     * Returns whether or not session is allowed to be re-used regardless of whether it is valid or not --> Only based on the max. time we
     * are using a session. Only call this if you have validated the session before and are sure that the current session is valid!!
     */
    private boolean sessionReUseAllowed(final Account account, final String session_create_property, final long session_refresh_timeout) {
        if (session_refresh_timeout > 0) {
            logger.info(String.format("Currently sessions are re-freshed every %d minutes", session_refresh_timeout));
        }
        final long timestamp_session_validity = account.getLongProperty(session_create_property, 0) + session_refresh_timeout * 60 * 1000l;
        if (session_refresh_timeout > 0 && System.currentTimeMillis() > timestamp_session_validity) {
            /*
             * 2019-12-23: We could avoid checking sessions as we know their age before already but I currently want all session_ids to get
             * checked to get better log results/find serverside issues.
             */
            logger.info(String.format("session seems to be valid but we'll get a new one as current session is older than %d minutes", session_refresh_timeout));
            return false;
        } else {
            if (session_refresh_timeout > 0) {
                final long timestamp_remaining_session_validity = timestamp_session_validity - System.currentTimeMillis();
                logger.info("Unless it expires serverside, current session is internally considered valid for: " + TimeFormatter.formatMilliSeconds(timestamp_remaining_session_validity, 0));
            }
            logger.info("Re-using last session");
            return true;
        }
    }

    private boolean isLoggedINWebsite() {
        return br.getCookie(RapidGatorNet.MAINPAGE, "user__", Cookies.NOTDELETEDPATTERN) != null;
    }

    private void setAccountTypeWebsite(final Account account, final Browser br) {
        if (br.containsHTML("Account\\s*:\\&nbsp;<a href=\"/article/premium\">Free</a>")) {
            account.setType(AccountType.FREE);
        } else {
            account.setType(AccountType.PREMIUM);
        }
    }

    private String login_api(final Account account, boolean isDownloadMode) throws Exception {
        synchronized (account) {
            final long lastPleaseWait = account.getLongProperty("lastPleaseWait", -1);
            final long pleaseWait = lastPleaseWait > 0 ? ((5 * 60 * 1000l) - (System.currentTimeMillis() - lastPleaseWait)) : 0;
            if (pleaseWait > 5000) {
                throw new AccountUnavailableException("Frequest logins. Please wait!", pleaseWait);
            }
            /* Before this was called 'avoidBlock' but it is not required anymore (in API mode)! */
            // accessMainpage(br);
            String session_id = account.getStringProperty(PROPERTY_sessionid);
            if (session_id != null) {
                logger.info("session_create = " + account.getLongProperty(PROPERTY_timestamp_session_create_api, 0));
                /* First try to re-use last token */
                getPage(API_BASEv2 + "user/info?token=" + Encoding.urlEncode(session_id));
                try {
                    handleErrors_api(null, null, account, br.getHttpConnection());
                    logger.info("Successfully validated last session");
                    if (sessionReUseAllowed(account, PROPERTY_timestamp_session_create_api, API_SESSION_ID_REFRESH_TIMEOUT_MINUTES)) {
                        account.setProperty("session_last_checked", System.currentTimeMillis());
                        return session_id;
                    }
                } catch (final PluginException e) {
                    logger.info("Failed to re-use last session_id");
                    logger.log(e);
                }
            }
            /* Avoid full logins - RG will temp. block accounts on too many full logins in a short time! */
            logger.info("Performing full login");
            /* Remove cookies from possible previous attempt to re-use old session_id! */
            br.clearCookies(this.getHost());
            getPage(API_BASEv2 + "user/login?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            /* 2019-12-14: session_id == PHPSESSID cookie */
            session_id = PluginJSonUtils.getJsonValue(br, "session_id");
            if (StringUtils.isEmpty(session_id)) {
                /* 2019-12-14: APIv2 */
                session_id = PluginJSonUtils.getJsonValue(br, "token");
            }
            if (StringUtils.isEmpty(session_id)) {
                logger.info("Failed to find session_id");
                handleErrors_api(null, null, account, br.getHttpConnection());
                logger.warning("Unknown login failure");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            /* Store session_id */
            account.setProperty(PROPERTY_sessionid, session_id);
            account.setProperty(PROPERTY_timestamp_session_create_api, System.currentTimeMillis());
            return session_id;
        }
    }

    private void accessMainpage(Browser br) throws Exception {
        getPage(br, "https://rapidgator.net/");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        correctDownloadLink(link);
        hotLinkURL = null;
        /*
         * 2019-12-17: Their traffic calculation seems to work really good. No need to save- and re-use directurls in order to
         * "save traffic".
         */
        if (PluginJsonConfig.get(RapidGatorConfig.class).isEnableAPIPremium()) {
            /* API */
            if (link.getBooleanProperty(HOTLINK, false)) {
                /* Check availablestatus via website if we have a hotlink */
                requestFileInformation(link);
            } else if (!API_TRUST_404_FILE_OFFLINE) {
                /* Check availablestatus via website if API cannot be fully trusted! */
                requestFileInformation(link);
            }
            if (hotLinkURL != null) {
                doFree(link, account);
            } else {
                handlePremium_api(link, account);
            }
        } else {
            /* Website */
            requestFileInformation(link);
            if (hotLinkURL != null) {
                doFree(link, account);
            } else {
                handlePremium_web(link, account);
            }
        }
    }

    public static String readErrorStream(final URLConnectionAdapter con) throws UnsupportedEncodingException, IOException {
        if (con == null) {
            return null;
        }
        if (con.getRequest() != null && con.getRequest().getHtmlCode() != null) {
            return con.getRequest().getHtmlCode();
        } else if (con.getRequest() != null && !con.getRequest().isRequested()) {
            throw new IOException("Request not sent yet!");
        } else if (!con.isConnected()) {
            // getInputStream/getErrorStream call connect!
            throw new IOException("Connection is not connected!");
        }
        con.setAllowedResponseCodes(new int[] { con.getResponseCode() });
        try {
            final InputStream es = con.getErrorStream();
            if (es == null) {
                throw new IOException("No errorstream!");
            }
            final BufferedReader f = new BufferedReader(new InputStreamReader(es, "UTF8"));
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
            con.disconnect();
        }
    }

    private void handleErrors_api(final String session_id, final DownloadLink link, final Account account, final URLConnectionAdapter con) throws Exception {
        if (con == null) {
            return;
        }
        /* Handle bare responsecodes first, then API */
        if (con.getResponseCode() == 401) {
            /* Invalid logindata */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (con.getResponseCode() == 404) {
            handle404API(link, account);
        } else if (con.getResponseCode() == 416) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
        } else if (con.getResponseCode() == 423) {
            // HTTP/1.1 423 Locked
            // {"response":null,"response_status":423,"response_details":"Error: Exceeded traffic"}
            // Hotlink?!
            /* 2019-12-16: {"response":null,"status":423,"details":"Error: Exceeded traffic"} --> See code below! */
        } else if (con.getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 60 * 60 * 1000l);
        } else if (con.getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503: Service Temporarily Unavailable", 5 * 60 * 1000l);
        }
        synchronized (account) {
            final String lang = System.getProperty("user.language");
            String errorMessage = RapidGatorNet.readErrorStream(con);
            final String statusString = new Regex(errorMessage, "status\"\\s*:\\s*\"?(\\d+)").getMatch(0);
            final long status = statusString != null ? Long.parseLong(statusString) : -1;
            if (errorMessage == null) {
                /* 2019-12-17: This String is not allowed to be null! */
                errorMessage = "None";
            }
            logger.info("ErrorMessage: " + errorMessage);
            if (link != null && (status == 423 || errorMessage.contains("Exceeded traffic"))) {
                /* 2019-12-16: {"response":null,"status":423,"details":"Error: Exceeded traffic"} */
                final AccountInfo ac = new AccountInfo();
                ac.setTrafficLeft(0);
                account.setAccountInfo(ac);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            final boolean sessionReset = session_id != null && session_id.equals(account.getStringProperty(PROPERTY_sessionid));
            if (errorMessage.contains("Denied by IP")) {
                throw new AccountUnavailableException("Denied by IP", 2 * 60 * 60 * 1000l);
            } else if (errorMessage.contains("Please wait")) {
                account.setProperty("lastPleaseWait", System.currentTimeMillis());
                if (link == null) {
                    /* we are inside fetchAccountInfo */
                    throw new AccountUnavailableException("Frequent logins. Please wait", 5 * 60 * 1000l);
                } else {
                    /* we are inside handlePremium */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server says: 'Please wait ...'", 10 * 60 * 1000l);
                }
            } else if (errorMessage.contains("User is not PREMIUM") || errorMessage.contains("This file can be downloaded by premium only") || errorMessage.contains("You can download files up to")) {
                if (sessionReset) {
                    logger.info("SessionReset:" + sessionReset);
                    account.setProperty(PROPERTY_sessionid, Property.NULL);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (errorMessage.contains("Login or password is wrong") || errorMessage.contains("Error: Error e-mail or password")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (errorMessage.contains("Password cannot be blank")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
                 * Unusual case but this may also happen frequently if users use strange chars as usernme/password so simply treat this as
                 * "login/password wrong"!
                 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (status == 401 && StringUtils.containsIgnoreCase(errorMessage, "Wrong e-mail or password")) {
                /* 2019-12-14: {"response":null,"response_status":401,"response_details":"Error: Wrong e-mail or password."} */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (status == 401 || StringUtils.containsIgnoreCase(errorMessage, "Session not exist") || StringUtils.containsIgnoreCase(errorMessage, "Session doesn't exist")) {
                // {"response":null,"status":401,"details":"Error. Session doesn't exist"}
                // {"response":null,"status":401,"details":"Error. Session not exist"}
                handleInvalidSession(link, account, null);
            } else if (status == 404) {
                handle404API(link, account);
            } else if (status == 500) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "API error 500", 5 * 60 * 1000l);
            } else if (StringUtils.containsIgnoreCase(errorMessage, "This download session is not for you") || StringUtils.containsIgnoreCase(errorMessage, "Session not found")) {
                handleInvalidSession(link, account, null);
            } else if (errorMessage.contains("\"Error: Error e-mail or password")) {
                /* Usually comes with response_status 401 --> Not exactly sure what it means but probably some kind of account issue. */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (errorMessage.contains("Error: You requested login to your account from unusual Ip address")) {
                /* User needs to confirm his current IP. */
                String statusMessage;
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nBitte bestätige deine aktuelle IP Adresse über den Bestätigungslink per E-Mail um den Account wieder nutzen zu können.";
                } else {
                    statusMessage = "\r\nPlease confirm your current IP adress via the activation link you got per mail to continue using this account.";
                }
                throw new AccountUnavailableException(statusMessage, 1 * 60 * 1000l);
            }
            /*
             * Unknown error?! TODO: Throw exception here once Rapidgator plugin runs better with APIv2 and all other glitches have been
             * taken care of!
             */
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @SuppressWarnings("deprecation")
    public void handlePremium_api(final DownloadLink link, final Account account) throws Exception {
        String session_id = null;
        final boolean isPremium;
        synchronized (account) {
            session_id = login_api(account, true);
            isPremium = Account.AccountType.PREMIUM.equals(account.getType());
        }
        if (!isPremium) {
            /* Free Account --> Only website possible (not API) */
            handleFree(link);
            return;
        }
        if (session_id == null) {
            /* This should never happen */
            logger.warning("session_id is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2019-12-16: Disabled API availablecheck for now as it is unreliable/returns wrong information! */
        if (false) {
            String fileName = link.getFinalFileName();
            if (fileName == null) {
                /* 2019-12-16: TODO: This call seems to be broken as it either returns 404 or 401. */
                /* 'old' request: apiURL + "v2/file/info?sid=" + session_id + "&url=" + Encoding.urlEncode(link.getDownloadURL()) */
                // this.getPage(API_BASEv2 + "file/info?token=" + session_id + "&url=" +
                // Encoding.urlEncode(link.getPluginPatternMatcher()));
                // this.getPage(API_BASEv2 + "file/info?sid=" + session_id + "&url=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
                /* No final filename yet? Do linkcheck! */
                /* Check via API */
                this.getPage(API_BASEv2 + "file/info?token=" + session_id + "&file_id=" + Encoding.urlEncode(this.getFID(link)));
                /* Error-Response maybe wrong - do not check for errors here! */
                // handleErrors_api(session_id, true, link, account, br.getHttpConnection());
                fileName = PluginJSonUtils.getJsonValue(br, "filename");
                if (StringUtils.isEmpty(fileName)) {
                    /* 2019-12-14: APIv2 */
                    fileName = PluginJSonUtils.getJsonValue(br, "name");
                }
                final String fileSize = PluginJSonUtils.getJsonValue(br, "size");
                final String fileHash = PluginJSonUtils.getJsonValue(br, "hash");
                if (fileName != null) {
                    link.setFinalFileName(fileName);
                }
                if (fileSize != null) {
                    final long size = Long.parseLong(fileSize);
                    link.setVerifiedFileSize(size);
                }
                if (fileHash != null) {
                    link.setMD5Hash(fileHash);
                }
            }
        }
        this.getPage(API_BASEv2 + "file/download?token=" + session_id + "&file_id=" + Encoding.urlEncode(this.getFID(link)));
        handleErrors_api(session_id, link, account, br.getHttpConnection());
        String url = PluginJSonUtils.getJsonValue(br, "url");
        if (StringUtils.isEmpty(url)) {
            /* 2019-12-14: APIv2 */
            url = PluginJSonUtils.getJsonValue(br, "download_url");
        }
        if (StringUtils.isEmpty(url)) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (PluginJsonConfig.get(RapidGatorConfig.class).isExperimentalEnforceSSL()) {
            url = url.replaceFirst("^http://", "https://");
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, true, getMaxChunks(account));
        dl.setFilenameFix(FIX_FILENAMES);
        if (!dl.getConnection().isOK() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
            logger.warning("The final dllink seems not to be a file!");
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            handleErrors_api(session_id, link, account, dl.getConnection());
            // so we can see errors maybe proxy errors etc.
            /* Try that errorhandling but it might not help! */
            handleErrors_api(session_id, link, account, br.getHttpConnection());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            dl.startDownload();
        } finally {
            synchronized (INVALIDSESSIONMAP) {
                final WeakHashMap<Account, String> map = INVALIDSESSIONMAP.get(link);
                if (map != null) {
                    map.remove(account);
                    if (map.size() == 0) {
                        INVALIDSESSIONMAP.remove(link);
                    }
                }
            }
        }
    }

    /** Workaround for serverside issue that API may returns error 404 instead of the real status if current session_id is invalid. */
    private void handle404API(final DownloadLink link, final Account account) throws Exception {
        logger.info("Error 404 happened --> Trying to find out whether session is invalid or file is offline");
        if (API_TRUST_404_FILE_OFFLINE) {
            /* File offline */
            logger.info("Error 404 --> Trusted file offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            /* Either bad session_id or file offline */
            /*
             * 2019-12-18: Seems like our session validity check still does not work which will lead to false positive 'file not found'
             * errors --> Avoid this and retry later instead! Proof: jdlog://6540330900751/
             */
            final boolean trust_session_validity_check = false;
            if (trust_session_validity_check) {
                logger.info("Checking for invalid session or 404 file not found");
                final boolean session_valid = validateSessionAPI(account);
                if (session_valid) {
                    /* Trust previous error --> File is offline */
                    logger.info("Session is valid --> File is offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    /* Get new session_id on next accountcheck */
                    logger.info("Session is invalid");
                    handleInvalidSession(link, account, null);
                }
            } else {
                /*
                 * Session validity check cannot be trusted either --> Check if URL is really offline; if yes, display offline; temp disable
                 * account and wait for new session
                 */
                if (this.getDownloadLink() != null) {
                    try {
                        requestFileInformation(this.getDownloadLink());
                        logger.info("File is online --> Probably expired session");
                    } catch (final InterruptedException e) {
                        throw e;
                    } catch (final Throwable e) {
                        if (e instanceof PluginException) {
                            final PluginException ep = (PluginException) e;
                            switch (ep.getLinkStatus()) {
                            case LinkStatus.ERROR_FILE_NOT_FOUND:
                            case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
                            case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                            case LinkStatus.ERROR_PLUGIN_DEFECT:
                                throw ep;
                            default:
                                /* Ignore other errors */
                                logger.log(e);
                                break;
                            }
                        } else {
                            logger.log(e);
                        }
                    }
                } else {
                    logger.info("Error 404 happened outside download handling which is unusual --> Probably expired session");
                }
                /* Probably expired session */
                handleInvalidSession(link, account, "404");
            }
        }
    }

    private boolean validateSessionAPI(final Account account) throws Exception {
        synchronized (account) {
            final String session_id = account.getStringProperty(PROPERTY_sessionid, null);
            if (session_id == null) {
                logger.severe("no session available?!");
                /* This should never happen */
                return false;
            }
            /*
             * 2019-12-16: Check running remote uploads to validate session as there is no extra API call available for verifying sessions
             * --> This should return the following for most users: {"response":[],"status":200,"details":null}
             */
            this.getPage(this.API_BASEv2 + "remote/info?token=" + session_id);
            // this.getPage(this.API_BASEv2 + "trashcan/content?token=" + session_id);
            final String status = PluginJSonUtils.getJson(br, "status");
            if ("200".equals(status)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static WeakHashMap<DownloadLink, WeakHashMap<Account, String>> INVALIDSESSIONMAP = new WeakHashMap<DownloadLink, WeakHashMap<Account, String>>();

    /** Call this on expired session_id! */
    private void handleInvalidSession(final DownloadLink link, final Account account, final String error_hint) throws PluginException {
        /*
         * TODO: Consider deleting current session_id to enforce creation of a new session_id.b Keep in mind that frequently creating new
         * session_ids is bad!
         */
        final String session_id;
        if (account != null) {
            synchronized (account) {
                session_id = account.getStringProperty(PROPERTY_sessionid);
            }
        } else {
            session_id = null;
        }
        synchronized (INVALIDSESSIONMAP) {
            if (link != null && account != null) {
                WeakHashMap<Account, String> map = INVALIDSESSIONMAP.get(link);
                if (map == null) {
                    map = new WeakHashMap<Account, String>();
                    map.put(account, session_id);
                    INVALIDSESSIONMAP.put(link, map);
                    // throw AccountUnavailableException
                } else if (!map.containsKey(account) || !StringUtils.equals(map.get(account), session_id)) {
                    map.put(account, session_id);
                    // throw AccountUnavailableException
                } else {
                    /* We've retried with new session but same error --> Problem is not the session but the file */
                    map.remove(account);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File seems to be temporarily not available, please try again later", 30 * 60 * 1000l);
                }
            }
            /* We should not have to reset the session_id property here as it should happen automatically on next accountcheck! */
            final long waittime = 1 * 60 * 1000l;
            if (error_hint != null) {
                throw new AccountUnavailableException(String.format("[%s]Session expired - waiting before opening new session", error_hint), waittime);
            } else {
                throw new AccountUnavailableException("Session expired - waiting before opening new session", waittime);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void handlePremium_web(final DownloadLink link, final Account account) throws Exception {
        logger.info("Performing cached login sequence!!");
        boolean validated_cookies = login_web(account, false);
        boolean logged_in = false;
        final int repeat = 2;
        for (int i = 0; i <= repeat; i++) {
            br.setFollowRedirects(false);
            getPage(br, link.getPluginPatternMatcher());
            logged_in = this.isLoggedINWebsite();
            if (!logged_in && !validated_cookies && i + 1 != repeat) {
                // lets login fully again, as hoster as removed premium cookie for some unknown reason...
                logger.info("Performing login sequence with cookie validation");
                br = new Browser();
                validated_cookies = login_web(account, true);
                continue;
            } else {
                break;
            }
        }
        if (!logged_in) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown login failure");
        }
        if (Account.AccountType.FREE.equals(account.getType())) {
            doFree(link, account);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                dllink = br.getRegex("var premium_download_link\\s*=\\s*'(https?://[^<>\"']+)';").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("'(https?://pr_srv\\.rapidgator\\.net//\\?r=download/index\\&session_id=[A-Za-z0-9]+)'").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("'(https?://pr\\d+\\.rapidgator\\.net//\\?r=download/index\\&session_id=[A-Za-z0-9]+)'").getMatch(0);
                    }
                }
            }
            if (dllink == null) {
                handleErrorsWebsite(this.br, link, account);
                /* Unknown failure */
                logger.warning("Could not find 'dllink'. Please report to JDownloader Development Team");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (PluginJsonConfig.get(RapidGatorConfig.class).isExperimentalEnforceSSL()) {
                dllink = dllink.replaceFirst("^http://", "https://");
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, Encoding.htmlDecode(dllink), true, getMaxChunks(account));
            dl.setFilenameFix(FIX_FILENAMES);
            if (!dl.getConnection().isOK() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
                logger.warning("The final dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                handleErrors_api(null, link, account, dl.getConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void handleErrorsWebsite(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        if (account != null) {
            /* Errors which should only happen in account mode */
            if (br.containsHTML("You have reached quota|You have reached daily quota of downloaded information for premium accounts")) {
                logger.info("You've reached daily download quota for " + account.getUser() + " account");
                final AccountInfo ac = new AccountInfo();
                ac.setTrafficLeft(0);
                account.setAccountInfo(ac);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.getCookie(RapidGatorNet.MAINPAGE, "user__") == null) {
                logger.info("Account seems to be invalid!");
                // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                account.setProperty("cookies", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (br.containsHTML("File is temporarily unavailable, please try again later\\. Maintenance in data center\\.")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is temporarily not available, please try again later", 15 * 60 * 1000l);
        } else if (br.containsHTML("File is temporarily not available, please try again later")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is temporarily not available, please try again later");
        } else if (br.containsHTML(">\\s*You have reached your hourly downloads limit\\.")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached your hourly downloads limit", FREE_RECONNECTWAIT_GENERAL);
        } else if (br.containsHTML("You can`t download not more than 1 file at a time in free mode\\.\\s*<|>\\s*Wish to remove the restrictions\\?")) {
            /*
             * 2020-03-11: Do not throw ERROR_IP_BLOCKED error here as this error will usually only show up for 30-60 seconds between
             * downloads or upon instant retry of an e.g. interrupted free download --> Reconnect is not required
             */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "You can't download more than one file within a certain time period in free mode", 10 * 60 * 1000l);
        } else if (br.containsHTML("Denied by IP") && false) {
            // disabled because I don't know the exact HTML of this error
            if (account != null) {
                throw new AccountUnavailableException("Denied by IP", 2 * 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Denied by IP", 2 * 60 * 60 * 1000l);
            }
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

    @Override
    public Class<RapidGatorConfig> getConfigInterface() {
        return RapidGatorConfig.class;
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