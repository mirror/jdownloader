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

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videobam.com" }, urls = { "http://(www\\.)?videobam\\.com/(?!user|faq|login|dmca|signup|terms)(videos/download/|widget/)?[A-Za-z0-9]+" }, flags = { 0 })
public class VideoBamCom extends PluginForHost {

    public VideoBamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) throws PluginException {
        setFUID(link);
        link.setUrlDownload("http://videobam.com/videos/download/" + FUID);
    }

    private String FUID = null;

    private String setFUID(DownloadLink link) throws PluginException {
        FUID = new Regex(link.getDownloadURL(), "widget/([A-Za-z0-9]+)").getMatch(0);
        if (FUID == null) FUID = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        if (FUID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return FUID;
    }

    @Override
    public String getAGBLink() {
        return "http://videobam.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setFUID(downloadLink);
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">404 Page Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("File name: ([^<>\"]*?)<br />").getMatch(0);
        final String filesize = br.getRegex("File size: ([^<>\"]*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(Encoding.htmlDecode(filename));
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final boolean resume = true;
        final int chunks = 1;
        final String ajaxDLurl = br.getRegex("\\'(/videos/ajax_download_url/[^<>\"]*?)\\'").getMatch(0);
        if (ajaxDLurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // Waittime can be skipped
        // int wait = 30;
        // final String waittime = br.getRegex("var timeout = (\\d+);").getMatch(0);
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        br.getPage("http://videobam.com" + ajaxDLurl);
        String dllink = null;
        if (br.containsHTML("You can not download more than 1 video per 30 minutes")) {
            // Try stream download, only throw exception if that also fails
            br.getPage("http://videobam.com/" + FUID);
            dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
        } else {
            if (br.containsHTML("\"url\":false")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("You can not download more than 1 video per 30 minutes")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}