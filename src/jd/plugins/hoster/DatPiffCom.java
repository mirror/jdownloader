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

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datpiff.com" }, urls = { "http://(www\\.)?datpiff\\.com/(.*?\\-download\\.php\\?id=[a-z0-9]+|mixtapes\\-detail\\.php\\?id=\\d+)" }, flags = { 2 })
public class DatPiffCom extends PluginForHost {

    private static final String PREMIUMONLY            = ">you must be logged in to download mixtapes<";
    private static final String ONLYREGISTEREDUSERTEXT = "Only downloadable for registered users";
    private static final String MAINPAGE               = "http://www.datpiff.com/";

    public DatPiffCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.datpiff.com/register");
    }

    public void correctDownloadLink(DownloadLink link) throws IOException {
        if (link.getDownloadURL().matches("http://(www\\.)?datpiff\\.com/mixtapes\\-detail\\.php\\?id=\\d+")) {
            link.setUrlDownload(link.getDownloadURL().replace("datpiff.com/mixtapes-detail.php?id=", "datpiff.com/pop-mixtape-download.php?id="));
        } else if (!link.getDownloadURL().contains("-download.php")) {
            final Browser br2 = new Browser();
            br2.getPage(link.getDownloadURL());
            String downID = br2.getRegex("openMixtape\\( \\'(.*?)\\'").getMatch(0);
            if (downID == null) downID = br2.getRegex("mixtapePlayer\\.swf\\?mid=(.*?)\"").getMatch(0);
            if (downID != null) link.setUrlDownload("http://www.datpiff.com/pop-mixtape-download.php?id=" + downID);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(ONLYREGISTEREDUSERTEXT)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.datpiffcom.only4premium", ONLYREGISTEREDUSERTEXT));
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("(>Download Unavailable<|>A zip file has not yet been generated<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (!link.getDownloadURL().contains("-download.php")) {
            logger.warning("downID not found, link is broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = br.getRegex("<title>Download Mixtape \\&quot;(.*?)\\&quot;</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<span>Download Mixtape<em>(.*?)</em></span>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        // Untested
        // final String timeToRelease =
        // br.getRegex("\\'dateTarget\\': (\\d+),").getMatch(0);
        // if (timeToRelease != null) throw new
        // PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
        // "Not yet released", Long.parseLong(timeToRelease) -
        // System.currentTimeMillis());
        final String fileID = new Regex(downloadLink.getDownloadURL(), "download\\.php\\?id=(.+)").getMatch(0);
        if (fileID == null || !br.containsHTML("download\\-mixtape")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://www.datpiff.com/download-mixtape", "id=" + fileID + "&x=84&y=11");
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Server doesn't send the correct filename directly, filename fix also
        // doesn't work so we have to do it this way
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
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
        account.setValid(true);
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.datpiff.com/terms";
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
        requestFileInformation(downloadLink);
        if (br.containsHTML(PREMIUMONLY)) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.datpiffcom.only4premium", ONLYREGISTEREDUSERTEXT));
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        doFree(link);
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.postPage("http://www.datpiff.com/login", "cmd=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie(MAINPAGE, "dp4uid") == null || br.getCookie(MAINPAGE, "lastuser") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}