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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class FileFactoryPl extends PluginForHost {


    public FileFactoryPl(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filefactory.pl/tos.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Error 404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<b>(.*?)</b><br><br>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex("Filesize:&nbsp;(.*?) \\|").getMatch(0);
        if (filename == null || filesize.equalsIgnoreCase("0.00 KB")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4390 $");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        String linkurl =  br.getRegex("<a href=\\\\'(.*?)\\\\' style=\\\\'font-size:14px;\\\\'>DOWNLOAD FILE</a>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        // this.sleep(60000, downloadLink); // uncomment when they find a better way to force wait time
        dl = br.openDownload(downloadLink, linkurl);
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