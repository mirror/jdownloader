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

import java.net.MalformedURLException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "swoopshare.com" }, urls = { "http://[\\w\\.]*?swoopshare\\.com/file/[a-z0-9]+" }, flags = { 0 })
public class SwoopshareCom extends PluginForHost {

    public SwoopshareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink downloadLink) throws MalformedURLException {
        // fileid should never be null...
        String fileid = new Regex(downloadLink.getDownloadURL(), "/file/([a-z0-9]+)").getMatch(0);
        if (fileid != null) downloadLink.setUrlDownload("http://en.swoopshare.com/file/" + fileid);

    }

    public String getAGBLink() {
        return "http://de.swoopshare.com/info/terms";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String size = br.getRegex("</b> \\((.*)yte\\)").getMatch(0);
        String name = br.getRegex("<title>(cshare\\.de|swoopshare) \\-(.*?)</title>").getMatch(1);
        if (name == null) {
            name = br.getRegex("<span style=\"font-size:26px; font\\-weight:bold\">(.*?)</span>").getMatch(0);
        }
        if (name == null || size == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(name.trim().replace("Download ", ""));
        downloadLink.setDownloadSize(SizeFormatter.getSize(size));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String finallink = br.getRegex("<span class=\"arrow\">\\&#187;</span> <b><a href=\"(.*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("('|\")(/get/d/.*?/.*?)('|\")").getMatch(1);
            if (finallink == null) {
                finallink = br.getRegex("win=window\\.open\\('(.*?)'").getMatch(0);
            }
        }
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        finallink = "http://" + br.getHost() + finallink.replace("?queue", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }

}