//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "twistys.com" }, urls = { "http://(www\\.)?(cdn\\d+\\.)?mdl\\.twistys\\.com/(content|visual)/[a-z0-9]+/.*?\\..{3}(\\?nvb=\\d+\\&nva=\\d+\\&hash=[a-z0-9]+)?" }, flags = { 2 })
public class TwisTysCom extends PluginForHost {

    public TwisTysCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://secure.twistys.com/signup/signup.php?");
    }

    @Override
    public String getAGBLink() {
        return "http://www.twistyssupport.com/";
    }

    private static final String MAINPAGE = "http://twistys.com";
    public static final Object  LOCK     = new Object();

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        // We need to log in to get the file status
        synchronized (LOCK) {
            Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Links are only checkable if a premium account is entered!");
            login(aa);
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(getFileNameFromHeader(con));
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
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
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for premium users");
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        // Save the session cookie and re-use it because logging in for each
        // downloadlink will get the account (temporarily) blocked
        String savedSessionCookie = this.getPluginConfig().getStringProperty("sessioncookie");
        boolean realLogin = false;
        if (savedSessionCookie != null) {
            br.setCookie(MAINPAGE, "sbsession", savedSessionCookie);
            br.getPage("http://members.twistys.com/");
            if (!br.containsHTML("/cgi-bin/sblogin/handoff2\\.cgi\\?site=members\\.twistys\\.com\\&u=")) {
                this.getPluginConfig().setProperty("sessioncookie", Property.NULL);
                this.getPluginConfig().save();
                realLogin = true;
                br.clearCookies(MAINPAGE);
            }
        } else {
            realLogin = true;
        }
        if (realLogin) {
            br.postPage("http://members.twistys.com/cgi-bin/sblogin/login.cgi", "uname=" + Encoding.urlEncode(account.getUser()) + "&pword=" + Encoding.urlEncode(account.getPass()) + "&Submit1.x=0&Submit1.y=0");
            String sessionCookie = br.getCookie(MAINPAGE, "sbsession");
            if (sessionCookie == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            this.getPluginConfig().setProperty("sessioncookie", sessionCookie);
            this.getPluginConfig().save();
        }

    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        // Möglicherweise serverfehler...
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