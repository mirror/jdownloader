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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.FlashfilesComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FlashfilesCom extends PluginForHost {
    public FlashfilesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://flash-files.com/signup.php");
    }

    @Override
    public String getAGBLink() {
        return "https://flash-files.com/tos.php";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "flash-files.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean FREE_RESUME                  = false;
    private final int     FREE_MAXCHUNKS               = 1;
    private final boolean ACCOUNT_FREE_RESUME          = false;
    private final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    /* 2020-09-21: Chunkload & resume impossible in premium mode (wtf?) */
    private final boolean ACCOUNT_PREMIUM_RESUME       = false;
    private final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Set fallback-filename */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        final String fid = this.getFID(link);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("role=\"tabpanel\"|name=\"hash\"")) {
            /* 2020-06-02: Not a file e.g.: https://flash-files.com/faq */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("(?i)>\\s*Filename\\s*:[^<]*<span[^>]*>([^<]+)</span>").getMatch(0);
        if (filename == null) {
            /* Old website */
            filename = br.getRegex("(?i)>\\s*FileName\\s*:\\s*(?:\\&nbsp)?([^<>\"]+)<").getMatch(0);
        }
        String filesize = br.getRegex("(?i)>\\s*FileSize\\s*:[^<]*<span[^>]*>([^<]+)</span>").getMatch(0);
        if (filesize == null) {
            /* Old website */
            filesize = br.getRegex("(?i)>\\s*FileSize\\s*:([^<>\"]+)<").getMatch(0);
        }
        if (!StringUtils.isEmpty(filename)) {
            // Content-Disposition header not always correct filename
            filename = Encoding.htmlDecode(filename).trim();
            link.setFinalFileName(filename);
        }
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleFreeDownloads(link, null, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleFreeDownloads(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        requestFileInformation(link);
        if (account != null) {
            login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        Form freeform = br.getFormbyActionRegex(".*?freedownload\\.php");
        if (freeform == null) {
            /* 2020-05-01: E.g. free.flash-files.com */
            /* 2020-06-02: freedownload.flash-files.com */
            freeform = br.getFormbyActionRegex(".+free(download)?\\..+");
        }
        if (freeform == null) {
            /* 2020-06-02 */
            final Form[] forms = br.getForms();
            for (final Form form : forms) {
                if (form.containsHTML("Free Download")) {
                    freeform = form;
                    break;
                }
            }
        }
        if (freeform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(false);
        final URLConnectionAdapter con = br.openFormConnection(freeform);
        if (con.getResponseCode() == 200 || con.getResponseCode() == 302) {
            /**
             * HTTP/1.1 302 Found
             *
             * Location: https://flash-files.com/ip-blocked.php
             */
            br.followConnection(true);
            final String waitSecondsStr = br.getRegex("counter\\s*=\\s*(\\d+);").getMatch(0);
            final int waitSeconds = Integer.parseInt(waitSecondsStr);
            if (waitSeconds > 600) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitSeconds * 1001l);
            }
        }
        if (con.getResponseCode() == 500) {
            br.followConnection(true);
            if (StringUtils.contains(br.getURL(), "/ip-blocked") || StringUtils.contains(br.getRedirectLocation(), "/ip-blocked")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1001l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 500", 10 * 60 * 1000l);
            }
        } else {
            br.followConnection();
        }
        br.setFollowRedirects(true);
        final String waitSecondsStr = br.getRegex("counter\\s*=\\s*(\\d+);").getMatch(0);
        final int waitSeconds = Integer.parseInt(waitSecondsStr);
        /* 2020-02-13: Normal waittime is 30 seconds, if waittime > 10 Minutes reconnect to get new IP to circumvent limit. */
        if (waitSeconds > 600) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitSeconds * 1001l);
        }
        // cat/mouse?
        Form downloadFileForm = br.getFormbyActionRegex(".*?download(file)?\\.php");
        if (downloadFileForm == null) {
            downloadFileForm = br.getFormbyActionRegex(".*?linkgenerate\\.php");
            if (downloadFileForm == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final long timeBefore = System.currentTimeMillis();
        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
        waitTime(link, timeBefore, waitSecondsStr);
        downloadFileForm.put("g-recaptcha-response", recaptchaV2Response);
        if (StringUtils.containsIgnoreCase(downloadFileForm.getAction(), "linkgenerate")) {
            br.submitForm(downloadFileForm);
            downloadFileForm = br.getFormbyActionRegex(".*?download(file)?\\.php");
            if (downloadFileForm == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadFileForm, resumable, maxchunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    /**
     * Handles pre download (pre-captcha) waittime.
     */
    protected void waitTime(final DownloadLink link, final long timeBefore, final String waitStr) throws PluginException {
        if (waitStr == null || !waitStr.matches("\\d+")) {
            return;
        }
        final int extraWaitSeconds = 1;
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
        logger.info("Found waittime, parsing waittime: " + waitStr);
        int wait = Integer.parseInt(waitStr);
        /*
         * Check how much time has passed during eventual captcha event before this function has been called and see how much time is left
         * to wait.
         */
        if (passedTime > 0) {
            /* This usually means that the user had to solve a captcha which cuts down the remaining time we have to wait. */
            logger.info("Total passed time during captcha: " + passedTime);
            wait -= passedTime;
        }
        if (wait > 0) {
            logger.info("Waiting final waittime: " + wait);
            sleep(wait * 1000l, link);
        } else if (wait < -extraWaitSeconds) {
            /* User needed more time to solve the captcha so there is no waittime left :) */
            logger.info("Congratulations: Time to solve captcha was higher than waittime --> No waittime left");
        } else {
            /* No waittime at all */
            logger.info("Found no waittime");
        }
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (!looksLikeDownloadableContent(con)) {
                    throw new IOException();
                } else {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return PluginJsonConfig.get(FlashfilesComConfig.class).getMaxSimultaneousFreeDownloads();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/login.php");
                final Form loginform = br.getFormbyProperty("name", "login");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (!isLoggedin(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("href\\s*=\\s*('|\")(logout\\.php|logout)\\1") || br.getURL().contains("/myfiles.php");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        /* Parts of this code are from 2018. Premium is untested! */
        if (br.getURL() == null || !br.getURL().contains("/myfiles.php")) {
            br.getPage("/myfiles.php");
        }
        String expire = br.getRegex("Expires On\\s*:[^<>\"]*(\\d{4}\\-\\d{2}\\-\\d{2}[^<>\"]*\\d{2}:\\d{2}:\\d{2})[^<>\"]*UTC").getMatch(0);
        if (expire != null) {
            if (Encoding.isHtmlEntityCoded(expire)) {
                expire = Encoding.htmlDecode(expire).trim();
            }
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", null));
        }
        if (expire == null || ai.isExpired()) {
            ai.setExpired(false);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setUnlimitedTraffic();
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            handleFreeDownloads(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            requestFileInformation(link);
            if (account != null) {
                login(account, false);
            }
            final String directurlproperty = "premium_directlink";
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (!attemptStoredDownloadurlDownload(link, directurlproperty, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS)) {
                final boolean useNewHandling = true;
                if (useNewHandling) {
                    /* 2021-11-08: New */
                    br.setFollowRedirects(true);
                    br.getPage(link.getPluginPatternMatcher());
                    final Form premiumDl = br.getFormbyProperty("id", "premiumdownload");
                    if (StringUtils.isEmpty(premiumDl.getAction())) {
                        premiumDl.setAction("/pre-down-file.php");
                    }
                    br.submitForm(premiumDl);
                    final String id = PluginJSonUtils.getJson(br, "filehash"); // == fid
                    final String downloadtoken = PluginJSonUtils.getJson(br, "downloadtoken");
                    if (StringUtils.isEmpty(id) || StringUtils.isEmpty(downloadtoken)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = "/pd.php?id=" + id + "&downloadtoken=" + downloadtoken;
                } else {
                    br.postPage("/alternate-download.php", "h=" + this.getFID(link));
                    final String id = PluginJSonUtils.getJson(br, "id");
                    final String downloadtoken = PluginJSonUtils.getJson(br, "downloadtoken");
                    if (StringUtils.isEmpty(id) || StringUtils.isEmpty(downloadtoken)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = id + "&downloadtoken=" + Encoding.urlEncode(downloadtoken);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
                if (!looksLikeDownloadableContent(dl.getConnection())) {
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection(true);
                    if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                link.setProperty(directurlproperty, dl.getConnection().getURL().toString());
            }
            dl.startDownload();
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directurlproperty, final boolean resumes, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumes, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            this.dl = null;
            link.removeProperty(directurlproperty);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        } else if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        } else {
            /* Premium accounts do not have captchas */
            return false;
        }
    }

    @Override
    public Class<? extends FlashfilesComConfig> getConfigInterface() {
        return FlashfilesComConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}