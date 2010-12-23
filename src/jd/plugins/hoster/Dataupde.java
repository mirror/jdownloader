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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dataup.de", "dataup.to" }, urls = { "http://[\\w\\.]*?dataup\\.de/\\d+/(.*)", "http://[\\w\\.]*?dataup\\.to/\\d+/." }, flags = { 0, 0 })
public class Dataupde extends PluginForHost {
    public Dataupde(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dataup.to/agb";
    }

    private static final String FILESIZEREGEX = "Gr\\&ouml;\\&szlig;e: (.*?)<br";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Die Datei wird gerade verarbeitet und steht in wenigen Minuten bereit\\!</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"box_header\"><h1>Download: (.*?)</h1></div>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("Name: (.*?)<br />").getMatch(0);
            if (filename == null) filename = br.getRegex("Stream: (.*?)</").getMatch(0);
        }
        // Gr&ouml;&szlig;e: 174,61MB<br
        String filesize = br.getRegex(FILESIZEREGEX).getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        requestFileInformation(downloadLink);
        // Skips the waittime
        br.postPage(downloadLink.getDownloadURL(), "submitter=Weiter+zum+Stream");
        // Often the download can't be started because of timeouts but at least
        // we can find the filesize here
        String filesize = br.getRegex(FILESIZEREGEX).getMatch(0);
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        String dllink = br.getRegex("class=\"download\" href=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://q\\d+\\.dataup\\.to:\\d+/download/\\d+/[a-z0-9]+/\\d+/.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("type=\"video/divx\" src=\"(http://.*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getLongContentLength() == 0) {
            dl.getConnection().disconnect();
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        /* DownloadLimit? */
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public void correctDownloadLink(DownloadLink downloadLink) {
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replace(".de/", ".to/"));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
