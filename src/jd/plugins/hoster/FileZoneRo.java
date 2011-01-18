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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filezone.ro" }, urls = { "http://[\\w\\.]*?filezone\\.ro/(files/[0-9a-z_]+/[0-9a-zA-Z_.]+|public.php\\?action=viewfile&file_id=\\d+|public/viewset/\\d+)" }, flags = { 0 })
public class FileZoneRo extends PluginForHost {

    public FileZoneRo(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://filezone.ro/terms.php";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        String dllink = null;
        if (br.containsHTML("pfile.php")) dllink = "http://filezone.ro/pfile.php?" + br.getRegex("pfile.php\\?(file_id=\\d+&d_id=\\d+&action=download)").getMatch(0);
        if (br.containsHTML("getfile.php")) dllink = "http://filezone.ro/" + br.getRegex("<a title=\".*\" alt=\".*\" href=\"(.*?)\" style=\"text-decoration:none;font-weight:bold;\">").getMatch(0);
        if (dllink.equals("http://filezone.ro/pfile.php?null") || dllink.equals("http://filezone.ro/null")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        String filename = null;
        String filesize = null;
        if (br.containsHTML("Not Found") || br.containsHTML("The upload set you are looking for does not exist. Perhaps it was deleted.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = br.getRegex("<span style=\"font-size:1.2em;font-weight:bold;color:#FFF\">(.*?) <br>Size: (.*?)</span><br />").getMatch(0);
        filesize = br.getRegex("<span style=\"font-size:1.2em;font-weight:bold;color:#FFF\">(.*?) <br>Size: (.*?)</span><br />").getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename);
        parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
