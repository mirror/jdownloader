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
import java.util.Arrays;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "stahomat.cz", "superload.cz" }, urls = { "https?://(?:www\\.)?stahomat\\.(?:cz|sk)/(stahnout|download)/[a-zA-Z0-9%-]+", "https?://(?:www\\.)?(superload\\.cz|superload\\.eu|superload\\.sk|superloading\\.com|stahovatelka\\.cz)/(stahnout|download)/[a-zA-Z0-9%-]+" })
public class StahomatCzSuperloadCz extends antiDDoSForHost {
    /* IMPORTANT: superload.cz and stahomat.cz use the same api */
    /* IMPORTANT2: 30.04.15: They block IPs from the following countries: es, it, jp, fr, cl, br, ar, de, mx, cn, ve */
    private static MultiHosterManagement mhm = null;

    public StahomatCzSuperloadCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + this.getHost() + "/");
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/terms";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void init() {
        mhm = new MultiHosterManagement(this.getHost());
    }

    private String get_api_base() {
        return "http://api." + this.getHost() + "/a-p-i";
    }

    public Browser prepBrowser(final Browser br) {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        return br;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = openAntiDDoSRequestConnection(br, br.createHeadRequest(link.getPluginPatternMatcher()));
            if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                br.followConnection();
            } else {
                link.setFinalFileName(getFileNameFromHeader(con));
                link.setDownloadSize(con.getLongContentLength());
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        final String fileName[] = br.getRegex("class=\"files-item file\"\\s*>\\s*<h.*?>\\s*(.*?)\\s*<span>\\s*(.*?)\\s*<").getRow(0);
        if (fileName == null || fileName.length == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setName(fileName[0] + fileName[1]);
        final String fileSize = br.getRegex("class=\"file-info-item-value file-info-item-value-high\"\\s*>\\s*([0-9\\.]+\\s*[kbmtg]+)\\s*<").getMatch(0);
        if (fileSize != null) {
            link.setDownloadSize(SizeFormatter.getSize(fileSize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        handleMultiHost(downloadLink, account);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink, final boolean isStoredDirectURL) throws Exception {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean resume;
        boolean updateCredits;
        if (isStoredDirectURL) {
            logger.info("Using stored directurl --> NOT updating credits after download as this method should not use up any new credits");
            updateCredits = true;
            /* 2019-07-24: Resume attempt will return response 400 for stored directurls */
            resume = false;
        } else {
            logger.info("Generated new directurl --> Updating credits after downloadstart");
            updateCredits = false;
            resume = true;
        }
        try {
            /* we want to follow redirects in final stage */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, 1);
            if (!dl.getConnection().isContentDisposition()) {
                /* download is not contentdisposition, so remove this host from premiumHosts list */
                br.followConnection();
                if (br.containsHTML("Not enough credits") || br.containsHTML("insufficient credits")) {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                updateCredits = false;
                if (br.containsHTML(">Omlouváme se, ale soubor se nepovedlo stáhnout")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
                }
                /* temp disabled the host */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                link.setProperty("superloadczdirectlink", dllink);
                /* contentdisposition, lets download it */
                dl.startDownload();
            }
        } finally {
            if (updateCredits) {
                try {
                    updateCredits(null, account);
                } catch (final Throwable e) {
                    logger.info("Could not updateCredits handleDL!");
                    logger.info(e.toString());
                }
            }
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        prepBrowser(br);
        final String pass = link.getStringProperty("pass", null);
        String downloadURL = checkDirectLink(link, "superloadczdirectlink");
        final boolean usingStoredDirecturl;
        if (downloadURL != null) {
            usingStoredDirecturl = true;
        } else {
            usingStoredDirecturl = false;
            /* request Download */
            postPageSafe(account, get_api_base() + "/download-url", "url=" + Encoding.urlEncode(link.getPluginPatternMatcher()) + (pass != null ? "&password=" + Encoding.urlEncode(pass) : "") + "&token=");
            downloadURL = PluginJSonUtils.getJsonValue(br, "link");
            if (downloadURL == null) {
                handleErrors(account, link);
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        // might need a sleep here hoster seems to have troubles with new links.
        handleDL(account, link, downloadURL, usingStoredDirecturl);
    }

    private void handleErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String error = PluginJSonUtils.getJsonValue(br, "error");
        if (StringUtils.equalsIgnoreCase(error, "invalidLink")) {
            logger.info("Superload.cz says 'invalid link', disabling real host for 1 hour.");
            mhm.putError(account, link, 60 * 60 * 1000l, "Invalid Link");
        } else if (StringUtils.equalsIgnoreCase(error, "temporarilyUnsupportedServer")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temp. Error. Try again later", 5 * 60 * 1000l);
        } else if (StringUtils.equalsIgnoreCase(error, "fileNotFound")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (StringUtils.equalsIgnoreCase(error, "unsupportedServer")) {
            logger.info("Superload.cz says 'unsupported server', disabling real host");
            mhm.putError(account, link, 5 * 60 * 1000l, "Unsuported Server");
        } else if (StringUtils.equalsIgnoreCase(error, "Lack of credits") || StringUtils.equalsIgnoreCase(error, "insufficient credits")) {
            logger.info("Superload.cz says 'Lack of credits', temporarily disabling account.");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (StringUtils.equalsIgnoreCase(error, "no credits")) {
            logger.info("No credits");
            final AccountInfo ai = account.getAccountInfo();
            ai.setTrafficLeft(0);
            account.setAccountInfo(ai);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (StringUtils.equalsIgnoreCase(error, "User deleted")) {
            synchronized (account) {
                account.removeProperty("token");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (StringUtils.containsIgnoreCase(error, "Unable to download the file")) {
            mhm.handleErrorGeneric(account, link, "unable_to_download_the_file", 50, 5 * 60 * 1000l);
        } else if (StringUtils.equalsIgnoreCase(error, "User deleted")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (StringUtils.equalsIgnoreCase(error, "Invalid token")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid logintoken", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable t) {
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        prepBrowser(this.br);
        login(account);
        try {
            updateCredits(ai, account);
        } catch (PluginException e) {
            throw e;
        } catch (IOException e) {
            logger.info("Could not updateCredits fetchAccountInfo!");
            logger.log(e);
        }
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(5);
        ai.setValidUntil(-1);
        ai.setStatus("Premium account");
        try {
            /* get the supported host list */
            postPage(br, get_api_base() + "/get-supported-hosts", "token=" + this.getLoginToken(account));
            final String[] hosts = new Regex(br.toString(), "\"([^\", ]+\\.[^\", ]+)").getColumn(0);
            final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            ai.setMultiHostSupport(this, supportedHosts);
        } catch (IOException e) {
            logger.info("Could not fetch ServerList");
            logger.log(e);
        }
        return ai;
    }

    private void login(final Account account) throws Exception {
        synchronized (account) {
            try {
                /*
                 * First check if old token is still working. NOT DOING SO WILL RESULT IN IP BANS/VERY SLOW LOGIN/TIMEOUTS, ESPECIALLY WHEN
                 * MULTIPLE ACCOUNTS ARE USED!!!
                 */
                boolean loggedinViaToken = false;
                String loginToken = this.getLoginToken(account);
                if (loginToken != null) {
                    logger.info("Checking stored login_token");
                    postPage(br, get_api_base() + "/get-status-bar", "token=" + loginToken);
                    /* E.g. error-response on expired/invalid token: {"error":"Invalid token"} */
                    loggedinViaToken = getSuccess();
                }
                if (loggedinViaToken) {
                    logger.info("Successfully re-used stored logintoken");
                } else {
                    logger.info("Performing full login");
                    postPage(get_api_base() + "/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + JDHash.getMD5(account.getPass()));
                    if (!getSuccess()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    loginToken = PluginJSonUtils.getJson(br, "token");
                    if (StringUtils.isEmpty(loginToken)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                account.setProperty("token", loginToken);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty("token");
                }
                throw e;
            }
        }
    }

    private AccountInfo updateCredits(AccountInfo ai, final Account account) throws Exception {
        if (ai == null) {
            ai = new AccountInfo();
        }
        final String loginToken = getLoginToken(account);
        if (loginToken == null) {
            logger.warning("loginToken invalid");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        postPage(br, get_api_base() + "/get-status-bar", "token=" + loginToken);
        final Integer credits = Integer.parseInt(PluginJSonUtils.getJson(br, "credits"));
        if (credits != null) {
            // 1000 credits = 1 GB, convert back into 1024 (Bytes)
            // String expression = "(" + credits + " / 1000) * 1073741824";
            // Double result = new DoubleEvaluator().evaluate(expression);
            ai.setTrafficLeft(SizeFormatter.getSize(credits + "MiB"));
        }
        return ai;
    }

    private String getLoginToken(final Account account) throws IOException, PluginException {
        return account.getStringProperty("token", null);
    }

    private boolean getSuccess() {
        final String error = PluginJSonUtils.getJson(br, "error");
        final String success = PluginJSonUtils.getJson(br, "success");
        /* No error = Success */
        return StringUtils.isEmpty(error) && !"false".equals(success);
    }

    private void postPageSafe(final Account account, final String page, final String postData) throws Exception {
        boolean failed = true;
        for (int i = 1; i <= 5; i++) {
            logger.info("Request try " + i + " of 5");
            try {
                postPage(br, page, postData + getLoginToken(account));
            } catch (final BrowserException e) {
                if (br.getRequest().getHttpConnection().getResponseCode() == 401) {
                    logger.info("Request failed (401) -> Re-newing token and trying again");
                    try {
                        this.login(account);
                    } catch (final Exception e_acc) {
                        logger.warning("Failed to re-new token!");
                        throw e_acc;
                    }
                    continue;
                }
                logger.info("Request failed (other BrowserException) -> Throwing BrowserException");
                throw e;
            }
            failed = false;
            break;
        }
        if (failed) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 10 * 60 * 1000l);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}