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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fsx.hu" }, urls = { "http://s.*?.fsx.hu/.+/.+" }, flags = { 2 })
public class FsxHu extends PluginForHost {

    public FsxHu(final PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = this.br.getRegex("következő időpontig érvényes: <b>(.*?)</b>").getMatch(0);
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
    public String getAGBLink() {
        return "http://www.fsx.hu/index.php?m=home&o=szabalyzat";
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
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.br.setFollowRedirects(true);
        this.requestFileInformation(downloadLink);
        this.handleFree0(downloadLink);
    }

    public void handleFree0(final DownloadLink downloadLink) throws Exception {
        String url1 = null;
        String url2 = null;
        for (int i = 0; i <= 40; i++) {
            final String gif[] = this.br.getRegex("img/(.*?)\\.gif").getColumn(-1);
            this.br.getHeaders().put("Referer", this.br.getURL());
            for (final String template : gif) {
                this.br.cloneBrowser().openGetConnection(template);
            }
            this.br.getHeaders().put("Referer", null);
            url1 = this.br.getRegex("<a id=\\'dlink\\' href=\"(.+?)\">").getMatch(0);
            url2 = this.br.getRegex("elem\\.href = elem\\.href \\+ \"(.+?)\";").getMatch(0);
            if ((url1 != null) && (url2 != null)) {
                break;
            }
            String serverQueueLength = this.br.getRegex("<font color=\"#FF0000\"><strong>(\\d+?)</strong></font> felhaszn.l. van el.tted").getMatch(0);
            if (serverQueueLength == null) {
                Plugin.logger.warning("serverQueueLength is null...");
                serverQueueLength = "notfound";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            // next run of handleFree() will report the file as deleted
            // if it is really deleted because fsx.hu sometimes reports
            // timeouted sessions as non-existing/removed downloads
            if (this.br.containsHTML("A kiv.lasztott f.jl nem tal.lhat. vagy elt.vol.t.sra ker.lt.")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 1 * 1000l);
            } else if (this.br.containsHTML("A kiv.lasztott f.jl let.lt.s.t nem kezdted meg")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 1000l); }
            // waittime
            int waitTime = 15;
            final String wait = this.br.getRegex("CONTENT=\"(\\d+);").getMatch(0);
            if (wait != null) {
                waitTime = Integer.parseInt(wait);
            }
            if (i != 0) {
                this.sleep(waitTime * 1001L, downloadLink, JDL.LF("plugins.hoster.fsxhu.waiting", "%s users in queue, ", serverQueueLength));
            }
            this.br.getPage("http://www.fsx.hu/download.php");
        }
        if ((url1 == null) || (url2 == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        final String url = url1 + url2;
        this.dl = BrowserAdapter.openDownload(this.br, downloadLink, url);
        if (this.dl.getConnection().getContentType().contains("html")) {
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.requestFileInformation(link);
        this.login(account);
        this.br.setFollowRedirects(false);
        this.br.getPage(link.getDownloadURL());
        this.handleFree0(link);
    }

    private void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        this.br.setCookiesExclusive(true);
        this.br.setFollowRedirects(true);
        this.br.clearCookies("www.fsx.hu");
        this.br.setCustomCharset("iso-8859-2");
        this.br.getPage("http://fsx.hu/index.php?m=home&o=admin");
        this.br.postPage("http://fsx.hu/index.php?m=home&o=admin", "u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()) + "&x=0&y=0");
        System.out.print(this.br.toString());
        if (!this.br.containsHTML("Előfizetésed a következő időpontig érvényes")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }

    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.br.setFollowRedirects(true);
        this.br.setCookiesExclusive(true);
        this.br.clearCookies("www.fsx.hu");
        this.br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        this.br.getPage(downloadLink.getDownloadURL());
        if (!this.br.containsHTML("V.lassz az ingyenes let.lt.s .s a regisztr.ci. k.z.l!")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        this.br.getPage("http://www.fsx.hu/download.php?i=1");
        final String filename = this.br.getRegex("<font color=\"#FF0000\" size=\"4\">(.+?)</font>").getMatch(0);
        final String filesize = this.br.getRegex("<strong>M.ret:</strong> (.+?) B.jt").getMatch(0);
        if ((filename == null) || (filesize == null)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
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
