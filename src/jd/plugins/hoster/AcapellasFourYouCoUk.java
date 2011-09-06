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
import jd.controlling.AccountController;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "acapellas4u.co.uk" }, urls = { "http://(www\\.)?acapellas4u\\.co\\.uk/\\d+\\-[a-z0-9\\-_]+" }, flags = { 2 })
public class AcapellasFourYouCoUk extends PluginForHost {

    public AcapellasFourYouCoUk(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.acapellas4u.co.uk/ucp.php?mode=register");
    }

    @Override
    public String getAGBLink() {
        return "http://www.acapellas4u.co.uk/ucp.php?mode=terms";
    }

    private static final String MAINPAGE = "http://www.acapellas4u.co.uk/";
    private static final Object LOCK     = new Object();
    private static final String USERTEXT = "Only downloadable for registered users!";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.acapellas4youcouk.only4registered", USERTEXT));
            return AvailableStatus.TRUE;
        }
        login(aa, false);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/download_list.php") || br.containsHTML("<title>ACAPELLAS4U \\&bull; Browse Artists</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>ACAPELLAS4U \\&bull; File Download \\&bull; (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"title\" : \"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("var idcomments_post_title = \\'(.*?)\\';").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<p><b>File Name: ([^\"\\']+)</b></p>").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("File Size: ([^<>\"\\']+)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.acapellas4youcouk.only4registered", USERTEXT));
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(false);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).matches(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).matches(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("acas4u_sevul_u") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.postPage("http://www.acapellas4u.co.uk/ucp.php?mode=login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&sid=&redirect=index.php&login=Login&autologin=on");
            if (br.getCookie(MAINPAGE, "acas4u_sevul_u") == null || "1".equals(br.getCookie(MAINPAGE, "acas4u_sevul_u"))) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
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
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String hash = br.getRegex("filehash=([a-z0-9]+)").getMatch(0);
        if (hash == null) {
            hash = br.getRegex("\"unique_id\" : \"(.*?)\"").getMatch(0);
            if (hash == null) {
                hash = br.getRegex("var idcomments_post_id = \\'([a-z0-9]+)\\';").getMatch(0);
                if (hash == null) {
                    hash = br.getRegex("name=\"filehash\" type=\"hidden\" value=\"(.*?)\"").getMatch(0);
                }
            }
        }
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://www.acapellas4u.co.uk/download_file.php?&filehash=" + hash + "&confirm=1/", false, 1);
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