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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "foxleech.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class FoxLeechCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static final String                            MAINPAGE           = "http://foxleech.com";
    private static final String                            NICE_HOST          = MAINPAGE.replaceAll("(https://|http://)", "");
    private static final String                            NICE_HOSTproperty  = MAINPAGE.replaceAll("(https://|http://|\\.|\\-)", "");
    private static final String                            USE_API            = "USE_API";

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
        AccountInfo ac = new AccountInfo();
        if (this.useAPI()) {
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

    /* no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        String dllink = checkDirectLink(link, NICE_HOST + "directlink");
        if (dllink == null) {
            if (this.useAPI()) {
                dllink = api_get_dllink(link, acc);
            } else {
                dllink = site_get_dllink(link, acc);
            }
        }
        int maxChunks = 1;
        if (link.getBooleanProperty(FoxLeechCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("Unhandled download error on " + NICE_HOST + ": " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(NICE_HOST + "directlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(FoxLeechCom.NOCHUNKS, false) == false) {
                    link.setProperty(FoxLeechCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            if (maxChunks == 1) {
                link.setProperty(NICE_HOST + "directlink", Property.NULL);
            }
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(FoxLeechCom.NOCHUNKS, false) == false) {
                link.setProperty(FoxLeechCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handlePluginBroken(final Account acc, final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            // tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
            /* Test mode, usually never throw plugin defect */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        br.getPage("http://www.foxleech.com/account");
        final String[] hosts = br.getRegex("class=\"host\\-icon\\-work\" alt=\"([^<>\"]*?)\"").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final String host : hosts) {
            supportedHosts.add(host);
        }
        long expire = System.currentTimeMillis();
        String days, hours, minutes, seconds;
        final Regex expireinfo = br.getRegex("type=\"text\"value=\"(\\d{2})Days (\\d{2})Hours (\\d{2})Minutes (\\d{2})Seconds \"");
        days = expireinfo.getMatch(0);
        hours = expireinfo.getMatch(1);
        minutes = expireinfo.getMatch(2);
        seconds = expireinfo.getMatch(3);
        if (days != null) {
            expire += Long.parseLong(days) * 24 * 60 * 60 * 1000;
        }
        if (hours != null) {
            expire += Long.parseLong(hours) * 60 * 60 * 1000;
        }
        if (minutes != null) {
            expire += Long.parseLong(minutes) * 60 * 1000;
        }
        if (seconds != null) {
            expire += Long.parseLong(seconds) * 1000;
        }
        ac.setValidUntil(expire);
        /* They only have accounts with traffic, no free/premium difference (other than no traffic) */
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        ac.setMultiHostSupport(supportedHosts);
        ac.setStatus("Premium User");
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
                            br.getPage("http://www.foxleech.com/downloader");
                            if (br.containsHTML("title=\"logout\">Logout</a>")) {
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
                br.getPage("http://www.foxleech.com/login");
                String postData = "B1=Login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                br.postPage("http://www.foxleech.com/login", postData);
                /* Even if the user entrs valid username/password, captcha may occur... */
                if (br.getCookie(MAINPAGE, "auth") == null && br.containsHTML("google\\.com/recaptcha/api")) {
                    /* TODO: Fix login captcha handling - it never works! */
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.findID();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", NICE_HOST, MAINPAGE, true);
                    final String c = getCaptchaCode(cf, dummyLink);
                    postData += "&recaptcha_challenge_field" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c);
                    br.postPage("http://www.foxleech.com/login", postData);
                }
                if (br.getCookie(MAINPAGE, "auth") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder Login-Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort (und Captcha) stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password (and captcha) you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                return true;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                return false;
            }
        }
    }

    private String site_get_dllink(final DownloadLink link, final Account acc) throws Exception {
        site_login(acc, false);
        final String url = Encoding.urlEncode(link.getDownloadURL());
        br.getPage("http://www.foxleech.com/Generate.php?link=" + url);
        String dllink = br.getRegex("\"link\":\"(http[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            handlePluginBroken(acc, link, "dllink_null", 10);
        }
        dllink = dllink.replace("\\", "");
        return dllink;
    }

    private AccountInfo api_fetchAccountInfo(final Account account) throws Exception {
        if (!api_login(account, true)) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder Login-Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort (und Captcha) stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password (and captcha) you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        return null;
    }

    private boolean api_login(final Account account, final boolean force) throws Exception {
        return false;
    }

    private String api_get_dllink(final DownloadLink link, final Account acc) throws Exception {
        return null;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
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

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        return true;
    }

    private static final boolean default_USE_API = false;

    protected boolean useAPI() {
        return getPluginConfig().getBooleanProperty(USE_API, default_USE_API);
    }

    private void setConfigElements() {
        /* No API available yet... */
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