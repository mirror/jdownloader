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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.TeraboxComFolder;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { TeraboxComFolder.class })
public class TeraboxCom extends PluginForHost {
    public TeraboxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.dubox.com/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
    }

    @Override
    public String getAGBLink() {
        return "https://www.dubox.com/";
    }

    private static List<String[]> getPluginDomains() {
        return TeraboxComFolder.getPluginDomains();
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
        // for (final String[] domains : getPluginDomains()) {
        // ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/file/([A-Za-z0-9\\-_]+)");
        // }
        /* No pattern required at all */
        ret.add("");
        return ret.toArray(new String[0]);
    }

    public static final String PROPERTY_DIRECTURL        = "directurl";
    public static final String PROPERTY_PASSWORD_COOKIE  = "password_cookie";
    public static final String PROPERTY_PAGINATION_PAGE  = "pagination_page";
    public static final String PROPERTY_ACCOUNT_JS_TOKEN = "js_token";
    public static final String PROPERTY_ACCOUNT_TOKEN    = "token";

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final Account account) {
        return 0;
    }

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
        try {
            return UrlQuery.parse(link.getPluginPatternMatcher()).get("fsid");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws IOException, PluginException {
        if (account == null) {
            /* Without account we can't generate new directurls and can't check existing directurls! */
            return AvailableStatus.UNCHECKABLE;
        }
        if (link.hasProperty(PROPERTY_PASSWORD_COOKIE)) {
            TeraboxComFolder.setPasswordCookie(this.br, this.getHost(), link.getStringProperty(PROPERTY_PASSWORD_COOKIE));
        }
        try {
            this.login(account, false);
        } catch (final Exception ignore) {
            logger.log(ignore);
            return AvailableStatus.UNCHECKABLE;
        }
        if (checkDirectLink(link, PROPERTY_DIRECTURL) != null) {
            logger.info("Availablecheck via directurl complete");
            return AvailableStatus.TRUE;
        } else {
            /*
             * Crawl the folder again to get a fresh directurl. There is no other way to do this. If the folder is big and the crawler has
             * to go through pagination, this can take a while!
             */
            final PluginForDecrypt decrypter = getNewPluginForDecryptInstance(getHost());
            final CryptedLink param = new CryptedLink(link.getContainerUrl(), link);
            if (link.getDownloadPassword() != null) {
                /* Crawler should not ask user again for that password! */
                param.setDecrypterPassword(link.getDownloadPassword());
            }
            try {
                /* 2021-04-24: Handling has been changed so array should only contain the one element we need! */
                final ArrayList<DownloadLink> items = ((jd.plugins.decrypter.TeraboxComFolder) decrypter).crawlFolder(this, param, account, this.getFID(link));
                DownloadLink target = null;
                for (final DownloadLink tmp : items) {
                    if (StringUtils.equals(this.getFID(tmp), this.getFID(link))) {
                        target = tmp;
                        break;
                    }
                }
                /* Assume that item is offline (deleted within folder). */
                if (target == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (!target.hasProperty(PROPERTY_DIRECTURL)) {
                    logger.warning("Failed to refresh directurl");
                    return AvailableStatus.UNCHECKABLE;
                } else {
                    logger.info("Successfully refreshed directurl");
                    link.setProperty(PROPERTY_DIRECTURL, target.getStringProperty(PROPERTY_DIRECTURL));
                    return AvailableStatus.TRUE;
                }
            } catch (final Exception e) {
                return AvailableStatus.UNCHECKABLE;
            }
        }
    }

    public static AvailableStatus parseFileInformation(final DownloadLink link, final Map<String, Object> entries) throws IOException, PluginException {
        final String filename = (String) entries.get("server_filename");
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
        final String md5 = (String) entries.get("md5");
        /* Typically only available when user is logged in. */
        final String directurl = (String) entries.get("dlink");
        // final String fsidStr = Long.toString(JavaScriptEngineFactory.toLong(entries.get("fs_id"), -1));
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        if (!StringUtils.isEmpty(md5)) {
            link.setMD5Hash(md5);
        }
        if (!StringUtils.isEmpty(directurl)) {
            link.setProperty(PROPERTY_DIRECTURL, directurl);
        }
        link.setAvailable(true);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null);
        throw new AccountRequiredException();
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
        return -1;
    }

    public AccountInfo login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Cookies userCookies = account.loadUserCookies();
            if (userCookies == null || userCookies.isEmpty()) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            setCookies(userCookies);
            if (!force) {
                return null;
            }
            // boolean userHasAddedCookiesFromInternalMainDomain = false;
            // for (final Cookie cookie : userCookies.getCookies()) {
            // if (cookie.getHost().equals(this.getHost())) {
            // userHasAddedCookiesFromInternalMainDomain = true;
            // break;
            // }
            // }
            logger.info("Performing full user-cookie login");
            br.getPage("https://www." + this.getHost() + "/disk/home");
            final String bdstoken = br.getRegex("\"bdstoken\":\"([a-f0-9]{32})\"").getMatch(0);
            if (bdstoken == null) {
                errorAccountInvalid(account);
            }
            account.setProperty(PROPERTY_ACCOUNT_TOKEN, bdstoken);
            /* Try to find additional account information */
            final AccountInfo ai = new AccountInfo();
            final Browser brc = br.cloneBrowser();
            brc.getPage("/rest/2.0/membership/proxy/user?method=query&membership_version=1.0&channel=dubox&web=1&app_id=250528&clienttype=0&bdstoken=" + bdstoken);
            /* 2021-04-14: Only free accounts are existent/supported */
            final Map<String, Object> root = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Number error_code = (Number) root.get("error_code");
            if (error_code != null && error_code.intValue() != 0) {
                /*
                 * Assume that cookies are invalid. When user logs out in browser it may happen that we can still get a token via html but
                 * the token is invalid.
                 */
                /* E.g. {"error_code":100003,"error_msg":"Invalid Bduss","request_id":"<Number>"} */
                errorAccountInvalid(account);
            }
            final Map<String, Object> data = (Map<String, Object>) root.get("data");
            final Map<String, Object> member_info = (Map<String, Object>) data.get("member_info");
            final Number is_vip = ((Number) member_info.get("is_vip"));
            if (is_vip != null && is_vip.intValue() == 1) {
                account.setType(AccountType.PREMIUM);
                final long vip_left_time = ((Number) member_info.get("vip_left_time")).longValue();
                if (vip_left_time > 0) {
                    ai.setValidUntil(System.currentTimeMillis() + vip_left_time * 1000);
                }
            } else {
                account.setType(AccountType.FREE);
            }
            final String jstoken = getJsToken(br, br.getHost());
            if (jstoken != null) {
                account.setProperty(PROPERTY_ACCOUNT_JS_TOKEN, jstoken);
            } else {
                logger.warning("Failed to find jstoken");
                account.removeProperty(PROPERTY_ACCOUNT_JS_TOKEN);
            }
            ai.setUnlimitedTraffic();
            return ai;
        }
    }

    private void setCookies(final Cookies cookies) {
        /* Set given cookies for all domains we know. */
        final List<String[]> domains = TeraboxComFolder.getPluginDomains();
        for (final String[] domainsarray : domains) {
            for (final String domain : domainsarray) {
                br.setCookies(domain, cookies);
            }
        }
    }

    public static String getJsToken(final Browser br, final String host) throws IOException {
        br.getPage("https://www." + host + "/");
        return regexJsToken(br);
    }

    public static String regexJsToken(final Browser br) {
        return br.getRegex("window\\.jsToken%20%3D%20a%7D%3Bfn%28%22([A-F0-9]{128})").getMatch(0);
    }

    private void errorAccountInvalid(final Account account) throws AccountInvalidException {
        account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
        if (account.hasEverBeenValid()) {
            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
        } else {
            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return login(account, true);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        final String dllink;
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            /* Avoid checking seemingly invalid stored directurl again in availablecheck! */
            link.removeProperty(PROPERTY_DIRECTURL);
            this.requestFileInformation(link, account);
            dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to refresh expired directurl", 5 * 60 * 1000l);
            }
        }
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, getDownloadLinkDownloadable(link), br.createGetRequest(dllink), this.isResumeable(link, account), this.getMaxChunks(account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(PROPERTY_DIRECTURL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        /*
         * htmldecode final filename just in case we're using in from Content-Disposition and not the one that was set during the crawl
         * process.
         */
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    private DownloadLinkDownloadable getDownloadLinkDownloadable(final DownloadLink link) {
        final String host = this.getHost();
        /* 2021-04-20: Workaround: Given MD5 values are wrong so let's not use these ones! */
        final DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link) {
            @Override
            public HashInfo getHashInfo() {
                return null;
            }

            @Override
            public String getHost() {
                final DownloadInterface dli = getDownloadInterface();
                if (dli != null) {
                    final URLConnectionAdapter connection = dli.getConnection();
                    if (connection != null) {
                        return connection.getURL().getHost();
                    }
                }
                return host;
            }
        };
        return downloadLinkDownloadable;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* 2021-04-14: No captchas at all */
        return false;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        /* 2021-04-14: Downloads only possible via account */
        if (account != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        // if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
        // link.removeProperty(PROPERTY_DIRECTURL);
        // }
    }
}