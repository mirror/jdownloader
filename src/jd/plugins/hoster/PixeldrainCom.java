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
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DefaultEditAccountPanel;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PixeldrainCom extends PluginForHost {
    public PixeldrainCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://pixeldrain.com/#pro");
    }

    @Override
    public String getAGBLink() {
        return "https://pixeldrain.com/about";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pixeldrain.com", "pixeldra.in" });
        return ret;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:api/file/|u/)?([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean  FREE_RESUME                                   = true;
    private static final int      FREE_MAXCHUNKS                                = 0;
    private static final int      FREE_MAXDOWNLOADS                             = 20;
    /* See docs: https://pixeldrain.com/api */
    public static final String    API_BASE                                      = "https://pixeldrain.com/api";
    protected static final String PIXELDRAIN_JD_API_HELP_PAGE                   = "https://pixeldrain.com/user/connect_app?app=jdownloader";
    private static final boolean  ACCOUNT_FREE_RESUME                           = true;
    private static final int      ACCOUNT_FREE_MAXCHUNKS                        = 0;
    private static final int      ACCOUNT_FREE_MAXDOWNLOADS                     = 20;
    private static final boolean  USE_API_FOR_LOGIN                             = true;
    private static final boolean  ACCOUNT_PREMIUM_RESUME                        = true;
    private static final int      ACCOUNT_PREMIUM_MAXCHUNKS                     = 0;
    private static final int      ACCOUNT_PREMIUM_MAXDOWNLOADS                  = 20;
    private static final String   PROPERTY_CAPTCHA_REQUIRED                     = "captcha_required";
    private static final String   PROPERTY_ACCOUNT_HAS_SHOWN_APIKEY_HELP_DIALOG = "has_shown_apikey_help_dialog";

    private boolean useAPIForLogin() {
        if (USE_API_FOR_LOGIN && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    private String getAPIURLUser() {
        return API_BASE + "/user";
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return this.checkLinks(urls, account);
    }

    public boolean checkLinks(final DownloadLink[] allLinks, final Account account) {
        if (allLinks == null || allLinks.length == 0) {
            return false;
        }
        try {
            if (account != null) {
                this.login(account, false);
            }
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                final StringBuilder sb = new StringBuilder();
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == allLinks.length || links.size() == 100) {
                        break;
                    } else {
                        links.add(allLinks[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink link : links) {
                    if (sb.length() > 0) {
                        /* Append comma */
                        sb.append("%2C");
                    }
                    sb.append(this.getFID(link));
                }
                br.getPage(API_BASE + "/file/" + sb.toString() + "/info");
                final List<Map<String, Object>> items;
                final Object response = JSonStorage.restoreFromString(br.toString(), TypeRef.OBJECT);
                if (response instanceof List) {
                    items = (List<Map<String, Object>>) response;
                } else {
                    items = new ArrayList<Map<String, Object>>();
                    items.add((Map<String, Object>) response);
                }
                for (final DownloadLink link : links) {
                    final String id = this.getFID(link);
                    Map<String, Object> data = null;
                    for (final Map<String, Object> item : items) {
                        final String thisID = (String) item.get("id");
                        if (StringUtils.equals(thisID, id)) {
                            data = item;
                            break;
                        }
                    }
                    if (data == null) {
                        /* id not in response, so its offline */
                        link.setAvailable(false);
                    } else {
                        setDownloadLinkInfo(this, link, data);
                    }
                }
                if (index == allLinks.length) {
                    /* We've reached the end */
                    break;
                }
            }
        } catch (final Exception ignore) {
            logger.log(ignore);
            return false;
        }
        return true;
    }

    /**
     * Using API according to https://pixeldrain.com/api
     *
     * @throws Exception
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.checkLinks(new DownloadLink[] { link }, account);
        checkErrors(br, link, account);
        if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    /** Shared function used by crawler & host plugin. */
    public static void setDownloadLinkInfo(final Plugin plugin, final DownloadLink link, final Map<String, Object> data) throws PluginException {
        final String filename = (String) data.get("name");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        link.setVerifiedFileSize(((Number) data.get("size")).longValue());
        link.setSha256Hash((String) data.get("hash_sha256"));
        final String description = (String) data.get("description");
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        final String availability = (String) data.get("availability");
        if (StringUtils.equals(availability, API_availability_CAPTCHA_REQUIRED)) {
            link.setProperty(PROPERTY_CAPTCHA_REQUIRED, true);
        } else {
            link.removeProperty(PROPERTY_CAPTCHA_REQUIRED);
        }
        link.setAvailable(true);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private static String API_availability_CAPTCHA_REQUIRED = "file_rate_limited_captcha_required";

    private void handleDownload(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        requestFileInformation(link, account);
        final Map<String, Object> data = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        String dllink = API_BASE + "/file/" + this.getFID(link) + "?download";
        if (API_availability_CAPTCHA_REQUIRED.equals(data.get("availability"))) {
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lfbzz4UAAAAAAaBgox1R7jU0axiGneLDkOA-PKf").getToken();
            dllink += "&recaptcha_response=" + URLEncode.encodeURIComponent(recaptchaV2Response);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String value = PluginJSonUtils.getJson(br, "value");
            final String message = PluginJSonUtils.getJson(br, "message");
            if (API_availability_CAPTCHA_REQUIRED.equals(value)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA, message);
            }
            checkErrors(br, link, account);
            /* We're using an API so let's never throw "Plugin defect" errors. */
            if (!StringUtils.isEmpty(message)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
        }
        dl.startDownload();
    }

    private void login(final Account account, final boolean force) throws Exception {
        if (useAPIForLogin()) {
            loginAPI(account, force);
        } else {
            loginWebsite(account, force);
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                prepBR(this.br);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return;
                    }
                    br.getPage("https://" + this.getHost() + "/user");
                    if (this.isLoggedinWebsite(this.br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/login");
                final Form loginform = br.getFormByInputFieldKeyValue("form", "login");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                final URLConnectionAdapter con = br.openFormConnection(loginform);
                if (con.getResponseCode() == 400) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    br.followConnection(true);
                }
                if (!isLoggedinWebsite(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private void loginAPI(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            prepBR(this.br);
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                account.removeProperty(PROPERTY_ACCOUNT_HAS_SHOWN_APIKEY_HELP_DIALOG);
            }
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                /**
                 * First try to migrate old accounts which still used website login. </br>
                 * Website cookies contain the API key too -> Extract and set this. Then delete cookies as we don't need them anymore.
                 */
                logger.info("Trying to convert website cookie to API key");
                final List<Cookie> allCookies = cookies.getCookies();
                Cookie apikeyCookie = null;
                for (final Cookie cookie : allCookies) {
                    if (StringUtils.equals(cookie.getKey(), "pd_auth_key")) {
                        apikeyCookie = cookie;
                        break;
                    }
                }
                if (apikeyCookie != null) {
                    final String apikey = apikeyCookie.getValue();
                    logger.info("Successfully found apikey in cookie: " + apikey);
                    account.setPass(apikey);
                } else {
                    /* This should never happen. In this case user will have to manually re-add account via apikey. */
                    logger.warning("Failed to find apikey in cookie");
                }
                /* Remove cookies as this is a one try event. */
                account.clearCookies("");
            }
            final String apikey = account.getPass();
            if (!isAPIKEY(apikey)) {
                showApiLoginInformation(account);
                throw new AccountInvalidException("Invalid API key format");
            }
            /* Set login auth header */
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Basic " + Encoding.Base64Encode(":" + apikey));
            if (!force) {
                logger.info("Trust apikey without check");
                return;
            }
            logger.info("Validating apikey");
            br.getPage(getAPIURLUser());
            final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            if (br.getHttpConnection().getResponseCode() == 401 || (Boolean) response.get("success") == Boolean.FALSE) {
                showApiLoginInformation(account);
                throw new AccountInvalidException("Invalid API key");
            }
            return;
        }
    }

    private void checkErrors(final Browser br, final DownloadLink link, final Account account) {
        /* TODO: Add functionality */
    }

    private void showApiLoginInformation(final Account account) {
        synchronized (account) {
            if (!account.hasProperty(PROPERTY_ACCOUNT_HAS_SHOWN_APIKEY_HELP_DIALOG)) {
                /* Only display this dialog once per account! */
                account.setProperty(PROPERTY_ACCOUNT_HAS_SHOWN_APIKEY_HELP_DIALOG, true);
                final Thread thread = new Thread() {
                    public void run() {
                        try {
                            String message = "";
                            final String title;
                            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                                title = "Pixeldrain - Login";
                                message += "Hallo liebe(r) Pixeldrain NutzerIn\r\n";
                                message += "Um deinen Pixeldrain Account in JDownloader verwenden zu können, musst du auf folgender Pixeldrain Seite einen API Key erstellen und ihn in JDownloader ins 'Passwort' bzw. 'API Key' Feld eingeben:\r\n";
                                message += PIXELDRAIN_JD_API_HELP_PAGE;
                            } else {
                                title = "Pixeldrain - Login";
                                message += "Hello dear Pixeldrain user\r\n";
                                message += "In order to use an account of this service in JDownloader, you need to generate an API key on the following page and put it into the 'Password' or 'API Key' field in JDownloader:\r\n";
                                message += PIXELDRAIN_JD_API_HELP_PAGE;
                            }
                            final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                            dialog.setTimeout(3 * 60 * 1000);
                            if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                                CrossSystem.openURL(PIXELDRAIN_JD_API_HELP_PAGE);
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
            }
        }
    }

    protected static boolean isAPIKEY(final String str) {
        if (str == null) {
            return false;
        } else if (str.replace("-", "").matches("[a-f0-9]{32}")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isLoggedinWebsite(final Browser br) {
        return br.getCookie(this.getHost(), "pd_auth_key", Cookies.NOTDELETEDPATTERN) != null && br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        /**
         * 2021-01-15: (Free) Accounts = No captcha required for downloading (usually not even via anonymous files but captchas can
         * sometimes be required for files with high traffic). </br>
         * There are also "Donator" Accounts (at this moment we don't try to differ between them) but the download process is no different
         * when using those!
         */
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!StringUtils.equalsIgnoreCase(br.getURL(), getAPIURLUser())) {
            br.getPage(getAPIURLUser());
        }
        final Map<String, Object> user = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        /* User will always only have one running subscription. */
        final Map<String, Object> subscription = (Map<String, Object>) user.get("subscription");
        ai.setUsedSpace(((Number) user.get("storage_space_used")).longValue());
        if (useAPIForLogin()) {
            /* Not necessarily a username/mail given but we need one as our "primary key". */
            account.setUser(user.get("email").toString());
        }
        final String subscriptionType = (String) subscription.get("type");
        if (StringUtils.isEmpty(subscriptionType) || subscriptionType.equalsIgnoreCase("free")) {
            /* Assume it's a free account */
            account.setType(AccountType.FREE);
            ai.setUnlimitedTraffic();
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        }
        account.setConcurrentUsePossible(true);
        final long monthlyTrafficMax = ((Number) subscription.get("monthly_transfer_cap")).longValue();
        if (monthlyTrafficMax == -1) {
            ai.setUnlimitedTraffic();
        } else {
            ai.setTrafficMax(monthlyTrafficMax);
            ai.setTrafficLeft(monthlyTrafficMax - ((Number) user.get("monthly_transfer_used")).longValue());
        }
        String accountStatusText = "Package: " + subscription.get("name");
        final double euroBalance = ((Number) user.get("balance_micro_eur")).doubleValue();
        accountStatusText += String.format(" | Balance: %2.2f€", euroBalance / 1000000);
        ai.setStatus(accountStatusText);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        if (acc != null && acc.getType() == AccountType.PREMIUM) {
            return false;
        } else if (link.hasProperty(PROPERTY_CAPTCHA_REQUIRED)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(final InputChangedCallbackInterface callback) {
        if (useAPIForLogin()) {
            return new PixeldrainAccountFactory(callback);
        } else {
            return new DefaultEditAccountPanel(callback, !getAccountwithoutUsername());
        }
    }

    public static class PixeldrainAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final String      APIKEYHELP       = "Enter your API Key";

        private String getPassword() {
            if (this.pass == null) {
                return null;
            } else if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
                return null;
            } else {
                return new String(this.pass.getPassword());
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
        private static String          EMPTYPW = "                 ";
        private final JLabel           apikeyLabel;

        public PixeldrainAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to generate an API Key for JD:"));
            add(new JLink(PIXELDRAIN_JD_API_HELP_PAGE));
            add(apikeyLabel = new JLabel("API Key: [a-f]{32}"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(APIKEYHELP);
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
            final String password = getPassword();
            if (isAPIKEY(password)) {
                apikeyLabel.setForeground(Color.BLACK);
                return true;
            } else {
                apikeyLabel.setForeground(Color.RED);
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