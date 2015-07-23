package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;
//import jd.plugins.components.GoogleHelper;
import jd.plugins.components.GoogleHelper;

import org.appwork.exceptions.WTFException;

@HostPlugin(revision = "$Revision: 29935 $", interfaceVersion = 3, names = { "google.com (Recaptcha)" }, urls = { "google://.+" }, flags = { 2 })
public class GooglePremium extends PluginForHost {

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
    public ConfigContainer getConfig() {
        throw new WTFException("Not implemented");
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

    public boolean hasConfig() {
        return false;
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
    public void resetDownloadlink(DownloadLink downloadLink) {

    }

    @Override
    public void resetPluginGlobals() {
    }

    protected void setConfigElements() {
    }

    @Override
    public void reset() {
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }
}
