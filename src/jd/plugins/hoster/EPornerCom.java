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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.EpornerComConfig;
import org.jdownloader.plugins.components.config.EpornerComConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eporner.com" }, urls = { "https?://(?:www\\.)?eporner\\.com/hd\\-porn/(\\w+)(/[^/]+)?" })
public class EPornerCom extends PluginForHost {
    public String   dllink        = null;
    private String  vq            = null;
    private boolean server_issues = false;

    public EPornerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.eporner.com/terms/";
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (!this.br.getURL().contains("porn/") || br.containsHTML("id=\"deletedfile\"") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?) \\- EPORNER Free HD Porn Tube</title>").getMatch(0);
        if (filename == null) {
            /* Filename inside url */
            filename = new Regex(link.getPluginPatternMatcher(), "eporner\\.com/hd\\-porn/\\w+/(.+)").getMatch(0);
            if (filename != null) {
                /* url filename --> Nicer url filename */
                filename = filename.replace("-", " ");
            }
        }
        if (filename == null) {
            /* Fallback to linkid inside url */
            filename = getFID(link);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        long filesize = 0;
        getDllink(this.br, link);
        if (dllink == null) {
            /* First try to get DOWNLOADurls */
            final String[][] dloadinfo = this.br.getRegex("href=\"(/dload/[^<>\"]+)\">Download MP4 \\(\\d+p, ([^<>\"]+)\\)</a>").getMatches();
            if (dloadinfo != null && dloadinfo.length != 0) {
                String tempurl = null;
                String tempsize = null;
                long tempsizel = 0;
                for (final String[] dlinfo : dloadinfo) {
                    tempurl = dlinfo[0];
                    tempsize = dlinfo[1];
                    tempsizel = SizeFormatter.getSize(tempsize);
                    if (tempsizel > filesize) {
                        filesize = tempsizel;
                        dllink = "http://www.eporner.com" + tempurl;
                    }
                }
            }
        }
        /* Failed to find DOWNLOADurls? Try to get STREAMurl. */
        if (dllink == null) {
            final String correctedBR = br.toString().replace("\\", "");
            final String continueLink = new Regex(correctedBR, "(\"|\\')(/config\\d+/\\w+/[0-9a-f]+(/)?)(\"|\\')").getMatch(1);
            if (continueLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(Encoding.htmlDecode(continueLink) + (continueLink.endsWith("/") ? "1920" : "/1920"));
            dllink = br.getRegex("<hd\\.file>(https?://.*?)</hd\\.file>").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<file>(https?://.*?)</file>").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("file:[\r\n\r ]*?\"(https?://[^<>\"]*?)\"").getMatch(0);
                }
            }
        }
        if ("http://download.eporner.com/na.flv".equalsIgnoreCase(dllink)) {
            server_issues = true;
        }
        filename = filename.trim();
        link.setFinalFileName(filename + ".mp4");
        /*
         * 2020-05-26: Checking their downloadlink counts to their daily downloadlimit so only check them if the filesize has not been found
         * already!
         */
        if (link.getView().getBytesTotal() <= 0 && dllink != null && !server_issues) {
            /* Only get filesize from url if we were not able to find it in html --> Saves us time! */
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
                } else {
                    /* 2020-05-26: Probably daily limit reached */
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        if (filesize > 0) {
            link.setDownloadSize(filesize);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            /* 2020-05-26: Limit = 100 videos per day for unregistered users, no limit for registered users */
            if (br.containsHTML(">\\s*You have downloaded more than|>\\s*Please try again tomorrow or register for free to unlock unlimited")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily download limit reached", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private void getDllink(final Browser br, final DownloadLink link) {
        vq = getPreferredStreamQuality();
        if (vq != null) {
            logger.info("Looking for user selected quality");
            dllink = br.getRegex("<a href=\"(/dload/[^\"]+)\"\\s*>Download MP4 \\(" + vq).getMatch(0);
            if (dllink != null) {
                logger.info("Found user selected quality: " + vq);
            } else {
                logger.info("Failed to find user selected quality: " + vq);
            }
        }
        if (dllink == null) {
            logger.info("Looking for BEST quality");
            final String[] allQualities = new String[] { "2160p", "1440p", "1080p", "720p", "480p", "360p", "240p" };
            for (final String qualityCandidate : allQualities) {
                vq = qualityCandidate;
                dllink = br.getRegex("<a href=\"(/dload/[^\"]+)\"\\s*>Download MP4 \\(" + vq).getMatch(0);
                if (dllink != null) {
                    logger.info("Picked quality: " + vq);
                    break;
                }
            }
        }
        final String filesize = br.getRegex(">Download MP4 \\(" + vq + "\\s*,\\s*(\\d+[^<>\"]+)\\)").getMatch(0);
        if (filesize != null) {
            logger.info("Found filesize for picked quality: " + filesize);
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return;
    }

    private String getPreferredStreamQuality() {
        final EpornerComConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        default:
            return null;
        case BEST:
            return null;
        case Q2160P:
            return "2160p";
        case Q1080P:
            return "1080p";
        case Q720P:
            return "720p";
        case Q480P:
            return "480p";
        case Q360P:
            return "360p";
        case Q240P:
            return "240p";
        }
    }

    @Override
    public Class<? extends EpornerComConfig> getConfigInterface() {
        return EpornerComConfig.class;
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