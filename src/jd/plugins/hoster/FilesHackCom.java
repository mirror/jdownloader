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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshack.com" }, urls = { "http://[\\w\\.]*?fileshack\\.com/(file|file_download)\\.x/[0-9]+" }, flags = { 0 })
public class FilesHackCom extends PluginForHost {

    public FilesHackCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileshack.com/extras/tos.x";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(file|file_download)", "file"));
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File was deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<dt>Filename</dt>.*?<dd title=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("<dt>Size</dt>.*?<dd>.*?\\((.*?)\\)</dd>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replace(",", "");
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setDebug(true);
        String dlink = link.getDownloadURL();
        dlink = dlink.replace("file.", "file_download.");
        br.getPage(dlink);
        Form form = br.getFormbyProperty("name", "nologinform");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.submitForm(form);

        String PublicWesternUSA = br.getRegex("'(/popup\\..*?pay=0)'").getMatch(0);
        if (PublicWesternUSA == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        PublicWesternUSA = "http://www.fileshack.com" + PublicWesternUSA;
        br.getPage(PublicWesternUSA);
        System.out.print(br.toString());
        String frameserver = new Regex(br.getURL(), "(http://[a-z]+\\.[a-z0-9]+\\.fileshack\\.com)").getMatch(0);
        if (frameserver == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String dlframe = br.getRegex("frameborder=\"[0-9]\" src=\"(.*?)\"").getMatch(0);
        if (dlframe == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dlframe = frameserver + dlframe;
        br.getPage(dlframe);
        System.out.print(br.toString());
        String nextframe = br.getRegex("\"><a href=\"(.*?)\"").getMatch(0);
        if (nextframe == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        nextframe = frameserver + nextframe;
        sleep(6000l, link);
        br.getPage(nextframe);
        System.out.print(br.toString());
        String dllink = br.getRegex("downloadbutton\"><a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0).startDownload();
    }

    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
