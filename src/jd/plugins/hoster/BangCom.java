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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.BangComConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.BangComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BangCom extends PluginForHost {
    public BangCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.bang.com/joinnow");
    }

    @Override
    public String getAGBLink() {
        return "https://www.bang.com/terms-of-service";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "bang.com", "videosz.com" });
        ret.add(new String[] { "eroticavipclub.com" });
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
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    public void setBrowser(Browser br) {
        super.setBrowser(br);
        br.setFollowRedirects(true);
    }

    private final int          ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    public static final String PROPERTY_MAINLINK            = "mainlink";
    public static final String PROPERTY_CONTENT_ID          = "content_id";
    public static final String PROPERTY_QUALITY_IDENTIFIER  = "quality";

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
        return link.getStringProperty(PROPERTY_CONTENT_ID) + "_" + link.getStringProperty(PROPERTY_QUALITY_IDENTIFIER);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        try {
            URLConnectionAdapter con;
            boolean hasAttemptedRefresh = false;
            do {
                final String directurl = link.getPluginPatternMatcher();
                if (isDownload) {
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, true, 0);
                    con = dl.getConnection();
                } else {
                    con = br.openHeadConnection(directurl);
                }
                if (hasAttemptedRefresh) {
                    break;
                }
                if (this.looksLikeDownloadableContent(con)) {
                    break;
                } else {
                    if (!canRefresh(link)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    logger.info("Directurl expired -> Refreshing it");
                    if (account == null) {
                        /* Account required to refresh directurls. */
                        throw new AccountRequiredException();
                    }
                    final BangComCrawler crawler = (BangComCrawler) this.getNewPluginForDecryptInstance(this.getHost());
                    final ArrayList<DownloadLink> results = crawler.crawlVideo(link.getStringProperty(PROPERTY_MAINLINK), account, null);
                    DownloadLink hit = null;
                    crawlLoop: for (final DownloadLink result : results) {
                        if (StringUtils.equals(getFID(link), getFID(result))) {
                            hit = result;
                            break crawlLoop;
                        }
                    }
                    if (hit == null) {
                        logger.warning("Failed to refresh directurl");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    logger.info("Successfully refreshed directurl");
                    link.setProperties(hit.getProperties());
                    hasAttemptedRefresh = true;
                }
            } while (true);
            if (!this.looksLikeDownloadableContent(con)) {
                if (hasAttemptedRefresh) {
                    logger.warning("Fresh directurl is invalid");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                if (hasAttemptedRefresh) {
                    logger.info("Fresh directurl is valid");
                }
                link.setVerifiedFileSize(con.getCompleteContentLength());
                final String serverFilename = Plugin.getFileNameFromHeader(con);
                if (serverFilename != null) {
                    link.setFinalFileName(serverFilename);
                }
            }
        } finally {
            if (!isDownload && dl != null) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                dl = null;
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean canRefresh(final DownloadLink link) {
        final String qualityIdentifier = link.getStringProperty(PROPERTY_QUALITY_IDENTIFIER);
        if (StringUtils.equals(qualityIdentifier, "THUMBNAIL") || StringUtils.equals(qualityIdentifier, "PREVIEW")) {
            return false;
        } else {
            /* Video URLs can expire and need to be refreshed then. */
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        if (this.dl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void login(final Account account, final boolean force) throws Exception {
        login(account, force, "https://www." + this.getHost() + "/");
    }

    public void login(final Account account, final boolean force, final String checkurl) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                final boolean cookieLoginOnly;
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    /* Testing: Allow non-cookie login */
                    cookieLoginOnly = false;
                } else {
                    /* Public/stable release: Allow only cookie login */
                    cookieLoginOnly = true;
                }
                if (userCookies != null || cookies != null) {
                    if (userCookies != null) {
                        br.setCookies(userCookies);
                    } else {
                        br.setCookies(cookies);
                    }
                    logger.info("Attempting user cookie login");
                    br.getPage(checkurl);
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        if (userCookies != null) {
                            if (account.hasEverBeenValid()) {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                            } else {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                            }
                        } else {
                            /* Try full login to refresh cookies */
                            br.clearAll();
                        }
                    }
                }
                if (cookieLoginOnly) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                logger.info("Performing full login");
                br.getPage("https://www." + this.getHost() + "/modal/login");
                final boolean useAjaxHandling = true;
                if (useAjaxHandling) {
                    final String csrftoken = br.getRegex("name=\"_csrf_token\" value=\"([^\"]+)").getMatch(0);
                    final Map<String, Object> postdata = new HashMap<String, Object>();
                    postdata.put("username", account.getUser());
                    postdata.put("password", account.getPass());
                    if (csrftoken != null) {
                        postdata.put("_csrf_token", csrftoken);
                    } else {
                        logger.warning("Failed to find csrftoken -> Login will probably fail");
                    }
                    br.postPageRaw(br.getURL(), JSonStorage.serializeToJson(postdata));
                } else {
                    final Form loginform = br.getFormbyActionRegex(".*/modal/login.*");
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("username", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginform);
                }
                if (!isLoggedin(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("/subscriptions");
        ai.setUnlimitedTraffic();
        /* 2023-01-31: A public API is available but so far is not of any use for us: https://api.bang.com */
        final String userJson = br.getRegex("window\\.user = (\\{.*?\\});\\s").getMatch(0);
        boolean isSubscriptionRunning = br.containsHTML("(?i)>\\s*Click to cancel");
        String email = null;
        if (userJson != null) {
            final Map<String, Object> user = restoreFromString(userJson, TypeRef.MAP);
            final Map<String, Object> accountType = (Map<String, Object>) user.get("accountType");
            if (!isSubscriptionRunning && accountType.get("type").toString().equalsIgnoreCase("paid")) {
                isSubscriptionRunning = true;
            }
            email = (String) user.get("email");
            // final String apiKey = (String)user.get("apiKey");
        } else {
            logger.warning("Failed to find userJson");
        }
        if (!StringUtils.isEmpty(email) && account.loadUserCookies() != null) {
            /*
             * Try to use unique usernames even when users are using cookie login and thus can enter whatever they want into the username
             * field in JDownloader.
             */
            account.setUser(email);
        }
        if (!isSubscriptionRunning) {
            /* Free Accounts got no advantages over using no account at all -> Do not allow the usage of such accounts. */
            ai.setExpired(true);
            return ai;
        } else {
            // TODO: Find expire-date
            account.setType(AccountType.PREMIUM);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* No captchas at all */
        return false;
    }

    @Override
    public Class<? extends BangComConfig> getConfigInterface() {
        return BangComConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}