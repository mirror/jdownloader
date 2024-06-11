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
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.HeadRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nonktube.com" }, urls = { "https?://(?:www\\.)?nonktube\\.com/(?:porn/)?video/(\\d+)/([a-z0-9\\-]+)" })
public class NonktubeCom extends PluginForHost {
    public NonktubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume    = true;
    private static final int     free_maxchunks = 0;
    private String               dllink         = null;

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/static/terms";
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
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        final String fid = this.getFID(link);
        final String urlSlug = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        link.setFinalFileName(urlSlug.replace("-", " ").trim() + ".mp4");
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher().replaceFirst("^(?i)http://", "https://"));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(fid)) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* RegExes for videohosts */
        String jssource = br.getRegex("\"?sources\"?\\s*:\\s*(\\[[^\\]]+\\])").getMatch(0);
        if (StringUtils.isEmpty(jssource)) {
            /* 2019-07-04: Wider attempt - find sources via pattern of their video-URLs. */
            jssource = br.getRegex("[A-Za-z0-9]+\\s*:\\s*(\\[[^\\]]+[a-z0-9]{60}/v\\.mp4[^\\]]+\\])").getMatch(0);
        }
        if (!StringUtils.isEmpty(jssource)) {
            logger.info("Found video json source");
            /*
             * Different services store the values we want under different names. E.g. vidoza.net uses 'res', most providers use 'label'.
             */
            final String[] possibleQualityObjectNames = new String[] { "label", "res" };
            /*
             * Different services store the values we want under different names. E.g. vidoza.net uses 'src', most providers use 'file'.
             */
            final String[] possibleStreamURLObjectNames = new String[] { "file", "src" };
            try {
                /*
                 * Important: Default is -1 so that even if only one quality is available without quality-identifier, it will be used!
                 */
                long quality_picked = -1;
                String dllink_temp = null;
                /*
                 * Important: Do not use "Plugin.restoreFromString" here as the input of this can also be js structure and not only json!!
                 */
                final List<Object> ressourcelist = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(jssource);
                final boolean onlyOneQualityAvailable = ressourcelist.size() == 1;
                final int userSelectedQuality = -1;
                if (userSelectedQuality == -1) {
                    logger.info("Looking for BEST video stream");
                } else {
                    logger.info("Looking for user selected video stream quality: " + userSelectedQuality);
                }
                boolean foundUserSelectedQuality = false;
                for (final Object videoo : ressourcelist) {
                    /* Check for single URL without any quality information e.g. uqload.com */
                    if (videoo instanceof String && onlyOneQualityAvailable) {
                        logger.info("Only one quality available --> Returning that");
                        dllink_temp = (String) videoo;
                        if (dllink_temp.startsWith("http")) {
                            dllink = dllink_temp;
                            break;
                        }
                    }
                    final Map<String, Object> entries;
                    if (videoo instanceof Map) {
                        entries = (Map<String, Object>) videoo;
                        for (final String possibleStreamURLObjectName : possibleStreamURLObjectNames) {
                            if (entries.containsKey(possibleStreamURLObjectName)) {
                                dllink_temp = (String) entries.get(possibleStreamURLObjectName);
                                break;
                            }
                        }
                    } else {
                        entries = null;
                    }
                    if (StringUtils.isEmpty(dllink_temp)) {
                        /* No downloadurl found --> Continue */
                        continue;
                    } else if (dllink_temp.contains(".mpd")) {
                        /* 2020-05-20: This plugin cannot yet handle DASH stream downloads */
                        logger.info("Skipping DASH stream: " + dllink_temp);
                        continue;
                    }
                    /* Find quality + downloadurl */
                    long quality_temp = 0;
                    for (final String possibleQualityObjectName : possibleQualityObjectNames) {
                        try {
                            final Object quality_temp_o = entries.get(possibleQualityObjectName);
                            if (quality_temp_o != null && quality_temp_o instanceof Number) {
                                quality_temp = ((Number) quality_temp_o).intValue();
                            } else if (quality_temp_o != null && quality_temp_o instanceof String) {
                                /* E.g. '360p' */
                                final String res = new Regex((String) quality_temp_o, "(\\d+)p?$").getMatch(0);
                                if (res != null) {
                                    quality_temp = (int) Long.parseLong(res);
                                }
                            }
                            if (quality_temp > 0) {
                                break;
                            }
                        } catch (final Throwable e) {
                            /* This should never happen */
                            logger.log(e);
                            logger.info("Failed to find quality via key '" + possibleQualityObjectName + "' for current downloadurl candidate: " + dllink_temp);
                            if (!onlyOneQualityAvailable) {
                                continue;
                            }
                        }
                    }
                    if (StringUtils.isEmpty(dllink_temp)) {
                        continue;
                    } else if (quality_temp == userSelectedQuality) {
                        /* Found user selected quality */
                        logger.info("Found user selected quality: " + userSelectedQuality);
                        foundUserSelectedQuality = true;
                        quality_picked = quality_temp;
                        dllink = dllink_temp;
                        break;
                    } else {
                        /* Look for best quality */
                        if (quality_temp > quality_picked) {
                            quality_picked = quality_temp;
                            dllink = dllink_temp;
                        }
                    }
                }
                if (!StringUtils.isEmpty(dllink)) {
                    logger.info("Quality handling for multiple video stream sources succeeded - picked quality is: " + quality_picked);
                    if (foundUserSelectedQuality) {
                        logger.info("Successfully found user selected quality: " + userSelectedQuality);
                    } else {
                        logger.info("Successfully found BEST quality: " + quality_picked);
                    }
                } else {
                    logger.info("Failed to find any stream downloadurl");
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("BEST handling for multiple video source failed");
            }
        }
        if (dllink != null) {
            dllink = Encoding.htmlOnlyDecode(dllink);
            if (!isDownload) {
                URLConnectionAdapter con = null;
                try {
                    HeadRequest headRequest = new HeadRequest(dllink);
                    headRequest.getHeaders().put(OPEN_RANGE_REQUEST);
                    con = br.openRequestConnection(headRequest);
                    handleConnectionErrors(br, con);
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } finally {
                    try {
                        if (con != null) {
                            con.disconnect();
                        }
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(DirectHTTP.PROPERTY_ServerComaptibleForByteRangeRequest, true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
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
