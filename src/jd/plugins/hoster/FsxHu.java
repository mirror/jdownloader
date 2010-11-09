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

import java.io.BufferedInputStream;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fsx.hu" }, urls = { "http://s.*?.fsx.hu/.+/.+" }, flags = { 2 })
public class FsxHu extends PluginForHost {

    public FsxHu(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://www.fsx.hu/index.php?m=home&o=szabalyzat";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("www.fsx.hu");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        if (!br.containsHTML("V.lassz az ingyenes let.lt.s .s a regisztr.ci. k.z.l!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.getPage("http://www.fsx.hu/download.php?i=1");
        String filename = br.getRegex("<font color=\"#FF0000\" size=\"4\">(.+?)</font>").getMatch(0);
        String filesize = br.getRegex("<strong>M.ret:</strong> (.+?) B.jt").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    private void downloadImage(String url) throws Exception {
        jd.http.Browser br1 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        BufferedInputStream input = null;
        try {
            con = br1.openGetConnection(url);
            input = new BufferedInputStream(con.getInputStream());
            byte[] b = new byte[1024];
            while (input.read(b) != -1) {
            }
        } finally {
            try {
                input.close();
            } catch (Throwable e) {
            }
            try {
                con.disconnect();
            } catch (Throwable e1) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        handleFree0(downloadLink);
    }

    public void handleFree0(DownloadLink downloadLink) throws Exception {
        downloadImage("http://www.fsx.hu/img/button-letoltes1.gif");
        downloadImage("http://www.fsx.hu/img/bg0.gif");
        downloadImage("http://www.fsx.hu/img/bg1dl.gif");
        downloadImage("http://www.fsx.hu/img/bg5.gif");
        downloadImage("http://www.fsx.hu/img/bg6.gif");
        downloadImage("http://www.fsx.hu/img/bg4b.gif");
        downloadImage("http://www.fsx.hu/img/bg3.gif");
        downloadImage("http://www.fsx.hu/img/style.css");
        String url1 = null;
        String url2 = null;
        for (int i = 0; i <= 30; i++) {
            logger.info("Attempt " + i + " of 30");
            String continueLink = br.getRegex("<font size=\"4\"><a href=\"(http://.*?)</a></font>").getMatch(0);
            if (continueLink != null) {
                logger.info("Found continueLink...");
                br.getPage(continueLink);
                System.out.print(br.toString());
            }
            url1 = br.getRegex("<a id=\\'dlink\\' href=\"(.+?)\">").getMatch(0);
            url2 = br.getRegex("elem\\.href = elem\\.href \\+ \"(.+?)\";").getMatch(0);
            if (url1 != null && url2 != null) break;
            String serverQueueLength = br.getRegex("<font color=\"#FF0000\"><strong>(\\d+?)</strong></font> felhaszn.l. van el.tted").getMatch(0);
            if (serverQueueLength == null) {
                logger.warning("serverQueueLength is null...");
                serverQueueLength = "notfound";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            // next run of handleFree() will report the file as deleted
            // if it is really deleted because fsx.hu sometimes reports
            // timeouted sessions as non-existing/removed downloads
            if (br.containsHTML("A kiv.lasztott f.jl nem tal.lhat. vagy elt.vol.t.sra ker.lt."))
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 1 * 1000l);
            else if (br.containsHTML("A kiv.lasztott f.jl let.lt.s.t nem kezdted meg")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 1000l);
            sleep(15000L, downloadLink, JDL.LF("plugins.hoster.fsxhu.waiting", "%s users in queue, ", serverQueueLength));
            br.getPage("http://www.fsx.hu/download.php");
            downloadImage("http://www.fsx.hu/img/bg0.gif");
        }
        if (url1 == null || url2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String url = url1 + url2;
        dl = BrowserAdapter.openDownload(br, downloadLink, url);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.clearCookies("www.fsx.hu");
        br.setCustomCharset("iso-8859-2");
        br.getPage("http://fsx.hu/index.php?m=home&o=admin");
        br.postPage("http://fsx.hu/index.php?m=home&o=admin", "u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()) + "&x=0&y=0");
        System.out.print(br.toString());
        if (!br.containsHTML("Előfizetésed a következő időpontig érvényes")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);

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
        String expire = br.getRegex("következő időpontig érvényes: <b>(.*?)</b>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            expire = expire.replaceAll("(<b>|</b>)", "");
            ai.setValidUntil(Regex.getMilliSeconds(expire, "yyyy-MMMM-dd hh:mm:ss", null));
        }
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        handleFree0(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public int getMaxConnections() {
        return 1;
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
