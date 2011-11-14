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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sockshare.com" }, urls = { "http://(www\\.)?sockshare.com/(mobile/)?(file|embed)/[A-Z0-9]+" }, flags = { 0 })
public class SockShareCom extends PluginForHost {

    public SockShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.sockshare.com/page.php?terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/mobile", "").replace("/embed/", "/file/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("sockshare.com/?404") || br.containsHTML("(>404 Not Found<|>This file doesn\\'t exist, or has been removed|<title>Share Files Easily on SockShare</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("<h1>(.*?)<strong>\\( (.*?) \\)</strong></h1>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\| SockShare</title>").getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setDebug(true);
        String hash = br.getRegex("<input type=\"hidden\" value=\"([a-z0-9]+)\" name=\"hash\">").getMatch(0);
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Can still be skipped
        // String waittime =
        // br.getRegex("var countdownNum = (\\d+);").getMatch(0);
        // int wait = 5;
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        br.postPage(br.getURL(), "hash=" + hash + "&confirm=Continue+as+Free+User");
        String streamID = br.getRegex("\"(/get_file\\.php.*?)\"").getMatch(0);
        if (streamID == null){
            streamID = br.getRegex("\'(/get_file\\.php.*?)\'").getMatch(0);
        }        
        if (streamID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!streamID.contains("key=")){
                String key = br.getRegex("key:\\s+\'#?\\$?([0-9a-f]+)\'").getMatch(0);
                streamID = key == null ? streamID = "" : streamID + "&key=" + key;           
        }
        br.setFollowRedirects(false);
        br.getPage("http://www.sockshare.com" + streamID);        
        String dllink = br.getRegex("<media:content url=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://media\\-b\\d+\\.sockshare\\.com/download/-*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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