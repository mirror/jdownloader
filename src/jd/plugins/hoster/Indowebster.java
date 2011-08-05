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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "indowebster.com" }, urls = { "http://[\\w\\.]*?indowebster\\.com/(download/files/.+|[^\\s]+\\.html)" }, flags = { 0 })
public class Indowebster extends PluginForHost {

    public Indowebster(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.indowebster.com/policy-tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(Requested file is deleted|image/default/404\\.png\")") || br.getURL().contains("/error") || br.getURL().contains("/files_not_found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Convert old links to new links
        String newlink = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+;URL=(http://v\\d+\\.indowebster\\.com/.*?)\"").getMatch(0);
        if (newlink != null) {
            newlink = newlink.trim();
            downloadLink.setUrlDownload(newlink.trim());
            logger.info("New link set...");
            br.getPage(newlink);
        }
        String filename = br.getRegex("<title>Free Download (.*?) \\| ").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"dl\\-title\" title=\"(.*?)\">").getMatch(0);
        }
        String filesize = br.getRegex(">Size : <span style=\"float:none;\">(.*?)</span><").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("Date upload: .{1,20} Size: (.*?)\"").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String ad_url = br.getRegex("<a id=\"download\" href=\"(http://.*?)\"").getMatch(0);
        if (ad_url == null) {
            ad_url = br.getRegex("\"(http://v\\d+\\.indowebster\\.com/downloads/jgjbcf/[a-z0-9]+)\"").getMatch(0);
        }
        if (ad_url == null) {
            logger.warning("ad_url is null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!ad_url.startsWith("http://")) ad_url = "http://www.indowebster.com/" + ad_url;
        br.getPage(ad_url);
        String dllink = br.getRegex("id=\"link\\-download\" align=\"center\"><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://v\\d+\\.indowebster\\.com/downloads/jgjbcf/[a-z0-9]+/\\d+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.trim();
        String waittime = br.getRegex("var sec = (\\d+);").getMatch(0);
        if (waittime == null) waittime = br.getRegex("document\\.counter\\.dl2\\.value=\\'(\\d+)\\';").getMatch(0);
        int wait = 25;
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">Indowebster\\.com under maintenance")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.indowebster.undermaintenance", "Under maintenance"), 30 * 60 * 1000l);
            if (br.containsHTML("But Our Download Server Can be Accessed from Indonesia Only")) throw new PluginException(LinkStatus.ERROR_FATAL, "Download Server Can be Accessed from Indonesia Only");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
