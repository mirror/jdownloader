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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chip.de" }, urls = { "https?://(?:www\\.)?(?:chip\\.de/downloads|download\\.chip\\.(?:eu|asia)/.{2})/[A-Za-z0-9_\\-]+_\\d+\\.html|https?://(?:www\\.)?chip\\.de/video/[^/]+_\\d+\\.html" }, flags = { 0 })
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
    private static final String type_file              = "http://(www\\.)?(chip\\.de/downloads|download\\.chip\\.(eu|asia)/.{2})/[A-Za-z0-9_\\-]+_\\d+\\.html";
    private static final String type_file_chip_eu      = "http://(www\\.)?download\\.chip\\.(eu|asia)/.{2}/[A-Za-z0-9_\\-]+_\\d+\\.html";
    private static final String type_video             = "https?://(?:www\\.)?chip\\.de/video/[^/]+_\\d+\\.html";

    private String              DLLINK                 = null;

    // Links sind Sprachen zugeordnet. Leider kann man diese nicht alle auf eine
    // Sprache abändern. Somit muss man alle Sprachen manuell einbauen oder
    // bessere Regexes finden, die überall funktionieren
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (link.getDownloadURL().matches(type_file_invalidlinks)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("ISO-8859-1");

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            final long responsecode = con.getResponseCode();
            if (responsecode == 404 || responsecode == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }

        String filename = null;
        String filename_url = null;
        if (link.getDownloadURL().matches(type_video)) {
            filename_url = new Regex(link.getDownloadURL(), "chip\\.de/video/(.+)_\\d+\\.html$").getMatch(0);
            filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\"").getMatch(0);
            final String sp = this.br.getRegex("sp/(\\d+)/embedIframeJs").getMatch(0);
            final String entryid = this.br.getRegex("/entry_id/([^/]*?)/").getMatch(0);
            final String uiconfid = this.br.getRegex("uiconf_id/(\\d+)").getMatch(0);
            String wid = this.br.getRegex("/partner_id/(\\d+)").getMatch(0);
            if (wid == null) {
                wid = this.br.getRegex("kaltura.com/p/(\\d+)").getMatch(0);
            }
            final String playerid = this.br.getRegex("playerConf\\[\"([^<>\"]*?)\"\\]").getMatch(0);
            if (sp == null || entryid == null || uiconfid == null || wid == null || playerid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* They use waay more arguments via browser - we don't need them :) */
            final String postData = "&cache_st=5&wid=_" + wid + "&uiconf_id=" + uiconfid + "&entry_id=" + entryid + "&playerId=" + playerid + "&urid=2.34";
            this.br.postPage("http://cdnapi.kaltura.com/html5/html5lib/v2.34/mwEmbedFrame.php", postData);
            final String json = this.br.getRegex("window\\.kalturaIframePackageData = (\\{.*?\\});").getMatch(0);
            if (json == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            long max_bitrate = 0;
            long max_bitrate_temp = 0;
            String ext = null;
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json);
            final ArrayList<Object> ressourcelist = (ArrayList) DummyScriptEnginePlugin.walkJson(entries, "entryResult/contextData/flavorAssets");
            for (final Object videoo : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) videoo;
                final String flavourid = (String) entries.get("id");
                ext = (String) entries.get("fileExt");
                if (flavourid == null) {
                    continue;
                }
                max_bitrate_temp = DummyScriptEnginePlugin.toLong(entries.get("bitrate"), 0);
                if (max_bitrate_temp > max_bitrate) {
                    DLLINK = "http://cdnapi.kaltura.com/p/" + wid + "/sp/" + sp + "/playManifest/entryId/" + entryid + "/flavorId/" + flavourid + "/format/url/protocol/http/a.mp4";
                    max_bitrate = max_bitrate_temp;
                }
            }
            // DLLINK = "http://video.chip.de/38396417/textzwei.flv";
            if (filename == null) {
                filename = filename_url;
            }
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (ext == null) {
                ext = "mp4";
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename = encodeUnicode(filename);
            filename += "." + ext;
            link.setFinalFileName(filename);
            try {
                try {
                    con = br.openHeadConnection(DLLINK);
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
            if (link.getDownloadURL().matches(type_file_chip_eu) && !this.br.containsHTML("class=\"downloadnow_button")) {
                /* chip.eu url without download button --> No downloadable content --> URL is offline for us */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
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
                /* All RegExes failed? Get filename from url. */
                filename = new Regex(link.getDownloadURL(), "/([A-Za-z0-9\\-]+)_\\d+\\.html$").getMatch(0);
                if (filename == null) {
                    filename = new Regex(link.getDownloadURL(), "http://(www\\.)?(chip\\.de/downloads|download\\.chip\\.(eu|asia)/.{2})/download\\-manager\\-for\\-free\\-zum\\-download(.+)").getMatch(0);
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
            final String getfile_url = br.getRegex("\"(/.{2}/download_getfile_[^<>\"]*?)\"").getMatch(0);
            if (getfile_url != null) {
                /* chip.eu */
                this.br.getPage(getfile_url);
                DLLINK = br.getRegex("If not, please click <a href=\"(http[^<>\"]*?)\"").getMatch(0);
            } else {
                /* chip.de */
                String step1 = br.getRegex("\"https?://x\\.chip\\.de/intern/dl/\\?url=(http[^<>\"]*?)\"").getMatch(0);
                if (step1 == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                step1 = Encoding.htmlDecode(step1);
                br.getPage(step1);
                String step2 = br.getRegex("\"(https?://(www\\.)?chip\\.de/downloads/c1_downloads_hs_getfile[^<>\"]*?)\"").getMatch(0);
                if (step2 != null) {
                    step2 = Encoding.htmlDecode(step2);
                    br.getPage(step2);
                }
                DLLINK = getDllink();
            }
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
        }
        if (dllink == null) {
            dllink = br.getRegex("</span></a></div><a href=\"(http.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("var adtech_dl_url = \\'(https?://[^<>\"]*?)\\';").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("(?:\"|\\')(https?://dl\\.cdn\\.chip\\.de/downloads/\\d+/.*?)(?:\"|\\')").getMatch(0);
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