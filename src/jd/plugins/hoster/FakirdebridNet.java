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
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fakirdebrid.net" }, urls = { "" })
public class FakirdebridNet extends PluginForHost {
    // private static final String WEBSITE_BASE = "https://fakirdebrid.net";
    private static final String          API_BASE  = "https://fakirdebrid.net/api";
    private static MultiHosterManagement mhm       = new MultiHosterManagement("fakirdebrid.net");
    private static final boolean         resume    = true;
    private static final int             maxchunks = -10;

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
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        if (!attemptStoredDownloadurlDownload(link, "", resume, maxchunks)) {
            this.login(account, false);
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Fakirdebrid.net plugin is still under development");
            }
            br.postPage(API_BASE + "/generate.php?pin=" + Encoding.urlEncode(account.getPass()), "url=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) entries.get("data");
            final String transloadURL = (String) entries.get("link");
            if (StringUtils.isEmpty(transloadURL)) {
                mhm.handleErrorGeneric(account, link, "Failed to generate transloadURL", 10, 5 * 60 * 1000l);
            }
            final boolean resumable = ((Boolean) entries.get("resume")).booleanValue();
            // final int maxChunks = ((Number)entries.get("maxchunks")).intValue();
            // br.getPage("https://" + this.getHost() + "/generate/");
            // final Browser brc = br.cloneBrowser();
            // brc.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
            // brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // brc.postPage("/generate/index.php?rand=0." + System.currentTimeMillis(), "captcha=none&urllist=" +
            // Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            // final String transloadURL = brc.getRegex("(https?://[^/]+/TransLoad/\\?id=[A-Za-z0-9]+)").getMatch(0);
            // if (transloadURL == null) {
            // throw new PluginException(LinkStatus.ERROR_FATAL, "Failed to find TransLoad URL");
            // }
            final String transloadID = UrlQuery.parse(transloadURL).get("id");
            br.getPage(transloadURL);
            final String continueURL = br.getRegex("(\\?id=" + transloadID + "[^<>\"\\']+\\&action=download)").getMatch(0);
            if (continueURL == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Failed to find continueURL");
            }
            br.getPage(continueURL);
            final String dllink = br.getRegex("(files/[^\"]+)\" class=\"DOWNLOAD\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Failed to find final downloadurl");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
            }
            link.setProperty(this.getHost() + "directlink", dl.getConnection().getURL().toString());
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
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object accountBannedO = entries.get("AccountBanned");
        if (accountBannedO instanceof Boolean && accountBannedO == Boolean.TRUE) {
            throw new AccountInvalidException("Account banned");
        }
        account.setType(AccountType.PREMIUM);
        ai.setStatus((String) entries.get("AccountType"));
        ai.setTrafficLeft(((Number) entries.get("trafficleft")).longValue());
        ai.setTrafficMax(((Number) entries.get("trafficlimit")).longValue());
        ai.setValidUntil(JavaScriptEngineFactory.toLong(entries.get("premium_until"), 0) * 1000, br);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        br.getPage(API_BASE + "/supportedhosts.php?pin=" + Encoding.urlEncode(account.getPass()));
        entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final List<Object> array_hoster;
        final Object arrayHosterO = entries.get("supportedhosts");
        /* 2021-05-27: API can return Map instead of expected Array */
        if (arrayHosterO instanceof Map) {
            final Map<String, Object> mapHoster = (Map<String, Object>) arrayHosterO;
            array_hoster = new ArrayList(mapHoster.values());
            // mapHoster.values();
        } else {
            array_hoster = (List<Object>) arrayHosterO;
        }
        for (final Object hoster : array_hoster) {
            final Map<String, Object> hostermap = (Map<String, Object>) hoster;
            final String domain = (String) hostermap.get("host");
            final boolean active = ((Boolean) hostermap.get("currently_working")).booleanValue();
            // final boolean resume = ((Boolean) hoster_map.get("resumable")).booleanValue();
            // final int maxchunks = ((Number) hoster_map.get("maxChunks")).intValue();
            // final int maxdls = ((Number) hoster_map.get("maxDownloads")).intValue();
            /* Workaround to find the real domain which we need to assign the properties to later on! */
            final ArrayList<String> supportedHostsTmp = new ArrayList<String>();
            supportedHostsTmp.add(domain);
            ai.setMultiHostSupport(this, supportedHostsTmp);
            final List<String> realDomainList = ai.getMultiHostSupport();
            if (realDomainList == null || realDomainList.isEmpty()) {
                /* Skip unsupported hosts or host plugins which don't allow multihost usage. */
                continue;
            }
            final String realDomain = realDomainList.get(0);
            if (active) {
                supportedHosts.add(realDomain);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean validateToken) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            prepBR(this.br);
            if (!validateToken) {
                logger.info("Trust token without login");
                return;
            } else {
                br.getPage(API_BASE + "/info.php?pin=" + Encoding.urlEncode(account.getPass()));
                handleErrorsAPI(br, account);
                /* Assume successful login if no error has happened! */
            }
        }
    }

    private void handleErrorsAPI(final Browser br, final Account account) throws AccountInvalidException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object errorO = entries.get("error");
        if (errorO instanceof Boolean && errorO == Boolean.TRUE) {
            final String message = (String) entries.get("message");
            final int errorcode = JavaScriptEngineFactory.toInteger(entries.get("code"), -1);
            switch (errorcode) {
            case -1:
                /* No error */
                break;
            case 1001:
                /* Invalid PIN */
                showPINLoginInformation();
                throw new AccountInvalidException(message);
            default:
                throw new AccountInvalidException(message);
            }
        }
    }

    private Thread showPINLoginInformation() {
        final String pinURLWithoutProtocol = "fakirdebrid.net/api/login.php";
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Fakirdebrid.net - Login";
                        message += "Hallo liebe(r) Fakirdebrid NutzerIn\r\n";
                        message += "Um deinen Fakirdebrid Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + pinURLWithoutProtocol + "'\t\r\n";
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
            }
            if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
                return null;
            }
            return new String(this.pass.getPassword());
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
        private static String          EMPTYPW = "                 ";

        public FakirdebridNetAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                add(new JLabel("Hier findest du deine API PIN:"));
            } else {
                add(new JLabel("Click here to find your API PIN:"));
            }
            add(new JLink("https://fakirdebrid.net/api/login.php"));
            add(new JLabel("API PIN:"));
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
            // final String userName = getUsername();
            // if (userName == null || !userName.trim().matches("^\\d{9}$")) {
            // idLabel.setForeground(Color.RED);
            // return false;
            // }
            // idLabel.setForeground(Color.BLACK);
            return getPassword() != null;
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