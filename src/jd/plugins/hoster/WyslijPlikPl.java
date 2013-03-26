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
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wyslijplik.pl" }, urls = { "http://[\\w\\.]*?wyslijplik\\.pl/download\\.php\\?sid=\\w{8}" }, flags = { 2 })
public class WyslijPlikPl extends PluginForHost {

    public WyslijPlikPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://wyslijplik.pl/tos.php";
    }

    /*
     * Note:
     * 
     * Simultan DL Limits are not implemented yet: - no limits for up to 250mb files - 5 300MB Downloads - 3 500MB Downloads - 1 1GB
     * Download
     * 
     * Well that's what i found in the faq - so the limits depend on the filesize Would also need more testlinks to bigger files...
     */
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String linkurl = br.getRegex("<a href='(http://\\w{2}\\.wyslijplik\\.pl/get\\.php\\?gid=\\w{8})'.*?</a>").getMatch(0);
        if (linkurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        /*
         * there won't be a error-message if dl does not exist, so check whether dl link is available
         */
        if (br.containsHTML("<a href='http://\\w{2}\\.wyslijplik\\.pl/get\\.php\\?gid=\\w{8}'.*?</a>")) {
            String filename = br.getRegex("<td width='230'><a href='.*?'  title=\"(.*?)\\.\"><b>").getMatch(0);
            String filesize = br.getRegex("<table class='showfiles'>.*?<td>(.*?)</td>").getMatch(0);
            if ((filename != null && filesize != null)) {
                downloadLink.setName(filename);
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}