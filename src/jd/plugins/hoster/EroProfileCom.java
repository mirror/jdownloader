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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eroprofile.com" }, urls = { "http://(www\\.)?eroprofile\\.com/m/(videos|photos)/view/[A-Za-z0-9\\-_]+" }, flags = { 2 })
public class EroProfileCom extends PluginForHost {

    public EroProfileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.eroprofile.com/p/help/termsOfUse";
    }

    private static final String VIDEOLINK   = "http://(www\\.)?eroprofile\\.com/m/videos/view/[A-Za-z0-9\\-_]+";
    private static Object       LOCK        = new Object();
    private static final String MAINPAGE    = "http://eroprofile.com";
    public static final String  NOACCESS    = "(>You do not have the required privileges to view this page|>No access<)";
    private static final String PREMIUMONLY = "The video could not be processed";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.setCookie("http://eroprofile.com/", "lang", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(NOACCESS)) {
            downloadLink.getLinkStatus().setStatusText("Only available for registered users");
            return AvailableStatus.TRUE;
        }
        if (downloadLink.getDownloadURL().matches(VIDEOLINK)) {
            if (br.containsHTML("(>Video not found|>The video could not be found|<title>EroProfile</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = getFilename();
            if (br.containsHTML(PREMIUMONLY)) {
                downloadLink.setName(filename + ".m4v");
                downloadLink.getLinkStatus().setStatusText("This file is only available to premium members");
                return AvailableStatus.TRUE;
            }
            DLLINK = br.getRegex("file:\\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = Encoding.htmlDecode(DLLINK);
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".m4v";
            downloadLink.setFinalFileName(filename + ext);
        } else {
            if (br.containsHTML("(>Photo not found|>The photo could not be found|<title>EroProfile</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = getFilename();
            DLLINK = br.getRegex("<div class=\"viewPhotoContainer\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (!DLLINK.startsWith("http")) DLLINK = "http://www.eroprofile.com" + DLLINK;
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".jpg";
            downloadLink.setFinalFileName(filename + ext);
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(NOACCESS)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to registered members");
        } else if (br.containsHTML(PREMIUMONLY)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to premium members");
        }
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
                br.setCookie("http://eroprofile.com/", "lang", "en");
                br.setFollowRedirects(false);
                br.getHeaders().put("X_REQUESTED_WITH", "XMLHttpRequest");
                br.postPage("http://www.eroprofile.com/process.php?0." + System.currentTimeMillis(), "a=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "memberID") == null || !br.toString().trim().equals("OK")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Free Account");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(br, account, false);
        br.setFollowRedirects(false);
        requestFileInformation(link);
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private String getFilename() throws PluginException {
        String filename = br.getRegex("<tr><th>Title:</th><td>([^<>\"]*?)</td></tr>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>EroProfile \\- ([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return Encoding.htmlDecode(filename.trim());
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
