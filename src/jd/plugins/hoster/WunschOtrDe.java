//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wunsch-otr.de" }, urls = { "http://(www\\.)?wunsch\\-otr\\.de/(\\?file=|index\\.php\\?mode=download\\&id=\\d+\\&name=)[^<>\"\\']+\\.otrkey" }, flags = { 0 })
public class WunschOtrDe extends PluginForHost {

    public WunschOtrDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://wunsch-otr.de";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("/index\\.php\\?mode=download\\&id=\\d+\\&name=", "/\\?file="));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Download wurde nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filesize = br.getRegex(">Gr\\&ouml;\\&szlig;e:</b></td><td>([^<>\"\\']+)</td>").getMatch(0);
        link.setName(Encoding.htmlDecode(new Regex(link.getDownloadURL(), "wunsch\\-otr\\.de/\\?file=(.+)").getMatch(0)));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = downloadLink.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            final String server = br.getRegex("name=\"warteschlange_server\" value=\"([^<>\"\\']+)\"").getMatch(0);
            br.postPage(downloadLink.getDownloadURL(), "warteschlange_name=" + Encoding.urlEncode(downloadLink.getName()) + "&warteschlange_server=" + Encoding.urlEncode(server) + "&warteschlange_check.x=" + new Random().nextInt(10) + "&warteschlange_check.y=" + new Random().nextInt(10));
            br.getPage("http://wunsch-otr.de/warteschlange.inc.php?frame=ja&dontplay=");
            for (int i = 0; i <= 400; i++) {
                /**
                 * Weil auch alle anderen Downloads hier aufgelistet sind müssen wir sichergehen, dass wir den richtigen Link bekommen
                 */
                dllink = br.getRegex("title=\"Du kannst nun Downloaden\" border=\"0\" /></a></td> <td>~\\d+</td> <td><a href=\"(http://wunsch\\-otr\\.de/\\?file=" + downloadLink.getName() + ")\"").getMatch(0);
                if (dllink == null) {
                    final String position = br.getRegex("title=\"Noch in der Warteschlange\" border=\"0\" /></td> <td>~(\\d+)</td>").getMatch(0);
                    if (position != null) {
                        logger.info("Warteschlange Position: " + position);
                        downloadLink.getLinkStatus().setStatusText("Warteschlange Position: " + position);
                    }
                    sleep(20 * 1000l, downloadLink);
                    br.getPage("http://wunsch-otr.de/warteschlange.inc.php?frame=ja&dontplay=nein");
                    continue;
                }
                break;
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(dllink);
            final Form dlForm = br.getFormbyProperty("id", "dl_form_");
            if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dlForm.put("show_link", "ja");
            dlForm.put("download%5BDownload+starten%5D.x=", Integer.toString(new Random().nextInt(10)));
            dlForm.put("download%5BDownload+starten%5D.y=", Integer.toString(new Random().nextInt(10)));
            br.submitForm(dlForm);
            dllink = br.getRegex("<div id=\"content_content_text\"><center><input type=\"text\" value=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://dl\\-stor\\-\\d+\\.wunsch\\-otr\\.de/files/[a-z0-9]+/[a-z0-9]+/otr/" + downloadLink.getName() + ")\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            /** Das sollte unter normalen Bedingungen nie passieren */
            if (br.containsHTML(">403 \\- Forbidden<")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Zu viele gleichzeitige Downloads", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}