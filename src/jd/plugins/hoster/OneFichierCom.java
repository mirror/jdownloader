//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1fichier.com" }, urls = { "https?://(?!www\\.)[a-z0-9\\-]+\\.(dl4free\\.com|alterupload\\.com|cjoint\\.net|desfichiers\\.com|dfichiers\\.com|megadl\\.fr|mesfichiers\\.org|piecejointe\\.net|pjointe\\.com|tenvoi\\.com|1fichier\\.com)/?" }, flags = { 2 })
public class OneFichierCom extends PluginForHost {

    private static AtomicInteger maxPrem          = new AtomicInteger(1);
    private static final String  PASSWORDTEXT     = "(Accessing this file is protected by password|Please put it on the box bellow|Veuillez le saisir dans la case ci-dessous)";
    private static final String  IPBLOCKEDTEXTS   = "(/>Téléchargements en cours|>veuillez patienter avant de télécharger un autre fichier|>You already downloading (some|a) file|>You can download only one file at a time|>Please wait a few seconds before downloading new ones|>You must wait for another download|Without premium status, you can download only one file at a time)";
    private static final String  FREELINK         = "freeLink";
    private static final String  PREMLINK         = "premLink";
    private static final String  SSL_CONNECTION   = "SSL_CONNECTION";
    private static final String  PREFER_RECONNECT = "PREFER_RECONNECT";
    private boolean              pwProtected      = false;

    public OneFichierCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.1fichier.com/en/register.pl");
        setConfigElements();
    }

    public void correctDownloadLink(final DownloadLink link) {
        // Remove everything after the domain
        if (!link.getDownloadURL().endsWith("/")) {
            Regex idhostandName = new Regex(link.getDownloadURL(), "https?://(.*?)\\.(.*?)(/|$)");
            link.setUrlDownload("http://" + idhostandName.getMatch(0) + "." + idhostandName.getMatch(1));
        } else {
            String addedLink = link.getDownloadURL().replace("https://", "http://");
            if (addedLink.endsWith("/")) addedLink = addedLink.substring(0, addedLink.length() - 1);
            link.setUrlDownload(addedLink);
        }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.getHeaders().put("User-Agent", "");
            br.getHeaders().put("Accept", "");
            br.getHeaders().put("Accept-Language", "");
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink dl : links) {
                    correctDownloadLink(dl);
                    sb.append("links[]=");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    sb.append("&");
                }
                br.postPageRaw("http://1fichier.com/check_links.pl", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String addedLink = dllink.getDownloadURL();
                    if (br.containsHTML(addedLink + ";;;NOT FOUND")) {
                        dllink.setAvailable(false);
                    } else {
                        final String[][] linkInfo = br.getRegex(dllink.getDownloadURL() + ";([^;]+);(\\d+)").getMatches();
                        if (linkInfo.length != 1) {
                            logger.warning("Linkchecker for 1fichier.com is broken!");
                            return false;
                        }
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(Encoding.htmlDecode(linkInfo[0][0]));
                        dllink.setDownloadSize(SizeFormatter.getSize(linkInfo[0][1]));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        pwProtected = false;
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.postPage("http://1fichier.com/check_links.pl", "links[]=" + Encoding.urlEncode(link.getDownloadURL()));
        if (br.containsHTML(";;;NOT FOUND")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">Software error:<")) {
            link.getLinkStatus().setStatusText("Cannot check availibility because of a server error!");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.toString().equals("wait")) {
            br.getPage(link.getDownloadURL());
            final String siteFilename = br.getRegex(">Nom du fichier :</th><td>([^<>\"]*?)</td>").getMatch(0);
            String siteFilesize = br.getRegex("<th>Taille :</th><td>([^<>\"]*?)</td>").getMatch(0);
            if (siteFilename == null || siteFilesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setName(Encoding.htmlDecode(siteFilename));
            siteFilesize = siteFilesize.replace("Mo", "MB");
            siteFilesize = siteFilesize.replace("Go", "GB");
            link.setDownloadSize(SizeFormatter.getSize(siteFilesize));
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("password")) {
            pwProtected = true;
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.onefichiercom.passwordprotected", "This link is password protected"));
            return AvailableStatus.UNCHECKABLE;
        }
        final String[][] linkInfo = br.getRegex("http://[^;]+;([^;]+);(\\d+)").getMatches();
        if (linkInfo == null || linkInfo.length == 0) {
            logger.warning("Available Status broken for link: " + link.getDownloadURL());
            return null;
        }
        String filename = linkInfo[0][0];
        if (filename == null) filename = br.getRegex(">File name :</th><td>([^<>\"]*?)</td>").getMatch(0);
        String filesize = linkInfo[0][1];
        if (filesize == null) filesize = br.getRegex(">File size :</th><td>([^<>\"]*?)</td></tr>").getMatch(0);
        if (filename != null) link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            long size = 0;
            link.setDownloadSize(size = SizeFormatter.getSize(filesize));
            if (size > 0) link.setProperty("VERIFIEDFILESIZE", size);
        }
        // Not available in new API
        // if ("1".equalsIgnoreCase(linkInfo[0][2])) {
        // link.setProperty("HOTLINK", true);
        // } else {
        // link.setProperty("HOTLINK", Property.NULL);
        // }

        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        if (account == null && link.getProperty("HOTLINK", null) != null) { return Integer.MAX_VALUE; }
        return super.getMaxSimultanDownload(link, account);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String dllink = downloadLink.getStringProperty(FREELINK, null);
        if (dllink != null) {
            /* try to resume existing file */
            br.setFollowRedirects(true);
            // at times the second chunk creates 404 errors!
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                /* could not resume, fetch new link */
                br.followConnection();
                downloadLink.setProperty(FREELINK, Property.NULL);
                dllink = null;
                br.setFollowRedirects(false);
                br.setCustomCharset("utf-8");
            } else {
                /* resume download */
                dl.startDownload();
                downloadLink.setProperty(FREELINK, Property.NULL);
                return;
            }
        }
        // use the English page, less support required
        boolean retried = false;
        String passCode = null;
        while (true) {
            br.getPage(downloadLink.getDownloadURL() + "/en/index.html");
            if (br.containsHTML(">Software error:<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            if (br.containsHTML(IPBLOCKEDTEXTS)) {
                final boolean preferReconnect = this.getPluginConfig().getBooleanProperty("PREFER_RECONNECT", false);
                // Warning ! Without premium status, you can download only one file at a time and you must wait up to 5 minutes between each
                // downloads.
                final String waittime = br.getRegex("you can download only one file at a time and you must wait (at least|up to) (\\d+) minutes between each downloads").getMatch(1);
                if (waittime != null && preferReconnect) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
                } else if (preferReconnect) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                } else if (waittime != null) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Short wait period, Reconnection not necessary", Integer.parseInt(waittime) * 60 * 1001l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Short wait period, Reconnection not necessary", 5 * 60 * 1001);
                }
            }
            if (br.containsHTML(PASSWORDTEXT) || pwProtected) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                br.postPage(br.getURL(), "pass=" + passCode);
                if (br.containsHTML(PASSWORDTEXT)) {
                    downloadLink.setProperty("pass", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.onefichiercom.wrongpassword", "Password wrong!"));
                } else {
                    if (passCode != null) downloadLink.setProperty("pass", passCode);
                }
            } else {
                // ddlink is within the ?e=1 page request! but it seems you need to do the following posts to be able to use the link
                // dllink = br.getRegex("(http.+/get/" + new Regex(downloadLink.getDownloadURL(), "https?://([^\\.]+)").getMatch(0) +
                // "[^;]+)").getMatch(0);
                br.postPage(downloadLink.getDownloadURL() + "/en/", "a=1&submit=Download+the+file");
            }
            if (dllink == null) dllink = br.getRedirectLocation();
            if (dllink == null) {
                String wait = br.getRegex(" var count = (\\d+);").getMatch(0);
                if (wait != null && retried == false) {
                    retried = true;
                    sleep(1000 * Long.parseLong(wait), downloadLink);
                    continue;
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            break;
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        downloadLink.setProperty(FREELINK, dllink);
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        br.getPage("https://1fichier.com/console/account.pl?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(JDHash.getMD5(account.getPass())));
        String timeStamp = br.getRegex("(\\d+)").getMatch(0);
        String freeCredits = br.getRegex("0[\r\n]+([0-9\\.]+)").getMatch(0);
        // Use site login/site download if either API is not working or API says that there are no credits available
        if ("error".equalsIgnoreCase(timeStamp) || ("0".equals(timeStamp) && freeCredits == null)) {
            /**
             * Only used if the API fails and is wrong but that usually doesn't happen!
             */
            try {
                login(account, true);
            } catch (Exception e) {
                ai.setStatus("Username/Password also invalid via site login!");
                account.setProperty("type", Property.NULL);
                account.setValid(false);
                return ai;
            }
            ai.setStatus("Free User (Credits available)");
            account.setValid(true);
            account.setProperty("type", "FREE");

            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("https://www.1fichier.com/en/console/details.pl");
            String freeCredits2 = br.getRegex(">Your account have ([^<>\"]*?) of direct download credits").getMatch(0);
            if (freeCredits2 != null)
                ai.setTrafficLeft(SizeFormatter.getSize(freeCredits2));
            else
                ai.setUnlimitedTraffic();
            maxPrem.set(1);
            try {
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
            account.setProperty("freeAPIdisabled", true);
            return ai;
        } else if ("0".equalsIgnoreCase(timeStamp)) {
            if (freeCredits != null && Float.parseFloat(freeCredits) > 0) {
                /* not finished yet */
                account.setValid(true);
                account.setProperty("type", "FREE");
                ai.setStatus("Free User (Credits available)");
                ai.setTrafficLeft(SizeFormatter.getSize(freeCredits + " GB"));
                try {
                    maxPrem.set(1);
                    account.setMaxSimultanDownloads(1);
                    account.setConcurrentUsePossible(false);
                } catch (final Throwable e) {
                }
                account.setProperty("freeAPIdisabled", false);
            } else {
                ai.setStatus("Free User (No credits left)");
                account.setProperty("type", Property.NULL);
                account.setValid(false);
                return ai;
            }
            return ai;
        } else {
            account.setValid(true);
            account.setProperty("type", "PREMIUM");
            ai.setStatus("Premium User");
            ai.setValidUntil(Long.parseLong(timeStamp) * 1000l + (24 * 60 * 60 * 1000l));
            ai.setUnlimitedTraffic();
            try {
                maxPrem.set(20);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            return ai;
        }
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                prepareBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                logger.info("Using site login because API is either wrong or no free credits...");
                br.postPage("https://www.1fichier.com/en/login.pl", "lt=on&Login=Login&secure=on&mail=" + Encoding.urlEncode(account.getUser()) + "&pass=" + account.getPass());
                final String logincheck = br.getCookie("http://1fichier.com/", "SID");
                if (logincheck == null || logincheck.equals("")) {
                    logger.info("Username/Password also invalid via site login!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
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

    private static final Object LOCK = new Object();

    @Override
    public String getAGBLink() {
        return "http://www.1fichier.com/en/cgu.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    private String handlePassword(final DownloadLink downloadLink, String passCode) throws IOException, PluginException {
        logger.info("This link seems to be password protected, continuing...");
        if (downloadLink.getStringProperty("pass", null) == null) {
            passCode = Plugin.getUserInput("Password?", downloadLink);
        } else {
            /* gespeicherten PassCode holen */
            passCode = downloadLink.getStringProperty("pass", null);
        }
        br.postPage(br.getURL(), "pass=" + passCode);
        if (br.containsHTML(PASSWORDTEXT)) {
            downloadLink.setProperty("pass", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.onefichiercom.wrongpassword", "Password wrong!"));
        }
        return passCode;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        String passCode = null;
        String dllink = link.getStringProperty(PREMLINK, null);
        boolean useSSL = getPluginConfig().getBooleanProperty(SSL_CONNECTION, true);
        if (oldStyle() == true) useSSL = false;
        if (dllink != null) {
            /* try to resume existing file */
            if (useSSL) dllink = dllink.replaceFirst("http://", "https://");
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                /* could not resume, fetch new link */
                br.followConnection();
                link.setProperty(PREMLINK, Property.NULL);
                dllink = null;
            } else {
                /* resume download */
                dl.startDownload();
                return;
            }
        }
        if ("FREE".equals(account.getStringProperty("type")) && account.getBooleanProperty("freeAPIdisabled")) {
            /**
             * Only used if the API fails and is wrong but that usually doesn't happen!
             */
            login(account, false);
            doFree(link);
        } else {
            br.setFollowRedirects(false);
            sleep(2 * 1000l, link);
            String url = link.getDownloadURL().replace("en/index.html", "");
            if (!url.endsWith("/")) url = url + "/";
            url = url + "?u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(JDHash.getMD5(account.getPass()));

            URLConnectionAdapter con = br.openGetConnection(url);
            if (con.getResponseCode() == 401) {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            br.followConnection();
            if (pwProtected || br.containsHTML("password")) passCode = handlePassword(link, passCode);
            dllink = br.getRedirectLocation();
            if (dllink != null && br.containsHTML(IPBLOCKEDTEXTS)) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 45 * 1000l);
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String useDllink = dllink;
            if (useSSL) useDllink = useDllink.replaceFirst("http://", "https://");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, useDllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) {
                link.setProperty("pass", passCode);
            }
            link.setProperty(PREMLINK, dllink);
            dl.startDownload();
        }
    }

    private boolean oldStyle() {
        String style = System.getProperty("ftpStyle", null);
        if ("new".equalsIgnoreCase(style)) return false;
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 10000) return true;
        return false;
    }

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, JDL.L("plugins.hoster.onefichiercom.com.ssl2", "Use Secure Communication over SSL")).setDefaultValue(true));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), PREFER_RECONNECT, JDL.L("plugins.hoster.onefichiercom.com.preferreconnect", "Reconnect, even if the wait time is only short (1-6 minutes)")).setDefaultValue(false));
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) { return; }
            br.setConnectTimeout(3 * 60 * 1000);
            br.setReadTimeout(3 * 60 * 1000);
            br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.116 Safari/537.36");
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
        } catch (Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}