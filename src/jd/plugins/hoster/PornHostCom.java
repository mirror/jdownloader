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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
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
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornhost.com/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("gallery not found") || br.containsHTML("You will be redirected to")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getPluginPatternMatcher().contains(".html")) {
            dllink = br.getRegex("class=\"image\"[^>]*>\\s*<img src=\"(http[^\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://file[0-9]+\\.pornhost\\.com/[0-9]+/.*?)\"").getMatch(0);
            }
        } else {
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
        }
        // Maybe we have a picture
        if (dllink == null) {
            dllink = br.getRegex("<div class=\"image\" style=\"width: \\d+px; height: \\d+px\">[\t\n\r ]+<img src=\"(http://[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink != null) {
            dllink = Encoding.htmlOnlyDecode(dllink);
            final String filename = Plugin.getFileNameFromURL(dllink);
            if (filename != null) {
                link.setFinalFileName(filename);
            }
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                con = brc.openHeadConnection(dllink);
                if (!looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
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
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML(">\\s*The movie needs to be converted first")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The movie needs to be converted first", 30 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            dl.setAllowFilenameFromURL(false);
            String name = Plugin.getFileNameFromHeader(dl.getConnection());
            if (ending != null && ending.length() <= 1) {
                String name2 = link.getName();
                name = new Regex(name, ".+?(\\..{1,4})").getMatch(0);
                if (name != null && !name2.endsWith(name)) {
                    name2 = name2 + name;
                    link.setFinalFileName(name2);
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
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