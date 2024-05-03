//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.ExtTextHighlighter;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GoogleDriveDirectoryIndex extends PluginForHost {
    public GoogleDriveDirectoryIndex(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://workers.dev/";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "workers.dev", "workers.cloudflare.com" });
        ret.add(new String[] { "cdn.web-dl.eu.org" });
        return ret;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.FAVICON };
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            /* Define no regex as crawler plugin decides which links are to be handled by this plugin. */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Host plugin that can handle instances of this project:
     * https://gitlab.com/ParveenBhadooOfficial/Google-Drive-Index/-/blob/master/README.md </br>
     * Be sure to add all domains to crawler plugin GoogleDriveDirectoryIndex.java too!
     */
    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        if (account != null) {
            login(account);
        }
        URLConnectionAdapter con = null;
        try {
            /* 2024-05-03: Needed in some cases or error 401 will be returned even with correct authorization. */
            br.getHeaders().put("Referer", link.getPluginPatternMatcher());
            if (isDownload) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), this.isResumeable(link, account), this.getMaxChunks(link, account));
                con = dl.getConnection();
            } else {
                con = br.openGetConnection(link.getPluginPatternMatcher());
            }
            handleConnectionErrors(br, con, null);
            if (con.getCompleteContentLength() > 0) {
                if (con.isContentDecoded()) {
                    link.setDownloadSize(con.getCompleteContentLength());
                } else {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            }
            /* Usually final filename is already set by crawler plugin. */
            final String fname = Plugin.getFileNameFromDispositionHeader(con);
            if (fname != null) {
                link.setFinalFileName(fname);
            }
        } finally {
            try {
                if (!isDownload) {
                    con.disconnect();
                }
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        if (dl == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con, final Account account) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 401) {
                /* Account required or existent account is missing rights to access that content! */
                if (account != null) {
                    throw new AccountInvalidException();
                } else {
                    throw new AccountRequiredException();
                }
            } else if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to file");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public void login(final Account account) throws Exception {
        synchronized (account) {
            String pw = account.getPass();
            if (pw == null) {
                /* Allow empty password! */
                pw = "";
            }
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account);
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(final InputChangedCallbackInterface callback) {
        return new EditAccountPanelWorkersDev(callback, this);
    }

    /** 2024-04-30: The sole purpose of this is to allow login without password slash login with blank password. */
    public class EditAccountPanelWorkersDev extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        protected String getPassword() {
            if (this.pass == null) {
                return null;
            } else {
                return new String(this.pass.getPassword());
            }
        }

        protected String getUsername() {
            if (name == null) {
                return "";
            } else {
                if (_GUI.T.jd_gui_swing_components_AccountDialog_help_username().equals(this.name.getText())) {
                    return null;
                }
                return this.name.getText();
            }
        }

        private final ExtTextField                  name;
        private final ExtPasswordField              pass;
        private final InputChangedCallbackInterface callback;
        private JLabel                              usernameLabel = null;
        private final JLabel                        passwordLabel;
        private final PluginForHost                 plg;

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

        public EditAccountPanelWorkersDev(final InputChangedCallbackInterface callback, final PluginForHost plg) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            this.plg = plg;
            this.callback = callback;
            add(usernameLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_name()));
            add(this.name = new ExtTextField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(name);
                }

                {
                    final HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);
                    addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("^(\\s+)")));
                    addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("(\\s+)$")));
                    refreshTextHighlighter();
                }
            });
            name.setHelpText(_GUI.T.jd_gui_swing_components_AccountDialog_help_username());
            add(passwordLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_pass()));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(pass);
                }
            }, "");
            /* Normal username & password login */
            pass.setHelpText(_GUI.T.BuyAndAddPremiumAccount_layoutDialogContent_pass());
        }

        public InputChangedCallbackInterface getCallback() {
            return callback;
        }

        public void setAccount(final Account defaultAccount) {
            if (defaultAccount != null) {
                name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            final boolean userok;
            final boolean passok = true;
            if (StringUtils.isEmpty(this.getUsername())) {
                usernameLabel.setForeground(Color.RED);
                userok = false;
            } else {
                usernameLabel.setForeground(Color.BLACK);
                userok = true;
            }
            // final String pw = getPassword();
            if (!passok) {
                passwordLabel.setForeground(Color.RED);
            } else {
                passwordLabel.setForeground(Color.BLACK);
            }
            if (userok && passok) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Account getAccount() {
            return new Account(getUsername(), getPassword());
        }

        @Override
        public JComponent getComponent() {
            return this;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}