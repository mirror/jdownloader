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
import java.util.Random;

import jd.PluginWrapper;
import jd.http.Browser;
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
import jd.utils.locale.JDL;

//This plugin gets all its links from a decrypter!
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chomikuj.pl" }, urls = { "\\&id=.*?\\&gallerylink=.*?60423fhrzisweguikipo9re.*?\\&" }, flags = { 2 })
public class ChoMikujPl extends PluginForHost {

    public ChoMikujPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "http://chomikuj.pl/Regulamin.aspx";
    }

    private String              DLLINK              = null;
    private static final String PREMIUMONLY         = "(Aby pobrać ten plik, musisz być zalogowany lub wysłać jeden SMS\\.|Właściciel tego chomika udostępnia swój transfer, ale nie ma go już w wystarczającej)";
    private static final String PREMIUMONLYUSERTEXT = "Download is only available for registered/premium users!";
    private static final String MAINPAGE            = "http://chomikuj.pl/";
    private static final String FILEIDREGEX         = "\\&id=(.*?)\\&";
    private boolean             videolink           = false;

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("60423fhrzisweguikipo9re", "chomikuj.pl"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (link.getStringProperty("video") == null) {
            getDllink(link);
            if (br.containsHTML("Najprawdopodobniej plik został w miedzyczasie usunięty z konta")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML(PREMIUMONLY)) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.chomikujpl.only4registered", PREMIUMONLYUSERTEXT));
                return AvailableStatus.TRUE;
            }
        } else {
            br.setFollowRedirects(true);
            videolink = true;
            br.getPage("http://chomikuj.pl/ShowVideo.aspx?id=" + new Regex(link.getDownloadURL(), FILEIDREGEX).getMatch(0));
            if (br.getURL().contains("chomikuj.pl/Error404.aspx") || br.containsHTML("(Nie znaleziono|Strona, której szukasz nie została odnaleziona w portalu\\.<|>Sprawdź czy na pewno posługujesz się dobrym adresem)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.setFollowRedirects(false);
            final String realID = br.getRegex("\\$\\(document\\)\\.ready\\( function\\(\\) \\{[\t\n\r ]+return ch\\.ShowVideo\\((\\d+),").getMatch(0);
            if (realID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://chomikuj.pl/Video.ashx?realid=" + realID + "&id=" + new Regex(link.getDownloadURL(), FILEIDREGEX).getMatch(0) + "&type=1&ts=" + new Random().nextInt(1000000000) + "&file=video&start=0");
            DLLINK = br.getRedirectLocation();
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // In case the link redirects to the finallink
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                if (!videolink) link.setFinalFileName(getFileNameFromHeader(con));
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
        requestFileInformation(downloadLink);
        if (br.containsHTML(PREMIUMONLY)) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.chomikujpl.only4registered", PREMIUMONLYUSERTEXT));
        if (!videolink) getDllink(downloadLink);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String additionalPath = downloadLink.getStringProperty("path");
        if (additionalPath != null) downloadLink.addSubdirectory(additionalPath);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void getDllink(DownloadLink theLink) throws NumberFormatException, PluginException, IOException {
        Browser br2 = br.cloneBrowser();
        // Set by the decrypter if the link is password protected
        String savedLink = theLink.getStringProperty("savedlink");
        String savedPost = theLink.getStringProperty("savedpost");
        if (savedLink != null && savedPost != null) br.postPage(savedLink, savedPost);
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage("http://chomikuj.pl/Chomik/License/Download", "fileId=" + new Regex(theLink.getDownloadURL(), FILEIDREGEX).getMatch(0));
        DLLINK = br2.getRegex("redirectUrl\":\"(http://.*?)\"").getMatch(0);
        if (DLLINK == null) DLLINK = br2.getRegex("\\\\u003ca href=\\\\\"(.*?)\\\\\"").getMatch(0);
        if (DLLINK != null) DLLINK = Encoding.htmlDecode(DLLINK);
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getPage(MAINPAGE);
        String viewstate = br.getRegex("name=\"__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"(.*?)\"").getMatch(0);
        if (viewstate == null) {
            logger.warning("Chomikuj.pl login broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage(MAINPAGE, "__EVENTTARGET=&__EVENTARGUMENT=&__VIEWSTATE=" + Encoding.urlEncode(viewstate) + "&PageCmd=&PageArg=&ctl00%24LoginTop%24LoginChomikName=" + Encoding.urlEncode(account.getUser()) + "&ctl00%24LoginTop%24LoginChomikPassword=" + Encoding.urlEncode(account.getPass()) + "&ctl00%24LoginTop%24LoginButton.x=0&ctl00%24LoginTop%24LoginButton.y=0&ctl00%24SearchInputBox=&ctl00%24SearchFileBox=&ctl00%24SearchType=all&SType=0&ctl00%24CT%24ChomikLog%24LoginChomikName=&ctl00%24CT%24ChomikLog%24LoginChomikPassword=");
        if (br.getCookie(MAINPAGE, "ChomikSession") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        account.setValid(true);
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        getDllink(link);
        if (DLLINK == null) {
            String[] crappyConfirmVars = br.getRegex("\\\\u0027(.*?)\\\\u0027").getColumn(0);
            if (crappyConfirmVars == null || crappyConfirmVars.length < 3) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // For some files they ask
            // "Do you really want to download this file", so we have to confirm
            // it with "YES" here ;)
            br.postPage("http://chomikuj.pl/Chomik/License/acceptOwnTransfer?fileId=" + new Regex(link.getDownloadURL(), FILEIDREGEX).getMatch(0), "orgFile=" + Encoding.urlEncode(crappyConfirmVars[1]) + "&userSelection=" + Encoding.urlEncode(crappyConfirmVars[2]));
            getDllink(link);
        }
        if (DLLINK == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
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