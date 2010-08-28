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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yousendit.com" }, urls = { "http://[\\w\\.]*?yousendit\\.com/(dl\\?phi_action=app/orchestrateDownload\\&rurl=http%253A%252F%252Fwww\\.yousendit\\.com%252Ftransfer\\.php%253Faction%253Dbatch_download%2526batch_id%253D|download/)[a-zA-Z0-9]+" }, flags = { 0 })
public class YouSendItCom extends PluginForHost {

    public YouSendItCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://yousendit.com/aboutus/legal/terms-of-service";
    }

    public void correctDownloadLink(DownloadLink link) {
        String fileID = new Regex(link.getDownloadURL(), "/dl\\?phi_action=app/orchestrateDownload\\&rurl=http%253A%252F%252Fwww\\.yousendit\\.com%252Ftransfer\\.php%253Faction%253Dbatch_download%2526batch_id%253D(.+)").getMatch(0);
        if (fileID != null) link.setUrlDownload("http://yousendit.com/download/" + fileID);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Download link is invalid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("style=\"width:390px; display:block; overflow:hidden;\">(.*?)</a>").getMatch(0);
        String filesize = br.getRegex("\">Size:\\&nbsp;<strong>(.*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set the final filename here because server sometimes doesn't give us
        // the correct filename
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("<div style=\"width:390px;font-size:14px;font-weight:bold\">[\t\r\n ]+<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("onclick=\\'showDownloadProcessing\\(this\\);\\' style=\\'position:absolute;top:10px;right:10px;\\'>[\t\r\n ]+<a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(directDownload\\?phi_action=app/directDownload\\&fl=[A-Za-z0]+(\\&experience=bas)?)\"").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!dllink.contains("yousendit.com")) dllink = "http://yousendit.com/" + dllink;
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