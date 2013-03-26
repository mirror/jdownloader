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
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "online.nolife-tv.com" }, urls = { "http://(www\\.)?online\\.nolife\\-tvdecrypted\\.com/index\\.php\\?id=\\d+" }, flags = { 2 })
public class OnlineNoLifeTvCom extends PluginForHost {

    private String              DLLINK              = null;
    private static final String ONLYPREMIUMUSERTEXT = "Only downloadable for premium members";
    private boolean             notDownloadable     = false;
    private static Object       LOCK                = new Object();
    private static final String MAINPAGE            = "http://online.nolife-tv.com/";
    private static final String SALT                = "YTUzYmUxODUzNzcwZjBlYmUwMzExZDY5OTNjN2JjYmU=";

    public OnlineNoLifeTvCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.nolife-tv.com/souscription");
    }

    public void correctDownloadLink(final DownloadLink link) {
        /** Videos come from a decrypter */
        link.setUrlDownload(link.getDownloadURL().replace("online.nolife-tvdecrypted.com/", "online.nolife-tv.com/"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage(MAINPAGE);
        if (!br.containsHTML("Type d'abonnement : <strong>Archives<")) {
            ai.setStatus("No premium account?!");
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex(">Valable jusqu'au : <strong>(\\d{2}/\\d{2}/\\d{4})</strong>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (notDownloadable) { throw new PluginException(LinkStatus.ERROR_FATAL, ONLYPREMIUMUSERTEXT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(DLLINK), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                final String pwhash = JDHash.getMD5(account.getPass());
                br.postPage("http://forum.nolife-tv.com/login.php?do=login", "vb_login_username=" + Encoding.urlEncode(account.getUser()) + "&cookieuser=1&vb_login_password=&s=&securitytoken=guest&do=login&vb_login_md5password=" + pwhash + "&vb_login_md5password_utf=" + pwhash);
                if (br.getCookie(MAINPAGE, "bbuserid") == null || br.getCookie(MAINPAGE, "bbpassword") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        /* Richtige Dateigröße bezogen auf den Accounttyp erhalten */
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null && br.getCookie(MAINPAGE, "bbpassword") == null) {
            login(aa, false);
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://online.nolife-tv.com/index.php") || br.containsHTML("<title>Nolife Online</title>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<div id=\"ligne_titre_big\" style=\"margin\\-top:10px;\">(.*?)</div><div").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Nolife Online \\- (.*?)( \\- [A-Za-z]+ \\d+)?</title>").getMatch(0);
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        filename = Encoding.htmlDecode(filename);
        if (!br.containsHTML("flashvars")) {
            notDownloadable = true;
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.onlinenolifetvcom.only4premium", ONLYPREMIUMUSERTEXT));
            downloadLink.setName(filename + ".mp4");
            return AvailableStatus.TRUE;
        } else {
            /*
             * Hier wird über die Kekse(Premium/Free) bestimmt welche Videoqualität man bekommt. Spart oben in den dlmethoden einige Zeilen
             * an Code. Die Methode "setBrowserExclusive()" muss dabei deaktiviert sein.
             */

            long ts = System.currentTimeMillis();
            br.postPage("/_nlfplayer/api/api_player.php", "a=UEM%7CSEM%7CMEM%7CCH%7CSWQ&id%5Fnlshow=" + getId(downloadLink.getDownloadURL()) + "&timestamp=" + ts + "&skey=" + getSKey(ts) + "&quality=0");
            DLLINK = br.getRegex("\\&url=(http://[^<>\"\\'\\&]+\\.mp4)\\&").getMatch(0);
            if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            filename = filename.trim();
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null) {
                ext = ".mp4";
            }
            downloadLink.setFinalFileName(filename + ext);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
    }

    private String getSKey(long s) {
        return JDHash.getMD5(JDHash.getMD5(String.valueOf(s)) + Encoding.Base64Decode(SALT));
    }

    private String getId(String s) {
        return new Regex(s, "\\?id=(\\d+)").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}