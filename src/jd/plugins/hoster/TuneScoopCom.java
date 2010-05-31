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

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tunescoop.com" }, urls = { "http://[\\w\\.]*?tunescoop\\.com/play/\\d+/" }, flags = { 0 })
public class TuneScoopCom extends PluginForHost {

    public TuneScoopCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tunescoop.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (!br.containsHTML("class=\"thickbox\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) - TuneScoop - Free music hosting and Sharing</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div style=\"font-size:24px\"><b>(.*?)</b></div>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("name=\"description\" content=\"(.*?) - TuneScoop\\.com, enjoy the Tune Sharing - TuneScoop - Free music hosting and Sharing\">").getMatch(0);

                if (filename == null) {
                    filename = br.getRegex("name=\"keywords\" content=\"(.*?),audio,sharing,script,youtube,clone,TuneScoop - Free music hosting and Sharing\">").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("color=\"#000000\"><b>Size:</b>(.*?)</font>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "dl=1");
        String dllink = br.getRegex("<div style=\"color:#000000; font-weight:bold\">Click the \"download\" button to download this Tune</div>[\n\r ]+<br /><br />[\n\r ]+<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\.tunescoop\\.com/download/\\d+/\\d+/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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