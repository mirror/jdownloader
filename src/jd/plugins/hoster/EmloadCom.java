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
import java.util.Locale;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class EmloadCom extends PluginForHost {
    public EmloadCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.emload.com/premium");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/help/tos";
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "emload.com", "wdupload.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?:/v2)?/file/([A-Za-z0-9\\-_]+)(/([^/]+))?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    public String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* 2024-03-12: Login even during availablecheck as an attempt to avoid failing linkcheck due to Cloudflare. */
        final boolean useAccountDuringAvailablecheck = true;
        if (useAccountDuringAvailablecheck) {
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            return requestFileInformation(link, account, false);
        } else {
            return requestFileInformation(link, null, false);
        }
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        final String fid = urlinfo.getMatch(0);
        final String filenameFromURL = urlinfo.getMatch(2);
        if (!link.isNameSet()) {
            /* Set fallback-filename */
            if (filenameFromURL != null) {
                link.setName(Encoding.htmlDecode(filenameFromURL).trim());
            } else {
                link.setName(fid);
            }
        }
        if (account != null) {
            this.login(br, account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*The file you are trying to download is no longer available") || br.containsHTML(">\\s*The file has been removed because")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("filename wordwrap\"[^>]*>([^<]+)<").getMatch(0);
        String filesize = br.getRegex("filesize\"[^>]*>([^<]+)<").getMatch(0);
        if (!StringUtils.isEmpty(filename)) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        doFree(link, "free_directlink");
    }

    private void doFree(final DownloadLink link, final String directlinkproperty) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final String premiumonly_api = PluginJSonUtils.getJson(br, "premium_only_files");
            if (br.containsHTML("This link only for premium") || !StringUtils.isEmpty(premiumonly_api)) {
                throw new AccountRequiredException();
            }
            /* 2020-01-20: TODO: Fix this! */
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
                    this.sleep(waittime - timePassed, link);
                }
                br.postPage("/captcha/php/checkGoogleCaptcha.php", "response=" + Encoding.urlEncode(recaptchaV2Response));
                if (!br.getRequest().getHtmlCode().equals("1")) {
                    /* Wrong login captcha -> Rare case */
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.setProperty(property, Property.NULL);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void login(final Browser br, final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            if (userCookies != null) {
                logger.info("Attempting user cookie login");
                setCookiesAndHeaders(br, account, userCookies);
                if (!validateCookies) {
                    return;
                }
                if (this.checkLoginStatus(br)) {
                    logger.info("Login via user cookies successful");
                    account.setProperty("useragent", br.getRequest().getHeaders().get(HTTPConstants.HEADER_REQUEST_USER_AGENT));
                    return;
                } else {
                    logger.info("Login via user cookies failed");
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
            } else if (cookies != null) {
                logger.info("Attempting cookie login");
                setCookiesAndHeaders(br, account, cookies);
                if (this.checkLoginStatus(br)) {
                    logger.info("Login via stored cookies successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Login via stored cookies failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                    /* Perform full login */
                }
            }
            logger.info("Performing full login");
            br.getPage("https://www." + getHost() + "/user/login");
            final boolean force_use_static_access_token = false;
            String access_token;
            final String hardcoded_access_token = "br68ufmo5ej45ue1q10w68781069v666l2oh1j2ijt94";
            if (force_use_static_access_token) {
                /* 2018-10-19 */
                access_token = hardcoded_access_token;
            } else {
                br.getPage("https://www." + this.getHost() + "/java/mycloud.js");
                access_token = br.getRegex("app:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
            }
            if (StringUtils.isEmpty(access_token)) {
                /* 2020-01-30: Their access_token has not changed for oever one year --> We're using it as a fallback */
                logger.info("Failed to find access_token --> Using static access_token as fallback");
                access_token = hardcoded_access_token;
            }
            // if (StringUtils.isEmpty(access_token)) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            String recaptchaV2Response = null;
            if (br.containsHTML("g-recaptcha")) {
                logger.info("Login captcha required");
                /* 2020-01-27: New and sometimes required */
                String reCaptchaKey = br.getRegex("class=\"g-recaptcha\" data-sitekey=\"([^\"]+)\"").getMatch(0);
                if (reCaptchaKey == null) {
                    /* 2020-01-27 */
                    logger.info("Falling back to static reCaptchaV2 key");
                    reCaptchaKey = "6Lc0vNIUAAAAAPs7i05tOzupSGG2ikUHobmDoZJa";
                }
                recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
            } else {
                logger.info("Login captcha NOT required");
            }
            br.getHeaders().put("Origin", "https://www." + this.getHost());
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            String postData = "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&keep=1";
            if (recaptchaV2Response != null) {
                postData += "&captcha=" + Encoding.urlEncode(recaptchaV2Response);
            }
            br.postPage("https://www." + this.getHost() + "/api/0/signmein?useraccess=&access_token=" + access_token, postData);
            final String result = PluginJSonUtils.getJson(br, "result");
            String userdata = PluginJSonUtils.getJson(br, "doz");
            if (!"ok".equals(result) || StringUtils.isEmpty(userdata)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            userdata = URLEncode.encodeURIComponent(userdata);
            br.setCookie(br.getHost(), "userdata", userdata);
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private void setCookiesAndHeaders(final Browser br, final Account account, final Cookies cookies) {
        /* Special User-Agent handling which can sometimes help to get around Cloudflare related problems. */
        final String lastUsedUserAgent = account.getStringProperty("useragent");
        final String userAgentFromCookies = cookies.getUserAgent();
        if (userAgentFromCookies != null) {
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, userAgentFromCookies);
        } else if (lastUsedUserAgent != null) {
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, lastUsedUserAgent);
        }
        br.setCookies(cookies);
    }

    private boolean checkLoginStatus(final Browser br) throws IOException {
        br.getPage("https://www." + getHost() + "/v2/notifications");
        return isLoggedin(br);
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("user/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(this.br, account, true);
        /* 2020-01-20: Static fallback according to website */
        final AccountInfo ai = new AccountInfo();
        final long trafficmax_fallback = 35000000000l;
        if (!br.getURL().endsWith("/me")) {
            br.getPage("/me");
        }
        final String accounttype = br.getRegex("(?i)<label>Your Plan</label>\\s*?<span class=\"known_values\"><div [^>]+></div>\\s*([^<>]+)\\s*</span>").getMatch(0);
        /* E.g. Lifetime Free Account */
        if (accounttype == null || accounttype.contains("Free")) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(this.getMaxSimultanFreeDownloadNum());
            account.setConcurrentUsePossible(false);
            ai.setUnlimitedTraffic();
        } else {
            final Regex bandwidth = br.getRegex("<b>\\s*Bandwidth\\s*</b></p>\\s*?<h4 class=\"text\\-center\">(\\d+(?:\\.\\d+)? ?(?:KB|MB|GB)) of (\\d+(?:\\.\\d+)? ?(KB|MB|GB))</h4>");
            final String trafficUsedStr = bandwidth.getMatch(0);
            final String trafficMaxStr = bandwidth.getMatch(1);
            final String expire = br.getRegex("Premium expires on <span [^<>]+>(\\d{4}\\-\\d{2}\\-\\d{2})<").getMatch(0);
            if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
            }
            final long trafficMax;
            if (trafficMaxStr != null) {
                trafficMax = SizeFormatter.getSize(trafficMaxStr);
            } else {
                trafficMax = trafficmax_fallback;
            }
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
            account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            requestFileInformation(link, account, true);
            br.getPage(link.getPluginPatternMatcher());
            doFree(link, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink_2");
            if (dllink == null) {
                final boolean followRedirectsBefore = br.isFollowingRedirects();
                try {
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
                } finally {
                    br.setFollowRedirects(followRedirectsBefore);
                }
                if (StringUtils.isEmpty(dllink)) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 401) {
                    /* This sometimes happens for premiumonly content */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            link.setProperty("premium_directlink", dl.getConnection().getURL().toExternalForm());
            dl.startDownload();
        }
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
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}