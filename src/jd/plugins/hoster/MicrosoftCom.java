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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "microsoft.com" }, urls = { "http://(www\\.)?microsoft\\.com/(en\\-us|de\\-de)/download/(details|confirmation)\\.aspx\\?id=\\d+" }, flags = { 0 })
public class MicrosoftCom extends PluginForHost {

    public MicrosoftCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.microsoft.com/en-us/legal/intellectualproperty/copyright/default.aspx";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.microsoft.com/en-us/download/details.aspx?id=" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">We are sorry, the page you requested cannot be found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String finfotable = br.getRegex("<table class=\"fileinfo\">(.*?)</table>").getMatch(0);
        if (finfotable == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String[] tableContent = new Regex(finfotable, "<p>([^<>\"]*?)</p>").getColumn(0);
        if (tableContent == null || tableContent.length < 4) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String filename = tableContent[2];
        final String filesize = tableContent[3];
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("var downloadFileUrl = \"(https?://[^<>\"]*?)\";").getMatch(0);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}