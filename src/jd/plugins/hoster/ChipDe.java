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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chip.de" }, urls = { "http://[\\w\\.]*?chip\\.de/downloads/.*?_\\d+\\.html" }, flags = { 0 })
public class ChipDe extends PluginForHost {

    public ChipDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.chip.de/s_specials/c1_static_special_index_13162756.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Seite nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)- Download - CHIP Online</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("somtr\\.prop18=\"(.*?)\";").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Zum Download:(.*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("name=\"ueberschrift\" value=\"(.*?)\"").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("var cxo_adtech_page_title = '(.*?)';").getMatch(0);
                    }
                }
            }
        }
        String filesize = br.getRegex("class=\"col1\">Dateigr\\&ouml;\\&szlig;e:</p>.*?<p class=\"col2\">(.*?)</p>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String step1 = br.getRegex("class=\"dl-btn\"><a href=\"(http.*?)\"").getMatch(0);
        if (step1 == null) {
            step1 = br.getRegex("<h2 class=\"item hProduct\"><a href=\"(http.*?)\"").getMatch(0);
            if (step1 == null) {
                step1 = br.getRegex("\"(http://www\\.chip\\.de/downloads/.*?downloads_auswahl_\\d+\\.html.*?)\"").getMatch(0);
            }
        }
        if (step1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(step1);
        String step2 = br.getRegex("<div class=\"dl-faktbox-row bottom\">.*?<a href=\"(http.*?)\"").getMatch(0);
        if (step2 == null) step2 = br.getRegex("\"(http://www\\.chip\\.de/downloads/.*?downloads_hs_getfile_.*?)\"").getMatch(0);
        if (step2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(step2);
        String dllink = br.getRegex("Falls der Download nicht beginnt,\\&nbsp;<a class=\"b\" href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("class=\"dl-btn\"><a href=\"(http.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("</span></a></div><a href=\"(http.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(http://dl\\.cdn\\.chip\\.de/downloads/\\d+/.*?)\"").getMatch(0);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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