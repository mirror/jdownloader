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
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "x7.to" }, urls = { "http://[\\w\\.]*?x7\\.to/(?!list)[a-zA-Z0-9]+" }, flags = { 2 })
public class XSevenTo extends PluginForHost {

    public XSevenTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://x7.to/foyer");
    }

    @Override
    public String getAGBLink() {
        return "http://x7.to/legal";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getPage("http://x7.to");
        br.postPage("http://x7.to/james/login", "id=" + Encoding.urlEncode(account.getUser()) + "&pw=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://x7.to/", "login") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://x7.to/my");
        if (!br.containsHTML("id=\"status\" value=\"premium\"")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        /* have to call this in order so set language */
        br.getPage("http://x7.to/lang/en");
        br.getPage("http://x7.to/my");
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
        String validUntil = br.getRegex("Premium member until (.*?)\"").getMatch(0);
        if (validUntil != null) {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil.trim(), "yyyy-MM-dd HH:mm:ss", null));
        } else {
            if (!br.containsHTML("img/sym/crown.png")) {
                ai.setExpired(true);
            }
        }
        String points = br.getRegex("upoints\".*?>([0-9.]*?)<").getMatch(0);
        if (points != null) {
            long p = Long.parseLong(points.replaceFirst("\\.", ""));
            ai.setPremiumPoints(p);
        }
        String money = br.getRegex("id=\"balance\">([0-9.]+)").getMatch(0);
        if (money != null) ai.setAccountBalance(money);
        String remaining = br.getRegex("class=\"aT aR\">.*?\">(.*?)<").getMatch(0);
        if (remaining != null && remaining.contains("unlimited")) {
            /* unlimited acc */
        } else if (remaining != null) {
            ai.setTrafficLeft(remaining);
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String dllink = null;
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            dllink = br.getRedirectLocation();
        } else {

            if (br.containsHTML("img/elem/trafficexh_de.png")) throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.uploadedto.errorso.premiumtrafficreached", "Traffic limit reached"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);

            dllink = br.getRegex("<b>Download</b>.*?href=\"(http://stor.*?)\"").getMatch(0);
            if (dllink == null && br.containsHTML("<b>Stream</b>")) {
                /* its a streamdownload */
                dllink = br.getRegex("window\\.location\\.href='(http://stor.*?)'").getMatch(0);
            }

        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        /* have to call this in order so set language */
        br.getPage("http://x7.to/lang/en");
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<title>x7\\.to » Download: (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<b>(Download|Stream)</b>.*?<span.*?>(.*?)<").getMatch(1);
            if (filename != null) {
                String extension = br.getRegex("<b>(Download|Stream)</b>.*?<span.*?>.*?<small.*?>(.*?)<").getMatch(1);
                if (extension == null) extension = "";
                filename = filename.trim() + extension;
            }
        }
        String filesize = br.getRegex("<b>(Download|Stream)</b>.*?\\((.*?)\\)").getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", ".")));
        if (br.containsHTML("(only premium members will be able to download the file|The requested file is larger than)")) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.XSevenTo.errors.only4premium", "Only downloadable for premium users"));
        return AvailableStatus.TRUE;
    }

    private Browser requestXML(Browser br, String url, String post, boolean clonebrowser) throws IOException {
        Browser brc = br;
        if (clonebrowser) brc = br.cloneBrowser();
        brc.setDebug(true);
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.postPage(url, post != null ? post : "");
        brc.getHeaders().remove("X-Requested-With");
        return brc;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("(only premium members will be able to download the file|The requested file is larger than)")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.XSevenTo.errors.only4premium", "Only downloadable for premium users"));
        String dllink = null;
        String fileID = new Regex(downloadLink.getDownloadURL(), "\\.to/([a-zA-Z0-9]+)").getMatch(0);
        boolean isStream = br.containsHTML("<b>Stream</b>");
        if (!isStream) {
            Browser brc = requestXML(br, "http://x7.to/james/ticket/dl/" + fileID, null, false);
            /* error handling */
            if (brc.containsHTML("err:")) {
                String error = brc.getRegex("err:\"(.*?)\"").getMatch(0);
                if (error == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (error.contains("limit-parallel")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                if (error.contains("limit-dl")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
                if (error.contains("Download denied")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerProblem", 30 * 60 * 1000l);
                /* unknown error */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (brc.containsHTML("type:'download")) {
                int waitsecs = 0;
                String waittime = brc.getRegex("wait:(\\d+)").getMatch(0);
                if (waittime != null) waitsecs = Integer.parseInt(waittime);
                if (waitsecs > 0) sleep(waitsecs * 1000l, downloadLink);
                dllink = brc.getRegex("url:'(.*?)'").getMatch(0);
            }
        } else {
            /* free users can only download the 10mins sample */
            br.getPage("http://x7.to/stream/" + fileID + "/h");
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!dllink.contains("x7.to/")) dllink = "http://x7.to/" + dllink;
        br.setDebug(true);
        /* streams are not resumable */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, !isStream, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("wird ein erneuter Versuch gestartet")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverproblems");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
