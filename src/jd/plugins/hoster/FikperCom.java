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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FikperCom extends PluginForHost {
    public FikperCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://fikper.com/register");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.API_KEY_LOGIN };
    }

    @Override
    public String getAGBLink() {
        return "https://fikper.com/terms-of-use";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fikper.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)(/([^/]+)(\\.html)?)?");
        }
        return ret.toArray(new String[0]);
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

    public int getMaxChunks(final Account account) {
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

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getFiletitleFromURL(final DownloadLink link) {
        final String title = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
        if (title != null) {
            return title.replaceFirst("(?i)\\.html$", "");
        } else {
            return null;
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        final String titleFromURL = getFiletitleFromURL(link);
        if (titleFromURL != null) {
            return titleFromURL;
        } else {
            return this.getFID(link);
        }
    }

    private final String API_BASE = "https://sapi.fikper.com/";

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        /* Mass-linkchecking via API is only possible for premium users. */
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account == null) {
            /* No mass linkchecking possible */
            return false;
        }
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            this.login(account, false);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* 2023-02-01: Send max 100 items at once RE: admin */
                    if (index == urls.length || links.size() == 100) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                final List<String> urlstocheck = new ArrayList<String>();
                for (final DownloadLink link : links) {
                    urlstocheck.add(link.getPluginPatternMatcher());
                }
                final Map<String, Object> postdata = new HashMap<String, Object>();
                postdata.put("links", urlstocheck);
                br.postPageRaw(API_BASE + "api/file/check-links", JSonStorage.serializeToJson(postdata));
                this.checkErrorsAPI(br, null, account, null);
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
                for (final DownloadLink link : links) {
                    if (!link.isNameSet()) {
                        link.setName(this.getWeakFilename(link));
                    }
                    final String fileID = this.getFID(link);
                    Map<String, Object> finfo = null;
                    for (final Map<String, Object> ressource : ressourcelist) {
                        final String fileIDTmp = (String) ressource.get("fileHashName");
                        if (StringUtils.equals(fileIDTmp, fileID)) {
                            finfo = ressource;
                            break;
                        }
                    }
                    if (finfo == null) {
                        /* This should never happen! */
                        link.setAvailable(false);
                    } else {
                        final Object error = finfo.get("error");
                        final String filename = (String) finfo.get("fileName");
                        final String filesize = StringUtils.valueOfOrNull(finfo.get("fileSize"));
                        if (!StringUtils.isEmpty(filename)) {
                            link.setFinalFileName(filename);
                        }
                        if (filesize != null && filesize.matches("\\d+")) {
                            /* 2023-01-31: Sometimes they're returning numbers as strings lol */
                            // 2023-02-02: According to the admin this has been fixed.
                            link.setVerifiedFileSize(Long.parseLong(filesize));
                        }
                        if (error != null) {
                            /* E.g. {"fileHashName":"filehash","error":404,"message":"Not found"} */
                            link.setAvailable(false);
                        } else {
                            link.setAvailable(true);
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        requestFileInformation(link, null);
        return AvailableStatus.TRUE;
    }

    private Map<String, Object> requestFileInformation(final DownloadLink link, final Account account) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getWeakFilename(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPageRaw(API_BASE, "{\"fileHashName\":\"" + this.getFID(link) + "\"}");
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            /* {"code":404,"message":"The file might be deleted."} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = this.checkErrorsAPI(br, link, account);
        String filename = (String) entries.get("name");
        String filesize = StringUtils.valueOfOrNull(entries.get("size"));
        final String message = (String) entries.get("message");
        if (filename == null && filesize == null && message != null && message.startsWith("{")) {
            /**
             * 2023-06-23: Workaround for very strange API response: </br>
             * {"code":403,"message":"{\"message\":\"To download this file you should be premium
             * user.\",\"data\":{\"name\":\"filetitle.rar\",\"size\":\"123456\",\"mimeType\":\"application/x-rar-compressed\"}}"} </br>
             * Devs: see https://board.jdownloader.org/showthread.php?t=93766
             */
            final Map<String, Object> entries2 = restoreFromString(message, TypeRef.MAP);
            final Map<String, Object> data = (Map<String, Object>) entries2.get("data");
            filename = (String) data.get("name");
            filesize = StringUtils.valueOfOrNull(data.get("size"));
            /* Fix map for later errorhandling. */
            if (!entries2.containsKey("code")) {
                entries2.put("code", entries.get("code"));
            }
            entries = entries2;
        }
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (filesize != null && filesize.matches("\\d+")) {
            link.setVerifiedFileSize(Long.parseLong(filesize));
        }
        final Boolean pwProtectedStatus = (Boolean) entries.get("password");
        if (pwProtectedStatus != null) {
            link.setPasswordProtected(pwProtectedStatus.booleanValue());
        }
        return entries;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (attemptStoredDownloadurlDownload(link, account)) {
            logger.info("Re-using previously generated directurl");
        } else {
            String dllink = null;
            if (account != null && AccountType.PREMIUM.equals(account.getType())) {
                this.login(account, false);
                br.getPage(API_BASE + "api/file/download/" + Encoding.urlEncode(this.getFID(link)));
                this.checkErrorsAPI(br, link, account, null);
                dllink = br.getRequest().getHtmlCode();
            } else {
                // if (account != null) {
                // this.login(account, false);
                // }
                final Map<String, Object> entries = requestFileInformation(link, account);
                if (link.isPasswordProtected()) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected items are not yet supported");
                }
                final long fileSize = link.getVerifiedFileSize();
                if (fileSize >= SIZEUNIT.GB.to(SIZEUNIT.B, 2)) {
                    throw new AccountRequiredException("You can download files up to 2GB in free mode");
                }
                this.checkErrorsAPI(br, link, account, entries);
                final String remainingDelay = StringUtils.valueOfOrNull(entries.get("remainingDelay"));
                if (remainingDelay != null) {
                    /* Downloadlimit has been reached */
                    final int limitWaitSeconds = Integer.parseInt(remainingDelay);
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, limitWaitSeconds * 1000l);
                }
                final long timeBefore = Time.systemIndependentCurrentJVMTimeMillis();
                final int waitBeforeDownloadMillis = ((Number) entries.get("delayTime")).intValue();
                final boolean skipPreDownloadWaittime = true; // 2022-11-14: Wait time is skippable
                final boolean useHcaptcha = true;
                /* 2024-05-03: Solvemedia captcha is broken serverside so fikper.com switched back to hCaptcha. */
                final boolean useSolvemediaCaptcha = false;
                final Map<String, Object> postdata = new HashMap<String, Object>();
                if (useHcaptcha) {
                    /* 2023-01-16 */
                    final CaptchaHelperHostPluginHCaptcha hCaptcha = getHcaptchaHelper(br);
                    final String hCaptchaResponse = hCaptcha.getToken();
                    postdata.put("captchaValue", hCaptchaResponse);
                } else if (useSolvemediaCaptcha) {
                    /* 2023-07-27 */
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    sm.setChallengeKey("U0PGYjYQo61wWfWxQ43vpsJrUQSpCiuY");
                    File cf = null;
                    try {
                        cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    } catch (final InterruptedException e) {
                        throw e;
                    } catch (final Exception e) {
                        if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support", -1, e);
                        } else {
                            throw e;
                        }
                    }
                    final String code = getCaptchaCode("solvemedia", cf, link);
                    final String chid = sm.getChallenge(code);
                    postdata.put("captchaValue", code);
                    postdata.put("challenge", chid);
                } else {
                    /* Old handling */
                    final String recaptchaV2Response = getRecaptchaHelper(br).getToken();
                    postdata.put("recaptcha", recaptchaV2Response);
                }
                final long passedTimeDuringCaptcha = Time.systemIndependentCurrentJVMTimeMillis() - timeBefore;
                postdata.put("fileHashName", this.getFID(link));
                postdata.put("downloadToken", entries.get("downloadToken").toString());
                final long waitBeforeDownloadMillisLeft = waitBeforeDownloadMillis - passedTimeDuringCaptcha;
                if (waitBeforeDownloadMillisLeft > 0 && !skipPreDownloadWaittime) {
                    this.sleep(waitBeforeDownloadMillisLeft, link);
                }
                br.postPageRaw(API_BASE, JSonStorage.serializeToJson(postdata));
                final Map<String, Object> dlresponse = this.checkErrorsAPI(br, link, account);
                final String filesize = StringUtils.valueOfOrNull(dlresponse.get("size"));
                if (filesize != null && filesize.matches("\\d+")) {
                    link.setVerifiedFileSize(Long.parseLong(filesize));
                }
                dllink = dlresponse.get("directLink").toString();
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), getMaxChunks(account));
            /* Save directurl even if it is "invalid" -> E.g. if error 429 happens we might be able to use the same URL later. */
            link.setProperty(getDirectlinkproperty(account), dl.getConnection().getURL().toString());
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                checkError429(dl.getConnection());
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl.startDownload();
    }

    private void checkError429(final URLConnectionAdapter con) throws PluginException {
        if (con.getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Error 429 too many connections", 30 * 1000l);
        }
    }

    private CaptchaHelperHostPluginRecaptchaV2 getRecaptchaHelper(final Browser br) {
        final String host = br.getHost(false);
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Ley0XQeAAAAAK-H0p0T_zeun7NnUgMcLFQy0cU3") {
            @Override
            protected String getSiteUrl() {
                /* We are on subdomain.fikper.com but captcha needs to be solved on fikper.com. */
                return br._getURL().getProtocol() + "://" + host;
            }
        };
    }

    private CaptchaHelperHostPluginHCaptcha getHcaptchaHelper(final Browser br) {
        final String host = br.getHost(false);
        return new CaptchaHelperHostPluginHCaptcha(this, br, "ddd70c6f-e4cb-45e2-9374-171fbc0d4137") {
            @Override
            protected String getSiteUrl() {
                /* We are on subdomain.fikper.com but captcha needs to be solved on fikper.com. */
                return br._getURL().getProtocol() + "://" + host;
            }
        };
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String property = getDirectlinkproperty(account);
        final String url = link.getStringProperty(property);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        boolean throwException = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, account), this.getMaxChunks(account));
            try {
                checkError429(dl.getConnection());
            } catch (final PluginException error429) {
                /* URL is valid but we can't use it right now. */
                valid = true;
                throwException = true;
                throw error429;
            }
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Exception e) {
            if (throwException) {
                throw e;
            } else {
                logger.log(e);
                return false;
            }
        } finally {
            if (!valid) {
                link.removeProperty(property);
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* 2024-02-02: New value = 1, see: https://board.jdownloader.org/showthread.php?t=95116 */
        return 1;
    }

    /** Using API: https://sapi.fikper.com/api/reference/ */
    private Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            prepareBrowserAPI(br, account);
            if (!force) {
                return null;
            }
            br.getPage(API_BASE + "api/account");
            final Map<String, Object> entries = checkErrorsAPI(br, null, account);
            return entries;
        }
    }

    private Map<String, Object> checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        try {
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            checkErrorsAPI(br, link, account, entries);
            return entries;
        } catch (final JSonMapperException e) {
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid API response", 60 * 1000l);
            } else {
                throw new AccountUnavailableException("Invalid API response", 60 * 1000);
            }
        }
    }

    private void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account, final Map<String, Object> entries) throws PluginException {
        if (entries == null) {
            /* We can only check for response-code errors */
            if (br.getHttpConnection().getResponseCode() == 401) {
                throw new AccountInvalidException("401 Invalid API key");
            }
            return;
        }
        final String codeStr = StringUtils.valueOfOrNull(entries.get("code"));
        if (codeStr == null) {
            return;
        } else if (!codeStr.matches("\\d+")) {
            logger.warning("Errorcode is not a number but: " + codeStr);
            return;
        }
        final int code = Integer.parseInt(codeStr);
        if (code == 200) {
            /* No error */
            return;
        }
        long waitMillisOnRetry = 1 * 60 * 1000;
        Map<String, Object> msgmap = null;
        final String message = StringUtils.valueOfOrNull(entries.get("message"));
        if (message != null && message.startsWith("{")) {
            /* json inside json */
            msgmap = restoreFromString(message, TypeRef.MAP);
            final Object remainingDelay = msgmap.get("remainingDelay");
            if (remainingDelay != null && remainingDelay.toString().matches("\\d+")) {
                final long waitMillisOnRetryTmp = Long.parseLong(remainingDelay.toString());
                if (waitMillisOnRetryTmp >= 60 * 1000) {
                    waitMillisOnRetry = waitMillisOnRetryTmp;
                }
            }
        }
        if (code == 401) {
            throw new AccountInvalidException("401 Invalid API key");
        } else if (code == 404) {
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else if (code == 405) {
            /* 2024-02-02: This may happen when we try to start too many [free] downloads at the same time */
            /* E.g. {"code":405,"message":"{\"delay\":7200000,\"remainingDelay\":\"7189\"}"} */
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting more downloads", waitMillisOnRetry);
            }
        }
        if (message != null) {
            if (message.matches("(?i)File size limit")) {
                // You can download files up to 2GB in free mode.
                // {"code":403,"message":"File size limit"}
                throw new AccountRequiredException("(?i)You can download files up to 2GB in free mode");
            } else if (message.matches("(?i)Invalid captcha")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else if (message.matches("(?i)Bandwidth limit")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
        }
        /* Last resort */
        final String lastResortErrormsg = "Code:" + codeStr + "|Message:" + message;
        /* Last resort for unknown errormessages. */
        if (link == null) {
            throw new AccountUnavailableException(lastResortErrormsg, waitMillisOnRetry);
        } else {
            throw new PluginException(LinkStatus.ERROR_FATAL, lastResortErrormsg);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> user = login(account, true);
        /* User can enter whatever he wants into username field -> Correct that as we want to have unique usernames. */
        account.setUser(user.get("email").toString());
        ai.setUsedSpace(((Number) user.get("usedSpace")).longValue());
        final long trafficUsed = ((Number) user.get("usedBandwidth")).longValue();
        final long traffixMax = ((Number) user.get("totalBandwidth")).longValue();
        ai.setTrafficLeft(traffixMax - trafficUsed);
        ai.setTrafficMax(traffixMax);
        final String expireDate = (String) user.get("premiumExpire");
        if (StringUtils.isEmpty(expireDate)) {
            /* Free users can't use the API */
            ai.setExpired(true);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH));
            account.setType(AccountType.PREMIUM);
            return ai;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public String getDirectlinkproperty(final Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return "free_directurl";
        } else {
            return "account_ " + acc.getType() + "_directurl";
        }
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
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

    public static Browser prepareBrowserAPI(final Browser br, final Account account) throws Exception {
        if (br == null) {
            return null;
        }
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("x-api-key", account.getPass());
        return br;
    }

    @Override
    protected String getAPILoginHelpURL() {
        return "https://fikper.com/settings/api";
    }

    @Override
    protected boolean looksLikeValidAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[A-Za-z0-9]{20,}")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}