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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rufox.ru" }, urls = { "https?://(?:www\\.)?files\\.rufox\\.ru/\\?(?:Act=byCategory&)?k=[a-z0-9]+|https?://video\\.rufox\\.ru/play/\\d+" })
public class RufoxRu extends PluginForHost {

    public RufoxRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://rufox.ru/";
    }

    private static final String TYPE_FILES = "https?://(?:www\\.)?files\\.rufox\\.ru/\\?(?:Act=byCategory&)?k=[a-z0-9]+";
    private static final String TYPE_VIDEO = "https?://video\\.rufox\\.ru/.+";

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(TYPE_FILES) && !link.getDownloadURL().contains("Act=byCategory")) {
            link.setUrlDownload(link.getDownloadURL().replace("?k", "?Act=byCategory&k"));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        String filename = null;
        String filesize = null;
        final String fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        if (link.getDownloadURL().matches(TYPE_FILES)) {
            if (this.br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("class=\"fileName\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<h2 class=\"fileName\">([^<>\"]*?)</h2>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>Файлы :: скачать ([^<>\"]*?) \\- RuFox</title>").getMatch(0);
            }
            if (filename == null) {
                filename = fid;
            }
            filename = Encoding.htmlDecode(filename.trim());
            link.setName(filename);

            filesize = br.getRegex("<tr class=\"info\">[\t\n\r ]+<td>\\&nbsp;</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        } else {
            if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().contains("/error/notfound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("itemprop=\"name\">([^<>\"]+)<").getMatch(0);
            if (filename == null) {
                filename = fid;
            }
            filename += ".flv";
            link.setFinalFileName(filename);

            filesize = this.br.getRegex("class=\"size_video\">([^<>\"]+)<").getMatch(0);
        }
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
            filesize = filesize.replace("Г", "G");
            filesize = filesize.replace("М", "M");
            filesize = filesize.replaceAll("(к|К)", "k");
            filesize = filesize.replaceAll("(Б|б)", "B");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = null;
        if (downloadLink.getDownloadURL().matches(TYPE_FILES)) {
            dllink = br.getRegex("\"(/download/[a-z0-9]{32})\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "http://files.rufox.ru" + dllink;
        } else {
            final String url_continue = this.br.getRegex("\"(https?://video\\.rufox\\.ru/code/[^<>\"]+)\"").getMatch(0);
            if (url_continue == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage(url_continue);
            final String videoid = this.br.getRegex("video_([A-Za-z0-9]+)").getMatch(0);
            if (videoid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "http://video.rufox.ru/video/video_" + videoid + ".flv";
        }
        /* More chunks possible but results in server errors most times */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            }
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
        return 2;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}