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
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wdupload.com" }, urls = { "https?://(?:www\\.)?wdupload\\.com/file/[A-Za-z0-9\\-_]+(/.+)?" })
public class WduploadCom extends PluginForHost {
    public WduploadCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.wdupload.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www.wdupload.com/help/tos";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME                  = false;
    private final int     FREE_MAXCHUNKS               = 1;
    private final int     FREE_MAXDOWNLOADS            = 1;
    private final boolean ACCOUNT_FREE_RESUME          = false;
    private final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), "/file/([^/]+)").getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"file-header text-center\">\\s*?<h1>([^<>\"]+)</h1>").getMatch(0);
        if (filename == null) {
            filename = new Regex(link.getPluginPatternMatcher(), "file/[^/]+/(.+)").getMatch(0);
        }
        if (filename == null) {
            filename = this.getLinkID(link);
        }
        String filesize = br.getRegex("class=\"file\\-size\">([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            if (br.containsHTML("This link only for premium")) {
                throw new AccountRequiredException();
            }
            final String dl_server = br.getRegex("freeaccess=\"(http[^<>\"]+)\"").getMatch(0);
            final String dl_token = br.getRegex("freetoken=\"([^<>\"]+)\"").getMatch(0);
            final String userid = br.getRegex("uid\\s*?=\\s*?\"(\\d+)\"").getMatch(0);
            if (StringUtils.isEmpty(dl_server) || StringUtils.isEmpty(dl_token) || StringUtils.isEmpty(userid)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* 2018-10-18: Captcha is skippable */
            final boolean skipCaptcha = true;
            if (!skipCaptcha) {
                br.getPage("/api/" + userid + "/ddelay?userid=" + userid);
                /* Usually 45 seconds */
                final String waittimeStr = PluginJSonUtils.getJson(this.br, "");
                if (StringUtils.isEmpty(waittimeStr)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final long waittime = Long.parseLong(waittimeStr) * 1001;
                final long timeBefore = System.currentTimeMillis();
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                final long timePassed = System.currentTimeMillis() - timeBefore;
                if (timePassed < waittime) {
                    this.sleep(waittime - timePassed, downloadLink);
                }
                br.postPage("/captcha/php/checkGoogleCaptcha.php", "response=" + Encoding.urlEncode(recaptchaV2Response));
                if (!br.toString().equals("1")) {
                    /* Rare case */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                /* Not required */
                // br.postPage("/api/0/downloadbtn?useraccess=&access_token=bla", "");
            }
            dllink = Encoding.htmlDecode(dl_server) + "download.php?accesstoken=" + Encoding.htmlDecode(dl_token);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public static Object LOCK = new Object();

    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(account.getHoster(), cookies);
                    return;
                }
                final boolean use_static_access_token = false;
                final String access_token;
                if (use_static_access_token) {
                    /* 2018-10-19 */
                    access_token = "br68ufmo5ej45ue1q10w68781069v666l2oh1j2ijt94";
                } else {
                    br.getPage("https://www." + account.getHoster() + "/java/mycloud.js");
                    access_token = br.getRegex("app:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
                }
                if (StringUtils.isEmpty(access_token)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("Origin", "https://www." + account.getHoster());
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("https://www." + account.getHoster() + "/api/0/signmein?useraccess=&access_token=" + access_token, "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&keep=1");
                final String result = PluginJSonUtils.getJson(br, "result");
                String userdata = PluginJSonUtils.getJson(br, "doz");
                if (!"ok".equals(result) || StringUtils.isEmpty(userdata)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                userdata = URLEncode.encodeURIComponent(userdata);
                br.setCookie(br.getHost(), "userdata", userdata);
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("/me");
        final String accounttype = br.getRegex("<label>Your Plan</label>\\s*?<span class=\"known_values\"><div [^>]+></div>([^<>]+)</span>").getMatch(0);
        /* E.g. Lifetime Free Account */
        if (accounttype == null || accounttype.contains("Free")) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setUnlimitedTraffic();
            ai.setStatus("Registered (free) user");
        } else {
            final Regex bandwidth = br.getRegex("<b>Bandwidth</b></p>\\s*?<h4 class=\"text\\-center\">(\\d+(?:\\.\\d+)? ?(?:KB|MB|GB)) of (\\d+(?:\\.\\d+)? ?(KB|MB|GB))</h4>");
            final String trafficUsedStr = bandwidth.getMatch(0);
            String trafficMaxStr = bandwidth.getMatch(1);
            if (trafficMaxStr == null) {
                /* Use static value as fallback (according to website 2018-10-19) */
                trafficMaxStr = "35GB";
            }
            final String expire = br.getRegex("Premium expires on <span [^<>]+>(\\d{4}\\-\\d{2}\\-\\d{2})<").getMatch(0);
            if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
            }
            final long trafficMax = SizeFormatter.getSize(trafficMaxStr);
            if (trafficUsedStr != null) {
                final long trafficUsed = SizeFormatter.getSize(trafficUsedStr);
                ai.setTrafficLeft(trafficMax - trafficUsed);
                ai.setTrafficMax(trafficMax);
            } else {
                /* Use hardcoded trafficMax value for both */
                ai.setTrafficLeft(trafficMax);
                ai.setTrafficMax(trafficMax);
            }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        if (account.getType() == AccountType.FREE) {
            br.getPage(link.getPluginPatternMatcher());
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink_2");
            if (dllink == null) {
                br.setFollowRedirects(false);
                br.getPage(link.getPluginPatternMatcher());
                /* First check if user has direct download enabled */
                dllink = br.getRedirectLocation();
                /* Direct download disabled? We have to find the final downloadurl. */
                if (StringUtils.isEmpty(dllink)) {
                    dllink = br.getRegex("\"(https?://[^/]+/download\\.php[^<>\"]+)\"").getMatch(0);
                }
                if (StringUtils.isEmpty(dllink)) {
                    dllink = br.getRegex("<p>Click here to download</p>\\s*?<a href=\"(https?://[^<>\"]+)\"").getMatch(0);
                }
                if (StringUtils.isEmpty(dllink)) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 401) {
                    /* This sometimes happens for premiumonly content */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dl.getConnection().getURL().toString());
            dl.startDownload();
        }
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
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}