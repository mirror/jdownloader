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

package jd.plugins.host;

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.JavaScript;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class ZShareNet extends PluginForHost {

    public ZShareNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zshare.net/TOS.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
            br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image"));
            String[] fileInfo = br.getRegex("File Name: .*?<font color=\".666666\">(.*?)</font>.*?Image Size: <font color=\".666666\">(.*?)</font></td>").getRow(0);
            downloadLink.setName(fileInfo[0]);
            downloadLink.setDownloadSize(Regex.getSize(fileInfo[1].replaceAll(",", "")));
            // Datei ist noch verfuegbar
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
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
        // Javascript für link
        String fnc = br.getRegex("(var link\\_enc\\=.*link\\_enc\\[i\\]\\;\\})").getMatch(0);
        // JS ausführen
        String link = new JavaScript(fnc).runJavaScript();
        // Link laden
        dl = br.openDownload(downloadLink, link, true, 1);
        // Möglicherweise serverfehler...
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
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
