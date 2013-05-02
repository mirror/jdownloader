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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nowdownload.eu", "nowdownload.co", "nowdownload.ch" }, urls = { "http://(www\\.)?nowdownload\\.eu/dl(\\d+)?/[a-z0-9]+", "http://(www\\.)?nowdownload\\.co/dl(\\d+)?/[a-z0-9]+", "http://(www\\.)?nowdownload\\.ch/dl(\\d+)?/[a-z0-9]+" }, flags = { 2, 0, 0 })
public class NowDownloadEu extends PluginForHost {

    public NowDownloadEu(PluginWrapper wrapper) {
        super(wrapper);

        /*
         * == Fix original link ==
         * 
         * For example .eu domain is blocked from some italian ISP, and .co from others, so we have to test all domains before proceed, to
         * select one available.
         */

        DOMAIN = validateHost();

        if (DOMAIN == null) {
            AVAILABLE_PRECHECK = false;
            DOMAIN = "eu";
        }

        MAINPAGE = "http://www.nowdownload." + DOMAIN;

        this.enablePremium(MAINPAGE + "/premium.php");
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/terms.php";
    }

    private String              MAINPAGE                = "http://www.nowdownload.eu";
    private String              DOMAIN                  = "eu";
    private Boolean             AVAILABLE_PRECHECK      = true;
    private static final String ua                      = RandomUserAgent.generate();
    private static Object       LOCK                    = new Object();
    private static final String TEMPUNAVAILABLE         = ">The file is being transfered\\. Please wait";
    private static final String TEMPUNAVAILABLEUSERTEXT = "Host says: 'The file is being transfered. Please wait!'";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.nowdownload." + DOMAIN + "/dl/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!AVAILABLE_PRECHECK) {
            DOMAIN = validateHost();
            if (DOMAIN == null) {
                link.getLinkStatus().setStatusText("All servers seems to be offline...");
                return AvailableStatus.FALSE;
            }
            AVAILABLE_PRECHECK = true;
            MAINPAGE = "http://www.nowdownload." + DOMAIN;
            this.enablePremium(MAINPAGE + "/premium.php");
        }
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.getHeaders().put("User-Agent", ua);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">This file does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(TEMPUNAVAILABLE)) {
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText(TEMPUNAVAILABLEUSERTEXT);
            return AvailableStatus.TRUE;
        }

        final Regex fileInfo = br.getRegex(">Downloading</span> <br> (.*?) ([\\d+\\.]+ (B|KB|MB|GB|TB))");
        String filename = fileInfo.getMatch(0).replace("<br>", "");
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(TEMPUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, TEMPUNAVAILABLEUSERTEXT, 60 * 60 * 1000l);
        String dllink = (checkDirectLink(downloadLink, "directlink"));
        if (dllink == null) dllink = getDllink();
        // This handling maybe isn't needed anymore
        if (dllink == null) {
            final String tokenPage = br.getRegex("\"(/api/token\\.php\\?token=[a-z0-9]+)\"").getMatch(0);
            final String continuePage = br.getRegex("\"(/dl2/[a-z0-9]+/[a-z0-9]+)\"").getMatch(0);
            if (tokenPage == null || continuePage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            int wait = 30;
            final String waittime = br.getRegex("\\.countdown\\(\\{until: \\+(\\d+),").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getPage(MAINPAGE + tokenPage);
            sleep(wait * 1001l, downloadLink);
            br.getPage(MAINPAGE + continuePage);
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
        dl.startDownload();
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
        final String[] domains = { "co", "ch", "eu" };

        for (int i = 0; i < domains.length; i++) {
            String domain = domains[i];
            try {
                Browser br = new Browser();
                workAroundTimeOut(br);
                br.setCookiesExclusive(true);
                br.getPage("http://www.nowdownload." + domain);
                br = null;
                return domain;
            } catch (Exception e) {
                logger.warning("NowDownload." + domain + " seems to be offline...");
            }
        }
        return null;
    }

    private String getDllink() {
        final String dllink = br.getRegex("\"(http://[a-z0-9]+\\.nowdownload\\.(eu|co|ch)/dl/[^<>\"]*?)\"").getMatch(0);
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
                br.setFollowRedirects(true);
                br.postPage(MAINPAGE + "/login.php", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.getURL().contains("login.php?e=1") || !br.getURL().contains("panel.php?logged=1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        final String dllink = br.getRegex("\"(http://[a-z0-9]+\\.nowdownload\\.(eu|co|ch)/dl/[^<>\"]*?)\"").getMatch(0);
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