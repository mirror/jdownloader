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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livemixtapes.com" }, urls = { "https?://((?:\\w+\\.)?livemixtapes\\.com/download(?:/mp3)?/\\d+/[a-z0-9\\-]+\\.html|club\\.livemixtapes\\.com/play/\\d+)" })
public class LiveMixTapesCom extends antiDDoSForHost {
    private static final String               CAPTCHATEXT        = "/captcha/captcha\\.gif\\?";
    private static final String               TYPE_REDIRECTLINK  = "https?://(www\\.)?livemixtap\\.es/[a-z0-9]+";
    private static final String               TYPE_DIRECTLINK    = "https?://club\\.livemixtapes\\.com/play/\\d+";
    private static final String               TYPE_ALBUM         = "https?://(?:www\\.)?livemixtapes\\.com/download/\\d+.*?";
    protected static HashMap<String, Cookies> antiCaptchaCookies = new HashMap<String, Cookies>();

    public LiveMixTapesCom(PluginWrapper wrapper) {
        super(wrapper);
        // Currently there is only support for free accounts
        this.enablePremium("http://www.livemixtapes.com/signup.html");
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            final String type;
            if (link.getPluginPatternMatcher().matches(TYPE_DIRECTLINK)) {
                type = "direct";
            } else if (link.getPluginPatternMatcher().matches(TYPE_ALBUM)) {
                type = "download_album";
            } else {
                type = "download_single";
            }
            return this.getHost() + "://" + fid + type;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/(\\d+)(?:/[a-z0-9\\-]+\\.html)?$").getMatch(0);
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if ((browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            return prepBr;
        }
        loadAntiCaptchaCookies(prepBr, host);
        prepBr.getHeaders().put("Accept-Encoding", "gzip, deflate, br");
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36");
        return super.prepBrowser(prepBr, host);
    }

    protected void loadAntiCaptchaCookies(Browser prepBr, final String host) {
        synchronized (antiCaptchaCookies) {
            if (!antiCaptchaCookies.isEmpty()) {
                for (final Map.Entry<String, Cookies> cookieEntry : antiCaptchaCookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    if (key != null && key.equals(host)) {
                        try {
                            prepBr.setCookies(key, cookieEntry.getValue(), false);
                        } catch (final Throwable e) {
                        }
                    }
                }
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private boolean isAccountRequired() {
        return br.containsHTML("class=\"download-member-only\"");
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(TYPE_DIRECTLINK)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(link.getPluginPatternMatcher());
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    /* Check if final filename has been set in crawler before */
                    if (link.getFinalFileName() == null) {
                        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con).trim()));
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            getPage(link.getPluginPatternMatcher());
            if (isUserVerifyNeeded() && !isDownload) {
                logger.info("Cannot do linkcheck because of captcha");
                return AvailableStatus.UNCHECKABLE;
            }
            this.handleUserVerify();
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(>Not Found</|The page you requested could not be found\\.<|>This mixtape is no longer available for download.<)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String timeRemaining = br.getRegex("TimeRemaining\\s*=\\s*(\\d+);").getMatch(0);
            if (timeRemaining != null) {
                /* TODO */
                link.setName(Encoding.htmlDecode(br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0)));
                return AvailableStatus.TRUE;
            }
            String filename = null, filesize = null, tracknumber = null;
            /* 2020-04-22: Seems like tracknumber is wrong for single files. Website will always display "1. "! */
            final boolean addTrackNumberToFilename = false;
            final Regex fileInfo = br.getRegex("<div class=\"track-num\">\\s*(\\d+)\\.\\s*<span>([^<>\"]+)</span>");
            tracknumber = fileInfo.getMatch(0);
            filename = fileInfo.getMatch(1);
            filesize = br.getRegex("class=\"mt-track-size\">([^<>\"]+)</div>").getMatch(0);
            if (filename == null) {
                /* Fallback */
                filename = getFID(link);
            }
            filename = Encoding.htmlDecode(filename).trim();
            /* Add tracknumber to name if possible */
            if (tracknumber != null && addTrackNumberToFilename) {
                filename = tracknumber + ". " + filename;
            }
            if (!filename.endsWith(".mp3") && !filename.endsWith(".zip")) {
                if (link.getPluginPatternMatcher().matches(TYPE_ALBUM)) {
                    filename += ".zip";
                } else {
                    filename += ".mp3";
                }
            }
            /* Only set final filename if not e.g. set preeviously in crawler. */
            if (link.getFinalFileName() == null) {
                link.setFinalFileName(filename);
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    private void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        handleUserVerify();
        br.setFollowRedirects(false);
        String dllink = null;
        boolean resume;
        int maxChunks;
        if (link.getPluginPatternMatcher().matches(TYPE_DIRECTLINK)) {
            dllink = link.getPluginPatternMatcher();
            resume = true;
            maxChunks = 0;
        } else {
            /* 2020-04-22: Resume possible */
            resume = true;
            maxChunks = 1;
            if (isAccountRequired()) {
                if (account != null) {
                    /* Should never happen */
                    throw new AccountUnavailableException("Session expired?", 2 * 60 * 1000l);
                } else {
                    throw new AccountRequiredException();
                }
            }
            final String timeRemaining = br.getRegex("TimeRemaining = (\\d+);").getMatch(0);
            if (timeRemaining != null) {
                link.getLinkStatus().setStatusText("Not yet released, cannot download");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            String timestamp = br.getRegex("name=\"timestamp\" value=\"(\\d+)\"").getMatch(0);
            Form dlform = br.getFormbyProperty("id", "downloadform");
            if (dlform == null) {
                /* 2021-02-25: E.g. for single mp3 files */
                dlform = br.getFormbyProperty("id", "adfreedownload");
            }
            if (dlform == null) {
                logger.warning("Failed to find dlform");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dlform.hasInputFieldByName("timestamp")) {
                timestamp = dlform.getInputFieldByName("timestamp").getValue();
            }
            if (br.containsHTML("<img src=\"[^\"]*?/captcha/captcha\\.gif\\?\\d+")) {
                /* 2020-04-22: Browser will also check for a reCaptchaV2 (lol two captchas) but this seems to be skippable here. */
                String captcha = br.getRegex("(/captcha/captcha\\.gif\\?\\d+)").getMatch(0);
                String code = getCaptchaCode(captcha, link);
                if (captcha == null || code == null || code.equals("")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dlform.put("code", Encoding.urlEncode(code));
                // postPage(br, br.getURL(), "retries=0&timestamp=" + timestamp + "&code=" + code);
                this.submitForm(dlform);
                if (br.containsHTML("<img src=\"[^\"]*?/captcha/captcha\\.gif\\?\\d+")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            } else if (br.containsHTML("solvemedia\\.com/papi/")) {
                /* 2020-04-22: This does not exist anymore??! */
                if (timestamp == null) {
                    logger.warning("Failed to find timestamp");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String challengekey = br.getRegex("ACPuzzle\\.create\\(\\'(.*?)\\'").getMatch(0);
                if (challengekey == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                sm.setChallengeKey(challengekey);
                final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                final String code = getCaptchaCode("solvemedia", cf, link);
                final String chid = sm.getChallenge(code);
                // Usually we have a waittime here but it can be skipped
                // int waittime = 40;
                // String wait =
                // br.getRegex("<span id=\"counter\">(\\d+)</span>").getMatch(0);
                // if (wait == null) wait =
                // br.getRegex("wait: (\\d+)").getMatch(0);
                // if (wait != null) {
                // waittime = Integer.parseInt(wait);
                // if (waittime > 1000) waittime = waittime / 1000;
                // sleep(waittime * 1001, downloadLink);
                // }
                try {
                    postPage(br, br.getURL(), "retries=0&timestamp=" + timestamp + "&adcopy_response=manual_challenge&adcopy_challenge=" + chid);
                } catch (Exception e) {
                }
                if (br.containsHTML("solvemedia\\.com/papi/")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            } else if (dlform.containsHTML("g-recaptcha-response")) {
                /*
                 * 2021-02-26: TODO: Fix this! Why does it take us to the "download" page only without returning a downloadurl? I've failed
                 * to make this work...
                 */
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                dlform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                if (StringUtils.isEmpty(dlform.getAction())) {
                    dlform.setAction(br.getURL().replace("/mixtapes/", "/download/"));
                }
                this.submitForm(dlform);
                // final Form dlform2 = br.getFormbyProperty("id", "adfreedownload");
                // dlform2.setAction(br.getURL().replace("/mixtapes/", "/download/"));
                // final String recaptchaV2Response2 = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                // dlform2.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response2));
                // br.setFollowRedirects(false);
                // br.submitForm(dlform2);
            }
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            return ai;
        }
        ai.setUnlimitedTraffic();
        /* 2019-07-29: As far as I know there are no 'premium' accounts available! */
        ai.setStatus("Registered User");
        account.setType(AccountType.FREE);
        return ai;
    }

    private void handleUserVerify() throws Exception {
        if (isUserVerifyNeeded()) {
            /* Handle login-captcha if required */
            final DownloadLink dlinkbefore = this.getDownloadLink();
            final DownloadLink dl_dummy;
            if (dlinkbefore != null) {
                dl_dummy = dlinkbefore;
            } else {
                /* E.g. captcha happens during accountcheck and not regular download. */
                dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + br.getHost(), true);
                this.setDownloadLink(dl_dummy);
            }
            Form captchaForm = br.getFormByInputFieldPropertyKeyValue("submit", "Submit");
            if (captchaForm == null) {
                captchaForm = br.getForm(0);
            }
            if (captchaForm == null) {
                logger.warning("Failed to find captchaForm");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            if (dlinkbefore != null) {
                this.setDownloadLink(dlinkbefore);
            }
            captchaForm.put("g-recaptcha-response", recaptchaV2Response);
            br.submitForm(captchaForm);
        }
    }

    private boolean isUserVerifyNeeded() {
        return br.getURL().contains("verify-user.php");
    }

    @Override
    public String getAGBLink() {
        return "https://www.livemixtapes.com/contact.html";
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        doFree(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* First login, then availablecheck --> Avoids captchas in availablecheck! */
        login(account);
        requestFileInformation(link, true);
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        doFree(link, account);
    }

    public void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // br.getPage(MAINPAGE);
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            logger.info("Trying to login via cookies");
            br.setCookies(account.getHoster(), cookies);
            getPage(br, "https://www." + account.getHoster() + "/");
            /* 2020-04-22: Captcha may even happen when cookies are still valid. Untested! ... but better check than don't check ;) */
            handleUserVerify();
            if (isLoggedIn()) {
                logger.info("Cookie login successful");
                account.saveCookies(br.getCookies(br.getHost()), "");
                return;
            } else {
                logger.info("Cookie login failed");
            }
        }
        logger.info("Performing full login");
        getPage(br, "https://www." + account.getHoster() + "/");
        handleUserVerify();
        postPage(br, "/login.php", "remember=y&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (!isLoggedIn()) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        account.saveCookies(br.getCookies(br.getHost()), "");
    }

    private boolean isLoggedIn() {
        return br.getCookie(br.getHost(), "u", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(br.getHost(), "p", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        if (link.getPluginPatternMatcher().contains("/download/")) {
            return true;
        } else {
            return false;
        }
    }
}