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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetConfigPanel;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumize.me" }, urls = { "premiumizedecrypted://.+" })
public class PremiumizeMe extends UseNet {
    private static final String          NICE_HOST                 = "premiumize.me";
    private static final String          NICE_HOSTproperty         = NICE_HOST.replaceAll("(\\.|\\-)", "");
    /* Connection limits */
    private static final boolean         ACCOUNT_PREMIUM_RESUME    = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private final String                 client_id                 = "616325511";
    // private static Object LOCK = new Object();
    private static MultiHosterManagement mhm                       = new MultiHosterManagement("premiumize.me");

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (isDirectURL(link.getDownloadLink())) {
            final String new_url = link.getPluginPatternMatcher().replaceAll("[a-z0-9]+decrypted://", "https://");
            link.setPluginPatternMatcher(new_url);
        }
    }

    /*
     * IMPORTANT INFORMATION: According to their support we can 'hammer' their API every 5 minutes so we could even make an
     * "endless retries" mode which, on fatal errors, waits 5 minutes, then tries again.
     */
    public PremiumizeMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getProtocol() + "premiumize.me");
    }

    @Override
    protected PluginConfigPanelNG createConfigPanel() {
        return new UsenetConfigPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean showKeyHandler(KeyHandler<?> keyHandler) {
                return "ssldownloadsenabled".equals(keyHandler.getKey());
            }

            @Override
            protected boolean useCustomUI(KeyHandler<?> keyHandler) {
                return !"ssldownloadsenabled".equals(keyHandler.getKey());
            }

            @Override
            protected void initAccountConfig(PluginForHost plgh, Account acc, Class<? extends AccountConfigInterface> cf) {
                super.initAccountConfig(plgh, acc, cf);
                extend(this, getHost(), getAvailableUsenetServer(), getAccountJsonConfig(acc));
            }
        };
    }

    /**
     * TODO: Maybe add a setting to not add .nzb and .torrent files when adding cloud folders as JD will automatically add the contents of
     * .nzb files after downloading them but in this case that makes no sense as when users add cloud URLs these will contain the
     * downloaded- and extracted contents of .nzb(and .torrent) files already.
     */
    public static interface PremiumizeMeConfigInterface extends UsenetAccountConfigInterface {
        public class Translation {
            public String getSSLDownloadsEnabled_label() {
                return _JDT.T.lit_ssl_enabled();
            }
        }

        public static final PremiumizeMeConfigInterface.Translation TRANSLATION = new Translation();

        @DefaultBooleanValue(true)
        @Order(10)
        boolean isSSLDownloadsEnabled();

        void setSSLDownloadsEnabled(boolean b);
    };

    @Override
    public String getAGBLink() {
        return getProtocol() + this.getHost() + "/?show=tos";
    }

    public static Browser prepBR(final Browser br) {
        return ZeveraCom.prepBR(br);
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new PremiumizeMeAccountFactory(callback);
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (isUsenetLink(link)) {
            return super.requestFileInformation(link);
        } else {
            return ZeveraCom.requestFileInformationDirectURL(this.br, link);
        }
    }

    public static String getCloudID(final String url) {
        if (url.contains("folder_id")) {
            return new Regex(url, "folder_id=([a-zA-Z0-9\\-_]+)").getMatch(0);
        } else {
            return new Regex(url, "id=([a-zA-Z0-9\\-_]+)").getMatch(0);
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (StringUtils.equals(getHost(), downloadLink.getHost()) && account == null) {
            // generated links do not require an account
            return true;
        }
        return account != null;
    }

    public boolean isDirectURL(final DownloadLink downloadLink) {
        return StringUtils.equals(getHost(), downloadLink.getHost());
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (!isDirectURL(link)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDL_DIRECT(null, link);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else if (isDirectURL(link)) {
            handleDL_DIRECT(account, link);
        } else {
            this.br = prepBR(this.br);
            mhm.runCheck(account, link);
            ZeveraCom.login(this.br, account, false, client_id);
            String dllink = ZeveraCom.getDllink(this.br, account, link, client_id, this);
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 2, 5 * 60 * 1000l);
            }
            handleDL_MOCH(account, link, dllink);
        }
    }

    private void handleDL_MOCH(final Account account, final DownloadLink link, final String dllink) throws Exception {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                updatestatuscode();
                handleAPIErrors(this.br);
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 2, 5 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw e;
        }
    }

    /** Account is not required */
    private void handleDL_DIRECT(final Account account, final DownloadLink link) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        final String contenttype = dl.getConnection().getContentType();
        if (contenttype.contains("html")) {
            br.followConnection();
            // updatestatuscode();
            // handleAPIErrors(this.br);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST, FEATURE.USENET };
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return ZeveraCom.fetchAccountInfoAPI(this, this.br, client_id, account);
    }

    private static String getProtocol() {
        return "https://";
    }

    // private void login(final Account account, final boolean force) throws Exception {
    // ZeveraCom.login(this.br, account, force, client_id);
    // }
    /** Keep this for possible future API implementation */
    private void updatestatuscode() {
    }

    /** Keep this for possible future API implementation */
    private void handleAPIErrors(final Browser br) throws PluginException {
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("usenet.premiumize.me", false, 119));
        ret.addAll(UsenetServer.createServerList("usenet.premiumize.me", true, 563));
        return ret;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public static class PremiumizeMeAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final String      IDHELP           = "Enter your account id (9 digits)";
        private final String      PINHELP          = "Enter your pin";

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

        private String getUsername() {
            if (IDHELP.equals(this.name.getText())) {
                return null;
            }
            return this.name.getText();
        }

        private final ExtTextField     name;
        private final ExtPasswordField pass;
        private static String          EMPTYPW = "                 ";
        private final JLabel           idLabel;

        public PremiumizeMeAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel(_GUI.T.premiumize_add_account_click_here()));
            add(new JLink(getProtocol() + "www.premiumize.me/account"));
            add(idLabel = new JLabel(_GUI.T.premiumize_add_account_idlabel()));
            add(this.name = new ExtTextField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            });
            name.setHelpText(IDHELP);
            add(new JLabel("PIN:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(PINHELP);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            final String userName = getUsername();
            if (userName == null || !userName.trim().matches("^\\d{9}$")) {
                idLabel.setForeground(Color.RED);
                return false;
            }
            idLabel.setForeground(Color.BLACK);
            return getPassword() != null;
        }

        @Override
        public Account getAccount() {
            return new Account(getUsername(), getPassword());
        }
    }
}