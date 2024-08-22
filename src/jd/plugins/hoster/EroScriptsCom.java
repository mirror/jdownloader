package jd.plugins.hoster;

import java.util.Map;

import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "discuss.eroscripts.com" }, urls = { "https?://discuss\\.eroscripts\\.com/uploads/([\\w\\-/]+)" })
public class EroScriptsCom extends antiDDoSForHost {
    private static final String COOKIE_ID       = "_forum_session";
    public static final String  FETCH_IMAGES    = "FETCH_IMAGES";
    public static final String  SMART_FILENAMES = "SMART_FILENAMES";

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "discuss.eroscripts.com" };
    }

    public EroScriptsCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://discuss.eroscripts.com/signup");
        setConfigElements();
    }

    protected void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FETCH_IMAGES, "Add images?").setDefaultValue(true));
    }

    public void login(final Browser br, final Account account, boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                account.setType(AccountType.FREE);
                getLogger().info("Performing login");
                Cookies cookies = account.loadCookies(COOKIE_ID);
                if (cookies != null && !cookies.isEmpty()) {
                    br.setCookies("https://discuss.eroscripts.com", cookies);
                    if (!validateCookies) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage("https://discuss.eroscripts.com/login");
                    if (br.getRedirectLocation() != null) {
                        // Update cookie timestamp
                        account.saveCookies(br.getCookies("https://discuss.eroscripts.com"), COOKIE_ID);
                        return;
                    }
                    account.clearCookies(COOKIE_ID);
                }
                br.getPage("https://discuss.eroscripts.com/login");
                Form form = br.getFormbyProperty("id", "hidden-login-form");
                if (form == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                Browser csrfBr = br.cloneBrowser();
                csrfBr.setHeader("accept", "application/json");
                csrfBr.getPage("https://discuss.eroscripts.com/session/csrf");
                final Map<String, Object> apiResponse = restoreFromString(csrfBr.toString(), TypeRef.MAP);
                form.put("login", account.getUser());
                form.put("password", account.getPass());
                form.put("second_factor_method", "1");
                form.setAction("https://discuss.eroscripts.com/session");
                form.setMethod(MethodType.POST);
                Browser formBr = br.cloneBrowser();
                formBr.setHeader("x-csrf-token", String.valueOf(apiResponse.get("csrf")));
                formBr.setHeader("x-requested-with", "XMLHttpRequest");
                formBr.submitForm(form);
                Map<String, Object> loginResponse = restoreFromString(formBr.toString(), TypeRef.MAP);
                if (loginResponse.containsKey("reason") && ((String) loginResponse.get("reason")).equals("invalid_second_factor")) {
                    String twoFactorCode = getUserInput("Two Factor code", null);
                    form.put("second_factor_method", "1");
                    form.put("second_factor_token", twoFactorCode);
                    formBr.submitForm(form);
                    loginResponse = restoreFromString(formBr.toString(), TypeRef.MAP);
                }
                if (loginResponse.containsKey("error")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, (String) loginResponse.get("error"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(formBr.getCookies("https://discuss.eroscripts.com"), COOKIE_ID);
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
        return "https://discuss.eroscripts.com/signup";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (link.getBooleanProperty("has_file_info")) {
            return AvailableStatus.TRUE;
        }
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (account == null) {
            throw new AccountRequiredException();
        }
        // TODO: check if browser instance has cookies set (called by decrypter plugin) so no additional login is required
        login(br, account, false);
        basicLinkCheck(br.cloneBrowser(), br.createGetRequest(link.getPluginPatternMatcher()), link, null, null);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        // no account required?
        dl = BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher());
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleFree(link);
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
