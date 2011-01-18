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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourfilesender.com" }, urls = { "http://[\\w\\.]*?yourfilesender\\.com/v/\\d+/(.*?\\.html)" }, flags = { 0 })
public class YourFileSendercom extends PluginForHost {

    public YourFileSendercom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.yourfilesender.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File was deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>YourFileSender - (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("alt=\"Click here to download (.*?)\"").getMatch(0);
        }
        String filesize = br.getRegex("</strong> \\((.*?)\\)\\.<br").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replace(",", "");
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        if (br.containsHTML("<span>You have got max allowed download sessions from the same IP!</span>")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000L); }
        String link = Encoding.htmlDecode(new Regex(br, Pattern.compile("unescape\\('(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        /* Datei herunterladen */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, false, 1);
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
