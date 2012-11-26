//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "magnovideo.com" }, urls = { "http://(www\\.)?magnovideo\\.com/[A-Z0-9]+" }, flags = { 0 })
public class MagnoVideoCom extends PluginForHost {

    public MagnoVideoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.magnovideo.com/pages.php?p=DMCA";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<title>File share metatitle \\- New MagnoVideo</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("property=\"og:title\" content=\"New MagnoVideo \\-([^<>\"]*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Z0-9]+)$").getMatch(0);
        br.setCookie(br.getHost(), "prepage", fid.concat("2"));
        br.getPage("/?v=" + fid);
        String path = br.getRegex("flv=http://[^/]+/([^<>\"]*?)\\&").getMatch(0);
        if (path == null) br.getPage("/?v=" + fid);
        path = br.getRegex("flv=http://[^/]+/([^<>\"]*?)\\&").getMatch(0);
        br.getPage("/player_config.php?mdid=" + fid);
        String dlHost = br.getRegex("<storage_path>(.*?)</storage_path>").getMatch(0);
        String burst = br.getRegex("<movie_burst>(\\d+)</movie_burst>").getMatch(0);
        if (path == null || dlHost == null || burst == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlHost + path + "?burst=" + burst, true, 1);
        fixFilename(downloadLink);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void fixFilename(final DownloadLink downloadLink) {
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        final String newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
        if (newExtension != null && !downloadLink.getName().endsWith(newExtension)) {
            final String oldExtension = downloadLink.getName().substring(downloadLink.getName().lastIndexOf("."));
            if (oldExtension != null)
                downloadLink.setFinalFileName(downloadLink.getName().replace(oldExtension, newExtension));
            else
                downloadLink.setFinalFileName(downloadLink.getName() + newExtension);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}