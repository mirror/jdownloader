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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class UpFileCom extends PluginForHost {

    public UpFileCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://up-file.com/page/terms.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://up-file.com", "lang", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1><span[^>]*>(.*?)</span>").getMatch(0);
        String filesize = br.getRegex("</span>\\s+<span[^>]*>\\[\\s(.*?)\\s\\]").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        if (br.containsHTML("Wait for your turn")) {
            String waittime = br.getRegex("your\\sturn&nbsp;&nbsp;<span\\sid=\"errt\"[^>]*>(.*?)</span>").getMatch(0);
            int waittimen = 0;
            waittimen = Integer.valueOf(waittime).intValue();
            if (waittimen == 0) waittimen = 60;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittimen * 1001l);
        } else if (br.containsHTML("Downloading is in process from your IP-Address")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1001l);
        } else {
            Form dlf = br.getFormbyProperty("id", "Premium");
            if (dlf == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dl = br.openDownload(downloadLink, dlf, false, 1);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
        
    }
}