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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "partage-facile.com" }, urls = { "http://[\\w\\.]*?partage-facile\\.com/[0-9A-Z]+/.+" }, flags = { 0 })
public class PartageFacileCom extends PluginForHost {

    public PartageFacileCom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.partage-facile.com/cgu.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        this.setBrowserExclusive();
        // br.setCustomCharset("UTF-8");
        // br.setFollowRedirects(false);
        // Wrong links show the mainpage so here we check if we got the mainpage
        // or not
        if (br.containsHTML("title\">Envoyez vos fichiers maintenant</td>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex reg = br.getRegex("<br>Fichier : <b>(.*?)</b> \\((.*?)\\)<br>");
        String filesize = reg.getMatch(1);
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = reg.getMatch(0);
        String filesize2 = null;
        if (filesize.contains("Mo")) {
            filesize2 = filesize.replaceAll("Mo", "MB");
        }
        if (filesize.contains("Go")) {
            filesize2 = filesize.replaceAll("Go", "GB");
        }
        if (filesize.contains("Ko")) {
            filesize2 = filesize.replaceAll("Ko", "KB");
        }
        if (filesize.contains("oct")) {
            filesize2 = filesize.replaceAll("oct", "b");
        }

        if (filename == null || filesize2 == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize2));

        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form dlform0 = br.getForm(1);
        if (dlform0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.submitForm(dlform0);
        Form dlform1 = br.getForm(0);
        if (dlform1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlform1, true, -20);
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
