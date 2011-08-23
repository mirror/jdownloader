package jd.plugins.hoster;

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

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fsx.hu" }, urls = { "http://s.*?.fsx.hu/.+/.+" }, flags = { 0 })
public class FsxHu extends PluginForHost {

    public FsxHu(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fsx.hu/index.php?o=dijcsomagok");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("kovetkezo idopontig ervenyes: (\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.fsx.hu/index.php?m=home&o=szabalyzat";
    }

    @Override
    public int getMaxConnections() {
        return 1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        handleFree0(downloadLink);
    }

    public void handleFree0(final DownloadLink downloadLink) throws Exception {
        String url1 = null;
        String url2 = null;
        for (int i = 0; i <= 40; i++) {
            final String gif[] = br.getRegex("img/(.*?)\\.gif").getColumn(-1);
            br.getHeaders().put("Referer", br.getURL());
            for (final String template : gif) {
                br.cloneBrowser().openGetConnection(template);
            }
            br.getHeaders().put("Referer", null);
            if (br.containsHTML("10 perced van")) {

                url1 = br.getRegex("\t\t<a href=\"(.+?)\"><span class=\"gomb jovahagyas\"").getMatch(0);
                url2 = "";
                break;
            }

            url1 = br.getRegex("font size=\"\\d+\"><a href=\"(http.*?)\"").getMatch(0);
            if (url1 != null) {
                /* new format */
                url2 = "";
                break;
            }

            url1 = br.getRegex("<a id=\\'dlink\\' href=\"(.+?)\">").getMatch(0);
            url2 = br.getRegex("elem\\.href = elem\\.href \\+ \"(.+?)\";").getMatch(0);
            if (url1 != null && url2 != null) {
                break;
            }

            String serverQueueLength = br.getRegex("<span style=\"color:#dd0000;font-weight:bold;\">(\\d+?)</span> felhaszn..l.. van el..tted").getMatch(0);
            if (serverQueueLength == null) {
                logger.warning("serverQueueLength is null...");
                serverQueueLength = "notfound";
            }
            // next run of handleFree() will report the file as deleted
            // if it is really deleted because fsx.hu sometimes reports
            // timeouted sessions as non-existing/removed downloads
            if (br.containsHTML("A kiv..lasztott f..jl nem tal..lhat.. vagy elt..vol..t..sra ker..lt")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 1 * 1000l);
            } else if (br.containsHTML("A kiv..lasztott f..jl let..lt..s..t nem kezdted meg")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 1000l); }
            // waittime
            int waitTime = 15;
            final String wait = br.getRegex("CONTENT=\"(\\d+);").getMatch(0);
            if (wait != null) {
                waitTime = Integer.parseInt(wait);
            }
            if (i != 0) {
                this.sleep(waitTime * 1001L, downloadLink, JDL.LF("plugins.hoster.fsxhu.waiting", "%s users in queue, ", serverQueueLength));
            }
            br.getPage("http://www.fsx.hu/download.php");
        }
        if (url1 == null || url2 == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        final String url = url1 + url2;
        dl = BrowserAdapter.openDownload(br, downloadLink, url);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        br.getHeaders().put("Accept", "text/html, */*");
        br.getHeaders().put("Content-Type", "text/html");
        br.getHeaders().put("User-Agent", "FSX letöltésvezérlo v1.1.0.3");
        br.getHeaders().put("Accept-Language", null);
        br.getHeaders().put("Accept-Encoding", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Pragma", null);
        br.getPage("http://fsx.hu/testaccount.php?un=" + Encoding.urlEncode(account.getUser()) + "&pw=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("A megadott jelszo nem megfelelo") || !br.containsHTML("A megadott hozzaferes aktiv, mely a kovetkezo idopontig ervenyes:")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        System.err.println("requestfileinfo");
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("www.fsx.hu");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        if (!br.containsHTML("V..lassz az ingyenes let..lt..s ..s a regisztr..ci.. k..z..l!")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        br.getPage("http://www.fsx.hu/download.php?i=1");
        final String filename = br.getRegex("<h1 style=\"padding-bottom:0;font-size:16px;\">(.+?)</h1>").getMatch(0);
        final String filesize = br.getRegex("M..ret: (.+?) b..jt").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
