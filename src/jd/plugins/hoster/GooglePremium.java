package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginForHost;

import org.jdownloader.plugins.components.google.GoogleAccountConfig;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.plugins.config.PluginConfigInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "recaptcha.google.com" }, urls = { "google://.+" }) public class GooglePremium extends PluginForHost {

    @Override
    public Boolean siteTesterDisabled() {
        // no tests required, dummy subdomain
        return Boolean.TRUE;
    }

    @Override
    public String getDescription() {
        return "Used for the purpose of ReCaptcha!";
    }

    @Override
    public String rewriteHost(String host) {
        if ("google.com (Recaptcha)".equals(host) || "recaptcha.google.com".equals(host)) {
            return "recaptcha.google.com";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://www.google.com/policies/terms/";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            GoogleHelper helper = new GoogleHelper(br);
            if (!helper.login(account)) {
                account.setValid(false);
                return ai;
            }
        } catch (final Exception e) {
            ai.setStatus(e.getMessage());
            account.setValid(false);
            return ai;
        }
        //
        ai.setStatus("Account OK");
        ai.setValidUntil(-1);
        account.setValid(true);
        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public Class<GoogleAccountConfig> getAccountConfigInterface(Account account) {
        return GoogleAccountConfig.class;
    }

    public GooglePremium(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://accounts.google.com/signup");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        handlePremium(downloadLink, null);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return null;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return super.isProxyRotationEnabledForLinkChecker();
    }

    @Override
    public void extendAccountSettingsPanel(Account acc, PluginConfigPanelNG panel) {
    }

    @Override
    public void resetDownloadlink(DownloadLink downloadLink) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void reset() {
    }

}
