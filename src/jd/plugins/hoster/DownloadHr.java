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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "download.hr" }, urls = { "https?://(?:www\\.)?download\\.hr/software\\-(.*?)\\.html" })
public class DownloadHr extends antiDDoSForHost {
    private static final String MAINPAGE = "http://www.download.hr";

    public DownloadHr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.download.hr/about/impressum";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "dhrgo", "da");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; de; rv:1.9.2.18) Gecko/20110614 Firefox/3.6.18");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("Accept-Encoding", "gzip,deflate");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || "http://www.download.hr/".equals(this.br.getURL()) || br.containsHTML("<title>Download\\.hr \\- Free Software Downloads</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        String filesize = br.getRegex(">Download</a>[\t\n\r ]+<b class=\"font\\d+\">\\((.*?)\\)</b>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<span class=\"file_fileinfo_right\">(.*?)</span>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("itemprop=\"fileSize\">([^<>\"]*?)</span>").getMatch(0);
        }
        if (filename != null) {
            link.setName(filename.trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        getPage(link.getDownloadURL().replace("/software-", "/download-"));
        String finallink = br.getRegex("\"(https?://(www\\.)?download\\.hr/go\\.php\\?file=[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finallink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 1 * 60 * 60 * 1000l);
            }
        }
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}