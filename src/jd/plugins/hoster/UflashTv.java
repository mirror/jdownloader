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
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uflash.tv" }, urls = { "https?://(www\\.)?uflash\\.tv/video/\\d+" })
public class UflashTv extends PluginForHost {
    public UflashTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public String getAGBLink() {
        return "http://www.uflash.tv/static/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("uflash.tv/error/video_missing")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("VIDEO \\| ([^<>\"]*?) \\| UFLASH\\.TV").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        if (br.containsHTML(">This is a private video")) {
            downloadLink.getLinkStatus().setStatusText("Private videos are only downloadable via account");
            downloadLink.setName(filename + ".flv");
            return AvailableStatus.TRUE;
        }
        String video = br.getRegex("var\\s*[\\w_]*\\s*=\\s*\"(a.*?)\"").getMatch(0);
        if (video != null) {
            final String videoID = new Regex(downloadLink.getDownloadURL(), "/video/(\\d+)").getMatch(0);
            video = Base64.decodeToString(video);
            if (StringUtils.contains(video, videoID)) {
                dllink = video;
            }
        }
        if (dllink == null) {
            br.getPage("/media/player/config.v89x.php?vkey=" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
            dllink = br.getRegex("<src>(https?://[^<>\"]*?)</src>").getMatch(0);
        }
        final String ext = ".mp4";
        downloadLink.setFinalFileName(filename + ext);
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openGetConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        downloadLink.setDownloadSize(con.getCompleteContentLength());
                    }
                } else {
                    brc.followConnection(true);
                    if (brc.containsHTML(">\\s*404 Not Found\\s*<")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        server_issues = true;
                    }
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
        if (br.containsHTML(">This is a private video")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML(">\\s*404 Not Found\\s*<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
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
