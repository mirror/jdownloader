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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class Cocosharecc extends PluginForHost {

    public Cocosharecc(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.cocoshare.cc/imprint";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("Download startet automatisch")) {
                downloadLink.setName(br.getRegex("<h1>(.*?)</h1>").getMatch(0));
                downloadLink.setDownloadSize(Regex.getSize(br.getRegex("Dateigr&ouml;sse:&nbsp;[&nbsp;]*(.*?)<br").getMatch(0).replaceAll("\\.", "")));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(false);

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        /* Warten */
        String waittime = br.getRegex("var num_timeout = (\\d+);").getMatch(0);
        if (waittime != null) {
            sleep(Integer.parseInt(waittime) * 1000, downloadLink);
        }

        /* DownloadLink holen */
        br.getPage("http://www.cocoshare.cc" + br.getRegex("<meta http-equiv=\"refresh\" content=\"\\d+; URL=(.*?)\"").getMatch(0));
        String downloadURL = br.getRedirectLocation();
        if (downloadURL == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        dl = br.openDownload(downloadLink, downloadURL);
        /* DownloadLimit? */
        if (dl.getRequest().getLocation() != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
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
