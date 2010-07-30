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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "softpedia.com" }, urls = { "http://[\\w\\.]*?softpedia\\.com/get/.+/.*?\\.shtml" }, flags = { 0 })
public class SoftPediaCom extends PluginForHost {

    public SoftPediaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.softpedia.com/user/terms.shtml";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>404 - page not found</h2>|404error\\.gif\"></td>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("google_ad_section_start --><h1>(.*?)<br/></h1><").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("style=\"padding-top: 15px;\">Softpedia guarantees that <b>(.*?)</b> is <b").getMatch(0);
            if (filename == null) filename = br.getRegex(">yahooBuzzArticleHeadline = \"(.*?)\";").getMatch(0);
        }
        String filesize = br.getRegex("([0-9\\.]+ (MB|KB))").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String nextPage = br.getRegex("<div align=\"center\">[\t\n\r ]+<a href=\"(.*?)\"").getMatch(0);
        br.getPage(nextPage);
        // They have many mirrors, we just pick a random one here because all
        // downloadlinks look pretty much the same
        String dllink = br.getRegex("\"(http://download\\.softpedia\\.(com|ro)/dl/.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("class=\"fontsize11\"><a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
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