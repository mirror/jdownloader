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
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anyfiles.net" }, urls = { "http://[\\w\\.]*?anyfiles.net/download/[a-z|0-9]+/.+\\.html" }, flags = { 0 })
public class AnyFilesNet extends PluginForHost {

    public AnyFilesNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);

        String uid1 = br.getRegex("[^ ]{18,}<input type='hidden' name='uid' value='(.*?)' />").getMatch(0);
        String uid2 = br.getRegex("<input type='hidden' name='uid2' value='(.*?)' /></th></table></form>").getMatch(0);
        String hcode = br.getRegex("type='hidden' name='hcode' value='(.*?)'>").getMatch(0);
        String ip = br.getRegex("<input type='hidden' name='ip' value='(.*?)'>").getMatch(0);

        Form form = br.getFormbyProperty("name", "Premium");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        form.put("uid1", uid1);
        form.put("uid2", uid2);
        form.put("hcode", hcode);
        form.put("frameset", "Die+Dateien+herunterladen");
        form.put("ip", ip);
        form.put("fix", "1");

        br.submitForm(form);
        String dllink = br.getRegex("<frame src=\"/tmpl/tmpl_frame_top.php\\?link=(.*?)\" name=\"topFrame\"").getMatch(0);

        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public String getAGBLink() {
        return "http://www.anyfiles.net/page/terms.php";
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.postPage("http://anyfiles.net/", "de.x=10&de.y=9&vote_cr=de");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("was not found on this server.") || br.containsHTML("The requested file was not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Der Besitzer der Datei begrenzte Anzahl von kostenlosen Downloads pro Tag")) {
            logger.warning(JDL.L("plugins.host.anyfilesnet.premiumonly", "Anyfiles.net: The owner of the file limited the number of free downloads per day, you will have to buy a premium account or wait till you can download the file as free user again!"));
            throw new PluginException(LinkStatus.ERROR_FATAL, "See log");
        }
        String filename = Encoding.htmlDecode(br.getRegex("DATEI:</td><td width=\"500\" valign=\"top\"><h2 style=\"margin-top:1px; margin-bottom:0px;\">(.*?)</h2></td></tr>").getMatch(0));
        String filesize = br.getRegex("SSE:</td><td width=\"500\" valign=\"top\">(.*?( Mb| b| Gb| Kb))  &nbsp;&nbsp;&nbsp").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
