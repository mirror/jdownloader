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

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wallbase.cc" }, urls = { "http://(www\\.)?wallbase.cc/wallpaper/\\d+" }, flags = { 2 })
public class WallBaseCc extends PluginForHost {

    public WallBaseCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://wallbase.cc/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final Account acc = AccountController.getInstance().getValidAccount(this);
        if (acc != null) login(this.br, acc, false);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        // Offline1
        if (br.getURL().equals("http://wallbase.cc/home") || br.getURL().equals("http://wallbase.cc/index.php") || br.containsHTML("Access denied\\!|This might be happening because|>We are experiencing some technical")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Offline2
        if (br.containsHTML("(>Not found \\(404\\)|>The page you requested was not found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) \\- Wallpaper \\(").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\(#\\d+\\) / Wallbase\\.cc</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".jpg");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDownload(downloadLink);
    }

    public void handleDownload(final DownloadLink downloadLink) throws Exception, PluginException {
        br.setFollowRedirects(false);
        final String dllink = getDllink();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink() throws PluginException {
        String finallink = br.getRegex("<div id=\"bigwall\" class=\"right\">[\t\n\r ]+<img src=\"(http://.*?)\"").getMatch(0);
        if (finallink == null) finallink = br.getRegex("\"(http://ns\\d+\\.ovh\\.net/.*?)\"").getMatch(0);
        /* simple Base64 */
        if (finallink == null) {
            finallink = br.getRegex("\\d+x\\d+ Wallpaper\"[^\\(]+\\(\'([^\']+)\'\\)").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<img src=\"[^\\(]+\\('([a-zA-Z0-9\\+/\\=]+)").getMatch(0);
            if (finallink != null) finallink = Encoding.Base64Decode(finallink);
        }
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return Encoding.htmlDecode(finallink);
    }

    private static final String MAINPAGE = "http://wallbase.cc";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public void login(Browser br, final Account account, final boolean force) throws Exception {
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
                br.setFollowRedirects(true);
                // br.getPage("");
                br.postPage("http://wallbase.cc/user/login", "nopass_email=Type+in+your+e-mail+and+press+enter&nopass=0&usrname=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("class=\"wlcm\">Hey \\&nbsp; ")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Account active");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        handleDownload(link);
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