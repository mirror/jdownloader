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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

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
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadLinkDownloadable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "downloader.guru" }, urls = { "" })
public class DownloaderGuru extends PluginForHost {
    private static final String          API_ENDPOINT     = "http://www.downloader.guru/";
    private static MultiHosterManagement mhm              = new MultiHosterManagement("downloader.guru");
    /* Last updated: 2015-03-31 */
    // private static final int defaultMAXDOWNLOADS = 20;
    private static final int             defaultMAXCHUNKS = 0;
    private static final boolean         defaultRESUME    = true;
    private int                          statuscode       = 0;

    @SuppressWarnings("deprecation")
    public DownloaderGuru(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.downloader.guru/Pricing.aspx");
    }

    @Override
    public String getAGBLink() {
        return "http://www.downloader.guru/Support.aspx";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
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
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        handleDL(account, link);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        if (dllink == null) {
            br.setFollowRedirects(true);
            /* request creation of downloadlink */
            /* Make sure that the file exists - unnecessary step in my opinion (psp) but admin wanted to have it implemented this way. */
            this.postRawAPISafe(account, API_ENDPOINT + "Transfers.ashx?sendlinks=1", link.getDownloadURL());
            /*
             * Returns json map "transfers" which contains array with usually only 1 object --> In this map we can find the "GeneratedLink"
             */
            dllink = PluginJSonUtils.getJsonValue(this.br, "GeneratedLink");
            if (dllink == null || !dllink.startsWith("http")) {
                logger.warning("Final downloadlink is null");
                mhm.handleErrorGeneric(account, this.getDownloadLink(), "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        final DownloadLinkDownloadable downloadable;
        if (link.getName().matches(".+(rar|r\\d+)$")) {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            final URLConnectionAdapter con = brc.openGetConnection(dllink);
            try {
                if (con.isOK() && con.isContentDisposition() && con.getLongContentLength() > 0) {
                    dllink = con.getRequest().getUrl();
                    if (link.getVerifiedFileSize() != -1 && link.getVerifiedFileSize() != con.getLongContentLength()) {
                        logger.info("Workaround for size missmatch(rar padding?!)!");
                        link.setVerifiedFileSize(con.getLongContentLength());
                    }
                }
            } finally {
                con.disconnect();
            }
            downloadable = new DownloadLinkDownloadable(link) {
                @Override
                public boolean isHashCheckEnabled() {
                    return false;
                }
            };
        } else {
            downloadable = new DownloadLinkDownloadable(link);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadable, br.createGetRequest(dllink), defaultRESUME, defaultMAXCHUNKS);
        } catch (PluginException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "RedirectLoop")) {
                mhm.handleErrorGeneric(account, this.getDownloadLink(), "redirectloop", 50, 5 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            updatestatuscode();
            handleAPIErrors(account, this.br);
            mhm.handleErrorGeneric(account, this.getDownloadLink(), "unknowndlerror", 50, 5 * 60 * 1000l);
        }
        link.setProperty(this.getHost() + "directlink", dllink);
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

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        login(account, false);
        /*
         * As long as we always perform a full login, this call is never needed as full login will return account type and expire date too.
         */
        // accessUserInfo();
        final ArrayList<String> supportedhostslist = new ArrayList();
        final ArrayList<String> supportedhostslistTables = new ArrayList();
        final String trafficleft = this.br.getRegex("Traffic: <span style=\"[^\"]+\">(\\d+[^<>\"]+)</span>").getMatch(0);
        if (trafficleft != null && !this.br.containsHTML("No Traffic")) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setConcurrentUsePossible(true);
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            /* Find- and add premium table */
            final String supportedhostslistTablePremium = this.br.getRegex("<table id=\"MainContent_DLPremiumHosters\"(.*?)</table>").getMatch(0);
            if (supportedhostslistTablePremium != null) {
                supportedhostslistTables.add(supportedhostslistTablePremium);
            }
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            account.setConcurrentUsePossible(false);
            /*
             * 2016-06-16: When logged in, top right corner says "No Traffic" while there is a list of "Free Trial Hosters". I've tested 4
             * of them but when sending the links to download, the error "You are out of Traffic!" will come up. I guess it's safe to say
             * that Free Accounts have no traffic.
             */
            ai.setTrafficLeft(0);
        }
        /* Find- and add free table - this is of course always available (free + premium). */
        final String supportedhostslistTableFree = this.br.getRegex("<table id=\"MainContent_DLFreeHosters\"(.*?)</table>").getMatch(0);
        if (supportedhostslistTableFree != null) {
            supportedhostslistTables.add(supportedhostslistTableFree);
        }
        for (final String supportedhostslistTable : supportedhostslistTables) {
            final String[] supportedHostsOfCurrentTable = new Regex(supportedhostslistTable, ">([^<>\"]+)(</b>\\s*)?</td>").getColumn(0);
            for (String domain : supportedHostsOfCurrentTable) {
                domain = Encoding.htmlDecode(domain).trim();
                domain = correctHost(domain);
                supportedhostslist.add(domain);
            }
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final Cookies cookies = account.loadCookies("");
            if (cookies != null && !force) {
                this.br.setCookies(this.getHost(), cookies);
                this.br.getPage(API_ENDPOINT + "/Download.aspx");
                if (br.containsHTML("Logout\\.aspx")) {
                    /* Refresh cookie timestamp */
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                /* Perform full login */
                this.br = prepBR(new Browser());
            }
            try {
                getAPISafe(account, API_ENDPOINT + "Login.aspx");
                Form loginform = this.br.getFormbyAction("./Login.aspx");
                if (loginform == null) {
                    loginform = this.br.getForm(0);
                }
                if (loginform == null) {
                    /* Should never happen */
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "\r\nPlugin broken, please contact the JDownloader Support!");
                    }
                }
                for (InputField inputField : loginform.getInputFields()) {
                    if (StringUtils.containsIgnoreCase(inputField.getKey(), "txt1")) {
                        inputField.setValue(Encoding.urlEncode(account.getUser()));
                    } else if (StringUtils.containsIgnoreCase(inputField.getKey(), "txt2")) {
                        inputField.setValue(Encoding.urlEncode(account.getPass()));
                    } else if (StringUtils.containsIgnoreCase(inputField.getKey(), "chkRememberME")) {
                        inputField.setValue("on");
                    }
                }
                postAPIFormSafe(account, loginform);
                if (this.br.getCookie(this.getHost(), "AuthCookie") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private void getAPISafe(final Account account, final String url) throws IOException, PluginException, InterruptedException {
        this.br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou've been blocked from the API!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        updatestatuscode();
        handleAPIErrors(account, this.br);
    }

    private void postRawAPISafe(final Account account, final String url, final String postData) throws IOException, PluginException, InterruptedException {
        this.br.postPageRaw(url, postData);
        updatestatuscode();
        handleAPIErrors(account, this.br);
    }

    private void postAPIFormSafe(final Account account, final Form form) throws Exception {
        this.br.submitForm(form);
        updatestatuscode();
        handleAPIErrors(account, this.br);
    }

    /** Performs slight domain corrections. */
    private String correctHost(String host) {
        if (host.equals("uploaded.to") || host.equals("uploaded.net")) {
            host = "uploaded.to";
        }
        return host;
    }

    /**
     * Errors 1-99 = JDErrors (by psp)
     */
    private void updatestatuscode() {
        final String errormessage = PluginJSonUtils.getJsonValue(this.br, "errormessage");
        if (errormessage != null && !"".equals(errormessage)) {
            if (errormessage.equalsIgnoreCase("You are out of Traffic!")) {
                statuscode = 1;
            } else {
                /* Hell - unknown errorcode! */
                statuscode = 666;
            }
        } else {
            statuscode = 0;
        }
    }

    private void handleAPIErrors(final Account account, final Browser br) throws PluginException, InterruptedException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* No traffic left (e.g. free account) */
                account.getAccountInfo().setTrafficLeft(0);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 666:
                // /* Unknown error */
                statusMessage = "Unknown error";
                logger.info("Unknown error");
                mhm.handleErrorGeneric(account, this.getDownloadLink(), "unknown_api_error", 50, 5 * 60 * 1000l);
            default:
                mhm.handleErrorGeneric(account, this.getDownloadLink(), "unknown_api_error_2", 50, 5 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info("Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
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