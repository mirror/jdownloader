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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fernsehkritik.tv" }, urls = { "http://fernsehkritik\\.tv/(jdownloaderfolge\\d+(\\-\\d)?\\.flv|inline\\-video/postecke\\.php\\?iframe=true\\&width=\\d+\\&height=\\d+\\&ep=\\d+)" }, flags = { 0 })
public class FernsehkritikTv extends PluginForHost {

    // Refactored on the 02.07.2011, Rev. 14521,
    // http://svn.jdownloader.org/projects/jd/repository/revisions/14521
    public FernsehkritikTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://fernsehkritik.tv/datenschutzbestimmungen/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String POSTECKE = "http://fernsehkritik\\.tv/inline\\-video/postecke\\.php\\?iframe=true\\&width=\\d+\\&height=\\d+\\&ep=\\d+";
    private String              DLLINK   = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getDownloadURL().matches(POSTECKE)) {
            final String episodenumber = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
            br.getPage("http://fernsehkritik.tv/folge-" + episodenumber + "/Start/");
            final String date = br.getRegex("var flattr_tle = \\'Fernsehkritik\\-TV Folge \\d+ vom(.*?)\\'").getMatch(0);
            if (date == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(downloadLink.getDownloadURL());
            DLLINK = br.getRegex("playlist = \\[ \\{ url: \\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (DLLINK == null) DLLINK = br.getRegex("\\'(http://dl\\d+\\.fernsehkritik\\.tv/postecke/postecke\\d+\\.flv)\\'").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setFinalFileName("Fernsehkritik-TV Postecke " + episodenumber + " vom " + date + ".flv");
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (!downloadLink.getDownloadURL().matches(POSTECKE)) {
            br.getPage("http://fernsehkritik.tv/js/directme.php?file=" + new Regex(downloadLink.getDownloadURL(), "fernsehkritik\\.tv/jdownloaderfolge(.+)").getMatch(0));
            DLLINK = br.getRedirectLocation();
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // More chunks work but download will stop at random point then
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}