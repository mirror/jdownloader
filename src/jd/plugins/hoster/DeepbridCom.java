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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deepbrid.com" }, urls = { "https?://(?:www\\.)?deepbrid\\.com/dl\\?f=([a-f0-9]{32})" })
public class DeepbridCom extends antiDDoSForHost {
    private static final String          API_BASE                   = "https://www.deepbrid.com/backend-dl/index.php";
    private static MultiHosterManagement mhm                        = new MultiHosterManagement("deepbrid.com");
    private static final int             defaultMAXDOWNLOADS        = -1;
    private static final int             defaultMAXCHUNKS           = 0;
    private static final boolean         defaultRESUME              = true;
    private static final String          PROPERTY_ACCOUNT_maxchunks = "maxchunks";

    public DeepbridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.deepbrid.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://www.deepbrid.com/page/terms";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Wrong request code")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename_url = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        String filename = br.getRegex("<b>File Name:?\\s*?</b></font><font[^>]+>([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = filename_url;
        }
        String filesize = br.getRegex("<b>File Size:?\\s*?</b></font><font[^>]+>([^<>\"]+)<").getMatch(0);
        filename = Encoding.htmlDecode(filename).trim();
        link.setName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null && link.getPluginPatternMatcher() != null && new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).matches()) {
            /* Without account itis only possible to download URLs for files which are on the server of this multihost! */
            return true;
        } else if (account == null) {
            /* Without account its not possible to download links from other filehosts */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String directlinkproperty = "directurl";
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            String ticketurl = link.getStringProperty("ticketurl", null);
            if (ticketurl != null) {
                getPage(ticketurl);
            } else {
                final Form dlform = br.getFormbyKey("download");
                if (dlform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                dlform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                final InputField dlfield = dlform.getInputField("download");
                if (dlfield != null && dlfield.getValue() == null) {
                    dlform.put("download", "");
                }
                submitForm(dlform);
                /* Store that URL as we can use it multiple times to generate new directurls for that particular file! */
                link.setProperty("ticketurl", br.getURL());
            }
            dllink = br.getRegex("(https?://[^\"\\']+/dl/[^\"\\']+)").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("href\\s*=\\s*\"(https?://[^\"]+)\"\\s*>\\s*DOWNLOAD NOW\\!").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                if (ticketurl != null) {
                    /* Trash stored ticket-URL and try again! */
                    link.setProperty("ticketurl", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        handleFree(link);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            if (account.getType() == AccountType.PREMIUM) {
                /* Use API in premium mode */
                this.postPage(API_BASE + "?page=api&app=jdownloader&action=generateLink", "pass=&link=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
            } else {
                /* Use website for free account downloads */
                this.postPage(API_BASE + "?page=api&action=generateLink", "pass=&link=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
            }
            dllink = PluginJSonUtils.getJsonValue(br, "link");
            if (StringUtils.isEmpty(dllink)) {
                this.handleKnownErrors(this.br, account, link);
                mhm.handleErrorGeneric(account, link, "dllinknull", 10, 5 * 60 * 1000l);
            }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        int maxchunks = (int) account.getLongProperty(PROPERTY_ACCOUNT_maxchunks, defaultMAXCHUNKS);
        if (maxchunks == 1) {
            maxchunks = 1;
        } else if (maxchunks > 0) {
            maxchunks = -maxchunks;
        } else {
            maxchunks = defaultMAXCHUNKS;
        }
        logger.info("Max. allowed chunks: " + maxchunks);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, maxchunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            handleKnownErrors(this.br, account, link);
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
        }
        try {
            this.dl.startDownload();
        } catch (Exception e) {
            final File part = new File(dl.getDownloadable().getFileOutputPart());
            if (part.exists() && part.length() < 5000) {
                final String content = IO.readFileToString(part);
                if (StringUtils.containsIgnoreCase(content, "VinaGet")) {
                    logger.log(e);
                    logger.info("ServerError workaround: VinaGet");
                    link.setChunksProgress(null);
                    part.delete();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError, retry later!", 15 * 60 * 1000l, e);
                }
            }
            throw e;
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        mhm.runCheck(account, link);
        login(account, false);
        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                final URLConnectionAdapter con = br2.openHeadConnection(dllink);
                try {
                    if (!looksLikeDownloadableContent(con)) {
                        throw new IOException();
                    } else {
                        return dllink;
                    }
                } finally {
                    con.disconnect();
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                return null;
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        login(account, false);
        if (br.getURL() == null || !br.getURL().contains("action=accountInfo")) {
            this.getAPISafe(API_BASE + "?page=api&action=accountInfo", account, null);
        }
        final String is_premium = PluginJSonUtils.getJson(br, "type");
        final String maxDownloadsStr = PluginJSonUtils.getJson(br, "maxDownloads");
        if (!"premium".equalsIgnoreCase(is_premium)) {
            account.setType(AccountType.FREE);
            /*
             * No downloads possible via free account via API. Via website, free downloads are possible but we were too lazy to add extra
             * login support via website in order to allow free account downloads.
             */
            ai.setTrafficLeft(0);
            account.setMaxSimultanDownloads(0);
            // ai.setUnlimitedTraffic();
            /* 2021-01-03: Usually there are 15 Minutes waittime between downloads in free mode -> Do not allow simultaneous downloads */
            // account.setMaxSimultanDownloads(1);
        } else {
            account.setType(AccountType.PREMIUM);
            if (maxDownloadsStr != null && maxDownloadsStr.matches("\\d+")) {
                logger.info("Using API maxdownloads: " + maxDownloadsStr);
                account.setMaxSimultanDownloads(Integer.parseInt(maxDownloadsStr));
            } else {
                logger.info("Using DEFAULT maxdownloads: " + defaultMAXDOWNLOADS);
                account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            }
            final String validuntil = PluginJSonUtils.getJsonValue(br, "expiration");
            ai.setStatus("Premium account");
            /* Correct expire-date - add 24 hours */
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validuntil, "yyyy-MM-dd", Locale.ENGLISH) + 24 * 60 * 60 * 1000, br);
            ai.setUnlimitedTraffic();
        }
        final String maxConnectionsStr = PluginJSonUtils.getJson(br, "maxConnections");
        if (maxConnectionsStr != null && maxConnectionsStr.matches("\\d+")) {
            logger.info("Setting maxchunks value: " + maxConnectionsStr);
            account.setProperty(PROPERTY_ACCOUNT_maxchunks, Long.parseLong(maxConnectionsStr));
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && maxDownloadsStr != null && maxConnectionsStr != null) {
                ai.setStatus(ai.getStatus() + String.format(" | MaxDls: %s MaxCon: %s", maxDownloadsStr, maxConnectionsStr));
            }
        }
        this.getAPISafe(API_BASE + "?page=api&action=hosters", account, null);
        LinkedHashMap<String, Object> entries;
        final ArrayList<Object> supportedhostslistO = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        for (final Object hostO : supportedhostslistO) {
            /* List can be given in two different varieties */
            if (hostO instanceof LinkedHashMap) {
                entries = (LinkedHashMap<String, Object>) hostO;
                for (final String host : entries.keySet()) {
                    final String isUP = (String) entries.get(host);
                    if (!"up".equalsIgnoreCase(isUP)) {
                        /* Skip hosts which do not work via this MOCH at this moment! */
                        continue;
                    }
                    if (host.equalsIgnoreCase("icerbox")) {
                        /* 2020-05-20: Workaround: https://board.jdownloader.org/showthread.php?t=84429 */
                        supportedhostslist.add("icerbox.com");
                        supportedhostslist.add("icerbox.biz");
                    } else {
                        supportedhostslist.add(host);
                    }
                }
            } else if (hostO instanceof String) {
                supportedhostslist.add((String) hostO);
            }
        }
        try {
            /*
             * Neither their API nor their website contains TLDs which is very bad ... but also their API-List and website list differ so
             * this is another workaround ...
             */
            logger.info("Checking for additional supported hosts on website (API list = unreliable)");
            getPage("/downloader");
            final String[] crippled_hosts = br.getRegex("class=\"hosters_([A-Za-z0-9]+)[^\"]*\"").getColumn(0);
            for (final String crippled_host : crippled_hosts) {
                if (!supportedhostslist.contains(crippled_host)) {
                    logger.info("Adding host from website which has not been given via API: " + crippled_host);
                    supportedhostslist.add(crippled_host);
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
            logger.info("Website-workaround to find additional supported hosts failed");
        }
        account.setConcurrentUsePossible(true);
        // List<String> assigned = ai.setMultiHostSupport(this, supportedhostslist);
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final Account account, final boolean forceFullLogin) throws Exception {
        synchronized (account) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust login cookies as they're not that old");
                        return;
                    }
                    logger.info("Trying to login via cookies");
                    if (isLoggedinAPI()) {
                        /* Save new cookie-timestamp */
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    }
                    logger.info("Cookie login failed --> Full login required");
                }
                logger.info("Attempting full login");
                getPage("https://www." + account.getHoster());
                getPage("https://www." + account.getHoster() + "/login");
                final Form loginform = br.getFormbyProperty("name", "login");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("amember_login", Encoding.urlEncode(account.getUser()));
                loginform.put("amember_pass", Encoding.urlEncode(account.getPass()));
                loginform.put("remember_login", "1");
                if (br.containsHTML("google\\.com/recaptcha/api")) {
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                submitForm(loginform);
                if (!isLoggedinAPI()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedinAPI() throws Exception {
        getPage(API_BASE + "?page=api&action=accountInfo");
        final String username = PluginJSonUtils.getJson(br, "username");
        final boolean loggedInViaCookies = username != null;
        /* Failure would redirect us to /login */
        final boolean urlOk = br.getURL().contains("page=api");
        if (loggedInViaCookies && urlOk) {
            return true;
        } else {
            return false;
        }
    }

    private void getAPISafe(final String accesslink, final Account account, final DownloadLink link) throws Exception {
        getPage(accesslink);
        handleKnownErrors(this.br, account, link);
    }

    private void handleKnownErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        long errorCode = 0;
        String errorMsg = null;
        try {
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            errorCode = JavaScriptEngineFactory.toLong(entries.get("error"), 0);
            errorMsg = (String) entries.get("message");
        } catch (final Throwable e) {
            logger.log(e);
        }
        if (errorCode == 0) {
            /* All ok */
        } else if (errorCode == 1) {
            /* No link entered - this should never happen */
            mhm.handleErrorGeneric(account, link, "api_error_1", 10, 5 * 60 * 1000l);
        } else if (errorCode == 2) {
            /* http:// or https:// required --> Should never happen(?) */
            mhm.handleErrorGeneric(account, link, "api_error_2", 10, 5 * 60 * 1000l);
        } else if (errorCode == 3) {
            /* Link/Host not supported */
            mhm.putError(account, link, 10 * 60 * 1000l, errorMsg);
        } else if (errorCode == 8) {
            /* Account limit reached -> Waittime required */
            /*
             * E.g. {"error":8, "message":"You have already downloaded, wait \u003Cb\u003E 14:11 minutes\u003C\/b\u003E to download again.
             * \u003Ca href=\"..\/signup\" target=\"_blank\"\u003EUpgrade to premium\u003C\/a\u003E and forget waiting times and enjoy
             * unlimited features!" }
             */
            final Regex waittimeRegex = new Regex(errorMsg, ".*You have already downloaded, wait.*(\\d{1,2}):(\\d{1,2}).*minute.*");
            final String waitMinutesStr = waittimeRegex.getMatch(0);
            final String waitSecondsStr = waittimeRegex.getMatch(1);
            if (waitMinutesStr != null && waitSecondsStr != null) {
                throw new AccountUnavailableException(errorMsg, Integer.parseInt(waitMinutesStr) * 60 * 1000l + Integer.parseInt(waitSecondsStr) * 1001l);
            } else {
                throw new AccountUnavailableException(errorMsg, 5 * 60 * 1000l);
            }
        } else if (errorCode == 10) {
            /* Filehoster under maintenance on our site */
            mhm.putError(account, link, 60 * 60 * 1000l, errorMsg);
        } else if (errorCode == 9) {
            /* Hosters limit reached for this day */
            mhm.putError(account, link, 60 * 60 * 1000l, errorMsg);
        } else if (errorCode == 15) {
            /* Service detected usage of proxy which they do not tolerate */
            /*
             * 2020-08-18: E.g. {"error":15, "message":"Proxy, VPN or VPS detected. If you think this is a mistake, please \u003Ca
             * href=\"..\/helpdesk\" target=\"_blank\"\u003Ecreate a support ticket\u003C\/a\u003E requesting to whitelist your account, we
             * will be so happy to assist you!" }
             */
            throw new AccountUnavailableException("Proxy, VPN or VPS detected. Contact deepbrid.com support!", 15 * 60 * 1000l);
        } else {
            /* Unknown error */
            mhm.handleErrorGeneric(account, link, "api_error_unknown", 10, 5 * 60 * 1000l);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}