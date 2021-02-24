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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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
import jd.plugins.components.MultiHosterManagement;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debridplanet.com" }, urls = { "" })
public class DebridplanetCom extends PluginForHost {
    private static final String          WEBSITE_BASE = "https://debridplanet.com";
    private static MultiHosterManagement mhm          = new MultiHosterManagement("debridplanet.com");
    private static final boolean         resume       = true;
    private static final int             maxchunks    = -10;

    @SuppressWarnings("deprecation")
    public DebridplanetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(WEBSITE_BASE + "/premium");
    }

    @Override
    public String getAGBLink() {
        return WEBSITE_BASE + "/tos";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void handleDLMultihoster(final Account account, final DownloadLink link, final String dllink) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        mhm.runCheck(account, link);
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            this.loginWebsite(account, false);
            br.setCurrentURL("https://debridplanet.com/debrid.php");
            final UrlQuery query = new UrlQuery();
            query.appendEncoded("urllist", link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            if (link.getDownloadPassword() != null) {
                query.appendEncoded("passe", link.getDownloadPassword());
            } else {
                query.add("passe", "undefined");
            }
            query.add("enable_https", "1");
            query.add("boxlinklist", "0");
            query.add("seckey", "undefined");
            query.add("seckey2", "undefined");
            PostRequest post = br.createPostRequest(WEBSITE_BASE + "/debrider/gen_process_link.php", query);
            post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            post.getHeaders().put("Origin", WEBSITE_BASE);
            br.getPage(post);
            dllink = br.getRegex("\"(https?://[^\"]+/dl\\d*/[^\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("id=\"linklist1\"[^>]*>(https?://[^<>\"]+)").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(https?://[^\"]+/dl\\?download=[^\"]+)\"").getMatch(0);
                }
            }
            if (dllink == null) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        handleDLMultihoster(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
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
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        if (br.getRequest() == null || !br.getURL().contains("/account")) {
            br.getPage("/account");
        }
        ai.setStatus("Premium account");
        final String premiumExpiredate = br.getRegex("until (\\d{2}/\\d{2}/\\d{2})").getMatch(0);
        if (premiumExpiredate == null) {
            account.setType(AccountType.FREE);
            /*
             * 2020-12-08: Free Accounts can download from some hosts (see websites' host list) but we just won't allow free account
             * downloads at all.
             */
            ai.setTrafficLeft(0);
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(premiumExpiredate, "dd/MM/yy", Locale.ENGLISH), br);
            account.setType(AccountType.PREMIUM);
            ai.setUnlimitedTraffic();
        }
        br.getPage("/debrider/gen_status.php");
        final List<HashMap<String, Object>> status = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST_HASHMAP);
        final List<String> supportedHosts = new ArrayList<String>();
        for (Map<String, Object> server : status) {
            final String host = (String) server.get("link");
            if (!"ONLINE".equals(server.get("status"))) {
                logger.info("Skipping offline host: " + host);
            } else {
                final String supported = (String) server.get("supported");
                if ("Supported".equalsIgnoreCase(supported) || "Unstable".equalsIgnoreCase(supported)) {
                    supportedHosts.add(host);
                } else {
                    logger.info("Skipping inactive host: " + host);
                }
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginWebsite(final Account account, final boolean validateCookies) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.getPage(WEBSITE_BASE + "/account");
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Trying to login via cookies");
                    br.setCookies(WEBSITE_BASE, cookies);
                    if (!validateCookies) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    br.getPage(WEBSITE_BASE + "/account");
                    if (this.isLoggedIN()) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage(WEBSITE_BASE + "/login.php");
                final Form loginform = br.getFormbyActionRegex(".*/login.*");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                // loginform.remove("loginformused");
                loginform.put("loginformused", "1");
                br.submitForm(loginform);
                if (!isLoggedIN()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() throws PluginException {
        return br.containsHTML("sitelokaction=logout");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}