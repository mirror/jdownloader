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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "archiv.to" }, urls = { "http://(www\\.)?archiv\\.to/((\\?Module\\=Details\\&HashID\\=|GET/)FILE[A-Z0-9]+|view/divx/[a-z0-9]+)" }, flags = { 0 })
public class ArchivTo extends PluginForHost {

    public ArchivTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://archiv.to/imprint";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(The desired file could not be found|Maybe owner deleted it or check your Hash again|file not found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Originaldatei</td>.*?<td class=.*?>: <a href=\".*?>(.*?)</a>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("style=\"color:black;text-decoration:none;font-size:14px;font-weight:bold\">(.*?)</a>").getMatch(0);
            if (filename == null) {
                filename = new Regex(downloadLink.getDownloadURL(), ".*?FILE([A-Z0-9]+)").getMatch(0);
                if (filename != null) filename += ".flv";
            }
        }
        String filesize = br.getRegex(">Dateigr.+e</td>.*?<td class=.*?>:(.*?)\\(").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("\\(~(.*?)\\)").getMatch(0);
            if (filesize == null) filesize = br.getRegex("<span style=\"font-size:14px;font-weight:bold\">(.*?)</span>").getMatch(0);
        }
        String md5hash = br.getRegex(">MD5-Hash</td>.*?<td class=.*?>:(.*?)<").getMatch(0);
        if (md5hash == null) md5hash = br.getRegex("<span style=\"font-family:monospace;font-size:14px;\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(filename);
        if (md5hash != null) downloadLink.setMD5Hash(md5hash.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".").replace("Megabytes", "Mb")));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(downloadLink);
        br.setDebug(true);
        String dllink = br.getRegex("autoplay=\"true\" custommode=\"none\" src=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://divx\\d+\\.archiv\\.to/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("Seems like the finallink doesn't go to a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
