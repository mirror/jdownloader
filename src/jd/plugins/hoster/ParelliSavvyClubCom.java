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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "parellisavvyclub.com" }, urls = { "http://[\\w\\.]*?parellisavvyclub\\.com/watchMedia\\.faces\\?id=\\d+" }, flags = { 2 })
public class ParelliSavvyClubCom extends PluginForHost {

    public ParelliSavvyClubCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://shop.parellinaturalhorsetraining.com/savvySignupStep1.jsf");
    }

    @Override
    public String getAGBLink() {
        return "http://www.parellisavvyclub.com/termsofservice.faces";
    }

    private static final String DLLINKREGEX = "\"(http://media\\.parelli\\.com/DownloadMedia\\?key=.*?)\"";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        String filename = link.getName();
        long filesize = link.getDownloadSize();
        if (filename.contains("watchMedia.faces_id=") || filesize == 0) {
            Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null) {
                link.getLinkStatus().setStatusText("Link only checkable if you have an account!");
                return AvailableStatus.UNCHECKABLE;
            }
            login(aa);
            br.getPage(link.getDownloadURL());
            filename = br.getRegex("\"http://media\\.parelli\\.com/DownloadMedia\\?key=.*?\">(.*?)<br />").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>Download(.*?)- SociFiles\\.com</title>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(filename.trim());
            String dllink = br.getRegex(DLLINKREGEX).getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setDownloadSize(br.openGetConnection(dllink).getContentLength());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadalbe for premium members");
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("https://www.parellisavvyclub.com/login.faces");
        String viewState = br.getRegex("id=\"javax\\.faces\\.ViewState\" value=\"(j_id\\d+)\"").getMatch(0);
        if (viewState == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("https://www.parellisavvyclub.com/login.faces", "funid=funid&funid%3AusernameInput=" + Encoding.urlEncode(account.getUser()) + "&funid%3ApasswordInput=" + Encoding.urlEncode(account.getPass()) + "&funid%3AloginBut.x=0&funid%3AloginBut.y=0&javax.faces.ViewState=" + viewState);
        logger.info(br.toString());
        br.getPage("http://www.parellisavvyclub.com/home.faces");
        if (!br.containsHTML("\">Welcome to the Journey")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        account.setValid(true);
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
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex(DLLINKREGEX).getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
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