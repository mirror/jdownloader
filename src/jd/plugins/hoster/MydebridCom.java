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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mydebrid.com" }, urls = { "" })
public class MydebridCom extends antiDDoSForHost {
    /* Documentation: https://api.mydebrid.com/v1/ */
    private static final String                  API_BASE             = "https://api.mydebrid.com/v1";
    private static MultiHosterManagement         mhm                  = new MultiHosterManagement("mydebrid.com");
    private static LinkedHashMap<String, Object> individualHostLimits = new LinkedHashMap<String, Object>();
    private static final int                     defaultMAXDOWNLOADS  = -1;
    private static final int                     defaultMAXCHUNKS     = 1;
    private static final boolean                 defaultRESUME        = true;
    private static final String                  PROPERTY_logintoken  = "token";
    private static final String                  PROPERTY_directlink  = "directlink";

    public MydebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://mydebrid.com/sign-up");
    }

    @Override
    public String getAGBLink() {
        return "https://mydebrid.com/terms-of-service";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never get called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + PROPERTY_directlink);
        if (dllink == null) {
            this.loginAPI(account, false);
            final String token = account.getStringProperty(PROPERTY_logintoken);
            postPage(API_BASE + "/get-download-url", String.format("token=%s&fileUrl=%s", Encoding.urlEncode(token), Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this))));
            dllink = PluginJSonUtils.getJsonValue(br, "downloadUrl");
            if (StringUtils.isEmpty(dllink)) {
                handleErrors(this.br, account, link);
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        boolean resume = defaultRESUME;
        int maxchunks = defaultMAXCHUNKS;
        if (individualHostLimits.containsKey(link.getHost())) {
            final LinkedHashMap<String, Long> limitMap = (LinkedHashMap<String, Long>) individualHostLimits.get(link.getHost());
            resume = limitMap.get("resumable") == 1 ? true : false;
            maxchunks = ((Number) limitMap.get("max_chunks")).intValue();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        link.setProperty(this.getHost() + PROPERTY_directlink, dl.getConnection().getURL().toString());
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        loginAPI(account, true);
        final String token = account.getStringProperty(PROPERTY_logintoken);
        if (br.getURL() == null || !br.getURL().contains("/account-status")) {
            this.postPage(API_BASE + "/account-status", "token=" + Encoding.urlEncode(token));
            handleErrors(br, account, null);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        boolean is_premium = "premium".equalsIgnoreCase((String) entries.get("accountType"));
        /* 2020-05-06: This will usually return "unlimited" */
        // final String trafficleft = (String) entries.get("remainingTraffic");
        if (!is_premium) {
            /* Assume free accounts cannot be used to download anything */
            account.setType(AccountType.FREE);
            ai.setStatus("Free account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            final String expireDate = (String) entries.get("expiryDate");
            if (!StringUtils.isEmpty(expireDate)) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "dd-MM-yyyy", Locale.ENGLISH), this.br);
            }
            ai.setUnlimitedTraffic();
        }
        postPage(API_BASE + "/get-hosts", "token=" + token);
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("hosts");
        final PluginFinder finder = new PluginFinder();
        for (final Object hostO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) hostO;
            final String host = (String) entries.get("name");
            if (StringUtils.isEmpty(host)) {
                /* This should never happen */
                continue;
            }
            long max_chunks = 1;
            long resumable = 0;
            // final long hostTrafficMaxDaily = JavaScriptEngineFactory.toLong(entries.get("dailyLimit"), 0);
            final long hostTrafficLeft = JavaScriptEngineFactory.toLong(entries.get("remaining"), -1);
            if (hostTrafficLeft == 0) {
                logger.info("Skipping host because no traffic left: " + host);
                continue;
            }
            max_chunks = JavaScriptEngineFactory.toLong(entries.get("maxChunks"), 1);
            if (max_chunks <= 0) {
                max_chunks = 1;
            } else if (max_chunks > 1) {
                max_chunks = -max_chunks;
            }
            final boolean canResume = ((Boolean) entries.get("resumable")).booleanValue();
            if (canResume) {
                resumable = 1;
            } else {
                resumable = 0;
            }
            final String originalHost = finder.assignHost(host);
            if (originalHost == null) {
                /* This should never happen */
                logger.info("Skipping host because failed to find supported/original host: " + host);
                continue;
            }
            supportedhostslist.add(originalHost);
            final LinkedHashMap<String, Long> limits = new LinkedHashMap<String, Long>();
            limits.put("max_chunks", max_chunks);
            limits.put("resumable", resumable);
            individualHostLimits.put(originalHost, limits);
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginAPI(final Account account, final boolean forceAuthCheck) throws Exception {
        String token = account.getStringProperty(PROPERTY_logintoken);
        if (token != null) {
            logger.info("Attempting token login");
            /*
             * 2020-03-24: No idea how long their token is supposed to last but I guess it should be permanent because a full login requires
             * a captcha!
             */
            if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l && !forceAuthCheck) {
                /* We trust our token --> Do not check it */
                logger.info("Trust login token as it is not that old");
                return;
            }
            this.postPage(API_BASE + "/account-status", "token=" + Encoding.urlEncode(token));
            if (br.getHttpConnection().getResponseCode() == 200) {
                logger.info("Token login successful");
                /* We don't really need the cookies but the timestamp ;) */
                account.saveCookies(br.getCookies(br.getHost()), "");
                return;
            } else {
                /* Most likely 401 unauthorized */
                logger.info("Token login failed");
                br.clearAll();
            }
        }
        /* Drop previous headers & cookies */
        logger.info("Performing full login");
        br = this.prepBR(br);
        final String postData = String.format("username=%s&password=%s", Encoding.urlEncode(account.getUser()), Encoding.urlEncode(account.getPass()));
        postPage(API_BASE + "/login", postData);
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

    /** Handles errors according to: https://api.mydebrid.com/v1/#errors */
    private void handleErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String errormsg = getErrormessage(br);
        if (!StringUtils.isEmpty(errormsg)) {
            if (errormsg.equalsIgnoreCase("INVALID_CREDENTIALS")) {
                /* Usually goes along with http response 400 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (errormsg.equalsIgnoreCase("TOKEN_EXPIRED") || errormsg.equalsIgnoreCase("INVALID_TOKEN")) {
                /* Existing session expired. */
                throw new AccountUnavailableException(errormsg, 1 * 60 * 1000l);
            } else if (errormsg.equalsIgnoreCase("LIMIT_EXCEEDED")) {
                /* Limit of individual host reached/exceeded */
                mhm.putError(account, link, 5 * 60 * 1000l, errormsg);
            } else if (errormsg.equalsIgnoreCase("HOST_UNAVAILABLE")) {
                mhm.putError(account, link, 5 * 60 * 1000l, errormsg);
            } else if (errormsg.equalsIgnoreCase("FILE_NOT_FOUND")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (errormsg.equalsIgnoreCase("INVALID_URL")) {
                /* This should never happen (?) --> Retry */
                mhm.handleErrorGeneric(account, link, errormsg, 50);
            } else {
                /* Handle all other errors e.g. "MISSING_PARAMS" or "Error processing input" or any other unknown error */
                if (link == null) {
                    /* No DownloadLink available --> Handle as account error */
                    throw new AccountUnavailableException(errormsg, 10 * 60 * 1000l);
                } else {
                    mhm.handleErrorGeneric(account, link, errormsg, 50);
                }
            }
        }
    }

    private String getErrormessage(final Browser br) {
        String errormsg = null;
        try {
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            errormsg = (String) entries.get("error");
        } catch (final Throwable e) {
        }
        return errormsg;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}