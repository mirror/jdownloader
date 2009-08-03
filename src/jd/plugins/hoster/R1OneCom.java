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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "r1one.com" }, urls = { "http://[\\w\\.]*?r1one\\.com/download.php\\?file\\=\\w+" }, flags = { 0 })
public class R1OneCom extends PluginForHost {

    public R1OneCom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
        setBrowserExclusive();
    }

    @Override
    public String getAGBLink() {
        return "http://r1one.com/index.php?page=tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML("Invalid download link.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex info = br.getRegex(Pattern.compile("<h1>(.*?) - (.*?)</h1>"));
        String name = info.getMatch(0);
        String filesize = info.getMatch(1);
        if (name == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        if(br.containsHTML("You're trying to download again too soon!")) {
            long waittime = Long.parseLong(br.getRegex(Pattern.compile(">You're trying to download again too soon!  Wait (.*?) seconds.")).getMatch(0));
            sleep(waittime * 1000, downloadLink);
        }

        String downloadUrl = br.getRegex(Pattern.compile("<a href=\"(.*?)\">Download</a>")).getMatch(0);
        dl = br.openDownload(downloadLink, downloadUrl, true, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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