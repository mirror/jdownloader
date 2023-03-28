//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RarbgTo extends PluginForHost {
    public RarbgTo(PluginWrapper wrapper) {
        super(wrapper);
        /* 2019-01-04: Try to avoid triggering their anti-spam measures! */
        this.setStartIntervall(1000l);
    }

    @Override
    public String getAGBLink() {
        return "https://rarbg.to/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "rarbg.to", "rarbg.com", "rarbgproxied.org", "rarbgproxy.org", "rarbgp2p.org", "proxyrarbg.org", "rarbgaccessed.org", "rarbgaccess.org", "rarbgget.org", "rarbggo.org", "rarbg.is", "rarbgmirror.com", "rarbgmirrored.org", "rarbgmirror.org", "rarbgprx.org", "rarbgto.org", "rarbgunblock.com", "rarbgway.org" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/torrent/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    /* Resume & chunkload is disabled as this website only contains small files. */
    private static final boolean              FREE_RESUME         = false;
    private static final int                  FREE_MAXCHUNKS      = 1;
    protected static HashMap<String, Cookies> antiCaptchaCookies  = new HashMap<String, Cookies>();
    private static final String               PROPERTY_DIRECTLINK = "directlink";
    /* Don't touch the following! */
    private static final AtomicInteger        freeRunning         = new AtomicInteger(0);

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        if (link != null && StringUtils.equals(getHost(), link.getHost())) {
            return link.getLinkID();
        } else {
            return super.getMirrorID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        loadAntiCaptchaCookies(this.br, this.getHostOfAddedURL(link));
        br.setFollowRedirects(true);
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".torrent");
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*No such torrent")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isThreadDefenceActive(this.br)) {
            /* Do not handle their anti bot protection during linkcheck as it may require the user to solve a captcha. */
            // this.handleThreadDefence(link, this.br);
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = br.getRegex("\\&f=([^<>\"]+)").getMatch(0);
        if (filename != null) {
            if (!filename.endsWith(".torrent")) {
                filename += ".torrent";
            }
            filename = Encoding.htmlDecode(filename).trim();
            link.setFinalFileName(filename);
        }
        /* Save this here so we can save one http request when downloading. */
        final String directurl = regexDllink(this.br);
        if (directurl != null) {
            link.setProperty(PROPERTY_DIRECTLINK, br.getURL(directurl).toString());
        }
        return AvailableStatus.TRUE;
    }

    protected void loadAntiCaptchaCookies(final Browser prepBr, final String targetHost) {
        synchronized (antiCaptchaCookies) {
            if (!antiCaptchaCookies.isEmpty()) {
                for (final Map.Entry<String, Cookies> cookieEntry : antiCaptchaCookies.entrySet()) {
                    final String cookieHost = cookieEntry.getKey();
                    if (cookieHost != null) {
                        try {
                            prepBr.setCookies(cookieHost, cookieEntry.getValue(), false);
                            /*
                             * Cookies are interchangable e.g. if challenge has been completed on "rarbg.to", cookies will also work for
                             * "rarbgproxy.org".
                             */
                            if (!cookieHost.equals(targetHost)) {
                                final List<Cookie> cookies = cookieEntry.getValue().getCookies();
                                for (final Cookie cookie : cookies) {
                                    prepBr.setCookie(targetHost, cookie.getKey(), cookie.getValue());
                                }
                            }
                        } catch (final Throwable e) {
                        }
                    }
                }
            }
        }
    }

    private boolean isThreadDefenceActive(final Browser br) {
        if (br.getURL().contains("threat_defence.php")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Handles websites' anti bot protection.
     *
     * @throws Exception
     */
    private void handleThreadDefence(final DownloadLink link, final Browser br) throws Exception {
        synchronized (antiCaptchaCookies) {
            int globalAttempts = 0;
            int captchaAttempt = 0;
            final int maxCaptchaAttempts = 3;
            while (isThreadDefenceActive(br) && globalAttempts <= maxCaptchaAttempts) {
                globalAttempts += 1;
                logger.info("Entering thread_defence handling round " + globalAttempts);
                int numberofCompletedChallengesInThisRun = 0;
                boolean executedFirstHandlingOnce = false;
                if (br.containsHTML("/threat_defence_ajax\\.php\\?sk=")) {
                    int attemptNumber = 0;
                    do {
                        attemptNumber += 1;
                        logger.info("First challenge attempt: " + 0);
                        final String sk = br.getRegex("var value_sk = '([a-z0-9]+)';").getMatch(0);
                        final String cid = br.getRegex("var value_c = '(\\d+)';").getMatch(0);
                        final String i = br.getRegex("var value_i = '(\\d+)';").getMatch(0);
                        final String r = br.getRegex("/threat_defence_ajax\\.php\\?sk=[^\"]+\\&r=(\\d+)'").getMatch(0);
                        /* This RegEx purposely always looks for "defence=2"! */
                        final String r2 = br.getRegex("/threat_defence\\.php\\?defence=2[^>]+\\&r=(\\d+)").getMatch(0);
                        /* Check if we found all required data and are allowed to try the current challenge by attemptNumber. */
                        if (sk == null || cid == null || i == null || r == null || r2 == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else if (!br.getURL().contains("defence=") && !br.containsHTML("defence=")) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (br.containsHTML(org.appwork.utils.Regex.escape("document.cookie = name+\"=\"+value_sk"))) {
                            final String oldSK = br.getCookie(br.getHost(), "sk", Cookies.NOTDELETEDPATTERN);
                            if (oldSK != null) {
                                /* E.g. on second run we'll get a new sk value while the old one is still there. */
                                logger.info("Old sk cookie: " + oldSK + " | New: " + sk);
                            } else {
                                logger.info("First sk cookie: " + sk);
                            }
                            br.setCookie(br.getHost(), "sk", sk);
                        }
                        final UrlQuery query = new UrlQuery();
                        query.add("sk", sk);
                        query.add("cid", cid);
                        query.add("i", i);
                        query.add("r", r);
                        query.add("_", Long.toString(System.currentTimeMillis()));
                        /* This will return an empty page */
                        final Browser brc = br.cloneBrowser();
                        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        brc.getPage("/threat_defence_ajax.php?" + query.toString());
                        /* Mimic browser wait. This cannot be skipped!! */
                        final String waitMillisStr = br.getRegex("window\\.location\\.href =.*?,\\s*(\\d{4})\\);").getMatch(0);
                        if (waitMillisStr != null) {
                            this.sleep(Long.parseLong(waitMillisStr), link);
                        } else {
                            /* This should not happen but we'll allow it to. */
                            logger.warning("Failed to parse waittime from HTML --> Using static fallback value");
                            this.sleep(5500l, link);
                        }
                        /* This request will either complete the challenge or ask for a captcha. */
                        br.getPage("/threat_defence.php?defence=2&sk=" + sk + "&cid=" + cid + "&i=" + i + "&ref_cookie=rarbg.to&r=" + r2);
                        /**
                         * The following may happen on first attempt if no cookies are present at all: </br> "<b>There is something wrong
                         * with your browser!</b><br/>
                         * Most likely you dont have javascript or cookies enabled<br/>
                         * <a href="/threat_defence.php?defence=1">Click here</a> to retry verifying your browser"
                         */
                        final String failureURL = br.getRegex("(?i)<a href=\"(/threat_defence\\.php\\?defence=1)\">Click here</a> to retry verifying your browser").getMatch(0);
                        if (failureURL != null) {
                            if (executedFirstHandlingOnce) {
                                /* This is supposed to happen only once per global run! */
                                logger.warning("Failure of first run re-occured");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            } else {
                                logger.info("Retry challenge via: " + failureURL);
                                /* This will redirect back to "/...defence=1" */
                                br.getPage(failureURL);
                                executedFirstHandlingOnce = true;
                                continue;
                            }
                        } else {
                            /* Assume that we've solved the challenge successfully */
                            logger.info("Successfully solved first challenge on attemptNumber: " + attemptNumber);
                            numberofCompletedChallengesInThisRun += 1;
                            break;
                        }
                    } while (true);
                }
                final Form captchaForm = getCaptchaForm(br);
                if (captchaForm != null) {
                    captchaAttempt += 1;
                    logger.info("Second challenge (captcha) attempt: " + captchaAttempt);
                    final String captchaURL = br.getRegex("(/threat_captcha\\.php\\?[^<>\"]+)").getMatch(0);
                    if (captchaURL == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* Download captcha because URL is only valid once but we may want to ask the user multiple times. */
                    final File captchaImage = downloadCaptcha(captchaURL, getLocalCaptchaFile());
                    /* Request captcha answer of user and verify input. */
                    String code;
                    while (true) {
                        /* Ask used an unlimited amount of times if he enters a wrong captcha-answer-format. */
                        code = this.getCaptchaCode(captchaImage, link);
                        if (this.isAbort()) {
                            throw new InterruptedException();
                        } else if (code == null || !code.matches("(?i)[a-z0-9]{5}")) {
                            logger.info("Invalid captcha format");
                            continue;
                        } else {
                            break;
                        }
                    }
                    captchaForm.put("solve_string", code.toUpperCase(Locale.ENGLISH));
                    /* Typically this will redirect us to: /torrents\\.php\\?r=\\d+ */
                    br.submitForm(captchaForm);
                    if (br.containsHTML("(?i)>\\s*Wrong captcha entered")) {
                        br.setFollowRedirects(true);
                        if (captchaAttempt >= maxCaptchaAttempts) {
                            /* Too many wrong captcha attempts */
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        } else {
                            logger.info("Wrong captcha --> Retry");
                            /* Usually "/threat_defence.php" */
                            final Regex captchaRetryInfo = br.getRegex("window\\.location\\.href\\s*=\\s*\"(/[^\"]+)\";\\s*\\},\\s*(\\d+)\\);");
                            final String url = captchaRetryInfo.getMatch(0);
                            final String retryWaitMillisStr = captchaRetryInfo.getMatch(1);
                            if (url == null || captchaRetryInfo == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            this.sleep(Long.parseLong(retryWaitMillisStr), link);
                            br.getPage(url);
                            continue;
                        }
                    } else {
                        logger.info("Successfully passed captcha challenge");
                        /* Check one last time just to make sure we did it! */
                        if (isThreadDefenceActive(br)) {
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Anti bot protection captcha handling failed after captcha");
                        } else {
                            numberofCompletedChallengesInThisRun += 1;
                        }
                    }
                }
                logger.info("Number of solved challenges: " + numberofCompletedChallengesInThisRun);
                if (numberofCompletedChallengesInThisRun == 0) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Anti bot protection handling failure");
                } else if (isThreadDefenceActive(br)) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Failed to pass bot protection", 30 * 60 * 1000l);
                } else {
                    logger.info("Successfully passed bot protection --> URL: " + br.toString());
                    /*
                     * Their anti bot handling is not tracking the URL we're coming from thus it won't redirect us to where we initially
                     * wanted to go --> Fix that
                     */
                    if (!br.getURL().contains(this.getFID(link)) && !this.canHandle(br.getURL())) {
                        logger.info("Accessing contentURL again because anti bot handling redirected us somewhere else instead: " + br.getURL());
                        br.getPage(link.getPluginPatternMatcher());
                        /* One final check */
                        if (isThreadDefenceActive(br)) {
                            /* This should absolutely never happen! */
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Anti bot protection captcha handling failed after final step");
                        }
                    }
                    /* Save new cookies to prevent future anti bot challenges */
                    antiCaptchaCookies.put(br.getHost(), br.getCookies(br.getHost()));
                    break;
                }
            }
        }
    }

    private File downloadCaptcha(final String captchaURL, final File captchaFile) throws Exception {
        URLConnectionAdapter con = null;
        final Browser brc = this.br.cloneBrowser();
        try {
            Browser.download(captchaFile, con = brc.openGetConnection(captchaURL));
        } catch (IOException e) {
            captchaFile.delete();
            throw e;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable ignore) {
            }
        }
        return captchaFile;
    }

    private Form getCaptchaForm(final Browser br) {
        return br.getFormbyActionRegex(".*/threat_defence\\.php");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        /* We are not able to re-use directurls without valid cookies! */
        loadAntiCaptchaCookies(this.br, this.getHostOfAddedURL(link));
        if (!attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link, true);
            this.handleThreadDefence(link, this.br);
            final String dllink = regexDllink(this.br);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else
                    /* 2021-08-31: Max. 100 torrents per IP per 24 hours. */
                    if (br.containsHTML("Your ip .* downloaded over \\d+ torrents in the past 24 hours")) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
            }
            link.setProperty(PROPERTY_DIRECTLINK, dl.getConnection().getURL().toString());
        }
        dl.setFilenameFix(true);
        /*
         * Start free downloads sequentially to prevent the need to solve antiBot captchas and also the possibility of multiple antiBot
         * captchas appearing at the same time.
         */
        try {
            /* Add a download slot */
            controlMaxFreeDownloads(null, link, +1);
            /* Start download */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxFreeDownloads(null, link, -1);
        }
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    private String getHostOfAddedURL(final DownloadLink link) {
        return Browser.getHost(link.getPluginPatternMatcher());
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTLINK);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, FREE_RESUME, FREE_MAXCHUNKS);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                /* Typically response 403 --> No anti bot cookies present or existing cookies expired. */
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private String regexDllink(final Browser br) {
        return br.getRegex("(/download\\.php[^<>\"]+)").getMatch(0);
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* Anti-bot captcha is required once per session. */
        return true;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return freeRunning.get() + 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}