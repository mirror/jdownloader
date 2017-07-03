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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "foxleech.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class FoxLeechCom extends antiDDoSForHost {
    private static MultiHosterManagement mhm       = new MultiHosterManagement("foxleech.com");
    private static final String          NOCHUNKS  = "NOCHUNKS";
    private static final String          MAINPAGE  = "http://foxleech.com";
    private static final String          NICE_HOST = MAINPAGE.replaceAll("https?://", "");
    private static final String          USE_API   = "USE_API";

    public FoxLeechCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.foxleech.com/plans");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.foxleech.com/contact";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac;
        if (true || this.useAPI()) {
            ac = api_fetchAccountInfo(account);
        } else {
            ac = site_fetchAccountInfo(account);
        }
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    /* no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink downloadLink, final Account account) throws Exception {
        mhm.runCheck(account, downloadLink);
        String dllink = checkDirectLink(downloadLink, NICE_HOST + "directlink");
        if (dllink == null) {
            if (true || this.useAPI()) {
                dllink = api_get_dllink(downloadLink, account);
            } else {
                dllink = site_get_dllink(downloadLink, account);
            }
        }
        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(FoxLeechCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("Unhandled download error on " + NICE_HOST + ": " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(NICE_HOST + "directlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(FoxLeechCom.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(FoxLeechCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            if (maxChunks == 1) {
                downloadLink.setProperty(NICE_HOST + "directlink", Property.NULL);
            }
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(FoxLeechCom.NOCHUNKS, false) == false) {
                downloadLink.setProperty(FoxLeechCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private static Object LOCK = new Object();

    private AccountInfo site_fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        if (!site_login(account, true)) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder Login-Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort (und Captcha) stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password (and captcha) you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        // post login you are taken to /mydashboard, here they show days left
        final String expireinfo = br.getRegex("<h4>(\\d+)</h4>\\s*<span>Premium\\s*Days</span>").getMatch(0);
        if (expireinfo == null) {
            throw new AccountInvalidException("Unsupported account type");
        }
        ac.setValidUntil(System.currentTimeMillis() + Long.parseLong(expireinfo) * 24 * 60 * 60 * 1000);
        // this url gives you the api url
        getPage("/account");
        final String api_url = br.getRegex("\"(https?://(www\\.)?foxleech\\.com/api/[^<>\"]*?)\"").getMatch(0);
        if (api_url != null) {
            account.setProperty("api_url", api_url);
        }
        /* They only have accounts with traffic, no free/premium difference (other than no traffic) */
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        // this url gives you host map
        getPage("/hosts");
        final String[][] hosts = br.getRegex("<img class=\"host-cool[^>]+></img>\\s*<b>\\s*(.*?)\\s*</b>\\s*</td>\\s*<td[^>]*>\\s*<a[^>]*>\\s*(.*?)</a>").getMatches();
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final String host[] : hosts) {
            if (!"Online".equalsIgnoreCase(host[1])) {
                continue;
            }
            supportedHosts.add(host[0]);
        }
        ac.setMultiHostSupport(this, supportedHosts);
        ac.setStatus("Premium Account");
        return ac;
    }

    @SuppressWarnings("unchecked")
    private boolean site_login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        /* Avoid login captcha on forced login */
                        if (force) {
                            getPage("http://www.foxleech.com/mydashboard");
                            if (br.containsHTML(">\\s*Logout\\s*</a>")) {
                                return true;
                            } else {
                                /* Foced login (check) failed - clear cookies and perform a full login! */
                                br.clearCookies(MAINPAGE);
                                account.setProperty("cookies", Property.NULL);
                            }
                        } else {
                            return true;
                        }
                    }
                }
                br.setFollowRedirects(true);
                getPage("http://www.foxleech.com/login");
                // ALWAYS USE FORMS!
                final Form login = br.getFormbyProperty("class", "login");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                // now if recaptcha is shown here FIRST we should support! not after a failed attempt!
                final String recaptchav2 = "<div class=\"g-recaptcha\"";
                if (login.containsHTML(recaptchav2)) {
                    if (this.getDownloadLink() == null) {
                        // login wont contain downloadlink
                        this.setDownloadLink(new DownloadLink(this, "Account Login!", this.getHost(), this.getHost(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    login.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                submitForm(login);
                if (br.getCookie(MAINPAGE, "auth") == null) {
                    // if recaptcha is shown here it should mean user:pass is wrong.
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder Login-Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort (und Captcha) stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password (and captcha) you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(this.getHost()));
                return true;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                return false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String site_get_dllink(final DownloadLink downloadLink, final Account account) throws Exception {
        String dllink;
        final String api_url = account.getStringProperty("api_url", null);
        final String url = Encoding.urlEncode(downloadLink.getDownloadURL());
        site_login(account, false);
        if (api_url != null) {
            /* Actually there is no reason to use this but whatever ... */
            postPage(api_url, "link=" + url);
        } else {
            getPage("http://www.foxleech.com/Generate.php?link=" + url);
        }
        handleErrorsGenerateLink(downloadLink, account);
        dllink = PluginJSonUtils.getJsonValue(br, "link");
        if (dllink == null) {
            mhm.handleErrorGeneric(account, downloadLink, "dllink_null", 10);
        }
        return dllink;
    }

    private void handleErrorsGenerateLink(final DownloadLink downloadLink, final Account account) throws PluginException, InterruptedException {
        handleErrorsGenerateLink(downloadLink, account, null);
    }

    private void handleErrorsGenerateLink(final DownloadLink downloadLink, final Account account, String inputError) throws PluginException, InterruptedException {
        final String error = inputError != null ? inputError : PluginJSonUtils.getJsonValue(br, "error");
        if (error != null) {
            if (error.contains("You have reached the daily limit for")) {
                /* Daily limit of a single host is reached */
                mhm.putError(account, downloadLink, 3 * 60 * 60 * 1000l, "Download limit reached for this host");
            } else if (error.equals("You don't have enough bandwidth to download this link")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "not enough quota left to download this link", 30 * 60 * 1000l);
            }
            mhm.handleErrorGeneric(account, downloadLink, "error_unknown", 10);
        }
    }

    private AccountInfo api_fetchAccountInfo(final Account account) throws Exception {
        synchronized (LOCK) {
            final AccountInfo ai = new AccountInfo();
            postPage("http://www.foxleech.com/api/jdownloader", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final String accountType = (String) entries.get("account");
            if (!"premium".equalsIgnoreCase(accountType)) {
                // unsupported account type
                throw new AccountInvalidException("Unsupported account type:" + accountType);
            }
            final Object expire = entries.get("expire_time");
            if (expire != null) {
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(expire.toString()), br);
            }
            final String apiurl = (String) entries.get("api_url");
            if (apiurl != null) {
                account.setProperty("api_url", apiurl);
            }
            final Object trafficLeft = entries.get("traffic_left_bytes");
            if (trafficLeft != null) {
                ai.setTrafficLeft(Long.parseLong(trafficLeft.toString()));
            }
            final Object trafficMax = entries.get("traffic_bytes");
            if (trafficLeft != null) {
                ai.setTrafficMax(Long.parseLong(trafficMax.toString()));
            }
            final String hosts = (String) entries.get("hosts");
            if (hosts != null) {
                ai.setMultiHostSupport(this, Arrays.asList(hosts.split(",")));
            }
            ai.setStatus("Premium Account");
            return ai;
        }
    }

    private String api_get_dllink(final DownloadLink downloadLink, final Account account) throws Exception {
        String api = account.getStringProperty("api_url", null);
        if (api == null) {
            account.setAccountInfo(api_fetchAccountInfo(account));
            account.getStringProperty("api_url", null);
            // not possible!
            if (api == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        postPage(api, "link=" + Encoding.urlEncode(downloadLink.getDownloadURL()));
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final String dllink = (String) entries.get("link");
        if (dllink == null) {
            handleErrorsGenerateLink(downloadLink, account, (String) entries.get("error"));
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property, null);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    private static final boolean default_USE_API = false;

    protected boolean useAPI() {
        return getPluginConfig().getBooleanProperty(USE_API, default_USE_API);
    }

    private void setConfigElements() {
        /* No API available yet: http://svn.jdownloader.org/issues/46706 */
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), USE_API,
        // JDL.L("plugins.hoster.FoxLeechCom.useAPI", "Use API (recommended!)")).setDefaultValue(default_USE_API));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}