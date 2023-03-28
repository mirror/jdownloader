//    jDownloader - Downloadmanager
//    Copyright (C) 2016  JD-Team support@jdownloader.org
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xkcd.com" }, urls = { "https?://(?:www\\.)?xkcd\\.com/(\\d+)/" })
public class XkcdCom extends PluginForHost {
    public XkcdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://xkcd.com";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String dllink = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        br.setFollowRedirects(true);
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".png");
        }
        br.getPage(link.getPluginPatternMatcher());
        String filename = this.br.getRegex("id=\"ctitle\">([^<>\"]+)</").getMatch(0);
        if (filename == null) {
            filename = this.br.getRegex("<title>xkcd: ([^<>\"]+)</title>").getMatch(0);
        }
        dllink = br.getRegex("or hotlinking/embedding.*?(http[^<>\"\\']+)").getMatch(0);
        if (StringUtils.endsWithCaseInsensitive(dllink, "/comics/")) {
            link.setName("Javascript entries are not supported");
            throw new PluginException(LinkStatus.ERROR_FATAL, "Javascript entries are not supported");
        }
        if (filename != null) {
            // set filename
            filename = filename.trim();
            String ext = getFileNameExtensionFromURL(dllink);
            if (ext == null || ext.length() > 5) {
                ext = ".png";
            }
            // cleanup extra . at the end of filename
            while (filename.endsWith(".")) {
                filename = filename.substring(0, filename.length() - 1);
            }
            link.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        }
        // do a request to find out the filesize? this can slow down tasks as its another request.
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    // incase redirect after advertised img link.
                    dllink = br.getURL();
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    /* make sure we close connection */
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}