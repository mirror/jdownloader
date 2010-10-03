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
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitshare.com" }, urls = { "http://[\\w\\.]*?bitshare\\.com/files/[a-z0-9]{8}/.*?\\.html" }, flags = { 0 })
public class BitShareCom extends PluginForHost {

    public BitShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://bitshare.com/terms-of-service.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">We are sorry, but the request file could not be found in our database")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex nameAndSize = br.getRegex("<h1>Downloading (.*?) - ([0-9\\.]+ [A-Za-z]+)</h1>");
        String filename = nameAndSize.getMatch(0);
        String filesize = nameAndSize.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize.replace("yte", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("You reached your hourly traffic limit")) {
            String wait = br.getRegex("id=\"blocktimecounter\">(\\d+) Seconds</span>").getMatch(0);
            if (wait == null) wait = br.getRegex("var blocktime = (\\d+);").getMatch(0);
            if (wait != null)
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
            else
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        String fileID = new Regex(downloadLink.getDownloadURL(), "bitshare\\.com/files/([a-z0-9]{8})/").getMatch(0);
        String tempID = br.getRegex("var ajaxdl = \"(.*?)\";").getMatch(0);
        if (fileID == null || tempID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://bitshare.com/files-ajax/" + fileID + "/request.html", "request=generateID&ajaxid=" + tempID);
        String rgexedWait = br.getRegex("file:(\\d+):").getMatch(0);
        int wait = 45;
        if (rgexedWait != null) wait = Integer.parseInt(rgexedWait);
        sleep(wait * 1001l, downloadLink);
        br.postPage("http://bitshare.com/files-ajax/" + fileID + "/request.html", "request=getDownloadURL&ajaxid=" + tempID);
        String dllink = br.getRegex("SUCCESS#(http://.+)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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
        return 2;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}