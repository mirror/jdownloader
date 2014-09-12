//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedyshare.com" }, urls = { "http://www(\\d+)?\\.speedyshare\\.com/remote/[A-Za-z0-9]+/d\\d+\\-[A-Za-z0-9]+|http://(www\\.)?(speedyshare\\.com|speedy\\.sh)/(files/)?[A-Za-z0-9]+" }, flags = { 2 })
public class SpeedyShareCom extends PluginForHost {

    private static final String                            PREMIUMONLY        = "(>This paraticular file can only be downloaded after you purchase|this file can only be downloaded with SpeedyShare Premium)";
    private static final String                            PREMIUMONLYTEXT    = "Only downloadable for premium users";
    private static final String                            MAINPAGE           = "http://www.speedyshare.com";
    private static final String                            CAPTCHATEXT        = "/captcha\\.php\\?";
    private static Object                                  LOCK               = new Object();
    private final String                                   REMOTELINK         = "http://www(\\d+)?\\.speedyshare\\.com/remote/[A-Za-z0-9]+/d\\d+\\-[A-Za-z0-9]+";
    private String                                         fuid               = null;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public SpeedyShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
        this.enablePremium("http://www.speedyshare.com/premium.php");
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("speedy.sh/", "speedyshare.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.speedyshare.com/terms.php";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36");
        br.getHeaders().put("Accept-Charset", null);
        // br.getHeaders().put("Connection", "keep-alive");
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser();
        setFUID(downloadLink);
        if (downloadLink.getDownloadURL().matches(REMOTELINK)) {
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null) {
                downloadLink.getLinkStatus().setStatusText("This link can only be checked/downloaded when a valid premium account is active");
                return AvailableStatus.UNCHECKABLE;
            } else {
                login(aa, false);
                br.setFollowRedirects(true);
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(downloadLink.getDownloadURL());
                    if (!con.getContentType().contains("html")) {
                        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con).trim()));
                        downloadLink.setDownloadSize(con.getLongContentLength());
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    return AvailableStatus.TRUE;
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        } else {
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("(class=sizetagtext>not found<|File not found|It has been deleted<|>or it never existed at all)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = br.getRegex("\"(og:title|name)\" content=\"Download File: ([^\"]+)").getMatch(1);
            if (filename == null) {
                filename = br.getRegex("<title>(.+) \\- Speedy Share \\- .+</title>").getMatch(0);
            }
            String filesize = br.getRegex("<div class=sizetagtext>(.*?)</div>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("([\\d\\.]+ (KB|MB|GB|TB))").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setName(Encoding.htmlDecode(filename));
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            if (br.containsHTML(PREMIUMONLY)) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.speedysharecom.errors.only4premium", PREMIUMONLYTEXT));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (downloadLink.getDownloadURL().matches(REMOTELINK)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        if (br.containsHTML("The one\\-hour limit has been reached\\. Wait")) {
            String wait[] = br.getRegex("id=minwait1>(\\d+):(\\d+)</span> minutes").getRow(0);
            long waittime = 1000l * 60 * Long.parseLong(wait[0]) + 1000 * Long.parseLong(wait[1]);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        }
        if (br.containsHTML("One hour download limit reached\\. Wait")) {
            long waittime = 30 * 60 * 1000l;
            String wait[] = br.getRegex("One hour download limit reached.*?id=wait.*?>(\\d+):(\\d+)<").getRow(0);
            try {
                waittime = 1000l * 60 * Long.parseLong(wait[0]) + 1000 * Long.parseLong(wait[1]);
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        }
        String finallink = null;
        if (br.containsHTML(PREMIUMONLY)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYTEXT);
        }
        if (!br.containsHTML(CAPTCHATEXT)) {
            finallink = br.getRegex("class=downloadfilename href=\\'(.*?)\\'").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<a href=('|\")([^>]+)\\1><img[^>]+(src=/gf/slowdownload\\.png|alt='Slow Download'|class=dlimg2)").getMatch(1);
                if (finallink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        if (finallink == null) {
            final long timeBefore = System.currentTimeMillis();
            String postLink = br.getRegex("\"(/files?/[A-Za-z0-9]+/download/.*?)\"").getMatch(0);
            String captchaLink = br.getRegex("(/captcha\\.php\\?uid=file\\d+)").getMatch(0);
            if (postLink == null || captchaLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String code = getCaptchaCode(MAINPAGE + captchaLink, downloadLink);
            final int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
            final String waittime = br.getRegex("</div>\\';[\t\n\r ]+secondscounter\\((\\d+)\\);").getMatch(0);
            int wait = 15;
            if (waittime != null) {
                wait = Integer.parseInt(waittime) / 10;
            }
            wait -= passedTime;
            if (wait > 0) {
                sleep(wait * 1001l, downloadLink);
            }
            br.postPage(MAINPAGE + postLink, "captcha=" + Encoding.urlEncode(code));
            finallink = br.getRedirectLocation();
        }
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (finallink.matches("/" + fuid + "/download/.+")) {
            br.getPage(finallink);
            finallink = br.getRedirectLocation();
            doMagic();
            sleep(3000, downloadLink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(CAPTCHATEXT)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            logger.warning("Downloadlink doesn't lead to a file!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBrowser();
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("https://www.speedyshare.com/login.php", "redir=%2user.php&remember=on&login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                String secondLoginurl = br.getRegex("http\\-equiv=\"REFRESH\" content=\"\\d+;url=(http://speedyshare.com/relogin[^<>\"]*?)\"").getMatch(0);
                if (secondLoginurl != null) {
                    br.setFollowRedirects(false);
                    br.getPage(secondLoginurl);
                }
                final String lang = System.getProperty("user.language");
                if (br.containsHTML("<b>Error occured</b>") && br.containsHTML(">Your login information has been used from several networks recently")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin fehlgeschlagen: Account wegen Loginversuchen\r\nüber zu viele verschiedene IPs temporär gesperrt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login failed, host determined you've logged in from too many IP subnets.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.setFollowRedirects(true);
                if (!br.getURL().endsWith("speedyshare.com/user.php")) {
                    br.getPage("http://speedyshare.com/user.php");
                }
                if (br.getCookie(MAINPAGE, "S") == null || !br.containsHTML("<td>Account type:</td><td><b style=\\'color: green\\'>Premium</b>")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            throw e;
        }
        // typical expire time.
        // <td>Account type:</td><td><b style='color: green'>Premium</b> (expires on 2014-08-17)</td></tr>
        String expire = br.getRegex("\\(expires on (\\d{4}-\\d{2}-\\d{2})\\)").getMatch(0);
        if (expire == null) {
            // Perpetual roll over, expire time taken on the assumption of next payment.
            // <td>Account type:</td><td><b style='color: green'>Premium</b></td></tr><tr><td>Automatic payments:&nbsp;
            // &nbsp;</td><td>Active (<a href=/faq.php style='color: blue;'>cancel</a>)</td></tr><tr><td>Last
            // payment:</td><td>2014-07-25</td></tr><tr><td>Next payment:</td><td>2014-08-24</td></tr>
            expire = br.getRegex("<td>Next payment:</td><td>(\\d{4}-\\d{2}-\\d{2})</td>").getMatch(0);
        }
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
            try {
                br.getPage("/remote_downloader.php");
                final String[] hosts = br.getRegex("src=/gf/ru/([A-Za-z0-9\\.]+)\\.png width=").getColumn(0);
                if (hosts == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
                /*
                 * set ArrayList<String> with all supported multiHosts of this service
                 */
                ai.setMultiHostSupport(supportedHosts);
                ai.setStatus("Premium Account");
            } catch (final Throwable e) {
                logger.info("Could not fetch ServerList from speedyshare.com: " + e.toString());
            }
        } else {
            ai.setStatus("Premium Account");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        br = new Browser();
        requestFileInformation(link);
        // there are redirects on the main url going to mainurl + "/filename.exe".login can ruin current br.getURL() if used further down
        final String currentBrURL = br.getURL();
        login(account, false);
        String finallink = null;
        if (link.getDownloadURL().matches(REMOTELINK)) {
            finallink = link.getDownloadURL();
        } else {
            br.setFollowRedirects(false);
            br.getPage(currentBrURL);
            finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = getFinalLink();
            }
            if (finallink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (finallink.matches("/" + fuid + "/[a-f0-9]+/download/.+")) {
                br.getPage(finallink);
                finallink = br.getRedirectLocation();
                doMagic();
                sleep(3000, link);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(finallink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void doMagic() {
        if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
            br.getCookies(MAINPAGE).remove("Max-Age");
            br.getCookies(MAINPAGE).remove("spl");
        } else {
            // stable is lame
            final HashMap<String, String> cookies = new HashMap<String, String>();
            for (final Cookie c : br.getCookies(MAINPAGE).getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            br.clearCookies(MAINPAGE);
            // load cookies we want..
            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                final String key = cookieEntry.getKey();
                final String value = cookieEntry.getValue();
                if (key != null && !key.matches("Max-Age|spl")) {
                    this.br.setCookie(MAINPAGE, key, value);
                }
            }
        }
    }

    private String getFinalLink() {
        String finallink = br.getRegex("class=downloadfilename href=('|\")(/(file/)?[^<>\"]*?)\\1").getMatch(1);
        if (finallink == null) {
            // premium
            finallink = br.getRegex("<a href=('|\")(/" + fuid + "/[a-f0-9]+/download/.*?)\\1><img alt=('|\")Fast Download\\3").getMatch(1);
        }
        return finallink;
    }

    private void setFUID(final DownloadLink downloadLink) {
        // not sure of remote link formats....
        if (downloadLink.getDownloadURL().matches(REMOTELINK)) {
            fuid = new Regex(downloadLink.getDownloadURL(), "speedyshare\\.com/remote/([A-Za-z0-9]+/d\\d+\\-[A-Za-z0-9]+)").getMatch(0);
            fuid = fuid.replace("/", "_");
        } else {
            fuid = new Regex(downloadLink.getDownloadURL(), "speedyshare\\.com/(files/)?([A-Z0-9]+)").getMatch(1);
        }
        if (fuid != null) {
            final String linkID = getHost() + "://" + fuid;
            try {
                downloadLink.setLinkID(linkID);
            } catch (final Throwable e) {
                downloadLink.setProperty("LINKDUPEID", linkID);
            }
        }
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        login(acc, false);
        String dllink = link.getStringProperty("speedysharedirectlink", null);
        if (dllink == null) {
            br.postPage("http://www.speedyshare.com/remote_downloader.php", "urls=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = br.getRegex("\"(http://(www\\.)?speedyshare\\.com/remote/[A-Za-z0-9\\-_/]+)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.getRedirectLocation() != null && !br.getRedirectLocation().contains("speedyshare.com/")) {
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("speedysharedirectlink", dllink);
        dl.startDownload();
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
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return true;
        }
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

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}