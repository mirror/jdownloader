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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class IFileIt extends PluginForHost {

    public IFileIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://ifile.it/tos";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)\\s+. Ticket").getMatch(0);
        String filesize = br.getRegex("nbsp;\\s+\\((.*?)\\)\\s").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        String dlLink;
        String previousLink = downloadLink.getStringProperty("directLink", null);
        if (previousLink == null) {
            String it = br.getRegex("file_key\" value=\"(.*?)\"").getMatch(0);
            Browser br2 = br.cloneBrowser();
            br2.getPage("http://ifile.it/download:dl_request?it=" + it + ",type=na,esn=1");
            if (br2.containsHTML("show_captcha")) {
                String code = getCaptchaCode("http://ifile.it/download:captcha?" + Math.random(), downloadLink);
                br2 = br.cloneBrowser();
                br2.getPage("http://ifile.it/download:dl_request?it=" + it + ",type=simple,esn=1,0d149=" + code + ",0d149x=0");
                if (br2.containsHTML("retry")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            br.getPage("http://ifile.it/dl");
            dlLink = br.getRegex("var __url\\s+=\\s+'(http://.*?)'").getMatch(0);
            if (dlLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dlLink = dlLink.replaceAll(" ", "%20");
            downloadLink.setProperty("directLink", dlLink);

        } else {
            dlLink = previousLink;
        }
        br.setDebug(true);
        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, dlLink, true, -2);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK()) {
            if (previousLink != null) {
                downloadLink.setProperty("directLink", null);
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("directLink", null);
    }
}
