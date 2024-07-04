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
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.AparatComConfig;
import org.jdownloader.plugins.components.config.AparatComConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.AparatCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AparatComCrawler extends PluginForDecrypt {
    public AparatComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "aparat.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    private static final String PATTERN_RELATIVE_VIDEO    = "/v/([A-Za-z0-9]+).*";
    private static final String PATTERN_RELATIVE_PLAYLIST = "/playlist/(\\d+)";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_VIDEO + "|" + PATTERN_RELATIVE_PLAYLIST + ")");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setAllowedResponseCodes(400);
        final Regex singlevideo = new Regex(param.getCryptedUrl(), PATTERN_RELATIVE_VIDEO);
        final Regex playlist = new Regex(param.getCryptedUrl(), PATTERN_RELATIVE_PLAYLIST);
        if (singlevideo.patternFind()) {
            return crawlSingleVideo(singlevideo.getMatch(0), null);
        } else if (playlist.patternFind()) {
            final String playlistid = playlist.getMatch(0);
            br.getPage("https://www." + this.getHost() + "/api/fa/v1/video/playlist/one/playlist_id/" + playlistid);
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> playlistmap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/attributes");
            final int numberofVideos = ((Number) playlistmap.get("cnt")).intValue();
            final String playlisttitle = playlistmap.get("title").toString();
            final String playlistdescription = (String) playlistmap.get("description");
            logger.info("Crawling playlist " + playlistid + " | Number of videos: " + numberofVideos + " | Title: " + playlisttitle);
            if (numberofVideos == 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Empty playlist");
            }
            final List<Map<String, Object>> items = (List<Map<String, Object>>) entries.get("included");
            for (final Map<String, Object> item : items) {
                final String type = item.get("type").toString();
                if (!StringUtils.equalsIgnoreCase(type, "Video")) {
                    /* Skip invalid items */
                    continue;
                }
                final Map<String, Object> videomap = (Map<String, Object>) item.get("attributes");
                final String videoid = videomap.get("uid").toString();
                final DownloadLink videoresult = this.createDownloadlink("https://www." + this.getHost() + "/v/" + videoid + "?playlist=" + playlistid);
                ret.add(videoresult);
            }
            /* Put all results into one package */
            final FilePackage fp = FilePackage.getInstance();
            /* Allow single video packages to get merged into one package if they are part of a playlist. */
            fp.setAllowInheritance(true);
            fp.setAllowMerge(true);
            if (!playlisttitle.isEmpty()) {
                fp.setName(Encoding.htmlDecode(playlisttitle));
            } else {
                fp.setName(playlistid);
            }
            if (!StringUtils.isEmpty(playlistdescription)) {
                fp.setComment(playlistdescription);
            }
            // filePackage.setComment(title);
            fp.setPackageKey("aparat_com://playlist/" + playlistid);
            fp.addLinks(ret);
        } else {
            /* Developer mistake -> This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlSingleVideo(final String videoid, final Integer forcedPreferredQualityHeight) throws PluginException, IOException {
        br.getPage("https://www." + this.getHost() + "/api/fa/v1/video/video/show/videohash/" + videoid + "?pr=1&mf=1");
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        final Number status = (Number) data.get("status");
        if (status != null && (status.intValue() == 404 || status.intValue() == 410)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String type = data.get("type").toString();
        if (!StringUtils.equalsIgnoreCase(type, "VideoShow")) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported type:" + type);
        }
        final Map<String, Object> videomap = (Map<String, Object>) data.get("attributes");
        // TODO: Make use of official downloadurl if available
        // final String officialDownloadlink = (String) videomap.get("file_link");
        String videotitle = (String) videomap.get("title");
        if (!StringUtils.isEmpty(videotitle)) {
            videotitle = Encoding.htmlDecode(videotitle).trim();
        }
        final String videodescription = (String) videomap.get("description");
        final List<Map<String, Object>> qualitymaps = (List<Map<String, Object>>) videomap.get("file_link_all");
        int videoHeightMax = -1;
        DownloadLink best = null;
        DownloadLink preferredQuality = null;
        final int preferredQualityHeight;
        if (forcedPreferredQualityHeight != null) {
            preferredQualityHeight = forcedPreferredQualityHeight.intValue();
        } else {
            final PreferredStreamQuality quality = PluginJsonConfig.get(AparatComConfig.class).getPreferredStreamQuality();
            if (quality == PreferredStreamQuality.Q144P) {
                preferredQualityHeight = 144;
            } else if (quality == PreferredStreamQuality.Q240P) {
                preferredQualityHeight = 240;
            } else if (quality == PreferredStreamQuality.Q360P) {
                preferredQualityHeight = 360;
            } else if (quality == PreferredStreamQuality.Q480P) {
                preferredQualityHeight = 480;
            } else if (quality == PreferredStreamQuality.Q720P) {
                preferredQualityHeight = 720;
            } else if (quality == PreferredStreamQuality.Q1080P) {
                preferredQualityHeight = 1080;
            } else if (quality == PreferredStreamQuality.Q2160P) {
                preferredQualityHeight = 2160;
            } else {
                preferredQualityHeight = -1;
            }
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final PluginForHost hosterplugin = this.getNewPluginForHostInstance(this.getHost());
        for (final Map<String, Object> qualitymap : qualitymaps) {
            final String thisprofile = qualitymap.get("profile").toString();
            if (!thisprofile.matches("(?i)\\d+p")) {
                continue;
            }
            final int thisVideoHeight = Integer.parseInt(thisprofile.toLowerCase(Locale.ENGLISH).replace("p", ""));
            final List<String> urls = (List<String>) qualitymap.get("urls");
            final String directurl = urls.get(0);
            final DownloadLink dl = this.createDownloadlink(directurl);
            dl.setDefaultPlugin(hosterplugin);
            final String profile = qualitymap.get("profile").toString(); // e.g. "1080p"
            String fileName;
            if (!StringUtils.isEmpty(videotitle)) {
                fileName = videotitle;
            } else {
                fileName = videoid;
            }
            if (profile != null) {
                fileName += "_" + profile;
            }
            dl.setFinalFileName(fileName + ".mp4");
            dl.setAvailable(true);
            dl.setContentUrl("https://www." + getHost() + "/v/" + videoid);
            dl.setProperty(AparatCom.PROPERTY_VIDEOID, videoid);
            dl.setProperty(AparatCom.PROPERTY_QUALITY_HEIGHT, thisVideoHeight);
            dl.setProperty(AparatCom.PROPERTY_QUALITY_DIRECTURL, directurl);
            ret.add(dl);
            if (thisVideoHeight > videoHeightMax || best == null) {
                videoHeightMax = thisVideoHeight;
                best = dl;
            }
            if (thisVideoHeight == preferredQualityHeight) {
                preferredQuality = dl;
            }
        }
        if (preferredQuality != null) {
            ret.clear();
            ret.add(preferredQuality);
        } else {
            ret.clear();
            ret.add(best);
        }
        /* Put all results into one package */
        final FilePackage fp = FilePackage.getInstance();
        if (!videotitle.isEmpty()) {
            fp.setName(videotitle);
        }
        if (!StringUtils.isEmpty(videodescription)) {
            fp.setComment(videodescription);
        }
        fp.setPackageKey("aparat_com://video/" + videoid);
        fp.addLinks(ret);
        return ret;
    }

    @Override
    public Class<? extends AparatComConfig> getConfigInterface() {
        return AparatComConfig.class;
    }
}