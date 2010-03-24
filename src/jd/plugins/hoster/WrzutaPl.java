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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wrzuta.pl" }, urls = { "http://[\\w\\._-]*?wrzuta\\.pl/(audio|film|obraz)/\\w+.+" }, flags = { 0 })
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
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Nie odnaleziono pliku.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = (Encoding.htmlDecode(br.getRegex(Pattern.compile("anie</h3>\\s+<h2>(.*?)</h2>", Pattern.CASE_INSENSITIVE)).getMatch(0)));
        String filesize = br.getRegex(Pattern.compile("Rozmiar: <strong>(.*?)</strong>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filetype = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/([^/]*)").getMatch(0);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replace(",", ".")));
        if (downloadLink.getIntegerProperty("nameextra", -1) != -1) filename = filename + "_" + downloadLink.getIntegerProperty("nameextra", -1);
        // Set the ending if the file doesn't have it but don't set it as a
        // final filename as it could be wrong!
        if (downloadLink.getDownloadURL().contains("/audio/") && !filename.contains(".mp3"))
            downloadLink.setName(filename.trim() + ".mp3");
        else
            downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        boolean addext = true;
        String fileid = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/" + filetype + "/([^/]*)").getMatch(0);
        if (fileid == null || filetype == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String linkurl = null;
        if (filetype.equalsIgnoreCase("audio")) {
            linkurl = br.getRegex("wrzuta_flv=(.*?)&wrzuta_mini").getMatch(0);
            if (linkurl == null)
                linkurl = downloadLink.getDownloadURL().replaceFirst("audio", "sr/f");
            else
                addext = false;
        }
        if (filetype.equalsIgnoreCase("film")) {
            linkurl = br.getRegex("wrzuta_flv=(.*?)&wrzuta_mini").getMatch(0);
            if (linkurl == null)
                linkurl = downloadLink.getDownloadURL().replaceFirst("film", "sr/f");
            else
                addext = false;
        }
        if (filetype.equalsIgnoreCase("obraz")) {
            linkurl = br.getRegex("wrzuta_flv=(.*?)&wrzuta_mini").getMatch(0);
            if (linkurl == null)
                linkurl = downloadLink.getDownloadURL().replaceFirst("obraz", "sr/f");
            else
                addext = false;
        }
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setDebug(true);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl);
        URLConnectionAdapter con = dl.getConnection();
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
        System.out.println(con.getContentType());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
