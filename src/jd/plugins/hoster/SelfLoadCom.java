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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=1, names = { "self-load.com"}, urls ={ "http://[\\w\\.]*?self-load\\.com/\\d+/.+"}, flags = {0})
public class SelfLoadCom extends PluginForHost {
    // Info: Hoster übergibt weder korrekten Dateinamen
    // (selbst wählbar über die aufrufende Url)
    // noch wird eine Dateigröße angegeben!
    public SelfLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public String getAGBLink() {
        return "http://self-load.com/impressum.html";
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        br.getPage(downloadLink.getDownloadURL().replaceAll("self-load.com", "heiker.pro"));
        String filename = br.getRegex(Pattern.compile("<br>.*?<b>(.*?)</b>&nbsp;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    //@Override
    /* public String getVersion() {
        return getVersion("$Revision$");
    } */

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        form.setAction("/redirect.php"); // *FIXME: ohne / wird eine falsche url
        // in der Form.getAction berechnet!
        // hoster schuld oder Form fixen?
        dl = br.openDownload(downloadLink, form, false, 1);
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
