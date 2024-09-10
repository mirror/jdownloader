//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PornhexCom extends PluginForHost {
    public PornhexCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private String dllink = null;

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pornhex.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    private static final Pattern TYPE_NORMAL = Pattern.compile("/video/(?!embed)([\\w-]+)");
    private static final Pattern TYPE_EMBED  = Pattern.compile("/video/embed/([A-Za-z0-9]+)");

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + TYPE_EMBED.pattern() + "|" + TYPE_NORMAL.pattern() + ")");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/info/tos";
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
        String fid = new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0);
        }
        return fid;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        final String extDefault = ".mp4";
        final String fid = this.getFID(link);
        final Regex embed = new Regex(link.getPluginPatternMatcher(), TYPE_EMBED);
        if (!link.isNameSet()) {
            link.setName(fid + extDefault);
        }
        if (!embed.patternFind() && !fid.contains("-")) {
            /* Invalid link e.g. https://pornhex.com/video/trending */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String urlSlug = null;
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (embed.patternFind()) {
            urlSlug = br.getRegex("clickUrl = 'https?://[^/]+/video/([\\w-]+)").getMatch(0);
        } else {
            urlSlug = fid;
        }
        String filename = null;
        if (urlSlug != null) {
            urlSlug = urlSlug.replace("-", " ").trim();
            filename = urlSlug + extDefault;
        }
        String jssource = br.getRegex("videoSources\\s*=\\s*(\\[[^\\]]+\\])").getMatch(0);
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
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            this.basicLinkCheck(br, br.createHeadRequest(this.dllink), link, filename, null);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    protected void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
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