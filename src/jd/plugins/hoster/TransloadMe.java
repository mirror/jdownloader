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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

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
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "transload.me" }, urls = { "" })
public class TransloadMe extends PluginForHost {
    private static final String          API_BASE                     = "http://api.transload.me/";
    private static final String          NICE_HOST                    = "transload.me";
    private static final String          NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String          NORESUME                     = NICE_HOSTproperty + "NORESUME";           /* Connection limits */
    private static final boolean         ACCOUNT_PREMIUM_RESUME       = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int             ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    private static MultiHosterManagement mhm                          = new MultiHosterManagement("transload.me");

    public TransloadMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://en.transload.me/?p=register");
    }

    @Override
    public String getAGBLink() {
        return "http://en.transload.me/?p=login&redir=helpdesk";
    }

    private Browser prepBrowser(final Browser br) {
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader " + getVersion());
        br.setCookie(getHost(), "lang", "en");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
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
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBrowser(br);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            mhm.runCheck(account, link);
            dllink = generateDownloadlinkAPI(account, link);
            if (dllink == null) {
                /* Should never happen */
                mhm.handleErrorGeneric(account, link, "dllinknull", 30, 2 * 60 * 1000l);
            }
        }
        handleDL(account, link, dllink);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        if (link.getBooleanProperty(NORESUME, false)) {
            resume = false;
            link.setProperty(NORESUME, Boolean.valueOf(false));
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                handleErrorsAPI(br, account, link);
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 5, 2 * 60 * 1000l);
            }
            dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw e;
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.isOK() && (con.isContentDisposition() || (!con.getContentType().contains("html") && con.getLongContentLength() > 0))) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            downloadLink.setProperty(property, Property.NULL);
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        return fetchAccountInfoAPI(account);
    }

    private String generateDownloadlinkAPI(final Account account, DownloadLink link) throws Exception {
        prepBrowser(br);
        getApi("require=downloadfile&link=" + Encoding.urlEncode(link.getDownloadURL()), account, link);
        final String dllink = PluginJSonUtils.getJsonValue(br, "link");
        return dllink;
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        synchronized (account) {
            final AccountInfo ai = new AccountInfo();
            prepBrowser(br);
            br.setFollowRedirects(true);
            getApi("require=accountdetalis", account, null);
            final String result = PluginJSonUtils.getJsonValue(br, "result");
            if ("5".equals(result)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (!"0".equals(result)) {
                final String error = PluginJSonUtils.getJson(br, "error");
                if (error != null) {
                    /* 2019-08-30: Hmm seems like their API is dead as it always returns error 6 */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "API error: " + error + " [Website might be dead]", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Balance left in USD */
            final String balance = PluginJSonUtils.getJsonValue(br, "balance");
            final String reg_date = PluginJSonUtils.getJsonValue(br, "reg_date");
            if (reg_date != null) {
                ai.setCreateTime(TimeFormatter.getMilliSeconds(reg_date, "yyyy-MM-dd", Locale.ENGLISH));
            }
            if (balance == null || Double.parseDouble(balance) <= 0) {
                account.setType(AccountType.FREE);
                throwZeroBalance(account);
            } else {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                ai.setStatus("Premium account balance " + balance + " USD");
                /*
                 * Set unlimited traffic as each filehost costs a different amount of money per GB see:
                 * http://en.transload.me/index.php?p=statistic
                 */
                ai.setUnlimitedTraffic();
            }
            getApi("require=supporthost", account, null);
            final Map<String, Object> host_list = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final List<Map<String, Object>> list = (List<Map<String, Object>>) host_list.get("host_list");
            List<String> supportedHosts = new ArrayList<String>();
            for (final Map<String, Object> entry : list) {
                final String status = String.valueOf(entry.get("status"));
                final String host = (String) entry.get("host");
                if (("0".equals(status) || "1".equals(status)) && host != null) {
                    // 0 - Works
                    // 1 - Unstable
                    // 2 - does Not work
                    // 3 - Support file exchanger in the recovery process
                    supportedHosts.add(host);
                }
            }
            supportedHosts = ai.setMultiHostSupport(this, supportedHosts);
            return ai;
        }
    }

    private void throwZeroBalance(final Account account) throws PluginException {
        synchronized (account) {
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) {
                /* E.g. account gets added for the first time AND no balance left. */
                ai = new AccountInfo();
            }
            ai.setTrafficLeft(0);
            ai.setProperty("multiHostSupport", Property.NULL);
            account.setAccountInfo(ai);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account balance 0.00 USD!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    /**
     * http://transload.me/en/?p=api
     *
     * @param input
     * @param account
     * @param link
     * @throws Exception
     */
    private void getApi(final String input, final Account account, final DownloadLink link) throws Exception {
        String accesslink = API_BASE + "?" + input + "&username=" + URLEncode.encodeURIComponent(account.getUser()) + "&password=" + URLEncode.encodeURIComponent(account.getPass());
        // accesslink += "&client_id=jdownloader";
        br.getPage(accesslink);
        handleErrorsAPI(br, account, link);
    }

    private int updatestatuscodeAPI() {
        final String result = PluginJSonUtils.getJsonValue(br, "result");
        if (result != null && result.matches("\\d+")) {
            return Integer.parseInt(result);
        } else {
            return 0;
        }
    }

    /* Please do not remove this function - future usage!! */
    private void handleErrorsAPI(final Browser br, final Account account, final DownloadLink link) throws Exception {
        final int statuscode = updatestatuscodeAPI();
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                // "error": "0" - the Request succeeds, if the code is 0, then you can process the result
                break;
            case 1:
                // "error": "1" - the File is not found or has been deleted.
                /* 2017-10-12: Do not trust this API response! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error:1", 60 * 60 * 1000l);
            case 2:
                // "error": "2" - file Sharing is not supported.
                // should be mh wide
                statusMessage = "Unsupported host";
                mhm.handleErrorGeneric(account, link, "unsupported_host", 5, 5 * 60 * 1000l);
            case 3:
                // "error": "3" - system error Occurred while processing, please try again later.
                statusMessage = "Temporary error occured";
                mhm.handleErrorGeneric(account, link, "temporary_error", 5, 2 * 60 * 1000l);
            case 4:
                // "error": "4" - On account of insufficient funds, replenish your balance.
                // update account scraping
                account.setAccountInfo(fetchAccountInfoAPI(account));
                mhm.handleErrorGeneric(account, link, "credits", 2, 10 * 60 * 1000l);
            case 5:
                // "error": "5" is Used or password is incorrect.
                if (System.getProperty("user.language").equals("de")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 6:
                // "error": "6" - Invalid method request.
                statusMessage = "Invalid API request - this should never happen!";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 7:
                // "error": "7" - You asked for too many links, wait a few minutes, or refill your account. (This occurs when the user has
                // generated a lot of links, though lacking means for downloading, you need to wait a few minutes before clearing balance)
                // so no more links for the account?
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
            case 8:
                // disabled api, switch to webmode
                throw new AccountUnavailableException("API is disabled", 60 * 60 * 1000l);
            default:
                mhm.handleErrorGeneric(account, link, "unknown_error_state", 50, 2 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}