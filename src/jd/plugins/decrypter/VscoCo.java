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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vsco.co" }, urls = { "https?://(?:[^/]+\\.vsco\\.co/grid/\\d+|(?:www\\.)?vsco\\.co/[\\w-]+/grid/\\d+|(?:www\\.)?vsco\\.co/[\\w-]+(/media/[a-f0-9]{24})?)" })
public class VscoCo extends PluginForDecrypt {
    public VscoCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String    PROPERTY_USERNAME      = "user";
    private static final String    PROPERTY_DATE          = "date";
    private static final String    PROPERTY_DATE_CAPTURED = "date_captured";
    private final SimpleDateFormat sd                     = new SimpleDateFormat("yyyy-MM-dd");

    @SuppressWarnings({ "unchecked" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String username = new Regex(param.getCryptedUrl(), "https?://([^/]+)\\.vsco\\.co/").getMatch(0);
        if (username == null) {
            username = new Regex(param.getCryptedUrl(), "vsco\\.co/([\\w-]+)").getMatch(0);
        }
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("window\\.__PRELOADED_STATE__ = (\\{.*?\\})</script>").getMatch(0);
        final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(json);
        final String singleImageID = new Regex(br.getURL(), "https?://[^/]+/[^/]+/media/([a-f0-9]{24})").getMatch(0);
        if (singleImageID != null) {
            /* Crawl single image */
            final Map<String, Object> media = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "medias/byId/" + singleImageID + "/media");
            final DownloadLink dl = crawlProcessMediaFirstPage(media, username);
            ret.add(dl);
        } else {
            /* Crawl profile */
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
            /* Add items of first page --> They got slightly different field names (wtf) which is why we're processing them separately. */
            for (final Map<String, Object> mediaWeakInfo : mediasFirstPage) {
                final String type = mediaWeakInfo.get("type").toString();
                final Map<String, Object> media;
                if (type.equals("image")) {
                    media = (Map<String, Object>) imagesFirstPage.get(mediaWeakInfo.get("image").toString());
                } else {
                    media = (Map<String, Object>) videosFirstPage.get(mediaWeakInfo.get("video").toString());
                }
                final DownloadLink dl = crawlProcessMediaFirstPage(media, username);
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
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
                    query.add("limit", "14");
                    query.add("cursor", Encoding.urlEncode(nextCursor));
                    ajax.getPage("/api/3.0/medias/profile?" + query.toString());
                    final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
                    nextCursor = (String) entries.get("next_cursor");
                    final List<Map<String, Object>> mediaArray = (List<Map<String, Object>>) entries.get("media");
                    for (final Map<String, Object> mediaArrayObj : mediaArray) {
                        try {
                            final String type = mediaArrayObj.get("type").toString(); // image/video
                            final Map<String, Object> media = (Map<String, Object>) mediaArrayObj.get(type);
                            final String hlsMaster = (String) media.get("playback_url");
                            if (!StringUtils.isEmpty(hlsMaster)) {
                                final DownloadLink dl = this.createDownloadlink(hlsMaster);
                                /* Set some Packagizer properties */
                                dl.setProperty(PROPERTY_USERNAME, username);
                                dl.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("created_date")).longValue())));
                                ret.add(dl);
                                fp.add(dl);
                                distribute(dl);
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
                                final String description = (String) media.get("description");
                                final String filename = username + "_" + media.get("_id") + getFileNameExtensionFromString(url_content, Boolean.TRUE.equals(isVideo) ? ".mp4" : ".jpg");
                                final DownloadLink dl = this.createDownloadlink(url_content);
                                dl.setContentUrl(media.get("permalink").toString());
                                dl.setFinalFileName(filename);
                                dl.setAvailable(true);
                                if (!StringUtils.isEmpty(description)) {
                                    dl.setComment(description);
                                }
                                /* Set some Packagizer properties */
                                dl.setProperty(PROPERTY_USERNAME, username);
                                dl.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("upload_date")).longValue())));
                                dl.setProperty(PROPERTY_DATE_CAPTURED, sd.format(new Date(((Number) media.get("capture_date_ms")).longValue())));
                                ret.add(dl);
                                fp.add(dl);
                                distribute(dl);
                            }
                        } catch (final Throwable e) {
                            logger.warning("WTF");
                            logger.log(e);
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
            }
        }
        return ret;
    }

    private DownloadLink crawlProcessMediaFirstPage(final Map<String, Object> media, final String username) {
        final String hlsMaster = (String) media.get("playbackUrl");
        // final String type2 = (String) media.get("contentType");
        if (!StringUtils.isEmpty(hlsMaster)) {
            /* 2022-08-30: New HLS video */
            final DownloadLink dl = this.createDownloadlink(hlsMaster);
            /* Set some Packagizer properties */
            dl.setProperty(PROPERTY_USERNAME, username);
            dl.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("createdDate")).longValue())));
            // dl.setProperty(PROPERTY_DATE_CAPTURED, sd.format(new Date(((Number) media.get("captureDateMs")).longValue())));
            return dl;
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
            final String description = (String) media.get("description");
            final String filename = username + "_" + media.get("id") + getFileNameExtensionFromString(url_content, Boolean.TRUE.equals(isVideo) ? ".mp4" : ".jpg");
            final DownloadLink dl = this.createDownloadlink(url_content);
            dl.setContentUrl(media.get("permalink").toString());
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
            if (!StringUtils.isEmpty(description)) {
                dl.setComment(description);
            }
            /* Set some Packagizer properties */
            dl.setProperty(PROPERTY_USERNAME, username);
            dl.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("uploadDate")).longValue())));
            dl.setProperty(PROPERTY_DATE_CAPTURED, sd.format(new Date(((Number) media.get("captureDateMs")).longValue())));
            if (Boolean.FALSE.equals(isVideo)) {
                final Map<String, Object> imageMeta = (Map<String, Object>) media.get("imageMeta");
                dl.setDownloadSize(((Number) imageMeta.get("fileSize")).longValue());
                // dl.setMD5Hash(imageMeta.get("fileHash").toString());
            }
            return dl;
        }
    }
}
