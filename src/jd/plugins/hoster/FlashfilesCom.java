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
    private final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean ACCOUNT_FREE_RESUME          = false;
    private final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    /* 2020-09-21: Chunkload & resume impossible in premium mode (wtf?) */
    private final boolean ACCOUNT_PREMIUM_RESUME       = false;
    private final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

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
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        final String fid = this.getFID(link);
        if (this.br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("role=\"tabpanel\"|name=\"hash\"")) {
            /* 2020-06-02: Not a file e.g.: https://flash-files.com/faq */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">FileName\\s*:\\s*(?:\\&nbsp)?([^<>\"]+)<").getMatch(0);
        String filesize = br.getRegex(">FileSize\\s*:([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = this.getFID(link);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
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
            logger.warning("Failed to find freeform1");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(freeform);
        final Form freeform2 = br.getFormbyActionRegex(".*?linkgenerate\\.php");
        if (freeform2 == null) {
            logger.warning("Failed to find freeform2");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String waitStr = br.getRegex("counter\\s*=\\s*(\\d+);").getMatch(0);
        int waittime = 30;
        if (waitStr != null) {
            logger.info("Found pre-download-waittime: " + waitStr);
            waittime = Integer.parseInt(waitStr);
        } else {
            logger.warning("Failed to find pre-download-waittime, using default: " + waittime);
        }
        /* 2020-02-13: Normal waittime is 30 seconds, if waittime > 10 Minutes reconnect */
        if (waittime > 600) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime * 1001l);
        }
        final long timeBefore = System.currentTimeMillis();
        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
        waitTime(link, timeBefore, waitStr);
        freeform2.put("g-recaptcha-response", recaptchaV2Response);
        br.submitForm(freeform2);
        final Form freeform3 = br.getFormbyActionRegex(".*?downloadfile\\.php");
        if (freeform3 == null) {
            logger.warning("Failed to find freeform3");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, freeform3, resumable, maxchunks);
        if (!isDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        // link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    /**
     * Handles pre download (pre-captcha) waittime.
     */
    protected void waitTime(final DownloadLink downloadLink, final long timeBefore, final String waitStr) throws PluginException {
        final int extraWaitSeconds = 1;
        int wait;
        if (waitStr != null && waitStr.matches("\\d+")) {
            int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
            logger.info("Found waittime, parsing waittime: " + waitStr);
            wait = Integer.parseInt(waitStr);
            /*
             * Check how much time has passed during eventual captcha event before this function has been called and see how much time is
             * left to wait.
             */
            wait -= passedTime;
            if (passedTime > 0) {
                /* This usually means that the user had to solve a captcha which cuts down the remaining time we have to wait. */
                logger.info("Total passed time during captcha: " + passedTime);
            }
        } else {
            /* No waittime at all */
            wait = 0;
        }
        if (wait > 0) {
            logger.info("Waiting final waittime: " + wait);
            sleep(wait * 1000l, downloadLink);
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
                if (!isDownloadableContent(con)) {
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

    protected boolean isDownloadableContent(URLConnectionAdapter con) throws IOException {
        return con != null && con.isOK() && con.isContentDisposition();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                boolean isLoggedin = false;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin()) {
                        logger.info("Cookie login successful");
                        isLoggedin = true;
                    } else {
                        logger.info("Cookie login failed");
                        isLoggedin = false;
                    }
                }
                if (!isLoggedin) {
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
                    if (!isLoggedin()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
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

    private boolean isLoggedin() {
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
                expire = Encoding.htmlDecode(expire);
            }
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", null));
        }
        if (expire == null || ai.isExpired()) {
            ai.setExpired(false);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium User");
            ai.setUnlimitedTraffic();
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            br.getPage(link.getPluginPatternMatcher());
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                // br.getPage(link.getPluginPatternMatcher());
                br.postPage("/alternate-download.php", "h=" + this.getFID(link));
                final String id = PluginJSonUtils.getJson(br, "id");
                final String downloadtoken = PluginJSonUtils.getJson(br, "downloadtoken");
                if (StringUtils.isEmpty(id) || StringUtils.isEmpty(downloadtoken)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = id + "&downloadtoken=" + Encoding.urlEncode(downloadtoken);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (!isDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            link.setProperty("premium_directlink", dl.getConnection().getURL().toString());
            dl.startDownload();
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
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        /* Premium accounts do not have captchas */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}