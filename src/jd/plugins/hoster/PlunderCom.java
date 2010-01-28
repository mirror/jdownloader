//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//plunder.com by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "plunder.com" }, urls = { "http://[\\w\\.]*?(youdownload\\.eu|binarybooty\\.com|mashupscene\\.com|plunder\\.com|files\\.youdownload\\.com)/((-download-[a-z0-9]+|.+-download-.+)\\.htm|(?!/)[0-9a-z]+)" }, flags = { 0 })
public class PlunderCom extends PluginForHost {

    public PlunderCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.plunder.com/x/tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        // Sometimes the links are outdated but they show the new links, the
        // problem is they they new and newer links could also be moved so this
        // is why we need the following part!
        for (int i = 0; i <= 5; i++) {
            String objectMoved = br.getRegex("<h2>Object moved to <a href=\"(.*?)\">here</a>").getMatch(0);
            if (objectMoved == null) objectMoved = br.getRegex("This document may be found <a HREF=\"(.*?)\"").getMatch(0);
            if (objectMoved != null) {
                objectMoved = Encoding.htmlDecode(objectMoved);
                if (!objectMoved.contains("http")) objectMoved = "http://www.plunder.com" + objectMoved;
                br.getPage(objectMoved);
                objectMoved = br.getRegex("<h2>Object moved to <a href=\"(.*?)\">here</a>").getMatch(0);
                if (objectMoved == null) objectMoved = br.getRegex("This document may be found <a HREF=\"(.*?)\"").getMatch(0);
                if (objectMoved != null) continue;
                downloadLink.setUrlDownload(objectMoved);
                break;
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (br.getURL().contains("/search/?f=")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)download - Plunder").getMatch(0);
        if (filename == null) filename = br.getRegex("<h2>Download</h2>(.*?)\\(").getMatch(0);
        String filesize = br.getRegex("<h2>Download</h2>.*?\\((.*?)\\)<BR").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("<h2>Download</h2>.*?a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://pearl\\.plunder\\.com/x/.*?/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            String checklink = br.getURL();
            if (br.containsHTML("You must log in to download more this session") || checklink.contains("/login/")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Register or perform a reconnect to download more!", 10 * 60 * 1001l);
            if (checklink.contains("/error")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
