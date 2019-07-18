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
import java.util.List;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "stahomat.cz" }, urls = { "" })
public class StahomatCz extends PluginForHost {
    /* IMPORTANT: superload.cz and stahomat.cz use the same api */
    /* IMPORTANT2: 30.04.15: They block IPs from the following countries: es, it, jp, fr, cl, br, ar, de, mx, cn, ve */
    private static final String          mName     = "stahomat.cz/";
    private static final String          mProt     = "http://";
    private static final String          mAPI      = "http://api.stahomat.cz/a-p-i";
    private static MultiHosterManagement mhm       = new MultiHosterManagement("stahomat.cz");
    private String                       api_token = null;

    public StahomatCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
    }

    private void setConstants(final Account account, final DownloadLink dl) throws IOException, PluginException {
        if (account != null) {
            this.getToken(account);
        }
    }

    /**
     * JD 2 Code. DO NOT USE OVERRIDE FOR COMPATIBILITY REASONS
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    public boolean checkLinks(DownloadLink[] urls) {
        prepBrowser();
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            List<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                logger.info("No account present, Please add a premium" + mName + "account.");
                for (DownloadLink dl : urls) {
                    /* no check possible */
                    dl.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                }
                return false;
            }
            // login(accs.get(0), false);
            br.setFollowRedirects(true);
            for (DownloadLink dl : urls) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(dl.getDownloadURL());
                    if (con.isContentDisposition()) {
                        dl.setFinalFileName(getFileNameFromHeader(con));
                        dl.setDownloadSize(con.getLongContentLength());
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                } finally {
                    try {
                        /* make sure we close connection */
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return link.getAvailableStatus();
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
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Download only works with a premium" + mName + "account.", PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    private void handleDL(Account account, DownloadLink link, String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            /* download is not contentdisposition, so remove this host from premiumHosts list */
            br.followConnection();
            if (br.containsHTML(">Omlouváme se, ale soubor se nepovedlo stáhnout")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            }
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 50, 5 * 60 * 1000l);
        } else {
            link.setProperty("superloadczdirectlink", dllink);
            /* contentdisposition, lets download it */
            dl.startDownload();
            try {
                updateCredits(null, account);
            } catch (Throwable e) {
                logger.info("Could not updateCredits handleDL!");
                logger.log(e);
            }
        }
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);
        mhm.runCheck(account, link);
        prepBrowser();
        br.setFollowRedirects(true);
        final String pass = link.getStringProperty("pass", null);
        this.getToken(account);
        if (api_token == null) {
            mhm.handleErrorGeneric(account, link, "login_token_null", 50, 5 * 60 * 1000l);
        }
        String dllink = checkDirectLink(link, "superloadczdirectlink");
        if (dllink == null) {
            /* request Download */
            if (pass != null) {
                postPageSafe(account, mAPI + "/download-url", "url=" + Encoding.urlEncode(link.getDownloadURL()) + "&password=" + Encoding.urlEncode(pass) + "&token=");
            } else {
                postPageSafe(account, mAPI + "/download-url", "url=" + Encoding.urlEncode(link.getDownloadURL()) + "&token=");
            }
            handleErrors(account, link);
            dllink = PluginJSonUtils.getJson(br, "link");
            if (dllink == null) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        // might need a sleep here hoster seems to have troubles with new links.
        handleDL(account, link, dllink);
    }

    private void handleErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String error = PluginJSonUtils.getJson(br, "error");
        if (error != null) {
            /* 2019-07-18: TODO: Some of these errormessages were copied from superload.cz by mistake(???) */
            if (error.equalsIgnoreCase("invalidLink")) {
                logger.info("'invalid link', disabling real host for 1 hour.");
                mhm.handleErrorGeneric(account, link, "invalidLink", 50, 5 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("temporarilyUnsupportedServer")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temp. Error. Try again later", 5 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("fileNotFound")) {
                logger.info("Detected untrusted fileNotFound --> Retrying until we have to temporarily disable the host");
                mhm.handleErrorGeneric(account, link, "untrusted_file_not_found", 50, 5 * 60 * 1000l);
                /* Do not trust their file_not_found status --> http://svn.jdownloader.org/issues/56989 */
                // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (error.equalsIgnoreCase("unsupportedServer")) {
                logger.info("'unsupported server', disabling real host for 1 hour.");
                mhm.handleErrorGeneric(account, link, "unsupportedServer", 50, 5 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Lack of credits")) {
                logger.info("'Lack of credits', temporarily disabling account.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (error.equalsIgnoreCase("Unable to download the file")) {
                mhm.handleErrorGeneric(account, link, "unable_to_download_file", 50, 5 * 60 * 1000l);
            } else {
                logger.warning("Unknown error:" + error);
            }
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        AccountInfo ai = new AccountInfo();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        prepBrowser();
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
            // get the supported host list,
            String hostsSup = br.cloneBrowser().postPage(mAPI + "/get-supported-hosts", "token=" + api_token);
            String[] hosts = new Regex(hostsSup, "\"([^\", ]+\\.[^\", ]+)").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            ai.setMultiHostSupport(this, supportedHosts);
        } catch (IOException e) {
            logger.info("Could not fetch ServerList from " + mName + ": " + e.toString());
            logger.log(e);
        }
        return ai;
    }

    private void login(final Account acc) throws IOException, PluginException {
        synchronized (acc) {
            try {
                br.postPage(mAPI + "/login", "username=" + Encoding.urlEncode(acc.getUser()) + "&password=" + JDHash.getMD5(acc.getPass()));
                if (!getSuccess()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                api_token = PluginJSonUtils.getJson(br, "token");
                if (api_token == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                acc.setProperty("token", api_token);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    acc.removeProperty("token");
                }
                throw e;
            }
        }
    }

    private AccountInfo updateCredits(AccountInfo ai, final Account account) throws PluginException, IOException {
        if (ai == null) {
            ai = new AccountInfo();
        }
        this.getToken(account);
        if (api_token == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage(mAPI + "/get-status-bar", "token=" + api_token);
        final Integer credits = Integer.parseInt(PluginJSonUtils.getJson(br, "credits"));
        if (credits != null) {
            // 1000 credits = 1 GB, convert back into 1024 (Bytes)
            // String expression = "(" + credits + " / 1000) * 1073741824";
            // Double result = new DoubleEvaluator().evaluate(expression);
            ai.setTrafficLeft(SizeFormatter.getSize(credits + "MiB"));
        }
        return ai;
    }

    private String getToken(final Account account) throws IOException, PluginException {
        api_token = PluginJSonUtils.getJson(br, "token");
        if (api_token == null) {
            login(account);
        }
        return api_token;
    }

    private boolean getSuccess() {
        return br.getRegex("\"success\":true").matches();
    }

    private void postPageSafe(final Account acc, final String page, final String postData) throws Exception {
        boolean failed = true;
        for (int i = 1; i <= 5; i++) {
            logger.info("Request try " + i + " of 5");
            try {
                br.postPage(page, postData + api_token);
            } catch (final BrowserException e) {
                if (br.getRequest().getHttpConnection().getResponseCode() == 401) {
                    logger.info("Request failed (401) -> Re-newing token and trying again");
                    try {
                        this.login(acc);
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