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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhost.com" }, urls = { "https?://(?:www\\.)?pornhost\\.com/([0-9]+/([0-9]+\\.html)?|[0-9]+|embed/\\d+)" })
public class PornHostCom extends PluginForHost {
    private String ending = null;
    private String dllink = null;

    public PornHostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornhost.com/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("gallery not found") || br.containsHTML("You will be redirected to")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(?:pornhost.com - )?([^<>\"]*?)( - Pornhost)?</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (StringUtils.isEmpty(filename)) {
            filename = new Regex(link.getPluginPatternMatcher(), "(\\d+)(\\.html)?$").getMatch(0);
        }
        filename = Encoding.htmlDecode(filename.trim());
        if (br.containsHTML(">The movie needs to be converted first")) {
            link.getLinkStatus().setStatusText("The movie needs to be converted first");
            link.setFinalFileName(filename.trim() + ".flv");
            return AvailableStatus.TRUE;
        }
        if (!link.getPluginPatternMatcher().contains(".html")) {
            dllink = br.getRegex("\"(https?://cdn\\d+\\.dl\\.pornhost\\.com/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("file: \"(.*?)\"").getMatch(0);
            }
            if (dllink == null) {
                /* 2020-04-30 */
                dllink = br.getRegex("class=\"download button\" target=\"blank\" href=\"(https[^<>\"]+)").getMatch(0);
            }
            if (dllink == null) {
                /* 2020-04-30 */
                dllink = br.getRegex("<source src=\"(http[^<>\"]+)\" type=\"video/mp4\">").getMatch(0);
            }
        } else {
            dllink = br.getRegex("style=\"width: 499px; height: 372px\">[\t\n\r ]+<img src=\"(http.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://file[0-9]+\\.pornhost\\.com/[0-9]+/.*?)\"").getMatch(0);
            }
        }
        // Maybe we have a picture
        if (dllink == null) {
            dllink = br.getRegex("<div class=\"image\" style=\"width: \\d+px; height: \\d+px\">[\t\n\r ]+<img src=\"(http://[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.urlDecode(dllink, true);
        final String ext = getFileNameExtensionFromString(dllink, ".flv");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
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
        if (br.containsHTML(">The movie needs to be converted first")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The movie needs to be converted first", 30 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        try {
            dl.setAllowFilenameFromURL(false);
            String name = Plugin.getFileNameFromHeader(dl.getConnection());
            if (ending != null && ending.length() <= 1) {
                String name2 = downloadLink.getName();
                name = new Regex(name, ".+?(\\..{1,4})").getMatch(0);
                if (name != null && !name2.endsWith(name)) {
                    name2 = name2 + name;
                    downloadLink.setFinalFileName(name2);
                }
            }
        } catch (final Throwable e) {
        }
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