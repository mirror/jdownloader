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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sendspace.pl" }, urls = { "http://[\\w\\.]*?sendspace.pl/file/[\\w]+/?" }, flags = { 0 })
public class SendSpacePl extends PluginForHost {

    public SendSpacePl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.sendspace.pl/rules/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<div class=\"info\"><a href=\"http://www.sendspace.pl/download/.*?\" class=\"black\"><b>(.*?)</b></a></div>").getMatch(0);
        String filesize = br.getRegex(Pattern.compile("<div class=\"text\"><span class=\"black3\">Rozmiar pliku:</span></div>.*?<div class=\"info\"><span class=\"blue4\">(.*?)</span></div>", Pattern.DOTALL)).getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        String dlLink = br.getRegex("<div class=\"info\"><a href=\"(http://www.sendspace.pl/download/.*?)\" class=\"black\"><b>.*?</b></a></div>").getMatch(0);
        if (dlLink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();

            br.getPage(dlLink);
            if (br.containsHTML("id\\=\"countdown\"")) {
                Regex time = br.getRegex("<b id=\"countdown\">(.*?) godziny (.*?) minut (.*?) sekund</b>");
                int hours = Integer.parseInt(time.getMatch(0));
                int minutes = Integer.parseInt(time.getMatch(1));
                int seconds = Integer.parseInt(time.getMatch(2));

                long waitTime = 1000l * (seconds + (minutes * 60) + (hours * 3600));

                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, waitTime);
            }

            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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