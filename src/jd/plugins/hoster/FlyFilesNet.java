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
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
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

    public FlyFilesNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + this.getHost() + "/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(getHost(), "lang", "english");
        br.setFollowRedirects(true);
        return br;
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replaceFirst("(?i)https://", "http://");
    }

    @Override
    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/terms.php";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return "flyfilesnet://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(getContentURL(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*File not found\\!")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("id=\"file_det\"[^>]*>\\s+(.+) \\- ([\\d\\.]+ (KB|MB|GB|TB))<br>");
        if (finfo.patternFind()) {
            link.setName(Encoding.htmlDecode(finfo.getMatch(0)).trim());
            link.setDownloadSize(SizeFormatter.getSize(finfo.getMatch(1)));
        } else {
            logger.warning("Failed to find file information");
        }
        return AvailableStatus.TRUE;
    }

    private enum CAPTCHA_TYPE {
        IMAGE,
        INTERACTIVE
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        String dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            requestFileInformation(link);
            if (br.containsHTML("This file available for downloading only for premium users")) {
                throw new AccountRequiredException();
            }
            final String captchaurl = br.getRegex("\"(/captcha/[^<>\"]*?)\"").getMatch(0);
            final String waitSecondsStr = br.getRegex("var\\s+timeWait\\s+=\\s+(\\d+);").getMatch(0);
            final String captchaSiteKey = br.getRegex("\\'sitekey\\'\\s*?:\\s*?\\'([^\\']+)\\'").getMatch(0);
            final String postURL = "https://" + this.getHost() + "/";
            final UrlQuery query = new UrlQuery();
            query.add("getDownLink", this.getFID(link));
            query.add("human", "1");
            if (waitSecondsStr != null) {
                final long wait = Long.parseLong(waitSecondsStr);
                /* Usually if there is a waittime it is a long waittime (1-2 hours). */
                if (wait > 0) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
                }
            }
            final CAPTCHA_TYPE captchatype;
            if (captchaurl != null) {
                final String code = this.getCaptchaCode(captchaurl, link);
                query.add("captcha_value", Encoding.urlEncode(code));
                captchatype = CAPTCHA_TYPE.IMAGE;
            } else if (br.containsHTML("ReCaptchaDownload") && captchaSiteKey != null) {
                /* 2016-12-29 */
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, captchaSiteKey).getToken();
                query.add("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                captchatype = CAPTCHA_TYPE.INTERACTIVE;
            } else if (br.containsHTML("Hu?CaptchaDownload") && captchaSiteKey != null) {
                /* 2016-12-29 */
                final String hCaptchaResponse = new CaptchaHelperHostPluginHCaptcha(this, br, captchaSiteKey).getToken();
                query.add("h-captcha-response", Encoding.urlEncode(hCaptchaResponse));
                captchatype = CAPTCHA_TYPE.INTERACTIVE;
            } else {
                captchatype = null;
            }
            br.postPage(postURL, query);
            // they don't show any info about limits or waits. You seem to just
            // get '#' instead of link.
            if (br.containsHTML("#downlinkCaptcha\\|0")) {
                if (captchatype == null) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Website says 'wrong captcha' but a captcha was never required");
                } else if (captchatype == CAPTCHA_TYPE.INTERACTIVE) {
                    /* 2024-08-30: They shadow ban VPNs by just never allowing the solution of the interactive captcha. */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "VPN blocked?");
                } else {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            } else if (br.containsHTML("#downlink\\|#")) {
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
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        if (con.isContentDecoded()) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        } else {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
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
        login(account, true);
        final AccountInfo ai = new AccountInfo();
        final String expire = new Regex(br, "id=\"premiumDate\"[^>]*>(\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            /* Expired premium --> Free accounts are not supported! */
            ai.setExpired(true);
            return ai;
        }
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH), br);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(cookies);
                if (!force) {
                    /* Do not check cookies */
                    return;
                }
                logger.info("Checking login cookies");
                br.getPage("https://" + this.getHost() + "/");
                if (isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    account.clearCookies("");
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost());
            br.postPage("/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
            if (!br.containsHTML("#login\\|1")) {
                throw new AccountInvalidException();
            }
            br.getPage("/");
            /* Double-check */
            if (!isLoggedin(br)) {
                logger.warning("Not logged in according to HTML code even though login looks to have worked");
                throw new AccountInvalidException();
            }
            /* Login successful -> Save cookies */
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    /** Only use this on sub-page /login.html !" */
    private boolean isLoggedin(final Browser br) {
        if (br.containsHTML("\"form_logout shadow round_corners\"")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String dllink = checkDirectLink(link, "premlink");
        if (dllink == null) {
            requestFileInformation(link);
            login(account, false);
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

    @Override
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