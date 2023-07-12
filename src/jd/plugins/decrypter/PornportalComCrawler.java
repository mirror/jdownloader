//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.PornportalComConfig;
import org.jdownloader.plugins.components.config.PornportalComConfig.FilenameScheme;
import org.jdownloader.plugins.components.config.PornportalComConfig.QualitySelectionMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.PornportalCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { PornportalCom.class })
public class PornportalComCrawler extends PluginForDecrypt {
    public PornportalComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.hoster.PornportalCom.getPluginDomains();
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
            // final String annotationName = domains[0];
            /* Premium URLs */
            String pattern = "https?://site-ma\\." + buildHostsPatternPart(domains) + "/(?:gallery|trailer|scene|series)/(\\d+)(/[a-z0-9\\-]+)?";
            /* Free URLs */
            pattern += "|https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:scene|series)/(\\d+)(/[a-z0-9\\-]+)?";
            // if (annotationName.equals("digitalplayground.com")) {
            // pattern += "|https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/scene/(\\d+)(/[a-z0-9\\-]+)?";
            // }
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /* Login if possible */
        final Account acc = getUserLogin();
        if (acc == null) {
            /* Anonymous API auth */
            logger.info("No account given --> Trailer download");
            if (!PornportalCom.prepareBrAPI(this, br, null)) {
                logger.info("Getting fresh API data");
                br.getPage("https://site-ma." + Browser.getHost(param.getCryptedUrl(), false) + "/login");
                if (!PornportalCom.prepareBrAPI(this, br, null)) {
                    logger.warning("Failed to set required API headers");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        final String contentID = new Regex(param.getCryptedUrl(), "(?i)(?:gallery|trailer|scene|series)/(\\d+)").getMatch(0);
        if (contentID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PluginForHost hostPlugin = getNewPluginForHostInstance(this.getHost());
        return crawlContentAPI(hostPlugin, contentID, acc, PluginJsonConfig.get(PornportalComConfig.class));
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private Account getUserLogin() throws Exception {
        final PluginForHost hostPlugin = getNewPluginForHostInstance(this.getHost());
        Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        if (aa == null) {
            /*
             * Try 'internal multihoster' handling e.g. user may have added account for erito.com which also grants premium access to other
             * sites e.g. fakehub.com.
             */
            logger.info("Failed to find main account --> Looking for 'multihoster account'");
            final ArrayList<String> allowedPornportalHosts = PornportalCom.getAllSupportedPluginDomainsFlat();
            final List<Account> multihostAccounts = AccountController.getInstance().getMultiHostAccounts(this.getHost());
            if (multihostAccounts != null) {
                for (final Account multihostAcc : multihostAccounts) {
                    final String multiHostHost = multihostAcc.getHoster();
                    if (multihostAcc.isEnabled() && allowedPornportalHosts.contains(multiHostHost)) {
                        logger.info("Found working multihost account: " + multihostAcc.getHoster());
                        aa = multihostAcc;
                        break;
                    }
                }
            }
        }
        if (aa != null) {
            ((jd.plugins.hoster.PornportalCom) hostPlugin).login(this.br, aa, this.getHost(), false);
            return aa;
        }
        return null;
    }

    public ArrayList<DownloadLink> crawlContentAPI(final PluginForHost plg, final String contentID, final Account account, final PornportalComConfig cfg) throws Exception {
        final FilenameScheme filenameScheme = cfg != null ? cfg.getFilenameScheme() : FilenameScheme.ORIGINAL;
        String api_base = PluginJSonUtils.getJson(br, "dataApiUrl");
        if (StringUtils.isEmpty(api_base)) {
            /* Fallback to static value e.g. loggedIN --> html containing json API information has not been accessed before */
            api_base = "https://site-api.project1service.com";
        }
        br.getPage(api_base + "/v2/releases/" + contentID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        final Map<String, Object> result = (Map<String, Object>) root.get("result");
        final ArrayList<Map<String, Object>> videoObjects = new ArrayList<Map<String, Object>>();
        /* Add current object - that itself could be a video object! */
        videoObjects.add(result);
        /* Look for more objects e.g. video split into multiple parts/scenes(??!) */
        final Object videoChildrenO = result.get("children");
        if (videoChildrenO != null) {
            final List<Map<String, Object>> children = (List<Map<String, Object>>) videoChildrenO;
            videoObjects.addAll(children);
        }
        final String host = this.getHost();
        final HashMap<String, DownloadLink> foundTrailers = new HashMap<String, DownloadLink>();
        final HashMap<String, DownloadLink> foundQualities = new HashMap<String, DownloadLink>();
        for (final Map<String, Object> clipInfo : videoObjects) {
            final String type = (String) clipInfo.get("type");
            // final String type = (String) entries.get("type");
            final String videoID = Long.toString(JavaScriptEngineFactory.toLong(clipInfo.get("id"), 0));
            if (StringUtils.isEmpty(type) || !type.matches("trailer|full|scene")) {
                /* Skip unsupported video types */
                continue;
            } else if (StringUtils.isEmpty(videoID)) {
                /* Skip invalid objects */
                continue;
            }
            String title = (String) clipInfo.get("title");
            String description = (String) clipInfo.get("description");
            if (StringUtils.isEmpty(title)) {
                /* Fallback */
                title = contentID;
            } else if (title.equalsIgnoreCase("trailer")) {
                title = contentID + "_trailer";
            }
            final boolean isTrailer = type.equals("trailer");
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            if (!StringUtils.isEmpty(description)) {
                fp.setComment(description);
            }
            final List<Map<String, Object>> allFullVideos = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> allTrailers = new ArrayList<Map<String, Object>>();
            try {
                final Map<String, Object> videoTypesMap = (Map<String, Object>) clipInfo.get("videos");
                final Map<String, Object> fullVideoRenditions = (Map<String, Object>) JavaScriptEngineFactory.walkJson(videoTypesMap, "full/files");
                final Map<String, Object> trailerRenditions = (Map<String, Object>) JavaScriptEngineFactory.walkJson(videoTypesMap, "mediabook/files");
                if (fullVideoRenditions == null && trailerRenditions == null) {
                    /* Skip non-video objects */
                    continue;
                } else {
                    if (fullVideoRenditions != null) {
                        allFullVideos.add(fullVideoRenditions);
                    }
                    if (trailerRenditions != null) {
                        allTrailers.add(trailerRenditions);
                    }
                }
            } catch (final Throwable e) {
                /* Skip non-video objects */
                continue;
            }
            /* Now walk through all qualities in all types */
            final List<Map<String, Object>> videoRenditionsToProcess;
            /* Prefer full videos over trailers */
            if (!allFullVideos.isEmpty()) {
                videoRenditionsToProcess = allFullVideos;
            } else {
                videoRenditionsToProcess = allTrailers;
            }
            for (final Map<String, Object> files : videoRenditionsToProcess) {
                final Iterator<Entry<String, Object>> qualities = files.entrySet().iterator();
                while (qualities.hasNext()) {
                    final Entry<String, Object> entry = qualities.next();
                    final Map<String, Object> videoInfo = (Map<String, Object>) entry.getValue();
                    final Object urlsO = videoInfo.get("urls");
                    if (urlsO instanceof List) {
                        /* Usually empty list --> Usually means that current clip is not available as full clip -> Trailer only */
                        continue;
                    }
                    String qualityIdentifier = (String) videoInfo.get("format");
                    final long filesize = JavaScriptEngineFactory.toLong(videoInfo.get("sizeBytes"), 0);
                    final Map<String, Object> downloadInfo = (Map<String, Object>) urlsO;
                    String downloadurl = (String) downloadInfo.get("download");
                    if (StringUtils.isEmpty(downloadurl)) {
                        /* Fallback to stream-URL (most times, an official downloadurl is available!) */
                        downloadurl = (String) downloadInfo.get("view");
                    }
                    if (StringUtils.isEmpty(downloadurl)) {
                        continue;
                    } else if (StringUtils.isEmpty(qualityIdentifier) || !qualityIdentifier.matches("\\d+p")) {
                        /* Skip invalid entries and hls and dash streams */
                        continue;
                    }
                    /* E.g. '1080p' --> '1080' */
                    qualityIdentifier = qualityIdentifier.replace("p", "");
                    String contentURL;
                    final String patternMatcher;
                    final DownloadLink dl;
                    if (account != null) {
                        /* Download with account */
                        contentURL = "https://site-ma." + host + "/scene/" + videoID;
                        patternMatcher = contentURL;
                        dl = new DownloadLink(plg, "pornportal", host, patternMatcher, true);
                    } else {
                        /* Without account users can only download trailers and their directurls never expire. */
                        patternMatcher = downloadurl;
                        contentURL = downloadurl;
                        dl = new DownloadLink(JDUtilities.getPluginForHost("DirectHTTP"), "pornportal", host, patternMatcher, true);
                    }
                    dl.setContentUrl(contentURL);
                    final String originalFilename = UrlQuery.parse(downloadurl).get("filename");
                    if (filenameScheme == FilenameScheme.ORIGINAL && originalFilename != null) {
                        dl.setFinalFileName(originalFilename);
                    } else if (filenameScheme == FilenameScheme.VIDEO_ID_TITLE_QUALITY_EXT) {
                        dl.setFinalFileName(videoID + "_" + title + "_" + qualityIdentifier + ".mp4");
                    } else {
                        dl.setFinalFileName(title + "_" + qualityIdentifier + ".mp4");
                    }
                    dl.setProperty(PornportalCom.PROPERTY_VIDEO_ID, videoID);
                    dl.setProperty(PornportalCom.PROPERTY_VIDEO_QUALITY, qualityIdentifier);
                    dl.setProperty(PornportalCom.PROPERTY_directurl, downloadurl);
                    if (filesize > 0) {
                        dl.setDownloadSize(filesize);
                    }
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    if (isTrailer) {
                        foundTrailers.put(qualityIdentifier, dl);
                    } else {
                        foundQualities.put(qualityIdentifier, dl);
                    }
                }
                if (!foundQualities.isEmpty()) {
                    break;
                } else {
                    /* This should never happen! */
                    plg.getLogger().warning("Failed to find any downloadable content for videoID: " + videoID);
                    break;
                }
            }
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (foundQualities.isEmpty() && !foundTrailers.isEmpty()) {
            logger.info("Failed to find any full clip -> Fallback to trailer download");
            foundQualities.putAll(foundTrailers);
        }
        if (!foundQualities.isEmpty()) {
            final ArrayList<DownloadLink> videos = new ArrayList<DownloadLink>();
            final List<String> selectedQualities = new ArrayList<String>();
            if (cfg == null || cfg.isSelectQuality2160()) {
                selectedQualities.add("2160");
            }
            if (cfg == null || cfg.isSelectQuality1080()) {
                selectedQualities.add("1080");
            }
            if (cfg == null || cfg.isSelectQuality720()) {
                selectedQualities.add("720");
            }
            if (cfg == null || cfg.isSelectQuality480()) {
                selectedQualities.add("480");
            }
            if (cfg == null || cfg.isSelectQuality360()) {
                selectedQualities.add("360");
            }
            /* Add user selected quality */
            final ArrayList<DownloadLink> foundSelection = new ArrayList<DownloadLink>();
            if (cfg == null || cfg.getQualitySelectionMode() == QualitySelectionMode.ALL_SELECTED) {
                for (final String selectedQuality : selectedQualities) {
                    if (foundQualities.containsKey(selectedQuality)) {
                        foundSelection.add(foundQualities.get(selectedQuality));
                    }
                }
            } else {
                /* BEST quality only */
                /* Known qualities sorted best -> Worst */
                final String[] allKnownQualities = new String[] { "2160", "1080", "720", "480", "360" };
                for (final String knownQuality : allKnownQualities) {
                    if (foundQualities.containsKey(knownQuality)) {
                        /* We found the best quality */
                        foundSelection.add(foundQualities.get(knownQuality));
                        break;
                    }
                }
            }
            if (!foundSelection.isEmpty()) {
                return foundSelection;
            } else {
                /* Fallback: Add all qualities if none were found by selection */
                logger.info("Failed to find any results by selection -> Returning all");
                final Iterator<Entry<String, DownloadLink>> iteratorQualities = foundQualities.entrySet().iterator();
                while (iteratorQualities.hasNext()) {
                    videos.add(iteratorQualities.next().getValue());
                }
            }
            ret.addAll(videos);
        }
        /* Crawl image gallery */
        final String description = (String) result.get("description");
        final FilePackage imagePackage = FilePackage.getInstance();
        imagePackage.setName(result.get("title").toString());
        if (description != null) {
            imagePackage.setComment(description);
        }
        final ArrayList<DownloadLink> images = new ArrayList<DownloadLink>();
        final List<Map<String, Object>> galleries = (List<Map<String, Object>>) result.get("galleries");
        for (final Map<String, Object> gallery : galleries) {
            final String format = gallery.get("format").toString();
            if (!format.equalsIgnoreCase("pictures")) {
                /* Skip e.g. thumbnails */
                continue;
            }
            final int filesCount = ((Number) gallery.get("filesCount")).intValue();
            final String filePattern = gallery.get("filePattern").toString();
            final String urlformatter = gallery.get("url").toString();
            for (int i = 1; i <= filesCount; i++) {
                final String filename = String.format(filePattern, i);
                final String directurl = urlformatter.replace(filePattern, filename);
                final DownloadLink pic = new DownloadLink(plg, "pornportal", host, directurl, true);
                pic.setName(filename);
                pic.setProperty(PornportalCom.PROPERTY_directurl, directurl);
                pic.setProperty(PornportalCom.PROPERTY_GALLERY_ID, contentID);
                pic.setProperty(PornportalCom.PROPERTY_GALLERY_POSITION, gallery.get("id"));
                pic.setProperty(PornportalCom.PROPERTY_GALLERY_DIRECTORY, gallery.get("directory"));
                pic.setProperty(PornportalCom.PROPERTY_GALLERY_SIZE, filesCount);
                pic.setProperty(PornportalCom.PROPERTY_GALLERY_IMAGE_POSITION, i);
                pic.setAvailable(true);
                pic._setFilePackage(imagePackage);
                images.add(pic);
            }
            ret.addAll(images);
        }
        if (ret.isEmpty()) {
            /* We should always find at least one item! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    public static String getProtocol() {
        return "https://";
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }
}