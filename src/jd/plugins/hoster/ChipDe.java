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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chip.de" }, urls = { "http://(www\\.)?(chip\\.de/downloads|download\\.chip\\.(eu|asia)/.{2})/(?!download\\-manager\\-for\\-free\\-zum\\-download).*?_\\d+\\.html" }, flags = { 0 })
public class ChipDe extends PluginForHost {

    public ChipDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("www.", ""));
    }

    @Override
    public String getAGBLink() {
        return "http://www.chip.de/s_specials/c1_static_special_index_13162756.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    // Links sind Sprachen zugeordnet. Leider kann man diese nicht alle auf eine
    // Sprache abändern. Somit muss man alle Sprachen manuell einbauen oder
    // bessere Regexes finden, die überall funktionieren
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("ISO-8859-1");

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (con.getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }

        br.setFollowRedirects(false);
        String filename = br.getRegex("<title>(.*?)\\- Download \\- CHIP Online</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("somtr\\.prop18=\"(.*?)\";").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Zum Download:(.*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("name=\"ueberschrift\" value=\"(.*?)\"").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("var cxo_adtech_page_title = \\'(.*?)\\';").getMatch(0);
                    }
                }
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setName(filename.trim());
        String filesize = br.getRegex(">Dateigr\\&ouml;\\&szlig;e:</p>[\t\n\r ]+<p class=\"col2\">([^<>\"]*?)<meta itemprop=\"fileSize\"").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<dt>(File size:|Размер файла:|Dimensioni:|Dateigröße:|Velikost:|Fájlméret:|Bestandsgrootte:|Rozmiar pliku:|Mărime fişier:|Dosya boyu:|文件大小：)<br /></dt>[\t\n\r ]+<dd>(.*?)<br /></dd>").getMatch(1);
        }
        if (filesize != null) {
            filesize = filesize.replace("GByte", "GB");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        String md5 = br.getRegex("<dt>(Контрольная сумма \\(MD 5\\):|Checksum:|Prüfsumme:|Kontrolní součet:|Szumma:|Suma kontrolna|Checksum|Kontrol toplamı:|校验码：)<br /></dt>[\t\n\r ]+<dd>(.*?)<br /></dd>").getMatch(1);
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String dllink;
        String step1 = br.getRegex("\"http://x\\.chip\\.de/intern/dl/\\?url=(http[^<>\"]*?)\"").getMatch(0);
        if (step1 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        step1 = Encoding.htmlDecode(step1);
        br.getPage(step1);
        String step2 = br.getRegex("\"(https?://(www\\.)?chip\\.de/downloads/c1_downloads_hs_getfile[^<>\"]*?)\"").getMatch(0);
        if (step2 != null) {
            br.getPage(step2);
        }
        dllink = getDllink();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    private String getDllink() {
        String dllink = br.getRegex("Falls der Download nicht beginnt,\\&nbsp;<a class=\"b\" href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("class=\"dl\\-btn\"><a href=\"(http.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("</span></a></div><a href=\"(http.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(http://dl\\.cdn\\.chip\\.de/downloads/\\d+/.*?)\"").getMatch(0);
                }
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}