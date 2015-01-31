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
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chip.de" }, urls = { "http://(www\\.)?(chip\\.de/downloads|download\\.chip\\.(eu|asia)/.{2})/[A-Za-z0-9\\-]+_\\d+\\.html|http://(www\\.)?chip\\.de/video/[A-Za-z0-9\\-]+_\\d+\\.html" }, flags = { 0 })
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

    private static final String type_file_invalidlinks = "http://(www\\.)?(chip\\.de/downloads|download\\.chip\\.(eu|asia)/.{2})/download\\-manager\\-for\\-free\\-zum\\-download.+";
    private static final String type_file              = "http://(www\\.)?(chip\\.de/downloads|download\\.chip\\.(eu|asia)/.{2})/[A-Za-z0-9\\-]+_\\d+\\.html";
    private static final String type_video             = "http://(www\\.)?chip\\.de/video/[A-Za-z0-9\\-]+_\\d+\\.html";

    private String              DLLINK                 = null;

    // Links sind Sprachen zugeordnet. Leider kann man diese nicht alle auf eine
    // Sprache abändern. Somit muss man alle Sprachen manuell einbauen oder
    // bessere Regexes finden, die überall funktionieren
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getDownloadURL().matches(type_file_invalidlinks)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
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
        String filename = null;
        if (link.getDownloadURL().matches(type_video)) {
            filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\"").getMatch(0);
            DLLINK = br.getRegex("itemprop=\"contentURL\" content=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) {
                DLLINK = br.getRegex("data\\-mp4=\"(https?://[^<>\"]*?)\"").getMatch(0);
            }
            if (DLLINK == null) {
                DLLINK = br.getRegex("\"(https?://video\\.chip\\.de/\\d+/[^<>\"]*?)\"").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename = encodeUnicode(filename);
            filename += ".mp4";
            link.setFinalFileName(filename);
            try {
                try {
                    try {
                        /* @since JD2 */
                        con = br.openHeadConnection(DLLINK);
                    } catch (final Throwable t) {
                        /* Not supported in old 0.9.581 Stable */
                        con = br.openGetConnection(DLLINK);
                    }
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            filename = br.getRegex("<title>(.*?)\\- Download \\- CHIP Online</title>").getMatch(0);
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
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename = encodeUnicode(filename);
            link.setName(filename);
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
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(type_video)) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            br.setFollowRedirects(true);
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
            DLLINK = getDllink();
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        }
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

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}