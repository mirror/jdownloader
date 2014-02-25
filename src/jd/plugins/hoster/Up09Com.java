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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up09.com" }, urls = { "http://(www\\.)?up09\\.com/download\\d+\\.html" }, flags = { 0 })
public class Up09Com extends PluginForHost {

    public Up09Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://up09.com/rules.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("class=\"error\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex(">اسم الملف</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        final String ext = br.getRegex(">نوع الملف</td>[\t\n\r ]+<td class=\"tddata\">([^<>\"]*?)</td>").getMatch(0);
        final String filesize = br.getRegex(">حجم الملف</td>[\t\n\r ]+<td class=\"tddata\">([^<>\"]*?)</td>").getMatch(0);
        if (filename == null || ext == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "." + Encoding.htmlDecode(ext.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = downloadLink.getDownloadURL().replace("up09.com/download", "up09.com/down-");
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
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}