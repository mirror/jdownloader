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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deepbrid.com" }, urls = { "https?://(?:www\\.)?deepbrid\\.com/dl\\?f=([a-f0-9]{32})" })
public class DeepbridCom extends PluginForHost {
    private static final String          API_BASE                   = "https://www.deepbrid.com/backend-dl/index.php";
    private static MultiHosterManagement mhm                        = new MultiHosterManagement("deepbrid.com");
    private static final int             defaultMAXDOWNLOADS        = -1;
    private static final int             defaultMAXCHUNKS           = 1;
    private static final boolean         defaultRESUME              = true;
    private static final String          PROPERTY_ACCOUNT_maxchunks = "maxchunks";

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    public DeepbridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.deepbrid.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://www.deepbrid.com/page/terms";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        /* See handleRedirects */
        br.setFollowRedirects(false);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String filename_url = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (!link.isNameSet()) {
            /* Fallback-filename */
            link.setName(filename_url);
        }
        this.setBrowserExclusive();
        getPage(br, link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*Wrong request code")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("(?i)<b>File Name:?\\s*?</b></font><font[^>]+>([^<>\"]+)<").getMatch(0);
        final String filesize = br.getRegex("(?i)<b>File Size:?\\s*?</b></font><font[^>]+>([^<>\"]+)<").getMatch(0);
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
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
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String directlinkproperty = "directurl";
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        String dllink;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            final String ticketurl = link.getStringProperty("ticketurl");
            if (ticketurl != null) {
                logger.info("Re-using stored ticketurl: " + ticketurl);
                getPage(br, ticketurl);
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
                submitForm(br, dlform);
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
                    link.removeProperty("ticketurl");
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        try {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(brc, link, dllink, false, 1);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                brc.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        handleFree(link);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        final String directlinkproperty = this.getHost() + "directlink";
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        String dllink;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            if (account.getType() == AccountType.PREMIUM) {
                /* Use API in premium mode */
                postPage(br, API_BASE + "?page=api&app=jdownloader&action=generateLink", "pass=&link=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
            } else {
                /* Use website for free account downloads */
                postPage(br, API_BASE + "?page=api&action=generateLink", "pass=&link=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
            }
            final Map<String, Object> resp = this.handleErrorsAPI(br, account, link);
            dllink = (String) resp.get("link");
            if (StringUtils.isEmpty(dllink)) {
                /* This should never happen! */
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 10, 20 * 60 * 1000l);
            }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        int maxchunks = account.getIntegerProperty(PROPERTY_ACCOUNT_maxchunks, defaultMAXCHUNKS);
        if (maxchunks == 1) {
            maxchunks = 1;
        } else if (maxchunks > 0) {
            maxchunks = -maxchunks;
        } else {
            maxchunks = defaultMAXCHUNKS;
        }
        logger.info("Max. allowed chunks: " + maxchunks);
        try {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(brc, link, dllink, defaultRESUME, maxchunks);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                brc.followConnection(true);
                handleErrorsAPI(brc, account, link);
                mhm.handleErrorGeneric(account, link, "Unknown download error", 10, 5 * 60 * 1000l);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        try {
            this.dl.startDownload();
        } catch (final Exception e) {
            /* Special errorhandling */
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

    private void getPage(final Browser br, final String url) throws IOException {
        br.getPage(url);
        handleRedirects(br);
    }

    private void postPage(final Browser br, final String url, final String postData) throws IOException {
        br.postPage(url, postData);
        handleRedirects(br);
    }

    private void submitForm(final Browser br, final Form form) throws IOException {
        br.submitForm(form);
        handleRedirects(br);
    }

    /**
     * Special handling as API requests [when user is not logged in] may resul in json response but at the same time return a 302 redirect
     * to a html page which we do not want.
     */
    private void handleRedirects(final Browser br) throws IOException {
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            if (br.getURL().contains(API_BASE) || br.getURL().contains(API_BASE.replaceFirst("www\\.", ""))) {
                logger.info("Ignoring redirect to keep json in browser | Redirect: " + redirect);
            } else {
                br.followRedirect(true);
            }
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        login(account, false);
        handleDL(account, link);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> userinfo = login(account, true);
        final Number points = (Number) userinfo.get("points");
        if (points != null) {
            ai.setPremiumPoints(points.intValue());
        }
        final String type = userinfo.get("type").toString();
        final Number maxSimultaneousDownloads = (Number) userinfo.get("maxDownloads");
        if (!"premium".equalsIgnoreCase(type)) {
            account.setType(AccountType.FREE);
            /*
             * No downloads possible via free account via API. Via website, free downloads are possible but we were too lazy to add extra
             * login support via website in order to allow free account downloads.
             */
            ai.setTrafficLeft(0);
            account.setMaxSimultanDownloads(0);
            // ai.setUnlimitedTraffic();
            /*
             * 2021-01-03: Usually there are 15 Minutes waittime between downloads in free mode -> Do not allow simultaneous downloads or no
             * downloads at all.
             */
            // account.setMaxSimultanDownloads(1);
            ai.setExpired(true); // 2023-07-21
        } else {
            account.setType(AccountType.PREMIUM);
            if (maxSimultaneousDownloads != null) {
                logger.info("Using API maxdownloads: " + maxSimultaneousDownloads);
                account.setMaxSimultanDownloads(maxSimultaneousDownloads.intValue());
            } else {
                logger.info("Using DEFAULT maxdownloads: " + defaultMAXDOWNLOADS);
                account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            }
            final String validuntil = userinfo.get("expiration").toString();
            /* Correct expire-date - add 24 hours */
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validuntil, "yyyy-MM-dd", Locale.ENGLISH) + 24 * 60 * 60 * 1000, br);
            ai.setUnlimitedTraffic();
        }
        final Number maxConnections = (Number) userinfo.get("maxConnections");
        if (maxConnections != null) {
            logger.info("Setting maxchunks value: " + maxConnections);
            account.setProperty(PROPERTY_ACCOUNT_maxchunks, maxConnections.intValue());
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            ai.setStatus(account.getType().getLabel() + " | MaxDls: " + maxSimultaneousDownloads + " MaxCon: " + maxConnections + " | Points: " + points);
        }
        getPage(br, API_BASE + "?page=api&action=hosters");
        final List<Object> supportedhostslistO;
        final Object supportedhostsO = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
        if (supportedhostsO instanceof Map) {
            /*
             * 2023-11-13: A map of string objects called "0", "1" an so on. Basically they are returnign a map of what is supposed to be an
             * array.
             */
            supportedhostslistO = new ArrayList<Object>();
            final Iterator<Entry<String, Object>> iterator = ((Map<String, Object>) supportedhostsO).entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                supportedhostslistO.add(entry.getValue());
            }
        } else {
            supportedhostslistO = (List<Object>) supportedhostsO;
        }
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        for (final Object hostO : supportedhostslistO) {
            /* List can be given in two different varieties */
            if (hostO instanceof Map) {
                final Map<String, Object> entries = (Map<String, Object>) hostO;
                for (final Map.Entry<String, Object> entry : entries.entrySet()) {
                    if (!"up".equalsIgnoreCase((String) entry.getValue())) {
                        /* Skip hosts which do not work via this MOCH at this moment! */
                        continue;
                    }
                    final String[] hosts = entry.getKey().split(",");
                    for (String host : hosts) {
                        if (host.equalsIgnoreCase("icerbox")) {
                            /* 2020-05-20: Workaround: https://board.jdownloader.org/showthread.php?t=84429 */
                            supportedhostslist.add("icerbox.com");
                            supportedhostslist.add("icerbox.biz");
                        } else {
                            supportedhostslist.add(host);
                        }
                    }
                }
            } else if (hostO instanceof String) {
                final String[] hosts = ((String) hostO).split(",");
                for (String host : hosts) {
                    if (host.equalsIgnoreCase("icerbox")) {
                        /* 2020-05-20: Workaround: https://board.jdownloader.org/showthread.php?t=84429 */
                        supportedhostslist.add("icerbox.com");
                        supportedhostslist.add("icerbox.biz");
                    } else {
                        supportedhostslist.add(host);
                    }
                }
            }
        }
        try {
            /*
             * Neither their API nor their website contains TLDs which is very bad ... but also their API-List and website list differ so
             * this is another workaround ...
             */
            logger.info("Checking for additional supported hosts on website (API list = unreliable)");
            getPage(br, "/service");
            final String[] crippled_hosts = br.getRegex("class=\"hosters_([A-Za-z0-9]+)[^\"]*\"").getColumn(0);
            for (final String crippled_host : crippled_hosts) {
                if (!supportedhostslist.contains(crippled_host)) {
                    logger.info("Adding host from website which has not been given via API: " + crippled_host);
                    supportedhostslist.add(crippled_host);
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
            logger.warning("Website-workaround to find additional supported hosts failed");
        }
        account.setConcurrentUsePossible(true);
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private Map<String, Object> login(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            /* Load cookies */
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            if (cookies != null || userCookies != null) {
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                } else {
                    br.setCookies(this.getHost(), userCookies);
                }
                if (!verifyCookies) {
                    /* Do not verify cookies */
                    return null;
                }
                logger.info("Trying to login via usercookies");
                try {
                    final Map<String, Object> userinfo = checkLoginAPI(br, account, true);
                    logger.info("UserCookie login successful");
                    if (userCookies == null) {
                        /* Only save cookies when needed. */
                        account.saveCookies(br.getCookies(br.getHost()), "");
                    }
                    return userinfo;
                } catch (final PluginException e) {
                    if (userCookies != null) {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    } else {
                        logger.info("Cookie login failed --> Full login required");
                        account.clearCookies("");
                    }
                }
            }
            logger.info("Attempting full login");
            getPage(br, "https://www." + account.getHoster());
            getPage(br, "https://www." + account.getHoster() + "/login");
            final Form loginform = br.getFormbyProperty("name", "login");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("amember_login", Encoding.urlEncode(account.getUser()));
            loginform.put("amember_pass", Encoding.urlEncode(account.getPass()));
            loginform.put("remember_login", "1");
            if (br.containsHTML("(?i)google\\.com/recaptcha/api")) {
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            submitForm(br, loginform);
            final Map<String, Object> userinfo = checkLoginAPI(br, account, false);
            if (userinfo == null) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return userinfo;
        }
    }

    private Map<String, Object> checkLoginAPI(final Browser br, final Account account, final boolean setUsernameOnAccount) throws Exception {
        final String urlpart = "?page=api&action=accountInfo";
        getPage(br, API_BASE + urlpart);
        final Map<String, Object> resp = this.handleErrorsAPI(br, account, null);
        final String username = (String) resp.get("username");
        boolean loggedInViaCookies = false;
        if (!StringUtils.isEmpty(username)) {
            loggedInViaCookies = true;
        }
        /* Failure would redirect us to /login */
        final boolean urlOk = br.getURL().contains(urlpart);
        if (loggedInViaCookies && urlOk) {
            if (setUsernameOnAccount) {
                /*
                 * For cookie login user can enter whatever he wants in "username" field. We want unique usernames so user cannot add the
                 * same account twice!
                 */
                account.setUser(username);
            }
            return resp;
        } else {
            throw new AccountInvalidException();
        }
    }

    private Map<String, Object> handleErrorsAPI(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException ignore) {
            /* This should never happen. */
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid API response", 60 * 1000l, ignore);
            } else {
                throw new AccountUnavailableException(ignore, "Invalid API response", 60 * 1000);
            }
        }
        final Number errorCodeO = (Number) entries.get("error");
        if (errorCodeO == null) {
            /* No error */
            return entries;
        }
        final int errorCode = errorCodeO.intValue();
        final String errorMsg = (String) entries.get("message");
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
            mhm.putError(account, link, 5 * 60 * 1000l, errorMsg);
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
            mhm.putError(account, link, 5 * 60 * 1000l, errorMsg);
        } else if (errorCode == 9) {
            /* Hosters limit reached for this day */
            mhm.putError(account, link, 5 * 60 * 1000l, errorMsg);
        } else if (errorCode == 15) {
            /* Service detected usage of proxy which they do not tolerate */
            /*
             * 2020-08-18: E.g. {"error":15, "message":"Proxy, VPN or VPS detected. If you think this is a mistake, please \u003Ca
             * href=\"..\/helpdesk\" target=\"_blank\"\u003Ecreate a support ticket\u003C\/a\u003E requesting to whitelist your account, we
             * will be so happy to assist you!" }
             */
            throw new AccountUnavailableException("Proxy, VPN or VPS detected. Contact " + getHost() + " support!", 15 * 60 * 1000l);
        } else {
            /* Unknown error */
            if (link == null) {
                /* Error happened during login */
                throw new AccountUnavailableException(errorMsg, 5 * 60 * 1000l);
            } else {
                mhm.handleErrorGeneric(account, link, errorMsg, 10, 5 * 60 * 1000l);
            }
        }
        return entries;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}