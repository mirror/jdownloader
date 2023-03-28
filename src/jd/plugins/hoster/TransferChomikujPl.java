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
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "transfer-chomikuj.pl" }, urls = { "" })
public class TransferChomikujPl extends PluginForHost {
    private static final String          API_BASE         = "https://transfer-chomikuj.pl/";
    private static MultiHosterManagement mhm              = new MultiHosterManagement("transfer-chomikuj.pl");
    private static final boolean         defaultResume    = true;
    private static final int             defaultMaxchunks = 0;

    @SuppressWarnings("deprecation")
    public TransferChomikujPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://allegro.pl/uzytkownik/TransferChomikuj");
    }

    @Override
    public String getAGBLink() {
        return "https://transfer-chomikuj.pl/";
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
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        final String directlinkProperty = this.getHost() + "directlink";
        if (!attemptStoredDownloadurlDownload(link, directlinkProperty)) {
            this.login(account, true);
            final Browser brc = br.cloneBrowser();
            this.setAjaxHeaders(brc);
            final UrlQuery query = new UrlQuery();
            query.add("links", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            /* Add download-password if available. */
            if (link.getDownloadPassword() != null) {
                query.add("password", Encoding.urlEncode(link.getDownloadPassword()));
            }
            brc.postPage("/check_links.php", query);
            String dllink = brc.getRegex("\"(https?://download\\.[^/]+/[^\"]+)").getMatch(0);
            if (dllink == null) {
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 10, 5 * 60 * 1000l);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultResume, defaultMaxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to file", 10, 5 * 60 * 1000l);
            }
            link.setProperty(directlinkProperty, dl.getConnection().getURL().toString());
        }
        dl.setFilenameFix(true);
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, defaultResume, defaultMaxchunks);
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

    private void setAjaxHeaders(final Browser br) {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final String expireDate = br.getRegex("(?i)Wygasa\\s*:\\s*(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        final Browser brc = br.cloneBrowser();
        setAjaxHeaders(brc);
        /* Obtain trafficLeft value using a separate request. */
        final String trafficLeft = brc.getPage("/quota.php");
        if (expireDate == null || trafficLeft == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        account.setType(AccountType.PREMIUM);
        ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
        /* This website only supports chomikuj.pl URLs. */
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        supportedHosts.add("chomikuj.pl");
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean validateCookies) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            prepBR(this.br);
            if (!isValidLoginCode(account.getPass())) {
                throw new AccountInvalidException("Invalid login code format!");
            }
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(cookies);
                if (!validateCookies) {
                    /* Do not validate cookies */
                    return;
                }
                br.getPage(API_BASE);
                if (this.isLoggedIN(br)) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(br.getHost());
                }
            }
            logger.info("Performing full login");
            br.getPage(API_BASE);
            Form loginform = br.getFormbyProperty("class", "login-form");
            if (loginform == null) {
                loginform = br.getFormbyKey("code");
            }
            loginform.put("code", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginform);
            if (!this.isLoggedIN(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("logout\\.php")) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isValidLoginCode(final String str) {
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

    @Override
    public AccountBuilderInterface getAccountFactory(final InputChangedCallbackInterface callback) {
        return new TransferChomikujPlAccountFactory(callback);
    }

    public static class TransferChomikujPlAccountFactory extends MigPanel implements AccountBuilderInterface {
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

        public TransferChomikujPlAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(apiPINLabel = new JLabel("Login code:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                pass.setHelpText("Gib deinen login Code ein");
            } else {
                pass.setHelpText("Enter your login code");
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
            if (TransferChomikujPl.isValidLoginCode(pw)) {
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