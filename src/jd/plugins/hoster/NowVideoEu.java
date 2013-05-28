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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nowvideo.eu", "nowvideo.co" }, urls = { "http://(www\\.)?(nowvideo\\.(eu|co|ch)/(?!share\\.php)(video/|player\\.php\\?v=)|embed\\.nowvideo\\.(eu|co|ch)/embed\\.php\\?v=)[a-z0-9]+", "NEVERUSETHISSUPERDUBERREGEXATALL2013" }, flags = { 2, 0 })
public class NowVideoEu extends PluginForHost {

    public NowVideoEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE.string + "/premium.php");
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

    public boolean rewriteHost(DownloadLink link) {
        if ("nowvideo.ch".equals(link.getHost())) {
            link.setHost("nowvideo.co");
            return true;
        }
        return false;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(MAINPAGE.string + "/player.php?v=" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
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

    private void correctCurrentDomain() {
        if (AVAILABLE_PRECHECK.get() == false) {
            synchronized (LOCK) {
                if (AVAILABLE_PRECHECK.get() == false) {
                    /*
                     * For example .eu domain are blocked from some Italian ISP, and .co from others, so need to test all domains before proceeding.
                     */

                    String CCtld = validateHost();
                    if (CCtld != null) {
                        ccTLD.string = CCtld;
                    }
                    MAINPAGE.string = "http://www.nowvideo." + CCtld;
                    this.enablePremium(MAINPAGE.toString() + "/premium.php");
                    AVAILABLE_PRECHECK.set(true);
                }
            }
        }
    }

    private String validateHost() {
        final String[] ccTLDs = { "eu", "co", "ch" };

        for (int i = 0; i < ccTLDs.length; i++) {
            String CCtld = ccTLDs[i];
            try {
                Browser br = new Browser();
                workAroundTimeOut(br);
                br.setCookiesExclusive(true);
                br.getPage("http://www.nowvideo." + CCtld);
                br = null;
                return CCtld;
            } catch (Exception e) {
                logger.warning("nowvideo." + CCtld + " seems to be offline...");
            }
        }
        return null;
    }

    private static Object          LOCK               = new Object();
    private static StringContainer MAINPAGE           = new StringContainer("http://www.nowvideo.eu");
    private static StringContainer ccTLD              = new StringContainer("eu");
    private static final String    ISBEINGCONVERTED   = ">The file is being converted.";
    private static AtomicBoolean   AVAILABLE_PRECHECK = new AtomicBoolean(false);

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        correctCurrentDomain();
        correctDownloadLink(link);
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
        br.getPage(MAINPAGE.string + "/api/player.api.php?pass=undefined&user=undefined&codes=undefined&file=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + "&key=" + Encoding.urlEncode(fKey));
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
     * Dev note: Never buy premium from them, as freeuser you have no limits, as premium neither and you can't even download the original videos as
     * premiumuser->Senseless!!
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
                br.postPage(MAINPAGE.string + "/login.php?return=", "register=Login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.getURL().contains("login.php?e=1") || !br.getURL().contains("panel.php?login=1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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