//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class Zippysharecom extends PluginForHost {

 
    private String url;

    public Zippysharecom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zippyshare.com/terms.html";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException {
     this.setBrowserExclusive();
            String url = downloadLink.getDownloadURL();
            for (int i = 1; i < 3; i++) {
          
                br.getPage(url);
                if (!br.containsHTML("File does not exist")) {
                    downloadLink.setName(Encoding.htmlDecode(br.getRegex(Pattern.compile("<strong>Name: </strong>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(0)));
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(br.getRegex( Pattern.compile("<strong>Size: </strong>(.*?)MB</font>", Pattern.CASE_INSENSITIVE)).getMatch(0).replaceAll(",", "\\.")) * 1024 * 1024));
                    return true;
                }
                Thread.sleep(250);
            }
     return true;
    }



    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        /* Link holen */
        String linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("downloadlink = unescape\\(\\'(.*?)\\'\\);", Pattern.CASE_INSENSITIVE)).getMatch(0));
        /* Datei herunterladen */
       dl=RAFDownload.download(downloadLink, br.createRequest(linkurl));

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