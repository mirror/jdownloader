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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.CamvaultXyzCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CamvaultXyz extends PluginForHost {
    public CamvaultXyz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.camvault.xyz/premium");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public void init() {
        CamvaultXyzCrawler.setRequestIntervalLimitGlobal();
    }

    @Override
    public String getAGBLink() {
        return "https://www.camvault.xyz/document/terms";
    }

    public static List<String[]> getPluginDomains() {
        return CamvaultXyzCrawler.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/cloud/file/([a-zA-Z0-9_/\\+\\=\\-%]+)");
        }
        return ret.toArray(new String[0]);
    }

    public static final String PATTERN_CLOUD             = "(?i)https?://[^/]+/cloud/file/([a-zA-Z0-9_/\\+\\=\\-%]+)";
    private final String       PROPERTY_INTERNAL_FILE_ID = "internal_file_id";

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
        final String internalFileID = link.getStringProperty(PROPERTY_INTERNAL_FILE_ID);
        if (internalFileID != null) {
            return internalFileID;
        } else {
            return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (!link.isNameSet()) {
            /* Set fallback-filename */
            final String fid = this.getFID(link);
            if (fid != null) {
                link.setName(fid + ".mp4");
            }
        }
        if (account == null || account.getType() != AccountType.PREMIUM) {
            throw new AccountRequiredException();
        }
        final String contenturl = link.getPluginPatternMatcher();
        this.login(account, contenturl, true);
        if (CamvaultXyzCrawler.isRateLimitReached(br)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Rate limit reached", 1 * 60 * 1000l);
        }
        if (br.getURL().matches(".*/premium$")) {
            /*
             * Either login session was randomly over or this item belongs to another user and thus cannot be accessed with given account.
             */
            throw new AccountRequiredException();
        } else if (CamvaultXyzCrawler.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getPluginPatternMatcher().matches(PATTERN_CLOUD)) {
            if (!this.canHandle(br.getURL())) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        parseFileInfo(br, link);
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfo(final Browser br, final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(PATTERN_CLOUD)) {
            final String filename = br.getRegex("class=\"fas fa-file\"[^>]*></i>([^<]+)<").getMatch(0);
            final String filesize = br.getRegex("class=\"fas fa-hdd\"[^>]*></i>([^<]+)<").getMatch(0);
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename).trim());
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        } else {
            final Regex fileInfo = br.getRegex("class=\"fa fa-file-video-o\"[^>]*></i>([^<]*) \\((\\d+(\\.\\d{1,2})? [A-Za-z]+)\\)</li>");
            if (fileInfo.patternFind()) {
                link.setName(Encoding.htmlDecode(fileInfo.getMatch(0)).trim());
                link.setDownloadSize(SizeFormatter.getSize(fileInfo.getMatch(1)));
            }
        }
        link.setAvailable(true);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public boolean login(final Account account, final String checkloginURL, final boolean force) throws Exception {
        if (account == null || checkloginURL == null) {
            throw new IllegalArgumentException();
        }
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (cookies != null || userCookies != null) {
                    logger.info("Attempting cookie login");
                    if (userCookies != null) {
                        br.setCookies(this.getHost(), userCookies);
                    } else {
                        br.setCookies(this.getHost(), cookies);
                    }
                    if (!force) {
                        /* Don't validate cookies */
                        return false;
                    }
                    br.getPage(checkloginURL);
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        if (userCookies == null) {
                            account.saveCookies(br.getCookies(br.getHost()), "");
                        }
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                        if (userCookies != null) {
                            /* Dead end */
                            if (account.hasEverBeenValid()) {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                            } else {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                            }
                        }
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/login.html");
                boolean hasRequiredCaptcha = false;
                int tryNumber = 1;
                while (!hasRequiredCaptcha && tryNumber <= 1) {
                    logger.info("Login attempt number: " + tryNumber);
                    final Form loginform = br.getFormbyActionRegex(".*/login.*");
                    if (loginform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put(Encoding.urlEncode("data[User][username]"), Encoding.urlEncode(account.getUser()));
                    loginform.put(Encoding.urlEncode("data[User][password]"), Encoding.urlEncode(account.getPass()));
                    loginform.put(Encoding.urlEncode("data[User][remember]"), "1");
                    if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                        hasRequiredCaptcha = true;
                    }
                    br.submitForm(loginform);
                    if (!CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                        /* No captcha, no retry */
                        break;
                    } else {
                        /* Captcha required -> Either invalid login credentials or website thinks we might be a bot. */
                        tryNumber++;
                        continue;
                    }
                }
                br.getPage(checkloginURL);
                if (CamvaultXyzCrawler.isRateLimitReached(br)) {
                    throw new AccountUnavailableException("Rate limit reached", 1 * 60 * 1000l);
                } else if (!isLoggedin(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("users/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, "https://www." + this.getHost() + "/premium", true);
        long expireTimestamp = 0;
        final String premiumExpiredateStr = br.getRegex("(?i)it will expire at\\s*<strong>(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})</strong>").getMatch(0);
        if (premiumExpiredateStr != null) {
            expireTimestamp = TimeFormatter.getMilliSeconds(premiumExpiredateStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            expireTimestamp = AccountInfo.getTimestampInServerContext(br, expireTimestamp);
        }
        if (expireTimestamp > System.currentTimeMillis()) {
            ai.setValidUntil(expireTimestamp);
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.requestFileInformation(link, account);
        if (link.getPluginPatternMatcher().matches(PATTERN_CLOUD)) {
            String dllink = br.getRegex("(?i)our browser does not support the video tag.\\s*<source src=\"(https?://[^\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https?://[^/]+/v2/\\?[a-f0-9]{128})").getMatch(0);
            }
            if (dllink != null) {
                /* Resume and chunkload possible */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            } else {
                final Form dlform = br.getFormbyProperty("id", "DownloadFileForm");
                if (dlform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Browser br2 = br.cloneBrowser();
                final String fid = dlform.getInputFieldByName("fileID").getValue();
                link.setProperty(PROPERTY_INTERNAL_FILE_ID, fid);
                br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br2.postPage("/cloud/downloadtoken", "file_id=" + fid);
                final Map<String, Object> entries = restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
                final String cdn = entries.get("cdn").toString();
                final String token = entries.get("token").toString();
                dlform.setAction(br._getURL().getProtocol() + "://" + cdn + "/v2");
                dlform.put("downloadToken", Encoding.urlEncode(token));
                /* No resume and chunkload possible */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlform, false, 1);
            }
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
            dl.startDownload();
        } else {
            /* E.g. /download/blabla-<numbers>.html */
            final Form dlform = br.getFormbyProperty("id", "DownloadDownloadForm");
            if (dlform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String fid = dlform.getInputFieldByName("videoToken").getValue();
            link.setProperty(PROPERTY_INTERNAL_FILE_ID, fid);
            /* 2023-11-28: They've added a captcha for premium downloads. */
            String recaptchaV2Response = null;
            final String reCaptchaKey = br.getRegex("google\\.com/recaptcha/api\\.js\\?render=([^\"]+)").getMatch(0);
            if (reCaptchaKey != null) {
                /* 2023-11-28 */
                recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey) {
                    @Override
                    public TYPE getType() {
                        return TYPE.INVISIBLE;
                    }
                }.getToken();
            } else {
                logger.info("Failed to find reCaptcha key -> Plugin broken or no captcha required");
            }
            final Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final UrlQuery query = new UrlQuery();
            query.add("token", fid);
            if (recaptchaV2Response != null) {
                query.add("secureToken", Encoding.urlEncode(recaptchaV2Response));
            }
            query.add("trim", "");
            br2.postPage("/gallery/downloadtoken", query);
            final Map<String, Object> entries = restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
            final String cdn = entries.get("cdn").toString();
            final String token = entries.get("token").toString();
            dlform.setAction(br._getURL().getProtocol() + "://" + cdn + "/");
            dlform.put("downloadToken", Encoding.urlEncode(token));
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlform, false, 1);
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
            dl.startDownload();
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        /* 2022-11-18: Only premium users can download files. */
        if (account != null && account.getType() == AccountType.PREMIUM) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* 2023-11-28: Even premium users need to solve an invisible reCaptcha captcha. */
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}