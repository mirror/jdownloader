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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=1, names = { "zshare.net"}, urls ={ "http://[\\w\\.]*?zshare\\.net/(download|video|image|audio|flash)/.*"}, flags = {0})
public class ZShareNet extends PluginForHost {

    public ZShareNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.zshare.net/TOS.html";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image"));
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[] fileInfo = br.getRegex("File Name: .*?<font color=\".666666\">(.*?)</font>.*?Image Size: <font color=\".666666\">(.*?)</font>").getRow(0);
        if (fileInfo[0] == null || fileInfo[1] == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(fileInfo[0]);
        downloadLink.setDownloadSize(Regex.getSize(fileInfo[1].replaceAll(",", "")));
        // Datei ist noch verfuegbar
        return AvailableStatus.TRUE;
    }

    // @Override
    /* public String getVersion() {
        return getVersion("$Revision$");
    } */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());

        br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/download"));
        // Form abrufen
        Form download = br.getForm(0);
        // Formparameter setzen (zufällige Klickpositionen im Bild)
        download.put("imageField.x", (Math.random() * 160) + "");
        download.put("imageField.y", (Math.random() * 60) + "");
        download.put("imageField", null);
        // Form abschicken
        br.submitForm(download);
        String fnc = br.getRegex("var link_enc\\=new Array\\(\\'(.*?)\\'\\)").getMatch(0);
        fnc = fnc.replaceAll("\\'\\,\\'", "");
        dl = br.openDownload(downloadLink, fnc, true, 1);

        // Möglicherweise serverfehler...
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
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
        // TODO Auto-generated method stub

    }
}
