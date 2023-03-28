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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shesfreaky.com" }, urls = { "https?://(?:www\\.)?shesfreaky\\.com/(?:video/[A-Za-z0-9\\-_]+-\\d+\\.html|embed/\\d+)" })
public class ShesFreakyCom extends PluginForHost {
    public ShesFreakyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String              dllink      = null;
    private static final String TYPE_NORMAL = "https?://[^/]+/video/([A-Za-z0-9\\-_]+)-(\\d+)\\.html";
    private static final String TYPE_EMBED  = "https?://[^/]+/embed/(\\d+)";

    @Override
    public String getAGBLink() {
        return "http://www.shesfreaky.com/terms/";
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
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(1);
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0) + ".mp4";
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0).replace("-", " ") + ".mp4";
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            /* Do this so we can find meaningful filenames even for embed URLs. */
            br.getPage("https://www." + this.getHost() + "/video/-" + this.getFID(link) + ".html");
        } else {
            br.getPage(link.getPluginPatternMatcher());
        }
        if (br.containsHTML("class=\"error\"|>404: File Not Found<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = br.getRegex("(?i)<title>(.*?)- ShesFreaky</title>").getMatch(0);
        if (title != null) {
            link.setFinalFileName(Encoding.htmlDecode(title).trim() + ".mp4");
        }
        dllink = br.getRegex("url: escape\\(\\'(https?://[^<>\"]*?)\\'\\)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"(.*?)\"").getMatch(0);
        }
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
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
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (this.br.containsHTML("class=\"private\\-video\"")) {
            /* E.g. 'You must be friends with blabla to video this content' */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
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
