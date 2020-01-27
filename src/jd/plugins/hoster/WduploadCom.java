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

import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wdupload.com" }, urls = { "https?://(?:www\\.)?wdupload\\.com/file/([A-Za-z0-9\\-_]+)(/.+)?" })
public class WduploadCom extends antiDDoSForHost {
    public WduploadCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.wdupload.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www.wdupload.com/help/tos";
    }

    /* Connection stuff */
    private final boolean        FREE_RESUME                  = false;
    private final int            FREE_MAXCHUNKS               = 1;
    private final int            FREE_MAXDOWNLOADS            = 1;
    private final boolean        ACCOUNT_FREE_RESUME          = false;
    private final int            ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int            ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private final boolean        ACCOUNT_PREMIUM_RESUME       = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    /*
     * New parts of the website, intriduced 2020-01-20 but then reverted back to old style 2020-01-21 </br> They will probably re-introduce
     * their changes! In this case, simply setting this to true should fix availablecheck, login and (premium) download!
     */
    private static final boolean useWebAPI                    = false;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    public String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        String filename = null, filesize = null;
        final String fid = this.getFID(link);
        final boolean use_API = false;
        if (useWebAPI) {
            /* 2020-01-20 */
            this.getPage("http://wduphp." + this.getHost() + "/api/filemanager/details/" + fid);
            if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = PluginJSonUtils.getJson(br, "name");
            filesize = PluginJSonUtils.getJson(br, "file_size");
        } else {
            getPage(link.getPluginPatternMatcher());
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("class=\"file-header text-center\">\\s*?<h1>([^<>\"]+)</h1>").getMatch(0);
            if (filename == null) {
                filename = new Regex(link.getPluginPatternMatcher(), "file/[^/]+/(.+)").getMatch(0);
            }
            if (filename == null) {
                filename = this.getLinkID(link);
            }
            filesize = br.getRegex("class=\"file\\-size\">([^<>\"]+)<").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = fid;
        }
        link.setName(filename);
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
                getPage("/api/" + userid + "/ddelay?userid=" + userid);
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
                postPage(br, "/captcha/php/checkGoogleCaptcha.php", "response=" + Encoding.urlEncode(recaptchaV2Response));
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

    public void login(Browser brlogin, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                brlogin.setFollowRedirects(true);
                brlogin.setCookiesExclusive(true);
                if (useWebAPI) {
                    /* 2020-01-20: New */
                    String token = account.getStringProperty("logintoken", null);
                    final Cookies cookies = account.loadCookies("");
                    if (cookies != null && token != null) {
                        brlogin.setCookies(account.getHoster(), cookies);
                        brlogin.getHeaders().put("Authorization", "Bearer " + token);
                        brlogin.getHeaders().put("Content-Type", "application/json");
                        this.getPage("http://wduphp." + this.getHost() + "/api/users/login-history?page_no=1&limit=10");
                        if (brlogin.toString().startsWith("{") && brlogin.getHttpConnection().getResponseCode() == 200) {
                            logger.info("Login via stored token was successful");
                            return;
                        }
                        logger.info("Login via stored token failed");
                        /* Drop old cookies & headers */
                        brlogin = new Browser();
                    }
                    /* 2020-01-20: TODO: Check for login captcha. Currently website will ask for it but it can be skipped! */
                    this.getPage(brlogin, "http://www." + this.getHost() + "/login");
                    final String postData = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", account.getUser(), account.getPass());
                    this.postPageRaw(brlogin, "http://wduphp." + this.getHost() + "/api/auth/login", postData, true);
                    final String success = PluginJSonUtils.getJson(brlogin, "success");
                    token = PluginJSonUtils.getJson(brlogin, "token");
                    if (!"true".equalsIgnoreCase(success) || StringUtils.isEmpty(token)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    account.setProperty("logintoken", token);
                    brlogin.getHeaders().put("Authorization", "Bearer " + token);
                } else {
                    final Cookies cookies = account.loadCookies("");
                    if (cookies != null) {
                        logger.info("Attempting cookie login");
                        brlogin.setCookies(account.getHoster(), cookies);
                        getPage(brlogin, "https://www." + getHost() + "/me");
                        if (brlogin.getCookie(brlogin.getHost(), "userdata", Cookies.NOTDELETEDPATTERN) != null) {
                            logger.info("Login via stored cookies successful");
                            account.saveCookies(brlogin.getCookies(account.getHoster()), "");
                            return;
                        }
                        logger.info("Login via stored cookies failed");
                        /* Perform full login */
                    }
                    logger.info("Performing full login");
                    getPage(brlogin, "https://www." + getHost() + "/user/login");
                    final boolean use_static_access_token = false;
                    final String access_token;
                    if (use_static_access_token) {
                        /* 2018-10-19 */
                        access_token = "br68ufmo5ej45ue1q10w68781069v666l2oh1j2ijt94";
                    } else {
                        getPage(brlogin, "https://www." + account.getHoster() + "/java/mycloud.js");
                        access_token = brlogin.getRegex("app:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
                    }
                    if (StringUtils.isEmpty(access_token)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    String recaptchaV2Response = null;
                    try {
                        final DownloadLink dl_dummy;
                        if (dlinkbefore != null) {
                            dl_dummy = dlinkbefore;
                        } else {
                            dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                            this.setDownloadLink(dl_dummy);
                        }
                        /* 2020-01-27: New and always required */
                        String reCaptchaKey = brlogin.getRegex("class=\"g-recaptcha\" data-sitekey=\"([^\"]+)\"").getMatch(0);
                        if (reCaptchaKey == null) {
                            /* 2020-01-27 */
                            logger.info("Falling back to static reCaptchaV2 key");
                            reCaptchaKey = "6Lc0vNIUAAAAAPs7i05tOzupSGG2ikUHobmDoZJa";
                        }
                        recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, brlogin, reCaptchaKey).getToken();
                    } catch (final Throwable e) {
                        logger.info("Possible login captcha failure");
                        e.printStackTrace();
                    } finally {
                        this.setDownloadLink(dlinkbefore);
                    }
                    brlogin.getHeaders().put("Origin", "https://www." + account.getHoster());
                    brlogin.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    String postData = "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&keep=1";
                    if (recaptchaV2Response != null) {
                        postData += "&captcha=" + Encoding.urlEncode(recaptchaV2Response);
                    }
                    postPage(brlogin, "https://www." + account.getHoster() + "/api/0/signmein?useraccess=&access_token=" + access_token, postData);
                    final String result = PluginJSonUtils.getJson(brlogin, "result");
                    String userdata = PluginJSonUtils.getJson(brlogin, "doz");
                    if (!"ok".equals(result) || StringUtils.isEmpty(userdata)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    userdata = URLEncode.encodeURIComponent(userdata);
                    brlogin.setCookie(brlogin.getHost(), "userdata", userdata);
                }
                account.saveCookies(brlogin.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        /* 2020-01-20: Static fallback according to website */
        final long trafficmax_fallback = 35000000000l;
        if (useWebAPI) {
            /*
             * 2020-01-20: This will result in error response 500 for freeusers but we do not require any special data for those anyways so
             * that can be ignored. Free accounts will be listed without any issues.
             */
            this.getPage("/api/users");
            // final String plan_type = PluginJSonUtils.getJson(br, "plan_type");
            long validUntil = 0;
            final String expiredateStr = PluginJSonUtils.getJson(br, "end_date");
            final String trafficMaxStr = PluginJSonUtils.getJson(br, "bandwidth");
            final String trafficUsedStr = PluginJSonUtils.getJson(br, "total_bandwith_downloaded");
            if (expiredateStr != null && expiredateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                validUntil = TimeFormatter.getMilliSeconds(expiredateStr, "yyyy-MM-dd", Locale.ENGLISH);
            }
            if (System.currentTimeMillis() > validUntil) {
                account.setType(AccountType.FREE);
                /* free accounts can still have captcha */
                account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setConcurrentUsePossible(false);
                ai.setUnlimitedTraffic();
                ai.setStatus("Registered (free) user");
            } else {
                /* TODO: Add expire date, trafficleft and so on */
                ai.setTrafficMax(trafficmax_fallback);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                ai.setStatus("Premium account");
                ai.setValidUntil(validUntil);
                final long trafficMax;
                long trafficUsed = 0;
                if (trafficMaxStr != null && trafficMaxStr.matches("\\d+")) {
                    trafficMax = Long.parseLong(trafficMaxStr);
                } else {
                    trafficMax = trafficmax_fallback;
                }
                if (trafficUsedStr != null && trafficUsedStr.matches("\\d+")) {
                    trafficUsed = Long.parseLong(trafficUsedStr);
                }
                ai.setTrafficLeft(trafficMax - trafficUsed);
                ai.setTrafficMax(trafficMax);
            }
        } else {
            if (!br.getURL().endsWith("/me")) {
                getPage("/me");
            }
            final String accounttype = br.getRegex("<label>Your Plan</label>\\s*?<span class=\"known_values\"><div [^>]+></div>\\s*([^<>]+)\\s*</span>").getMatch(0);
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
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                ai.setStatus("Premium account");
            }
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(this.br, account, false);
        if (account.getType() == AccountType.FREE) {
            requestFileInformation(link);
            getPage(link.getPluginPatternMatcher());
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink_2");
            if (dllink == null) {
                br.setFollowRedirects(false);
                if (useWebAPI) {
                    requestFileInformation(link);
                    final String logintoken = account.getStringProperty("logintoken", null);
                    final String file_uploading_api_url = PluginJSonUtils.getJson(br, "file_uploading_api_url");
                    if (StringUtils.isEmpty(file_uploading_api_url)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = String.format("%s/%s?token=%s", file_uploading_api_url, this.getFID(link), logintoken);
                } else {
                    getPage(link.getPluginPatternMatcher());
                    /* First check if user has direct download enabled */
                    dllink = br.getRedirectLocation();
                    /* Direct download disabled? We have to find the final downloadurl. */
                    if (StringUtils.isEmpty(dllink)) {
                        dllink = br.getRegex("\"(https?://[^/]+/download\\.php[^<>\"]+)\"").getMatch(0);
                    }
                    if (StringUtils.isEmpty(dllink)) {
                        dllink = br.getRegex("<p>Click here to download</p>\\s*?<a href=\"(https?://[^<>\"]+)\"").getMatch(0);
                    }
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