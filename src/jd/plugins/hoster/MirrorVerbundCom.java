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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mirror-verbund.com" }, urls = { "http://(www\\.)?mirror\\-verbund\\.com/\\?file=[^<>\"\\']+\\.otrkey" }, flags = { 0 })
public class MirrorVerbundCom extends PluginForHost {

    public MirrorVerbundCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mirror-verbund.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = new Regex(link.getDownloadURL(), "mirror\\-verbund\\.com/\\?file=([^<>\"\\']+.otrkey)").getMatch(0);
        filename = Encoding.htmlDecode(filename.trim());
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        final String continueLink = br.getRegex("style=\"display:none;\"><a href=\"javascript:void\\(\\);\" onClick=\"mvpop_dl\\(\\'(\\d+_[a-z0-9]+)\\'\\)").getMatch(0);
        if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://mirror-verbund.com/download_" + continueLink + ".html");
        if (br.containsHTML("(>Interner Fehler, dass otrkey ist auf keinem Server mehr oder der Server ist offline|Die URL wurde manipuliert)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        dllink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/dl_g/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            /**
             * Limit erreicht? Wir müssen keine 2 Stunden warten, 30 Minuten reichen
             */
            if (br.containsHTML("(>Du darfst als Gast max\\.|Mehr Infos auf der Startseite</b>)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
            String warte = br.getRegex("\\'(warte\\.php\\?co2=[^<>\"\\']+)\\'").getMatch(0);
            if (warte == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            warte = "http://www.mirror-verbund.com/" + warte.replace("warte.php?co2=", "warte_s.php?co2=");
            for (int i = 0; i <= 500; i++) {
                br.getPage(warte);
                if (br.containsHTML("(file\\.php\\?file=\\d+|>Fertig gewartet:</|\">Download starten</)")) break;
                String platz = br.getRegex("</td><td>~(\\d+)</td><td>").getMatch(0);
                if (platz != null) {
                    downloadLink.getLinkStatus().setStatusText("Platz in der warteschlange: " + platz);
                    logger.info("Platz in der warteschlange: " + platz);
                }
                sleep(45 * 1000l, downloadLink);
            }
            dllink = br.getRegex("\\'(file\\.php\\?file=\\d+\\&hash=[a-z0-9]+\\&co2=[^<>\"\\']+)\\'").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://mirror-verbund.com/" + dllink);
            dllink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/dl_g/[^<>\"\\']+)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("src=\"http://js\\.adscale\\.de/getads\\.js\"></script>[\t\n\r ]+<a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -8);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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