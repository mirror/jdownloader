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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

//nahraj.cz by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nahraj.cz" }, urls = { "http://[\\w\\.]*?nahraj\\.cz/content/(view|download)/[a-z|0-9]+-[a-z|0-9]+-[a-z|0-9]+-[a-z|0-9]+-[a-z|0-9]+" }, flags = { 0 })
public class NahrajCz extends PluginForHost {

    public NahrajCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://nahraj.cz/";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        String downloadlinklink = link.getDownloadURL().replaceAll("(view|download)", "view");
        link.setUrlDownload(downloadlinklink);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        String dlpage = link.getDownloadURL().replaceAll("(view|download)", "download");
        br.getPage(dlpage);
        if (br.containsHTML("Neznamý soubor")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename0 = br.getRegex("class=\"title\">(.*?)</span>").getMatch(0);
        String filename1 = br.getRegex("content/dw/.*?-.*?-.*?-.*?-.*?/.*?(\\..*?)\">.*?<div id=\"widget").getMatch(0);
        String filename = filename0 + filename1;
        String filesize = br.getRegex("class=\"size\">(.*?)</span>").getMatch(0);
        if (filename0 == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        // sleep(5000l, downloadLink);
        // handling for photo-links
        if (br.containsHTML("wrapper image")) {
            String dllink = br.getRegex("<div class=\"item-content\"><a href=\"(.*?)\" rel").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String filename0 = br.getRegex("<div class=\"wrapper image\">.*?<h1>(.*?)</h1>.*?<div class=\"item-content").getMatch(0);
            String filename1 = br.getRegex("item-content\"><a href=\"http://www.nahraj.cz/content/dw/[a-z|0-9]+-[a-z|0-9]+-[a-z|0-9]+-[a-z|0-9]+-[a-z|0-9]+/orig/[a-z|0-9]+(\\..*?)\" rel=\"faceb").getMatch(0);
            String filename = filename0 + filename1;
            downloadLink.setFinalFileName(filename);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        } else {
            // handling for all other links
            String dlpage = downloadLink.getDownloadURL().replaceAll("(view|download)", "download");
            br.getPage(dlpage);
            String dllink = br.getRegex("multipart/form-data\" action=\"(.*?)\">").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String filename0 = br.getRegex("class=\"title\">(.*?)</span>").getMatch(0);
            String filename1 = br.getRegex("content/dw/.*?-.*?-.*?-.*?-.*?/.*?(\\..*?)\">.*?<div id=\"widget").getMatch(0);
            String filename = filename0 + filename1;
            downloadLink.setFinalFileName(filename);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
