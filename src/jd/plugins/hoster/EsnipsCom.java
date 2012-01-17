//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "esnips.com" }, urls = { "http://(www\\.)?esnips\\.com/((ns)?doc/[a-z0-9\\-]+|displayimage\\.php\\?[\\w\\&\\=]+)" }, flags = { 0 })
public class EsnipsCom extends PluginForHost {

    public EsnipsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.esnips.com/index.php?file=minicms/cms&id=3";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws IOException {
        Browser br2 = new Browser();
        br2.setFollowRedirects(false);
        link.setUrlDownload(link.getDownloadURL().replace("esnips.com/nsdoc", "esnips.com/doc"));
        if (!new Regex(link.getDownloadURL(), "esnips\\.com/displayimage\\.php").matches()) {
            br2.getPage(link.getDownloadURL());
            final String newUrl = br2.getRedirectLocation();
            if (newUrl != null) link.setUrlDownload(newUrl);
        }
        if (!link.getDownloadURL().contains("&lang=english")) {
            link.setUrlDownload(link.getDownloadURL() + "&lang=english");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>Error \\- eSnips</title>|<h2>Error</h2>|>The selected album/file does not exist)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"fix\"></div>[\t\n\r ]+<div class=\"title_cover\">[\t\n\r ]+<h2>(.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>[^/<>\"]+ \\- (.*?) \\- eSnips</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String hash = br.getRegex("\"hash_c\":\"([a-z0-9]+)\"").getMatch(0);
        String dllink = br.getRegex("\"return dl_me\\(this\\);\" href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://ng\\-st\\.esnips\\.com/dl_serve\\.php\\?file_id=userfolders/esnips/central/[a-z0-9\\-/]+\\&ts=\\d+)\"").getMatch(0);
        if (dllink == null || hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink) + "&hash=" + hash;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /** Server sends us internal filenames, we want nice filenames */
        final String tempFname = getFileNameFromHeader(dl.getConnection());
        String ext = tempFname.substring(tempFname.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            downloadLink.setFinalFileName(tempFname);
        } else {
            downloadLink.setFinalFileName(downloadLink.getName() + ext);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}