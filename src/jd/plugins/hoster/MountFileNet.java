//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mountfile.net" }, urls = { "https?://(?:www\\.)?mountfile\\.net/(?!d/)[A-Za-z0-9]+" })
public class MountFileNet extends antiDDoSForHost {
    /* For reconnect special handling */
    private static Object            CTRLLOCK                      = new Object();
    private final String             EXPERIMENTALHANDLING          = "EXPERIMENTALHANDLING";
    private static Map<String, Long> blockedIPsMap                 = new HashMap<String, Long>();
    private final String             PROPERTY_LAST_BLOCKED_IPS_MAP = "mountfilenet_last_blockedIPsMap";
    private static final long        FREE_RECONNECTWAIT_GENERAL    = 1 * 60 * 60 * 1000L;

    public MountFileNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://mountfile.net/premium/");
    }

    @Override
    public String getAGBLink() {
        return "https://mountfile.net/terms/";
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        } else if (AccountType.FREE.equals(acc.getType())) {
            /* free accounts also have captchas */
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found<") || br.getURL().equals("http://mountfile.net/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex fileInfo = br.getRegex("<h2\\s*style\\s*=\\s*\"margin:0\"\\s*>\\s*([^<>\"]*?)\\s*</h2>\\s*<div\\s*class\\s*=\\s*\"comment\"\\s*>\\s*([0-9\\.,\\sTGKBM]+)");
        final String filename = fileInfo.getMatch(0);
        final String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) {
            if (!br.containsHTML("class=\"downloadPageTableV2\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename).trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    private void errorHandling(final Browser br, final Account account, final DownloadLink link) throws PluginException {
        if (br.containsHTML("(?i)you have reached a download limit for today")) {
            /* 2015-09-15: daily downloadlimit = 20 GB */
            logger.info("Daily downloadlimit reached");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (br.containsHTML("(?i)File was deleted by owner or due to a violation of service rules")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)Unfortunately, it can be downloaded only with premium")) {
            // <span class="error">File size is larger than 1 GB. Unfortunately, it can be downloaded only with premium</span>
            throw new AccountRequiredException();
        } else if (br.containsHTML("(?i)>\\s*Sorry, you have reached a download limit for today \\([\\w \\.]+\\)\\. Please wait for tomorrow")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Reached the download limit!", 60 * 60 * 1000l);
        } else if (br.containsHTML("(?i)>\\s*All slots for the slow download are in use now, please try again later\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "All slots for the slow download are in use now, please try again later", 30 * 60 * 1000l);
        }
        String reconnectWait = br.getRegex("(?i)You should wait (\\d+) minutes before downloading next file").getMatch(0);
        if (reconnectWait == null) {
            reconnectWait = br.getRegex("(?i)Please wait (\\d+) minutes before downloading next file or").getMatch(0);
        }
        if (reconnectWait != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 60 * 1001l);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        doFree(link, null);
    }

    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        final String currentIP = new BalancedWebIPCheck(null).getExternalIP().getIP();
        final boolean useExperimentalHandling = this.getPluginConfig().getBooleanProperty(this.EXPERIMENTALHANDLING, false);
        long lastdownload = 0;
        long passedTimeSinceLastDl = 0;
        synchronized (CTRLLOCK) {
            /* Load list of saved IPs + timestamp of last download */
            final Object lastdownloadmap = this.getPluginConfig().getProperty(PROPERTY_LAST_BLOCKED_IPS_MAP);
            if (lastdownloadmap != null && lastdownloadmap instanceof Map && blockedIPsMap.isEmpty()) {
                blockedIPsMap.putAll((Map<String, Long>) lastdownloadmap);
            }
        }
        if (useExperimentalHandling) {
            /*
             * If the user starts a download in free (unregistered) mode the waittime is on his IP. This also affects free accounts if he
             * tries to start more downloads via free accounts afterwards BUT nontheless the limit is only on his IP so he CAN download
             * using the same free accounts after performing a reconnect!
             */
            lastdownload = getPluginSavedLastDownloadTimestamp(currentIP);
            passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload;
            if (passedTimeSinceLastDl < FREE_RECONNECTWAIT_GENERAL) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, FREE_RECONNECTWAIT_GENERAL - passedTimeSinceLastDl);
            }
        }
        requestFileInformation(link);
        errorHandling(br, null, link);
        postPage(br.getURL(), "free=Slow+download&hash=" + fid);
        errorHandling(br, null, link);
        final long timeBefore = System.currentTimeMillis();
        if (br.containsHTML("<div id=\"(\\w+)\".+grecaptcha\\.render\\(\\s*'\\1',")) {
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            waitTime(timeBefore, link);
            postPage(br.getURL(), "free=Get+download+link&hash=" + fid + "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
        } else if (containsHCaptcha(this.br)) {
            /* 2021-06-28 */
            final String hcaptchaResponse = new CaptchaHelperHostPluginHCaptcha(this, br).getToken();
            waitTime(timeBefore, link);
            postPage(br.getURL(), "free=Get+download+link&hash=" + fid + "&g-recaptcha-response=" + Encoding.urlEncode(hcaptchaResponse) + "&h-captcha-response=" + Encoding.urlEncode(hcaptchaResponse));
        }
        errorHandling(br, null, link);
        String dllink = br.getRegex("\"(https?://d\\d+\\.mountfile.net/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<div style=\"margin: 10px auto 20px\" class=\"center\">[\t\n\r ]+<a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, false, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            errorHandling(br, null, link);
            if (br.containsHTML("not found")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        blockedIPsMap.put(currentIP, System.currentTimeMillis());
        getPluginConfig().setProperty(PROPERTY_LAST_BLOCKED_IPS_MAP, blockedIPsMap);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private void waitTime(long timeBefore, final DownloadLink link) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        int wait = 60;
        final String ttt = br.getRegex("var sec = (\\d+)").getMatch(0);
        if (ttt != null) {
            wait = Integer.parseInt(ttt);
        }
        wait -= passedTime;
        if (wait > 0) {
            sleep(wait * 1000l, link);
        }
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    this.br.getPage("https://" + this.getHost() + "/");
                    if (this.br.containsHTML("account/logout/")) {
                        return;
                    }
                    this.br = new Browser();
                }
                br.setFollowRedirects(true);
                getPage("https://" + this.getHost() + "/account/login/");
                String postData = "url=http%253A%252F%252Fmountfile.net%252F&send=sign+in&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                final String captchaurl = this.br.getRegex("(/captcha/\\?\\d+)").getMatch(0);
                if (captchaurl != null) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), "http://" + this.getHost(), true);
                    final String c = getCaptchaCode(captchaurl, dummyLink);
                    postData += "&captcha=" + Encoding.urlEncode(c);
                }
                postPage("/account/login", postData);
                if (br.getCookie(br.getHost(), "usid", Cookies.NOTDELETEDPATTERN) == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        login(account, true);
        String expire = br.getRegex("premium till (\\d{2}/\\d{2}/\\d{2})").getMatch(0);
        if (expire == null) {
            expire = br.getRegex("premium till ([A-Za-z]+ \\d{1,2}, \\d{4})").getMatch(0);
        }
        final boolean is_premium_without_expiredate = br.containsHTML("eternal premium");
        if (!is_premium_without_expiredate && expire == null) {
            account.setType(AccountType.FREE);
        } else {
            if (expire != null && expire.matches("\\d{2}/\\d{2}/\\d{2}")) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", Locale.ENGLISH));
            } else if (expire != null) {
                /* New 2016-10-19 */
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMMM dd, yyyy", Locale.ENGLISH));
            }
            account.setType(AccountType.PREMIUM);
        }
        /* List of downloads done by the user. It also contains information about the remaining traffic. */
        br.getPage("/stat/download");
        final Regex trafficleftInfo = br.getRegex("(?i)Downloaded in last 24 hours: ([0-9\\.]+) of ([0-9\\.]+) GB\\s*<");
        if (trafficleftInfo.patternFind()) {
            final String sizeUnit = "GB";
            final Long trafficMax = SizeFormatter.getSize(trafficleftInfo.getMatch(1) + sizeUnit);
            ai.setTrafficMax(trafficMax);
            ai.setTrafficLeft(trafficMax - SizeFormatter.getSize(trafficleftInfo.getMatch(0) + sizeUnit));
        } else {
            logger.warning("Failed to find traffic left information");
        }
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            login(account, false);
            doFree(link, account);
        } else {
            requestFileInformation(link);
            login(account, false);
            errorHandling(br, account, link);
            /* First check if user has direct download enabled. */
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, link.getDownloadURL(), true, 0);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                /* No direct download? Manually get directurl ... */
                br.followConnection(true);
                errorHandling(br, account, link);
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                postPage("/load/premium/", "js=1&hash=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
                errorHandling(br, account, link);
                String dllink = PluginJSonUtils.getJsonValue(this.br, "ok");
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
                if (!looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    errorHandling(br, account, link);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private long getPluginSavedLastDownloadTimestamp(final String currentIP) {
        long lastdownload = 0;
        synchronized (blockedIPsMap) {
            final Iterator<Entry<String, Long>> it = blockedIPsMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Long> ipentry = it.next();
                final String ip = ipentry.getKey();
                final long timestamp = ipentry.getValue();
                if (System.currentTimeMillis() - timestamp >= FREE_RECONNECTWAIT_GENERAL) {
                    /* Remove old entries */
                    it.remove();
                }
                if (ip.equals(currentIP)) {
                    lastdownload = timestamp;
                }
            }
        }
        return lastdownload;
    }

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), this.EXPERIMENTALHANDLING, JDL.L("plugins.hoster.mountfilenet.useExperimentalWaittimeHandling", "Activate experimental waittime handling to prevent additional captchas?")).setDefaultValue(false));
    }

    private static AtomicReference<String> userAgent = new AtomicReference<String>(null);

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}