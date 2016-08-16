//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.Random;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wrzuta.pl" }, urls = { "http://[\\w\\.\\-]*?wrzuta\\.pl/(audio|film|obraz)/[a-zA-Z0-9]{11}" }, flags = { 0 })
public class WrzutaPl extends PluginForHost {

    private String filetype = null;
    private String filename = null;

    public WrzutaPl(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wrzuta.pl/regulamin/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        boolean addext = true;
        String fileId = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/" + filetype + "/([^/]*)").getMatch(0);
        String host = new Regex(downloadLink.getDownloadURL(), "https?://(.*?)/").getMatch(0);
        if (fileId == null || filetype == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String linkUrl = null;
        br.setFollowRedirects(true);
        if (filetype.equalsIgnoreCase("audio")) {

            String wrzutaPlayerSource = br.getRegex("window\\['__wrzuta_player_src'\\] = (.*) \\+ '\\?autoplay=true';").getMatch(0);

            String xmlAudioPage = "http://" + host + "/xml/kontent/" + fileId + "/wrzuta.pl/sa/" + new Random().nextInt(100000);
            br.getPage(xmlAudioPage);
            linkUrl = br.getRegex("<fileId><\\!\\[CDATA\\[(http://.*?)\\]\\]></fileId>").getMatch(0);
            addext = false;
            if (linkUrl == null) {
                if (wrzutaPlayerSource == null) {
                    // try with simple player
                    br.getPage("http://" + host + "/u/" + fileId);
                    String getVar = br.getRegex("context\\.setOption\\(options, 'flashSrc', encodeURIComponent\\((.*)\\)\\);").getMatch(0);
                    if (getVar != null) {
                        // probably better Regex is needed, less greedy
                        String match = br.getRegex("(var " + getVar + ".*;)").getMatch(0);
                        match = match.substring(0, match.indexOf(";"));
                        linkUrl = getURLFromVar(match);

                    } else {
                        // new method - get linkurl directly from embeded url based on username and fileid
                        // http://vengodelmar007.wrzuta.pl/audio/0psjP9Mz9Vz/ricchi_e_poveri_-_voulez_vous_dancer
                        // Json description is at:
                        // http://www.wrzuta.pl/npp/embed/vengodelmar007/0psjP9Mz9Vz?login=vengodelmar007&key=0psjP9Mz9Vz
                        final String userName = new Regex(downloadLink.getDownloadURL(), "https?://([^<>\"]+)\\.wrzuta\\.pl").getMatch(0);
                        final String embedURL = "http://www.wrzuta.pl/npp/embed/" + userName + "/" + fileId + "?login=" + userName + "&key=" + fileId;
                        br.getPage(embedURL);
                        linkUrl = PluginJSonUtils.getJsonValue(br, "url");
                    }

                    logger.info(linkUrl);
                } else {
                    // try to get final link via wrzuta player
                    String[][] matches = new Regex(wrzutaPlayerSource, "\"[a-zA-Z:/\\.0-9]*\"").getMatches();
                    String playerUrl = concatenate(matches);
                    br.getPage(playerUrl);

                    String flashSrcUrl = br.getRegex("var __flashSrcUrl = (.*)var __htmlSrcUrl ").getMatch(0);
                    if (flashSrcUrl == null) {
                        String encodeURIComponent = br.getRegex("'flashSrc', encodeURIComponent\\((.*?)\\)").getMatch(0);

                        flashSrcUrl = br.getRegex("var " + encodeURIComponent + " = (.*?);").getMatch(0);
                    }
                    matches = new Regex(flashSrcUrl, "\"[a-zA-z\\d:/\\.\\?=%&]*\"").getMatches();

                    linkUrl = concatenate(matches);
                }
            }
        } else if (filetype.equalsIgnoreCase("film")) {
            String xmlFilmPage = "http://" + host + "/xml/kontent/" + fileId + "/wrztua.pl/sa/" + new Random().nextInt(100000);
            br.getPage(xmlFilmPage);
            linkUrl = br.getRegex("<fileId><\\!\\[CDATA\\[(http://.*?)\\]\\]></fileId>").getMatch(0);
            addext = false;
        } else if (filetype.equalsIgnoreCase("obraz")) {
            linkUrl = br.getRegex("<img id=\"image\" src=\"(.*?)\"").getMatch(0);
            if (linkUrl == null) {
                linkUrl = downloadLink.getDownloadURL().replaceFirst("obraz", "sr/f");
            }
        }
        if (linkUrl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setDebug(true);
        br.setFollowRedirects(true);
        // set one chunk as film and audio based links will reset soon as second
        // chunk is connected.
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkUrl, true, 1);

        URLConnectionAdapter con = dl.getConnection();
        if (con.getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (downloadLink.getFinalFileName() == null) {
            if (!con.getContentType().equalsIgnoreCase("unknown") && addext != false) {
                if (con.getContentType().contains("mpeg3") || con.getContentType().contains("audio/mpeg")) {
                    downloadLink.setFinalFileName(filename.trim() + ".mp3");
                } else if (con.getContentType().contains("flv")) {
                    downloadLink.setFinalFileName(filename.trim() + ".flv");
                } else if (con.getContentType().contains("png")) {
                    downloadLink.setFinalFileName(filename.trim() + ".png");
                } else if (con.getContentType().contains("gif")) {
                    downloadLink.setFinalFileName(filename.trim() + ".gif");
                } else if (con.getContentType().contains("application/zip")) {
                    downloadLink.setFinalFileName(filename.trim() + ".zip");
                } else if (con.getContentType().contains("audio/mid")) {
                    downloadLink.setFinalFileName(filename.trim() + ".mid");
                } else if (con.getContentType().contains("application/pdf")) {
                    downloadLink.setFinalFileName(filename.trim() + ".pdf");
                } else if (con.getContentType().contains("application/rtf")) {
                    downloadLink.setFinalFileName(filename.trim() + ".rtf");
                } else if (con.getContentType().contains("application/msword")) {
                    downloadLink.setFinalFileName(filename.trim() + ".doc");
                } else if (con.getContentType().contains("jpg") || con.getContentType().contains("jpeg")) {
                    downloadLink.setFinalFileName(filename.trim() + ".jpg");
                } else if (con.getContentType().contains("bmp") || con.getContentType().contains("bitmap")) {
                    downloadLink.setFinalFileName(filename.trim() + ".bmp");
                } else {
                    logger.info("Unknown filetype: " + con.getContentType() + ", cannot determine file extension...");
                }
            }
        }
        dl.startDownload();
    }

    private String concatenate(String[][] m) {
        String concatenatedString = "";
        for (String s[] : m) {

            concatenatedString = concatenatedString.concat(s[0]);
        }
        concatenatedString = concatenatedString.replace("\"", "");
        return concatenatedString;
    }

    private String getURLFromVar(String m) {
        String URL = "";
        int start = -1;
        int end = -1;
        do {
            start = m.indexOf("\"", end + 1);
            if (start != -1) {
                end = m.indexOf("\"", start + 1);
                URL = URL + m.substring(start + 1, end);
            }

        } while (start != -1);

        return URL;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">Nie odnaleziono pliku\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = (Encoding.htmlDecode(br.getRegex("<h1 class=\"header\">(.*?)</h1>").getMatch(0)));
        if (filename == null) {
            filename = (Encoding.htmlDecode(br.getRegex("<meta name=\"title\" content=\"(.*?)\" />").getMatch(0)));
        }
        if (filename == null) {
            filename = (Encoding.htmlDecode(br.getRegex("div class=\"file-title\">.*?<h1>(.*?)</h1>").getMatch(0)));
        }
        if (filename == null) {
            filename = br.getRegex("<meta content=\"(.*?)\" name=\"medium\">.+<meta content=\"(.*?)\" name=\"title\">").getMatch(1);
        }
        String filesize = br.getRegex("Rozmiar: <strong>(.*?)</strong>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<span id=\"file_info_size\">[\t\n\r ]+<strong>(.*?)</strong>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filetype = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/([^/]*)").getMatch(0);
        if (filesize != null) {
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        }
        if (downloadLink.getIntegerProperty("nameextra", -1) != -1) {
            filename = filename + "_" + downloadLink.getIntegerProperty("nameextra", -1);
        }
        // Set the ending if the file doesn't have it but don't set it as a
        // final filename as it could be wrong!
        if (downloadLink.getDownloadURL().contains("/audio/") && !filename.contains(".mp3")) {
            downloadLink.setName(Encoding.htmlDecode(filename.trim() + ".mp3"));
        } else {
            downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}