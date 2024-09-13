//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "leechall.io" }, urls = { "" })
public class LeechallIo extends PluginForHost {
    /* Connection limits */
    private final boolean                ACCOUNT_PREMIUM_RESUME             = true;
    private final int                    ACCOUNT_PREMIUM_MAXCHUNKS          = 0;
    private static MultiHosterManagement mhm                                = new MultiHosterManagement("leechall.io");
    private final String                 WEBAPI_BASE                        = "https://leechall.io/api";
    private final String                 PROPERTY_ACCOUNT_ACCESS_TOKEN      = "access_token";
    private final String                 PROPERTY_ACCOUNT_RECAPTCHA_SITEKEY = "recaptchasitekey";
    private final String                 RECAPTCHA_SITEKEY_STATIC           = "6LdqV7AiAAAAAK50kHwrESPTEwVuBpAX0MCrVI0e"; /* 2023-06-27 */
    private final String                 H_CAPTCHA_SITEKEY_STATIC           = "b858042e-5b84-454c-8eda-5b3e670486d4";     /* 2024-09-12 */
    /* Don't touch the following! */
    private static final AtomicInteger   runningDls                         = new AtomicInteger(0);

    public LeechallIo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/plans");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.USERNAME_IS_EMAIL };
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/terms";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, "JDownloader");
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "application/json;charset=UTF-8");
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT, "application/json, text/plain, */*");
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://" + this.getHost());
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        login(account, false);
        final String directurlproperty = this.getHost() + "_directurl";
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        final String dllink;
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            logger.info("Generating fresh directurl");
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, "https://" + this.getHost() + "/downloader");
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("url", link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            if (link.getDownloadPassword() != null) {
                postdata.put("password", link.getDownloadPassword());
            }
            final String urlLinkgenerator = "/user/generator";
            Map<String, Object> resp = this.accessAPI(link, urlLinkgenerator, postdata, false);
            final String message = (String) resp.get("message");
            if (StringUtils.equalsIgnoreCase(message, "Please verify to continue.")) {
                /* Special case: Captcha required */
                final Map<String, Object> postdata2 = new HashMap<String, Object>();
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, account.getStringProperty(PROPERTY_ACCOUNT_RECAPTCHA_SITEKEY, RECAPTCHA_SITEKEY_STATIC)).getToken();
                postdata2.put("g-recaptcha-response", recaptchaV2Response);
                resp = this.accessAPI(link, "/user/generator/verify", postdata2, true);
                /* Try again */
                resp = this.accessAPI(link, urlLinkgenerator, postdata, true);
            } else {
                /* No captcha required -> Check for errors as we didn't do that yet. */
                this.checkErrorsWebapi(resp, link);
            }
            final Map<String, Object> file = (Map<String, Object>) resp.get("file");
            dllink = file.get("dllink").toString();
            if (StringUtils.isEmpty(dllink)) {
                /* This should never happen. */
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadlink", 20, 5 * 60 * 1000l);
            }
            link.setProperty(directurlproperty, dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        try {
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to file", 2, 5 * 60 * 1000l);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired?", e);
            } else {
                throw e;
            }
        }
        /* Add a download slot */
        controlMaxSimultaneousDownloads(account, link, +1);
        try {
            /* Start download */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxSimultaneousDownloads(account, link, -1);
        }
    }

    protected void controlMaxSimultaneousDownloads(final Account account, final DownloadLink link, final int num) {
        synchronized (runningDls) {
            final int before = runningDls.get();
            final int after = before + num;
            runningDls.set(after);
            logger.info("runningDls(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* 2023-06-28: Start downloads sequentially to avoid the need to solve multiple of those nasty captchas for premium users lol */
        return runningDls.get() + 1;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> loginmap = login(account, true);
        final Map<String, Object> resp;
        if (br.getURL().endsWith("/user/account")) {
            /* Page we need has already been accessed before. */
            resp = loginmap;
        } else {
            resp = this.getUserInfoMap(br);
        }
        final Map<String, Object> data = (Map<String, Object>) resp.get("data");
        final String status = data.get("status").toString();
        if (!status.equalsIgnoreCase("active")) {
            throw new AccountInvalidException("Account is not active");
        }
        final long total_downloadedBytes = ((Number) data.get("total_downloaded")).longValue();
        final Number total_files = (Number) data.get("total_files");
        if (Boolean.TRUE.equals(data.get("has_premium"))) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
            /**
             * Free users cannot download anything thus adding such accounts to JDownloader doesn't make any sense -> Mark them as expired.
             * </br>
             * Website says: https://leechall.io/downloader --> "Please upgrade premium to use this service."
             */
            ai.setExpired(true);
        }
        final String expiredate = (String) data.get("expired_at");
        if (!StringUtils.isEmpty(expiredate)) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate, "yyyy-MM-dd HH:mm:ss", Locale.US), br);
        }
        final SIZEUNIT maxSizeUnit = (SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue();
        ai.setStatus(account.getType().getLabel() + " | Total downloaded: " + SIZEUNIT.formatValue(maxSizeUnit, total_downloadedBytes) + " | Files: " + total_files);
        final Map<String, Object> respbandwidth = this.accessAPI("/user/bandwidth");
        final Map<String, Object> bandwidth = (Map<String, Object>) respbandwidth.get("bandwidth");
        final long dailytrafficmaxbytes = Long.parseLong(bandwidth.get("maximum").toString());
        final long dailytrafficusedbytes = Long.parseLong(bandwidth.get("usage").toString());
        ai.setTrafficMax(dailytrafficmaxbytes);
        ai.setTrafficLeft(dailytrafficmaxbytes - dailytrafficusedbytes);
        /* Collect file hosts where used has reached limits so we can skip them later and log that. */
        final Map<String, Object> resplimits = this.accessAPI("/user/limits");
        final List<Map<String, Object>> limitlist = (List<Map<String, Object>>) resplimits.get("data");
        final ArrayList<String> hostsLimitReachedBandwidth = new ArrayList<String>();
        final ArrayList<String> hostsLimitReachedFileNum = new ArrayList<String>();
        for (final Map<String, Object> limitinfo : limitlist) {
            final String host = limitinfo.get("host").toString();
            final Map<String, Object> limitsbandwidth = (Map<String, Object>) limitinfo.get("bandwidth");
            if (limitsbandwidth != null && Long.parseLong(limitsbandwidth.get("used").toString()) > Long.parseLong(limitsbandwidth.get("total").toString())) {
                hostsLimitReachedBandwidth.add(host);
            }
            final Map<String, Object> limitsfilenum = (Map<String, Object>) limitinfo.get("files");
            if (limitsfilenum != null && Long.parseLong(limitsfilenum.get("used").toString()) >= Long.parseLong(limitsfilenum.get("total").toString())) {
                hostsLimitReachedFileNum.add(host);
            }
        }
        /* Now collect all supported- and working hosts. */
        final Map<String, Object> respsupportedhosts = this.accessAPI("/app/status");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final List<Map<String, Object>> hostlist = (List<Map<String, Object>>) respsupportedhosts.get("data");
        for (final Map<String, Object> hostinfo : hostlist) {
            final String host = hostinfo.get("host").toString();
            if (!"working".equals(hostinfo.get("status"))) {
                logger.info("Skipping host which is not listed as 'working': " + host);
                continue;
            } else if (hostsLimitReachedBandwidth.contains(host)) {
                logger.info("Ignoring host because individual trafficlimit has been reached: " + host);
                continue;
            } else if (hostsLimitReachedFileNum.contains(host)) {
                logger.info("Ignoring host because files number limit has been reached: " + host);
                continue;
            } else {
                supportedHosts.add(host);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final Cookies cookies = account.loadCookies("");
            String access_token = account.getStringProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN);
            if (cookies != null && access_token != null) {
                this.br.setCookies(this.getHost(), cookies);
                br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + access_token);
                if (!force) {
                    /* Do not check cookies */
                    return null;
                }
                try {
                    final Map<String, Object> userinfomap = getUserInfoMap(br);
                    /* No exception -> Login was successful */
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return userinfomap;
                } catch (final Exception e) {
                    logger.log(e);
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("email", account.getUser());
            postdata.put("password", account.getPass());
            /* Get captcha siteKey */
            final String websiteloginurl = "https://" + this.getHost() + "/login";
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, websiteloginurl);
            brc.getPage("https://" + this.getHost() + "/js/chunk-06b4b5f5.38692513.js");
            final boolean isHCaptcha = true;
            if (isHCaptcha) {
                /* 2024-09-12 */
                String hCaptchaSiteKey = brc.getRegex("VUE_APP_HCAPTCHA:\"([^\"]+)").getMatch(0);
                if (hCaptchaSiteKey == null) {
                    logger.warning("Failed to find hCaptchaSiteKey --> Fallback to hardcoded value: " + H_CAPTCHA_SITEKEY_STATIC);
                    hCaptchaSiteKey = H_CAPTCHA_SITEKEY_STATIC;
                }
                final String hcaptchaResponse = new CaptchaHelperHostPluginHCaptcha(this, brc, hCaptchaSiteKey).getToken();
                postdata.put("h-captcha-response", hcaptchaResponse);
            } else {
                /* Legacy */
                String reCaptchaSitekey = brc.getRegex("sitekey\\s*:\"([^\"]+)").getMatch(0);
                if (reCaptchaSitekey == null) {
                    logger.warning("Failed to find reCaptchaSitekey --> Fallback to hardcoded value: " + RECAPTCHA_SITEKEY_STATIC);
                    reCaptchaSitekey = RECAPTCHA_SITEKEY_STATIC;
                }
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, brc, reCaptchaSitekey).getToken();
                postdata.put("g-recaptcha-response", recaptchaV2Response);
            }
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, websiteloginurl);
            final Map<String, Object> resp = this.accessAPI(null, "/auth/login", postdata, true);
            access_token = (String) resp.get("access_token");
            if (StringUtils.isEmpty(access_token)) {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setCookie(br.getHost(), "access_token", access_token);
            account.saveCookies(br.getCookies(br.getHost()), "");
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + access_token);
            account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN, access_token);
            account.setProperty(PROPERTY_ACCOUNT_RECAPTCHA_SITEKEY, RECAPTCHA_SITEKEY_STATIC);
            return resp;
        }
    }

    /** API GET request with global browser. */
    private Map<String, Object> accessAPI(final String url) throws IOException, PluginException, InterruptedException {
        return accessAPI(br, null, url, null, true);
    }

    private Map<String, Object> accessAPI(final DownloadLink link, final String url) throws IOException, PluginException, InterruptedException {
        return accessAPI(br, link, url, null, true);
    }

    private Map<String, Object> accessAPI(final DownloadLink link, final String url, final Map<String, Object> postdata) throws IOException, PluginException, InterruptedException {
        return accessAPI(br, link, url, postdata, true);
    }

    private Map<String, Object> accessAPI(final DownloadLink link, final String url, final Map<String, Object> postdata, final boolean checkJsonErrors) throws IOException, PluginException, InterruptedException {
        return accessAPI(br, link, url, postdata, true);
    }

    private Map<String, Object> accessAPI(final Browser br, final DownloadLink link, final String url, final Map<String, Object> postdata, final boolean checkJsonErrors) throws IOException, PluginException, InterruptedException {
        if (postdata != null) {
            br.postPageRaw(this.WEBAPI_BASE + url, JSonStorage.serializeToJson(postdata));
        } else {
            br.getPage(this.WEBAPI_BASE + url);
        }
        return this.checkErrorsWebapi(br, link, checkJsonErrors);
    }

    private Map<String, Object> getUserInfoMap(final Browser br) throws IOException, PluginException, InterruptedException {
        return this.accessAPI(br, null, "/user/account", null, true);
    }

    private Map<String, Object> checkErrorsWebapi(final Browser br, final DownloadLink link, final boolean checkJsonErrors) throws PluginException, InterruptedException {
        if (br.getHttpConnection().getResponseCode() == 401) {
            throw new AccountInvalidException("Session expired");
        }
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException jse) {
            throw new AccountUnavailableException("Bad API answer", 1 * 60 * 1000l);
        }
        if (checkJsonErrors) {
            return checkErrorsWebapi(entries, link);
        } else {
            return entries;
        }
    }

    private Map<String, Object> checkErrorsWebapi(final Map<String, Object> entries, final DownloadLink link) throws PluginException, InterruptedException {
        final Boolean success = (Boolean) entries.get("success");
        final Map<String, Object> errormap;
        if (Boolean.FALSE.equals(success)) {
            final String errormessage = entries.get("message").toString();
            if (link == null) {
                /* Account error */
                throw new AccountInvalidException(errormessage);
            } else if (errormessage.matches("(?i).*Invalid email or password.*")) {
                /* Account error by errormessage */
                throw new AccountInvalidException(errormessage);
            } else {
                /* Download error */
                /* First check for specific errors, then jump into generic errorhandling. */
                if (errormessage.equalsIgnoreCase("Can not get file size. Folder not supported. Check your link or try again.")) {
                    /* Should never happens. Can only happen if a wrong mega.nz link is supplied. */
                    logger.warning("Somehow a wrong mega(?) link was sent to " + this.getHost());
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
                } else {
                    mhm.handleErrorGeneric(null, link, errormessage, 50);
                }
            }
        } else if ((errormap = (Map<String, Object>) entries.get("errors")) != null) {
            /* For example {"message":"The given data was invalid.","errors":{"email":["The email must be a valid email address."]}} */
            final Iterator<Entry<String, Object>> iterator = errormap.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                final List<String> errors = (List<String>) entry.getValue();
                final String errormessage = errors.get(0);
                if (link == null) {
                    throw new AccountInvalidException(errormessage);
                } else {
                    mhm.handleErrorGeneric(null, link, errormessage, 50);
                }
            }
        }
        return entries;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}