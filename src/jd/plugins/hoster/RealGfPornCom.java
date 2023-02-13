//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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
import org.jdownloader.plugins.controller.LazyPlugin;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "realgfporn.com" }, urls = { "https?://(?:www\\.)?realgfporn\\.com/videos/([a-z0-9\\-_%\\.]+)-(\\d+)\\.html" })
public class RealGfPornCom extends PluginForHost {
    public RealGfPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.realgfporn.com/DMCA.html";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (!link.isNameSet()) {
            final String urlSlug = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
            link.setName(urlSlug + ".mp4");
        }
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("<title>Free Amateur and Homemade Porn Videos  \\– Real Girlfriend Porn</title>") || br.containsHTML("class=\"deleted\"") || !br.getURL().contains(this.getFID(link))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h3 class=\"video_title\">(.*?)</h3>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)( - Real Girlfriend Porn)?</title>").getMatch(0);
        }
        dllink = br.getRegex("<source src='(.*?)'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\\(\\'file\\',\\'(https?://.*?)\\'\\)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\\'(https?://media\\d+\\.realgfporn\\.com/videos/.*?)\\'").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\\&file=(https?://(www\\.)realgfporn\\.com/videos/.*?)\\&height=").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("<param name=\"filename\" value=\"(https?://.*?)\"").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("file\\s*:\\s*(\"|'|)(https?://.*?)\\1").getMatch(1);
                        }
                    }
                }
            }
        }
        if (filename != null) {
            filename = filename.trim();
            link.setFinalFileName(Encoding.htmlDecode(filename).trim() + ".mp4");
        }
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
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

    private void handleConnectionErrors(final URLConnectionAdapter con) throws PluginException {
        if (!this.looksLikeDownloadableContent(con)) {
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        handleConnectionErrors(dl.getConnection());
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
    public void resetDownloadlink(DownloadLink link) {
    }
}