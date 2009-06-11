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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;

public class MilleDriveCom extends PluginForHost {

    public MilleDriveCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.setStartIntervall(5000l);
    }

    //@Override
    public String getAGBLink() {
        return "http://milledrive.com/terms_of_service/";
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String firstlink = downloadLink.getDownloadURL();
        br.getPage(firstlink);
        if (!br.containsHTML("<head>")) 
        {
            this.sleep(2000, downloadLink);
            br.getPage(firstlink);
        }
        if (br.containsHTML("URL does not exist") || br.containsHTML("404 not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("/wait_encode.png") || br.containsHTML("This video is still being encoded")) downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugin.hoster.milledrive.com.stillencoding","This video is still being encoded"));
        String filename, filesize;
        if (!firstlink.contains("/files/")) // for videos & music links
        {
            filename = br.getRegex("down.direct\"\\s+href=\"http://.*?milledrive.com/files/\\w+/\\d+/(.*?)\"").getMatch(0);
            filesize = br.getRegex("Size:</span>\\s(.*?)\\s</span>").getMatch(0);
            filesize = filesize.replaceFirst("bytes", "B");
            if (filename == null) filename = br.getRegex("<title>(.*?) - Milledrive</title>").getMatch(0);
        } else {
            filename = br.getRegex("id=\"free-down\" action=\".*milledrive.com/files/\\d+/(.*?)\"").getMatch(0);
            filesize = br.getRegex("\\|\\s+<span style=[^>]*>(.*?)</span>").getMatch(0);
        }
        //System.out.println(filename+" "+filesize);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("/wait_encode.png") || br.containsHTML("This video is still being encoded")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,JDLocale.L("plugin.hoster.milledrive.com.stillencoding","This video is still being encoded"),15*60*1000);
        String directlink = br.getRegex("file:\"(.*?)\"").getMatch(0);
        if (directlink == null) {
            String firstlink = downloadLink.getDownloadURL();
            if (!firstlink.contains("/files/")) {
                firstlink = br.getRegex("id=\"down-direct\"\\s+href=\"(.*?)\"").getMatch(0);
                if (firstlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                br.getPage(firstlink);
            }
            Form down1 = br.getFormbyProperty("id", "free-down");
            if (down1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            this.sleep(30001, downloadLink);
            br.submitForm(down1);
            Form down2 = br.getFormbyProperty("id", "free-down");
            if (br.containsHTML("The requested URL does not exist")) throw new PluginException(LinkStatus.ERROR_RETRY);
            String downurl = br.getRegex("name=\"down-url\" value=\"(.*?)\"").getMatch(0);
            String ticket = br.getRegex("name=\"ticket\" value=\"(.*?)\"").getMatch(0);
            if (downurl == null || ticket == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dl = br.openDownload(downloadLink, down2, true, 1);

        } else {
            String finalfilename = downloadLink.getName();
            dl = br.openDownload(downloadLink, directlink, true, 1);
            downloadLink.setFinalFileName(finalfilename);
        }
        dl.startDownload();
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
        
    }
}