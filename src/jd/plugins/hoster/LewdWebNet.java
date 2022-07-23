package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "forum.lewdweb.net" }, urls = { "" })
public class LewdWebNet extends PluginForHost {
    private static final String COOKIE_ID        = "xf_session";
    private static final String COOKIE_URL       = "https://forum.lewdweb.net";
    public static final long    trust_cookie_age = 5 * 60 * 1000l;

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "forum.lewdweb.net" };
    }

    @SuppressWarnings("deprecation")
    public LewdWebNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getAGBLink());
    }

    public void login(final Browser br, final Account account, boolean alwaysLogin) throws Exception {
        synchronized (account) {
            try {
                account.setType(AccountType.FREE);
                getLogger().info("Performing login");
                Cookies cookies = account.loadCookies(COOKIE_ID);
                if (cookies != null && !cookies.isEmpty()) {
                    br.setCookies(COOKIE_URL, cookies);
                    if (!alwaysLogin && System.currentTimeMillis() - account.getCookiesTimeStamp(COOKIE_ID) <= trust_cookie_age) {
                        getLogger().info("Trust login cookies:" + account.getType());
                        return;
                    }
                    String page = br.getPage("https://discuss.eroscripts.com/login");
                    if (page.contains("You are already logged in")) {
                        // Update cookie timestamp
                        account.saveCookies(br.getCookies(COOKIE_URL), COOKIE_ID);
                        return;
                    }
                    account.clearCookies(COOKIE_ID);
                }
                String page = br.getPage("https://forum.lewdweb.net/login/login");
                if (page.contains("You are already logged in")) {
                    // Update cookie timestamp
                    account.saveCookies(br.getCookies(COOKIE_URL), COOKIE_ID);
                    return;
                }
                Form form = br.getFormbyProperty("class", "block");
                if (form == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                form.put("login", account.getUser());
                form.put("password", account.getPass());
                form.setMethod(MethodType.POST);
                Browser formBr = br.cloneBrowser();
                formBr.submitForm(form);
                if (formBr.getHttpConnection().getResponseCode() != 303) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(formBr.getCookies(COOKIE_URL), COOKIE_ID);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies(COOKIE_ID);
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.login(this.br, account, true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://forum.lewdweb.net/index.php?register/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return null;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
