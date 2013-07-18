//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youjizz.com" }, urls = { "http://(www\\.)?youjizz\\.com/videos/(embed/\\d+|.*?\\-\\d+\\.html)" }, flags = { 0 })
public class YouJizzCom extends PluginForHost {

    private String DLLINK = null;

    public YouJizzCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.youjizz.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().contains("/embed/")) {
            link.setUrlDownload("http://www.youjizz.com/videos/x-" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + ".html");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        // if (!br.containsHTML("flvPlayer\\.swf")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (filename == null || filename.trim().length() == 0) filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (filename == null || filename.trim().length() == 0) filename = br.getRegex("title1\">(.*?)</").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.trim();
        final String embed = br.getRegex("src=\\'(http://www.youjizz.com/videos/embed/[0-9]+)\\'").getMatch(0);
        if (embed == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.getPage(embed);
        DLLINK = br.getRegex("addVariable\\(\"file\",.*?\"(http://.*?\\.flv(\\?.*?)?)\"").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("\"(http://(mediax|cdn[a-z]\\.videos)\\.youjizz\\.com/[A-Z0-9]+\\.flv(\\?.*?)?)\"").getMatch(0);
            if (DLLINK == null) {
                String playlist = br.getRegex("so\\.addVariable\\(\"playlist\", \"(https?://(www\\.)?youjizz\\.com/playlist\\.php\\?id=\\d+)").getMatch(0);
                if (playlist == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                Browser br2 = br.cloneBrowser();
                br2.getPage(playlist);
                // multiple qualities (low|med|high) grab highest for now, decrypter will be needed for others.
                DLLINK = br2.getRegex("<level bitrate=\"\\d+\" file=\"(https?://(\\w+\\.){1,}youjizz\\.com/[^\"]+)\" ?></level>[\r\n\t ]+</levels>").getMatch(0);
                if (DLLINK != null) DLLINK = DLLINK.replace("%252", "%2");
            }
        }
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                String ext = getFileNameFromHeader(con).substring(getFileNameFromHeader(con).lastIndexOf("."));
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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