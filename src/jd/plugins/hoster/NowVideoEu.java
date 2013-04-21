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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nowvideo.eu", "nowvideo.co", "nowvideo.ch" }, urls = { "http://(www\\.)?(nowvideo\\.eu/(?!share\\.php)(video/|player\\.php\\?v=)|embed\\.nowvideo\\.eu/embed\\.php\\?v=)[a-z0-9]+", "http://(www\\.)?(nowvideo\\.co/(?!share\\.php)(video/|player\\.php\\?v=)|embed\\.nowvideo\\.co/embed\\.php\\?v=)[a-z0-9]+", "http://(www\\.)?(nowvideo\\.ch/(?!share\\.php)(video/|player\\.php\\?v=)|embed\\.nowvideo\\.ch/embed\\.php\\?v=)[a-z0-9]+" }, flags = { 2, 0, 0 })
public class NowVideoEu extends PluginForHost {

    public NowVideoEu(PluginWrapper wrapper) {
        super(wrapper);

        DOMAIN = validateHost();

        if (DOMAIN == null) {
            AVAILABLE_PRECHECK = false;
            DOMAIN = "eu";
        }

        MAINPAGE = "nowvideo." + DOMAIN;

        this.enablePremium("http://www." + MAINPAGE + "/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www." + MAINPAGE + "/terms.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www." + MAINPAGE + "/player.php?v=" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
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
                br.getPage("http://nowvideo." + domain);
                br = null;
                return domain;
            } catch (Exception e) {
                logger.warning("NowVideo." + domain + " seems to be offline...");
            }
        }
        return null;
    }

    private static Object       LOCK               = new Object();
    private String              MAINPAGE           = "nowvideo.eu";
    private String              DOMAIN             = "eu";
    private static final String ISBEINGCONVERTED   = ">The file is being converted.";
    private Boolean             AVAILABLE_PRECHECK = true;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {

        if (!AVAILABLE_PRECHECK) {
            DOMAIN = validateHost();

            if (DOMAIN == null) {
                link.getLinkStatus().setStatusText("All servers seems to be offline...");
                throw new PluginException(LinkStatus.ERROR_NO_CONNECTION);
            }

            MAINPAGE = "nowvideo." + DOMAIN;

            this.enablePremium("http://www." + MAINPAGE + "/premium.php");
        }

        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>This file no longer exists on our servers|>Possible reasons:)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(ISBEINGCONVERTED)) {
            link.getLinkStatus().setStatusText("This file is being converted!");
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + ".flv");
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("<div class=\"video_details radius\\d+\" style=\"height:125px;position:relative;\">[\t\n\r ]+<h4>([^<>\"]*?)</h4>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(ISBEINGCONVERTED)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is being converted!", 2 * 60 * 60 * 1000l);
        final String fKey = br.getRegex("flashvars\\.filekey=\"([^<>\"]*?)\"").getMatch(0);
        if (fKey == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www." + MAINPAGE + "/api/player.api.php?pass=undefined&user=undefined&codes=undefined&file=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + "&key=" + Encoding.urlEncode(fKey));
        String dllink = br.getRegex("url=(http://[^<>\"]*?\\.flv)\\&title").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
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
                            this.br.setCookie("http://" + MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("http://www." + MAINPAGE + "/login.php?return=", "register=Login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.getURL().contains("login.php?e=1") || !br.getURL().contains("panel.php?login=1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://" + MAINPAGE);
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
        final String dllink = br.getRegex("\"(http://[a-z0-9]+\\.nowvideo\\.co/dl/[^<>\"]*?)\"").getMatch(0);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}