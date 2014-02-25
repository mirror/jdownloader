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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nowdownload.eu", "likeupload.net" }, urls = { "http://(www\\.)?nowdownload\\.(eu|co|ch|sx|ag)/(dl(\\d+)?/|down(load)?\\.php\\?id=)[a-z0-9]+", "https?://(www\\.)?likeupload\\.(net|org)/[a-z0-9]{12}" }, flags = { 2, 2 })
public class NowDownloadEu extends PluginForHost {

    public NowDownloadEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE.string + "/premium.php");
    }

    public Boolean rewriteHost(DownloadLink link) {
        if (link != null && ("nowdownload.co".equals(link.getHost()) || "nowdownload.ch".equals(link.getHost()) || "nowdownload.sx".equals(link.getHost()) || "nowdownload.ag".equals(link.getHost()))) {
            link.setHost("nowdownload.eu");
            return true;
        }
        return false;
    }

    public Boolean rewriteHost(Account acc) {
        if (acc != null && ("nowdownload.co".equals(acc.getHoster()) || "nowdownload.ch".equals(acc.getHoster()) || "nowdownload.sx".equals(acc.getHoster()) || "nowdownload.ag".equals(acc.getHoster()))) {
            acc.setHoster("nowdownload.eu");
            return true;
        }
        return false;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE.string + "/terms.php";
    }

    public static class StringContainer {
        public String string = null;

        public StringContainer(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private static StringContainer MAINPAGE                = new StringContainer("http://www.nowdownload.sx");
    private static StringContainer DOMAIN                  = new StringContainer("sx");
    private static AtomicBoolean   AVAILABLE_PRECHECK      = new AtomicBoolean(false);
    private static StringContainer ua                      = new StringContainer(RandomUserAgent.generate());
    private static Object          LOCK                    = new Object();
    private final String           TEMPUNAVAILABLE         = ">The file is being transfered\\. Please wait";
    private final String           TEMPUNAVAILABLEUSERTEXT = "Host says: 'The file is being transfered. Please wait!'";
    private final String           domains                 = "nowdownload\\.(eu|co|ch|sx|ag)";

    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().contains("likeupload.")) {
            // all likeupload uid to nowdownload uid contain 0 prefix
            link.setUrlDownload("http://www.nowdownload." + DOMAIN + "/dl/0" + new Regex(link.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0));
        } else {
            link.setUrlDownload("http://www.nowdownload." + DOMAIN + "/dl/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
        }
    }

    private void correctCurrentDomain() {
        if (AVAILABLE_PRECHECK.get() == false) {
            synchronized (LOCK) {
                if (AVAILABLE_PRECHECK.get() == false) {
                    /*
                     * == Fix original link ==
                     * 
                     * For example .eu domain is blocked from some italian ISP, and .co from others, so we have to test all domains before
                     * proceed, to select one available.
                     */

                    String newDomain = validateHost();
                    if (newDomain != null) {
                        DOMAIN.string = newDomain;
                    }
                    MAINPAGE.string = "http://www.nowdownload." + newDomain;
                    this.enablePremium(MAINPAGE.toString() + "/premium.php");
                    AVAILABLE_PRECHECK.set(true);
                }
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        correctCurrentDomain();
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.getHeaders().put("User-Agent", ua.string);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">This file does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(TEMPUNAVAILABLE)) {
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText(TEMPUNAVAILABLEUSERTEXT);
            return AvailableStatus.TRUE;
        }

        final Regex fileInfo = br.getRegex(">Downloading</span> <br> (.*?) ([\\d+\\.]+ (B|KB|MB|GB|TB))");
        String filename = br.getRegex(domains + "/nowdownload/[a-z0-9]+/[a-z0-9]+/[^<>]+/([^<>\"]*?)\"").getMatch(1);
        if (filename == null) filename = fileInfo.getMatch(0).replace("<br>", "");
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(TEMPUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, TEMPUNAVAILABLEUSERTEXT, 60 * 60 * 1000l);
        String dllink = (checkDirectLink(downloadLink, "directlink"));
        if (dllink == null) dllink = getDllink();
        // This handling maybe isn't needed anymore
        if (dllink == null && br.containsHTML("w,i,s,e")) {
            String result = unWise();
            br.getRequest().setHtmlCode(result + "\r\n" + br.getRequest().getHtmlCode());
        }
        if (dllink == null) {
            final String tokenPage = br.getRegex("\"(/api/token\\.php\\?token=[a-z0-9]+)\"").getMatch(0);
            String continuePage = br.getRegex("\"(/dl2/[a-z0-9]+/[a-z0-9]+)\"").getMatch(0);
            if (continuePage == null) {
                continuePage = br.getRegex("href=\"([^<>]+)\">Download your file").getMatch(0);
            }
            if (tokenPage == null || continuePage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            int wait = 30;
            final String waittime = br.getRegex("\\.countdown\\(\\{until: \\+(\\d+),").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getPage(MAINPAGE.string + tokenPage);
            sleep(wait * 1001l, downloadLink);
            br.getPage(MAINPAGE.string + continuePage);
            if (br.containsHTML(">You need Premium Membership to download this file")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
            }
            if (br.containsHTML(">You have reached your daily download limit")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached your daily download limit", 60 * 60 * 1000l); }
            dllink = getDllink();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String filename = new Regex(dllink, ".+/[^_]+_(.+)").getMatch(0);
            if (filename != null) downloadLink.setFinalFileName(Encoding.urlDecode(filename, false));
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) return null;
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("ECMAScript");
        try {
            engine.eval("var res = " + fn);
            result = (String) engine.get("res");
            result = new Regex(result, "eval\\((.*?)\\);$").getMatch(0);
            engine.eval("res = " + result);
            result = (String) engine.get("res");
            String res[] = result.split(";\\s;");
            engine.eval("res = " + new Regex(res[res.length - 1], "eval\\((.*?)\\);$").getMatch(0));
            result = (String) engine.get("res");
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
        return result;
    }

    private static void workAroundTimeOut(final Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(45000);
                br.setReadTimeout(45000);
            }
        } catch (final Throwable e) {
        }
    }

    private String validateHost() {
        final String[] ccTLDs = { "sx", "eu", "co", "ch", "ag" };

        for (int i = 0; i < ccTLDs.length; i++) {
            String domain = ccTLDs[i];
            try {
                Browser br = new Browser();
                workAroundTimeOut(br);
                br.setCookiesExclusive(true);
                br.getPage("http://www.nowdownload." + domain);
                String redirect = br.getRedirectLocation();
                br = null;
                if (redirect != null)
                    return new Regex(redirect, domains).getMatch(0);
                else
                    return domain;
            } catch (Exception e) {
                logger.warning("NowDownload." + domain + " seems to be offline...");
            }
        }
        return null;
    }

    private String getDllink() {
        String dllink = br.getRegex("\"(http://(?!www\\.)[^/]+/(nowdownload|dl)/[^<>\"]*?)\"").getMatch(0);
        return dllink;
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }

        }
        return dllink;
    }

    /**
     * Dev note: Never buy premium from them, as freeuser you have no limits, as premium neither and you can't even download the original
     * videos as premiumuser->Senseless!!
     */
    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                correctCurrentDomain();
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
                            this.br.setCookie(MAINPAGE.string, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage(MAINPAGE.string + "/login.php", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.getURL().contains("login.php?e=1") || !br.getURL().contains("panel.php?logged=1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE.string);
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
        if (!account.getUser().matches(".+@.+")) {
            ai.setStatus("Please enter your E-Mail adress as username!");
            account.setValid(false);
            return ai;
        }
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("\"(http://[a-z0-9]+\\." + domains + "/dl/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = getDllink();
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}