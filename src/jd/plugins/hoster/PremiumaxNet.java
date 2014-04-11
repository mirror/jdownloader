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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumax.net" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class PremiumaxNet extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(20);
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static final String                            MAINPAGE           = "http://premiumax.net";
    private static final String                            NICE_HOST          = "premiumax.net";
    private static final String                            NICE_HOSTproperty  = "premiumaxnet";
    private static final String[][]                        HOSTS              = { { "4shared", "4shared.com" }, { "asfile", "asfile.com" }, { "bitshare", "bitshare.com" }, { "datafile", "datafile.com," }, { "ddlstorage", "ddlstorage.com" }, { "depfile", "depfile.com" }, { "depositfiles", "depositfiles.com" }, { "dizzcloud", "dizzcloud.com" }, { "easybytez", "easybytez.com" }, { "extmatrix", "extmatrix.com" }, { "fayloobmennik", "fayloobmennik.net" }, { "filecloud", "filecloud.io" }, { "filefactory", "filefactory.com" }, { "filemonkey", "filemonkey.in" }, { "fileom", "fileom.com" }, { "filepost", "filepost.com" }, { "filesflash", "filesflash.com" }, { "filesmonster", "filesmonster.com" }, { "firedrive", "firedrive.com" }, { "freakshare", "freakshare.com" }, { "hugefiles", "hugefiles.net" }, { "hulkfile", "hulkfile.eu" }, { "k2share", "keep2share.cc" }, { "kingfiles", "kingfiles.net" },
            { "letitbit", "letitbit.net" }, { "luckyshare", "luckyshare.net" }, { "lumfile", "lumfile.com" }, { "mediafire", "mediafire.com" }, { "megairon", "megairon.net" }, { "megashares", "megashares.com" }, { "mightyupload", "mightyupload.com" }, { "netload", "netload.in" }, { "novafile", "novafile.com" }, { "putlocker", "putlocker.com" }, { "rapidgator", "rapidgator.net" }, { "rapidshare", "rapidshare.com" }, { "ryushare", "ryushare.com" }, { "sendspace", "sendspace.com" }, { "shareflare", "shareflare.net" }, { "terafile", "terafile.co" }, { "turbobit", "turbobit.net" }, { "ultramegabit", "ultramegabit.com" }, { "uploadable", "uploadable.ch" }, { "uploaded", "uploaded.net" }, { "uppit", "uppit.com" }, { "videomega", "videomega.tv" }, { "zippyshare", "zippyshare.com" } };

    public PremiumaxNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.premiumax.net/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.premiumax.net/more/terms-and-conditions.html";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        if (!login(account, true)) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder Login Captcha falsch eingegeben!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or wrong login captcha input!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        br.getPage("http://www.premiumax.net/profile/");
        boolean is_freeaccount = false;
        final String expire = br.getRegex("<span>Premium until: </span><strong>([^<>\"]*?)</strong>").getMatch(0);
        if (expire != null) {
            ac.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy hh:mm", Locale.ENGLISH));
            ac.setStatus("Premium User");
        } else {
            ac.setStatus("Registered (free) user");
            is_freeaccount = true;
        }
        ac.setUnlimitedTraffic();
        // now let's get a list of all supported hosts:
        final ArrayList<String> supportedHosts = new ArrayList<String>();

        br.getPage("http://www.premiumax.net/hosts.html");
        /* Apply supported hosts depending on account type */
        for (final String[] filehost : HOSTS) {
            final String crippledHost = filehost[0];
            final String realHost = filehost[1];
            final String hostText = br.getRegex("<span>" + crippledHost + "</span>(.*?)</tr>").getMatch(0);
            if (hostText != null) {
                final String[] imgs = new Regex(hostText, "src=\"(tmpl/images/[^<>\"]*?)\"").getColumn(0);
                if (imgs != null && imgs.length >= 4 && imgs[3].equals("tmpl/images/ico_yes.png") && (!is_freeaccount && imgs[2].equals("tmpl/images/ico_yes.png") || is_freeaccount && imgs[1].equals("tmpl/images/ico_yes.png"))) {
                    supportedHosts.add(realHost);
                }
            }
        }
        if (supportedHosts.contains("uploaded.net") || supportedHosts.contains("ul.to") || supportedHosts.contains("uploaded.to")) {
            if (!supportedHosts.contains("uploaded.net")) {
                supportedHosts.add("uploaded.net");
            }
            if (!supportedHosts.contains("ul.to")) {
                supportedHosts.add("ul.to");
            }
            if (!supportedHosts.contains("uploaded.to")) {
                supportedHosts.add("uploaded.to");
            }
        }
        ac.setProperty("multiHostSupport", supportedHosts);
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        login(acc, true);
        String dllink = checkDirectLink(link, "premiumaxnetdirectlink");
        if (dllink == null) {
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://www.premiumax.net/direct_link.html?rand=0." + System.currentTimeMillis(), "captcha=&key=indexKEY&urllist=" + Encoding.urlEncode(link.getDownloadURL()));
            if (br.containsHTML("temporary problem")) {
                logger.info("Current hoster is temporarily not available via premiumax.net -> Disabling it");
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            } else if (br.containsHTML("You do not have the rights to download from")) {
                logger.info("Current hoster is not available via this premiumax.net account -> Disabling it");
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            } else if (br.containsHTML("We do not support your link")) {
                logger.info("Current hoster is not supported by premiumax.net -> Disabling it");
                tempUnavailableHoster(acc, link, 3 * 60 * 60 * 1000l);
            } else if (br.containsHTML("You only can download")) {
                /* We're too fast - usually this should not happen */
                throw new PluginException(LinkStatus.ERROR_RETRY, "Too many connections active, try again in some seconds...");
            } else if (br.containsHTML("> Our server can\\'t connect to")) {
                logger.info(NICE_HOST + ": cantconnect");
                int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "timesfailed_cantconnect", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 10) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "timesfailed_cantconnect", timesFailed);
                    logger.info(NICE_HOST + ": cantconnect -> Retrying");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "cantconnect");
                } else {
                    link.setProperty(NICE_HOSTproperty + "timesfailed_cantconnect", Property.NULL);
                    logger.info(NICE_HOST + ": cantconnect - disabling current host!");
                    tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                }
            }

            dllink = br.getRegex("\"(http://(www\\.)?premiumax\\.net/dl/[a-z0-9]+/?)\"").getMatch(0);
            if (dllink == null) {
                logger.info(NICE_HOST + ": dllinknullerror");
                int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "timesfailed_dllinknullerror", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "timesfailed_dllinknullerror", timesFailed);
                    logger.info(NICE_HOST + ": dllinknullerror -> Retrying");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "dllinknullerror");
                } else {
                    link.setProperty(NICE_HOSTproperty + "timesfailed_dllinknullerror", Property.NULL);
                    logger.info(NICE_HOST + ": dllinknullerror - disabling current host!");
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                }
            }
        }

        int maxChunks = 0;
        if (link.getBooleanProperty(PremiumaxNet.NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("Unhandled download error on premiumax.net: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premiumaxnetdirectlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(PremiumaxNet.NOCHUNKS, false) == false) {
                    link.setProperty(PremiumaxNet.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(PremiumaxNet.NOCHUNKS, false) == false) {
                link.setProperty(PremiumaxNet.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
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

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        /* Avoids enerving login captchas */
                        if (force) {
                            br.getPage("http://www.premiumax.net/");
                            if (br.containsHTML(">Sign out</a>")) {
                                return true;
                            } else {
                                br.clearCookies(MAINPAGE);
                                logger.info("Seems like the cookies are no longer valid -> Doing a full refresh");
                            }
                        } else {
                            return true;
                        }
                    }
                }
                br.getPage("http://www.premiumax.net/");
                if (br.containsHTML(">Sign out</a>")) return true;
                final String stayin = br.getRegex("type=\"hidden\" name=\"stayloggedin\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (stayin == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final DownloadLink dummyLink = new DownloadLink(this, "Account", "premiumax.net", "http://premiumax.net", true);
                final String code = getCaptchaCode("http://www.premiumax.net/veriword.php", dummyLink);
                br.postPage("http://www.premiumax.net/", "serviceButtonValue=login&service=login&stayloggedin=" + stayin + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&formcode=" + code);
                if (br.getCookie(MAINPAGE, "WebLoginPE") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
                }
            }
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}