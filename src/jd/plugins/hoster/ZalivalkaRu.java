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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zalivalka.ru" }, urls = { "http://(www\\.)?zalivalka\\.ru/\\d+" }, flags = { 0 })
public class ZalivalkaRu extends PluginForHost {

    public ZalivalkaRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://zalivalka.ru/";
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/error.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex finfo = br.getRegex("<div class=\"filename\">[\r\n\t ]+<span class=\"left\">([^<>\"]*?)</span>[\r\n\t ]+<span class=\"right\">([^<>\"]*?)<div class=\"right\">");
        final String filename = finfo.getMatch(0);
        String filesize = finfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = fixFilesize(filesize);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String postpage = br.getRegex("<form action=\"(http://s\\d+\\.zalivalka\\.ru/download/[^<>\"]*?)\"").getMatch(0);
        final String download_key = br.getRegex("name=\"download_key\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (postpage == null || download_key == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        int maxChunks = -2;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) maxChunks = 1;

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, postpage, "download_key=" + download_key, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            /* Page simply re-loaded without downloadstart */
            if (br.containsHTML("<meta name=\"keywords\"")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(ZalivalkaRu.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(ZalivalkaRu.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(ZalivalkaRu.NOCHUNKS, false) == false) {
                downloadLink.setProperty(ZalivalkaRu.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private String fixFilesize(String filesize) {
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replaceAll("(к|К)", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize + "b";
        return filesize;
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