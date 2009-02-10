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

package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

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
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Nie odnaleziono pliku.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = (Encoding.htmlDecode(br.getRegex(Pattern.compile("anie</h3><h2>(.*?)</h2><div", Pattern.CASE_INSENSITIVE)).getMatch(0)));
        String filesize = br.getRegex(Pattern.compile("Rozmiar: <strong>(.*?)</strong>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filetype = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/([^/]*)").getMatch(0);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        if (downloadLink.getIntegerProperty("nameextra", -1) != -1) {
            filename = filename + "_" + downloadLink.getIntegerProperty("nameextra", -1);
            downloadLink.setName(filename.trim());
        } else
            downloadLink.setName(filename.trim());
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        String fileid = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/" + filetype + "/([^/]*)").getMatch(0);
        if (fileid == null || filetype == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String linkurl = null;
        if (filetype.equalsIgnoreCase("audio")) {
            linkurl = "http://wrzuta.pl/aud/file/" + fileid + "/";
        }
        if (filetype.equalsIgnoreCase("film")) {
            linkurl = "http://wrzuta.pl/vid/file/" + fileid + "/";
        }
        if (filetype.equalsIgnoreCase("obraz")) {
            linkurl = "http://wrzuta.pl/img/file/" + fileid + "/";
        }
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setDebug(true);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, linkurl);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.getContentType().equalsIgnoreCase("unknown")) {
            if (con.getContentType().contains("mpeg3")) {
                downloadLink.setFinalFileName(filename.trim() + ".mp3");
            } else if (con.getContentType().contains("flv")) {
                downloadLink.setFinalFileName(filename.trim() + ".flv");
            } else if (con.getContentType().contains("png")) {
                downloadLink.setFinalFileName(filename.trim() + ".png");
            } else if (con.getContentType().contains("jpg") || con.getContentType().contains("jpeg")) {
                downloadLink.setFinalFileName(filename.trim() + ".jpg");
            } else if (con.getContentType().contains("bmp") || con.getContentType().contains("bitmap")) {
                downloadLink.setFinalFileName(filename.trim() + ".bmp");
            } else {
                logger.info("Unknown filetype, cannot determine file extension...");
            }
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}