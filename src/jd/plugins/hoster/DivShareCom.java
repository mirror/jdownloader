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

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "divshare.com" }, urls = { "http://[\\w\\.]*?divshare.com/(download|image|direct)/[0-9]+-.+" }, flags = { 0 })
public class DivShareCom extends PluginForHost {

    public DivShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.divshare.com/page/terms";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        if (link.getDownloadURL().contains("direct")) {
            String dllink;
            dllink = br.getRedirectLocation();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            link.setFinalFileName(null);
            dl = br.openDownload(link, dllink, false, 1);
            dl.startDownload();

        } else {
            String infolink = link.getDownloadURL();
            br.getPage(infolink);
            // Check ob ein Passwort verlangt wird, wenn keins verlangt wird
            // wird der Link sofort geändert und geladen (unten ists dann infolink2)!
            if (br.containsHTML("This file is password protected.")) {
                for (int retry = 1; retry <= 5; retry++) {
                    Form form = br.getForm(0);
                    String pass = UserIO.getInstance().requestInputDialog("Password");
                    form.put("gallery_password", pass);
                    br.submitForm(form);
                    //wenn die Seite den text "..." beinhaltet (wenns dieselbe Seite wie vor der eingabe ist) ist das Password falsch
                    if (!br.containsHTML("This file is password protected.")) throw new PluginException(LinkStatus.ERROR_RETRY);
                    logger.warning("Wrong password!");
                }
            }
            String infolink2 = link.getDownloadURL().replaceAll("divshare.com/(download|image)", "divshare.com/download/launch");
            br.getPage(infolink2);
            String dllink;
            dllink = br.getRegex("refresh\" content=\"1; url=(.*?)\" />").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            link.setFinalFileName(null);
            dl = br.openDownload(link, dllink, false, 1);
            dl.startDownload();

        }

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        // handling für die links, die "direct" beinhalten (diese starten
        // sofort)
        if (downloadLink.getDownloadURL().contains("direct")) {
            if (br.containsHTML("Sorry, we couldn't find this file.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("This file is secured by Divshare.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        } else {
            // handling für die anderen Links (mit "download" bzw. "image" im
            // Link
            if (br.containsHTML("Sorry, we couldn't find this file.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("This file is secured by Divshare.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // alte Methode um an den filename der Downloadseite zu kommen
            // br.getRegex("<title>(.*?) - DivShare</title>").getMatch(0);
            String filename0 = downloadLink.getDownloadURL().replaceAll("divshare.com/(download|image)", "divshare.com/sharing");
            Browser br2 = br.cloneBrowser();
            br2.getPage(filename0);
            String filename = br2.getRegex("no-repeat left center;\">(.*?)</span>").getMatch(0);
            Regex reg = br.getRegex("<b>File Size:</b> (.*?) <span class=\"tiny\">(.*?)</span><br />");
            String filesize = reg.getMatch(0) + " " + reg.getMatch(1);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // parameter.setName(filename.trim());
            downloadLink.setName(filename.replaceAll("\\)|\\(", ""));
            downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}