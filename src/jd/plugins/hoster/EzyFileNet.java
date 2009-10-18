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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//ezyfile by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ezyfile.net" }, urls = { "http://[\\w\\.]*?ezyfile\\.net/[a-z|0-9]+/.+" }, flags = { 0 })
public class EzyFileNet extends PluginForHost {

    public EzyFileNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://ezyfile.net/tou.html";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.ezyfile.net", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file with this name")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Software error")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<font style=\"font-size:12px;\">You have requested <font color=\"red\">http://ezyfile.net/[a-z|0-9]+/(.*?)</font>").getMatch(0));
        String filesize = br.getRegex("</font> ((.*?))</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        // Form um auf free zu "klicken"
        Form DLForm0 = br.getForm(0);
        if (DLForm0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        DLForm0.remove("method_premium");
        br.submitForm(DLForm0);
        if (br.containsHTML("You're using all download slots for IP")) {
            int waittime = 3 * 60 * 1001;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        // Form um auf "Datei herunterladen" zu klicken
        Form DLForm1 = br.getFormbyProperty("name", "F1");
        if (DLForm1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String passCode = null;
        if (br.containsHTML("<br><b>Password:</b>")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            DLForm1.put("password", passCode);
        }
        br.submitForm(DLForm1);
        if (br.containsHTML("Wrong password")) {
            downloadLink.setProperty("pass", null);
            logger.warning("Wrong password!");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        String dllink = br.getRegex("<a href=\"(http://ezy[0-9]+.*)\">http://").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}