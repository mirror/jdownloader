//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imagevenue.com" }, urls = { "http://(www\\.)?img[0-9]+\\.imagevenue\\.com/img\\.php\\?(loc=[^&]+&)?image=.{4,300}" })
public class ImageVenueCom extends PluginForHost {
    public ImageVenueCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://imagevenue.com/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Offline links should also have nice filenames
        link.setName(new Regex(link.getDownloadURL(), "imagevenue\\.com/img\\.php\\?(.+)").getMatch(0));
        this.br.setAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        /* Error handling */
        if (br.containsHTML("This image does not exist on this server|<title>404 Not Found</title>|>The requested URL /img\\.php was not found on this server\\.<") || this.br.getHttpConnection().getResponseCode() == 500) {
            logger.warning("File offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        String finallink = br.getRegex("id=\"thepic\".*?SRC=\"(.*?)\"").getMatch(0);
        if (finallink == null) {
            if (br.containsHTML("tempval\\.focus\\(\\)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.warning("Could not find finallink reference");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String server = new Regex(link.getDownloadURL(), "(img[0-9]+\\.imagevenue\\.com/)").getMatch(0);
        finallink = "http://" + server + finallink;
        String ending = new Regex(finallink, "imagevenue\\.com.*?\\.(.{3,4}$)").getMatch(0);
        String filename0 = new Regex(finallink, "imagevenue\\.com/.*?/.*?/\\d+.*?_(.*?)($|\\..{2,4}$)").getMatch(0);
        if (ending != null && filename0 != null) {
            filename = filename0 + "." + ending;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        URLConnectionAdapter con = null;
        try {
            con = openConnection(this.br, finallink);
            if (!con.isOK()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            long size = con.getLongContentLength();
            link.setDownloadSize(Long.valueOf(size));
            link.setName(filename.trim());
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        String finallink = br.getRegex("id=\"thepic\".*?SRC=\"(.*?)\"").getMatch(0);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String server = new Regex(downloadLink.getDownloadURL(), "(img[0-9]+\\.imagevenue\\.com/)").getMatch(0);
        finallink = "http://" + server + finallink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        dl.startDownload();
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}