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

import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hdpussy.xxx" }, urls = { "https?://(?:www\\.)?hdpussy\\.xxx/video/([a-f0-9]{32})/" })
public class HdPussyXxx extends PluginForHost {
    public HdPussyXxx(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://hdpussy.xxx/";
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        int counter = 0;
        do {
            counter += 1;
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(link.getPluginPatternMatcher());
                if (this.looksLikeDownloadableContent(con)) {
                    /* 2019-02-21: Directurl */
                    dllink = link.getPluginPatternMatcher();
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    link.setFinalFileName(getFID(link) + ".mp4");
                    return AvailableStatus.TRUE;
                } else {
                    br.followConnection();
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*This page not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!this.canHandle(this.br.getURL())) {
                /**
                 * 2021-08-09: E.g. external redirect to advertising website. </br> This may sometimes happen randomly but not more than two
                 * times in a row so retry if this happens.
                 */
                if (counter < 4) {
                    logger.info("MAYBE Offline because of redirect to external website: " + this.br.getURL() + " | Attempt: " + counter);
                    continue;
                } else {
                    logger.info("Offline because of redirect to external website: " + this.br.getURL());
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                break;
            }
        } while (true);
        String filename = br.getRegex("<div class=\"title\\-box\">([^<>\"]*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)\\| HD Pussy XXX</title>").getMatch(0);
        }
        dllink = br.getRegex("file\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
        String ext = null;
        if (dllink != null) {
            if (!dllink.startsWith("http")) {
                /* E.g. missing videosource, player will show error 'No playable sources found' --> Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        }
        if (ext == null || ext.length() > 5) {
            ext = ".mp4";
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setFinalFileName(filename + ext);
        }
        /* Do NOT check for filesize as their directurls often time out which would make this process really really slow! */
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            final long responsecode = dl.getConnection().getResponseCode();
            if (responsecode == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (responsecode == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            }
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
