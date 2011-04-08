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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "diglo.com" }, urls = { "http://(www\\.)?diglo\\.com/download/[a-z0-9]+" }, flags = { 0 })
public class DigloCom extends PluginForHost {

    public DigloCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.diglo.com/terms_of_use.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>diglo\\.com</title>|/images/404\\.gif\\'\\))")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("<h2><span>Download:</span>(.*?)\\&nbsp;\\&nbsp;\\&nbsp;(.*?)</h2>");
        String filename = br.getRegex("var fullname = \"(.*?)\";").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div class=\"dl_info\">[\t\n\r ]+<span title=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) free download</title>").getMatch(0);
                if (filename == null) {
                    filename = fileInfo.getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("class=\"row odd\"><span>Filename:</span>(.*?)</div>").getMatch(0);
                    }
                }
            }
        }
        String filesize = fileInfo.getMatch(1);
        if (filesize == null) {
            filesize = br.getRegex("<span class=\"fileSize\">(.*?)</span>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("class=\"row\"><span>Filesize:</span>(.*?)</div>").getMatch(0);
            }
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("download/", "download/free/"));
        br.setFollowRedirects(false);
        String dllink = br.getRegex("id=\"link_activator\" style=\"display:none\"><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://x\\d+\\.diglo\\.com/download/[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 45;
        String waittime = br.getRegex("var wait_time = (\\d+)").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(You are already downloading a file|Please wait until your download is complete or)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}