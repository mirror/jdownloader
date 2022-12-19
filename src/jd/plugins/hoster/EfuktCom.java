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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "efukt.com" }, urls = { "https?://(?:www\\.)?efukt\\.com/(\\d+[A-Za-z0-9_\\-]+\\.html|out\\.php\\?id=\\d+|view\\.gif\\.php\\?id=\\d+)" })
public class EfuktCom extends antiDDoSForHost {
    public EfuktCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://efukt.com/tos/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<h1\\s*class=\"title\">(.*?)</h1").getMatch(0);
        if (title == null) {
            title = br.getRegex("id=\"movie_title\" style=\"[^<>\"]+\">([^<>]*?)</div>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)").getMatch(0);
        }
        if (link.getPluginPatternMatcher().contains("view.gif.php")) {
            this.dllink = br.getRegex("<a href=\"(https?://[^\"]+\\.gif)\"[^>]*class=\"image_anchor anchored_item\"").getMatch(0);
        } else {
            dllink = br.getRegex("source\\s*src=\"(https?[^<>\"]*?)\"\\s*type=\"video").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(?:file|url):[\t\n\r ]*?(?:\"|\\')(https?[^<>\"]*?)(?:\"|\\')").getMatch(0);
            }
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            title = encodeUnicode(title);
            link.setFinalFileName(title + ".mp4");
        }
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(dllink);
                handleConnectionErrors(con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final URLConnectionAdapter con) throws PluginException {
        if (!this.looksLikeDownloadableContent(con)) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* 2022-12-19: Looks like etag can vary so we'll double-check for Content-Length header. */
        final String etag = con.getRequest().getResponseHeader("etag");
        // 2022-12-19: Content-Length: 73003
        if (StringUtils.equalsIgnoreCase(etag, "\"637be5da-11d2b\"") || StringUtils.equalsIgnoreCase(etag, "\"63a05f27-11d2b\"")) {
            con.disconnect();
            /* Dummy video containing text "Video removed" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (con.getCompleteContentLength() == 73003) {
            con.disconnect();
            /* Dummy video containing text "Video removed" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
