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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pcwelt.de" }, urls = { "https?://(www\\.)?pcwelt\\.de/downloads/[^<>\"]+\\-\\d+\\.html" })
public class PcWeltDe extends PluginForHost {
    public PcWeltDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pcwelt.de/news/Impressum-975146.html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (!br.getURL().contains("/downloads/") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1 class=\"headline\">(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div class=\"boxed\">[\t\n\r ]+<div class=\"left\">(.*?)</div>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]+) \\- Download \\-").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        }
        if (filename == null) {
            filename = new Regex(link.getDownloadURL(), "/([^/]*?)\\.html$").getMatch(0);
        }
        String filesize = br.getRegex(">Dateigr&ouml;\\&szlig;e:</th>[\t\n\r ]+<td class=\"col\\-\\d+\">(.*?)</td>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<strong>Dateigröße:</strong>([^<>\"]*?)</li>").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filesize = filesize.replace(",", ".");
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (!filesize.equals("-")) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String undefined_downloadlink = br.getRegex("itemprop=\"url\"(?: rel=\"nofollow\")? href=\"(http[^<>\"]*?)\"").getMatch(0);
        String dllink = br.getRegex("href=\"(http[^<>\"]+?download\\.pcwelt\\.de[^<>\"]+)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(http[^<>\"]+?pcwelt.de[^<>\"]+?download_file[^<>\"]+?)\"").getMatch(0);
        }
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
        }
        if (dllink == null && undefined_downloadlink != null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Nicht downloadbar: externe Downloadquelle");
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
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