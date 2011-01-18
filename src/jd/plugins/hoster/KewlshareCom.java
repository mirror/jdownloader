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

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kewlshare.com" }, urls = { "http://[\\w\\.]*?kewlshare\\.com/(dl/[\\w]+/.*?\\.html|share/[a-z0-9]+)" }, flags = { 2 })
public class KewlshareCom extends PluginForHost {

    public KewlshareCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://kewlshare.com/loginpremium.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        String fileid = new Regex(link.getDownloadURL(), "/share/([a-z0-9]+)").getMatch(0);
        if (fileid != null) {
            link.setUrlDownload("http://kewlshare.com/dl/" + fileid + "/");
        }
    }

    @Override
    public String getAGBLink() {
        return "http://kewlshare.com/tos";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getPage("http://kewlshare.com/");
        br.postPage("http://kewlshare.com/login.php", "login=null&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&loginButton.x=51&loginButton.y=14&loginButton=Login");
        if (br.getCookie("http://kewlshare.com/", "file_uid") == null || br.getCookie("http://kewlshare.com/", "file_logined") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://kewlshare.com/myaccount.php");
        String type = br.getRegex("ACCOUNT TYPE<.*?class=.*?>.*?>(.*?)<").getMatch(0);
        if (type == null || !type.equalsIgnoreCase("Premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String expires = br.getRegex("Expire Date.*?>.*?class=.*?>.*?>(.*?)<").getMatch(0);
        if (expires != null) {
            /* FIXME: days and months right? */
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "MM-dd-yy", null));
            account.setValid(true);
        } else {
            account.setValid(false);
        }
        return ai;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("the file you requested is either deleted or not found in our database") || (br.getRedirectLocation() != null && br.getRedirectLocation().contains("NO_FILE_FOUND"))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)\\.html</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        Form dlForm = br.getForm(0);
        if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlForm, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, "No free downloads possible, contact kewlshare support!");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2500;
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
