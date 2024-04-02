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

import java.io.IOException;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "flyfiles.net" }, urls = { "https?://(?:www\\.)?flyfiles\\.net/([a-z0-9]{10})" })
public class FlyFilesNet extends PluginForHost {
    private static final String PROPERTY_NOCHUNKS = "NOCHUNKS";

    // DEV NOTES
    // mods:
    // non account: 3 * 1
    // free account:
    // premium account: 20 * 20
    // protocol: has https but is fubar.
    // other: no redirects
    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    public FlyFilesNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + this.getHost() + "/");
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/terms.php";
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(this.getHost(), "lang", "english");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*File not found\\!")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("id=\"file_det\"[^>]*>\\s+(.+) \\- ([\\d\\.]+ (KB|MB|GB|TB))<br>");
        if (finfo.patternFind()) {
            link.setName(Encoding.htmlDecode(finfo.getMatch(0)).trim());
            link.setDownloadSize(SizeFormatter.getSize(finfo.getMatch(1)));
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final String fid = new Regex(link.getDownloadURL(), "net/(.*)").getMatch(0);
        String dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            requestFileInformation(link);
            if (br.containsHTML("(?i)This file available for downloading only for premium users")) {
                throw new AccountRequiredException();
            }
            String captchaurl = br.getRegex("\"(/captcha/[^<>\"]*?)\"").getMatch(0);
            final String waittime = br.getRegex("var\\s+timeWait\\s+=\\s+(\\d+);").getMatch(0);
            final String reCaptchaV2ID = br.getRegex("\\'sitekey\\'\\s*?:\\s*?\\'([^\"\\']+)\\'").getMatch(0);
            final String postURL = "https://" + this.getHost() + "/";
            String postata = "getDownLink=" + fid;
            if (waittime != null) {
                final long wait = Long.parseLong(waittime);
                /* Usually if there is a waittime it is a long waittime (1-2 hours). */
                if (wait > 0) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
                }
            }
            if (captchaurl != null) {
                final String code = this.getCaptchaCode(captchaurl, link);
                postata += "&captcha_value=" + Encoding.urlEncode(code);
                br.postPage(postURL, postata);
                if (br.containsHTML("#downlinkCaptcha\\|0")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            } else if (br.containsHTML("ReCaptchaDownload") && reCaptchaV2ID != null) {
                /* 2016-12-29 */
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaV2ID).getToken();
                postata += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                br.postPage(postURL, postata);
            } else {
                br.postPage(postURL, postata);
            }
            // they don't show any info about limits or waits. You seem to just
            // get '#' instead of link.
            if (br.containsHTML("#downlink\\|#")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Hoster connection limit reached.", 10 * 60 * 1000l);
            }
            dllink = getDllink(br);
            if (dllink == null) {
                /* 2022-05-30: Assume that content is only downloadable with premium account */
                throw new AccountRequiredException();
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            /* 2022-05-30: Assume that content is only downloadable with premium account */
            throw new AccountRequiredException();
        }
        link.setProperty("directlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    public String getDllink(final Browser br) {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("#downlink\\|(https?://\\w+\\.flyfiles\\.net/\\w+/.+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https?://.+)").getMatch(0);
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, true);
        String expire = new Regex(br, "(?i)<u>Premium</u>: <span id=\"premiumDate\">(\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})</span>").getMatch(0);
        if (expire == null) {
            /* Expired premium --> Free accounts are not supported! */
            ai.setExpired(true);
            return ai;
        }
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", null));
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            prepBrowser();
            br.setCookiesExclusive(true);
            prepBrowser();
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(cookies);
                if (!force) {
                    /* Do not check cookies */
                    return;
                }
                logger.info("Checking login cookies");
                br.getPage("https://" + this.getHost() + "/login.html");
                if (isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    /* Access sub-page which contains information about this account. */
                    br.getPage("/");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                } else {
                    logger.info("Cookie login failed");
                    account.clearCookies("");
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost());
            br.postPage("/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
            if (!isLoggedin(br)) {
                throw new AccountInvalidException();
            }
            br.getPage("/");
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    /** Only use this on sub-page /login.html !" */
    private boolean isLoggedin(final Browser br) {
        if (br.containsHTML("#login\\|1")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        String dllink = checkDirectLink(link, "premlink");
        if (dllink == null) {
            br.postPage("https://" + this.getHost() + "/", "getDownLink=" + new Regex(link.getDownloadURL(), "net/(.*)").getMatch(0));
            dllink = getDllink(br);
            // they don't show any info about limits or waits. You seem to just
            // get '#' instead of link.
            if (br.containsHTML("#downlink\\|#")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Hoster connection limit reached.", 10 * 60 * 1000l);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        int maxChunks = 0;
        if (link.getBooleanProperty(FlyFilesNet.PROPERTY_NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, maxChunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(FlyFilesNet.PROPERTY_NOCHUNKS, false) == false) {
                    link.setProperty(FlyFilesNet.PROPERTY_NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(FlyFilesNet.PROPERTY_NOCHUNKS, false) == false) {
                link.setProperty(FlyFilesNet.PROPERTY_NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
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
        } else if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        } else {
            return false;
        }
    }
}