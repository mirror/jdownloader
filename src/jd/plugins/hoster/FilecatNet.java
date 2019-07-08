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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filecat.net" }, urls = { "https?://(?:www\\.)?filecat\\.net/f/([A-Za-z0-9]+)" })
public class FilecatNet extends PluginForHost {
    public FilecatNet(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://www.file4.net/tos";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME       = true;
    private final int     FREE_MAXCHUNKS    = 0;
    private final int     FREE_MAXDOWNLOADS = 1;
    // private final boolean ACCOUNT_FREE_RESUME = true;
    // private final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setAllowedResponseCodes(new int[] { 400 });
        br.getPage("https://api." + this.getHost() + "/file/" + this.getLinkID(link));
        if (this.br.getHttpConnection().getResponseCode() == 400 || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = PluginJSonUtils.getJson(br, "name");
        if (StringUtils.isEmpty(filename)) {
            filename = this.getLinkID(link);
        }
        String filesize = PluginJSonUtils.getJson(br, "size");
        if (StringUtils.isEmpty(filename)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename);
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(Long.parseLong(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final String fid = this.getLinkID(link);
            final String premiumonly = PluginJSonUtils.getJson(br, "premonly");
            if ("true".equalsIgnoreCase(premiumonly)) {
                throw new AccountRequiredException();
            }
            br.getHeaders().put("Accept", "application/json, text/plain, */*");
            br.getHeaders().put("X-URL", link.getPluginPatternMatcher());
            br.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
            /* Important! This sets the crucial "PHPSESSID" cookie!! */
            br.getPage("/app");
            final PostRequest preWaitReq = br.createJSonPostRequest("/dwnldreq", "{\"id\":null,\"file_uid\":\"" + fid + "\",\"captcha_token\":null}");
            br.openRequestConnection(preWaitReq);
            br.loadConnection(null);
            final String reject_reason = PluginJSonUtils.getJson(br, "reject_reason");
            if (reject_reason != null) {
                // final String reject_msg = PluginJSonUtils.getJson(br, "reject_msg");
                if (reject_reason.equalsIgnoreCase("ip_daily_downloads_limit")) {
                    /* No exact waittime given */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
                }
            }
            final String id = PluginJSonUtils.getJson(br, "id");
            String waitSecondsStr = PluginJSonUtils.getJson(br, "wait_sec");
            final String captcha_needed = PluginJSonUtils.getJson(br, "captcha_needed");
            if (StringUtils.isEmpty(id) || StringUtils.isEmpty(waitSecondsStr) || StringUtils.isEmpty(captcha_needed)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final long timestampBeforeCaptcha = System.currentTimeMillis();
            if ("true".equalsIgnoreCase(captcha_needed)) {
                /* 2019-07-08: Hardcoded reCaptchaV2 key */
                final String rcKey = "6LfFS28UAAAAAIaK3SXWYWZ_iPK-zfOr-NmZaY0f";
                final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcKey);
                final String recaptchaV2Response = rc2.getToken();
                waitTime(link, timestampBeforeCaptcha, waitSecondsStr);
                final PostRequest afterCaptchaReq = br.createJSonPostRequest("/dwnldreq", "{\"id\":" + id + ",\"file_uid\":\"" + fid + "\",\"captcha_token\":\"" + recaptchaV2Response + "\"}");
                br.openRequestConnection(afterCaptchaReq);
                br.loadConnection(null);
            } else {
                /* No captcha, only wait */
                waitTime(link, timestampBeforeCaptcha, waitSecondsStr);
            }
            dllink = PluginJSonUtils.getJson(br, "link");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!dllink.startsWith("http")) {
                dllink = "https://" + dllink;
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private boolean preDownloadWaittimeSkippable() {
        return false;
    }

    /**
     * Handles pre download (pre-captcha) waittime. If WAITFORCED it ensures to always wait long enough even if the waittime RegEx fails.
     */
    protected void waitTime(final DownloadLink downloadLink, final long timeBefore, final String waitStr) throws PluginException {
        /* Ticket Time */
        if (this.preDownloadWaittimeSkippable()) {
            /* Very rare case! */
            logger.info("Skipping pre-download waittime: " + waitStr);
        } else {
            final int extraWaitSeconds = 1;
            int wait;
            if (waitStr != null && waitStr.matches("\\d+")) {
                int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
                logger.info("Found waittime, parsing waittime: " + waitStr);
                wait = Integer.parseInt(waitStr);
                /*
                 * Check how much time has passed during eventual captcha event before this function has been called and see how much time
                 * is left to wait.
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
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                /* 2019-07-08: Important: Do NOT use HeadConnection here! This will lead to response 404!! */
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    // private static Object LOCK = new Object();
    //
    // private void login(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
    // br.setFollowRedirects(true);
    // br.setCookiesExclusive(true);
    // final Cookies cookies = account.loadCookies("");
    // if (cookies != null && !force) {
    // this.br.setCookies(this.getHost(), cookies);
    // return;
    // }
    // br.getPage("");
    // if (br.containsHTML("")) {
    // final DownloadLink dlinkbefore = this.getDownloadLink();
    // final DownloadLink dl_dummy;
    // if (dlinkbefore != null) {
    // dl_dummy = dlinkbefore;
    // } else {
    // dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
    // this.setDownloadLink(dl_dummy);
    // }
    // final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
    // if (dlinkbefore != null) {
    // this.setDownloadLink(dlinkbefore);
    // }
    // // g-recaptcha-response
    // }
    // br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
    // if (!isLoggedin()) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // account.saveCookies(this.br.getCookies(this.getHost()), "");
    // } catch (final PluginException e) {
    // account.clearCookies("");
    // throw e;
    // }
    // }
    // }
    //
    // private boolean isLoggedin() {
    // return br.getCookie(this.getHost(), "", Cookies.NOTDELETEDPATTERN) != null;
    // }
    //
    // @Override
    // public AccountInfo fetchAccountInfo(final Account account) throws Exception {
    // final AccountInfo ai = new AccountInfo();
    // try {
    // login(account, true);
    // } catch (final PluginException e) {
    // throw e;
    // }
    // String space = br.getRegex("").getMatch(0);
    // if (space != null) {
    // ai.setUsedSpace(space.trim());
    // }
    // ai.setUnlimitedTraffic();
    // if (br.containsHTML("")) {
    // account.setType(AccountType.FREE);
    // /* free accounts can still have captcha */
    // account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
    // account.setConcurrentUsePossible(false);
    // ai.setStatus("Registered (free) user");
    // } else {
    // final String expire = br.getRegex("").getMatch(0);
    // if (expire == null) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
    // }
    // account.setType(AccountType.PREMIUM);
    // account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
    // account.setConcurrentUsePossible(true);
    // ai.setStatus("Premium account");
    // }
    // return ai;
    // }
    //
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // requestFileInformation(link);
    // login(account, false);
    // br.getPage(link.getPluginPatternMatcher());
    // if (account.getType() == AccountType.FREE) {
    // doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    // } else {
    // String dllink = this.checkDirectLink(link, "premium_directlink");
    // if (dllink == null) {
    // dllink = br.getRegex("").getMatch(0);
    // if (StringUtils.isEmpty(dllink)) {
    // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // }
    // dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
    // if (dl.getConnection().getContentType().contains("html")) {
    // if (dl.getConnection().getResponseCode() == 403) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
    // } else if (dl.getConnection().getResponseCode() == 404) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
    // }
    // logger.warning("The final dllink seems not to be a file!");
    // br.followConnection();
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // link.setProperty("premium_directlink", dl.getConnection().getURL().toString());
    // dl.startDownload();
    // }
    // }
    //
    // @Override
    // public int getMaxSimultanPremiumDownloadNum() {
    // return ACCOUNT_FREE_MAXDOWNLOADS;
    // }
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