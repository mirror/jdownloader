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
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedyshare.com" }, urls = { "http://(www\\.)?(speedyshare\\.com|speedy\\.sh)/((files?|remote)/)?[A-Za-z0-9]+" }, flags = { 2 })
public class SpeedyShareCom extends PluginForHost {

    private static final String                            PREMIUMONLY        = "(>This paraticular file can only be downloaded after you purchase|this file can only be downloaded with SpeedyShare Premium)";
    private static final String                            PREMIUMONLYTEXT    = "Only downloadable for premium users";
    private static final String                            MAINPAGE           = "http://www.speedyshare.com";
    private static final String                            CAPTCHATEXT        = "/captcha\\.php\\?";
    private static Object                                  LOCK               = new Object();
    private final String                                   REMOTELINK         = "http://(www\\.)?speedyshare\\.com/remote/[A-Za-z0-9]+";
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
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser();
        if (downloadLink.getDownloadURL().matches(REMOTELINK)) {
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null) {
                downloadLink.getLinkStatus().setStatusText("This link can only be checked when a valid premium account is active");
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
                    downloadLink.getLinkStatus().setStatusText("This file can only be downloaded by premium users");
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
            if (br.containsHTML("(class=sizetagtext>not found<|File not found|It has been deleted<|>or it never existed at all)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = br.getRegex("\"(og:title|name)\" content=\"Download File: ([^\"]+)").getMatch(1);
            if (filename == null) filename = br.getRegex("<title>(.+) \\- Speedy Share \\- .+</title>").getMatch(0);
            String filesize = br.getRegex("<div class=sizetagtext>(.*?)</div>").getMatch(0);
            if (filesize == null) filesize = br.getRegex("([\\d\\.]+ (KB|MB|GB|TB))").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            downloadLink.setName(Encoding.htmlDecode(filename));
            if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
            if (br.containsHTML(PREMIUMONLY)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.speedysharecom.errors.only4premium", PREMIUMONLYTEXT));
            br.setFollowRedirects(false);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(REMOTELINK)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
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
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYTEXT);
        }
        if (!br.containsHTML(CAPTCHATEXT)) {
            finallink = br.getRegex("class=downloadfilename href=\\'(.*?)\\'").getMatch(0);
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (finallink == null) {
            final long timeBefore = System.currentTimeMillis();
            String postLink = br.getRegex("\"(/files?/[A-Za-z0-9]+/download/.*?)\"").getMatch(0);
            String captchaLink = br.getRegex("(/captcha\\.php\\?uid=file\\d+)").getMatch(0);
            if (postLink == null || captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(MAINPAGE + captchaLink, downloadLink);
            final int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
            final String waittime = br.getRegex("</div>\\';[\t\n\r ]+secondscounter\\((\\d+)\\);").getMatch(0);
            int wait = 15;
            if (waittime != null) wait = Integer.parseInt(waittime) / 10;
            wait -= passedTime;
            if (wait > 0) sleep(wait * 1001l, downloadLink);
            br.postPage(MAINPAGE + postLink, "captcha=" + Encoding.urlEncode(code));
            finallink = br.getRedirectLocation();
        }
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
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
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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
                br.setFollowRedirects(false);
                br.postPage("https://www.speedyshare.com/login.php", "redir=%2Fupload_page.php&remember=on&login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                br.getPage("http://www.speedyshare.com/user.php");
                if (br.getCookie(MAINPAGE, "spl") == null || !br.containsHTML("<td>Account type:</td><td><b style=\\'color: green\\'>Premium</b>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        final boolean multihostsupport = multiHostSupported();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            if (multihostsupport) {
                ai.setProperty("multiHostSupport", Property.NULL);
            }
            return ai;
        }
        final String expire = br.getRegex("<td>Next payment:</td><td>([^<>\"]*?)</td>").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        if (multihostsupport) {
            try {
                br.getPage("http://www.speedyshare.com/remote_downloader.php");
                final String[] hosts = br.getRegex("src=/gf/ru/([A-Za-z0-9\\.]+)\\.png width=").getColumn(0);
                if (hosts == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
                /*
                 * set ArrayList<String> with all supported multiHosts of this service
                 */
                ai.setProperty("multiHostSupport", supportedHosts);
                ai.setStatus("Premium account valid: " + supportedHosts.size() + " Hosts via speedyshare.com available");
            } catch (final Throwable e) {
                account.setProperty("multiHostSupport", Property.NULL);
                logger.info("Could not fetch ServerList from speedyshare.com: " + e.toString());
            }
        } else {
            ai.setStatus("Premium user");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        String finallink = null;
        if (link.getDownloadURL().matches(REMOTELINK)) {
            finallink = link.getDownloadURL();
        } else {
            br.setFollowRedirects(false);
            br.getPage(link.getDownloadURL());
            finallink = br.getRedirectLocation();
            if (finallink == null) finallink = br.getRegex("class=downloadfilename href=\\'((file/)?[^<>\"]*?)\\'").getMatch(0);
            if (finallink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!finallink.startsWith("http")) finallink = "http://www.speedyshare.com" + finallink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(finallink), true, 0);
        if (dl.getConnection().getContentType().contains("html") && !link.getDownloadURL().matches(REMOTELINK)) {
            br.followConnection();
            finallink = br.getRegex("class=downloadfilename href=\\'(/(file/)?[^<>\"]*?)\\'").getMatch(0);
            if (finallink == null) {
                logger.warning("The final dllink seems not to be a file!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (dl.getConnection().getContentType().contains("html") && link.getDownloadURL().matches(REMOTELINK)) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!finallink.startsWith("http")) finallink = "http://www.speedyshare.com" + finallink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(finallink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

    private boolean multiHostSupported() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 16116) return false;
        return true;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
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