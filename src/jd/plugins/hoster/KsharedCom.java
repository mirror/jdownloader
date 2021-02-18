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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KsharedCom extends PluginForHost {
    public KsharedCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.kshared.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www.kshared.com/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "kshared.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/file/([A-Za-z0-9]+)(/([^/]+))?");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean       FREE_RESUME                  = true;
    private final int           FREE_MAXCHUNKS               = 0;
    private final int           FREE_MAXDOWNLOADS            = 20;
    private final boolean       ACCOUNT_FREE_RESUME          = true;
    private final int           ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean       ACCOUNT_PREMIUM_RESUME       = true;
    private final int           ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int           ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private static final String PROPERTY_PREMIUMONLY         = "premiumonly";
    private static final String PROPERTY_ACCOUNT_TOKEN       = "access_token";
    private static final String PROPERTY_ACCOUNT_UT          = "ut";
    private static final String PROPERTY_ACCOUNT_UD          = "ud";

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

    private String getFallbackFilename(final DownloadLink link) {
        final String filenameURL = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
        if (filenameURL != null) {
            return filenameURL;
        } else {
            return this.getFID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        findAndSetBearerToken();
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("ud", null);
        data.put("ut", null);
        data.put("fileid", this.getFID(link));
        br.postPageRaw("https://www." + this.getHost() + "/v1/drive/get_download", JSonStorage.serializeToJson(data));
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if (entries == null) {
            /* 2021-02-18: Returns broken json for offline items */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (Map<String, Object>) entries.get("file");
        // final Object errorO = entries.get("error");
        // if (errorO != null) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        final String filename = (String) entries.get("name");
        final long filesize = ((Number) entries.get("size")).longValue();
        final String lockedStatus = (String) entries.get("locked");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        } else if (!link.isNameSet()) {
            link.setName(getFallbackFilename(link));
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        if (lockedStatus.equalsIgnoreCase("premium")) {
            link.setProperty(PROPERTY_PREMIUMONLY, true);
        } else {
            link.removeProperty(PROPERTY_PREMIUMONLY);
        }
        return AvailableStatus.TRUE;
    }

    private boolean isPremiumonly(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_PREMIUMONLY, false);
    }

    private String findAndSetBearerToken() {
        final String hash = br.getRegex("hash\\s*:\\s*\"([^\"]+)\"").getMatch(0);
        if (hash != null) {
            br.getHeaders().put("Authorization", "Bearer " + hash);
        }
        return hash;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (isPremiumonly(link)) {
                throw new AccountRequiredException();
            }
            /* TODO */
            /*
             * 2021-02-18: Seems like this filehost is so new they didn't even yet develop the free download process... (can be set in
             * premium accounts but all files are displayed as premiumonly nontheless!)
             */
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* 2021-02-18: Unfinished plugin */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = br.getRegex("").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
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
                    return dllink;
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                String accessToken = this.accountGetAccessToken(account);
                String ut = this.accountGetUT(account);
                String ud = this.accountGetUD(account);
                Map<String, Object> data2 = new HashMap<String, Object>();
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && accessToken != null && ut != null && ud != null) {
                    logger.info("Attempting token login");
                    this.br.setCookies(this.getHost(), cookies);
                    this.br.getHeaders().put("Authorization", "Bearer " + accessToken);
                    br.getHeaders().put("Origin", "https://www.kshared.com");
                    br.getHeaders().put("Referer", "https://www.kshared.com/drive");
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    data2.put("ud", ud);
                    data2.put("ut", ut);
                    br.postPageRaw("https://www." + this.getHost() + "/v1/account/is_user", JSonStorage.serializeToJson(data2));
                    boolean success = false;
                    try {
                        this.handleErrors(null, account);
                        success = true;
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                    if (success) {
                        logger.info("Token login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Token login failed");
                        br.clearAll();
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/account/signin");
                findAndSetBearerToken();
                final Map<String, Object> data = new HashMap<String, Object>();
                data.put("email", account.getUser());
                data.put("passw", account.getPass());
                br.postPageRaw("/v1/account/signin", JSonStorage.serializeToJson(data));
                this.handleErrors(null, account);
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                // accessToken = (String) entries.get("accesstoken");
                ut = (String) entries.get("ut");
                ud = (String) entries.get("accesstoken");
                if (StringUtils.isEmpty(accessToken) || StringUtils.isEmpty(ut) || StringUtils.isEmpty(ud)) {
                    /* This should never happen */
                    throw new AccountUnavailableException("Unknown failure", 10 * 60 * 1000l);
                }
                data2.clear();
                data2.put("ud", ud);
                data2.put("ut", ut);
                br.getHeaders().put("Authorization", "Bearer " + accessToken);
                br.postPageRaw("/v1/account/is_user", JSonStorage.serializeToJson(data2));
                account.setProperty(PROPERTY_ACCOUNT_TOKEN, accessToken);
                account.setProperty(PROPERTY_ACCOUNT_UT, ut);
                account.setProperty(PROPERTY_ACCOUNT_UD, ud);
                /* No error? Login was successful! */
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private String accountGetAccessToken(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
    }

    private String accountGetUT(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_UT);
    }

    private String accountGetUD(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_UD);
    }

    private void handleErrors(final DownloadLink link, final Account account) throws PluginException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        /** 2021-02-18: In some rare cases "error" can be true WITHOUT "reason" and without "message"! */
        /* TODO */
        final boolean isError = entries.containsKey("error") ? ((Boolean) entries.get("error")).booleanValue() : false;
        if (isError) {
            final String reason = (String) entries.get("reason");
            String message = (String) entries.get("message");
            if (reason == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (reason.equalsIgnoreCase("AccountNotFound") || reason.equalsIgnoreCase("InvalidPassword")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (reason.equalsIgnoreCase("notfound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (reason.equalsIgnoreCase("bandwidth")) {
                throw new AccountUnavailableException("Bandwidth limit reached", 5 * 60 * 1000l);
            } else {
                if (link == null) {
                    throw new AccountUnavailableException("Unknown error: " + reason, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error: " + reason);
                }
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("me");
        final Map<String, Object> spaceInfo = (Map<String, Object>) entries.get("disk");
        final Map<String, Object> trafficInfo = (Map<String, Object>) entries.get("bandwidth");
        final long trafficMax = ((Number) trafficInfo.get("total")).longValue();
        final long trafficUsed = ((Number) trafficInfo.get("used")).longValue();
        ai.setTrafficMax(trafficMax);
        ai.setTrafficLeft(trafficMax - trafficUsed);
        ai.setUsedSpace(((Number) spaceInfo.get("used")).longValue());
        final boolean hasPremium = ((Boolean) entries.get("hasPremium")).booleanValue();
        if (!hasPremium) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            /* E.g. "1 Month" */
            final String proPlan = (String) entries.get("proPlan");
            final long expireTimestamp = JavaScriptEngineFactory.toLong(entries.get("premiumExpires"), 0);
            ai.setValidUntil(expireTimestamp * 1000l, this.br);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account: " + proPlan);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            /* TODO */
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                final Map<String, Object> data = new HashMap<String, Object>();
                data.put("ud", this.accountGetUD(account));
                data.put("ut", this.accountGetUT(account));
                data.put("passw", link.getDownloadPassword());
                data.put("fileid", this.getFID(link));
                br.postPageRaw("https://www." + this.getHost() + "/v1/drive/get_download_link", JSonStorage.serializeToJson(data));
                this.handleErrors(link, account);
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                dllink = (String) entries.get("uri");
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl", 5 * 60 * 1000l);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}