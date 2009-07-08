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
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=2, names = { "archiv.to"}, urls ={ "http://[\\w\\.]*?archiv\\.to/\\?Module\\=Details&HashID\\=.*"}, flags = {0})
public class ArchivTo extends PluginForHost {

    public ArchivTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://archiv.to/?Module=Policy";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        try {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
            String page = br.getPage(downloadLink.getDownloadURL());

            downloadLink.setName(new Regex(page, "<a href=\".*?archiv.*?\" style=\"Color.*?\">(.*?)</a></td></tr>").getMatch(0));
            downloadLink.setMD5Hash(new Regex(page, "<td width=\"23%\">MD5 Code</td>\\s*<td width=\"77%\">: ([0-9a-z]*?)</td>").getMatch(0));
            downloadLink.setDownloadSize(Long.parseLong(new Regex(page, "<td width=.*?>Dateigröße</td>\\s*<td width=.*?>: (\\d+?) Bytes \\(.*\\)</td>").getMatch(0)));

            return AvailableStatus.TRUE;
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    // @Override
    /*  /* /* public String getVersion() {
        return getVersion("$Revision$");
    } */  

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(downloadLink);

        br.getPage(downloadLink.getDownloadURL());
        String link = br.getRegex("<a href=\"http://ww\\.archiv\\.to/(Get.*?)\" style=").getMatch(0);
        String dlLink = "http://archiv.to/" + Encoding.htmlDecode(link);

        dl = br.openDownload(downloadLink, dlLink);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
