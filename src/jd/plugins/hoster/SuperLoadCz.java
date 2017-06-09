//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "superload.cz" }, urls = { "http://\\w+\\.superload\\.eu/download\\.php\\?a=[a-z0-9]+" })
public class SuperLoadCz extends antiDDoSForHost {
    /* IMPORTANT: superload.cz and stahomat.cz use the same api */
    /* IMPORTANT2: 30.04.15: They block IPs from the following countries: es, it, jp, fr, cl, br, ar, de, mx, cn, ve */
    // DEV NOTES
    // supports last09 based on pre-generated links and jd2

    private static final String          mName             = "superload.cz/";
    private static final String          mProt             = "http://";
    private static final String          mAPI              = "http://api.superload.cz/a-p-i";
    private static final String          NICE_HOSTproperty = "superloadcz";
    private static MultiHosterManagement mhm               = new MultiHosterManagement("superload.cz");

    private static Object                LOCK              = new Object();

    public SuperLoadCz(final PluginWrapper wrapper) {
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

    public Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setCustomCharset("utf-8");
        prepBr.setConnectTimeout(3 * 60 * 1000);
        prepBr.setReadTimeout(3 * 60 * 1000);
        prepBr.setAllowedResponseCodes(401);
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    /**
     * JD 2 Code. DO NOT USE OVERRIDE FOR COMPATIBILITY REASONS
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        Browser br = prepBrowser(new Browser());
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(final DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Download only works with a premium" + mName + "account.", PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        showMessage(downloadLink, "Task 1: Check URL validity!");
        requestFileInformation(downloadLink);
        showMessage(downloadLink, "Task 2: Download begins!");
        handleDL(account, downloadLink, downloadLink.getDownloadURL());
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        boolean updateCredits = true;
        try {
            /* we want to follow redirects in final stage */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (!dl.getConnection().isContentDisposition()) {
                /* download is not contentdisposition, so remove this host from premiumHosts list */
                br.followConnection();
                if (br.containsHTML("Not enough credits")) {
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

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        prepBrowser(br);
        final String pass = link.getStringProperty("pass", null);
        String dllink = checkDirectLink(link, "superloadczdirectlink");
        if (dllink == null) {
            showMessage(link, "Task 1: Generating Link");
            /* request Download */
            postPageSafe(account, mAPI + "/download-url", "url=" + Encoding.urlEncode(link.getDownloadURL()) + (pass != null ? "&password=" + Encoding.urlEncode(pass) : "") + "&token=");
            dllink = PluginJSonUtils.getJsonValue(br, "link");
            if (dllink == null) {
                final String error = PluginJSonUtils.getJsonValue(br, "error");
                if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else if (StringUtils.equalsIgnoreCase(error, "invalidLink")) {
                    logger.info("Superload.cz says 'invalid link', disabling real host for 1 hour.");
                    mhm.putError(null, link, 60 * 60 * 1000l, "Invalid Link");
                } else if (StringUtils.equalsIgnoreCase(error, "temporarilyUnsupportedServer")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temp. Error. Try again later", 5 * 60 * 1000l);
                } else if (StringUtils.equalsIgnoreCase(error, "fileNotFound")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (StringUtils.equalsIgnoreCase(error, "unsupportedServer")) {
                    logger.info("Superload.cz says 'unsupported server', disabling real host");
                    mhm.putError(null, link, 5 * 60 * 1000l, "Unsuported Server");
                } else if (StringUtils.equalsIgnoreCase(error, "Lack of credits")) {
                    logger.info("Superload.cz says 'Lack of credits', temporarily disabling account.");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (StringUtils.equalsIgnoreCase(error, "no credits")) {
                    logger.info("No credits");
                    final AccountInfo ai = account.getAccountInfo();
                    ai.setTrafficLeft(0);
                    account.setAccountInfo(ai);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (StringUtils.equalsIgnoreCase(error, "User deleted")) {
                    /* WTF what does this error mean?? */
                    logger.info("Superload.cz says 'User deleted'");
                    // to me this means disable user account! -raz
                    mhm.putError(account, link, 60 * 60 * 1000l, "Invalid Link");
                } else if (StringUtils.containsIgnoreCase(error, "Unable to download the file")) {
                    handleErrorRetries(null, link, "Unable to download file", 10, 10 * 60 * 1000l);
                } else if (dllink == null) {
                    // this will allow statserv to pick up these errors, and we can improve the plugin.
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unhandled Error Type");
                }
            }
            showMessage(link, "Task 2: Download begins!");
        }
        // might need a sleep here hoster seems to have troubles with new links.
        handleDL(account, link, dllink);
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        prepBrowser(br);
        // this will force new login.
        account.setProperty("token", Property.NULL);
        try {
            updateCredits(ai, account);
        } catch (Exception e) {
            logger.info("Could not updateCredits fetchAccountInfo!");
            logger.info(e.toString());
            throw e;
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(5);
        ai.setValidUntil(-1);
        ai.setStatus("Premium Account");
        try {
            // get the supported host list,
            String hostsSup = br.cloneBrowser().postPage(mAPI + "/get-supported-hosts", "token=" + getToken(account));
            String[] hosts = new Regex(hostsSup, "\"([^\", ]+\\.[^\", ]+)").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            ai.setMultiHostSupport(this, supportedHosts);
        } catch (Throwable e) {
            logger.info("Could not fetch ServerList from " + mName + ": " + e.toString());
        }
        return ai;
    }

    private String getToken(final Account account) throws PluginException, IOException {
        synchronized (LOCK) {
            String token = account.getStringProperty("token", null);
            if (inValidate(token)) {
                // relogin
                login(account);
                token = account.getStringProperty("token", null);
                if (inValidate(token)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            return token;
        }
    }

    private boolean login(final Account acc) throws IOException, PluginException {
        final Browser login = prepBrowser(new Browser());
        login.postPage(mAPI + "/login", "username=" + Encoding.urlEncode(acc.getUser()) + "&password=" + JDHash.getMD5(acc.getPass()));
        if (login.getHttpConnection() != null && login.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (!getSuccess(login)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String token = PluginJSonUtils.getJsonValue(login, "token");
        if (!inValidate(token)) {
            acc.setProperty("token", token);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return true;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private AccountInfo updateCredits(final AccountInfo ai, final Account account) throws PluginException, IOException {
        AccountInfo ac = ai;
        if ((ai == null || ac == null) && account != null) {
            ac = account.getAccountInfo();
        }
        if (ac == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage(mAPI + "/get-status-bar", "token=" + getToken(account));
        final String credits = PluginJSonUtils.getJsonValue(br, "credits");
        if (!inValidate(credits) && credits.matches("[\\d\\.]+")) {
            // 1000 credits = 1 GB, convert back into 1024 (Bytes)
            // String expression = "(" + credits + " / 1000) * 1073741824";
            // Double result = new DoubleEvaluator().evaluate(expression);
            ac.setTrafficLeft(SizeFormatter.getSize(credits + "MiB"));
        }
        if (ai == null) {
            account.setAccountInfo(ac);
        }
        return ac;
    }

    private boolean getSuccess(final Browser ibr) {
        return ibr.getRegex("\"success\":true").matches();
    }

    private void postPageSafe(final Account acc, final String page, final String postData) throws Exception {
        boolean failed = true;
        boolean tokenFailed = false;
        for (int i = 1; i <= 5; i++) {
            logger.info("Request try " + i + " of 5");
            br.postPage(page, postData + getToken(acc));
            if (br.getHttpConnection() == null) {
                Thread.sleep(2500);
            } else if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 401) {
                logger.info("Request failed (401) -> Re-newing token and trying again");
                acc.getProperty("token", Property.NULL);
            } else if ("Invalid token".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "error"))) {
                logger.info("Old token failed, will retry one more time, but this time with new token");
                // dump old token, will force new full login.
                acc.setProperty("token", Property.NULL);
                if (tokenFailed) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Can not gather new token", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                // prevent more than one retry.
                tokenFailed = true;
            } else {
                failed = false;
                break;
            }
        }
        if (failed) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 10 * 60 * 1000l);
        }
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final Account account, final DownloadLink downloadlink, final String error, final int maxRetries, final long disableTime) throws PluginException {
        int timesFailed = downloadlink.getIntegerProperty(NICE_HOSTproperty + "-failedtimes_" + error, 0);
        if (timesFailed <= maxRetries) {
            logger.info("Retrying -> " + error);
            timesFailed++;
            downloadlink.setProperty(NICE_HOSTproperty + "-failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            downloadlink.setProperty(NICE_HOSTproperty + "-failedtimes_" + error, Property.NULL);
            logger.info("Disabling current host -> " + error);
            mhm.putError(account, downloadlink, disableTime, error);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}