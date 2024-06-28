//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "thisav.com" }, urls = { "https?://(?:www\\.)?thisav\\.com/video/(\\d+)/" })
public class ThisAVCom extends PluginForHost {
    public ThisAVCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private static final String  DEFAULT_EXTENSION = ".mp4";
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://www.thisav.com/static/terms";
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String videoID = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(videoID + DEFAULT_EXTENSION);
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(videoID)) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>([^<>\"]*?) - ").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title.trim());
            link.setFinalFileName(this.applyFilenameExtension(title, DEFAULT_EXTENSION));
        }
        String mpdUrl = br.getRegex("<source src=\"([^\"]+)\"[^>]+label=('|\")HD\\2").getMatch(0);
        if (mpdUrl == null) {
            mpdUrl = br.getRegex("<source src=\"([^\"]+)\"[^>]+label=('|\")SD\\2").getMatch(0);
        }
        if (mpdUrl == null) {
            mpdUrl = br.getRegex("<source src=\"([^\"]+)\"[^>]+dash\\+xml").getMatch(0);
        }
        if (mpdUrl == null) {
            final String cryptedScripts[] = br.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
            if (cryptedScripts != null && cryptedScripts.length != 0) {
                for (String crypted : cryptedScripts) {
                    String decoded = null;
                    try {
                        final Regex params = new Regex(crypted, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");
                        String p = params.getMatch(0).replaceAll("\\\\", "");
                        int a = Integer.parseInt(params.getMatch(1));
                        int c = Integer.parseInt(params.getMatch(2));
                        String[] k = params.getMatch(3).split("\\|");
                        while (c != 0) {
                            c--;
                            if (k[c].length() != 0) {
                                p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                            }
                        }
                        decoded = p;
                        mpdUrl = new Regex(decoded, "source\\s*=\\s*'(http[^<>\"']+)'").getMatch(0);
                    } catch (Exception e) {
                        logger.log(e);
                    }
                }
            }
        }
        if (mpdUrl != null) {
            br.getPage(mpdUrl);
            link.setDownloadSize(getDashFileSize());
            String baseUrl = br.getRegex("<BaseURL>([^<>]+)</").getMatch(0);
            if (baseUrl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = mpdUrl.replaceFirst("/([^/]+)$", "/" + baseUrl);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (StringUtils.isEmpty(this.dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private long getDashFileSize() {
        long result = -1;
        String[] mediaRange = br.getRegex("SegmentURL mediaRange=\"\\d+-(\\d+)\"").getColumn(0);
        if (mediaRange != null) {
            result = Long.parseLong(mediaRange[mediaRange.length - 1]);
        }
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}