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

import java.util.Arrays;

import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveEdgeContainer;
import jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveEdgeContainer.Protocol;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animalplanet.de", "discovery.de" }, urls = { "https?://(?:www\\.)?animalplanet\\.de.*?#\\d+", "https?://(?:www\\.)?discovery\\.de.*?#\\d+" })
public class AnimalplanetDe extends antiDDoSForHost {
    public AnimalplanetDe(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags: Dmax.de, tlc.de

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://www.animalplanet.de/agb";
    }

    /** 2018-08-07: Quick way to get hls streams: https://edge.api.brightcove.com/playback/v1/accounts/1659832546/videos/<videoID> */
    @SuppressWarnings({ "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* This html will contain required Brightcove Player information! */
        final String jQueryURL = br.getRegex("jQuery\\.get\\(\"(/content/[^<>\"]+/jcr:content/[^<>\"]+)\"").getMatch(0);
        if (jQueryURL == null) {
            /* Probably offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(jQueryURL);
        final BrightcoveEdgeContainer bestBrightcoveVersion = jd.plugins.decrypter.BrightcoveDecrypter.findBESTBrightcoveEdgeContainerAuto(br, Arrays.asList(new Protocol[] { Protocol.HTTP }));
        if (bestBrightcoveVersion == null || bestBrightcoveVersion.getDownloadURL() == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String filename = bestBrightcoveVersion.getStandardFilename();
        dllink = bestBrightcoveVersion.getDownloadURL();
        downloadLink.setFinalFileName(filename);
        downloadLink.setDownloadSize(bestBrightcoveVersion.getFilesize());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
