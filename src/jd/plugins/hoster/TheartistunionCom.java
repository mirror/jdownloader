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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "theartistunion.com" }, urls = { "https?://(?:www\\.)?theartistunion\\.com/tracks/([A-Za-z0-9]+)" })
public class TheartistunionCom extends antiDDoSForHost {
    public TheartistunionCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://theartistunion.com/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;
    private String               url               = null;

    private String getTrackID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.AudioExtensions.MP3);
        this.setBrowserExclusive();
        getPage("https://" + this.getHost() + "/api/v3/tracks/" + this.getTrackID(link) + ".json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* 2019-07-04: {"error":"Could not find track"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = PluginJSonUtils.getJson(br, "s_title");
        if (!StringUtils.isEmpty(filename)) {
            /* 2019-07-04: Many titles contain this sh1t - remove that! */
            filename = filename.replace(" [FREE DOWNLOAD]", "");
            filename += ".mp3";
            link.setFinalFileName(filename);
        } else {
            link.setName(this.getLinkID(link) + ".mp3");
        }
        final String audio_source = PluginJSonUtils.getJson(br, "audio_source");
        if (StringUtils.isEmpty(audio_source)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            this.url = audio_source;
            if (StringUtils.containsIgnoreCase(audio_source, "/stream_files/")) {
                logger.info("Trying to find better quality download");
                final String dllink_better_quality = audio_source.replace("/stream_files/", "/original_files/");
                final URLConnectionAdapter con = br.openGetConnection(dllink_better_quality);
                try {
                    if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                        logger.info("No better quality download found");
                    } else {
                        logger.info("Found better quality download");
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                        url = dllink_better_quality;
                    }
                } finally {
                    con.disconnect();
                }
            } else {
                logger.info("No better quality download possible");
            }
            if (link.getVerifiedFileSize() == -1) {
                final URLConnectionAdapter con = br.openGetConnection(url);
                try {
                    if (!con.getContentType().contains("text") && con.isOK() && con.getLongContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } finally {
                    con.disconnect();
                }
            }
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}