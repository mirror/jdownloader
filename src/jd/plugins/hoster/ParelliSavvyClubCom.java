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
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "parellisavvyclub.com" }, urls = { "http://(www\\.)?parellisavvyclub\\.com/(watchMedia\\.faces\\?id=\\d+|video\\?sckey=[^\"\\']+(\\&pl=\\d+)?)" }, flags = { 2 })
public class ParelliSavvyClubCom extends PluginForHost {

    public ParelliSavvyClubCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://shop.parellinaturalhorsetraining.com/savvySignupStep1.jsf");
    }

    @Override
    public String getAGBLink() {
        return "http://www.parellisavvyclub.com/termsofservice.faces";
    }

    private static final String MAINPAGE = "http://www.parellisavvyclub.com";
    private static final Object LOCK     = new Object();

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        String filename = link.getName();
        Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Link only checkable if you have an account!");
            return AvailableStatus.UNCHECKABLE;
        }
        login(aa, false);
        br.getPage(link.getDownloadURL());
        String dllink = getDllink();
        if (dllink == null) throw new Exception("Hey .bismarck, RTMP support is missing ;)");
        filename = new Regex(dllink, ".*?parelli\\.com/.{1,10}/(.*?)\\?Policy=").getMatch(0);
        if (filename == null) filename = br.getRegex("class=\"plWatchVideo playing\" rel=\"nofollow\" href=\"[^\"\\']+\">(.*?)<br>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        String ext = filename.substring(filename.lastIndexOf("."));
        if (ext == null) ext = ".mov";
        if (!filename.contains(ext)) filename += ext;
        link.setFinalFileName(filename);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html"))
                link.setDownloadSize(con.getLongContentLength());
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
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadalbe for premium members");
    }

    private String getDllink() {
        String dllink = br.getRegex("(http://down\\d+\\.parelli\\.com/[^\"\\']+)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("flashvars=\"file=(http://.*?)\"").getMatch(0);
        return dllink;
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
                if (cookies.containsKey("userName") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(false);
            br.getPage("https://www.parellisavvyclub.com/login.faces");
            String viewState = br.getRegex("id=\"javax\\.faces\\.ViewState\" value=\"(j_id\\d+)\"").getMatch(0);
            if (viewState == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.postPage("https://www.parellisavvyclub.com/login.faces", "funid=funid&funid%3AusernameInput=" + Encoding.urlEncode(account.getUser()) + "&funid%3ApasswordInput=" + Encoding.urlEncode(account.getPass()) + "&funid%3AloginBut.x=0&funid%3AloginBut.y=0&javax.faces.ViewState=" + viewState);
            if (br.getCookie(MAINPAGE, "userName") == null || br.getCookie(MAINPAGE, "email") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            account.setValid(true);
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
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        String dllink = getDllink();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
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