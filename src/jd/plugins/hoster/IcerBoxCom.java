//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "icerbox.com" }, urls = { "https?://(www\\.)?icerbox\\.com/[A-Z0-9]{8}" })
public class IcerBoxCom extends antiDDoSForHost {
    private final String language = System.getProperty("user.language");
    private final String baseURL  = "https://icerbox.com";
    private final String apiURL   = "https://icerbox.com/api/v1";

    public IcerBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(baseURL + "/premium");
        // setConfigElement();
    }

    @Override
    public String getAGBLink() {
        return baseURL + "/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    private void setConstants(final Account account) {
        if (account != null) {
            if (account.getType() == AccountType.FREE) {
                // free account
                chunks = 1;
                resumes = false;
                isFree = true;
                directlinkproperty = "freelink2";
            } else {
                // premium account
                chunks = 0;
                resumes = true;
                isFree = false;
                directlinkproperty = "premlink";
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = false;
            isFree = true;
            directlinkproperty = "freelink";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    private boolean freedl = false;

    @Override
    protected boolean useRUA() {
        if (freedl) {
            return true;
        }
        return false;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.addAllowedResponseCodes(500);
        }
        return prepBr;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink != null) {
            // at this given time only premium acn be downloaded
            if (account != null && account.getType() == AccountType.PREMIUM) {
                return true;
            }
        }
        return false;
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        boolean okay = true;
        try {
            final Browser br = new Browser();
            br.getHeaders().put("Accept", "application/json");
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    if (links.size() == 100 || index == urls.length) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                final StringBuilder sb = new StringBuilder();
                boolean atLeastOneDL = false;
                for (final DownloadLink dl : links) {
                    if (atLeastOneDL) {
                        sb.append(",");
                    }
                    sb.append(getFUID(dl));
                    atLeastOneDL = true;
                }
                getPage(br, apiURL + "/files?ids=" + sb);
                if (br.containsHTML("In these moments we are upgrading the site system")) {
                    for (final DownloadLink dl : links) {
                        dl.getLinkStatus().setStatusText("Hoster is in maintenance mode. Try again later");
                        dl.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                    }
                    return true;
                }
                for (final DownloadLink dl : links) {
                    final String filter = br.getRegex("(\\{\"id\":\"" + getFUID(dl) + "\",.*?\\})").getMatch(0);
                    if (filter == null) {
                        dl.setProperty("apiInfo", Property.NULL);
                        okay = false;
                        continue;
                    }
                    final String status = PluginJSonUtils.getJsonValue(filter, "status");
                    if ("active".equalsIgnoreCase(status)) {
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                    final String name = PluginJSonUtils.getJsonValue(filter, "name");
                    final String size = PluginJSonUtils.getJsonValue(filter, "size");
                    final String md5 = PluginJSonUtils.getJsonValue(filter, "md5");
                    final String prem = PluginJSonUtils.getJsonValue(filter, "free_available");
                    final String pass = PluginJSonUtils.getJsonValue(filter, "password");
                    if (name != null) {
                        dl.setFinalFileName(name);
                    }
                    if (size != null) {
                        dl.setVerifiedFileSize(Long.parseLong(size));
                    }
                    if (md5 != null) {
                        dl.setMD5Hash(md5);
                    }
                    if (prem != null) {
                        dl.setProperty("premiumRequired", Boolean.parseBoolean(prem));
                    } else {
                        dl.setProperty("premiumRequired", Property.NULL);
                    }
                    if (pass != null) {
                        dl.setProperty("passwordRequired", Boolean.parseBoolean(pass));
                    } else {
                        dl.setProperty("passwordRequired", Property.NULL);
                    }
                    dl.setProperty("apiInfo", Boolean.TRUE);
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return okay;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (useAPI()) {
            return requestFileInformationApi(link);
        } else {
            return AvailableStatus.UNCHECKABLE;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        if (useAPI()) {
            handleDownload_API(downloadLink, null);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    protected static Object LOCK = new Object();

    /**
     * useAPI frame work? <br />
     * Override this when incorrect
     *
     * @return
     */
    private boolean useAPI() {
        return true;
        // return getPluginConfig().getBooleanProperty(preferAPI, preferAPIdefault);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return fetchAccountInfoApi(account);
    }

    private AccountInfo fetchAccountInfoApi(final Account account) throws Exception {
        synchronized (LOCK) {
            try {
                if (inValidate(account.getUser()) || !account.getUser().matches(".+@.+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid username (must be email address)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final AccountInfo ai = new AccountInfo();
                Browser ajax = new Browser();
                ajax.getHeaders().put("Accept", "application/json");
                postPage(ajax, apiURL + "/auth/login", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                handleApiErrors(ajax, account, null);
                // recaptcha can happen here on brute force attack
                if (ajax.getHttpConnection().getResponseCode() == 429) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account Login", getHost(), getHost(), true);
                    final DownloadLink odl = this.getDownloadLink();
                    this.setDownloadLink(dummyLink);
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, ajax, "6LcKRRITAAAAAExk3Pb2MfEBMP7HGTk8HG4cRBXv").getToken();
                    if (odl != null) {
                        this.setDownloadLink(odl);
                    }
                    postPage(ajax, apiURL + "/auth/login", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
                    handleApiErrors(ajax, account, null);
                    if (ajax.getHttpConnection().getResponseCode() == 429) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                // token
                final String token = PluginJSonUtils.getJsonValue(ajax, "token");
                if (token == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                account.setProperty("token", token);
                ajax.getHeaders().put("Authorization", "Bearer " + account.getStringProperty("token"));
                getPage(ajax, apiURL + "/user/account");
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
                final Boolean is_premium = (Boolean) JavaScriptEngineFactory.walkJson(entries, "data/has_premium");
                final String expire = (String) JavaScriptEngineFactory.walkJson(entries, "data/premium/date");
                final Long dailyTrafficMax = (Long) JavaScriptEngineFactory.walkJson(entries, "data/package/volume");
                final Long dailyTrafficUsed = ((Number) JavaScriptEngineFactory.walkJson(entries, "data/downloaded_today")).longValue();
                // free account
                if (Boolean.FALSE.equals(is_premium)) {
                    // jdlog://8835079150841
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Free Accounts on this provider are not supported", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    // available traffic
                    ai.setTrafficLeft(dailyTrafficMax - dailyTrafficUsed);
                    ai.setTrafficMax(dailyTrafficMax);
                    // date
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.replaceFirst("\\.0{6}", ""), "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), ajax);
                    if (Boolean.TRUE.equals(is_premium) && !ai.isExpired()) {
                        // premium account
                        account.setType(AccountType.PREMIUM);
                        ai.setStatus("Premium Account");
                        account.setValid(true);
                    } else {
                        // this shouldn't happen....
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Free Accounts on this provider are not supported", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    return ai;
                }
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty("token");
                }
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account);
        if (useAPI()) {
            handleDownload_API(downloadLink, account);
            return;
        }
    }

    private void handleDownload_API(final DownloadLink downloadLink, final Account account) throws Exception {
        // prevent more than one download starting at a time, so that we don't perform relogin multiple times within synchronised queues
        synchronized (LOCK) {
            requestFileInformationApi(downloadLink);
            dllink = checkDirectLink(downloadLink, directlinkproperty);
            if (inValidate(dllink)) {
                // links that require premium...
                if (downloadLink.getBooleanProperty("premiumRequired", false) && account == null) {
                    throwPremiumRequiredException(downloadLink, true);
                }
                br = new Browser();
                br.getHeaders().put("Accept", "application/json");
                if (account != null && account.getType() == AccountType.PREMIUM) {
                    final String token = account.getStringProperty("token", null);
                    if (token == null) {
                        // this should not happen.
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String req = apiURL + "/dl/ticket";
                    br.getHeaders().put("Authorization", "Bearer " + token);
                    postPage(req, "file=" + getFUID(downloadLink));
                    dllink = PluginJSonUtils.getJsonValue(br, "url");
                } else {
                    // currently only premium accounts can download... pointless making other download support at this time
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                }
                handleApiErrors(br, account, downloadLink);
                if (inValidate(dllink)) {
                    if (br.toString().matches("Connect failed: Can't connect to local MySQL server.+")) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
        if (!dl.getConnection().isContentDisposition()) {
            downloadLink.setProperty(directlinkproperty, Property.NULL);
            handleDownloadErrors(account, downloadLink, true);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private final void handleDownloadErrors(final Account account, final DownloadLink downloadLink, final boolean lastChance) throws PluginException, IOException {
        br.followConnection();
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleApiErrors(final Browser br, final Account account, final DownloadLink downloadLink) throws Exception {
        final int resCode = br.getHttpConnection().getResponseCode();
        if (resCode == 401 && downloadLink == null) {
            br.followConnection();
            // invalid user/pass
            if ("de".equalsIgnoreCase(language)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("pl".equalsIgnoreCase(language)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (resCode == 403 || resCode == 401) {
            br.followConnection();
            if (account != null && downloadLink == null) {
                // banned user within login only
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account has been banned!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            // admin states that download routine can throw this as well.
            // we will invalidate the session token, throw temp hoster unvail, and relogin.
            else if (account != null && downloadLink != null) {
                final long was = account.getAccountInfo().getTrafficLeft();
                // relogin due to limit been reached and token equals null.
                account.setAccountInfo(fetchAccountInfoApi(account));
                final long now = account.getAccountInfo().getTrafficLeft();
                // throw retry so that core can re-analyse if account has traffic left.
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You are possibly out of available traffic; 'was = " + was + "; now = " + now + ";", 30 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private void throwPremiumRequiredException(DownloadLink link, boolean setProperty) throws PluginException {
        if (setProperty) {
            link.setProperty("premiumRequired", Boolean.TRUE);
        }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        if (property != null) {
            final String dllink = downloadLink.getStringProperty(property);
            if (dllink != null) {
                URLConnectionAdapter con = null;
                try {
                    final Browser br2 = br.cloneBrowser();
                    br2.setFollowRedirects(true);
                    try {
                        // @since JD2
                        con = br2.openHeadConnection(dllink);
                    } catch (final Throwable t) {
                        con = br2.openGetConnection(dllink);
                    }
                    if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                        downloadLink.setProperty(property, Property.NULL);
                    } else {
                        return dllink;
                    }
                } catch (final Exception e) {
                    downloadLink.setProperty(property, Property.NULL);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return null;
    }

    private AvailableStatus requestFileInformationApi(final DownloadLink link) throws IOException, PluginException {
        final boolean checked = checkLinks(new DownloadLink[] { link });
        // we can't throw exception in checklinks! This is needed to prevent multiple captcha events!
        if (!checked && hasAntiddosCaptchaRequirement()) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (!checked || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.setProperty("apiInfo", Property.NULL);
            link.setProperty("freelink2", Property.NULL);
            link.setProperty("freelink", Property.NULL);
            link.setProperty("premlink", Property.NULL);
            link.setProperty("premiumRequired", Property.NULL);
            link.setProperty("passwordRequired", Property.NULL);
        }
    }

    private String  dllink             = null;
    private String  directlinkproperty = null;
    private int     chunks             = 0;
    private boolean resumes            = true;
    private boolean isFree             = true;

    private String getFUID(DownloadLink downloadLink) {
        final String fuid = new Regex(downloadLink.getDownloadURL(), "icerbox\\.com/([A-Z0-9]+)").getMatch(0);
        return fuid;
    }

    public boolean hasCaptcha(final DownloadLink downloadLink, final jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    protected void runPostRequestTask(final Browser ibr) throws PluginException {
        if (ibr.containsHTML(">OUR WEBSITE IS UNDER CONSTRUCTION</strong>")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Website Under Construction!", 15 * 60 * 1000l);
        }
    }
}