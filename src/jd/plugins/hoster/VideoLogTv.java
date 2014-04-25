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
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videolog.tv" }, urls = { "http://(www\\.)?videolog\\.tv/video(\\?|\\.php\\?id=)\\d+" }, flags = { 0 })
public class VideoLogTv extends PluginForHost {

    public VideoLogTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.videolog.tv/comunidade/videolog/?page_id=284";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            br.getPage(downloadLink.getDownloadURL());
        } catch (final BrowserException e) {
            if (br.getHttpConnection().getResponseCode() == 500) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw e;
        }
        if (br.getURL().equals("http://videolog.tv/index.php") || br.containsHTML("<title>Videolog \\| A maior comunidade de produtores de vídeo do Brasil</title>|>Esse vídeo que você procurou não foi encontrado|>Vídeo não encontrado<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Removed because of copyright stuff
        if (br.containsHTML(">Este v\\&iacute;deo foi bloqueado por desrespeitar o acordo de utiliza\\&ccedil;\\&atilde;o,")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<input type=\\'hidden\\' id=\"prop3\" value=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1 class=\"titulo\\-video\">(.*?) <g:plusone>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<title>(.*?) \\- Videolog</title>").getMatch(0);
                }
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename.trim());
        if (br.containsHTML(">V\\&iacute;deo para maiores de 18 anos")) {
            downloadLink.getLinkStatus().setStatusText("18+ video: Only available for registered users");
            downloadLink.setName(filename + ".mp4");
            return AvailableStatus.TRUE;
        }
        try {
            br.getPage("http://embed-video.videolog.tv/api/player/video.php?format=xml&id=" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
        } catch (final BrowserException e) {
            if (br.getHttpConnection().getResponseCode() == 500) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw e;
        }
        /** Prefer HD quality */
        DLLINK = br.getRegex("<hd_720><\\!\\[CDATA\\[(http://.*?)\\]\\]></hd_720>").getMatch(0);
        if (DLLINK == null) DLLINK = br.getRegex("<standard><\\!\\[CDATA\\[(http://.*?)\\]\\]></standard>").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = ".mp4";
        downloadLink.setFinalFileName(filename + ext);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        if (br.containsHTML(">V\\&iacute;deo para maiores de 18 anos")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "18+ video: Only available for registered users");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
