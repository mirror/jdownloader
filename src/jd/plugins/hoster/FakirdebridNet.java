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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.MultiHostHost;
import jd.plugins.MultiHostHost.MultihosterHostStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fakirdebrid.net" }, urls = { "" })
public class FakirdebridNet extends PluginForHost {
    // private static final String WEBSITE_BASE = "https://fakirdebrid.net";
    private static final String          API_BASE           = "https://fakirdebrid.net/api";
    private static MultiHosterManagement mhm                = new MultiHosterManagement("fakirdebrid.net");
    private static final boolean         defaultResume      = true;
    private static final int             defaultMaxchunks   = -10;
    private final String                 PROPERTY_RESUME    = "fakirdebrid_resume";
    private final String                 PROPERTY_MAXCHUNKS = "fakirdebrid_maxchunks";

    @SuppressWarnings("deprecation")
    public FakirdebridNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://fakirdebrid.net/showthread.php?tid=184&pid=1370#pid1370");
    }

    @Override
    public String getAGBLink() {
        return "https://fakirdebrid.net/konu-terms-of-use.html";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        if (!attemptStoredDownloadurlDownload(link, this.getHost() + "directlink", link.getBooleanProperty(PROPERTY_RESUME, defaultResume), link.getIntegerProperty(PROPERTY_MAXCHUNKS, defaultMaxchunks))) {
            this.login(account, false);
            // br.setAllowedResponseCodes(new int[] { 503 });
            Map<String, Object> entries = null;
            int counter = 0;
            String passCode = link.getDownloadPassword();
            do {
                String postData = "url=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
                // if (link.getContainerUrl() != null) {
                // /* 2022-01-04: TODO: Requested by admin (include referer as parameter if available) but unclear which parameter-name to
                // use for this. */
                // postData += "&reflink=" + Encoding.urlEncode(link.getContainerUrl());
                // }
                if (counter > 0) {
                    /* 2nd try: First provided password was invalid or no password has been tried on first attempt. */
                    passCode = getUserInput("Password?", link);
                }
                if (passCode != null) {
                    /* Append password like: "|<password>" */
                    postData += Encoding.urlEncode("|" + passCode);
                }
                br.postPage(API_BASE + "/generate.php?pin=" + Encoding.urlEncode(account.getPass()), postData);
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Object errorCodeO = entries.get("code");
                if (errorCodeO != null && errorCodeO instanceof String && errorCodeO.toString().equalsIgnoreCase("Password_Required")) {
                    logger.info("Password required");
                    counter += 1;
                    // continue;
                } else if (errorCodeO != null && errorCodeO instanceof String && errorCodeO.toString().equalsIgnoreCase("Wrong_Password")) {
                    logger.info("Wrong password");
                    counter += 1;
                    // continue;
                } else {
                    /* No password required or correct password entered */
                    break;
                }
            } while (counter <= 2);
            this.handleErrorsAPI(this.br, link, account);
            if (passCode != null) {
                /* Save password for the next time. */
                link.setDownloadPassword(passCode);
            }
            entries = (Map<String, Object>) entries.get("data");
            final boolean resumable = ((Boolean) entries.get("resumable")).booleanValue();
            int maxChunks = ((Number) entries.get("maxchunks")).intValue();
            if (maxChunks > 1) {
                maxChunks = -maxChunks;
            }
            String dllink = null;
            final boolean useNewHandling = true; /* 2021-06-21 */
            if (useNewHandling) {
                /* 2021-06-21: Testing */
                final String apilink = (String) entries.get("apilink");
                br.getPage(apilink);
                this.handleErrorsAPI(this.br, link, account);
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                entries = (Map<String, Object>) entries.get("data");
                final int files_done = ((Number) entries.get("files_done")).intValue();
                if (files_done != 1) {
                    final double percentageCompleted = Double.parseDouble(entries.get("completed").toString());
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is being transferred to fakirdebrid servers: " + percentageCompleted + "%", 5 * 1000l);
                } else {
                    dllink = (String) entries.get("link");
                }
            } else {
                /* 2022-01-04: Deprecated */
                final List<String> urls = (List<String>) entries.get("links");
                if (urls.isEmpty()) {
                    mhm.handleErrorGeneric(account, link, "Failed to generate transloadURL", 10, 5 * 60 * 1000l);
                }
                String transloadURL = null;
                /*
                 * Go through array of list containing the same URL with different domains as some are blocked. Typical domains include:
                 * turkleech.com, fakirdebrid.xyz, fakirdebrid.info
                 */
                for (final String url : urls) {
                    try {
                        /**
                         * E.g. server2.turkleech.com/TransLoad/?id=bla </br>
                         * Such URLs will also work fine without login cookies
                         */
                        br.getPage(url);
                        transloadURL = url;
                        break;
                    } catch (final IOException ignore) {
                        logger.log(ignore);
                    }
                }
                if (StringUtils.isEmpty(transloadURL)) {
                    mhm.handleErrorGeneric(account, link, "Failed to find working transloadURL", 10, 5 * 60 * 1000l);
                }
                logger.info("Selected transloadURL/domain: " + transloadURL);
                final String transloadID = UrlQuery.parse(transloadURL).get("id");
                final String continueURL = br.getRegex("(\\?id=" + Regex.escape(transloadID) + "[^<>\"\\']+\\&action=download)").getMatch(0);
                if (continueURL == null) {
                    mhm.handleErrorGeneric(account, link, "Failed to find continueURL", 10, 5 * 60 * 1000l);
                }
                br.getPage(continueURL);
                dllink = br.getRegex("(files/[^\"\\']+)(?:\"|') class=.DOWNLOAD").getMatch(0);
            }
            if (dllink == null) {
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 10, 5 * 60 * 1000l);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxChunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to file", 10, 5 * 60 * 1000l);
            }
            link.setProperty(this.getHost() + "directlink", dl.getConnection().getURL().toString());
            link.setProperty(PROPERTY_RESUME, resumable);
            link.setProperty(PROPERTY_MAXCHUNKS, maxChunks);
        }
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        if (Boolean.TRUE.equals(entries.get("banned"))) {
            throw new AccountInvalidException("Account banned");
        }
        /*
         * User only enters apikey as password and could enter anything into the username field --> Make sure it contains an unique value.
         */
        final String username = (String) entries.get("username");
        if (!StringUtils.isEmpty(username)) {
            account.setUser(username);
        }
        account.setType(AccountType.PREMIUM);
        final String message = (String) entries.get("message");
        final String accountTypeStr = (String) entries.get("type");
        if (!StringUtils.isEmpty(message) && !StringUtils.isEmpty(accountTypeStr)) {
            ai.setStatus(message + " - " + accountTypeStr);
        } else if (!StringUtils.isEmpty(accountTypeStr)) {
            ai.setStatus(accountTypeStr);
        } else if (!StringUtils.isEmpty(message)) {
            ai.setStatus(message);
        }
        ai.setTrafficLeft(((Number) entries.get("trafficleft")).longValue());
        ai.setTrafficMax(((Number) entries.get("trafficlimit")).longValue());
        ai.setValidUntil(JavaScriptEngineFactory.toLong(entries.get("premium_until"), 0) * 1000, br);
        br.getPage(API_BASE + "/supportedhosts.php?pin=" + Encoding.urlEncode(account.getPass()));
        entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final List<Map<String, Object>> arrayHoster;
        final Object arrayHosterO = entries.get("supportedhosts");
        /* 2021-05-27: API can return Map instead of expected Array */
        if (arrayHosterO instanceof Map) {
            final Map<String, Object> mapHoster = (Map<String, Object>) arrayHosterO;
            arrayHoster = new ArrayList(mapHoster.values());
            // mapHoster.values();
        } else {
            arrayHoster = (List<Map<String, Object>>) arrayHosterO;
        }
        final List<MultiHostHost> supportedHosts = new ArrayList<MultiHostHost>();
        for (final Map<String, Object> hostermap : arrayHoster) {
            final String domain = hostermap.get("host").toString();
            final boolean active = ((Boolean) hostermap.get("currently_working")).booleanValue();
            final MultiHostHost mhost = new MultiHostHost(domain);
            if (!active) {
                mhost.setStatus(MultihosterHostStatus.DEACTIVATED_MULTIHOST);
            }
            mhost.setResume(((Boolean) hostermap.get("resumable")).booleanValue());
            mhost.setMaxChunks(((Number) hostermap.get("maxChunks")).intValue());
            mhost.setMaxDownloads(((Number) hostermap.get("maxDownloads")).intValue());
            mhost.setTrafficMax(((Number) hostermap.get("traffixmax_daily")).longValue());
            mhost.setTrafficLeft(((Number) hostermap.get("trafficleft")).longValue());
            supportedHosts.add(mhost);
        }
        ai.setMultiHostSupportV2(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean validateToken) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            prepBR(this.br);
            if (!isValidAPIPIN(account.getPass())) {
                throw new AccountInvalidException("Invalid API PIN format.\r\n Find your API PIN here: " + pinURLWithoutProtocol);
            }
            if (!validateToken) {
                logger.info("Trust token without login");
                return;
            } else {
                br.getPage(API_BASE + "/info.php?pin=" + Encoding.urlEncode(account.getPass()));
                handleErrorsAPI(br, this.getDownloadLink(), account);
                /* Assume successful login if no error has happened! */
            }
        }
    }

    private static boolean isValidAPIPIN(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[A-Za-z0-9]{10,}")) {
            return true;
        } else {
            return false;
        }
    }

    private static String correctPassword(final String pw) {
        return pw.trim();
    }

    /**
     * 'PIN_Required' => 'PIN Required', </br>
     * 'URL_Required' => 'You did not add URL data', </br>
     * 'PIN_Invalid' => 'Invalid PIN',</br>
     * 'NOT_Supported' => 'This service is not supported or not supported by API.',</br>
     * 'Limit_Error_Transfer' => 'You have exhausted all transfer limits of your account, You need to purchase a new package.',</br>
     * 'Limit_Error_Premium' => 'You have filled the daily limits of your account. Please try again after the limits are reset.',</br>
     * 'Link_Error_Browser' => 'This link can only be downloaded via the browser.',</br>
     * 'Link_Error' => 'An unknown error occurred while creating the link.',</br>
     * 'File_not_found' => 'File not found',</br>
     * 'Password_Required' => 'This file is protected by password. Please add link Password.',</br>
     * 'Wrong_Password' => 'Wrong Password, Please try again.',</br>
     * 'File_Unavailable' => 'This link is currently unavailable. Please try again later.',</br>
     * 'Price_File' => 'This link cannot be downloaded with premium account, You can download it by purchasing this link only.',</br>
     * 'Download_Server_Error' => 'Download server with your file is temporarily unavailable.',</br>
     * 'OVH_Yangin' => 'This file seems to have been affected by the fire in the OVH datacenter.',</br>
     * 'Size_Error' => 'The premium download link for this file is not working.',</br>
     * 'Banned_Account' => 'Banned Account',</br>
     * 'Free_Account' => 'Not supported for free members.',
     */
    private void handleErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object errorO = entries.get("error");
        if (errorO instanceof Boolean && errorO == Boolean.TRUE) {
            final String message = (String) entries.get("message");
            final Object errorCodeO = entries.get("code");
            if (errorCodeO instanceof Number || errorCodeO.toString().matches("\\d+")) {
                /* 2021-09-14: Seems like this is not required anymore because the "code" field is always a String. */
                final int errorcode = Integer.parseInt(errorCodeO.toString());
                switch (errorcode) {
                case -1:
                    /* No error */
                    break;
                case 1001:
                    /* Invalid PIN */
                    if (account.getLastValidTimestamp() == -1) {
                        showPINLoginInformation();
                    }
                    throw new AccountInvalidException(message);
                default:
                    throw new AccountInvalidException(message);
                }
            } else {
                /* Distinguish between temp. account errors, permanent account errors and downloadlink/host related errors. */
                final String errorStr = errorCodeO.toString();
                if (errorStr.equalsIgnoreCase("PIN_Invalid")) {
                    /* Only show this dialog if user has tried to add this account for the first time. */
                    if (account.getLastValidTimestamp() == -1) {
                        showPINLoginInformation();
                    }
                    throw new AccountInvalidException(message);
                } else if (errorStr.equalsIgnoreCase("Banned_Account") || errorStr.equalsIgnoreCase("Free_Account")) {
                    /* Permanent account error */
                    throw new AccountInvalidException(message);
                } else if (errorStr.equalsIgnoreCase("Limit_Error_Transfer") || errorStr.equalsIgnoreCase("Limit_Error_Premium")) {
                    /* Temp. account error */
                    throw new AccountUnavailableException(errorStr, 5 * 60 * 1000l);
                } else if (errorStr.equalsIgnoreCase("Password_Required")) {
                    link.setPasswordProtected(true);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Password required");
                } else if (errorStr.equalsIgnoreCase("Wrong_Password")) {
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else if (errorStr.equalsIgnoreCase("File_not_found")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    logger.info("Unknown error happened: " + errorStr);
                    if (link == null) {
                        throw new AccountUnavailableException(errorStr, 5 * 60 * 1000l);
                    } else {
                        mhm.handleErrorGeneric(account, this.getDownloadLink(), errorStr, 10);
                    }
                }
            }
        }
    }

    private static final String pinURLWithoutProtocol = "fakirdebrid.net/api/login.php";

    private Thread showPINLoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Fakirdebrid.net - Login";
                        message += "Hallo liebe(r) Fakirdebrid NutzerIn\r\n";
                        message += "Um deinen Fakirdebrid Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne den folgenden Link im Browser falls das nicht bereits automatisch passiert ist:\r\n\t'" + pinURLWithoutProtocol + "'\t\r\n";
                        message += "2. Kopiere deine PIN, versuche erneut einen fakirdebrid Account hinzuzufügen und gib sie in JDownloader ein.\r\n";
                        message += "Falls du myjdownloader verwendest, gib die PIN in das Benutzername- und in das Passwort Feld ein.\r\n";
                    } else {
                        title = "Fakirdebrid.net - Login";
                        message += "Hello dear Fakirdebrid user\r\n";
                        message += "In order to use this service in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically:\r\n\t'" + pinURLWithoutProtocol + "'\t\r\n";
                        message += "2. Copy your PIN, try to add your fakirdebrid account to JD again and enter the PIN into the PIN field.\r\n";
                        message += "If you're using myjdownloader, enter the PIN in both the username- and password fields.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL("https://" + pinURLWithoutProtocol);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(final InputChangedCallbackInterface callback) {
        return new FakirdebridNetAccountFactory(callback);
    }

    public static class FakirdebridNetAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private String getPassword() {
            if (this.pass == null) {
                return null;
            } else {
                return correctPassword(new String(this.pass.getPassword()));
            }
        }

        public boolean updateAccount(Account input, Account output) {
            boolean changed = false;
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                changed = true;
            }
            if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                changed = true;
            }
            return changed;
        }

        private final ExtPasswordField pass;
        private final JLabel           apiPINLabel;

        public FakirdebridNetAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                add(new JLabel("Hier findest du deine API PIN:"));
            } else {
                add(new JLabel("Click here to find your API PIN:"));
            }
            add(new JLink("https://fakirdebrid.net/api/login.php"));
            add(apiPINLabel = new JLabel("API PIN:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                pass.setHelpText("Gib deine API PIN ein");
            } else {
                pass.setHelpText("Enter your API PIN");
            }
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                // name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            final String pw = getPassword();
            if (FakirdebridNet.isValidAPIPIN(pw)) {
                apiPINLabel.setForeground(Color.BLACK);
                return true;
            } else {
                apiPINLabel.setForeground(Color.RED);
                return false;
            }
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}