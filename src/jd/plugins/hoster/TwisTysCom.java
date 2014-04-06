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

import java.util.HashMap;

import jd.PluginWrapper;
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

    private static final String             MAINPAGE = "http://www.twistys.com";

    private static HashMap<Account, String> MAP      = new HashMap<Account, String>();

    public TwisTysCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://secure.twistys.com/signup/signup.php?");
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
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.twistyssupport.com/";
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
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        // Möglicherweise serverfehler...
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            synchronized (MAP) {
                br.getPage("http://members.twistys.com/home/");
                if (!br.containsHTML("Logout of Twistys")) {
                    MAP.remove(account);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void login(Account account, boolean force) throws Exception {
        synchronized (MAP) {
            try {
                this.setBrowserExclusive();
                br.setFollowRedirects(true);
                // Save the session cookie and re-use it because logging in for each
                // downloadlink will get the account (temporarily) blocked
                String savedSessionCookie = MAP.get(account);
                boolean ret = true;
                if (savedSessionCookie != null) {
                    br.setCookie(MAINPAGE, "PHPSESSID", savedSessionCookie);
                    if (force) {
                        br.getPage("http://members.twistys.com/home/");
                        String sessionCookie = br.getCookie(MAINPAGE, "PHPSESSID");
                        if (sessionCookie == null || !br.containsHTML("Logout of Twistys")) {
                            ret = false;
                        }
                    }
                    if (ret) return;
                }
                br.getPage("http://members.twistys.com/home/");
                br.postPage("http://members.twistys.com/access/loginsubmit", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&Submit1.x=94&Submit1.y=25");
                String sessionCookie = br.getCookie(MAINPAGE, "PHPSESSID");
                if (sessionCookie == null || !br.containsHTML("Logout of Twistys")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                MAP.put(account, sessionCookie);
            } catch (final PluginException e) {
                MAP.remove(account);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        // We need to log in to get the file status
        synchronized (MAP) {
            Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null) {
                link.getLinkStatus().setStatusText("Links are only checkable if a premium account is entered!");
                return AvailableStatus.UNCHECKABLE;
            }
            login(aa, false);
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
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}