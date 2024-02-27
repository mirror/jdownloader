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

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.VscoCo;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VscoCoCrawler extends PluginForDecrypt {
    public VscoCoCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static final String     PROPERTY_USERNAME      = "user";
    private final String           PROPERTY_DATE          = "date";
    private final String           PROPERTY_DATE_CAPTURED = "date_captured";
    private final SimpleDateFormat sd                     = new SimpleDateFormat("yyyy-MM-dd");
    private VscoCo                 hosterPlugin           = null;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vsco.co" });
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

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            final String domainsPattern = buildHostsPatternPart(domains);
            ret.add("https?://(?:[^/]+\\." + domainsPattern + "/grid/\\d+|(?:www\\.)?" + domainsPattern + "/[\\w-]+/grid/\\d+|(?:www\\.)?" + domainsPattern + "/[\\w-]+(/media/[a-f0-9]{24})?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected DownloadLink createDownloadlink(final String url) {
        return new DownloadLink(this.hosterPlugin, this.getHost(), url);
    }

    @SuppressWarnings({ "unchecked" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        hosterPlugin = (VscoCo) this.getNewPluginForHostInstance(this.getHost());
        final String contenturl = param.getCryptedUrl().replaceFirst("^(?i)http://", "https://");
        String username = new Regex(contenturl, "(?i)https?://([^/]+)\\.vsco\\.co/").getMatch(0);
        if (username == null) {
            username = new Regex(contenturl, "(?i)vsco\\.co/([\\w-]+)").getMatch(0);
        }
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        if (account != null) {
            /* Registered users can view more items */
            hosterPlugin.login(account, false);
        }
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("window\\.__PRELOADED_STATE__ = (\\{.*?\\})</script>").getMatch(0);
        final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(json);
        final String singleImageID = new Regex(br.getURL(), "(?i)https?://[^/]+/[^/]+/media/([a-f0-9]{24})").getMatch(0);
        if (singleImageID != null) {
            /* Crawl single image */
            final Map<String, Object> media = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "medias/byId/" + singleImageID + "/media");
            return crawlProcessMediaFirstPage(media, username);
        } else {
            /* Crawl profile */
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            final Map<String, Object> entities = (Map<String, Object>) root.get("entities");
            final Map<String, Object> imagesFirstPage = (Map<String, Object>) entities.get("images");
            final Map<String, Object> videosFirstPage = (Map<String, Object>) entities.get("videos");
            final Map<String, Object> siteInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "sites/siteByUsername/" + username);
            final Map<String, Object> site = (Map<String, Object>) siteInfo.get("site");
            final String siteid = site.get("id").toString();
            final Map<String, Object> firstPageMediaInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "medias/bySiteId/" + siteid);
            final String authToken = JavaScriptEngineFactory.walkJson(root, "users/currentUser/tkn").toString();
            /* Using the same value as website */
            int max_count_per_pagination_page = 14;
            int page = 1;
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(username);
            final List<Map<String, Object>> mediasFirstPage = (List<Map<String, Object>>) firstPageMediaInfo.get("medias");
            if (mediasFirstPage == null) {
                throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE);
            }
            /* Add items of first page --> They got slightly different field names (wtf) which is why we're processing them separately. */
            for (final Map<String, Object> mediaWeakInfo : mediasFirstPage) {
                final String type = mediaWeakInfo.get("type").toString();
                final Map<String, Object> media;
                if (type.equals("image")) {
                    media = (Map<String, Object>) imagesFirstPage.get(mediaWeakInfo.get("image").toString());
                } else {
                    media = (Map<String, Object>) videosFirstPage.get(mediaWeakInfo.get("video").toString());
                }
                final ArrayList<DownloadLink> results = crawlProcessMediaFirstPage(media, username);
                fp.addLinks(results);
                ret.addAll(results);
                distribute(results);
            }
            String nextCursor = (String) firstPageMediaInfo.get("nextCursor");
            logger.info("Crawled page " + page + " | Items crawled so far: " + ret.size() + " | nextCursor: " + nextCursor);
            final Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("Authorization", "Bearer " + authToken);
            ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            if (!StringUtils.isEmpty(nextCursor)) {
                do {
                    page++;
                    final UrlQuery query = new UrlQuery();
                    query.add("site_id", siteid);
                    query.add("limit", Integer.toString(max_count_per_pagination_page));
                    query.add("cursor", Encoding.urlEncode(nextCursor));
                    ajax.getPage("/api/3.0/medias/profile?" + query.toString());
                    final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
                    nextCursor = (String) entries.get("next_cursor");
                    final List<Map<String, Object>> mediaArray = (List<Map<String, Object>>) entries.get("media");
                    for (final Map<String, Object> mediaArrayObj : mediaArray) {
                        final ArrayList<DownloadLink> thisResults = new ArrayList<DownloadLink>();
                        final String type = mediaArrayObj.get("type").toString(); // image/video
                        final Map<String, Object> media = (Map<String, Object>) mediaArrayObj.get(type);
                        final String description = (String) media.get("description");
                        final String mediaID = media.get("_id").toString();
                        final String hlsMaster = (String) media.get("playback_url");
                        if (!StringUtils.isEmpty(hlsMaster)) {
                            final ArrayList<DownloadLink> videoQualities = this.crawlHlsVideo(username, mediaID, hlsMaster);
                            for (final DownloadLink quality : videoQualities) {
                                /* Set some Packagizer properties */
                                quality.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("created_date")).longValue())));
                                thisResults.add(quality);
                                // quality.setProperty(PROPERTY_DATE_CAPTURED, sd.format(new Date(((Number)
                                // media.get("captureDateMs")).longValue())));
                            }
                        } else {
                            final Boolean isVideo = (Boolean) media.get("is_video");
                            String url_content = null;
                            if (Boolean.TRUE.equals(isVideo)) {
                                url_content = media.get("video_url").toString();
                            } else {
                                url_content = media.get("responsive_url").toString();
                            }
                            if (!(url_content.startsWith("http") || url_content.startsWith("//"))) {
                                url_content = Request.getLocation("//" + url_content, br.getRequest());
                            }
                            final String filename = username + "_" + media.get("_id") + getFileNameExtensionFromString(url_content, Boolean.TRUE.equals(isVideo) ? ".mp4" : ".jpg");
                            String filenameFromURL = null;
                            try {
                                filenameFromURL = Plugin.getFileNameFromURL(url_content);
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            final DownloadLink link = this.createDownloadlink(url_content);
                            link.setContentUrl(media.get("permalink").toString());
                            /* Set some Packagizer properties */
                            link.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("upload_date")).longValue())));
                            link.setProperty(PROPERTY_DATE_CAPTURED, sd.format(new Date(((Number) media.get("capture_date_ms")).longValue())));
                            if (filenameFromURL != null && VscoCo.isPreferOriginalFilenames()) {
                                link.setName(filenameFromURL);
                            } else {
                                link.setFinalFileName(filename);
                            }
                            link.setAvailable(true);
                            if (Boolean.FALSE.equals(isVideo)) {
                                final Map<String, Object> image_meta = (Map<String, Object>) media.get("image_meta");
                                if (image_meta != null) {
                                    final Number file_size = (Number) image_meta.get("file_size");
                                    if (file_size != null) {
                                        link.setDownloadSize(file_size.longValue());
                                    }
                                    // dl.setMD5Hash(imageMeta.get("file_hash").toString());
                                }
                            }
                            thisResults.add(link);
                        }
                        /* Set additional properties */
                        for (final DownloadLink link : thisResults) {
                            link.setProperty(PROPERTY_USERNAME, username);
                            link.setProperty(VscoCo.PROPERTY_MEDIA_ID, mediaID);
                            if (!StringUtils.isEmpty(description)) {
                                link.setComment(description);
                            }
                            ret.add(link);
                            fp.add(link);
                            distribute(link);
                        }
                    }
                    logger.info("Crawled page " + page + " | Items crawled so far: " + ret.size() + " | nextCursor: " + nextCursor);
                    if (mediaArray.size() < max_count_per_pagination_page) {
                        /* Fail safe */
                        logger.info("Stopping because: Current page contains less items than " + max_count_per_pagination_page);
                        break;
                    } else if (StringUtils.isEmpty(nextCursor)) {
                        logger.info("Stopping because: No nextCursor available");
                        break;
                    } else if (this.isAbort()) {
                        logger.info("Stopping because: Crawl process aborted by user");
                        break;
                    } else {
                    }
                } while (true);
            } else {
                logger.info("No pagination available for this profile");
            }
            return ret;
        }
    }

    /** This returns an array just in case we decide to add multi quality selection for HLS videos. */
    private ArrayList<DownloadLink> crawlProcessMediaFirstPage(final Map<String, Object> media, final String username) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String hlsMaster = (String) media.get("playbackUrl");
        // final String type2 = (String) media.get("contentType");
        final String mediaID = media.get("id").toString();
        final String description = (String) media.get("description");
        if (!StringUtils.isEmpty(hlsMaster)) {
            /* 2022-08-30: New HLS video */
            final ArrayList<DownloadLink> videoQualities = this.crawlHlsVideo(username, mediaID, hlsMaster);
            for (final DownloadLink quality : videoQualities) {
                /* Set some Packagizer properties */
                quality.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("createdDate")).longValue())));
                // quality.setProperty(PROPERTY_DATE_CAPTURED, sd.format(new Date(((Number) media.get("captureDateMs")).longValue())));
            }
            ret.addAll(videoQualities);
        } else {
            final Boolean isVideo = (Boolean) media.get("isVideo");
            String url_content = null;
            if (Boolean.TRUE.equals(isVideo)) {
                url_content = media.get("videoUrl").toString();
            } else {
                url_content = media.get("responsiveUrl").toString();
            }
            if (!(url_content.startsWith("http") || url_content.startsWith("//"))) {
                url_content = Request.getLocation("//" + url_content, br.getRequest());
            }
            final String filename = username + "_" + media.get("id") + getFileNameExtensionFromString(url_content, Boolean.TRUE.equals(isVideo) ? ".mp4" : ".jpg");
            String filenameFromURL = null;
            try {
                filenameFromURL = Plugin.getFileNameFromURL(url_content);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            final DownloadLink link = this.createDownloadlink(url_content);
            link.setContentUrl(media.get("permalink").toString());
            /* Set some Packagizer properties */
            link.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("uploadDate")).longValue())));
            final Number captureDateMs = (Number) media.get("captureDateMs");
            if (captureDateMs != null) {
                link.setProperty(PROPERTY_DATE_CAPTURED, sd.format(new Date(captureDateMs.longValue())));
            }
            if (filenameFromURL != null && VscoCo.isPreferOriginalFilenames()) {
                link.setName(filenameFromURL);
            } else {
                link.setFinalFileName(filename);
            }
            link.setAvailable(true);
            if (Boolean.FALSE.equals(isVideo)) {
                final Map<String, Object> imageMeta = (Map<String, Object>) media.get("imageMeta");
                if (imageMeta != null) {
                    /* Filesize is not always given */
                    final Number filesize = (Number) imageMeta.get("fileSize");
                    if (filesize != null) {
                        link.setDownloadSize(filesize.longValue());
                    }
                    // dl.setMD5Hash(imageMeta.get("fileHash").toString());
                }
            }
            ret.add(link);
        }
        /* Add some properties */
        for (final DownloadLink link : ret) {
            link.setProperty(PROPERTY_USERNAME, username);
            link.setProperty(VscoCo.PROPERTY_MEDIA_ID, mediaID);
            if (!StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
        }
        return ret;
    }

    /** This returns an array just in case we decide to add multi quality selection. */
    private ArrayList<DownloadLink> crawlHlsVideo(final String username, final String mediaID, final String hlsMaster) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final DownloadLink video = this.createDownloadlink(hlsMaster);
        video.setAvailable(true);
        video.setFinalFileName(username + "_" + mediaID + ".mp4");
        ret.add(video);
        return ret;
    }
}
