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
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ddebrid.com" }, urls = { "" })
public class DdebridCom extends PluginForHost {
    private static final String          API_BASE            = "https://ddebrid.com/api";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("ddebrid.com");
    private static final int             defaultMAXDOWNLOADS = -1;
    private static final int             defaultMAXCHUNKS    = 0;
    private static final boolean         defaultRESUME       = true;
    private static final String          PROPERTY_logintoken = "token";
    private static final String          PROPERTY_directlink = "directlink";

    @SuppressWarnings("deprecation")
    public DdebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://ddebrid.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://ddebrid.com/support";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
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
            final String token = account.getStringProperty(PROPERTY_logintoken);
            br.getPage(API_BASE + "/download?token=" + token + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            dllink = PluginJSonUtils.getJsonValue(br, "link");
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
        final String token = account.getStringProperty(PROPERTY_logintoken);
        if (br.getURL() == null || !br.getURL().contains("/info")) {
            br.getPage(API_BASE + "/info?token=" + token);
            handleErrors(br, account, null);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object traffic_leftO = entries.get("traffic_left");
        boolean is_premium = "Premium".equalsIgnoreCase((String) entries.get("type"));
        if (!is_premium) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            /* TODO: Implement traffic_left */
            final long timestampValidUntil = JavaScriptEngineFactory.toLong(entries.get("timestamp"), 0);
            if (timestampValidUntil > 0) {
                ai.setValidUntil(timestampValidUntil * 1000l, this.br);
            }
        }
        if (traffic_leftO != null && traffic_leftO instanceof String) {
            ai.setUnlimitedTraffic();
        } else if (traffic_leftO != null && traffic_leftO instanceof Long) {
            ai.setTrafficLeft((Long) traffic_leftO);
        } else {
            logger.info("Failed to find any processable traffic_left value");
        }
        br.getPage(API_BASE + "/status");
        entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("result");
        final PluginFinder finder = new PluginFinder();
        for (final Object hostO : ressourcelist) {
            entries = (Map<String, Object>) hostO;
            final String host = (String) entries.get("host");
            if (StringUtils.isEmpty(host)) {
                /* This should never happen */
                continue;
            }
            final String status = (String) entries.get("status");
            if (!"online".equalsIgnoreCase(status)) {
                logger.info("Skipping currently unavailable host: " + host);
                continue;
            }
            /* Additionally available: daily_limit */
            // final boolean resumeable = ((Boolean) entries.get("resumeable")).booleanValue();
            final String originalHost = finder.assignHost(host);
            if (originalHost == null) {
                /* This should never happen */
                continue;
            }
            supportedhostslist.add(originalHost);
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginAPI(final Account account, final boolean forceAuthCheck) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            String token = account.getStringProperty(PROPERTY_logintoken);
            if (token != null) {
                logger.info("Attempting token login");
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !forceAuthCheck) {
                    /* We trust our token --> Do not check them */
                    logger.info("Trust login token as it is not that old");
                    return;
                }
                br.getPage(API_BASE + "/info?token=" + token);
                final String status = PluginJSonUtils.getJson(br, "status");
                if ("success".equalsIgnoreCase(status)) {
                    logger.info("Token login successful");
                    /* We don't really need the cookies but the timestamp ;) */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    /* 2020-11-16: E.g. {"status":"error","reason":"Token not found."} */
                    logger.info("Token login failed");
                    this.br.clearAll();
                }
            }
            logger.info("Performing full login");
            if (StringUtils.isEmpty(account.getUser()) || !account.getUser().matches(".+@.+\\..+")) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            this.prepBR(this.br);
            br.getPage(API_BASE + "/login?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            token = PluginJSonUtils.getJson(br, "token");
            if (StringUtils.isEmpty(token)) {
                handleErrors(br, account, null);
                /* This should never happen - do not permanently disable accounts for unexpected login errors! */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unknown login failure", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            account.setProperty(PROPERTY_logintoken, token);
            /* We don't really need the cookies but the timestamp ;) */
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private void handleErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String status = (String) entries.get("status");
        if (!"success".equalsIgnoreCase(status)) {
            String errormsg = (String) entries.get("reason");
            if (StringUtils.isEmpty(errormsg)) {
                errormsg = "Unknown error";
            }
            if (errormsg.equalsIgnoreCase("Wrong credentials")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (errormsg.equalsIgnoreCase("Token not found.")) {
                /* Existing session expired. */
                throw new AccountUnavailableException(errormsg, 1 * 60 * 1000l);
            } else {
                if (link == null) {
                    throw new AccountUnavailableException(errormsg, 5 * 60 * 1000l);
                } else {
                    mhm.handleErrorGeneric(account, link, errormsg, 50);
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