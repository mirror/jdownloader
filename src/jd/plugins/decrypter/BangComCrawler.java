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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.BangComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.BangCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { BangCom.class })
public class BangComCrawler extends PluginForDecrypt {
    public BangComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static List<String[]> getPluginDomains() {
        return BangCom.getPluginDomains();
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

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/video/([A-Za-z0-9]+)/([a-z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return crawlVideo(param.getCryptedUrl(), account, PluginJsonConfig.get(BangComConfig.class));
    }

    public <QualitySelectionMode> ArrayList<DownloadLink> crawlVideo(final String url, final Account account, final BangComConfig cfg) throws Exception {
        if (StringUtils.isEmpty(url)) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final List<String> knownVideoQualities = Arrays.asList(new String[] { "2160p", "1080p", "720p", "540p", "480p", "360p" });
        final List<String> selectedVideoQualities = new ArrayList<String>();
        if (cfg == null || cfg.isCrawl2160p()) {
            selectedVideoQualities.add(knownVideoQualities.get(0));
        }
        if (cfg == null || cfg.isCrawl1080p()) {
            selectedVideoQualities.add(knownVideoQualities.get(1));
        }
        if (cfg == null || cfg.isCrawl720p()) {
            selectedVideoQualities.add(knownVideoQualities.get(2));
        }
        if (cfg == null || cfg.isCrawl540p()) {
            selectedVideoQualities.add(knownVideoQualities.get(3));
        }
        if (cfg == null || cfg.isCrawl480p()) {
            selectedVideoQualities.add(knownVideoQualities.get(4));
        }
        if (cfg == null || cfg.isCrawl360p()) {
            selectedVideoQualities.add(knownVideoQualities.get(5));
        }
        if (selectedVideoQualities.isEmpty() && cfg != null && !cfg.isGrabPreviewVideo() && !cfg.isGrabThumbnail()) {
            logger.info("Returning nothing because user has deselected all qualities -> Disabled crawler");
            return ret;
        }
        br.setFollowRedirects(true);
        final BangCom plg = (BangCom) this.getNewPluginForHostInstance(this.getHost());
        if (account != null) {
            plg.login(account, true, url);
        } else {
            br.getPage(url);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> videoObject = null;
        final String[] jsSnippets = br.getRegex("<script type=\"application/ld\\+json\">(.*?)</script>").getColumn(0);
        for (final String jsSnippet : jsSnippets) {
            final Map<String, Object> entries = restoreFromString(jsSnippet, TypeRef.MAP);
            final String type = (String) entries.get("@type");
            if (StringUtils.equalsIgnoreCase(type, "VideoObject")) {
                videoObject = entries;
                break;
            }
        }
        if (videoObject == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String contentID = videoObject.get("@id").toString();
        final String title = videoObject.get("name").toString();
        final String thumbnailUrl = videoObject.get("thumbnailUrl").toString(); // always available
        final String previewURL = videoObject.get("contentUrl").toString(); // always available
        final String description = (String) videoObject.get("description");
        final String photosAsZipURL = br.getRegex("\"(https?://photos\\.[^/]+/\\.zip[^\"]+)\"").getMatch(0); // not always available
        if (StringUtils.isEmpty(previewURL) || StringUtils.isEmpty(thumbnailUrl)) {
        }
        if (cfg == null || cfg.isGrabThumbnail()) {
            final DownloadLink thumb = new DownloadLink(plg, null, this.getHost(), thumbnailUrl, true);
            thumb.setProperty(BangCom.PROPERTY_QUALITY_IDENTIFIER, "THUMBNAIL");
            ret.add(thumb);
        }
        /*
         * Enforce preview video download if no account is given as without account we will not be able to download any full length streams.
         */
        if (cfg == null || cfg.isGrabPreviewVideo() || account == null) {
            final DownloadLink preview = new DownloadLink(plg, null, this.getHost(), previewURL, true);
            preview.setProperty(BangCom.PROPERTY_QUALITY_IDENTIFIER, "PREVIEW");
            ret.add(preview);
        }
        if (photosAsZipURL != null && (cfg == null || cfg.isGrabPhotosZipArchive())) {
            final DownloadLink zip = new DownloadLink(plg, null, this.getHost(), photosAsZipURL, true);
            zip.setProperty(BangCom.PROPERTY_QUALITY_IDENTIFIER, "ZIP");
            ret.add(zip);
        }
        final String[] videoDownloadurls = br.getRegex("\"(https?://[^\"]+\\d+p\\.mp4[^\"]+)\"").getColumn(0);
        videoCrawler: if (videoDownloadurls != null && videoDownloadurls.length > 0) {
            /* Video streams are only available for premium users */
            final ArrayList<DownloadLink> allVideoItems = new ArrayList<DownloadLink>();
            final ArrayList<DownloadLink> selectedVideoItems = new ArrayList<DownloadLink>();
            DownloadLink bestVideo = null;
            int pixelHeightBest = -1;
            DownloadLink bestVideoOfSelection = null;
            int pixelHeightBestOfSelection = -1;
            final String[] videoFilesizes = br.getRegex("class=\"text-light text-opacity-75\">([^<]+)</span>").getColumn(0);
            // final String[] videoQualityLabels = br.getRegex("</svg>([^<]+)</span>").getColumn(0);
            int position = -1;
            for (String videoDownloadurl : videoDownloadurls) {
                position++;
                final String pixelHeightStr = new Regex(videoDownloadurl, "(\\d+)p\\.mp4").getMatch(0);
                if (pixelHeightStr == null) {
                    logger.warning("Unsupported video format/url: " + videoDownloadurl);
                    continue;
                }
                final int pixelHeight = Integer.parseInt(pixelHeightStr);
                if (videoDownloadurl.contains("&amp;")) {
                    videoDownloadurl = Encoding.htmlDecode(videoDownloadurl);
                }
                final DownloadLink video = new DownloadLink(plg, null, this.getHost(), videoDownloadurl, true);
                /* Set rough filesize if we're able to find it. */
                if (videoFilesizes != null && videoFilesizes.length == videoDownloadurls.length) {
                    final String filesizeStr = videoFilesizes[position];
                    video.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                }
                video.setProperty(BangCom.PROPERTY_QUALITY_IDENTIFIER, pixelHeightStr + "p");
                if (selectedVideoQualities.contains(pixelHeightStr + "p")) {
                    selectedVideoItems.add(video);
                    /* Determine best quality within selected qualities */
                    if (bestVideoOfSelection == null || pixelHeight > pixelHeightBestOfSelection) {
                        pixelHeightBestOfSelection = pixelHeight;
                        bestVideoOfSelection = video;
                    }
                }
                /* Determine best overall quality */
                if (bestVideo == null || pixelHeight > pixelHeightBest) {
                    pixelHeightBest = pixelHeight;
                    bestVideo = video;
                }
                allVideoItems.add(video);
            }
            if (allVideoItems.isEmpty()) {
                logger.warning("Failed to find any video items");
                break videoCrawler;
            }
            if (cfg != null) {
                final BangComConfig.QualitySelectionMode mode = cfg.getQualitySelectionMode();
                if (mode == BangComConfig.QualitySelectionMode.BEST && bestVideo != null) {
                    ret.add(bestVideo);
                } else if (mode == BangComConfig.QualitySelectionMode.BEST_OF_SELECTED && bestVideoOfSelection != null) {
                    ret.add(bestVideoOfSelection);
                } else if (mode == BangComConfig.QualitySelectionMode.ALL_SELECTED && selectedVideoItems.size() > 0) {
                    ret.addAll(selectedVideoItems);
                } else {
                    /* Fallback */
                    logger.info("No results according to user quality selection -> Adding all video qualities");
                    ret.addAll(allVideoItems);
                }
            } else {
                ret.addAll(allVideoItems);
            }
        } else {
            if (account != null) {
                logger.warning("Failed to find any video streams although account is available -> Possible plugin failure!");
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        for (final DownloadLink result : ret) {
            result.setContainerUrl(br.getURL());
            result.setProperty(BangCom.PROPERTY_MAINLINK, br.getURL());
            result.setProperty(BangCom.PROPERTY_CONTENT_ID, contentID);
            result.setAvailable(true);
        }
        fp.addLinks(ret);
        return ret;
    }
}
