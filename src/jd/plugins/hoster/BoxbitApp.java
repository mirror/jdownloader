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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "boxbit.app" }, urls = { "" })
public class BoxbitApp extends PluginForHost {
    /** New project of: geragera.com.br */
    private static final String          API_BASE                        = "https://api.boxbit.app";
    private static MultiHosterManagement mhm                             = new MultiHosterManagement("boxbit.app");
    private static final int             defaultMAXDOWNLOADS             = -1;
    private static final int             defaultMAXCHUNKS                = 0;
    private static final boolean         defaultRESUME                   = true;
    private static final String          PROPERTY_userid                 = "userid";
    private static final String          PROPERTY_logintoken             = "token";
    private static final String          PROPERTY_logintoken_valid_until = "token_valid_until";
    private static final String          PROPERTY_directlink             = "directlink";

    @SuppressWarnings("deprecation")
    public BoxbitApp(PluginWrapper wrapper) {
        super(wrapper);
        /* 2021-08-17: Under development */
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            this.enablePremium("https://boxbit.app/!/register");
        }
    }

    @Override
    public String getAGBLink() {
        return "https://boxbit.app/";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("Content-Type", "application/json");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        return account != null;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handlePremium should never get called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + PROPERTY_directlink);
        br.setFollowRedirects(true);
        boolean resume = link.getBooleanProperty("resumable", defaultRESUME);
        int maxChunks = (int) link.getLongProperty("maxchunks", defaultMAXCHUNKS);
        if (dllink == null) {
            this.loginAPI(account, false);
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("link", link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            if (link.getDownloadPassword() != null) {
                postdata.put("password", link.getDownloadPassword());
            }
            br.postPageRaw(API_BASE + "/users/" + account.getStringProperty(PROPERTY_userid) + "/downloader/request-file", JSonStorage.serializeToJson(postdata));
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            dllink = (String) entries.get("TODO");
            if (StringUtils.isEmpty(dllink)) {
                handleErrors(this.br, account, link);
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
            final String maxchunksStr = PluginJSonUtils.getJson(br, "maxchunks");
            final String resumeStr = PluginJSonUtils.getJson(br, "resumeable");
            if (maxchunksStr != null && maxchunksStr.matches("\\d+")) {
                maxChunks = -Integer.parseInt(maxchunksStr);
                link.setProperty("maxchunks", maxChunks);
            }
            if (resumeStr != null && resumeStr.matches("true|false")) {
                resume = Boolean.parseBoolean(resumeStr);
                link.setProperty("resumable", resume);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
        link.setProperty(this.getHost() + PROPERTY_directlink, dl.getConnection().getURL().toString());
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            handleErrors(this.br, account, link);
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 20, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        mhm.runCheck(account, link);
        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new IOException();
                } else {
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
        } else {
            return null;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        loginAPI(account, true);
        if (br.getURL() == null || !br.getURL().contains("/users/")) {
            br.getPage(API_BASE + "/users/" + account.getStringProperty(PROPERTY_userid));
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> user = (Map<String, Object>) entries.get("user");
        final Map<String, Object> subscription = (Map<String, Object>) user.get("subscription");
        List<Map<String, Object>> hosts = null;
        if (subscription.containsKey("current")) {
            setAccountInfo(account, AccountType.FREE);
        } else {
            final Map<String, Object> currentSubscription = (Map<String, Object>) subscription.get("current");
            if ((Boolean) currentSubscription.get("is_active")) {
                setAccountInfo(account, AccountType.PREMIUM);
                final String end_date = currentSubscription.get("end_date").toString();
                ai.setValidUntil(TimeFormatter.getMilliSeconds(end_date, "yyyy-MM-dd'T'HH:mm:ss'.000000Z'", Locale.ENGLISH));
                ai.setStatus("Package: " + currentSubscription.get("package_name"));
                hosts = (List<Map<String, Object>>) currentSubscription.get("filehosts");
            } else {
                /* Expired premium account?? */
                setAccountInfo(account, AccountType.FREE);
            }
        }
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        final PluginFinder finder = new PluginFinder();
        for (final Map<String, Object> hostInfo : hosts) {
            final Map<String, Object> hostDetails = (Map<String, Object>) hostInfo.get("details");
            /* TODO: Ask admin to return full domain names with TLD! */
            final String host = (String) user.get("identifier");
            if (hostDetails.get("status").toString().equalsIgnoreCase("working")) {
                final String originalHost = finder.assignHost(host);
                if (originalHost == null) {
                    /* This should never happen */
                    continue;
                } else {
                    supportedhostslist.add(originalHost);
                }
            } else {
                logger.info("Skipping non working host: " + host);
            }
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void setAccountInfo(final Account account, final AccountType type) {
        if (type == AccountType.PREMIUM) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
        } else {
            account.setType(type);
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
        }
    }

    private void loginAPI(final Account account, final boolean forceAuthCheck) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            this.prepBR(this.br);
            String token = account.getStringProperty(PROPERTY_logintoken);
            final String userid = account.getStringProperty(PROPERTY_userid);
            Map<String, Object> entries;
            if (token != null && userid != null) {
                logger.info("Attempting token login");
                br.getHeaders().put("Authorization", "Bearer " + token);
                if (!forceAuthCheck) {
                    /* We trust our token --> Do not check them */
                    logger.info("Trust login token without check");
                    return;
                } else {
                    /* TODO: Add token refresh */
                    br.getPage(API_BASE + "/users/" + userid);
                    this.handleErrors(this.br, account, getDownloadLink());
                    entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                }
            }
            logger.info("Performing full login");
            br.postPageRaw(API_BASE + "/auth/login", "{\"email\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() + "\"}");
            this.handleErrors(this.br, account, getDownloadLink());
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> auth = (Map<String, Object>) entries.get("auth");
            token = auth.get("access_token").toString();
            account.setProperty(PROPERTY_logintoken, token);
            account.setProperty(PROPERTY_logintoken_valid_until, System.currentTimeMillis() + ((Number) auth.get("expires_in")).longValue() * 1000);
            account.setProperty(PROPERTY_userid, JavaScriptEngineFactory.walkJson(entries, "user/uuid").toString());
            /* We don't really need the cookies but the timestamp ;) */
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private void handleErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String message = (String) entries.get("message");
        if (message != null) {
            /* TODO: Check/add some more errorcases */
            if (message.equalsIgnoreCase("Unauthenticated.")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                if (link == null) {
                    throw new AccountUnavailableException(message, 5 * 60 * 1000l);
                } else {
                    mhm.handleErrorGeneric(account, link, message, 50);
                }
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}