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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "smotri.com" }, urls = { "http://(www\\.)?smotri\\.com/video/view/\\?id=v[a-z0-9]+" })
public class SmotriCom extends PluginForHost {
    public SmotriCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public String getAGBLink() {
        return "http://smotri.com/info/?page=rules";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        downloadLink.setName(fid + ".mp4");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("class=\"top\\-info\\-404\">|Видео не прошло модерацию<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains("/video/view/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Confirm that we're over 18 years old if necessary
        final String ageconfirm = br.getRegex("\"(/video/view/\\?id=v[a-z0-9]+\\&confirm=[A-Za-z0-9]+)\"").getMatch(0);
        if (ageconfirm != null) {
            br.getPage("http://smotri.com" + ageconfirm);
        }
        String filename = br.getRegex("itemprop=\"name\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        br.postPage("http://smotri.com/video/view/url/bot/", "video_url=1&ticket=" + fid);
        if (br.containsHTML("\"_pass_protected\":1")) {
            downloadLink.getLinkStatus().setStatusText("Password protected links are not supported yet");
            downloadLink.setName(filename + ".mp4");
            return AvailableStatus.TRUE;
        }
        // dllink = br.getRegex("\"_vidURL_mp4\":\"(http[^<>\"]*?)\"").getMatch(0);
        dllink = PluginJSonUtils.getJsonValue(br, "_vidURL_mp4");
        if (dllink == null) {
            // dllink = br.getRegex("\"_vidURL\":\"(http[^<>\"]*?)\"").getMatch(0);
            dllink = PluginJSonUtils.getJsonValue(br, "_vidURL");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String ext;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, ".mp4");
            if (StringUtils.isEmpty(ext)) {
                ext = ".flv";
            }
        } else {
            ext = ".flv";
        }
        downloadLink.setFinalFileName(filename + ext);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
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
        if (br.containsHTML("\"_pass_protected\":1")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not supported yet");
        }
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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