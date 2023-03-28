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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anyhentai.com" }, urls = { "https?://\\w+\\.anyhentai\\.com/([^/]+)\\.mp4(?:\\?.*)?" })
public class AnyhentaiCom extends PluginForHost {
    private String customFavIconHost = null;

    public AnyhentaiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "https://anyhentai.com/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    /** 2021-03-10: Small helper plugin so JD can handle directURLs of this CDN. */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String fid = this.getFID(link);
        if (!link.isNameSet() && fid != null) {
            link.setName(fid + ".mp4");
        }
        this.setBrowserExclusive();
        /*
         * To process anyhentai media link from javhub and highporn with reference.
         */
        prepBrDownload(this.br);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(link.getPluginPatternMatcher());
            try {
                connectionErrorhandling(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    throw e;
                } else {
                    logger.log(e);
                    /* Ignore error. E.g. error 503 too many connections --> Still we know that the file is online! */
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        prepBrDownload(this.br);
        this.dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 1);
        this.connectionErrorhandling(br, this.dl.getConnection());
        this.dl.startDownload();
    }

    private void connectionErrorhandling(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (con.getResponseCode() == 503) {
            /*
             * 2021-08-26: Users commonly get these directURLs via VideoDownloadHelper but this website has a very strict connection limit
             * of 1 per video so as long as the video is playing in the users' browser, we will get a 503 response in JD.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many connections: Close the video player in your browser to be able to download this item", 3 * 60 * 1000l);
        } else if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            /* Typically error 404. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private void prepBrDownload(final Browser br) {
        br.setFollowRedirects(true);
        /* Required else we won't be able to download/stream the content and will get an error 400. */
        br.getHeaders().put("Referer", "https://javhub.net");
    }

    public String getCustomFavIconURL(final DownloadLink link) {
        if (link != null) {
            final String domain = Browser.getHost(link.getPluginPatternMatcher(), true);
            if (domain != null) {
                return domain;
            }
        }
        if (this.customFavIconHost != null) {
            return this.customFavIconHost;
        } else {
            return null;
        }
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