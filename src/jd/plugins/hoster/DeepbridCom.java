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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deepbrid.com" }, urls = { "https?://(?:www\\.)?deepbrid\\.com/dl\\?f=([a-f0-9]{32})" })
public class DeepbridCom extends antiDDoSForHost {
    private static final String          API_BASE            = "https://www.deepbrid.com/backend-dl/index.php";
    private static final String          NICE_HOST           = "deepbrid.com";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("deepbrid.com");
    private static final String          NICE_HOSTproperty   = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final int             defaultMAXDOWNLOADS = -1;
    private static final int             defaultMAXCHUNKS    = 0;
    private static final boolean         defaultRESUME       = true;
    private static Object                LOCK                = new Object();

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
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null && downloadLink.getPluginPatternMatcher() != null && new Regex(downloadLink.getPluginPatternMatcher(), this.getSupportedLinks()).matches()) {
            /* Without account itis only possible to download URLs for files which are on the server of this multihost! */
            return true;
        } else if (account == null) {
            /* Without account its not possible to download links from other filehosts */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String directlinkproperty = "directurl";
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            String ticketurl = downloadLink.getStringProperty("ticketurl", null);
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
                downloadLink.setProperty("ticketurl", br.getURL());
            }
            dllink = br.getRegex("(https?://[^\"\\']+/dl/[^\"\\']+)").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("href=\"(https?://[^\"]+)\">DOWNLOAD NOW\\!").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                if (ticketurl != null) {
                    /* Trash stored ticket-URL and try again! */
                    downloadLink.setProperty("ticketurl", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        handleFree(link);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            final boolean use_api_for_downloads = true;
            if (use_api_for_downloads) {
                this.postPage(API_BASE + "?page=api&app=jdownloader&action=generateLink", "pass=&link=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
            } else {
                this.postPage(API_BASE + "?page=api&action=generateLink", "pass=&link=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
            }
            dllink = PluginJSonUtils.getJsonValue(br, "link");
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 10, 5 * 60 * 1000l);
            }
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleKnownErrors(this.br, account, link);
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
        login(account, false);
        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
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
        if (!"premium".equalsIgnoreCase(is_premium)) {
            account.setType(AccountType.FREE);
            /* No downloads possible via free account */
            ai.setTrafficLeft(0);
            account.setMaxSimultanDownloads(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            final String validuntil = PluginJSonUtils.getJsonValue(br, "expiration");
            ai.setStatus("Premium account");
            /* Correct expire-date - add 24 hours */
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validuntil, "yyyy-MM-dd", Locale.ENGLISH) + 24 * 60 * 60 * 1000);
            ai.setUnlimitedTraffic();
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
                    supportedhostslist.add(host);
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
            final String[] crippled_hosts = br.getRegex("hosters\\-icons/([a-z0-9\\-]+)\\.png").getColumn(0);
            for (final String crippled_host : crippled_hosts) {
                if (!supportedhostslist.contains(crippled_host)) {
                    logger.info("Adding host from website which has not been given via API: " + crippled_host);
                    supportedhostslist.add(crippled_host);
                }
            }
        } catch (final Throwable e) {
            logger.info("Website-workaround to find additional supported hosts failed");
        }
        /* 2019-04-05: Workaround for MEGA */
        if (supportedhostslist != null && supportedhostslist.contains("mega")) {
            supportedhostslist.add("mega.co.nz");
        }
        account.setConcurrentUsePossible(true);
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final Account account, final boolean forceFullLogin) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedInViaCookies = false;
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        return;
                    }
                    loggedInViaCookies = isLoggedinAPI();
                    if (isLoggedinAPI()) {
                        /* Save new cookie-timestamp */
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    }
                }
                if (!loggedInViaCookies) {
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
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
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
        /*
         * E.g. {"error":8, "message":"You have already downloaded, wait \u003Cb\u003E 14:11 minutes\u003C\/b\u003E to download again.
         * \u003Ca href=\"..\/signup\" target=\"_blank\"\u003EUpgrade to premium\u003C\/a\u003E and forget waiting times and enjoy unlimited
         * features!" }
         */
        final String errorcode = PluginJSonUtils.getJson(br, "error");
        if (errorcode != null) {
            if (errorcode.equals("0")) {
                /* All ok */
            } else if (errorcode.equals("1")) {
                /* No link entered - this should never happen */
                mhm.handleErrorGeneric(account, link, "api_error_1", 10, 5 * 60 * 1000l);
            } else if (errorcode.equals("2")) {
                /* http:// or https:// required --> WTF no idea what this means */
                mhm.handleErrorGeneric(account, link, "api_error_2", 10, 5 * 60 * 1000l);
            } else if (errorcode.equals("3")) {
                /* Link not supported */
                mhm.putError(account, link, 10 * 60 * 1000l, "Unsupported host");
            } else if (errorcode.equals("9")) {
                /* Hosters limit reached for this day */
                mhm.putError(account, link, 10 * 60 * 1000l, "Daily limit reached for this host");
            } else if (errorcode.equals("15")) {
                /* Service detected usage of proxy which they do not tolerate */
                mhm.putError(account, link, 15 * 60 * 1000l, "Proxy, VPN or VPS detected. Please contact deepbrid support!");
            } else {
                /* Unknown error */
                mhm.handleErrorGeneric(account, link, "api_error_unknown", 10, 5 * 60 * 1000l);
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}