//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zshare.net" }, urls = { "http://[\\w\\.]*?zshare\\.net/(download|video|image|audio|flash)/.*" }, flags = { 0 })
public class ZShareNet extends PluginForHost {

    public ZShareNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.zshare.net/TOS.html";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (downloadLink.getDownloadURL().contains(".html")) {
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL());
            br.getPage(br.getRedirectLocation().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/download"));
        } else {
            br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/download"));
        }
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File Name:.*?<font color=\"#666666\".*?>(.*?)</font>").getMatch(0);
        String filesize = br.getRegex("File Size:.*?<font color=\"#666666\".*?>(.*?)</font>").getMatch(0);
        if (filename != null && filesize == null) filesize = br.getRegex("Image Size:.*?<font color=\"#666666\".*?>(.*?)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "")));

        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // Form abrufen
        Form download = br.getForm(0);
        String dlUrl = null;
        if (download != null) {
            // Formparameter setzen (zufällige Klickpositionen im Bild)
            download.put("imageField.x", (Math.random() * 160) + "");
            download.put("imageField.y", (Math.random() * 60) + "");
            download.put("imageField", null);
            // Form abschicken
            br.submitForm(download);
            String fnc = br.getRegex("var link_enc\\=new Array\\(\\'(.*?)\\'\\)").getMatch(0);
            fnc = fnc.replaceAll("\\'\\,\\'", "");
            dlUrl = fnc;
        } else {
            dlUrl = br.getRegex("<td bgcolor=\"#CCCCCC\">.*?<img src=\"(http://.*?.zshare.net/download/.*?)\"").getMatch(0);
            if (dlUrl == null) {
                dlUrl = br.getRegex("<td bgcolor=\"#CCCCCC\">.*?<img src=\"(.*?)\"").getMatch(0);
                if (!dlUrl.startsWith("/")) dlUrl = "/" + dlUrl;
            }
        }
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 1);

        // Möglicherweise serverfehler...
        if (!dl.getConnection().isContentDisposition() || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("404 - Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {

    }
}
