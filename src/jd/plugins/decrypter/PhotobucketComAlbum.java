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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PhotobucketComAlbum extends PluginForDecrypt {
    public PhotobucketComAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "photobucket.com" });
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
            ret.add("^https?://(?:(next|app)\\.)?" + buildHostsPatternPart(domains) + "/(u/[^/]+(?:/a/[a-f0-9\\-]+)?$|user/[^/]+)");
        }
        return ret.toArray(new String[0]);
    }

    private final String       TYPE_ALBUM = "https?://[^/]+/u/([^/]+)/a/([a-f0-9\\-]+)(/p/([a-f0-9\\-]+))?";
    private final String       TYPE_USER  = "https?://[^/]+/(?:u|user)/([^/]+)$";
    public static final String API_BASE   = "https://app.photobucket.com/api/graphql/v2";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_ALBUM)) {
            return crawlAlbum(param);
        } else if (param.getCryptedUrl().matches(TYPE_USER)) {
            return this.crawlUser(param);
        } else {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param) throws Exception {
        final String user = new Regex(param.getCryptedUrl(), TYPE_USER).getMatch(0);
        if (user == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBr(br);
        br.postPageRaw(API_BASE, "{ \"operationName\": \"GetAllPublicAlbums\",  \"variables\": {        \"sortBy\": {           \"field\": \"TITLE\",           \"desc\": false     },      \"owner\": \"" + PluginJSonUtils.escape(user) + "\"    },  \"query\": \"query GetAllPublicAlbums($sortBy: Sorter!, $owner: String!) {  getAllPublicAlbums(sortBy: $sortBy, owner: $owner) {    ...AlbumTreeItemFragment    __typename  }}fragment AlbumTreeItemFragment on AlbumTreeItem {  id  title  privacyMode  parentAlbumId  imageCount  __typename}\"}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* User profile does not exist */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        handleErrors(entries);
        final List<Map<String, Object>> albums = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "data/getAllPublicAlbums");
        if (albums == null || albums.isEmpty()) {
            /* User doesn't have any (public) albums */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* All of these URLs will go into this crawler again to find the individual images. */
        for (final Map<String, Object> album : albums) {
            ret.add(this.createDownloadlink(generateURLAlbum(user, album.get("id").toString())));
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlAlbum(final CryptedLink param) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_ALBUM);
        final String user = urlinfo.getMatch(0);
        final String albumID = urlinfo.getMatch(1);
        if (albumID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBr(br);
        /* Get album information */
        br.postPageRaw(API_BASE, "{\"operationName\":\"GetPublicAlbumDetails\",\"variables\":{\"owner\":\"" + PluginJSonUtils.escape(user) + "\",\"albumId\":\"" + PluginJSonUtils.escape(albumID) + "\"},\"query\":\"query GetPublicAlbumDetails($albumId: String!, $owner: String!, $password: String) {  getPublicAlbumDetails(albumId: $albumId, owner: $owner, password: $password) {    id    title    privacyMode    description    nestedAlbumsCount    parentAlbumId    totalUserUsedSpace    totalUserImageCount    imageCountIncludeSubAlbums    imageCount    sorting {      field      desc      __typename    }    __typename  }}\"}");
        final Map<String, Object> albumInfo0 = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        this.handleErrors(albumInfo0);
        final Map<String, Object> albumInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(albumInfo0, "data/getPublicAlbumDetails");
        final int nestedAlbumsCount = ((Number) albumInfo.get("nestedAlbumsCount")).intValue();
        final int imageCount = ((Number) albumInfo.get("imageCount")).intValue();
        if (imageCount == 0) {
            /* Empty album */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String thisAlbumPath = this.getAdoptedCloudFolderStructure(user) + "/" + albumInfo.get("title");
        final FilePackage albumPackage = FilePackage.getInstance();
        albumPackage.setName(thisAlbumPath);
        final String description = (String) albumInfo.get("description");
        if (!StringUtils.isEmpty(description)) {
            albumPackage.setComment(description);
        }
        /* Crawl album images */
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int pageImages = 1;
        String scrollPointerImages = null;
        do {
            if (scrollPointerImages != null) {
                br.postPageRaw(API_BASE, "{ \"operationName\": \"GetPublicAlbumImagesV2\",  \"variables\": {        \"albumId\": \"" + PluginJSonUtils.escape(albumID) + "\",      \"pageSize\": 40,       \"sortBy\": {           \"field\": \"DATE\",            \"desc\": true      },      \"scrollPointer\": \"" + PluginJSonUtils.escape(scrollPointerImages)
                        + "\",      \"password\": \"\"  },  \"query\": \"query GetPublicAlbumImagesV2($albumId: String!, $sortBy: Sorter, $scrollPointer: String, $pageSize: Int, $password: String) {  getPublicAlbumImagesV2(albumId: $albumId, sortBy: $sortBy, scrollPointer: $scrollPointer, pageSize: $pageSize, password: $password) {    scrollPointer    items {      ...ImageFragment      __typename    }    __typename  }}fragment ImageFragment on Image {  id  isBlurred  nsfw  title  username  image {    url    __typename  }  thumbnailImage {    url    width    height    __typename  }  originalImage {    url    width    height    isLandscape    size    __typename  }  livePhoto {    url    width    height    isLandscape    __typename  }  clarifaiTags  description  userTags  uploadDate  dateTaken  originalFilename  postProcess  status  isVideoType  albumName  albumId  __typename}\"}");
            } else {
                br.postPageRaw(API_BASE, "{ \"operationName\": \"GetPublicAlbumImagesV2\",  \"variables\": {        \"albumId\": \"" + PluginJSonUtils.escape(albumID)
                        + "\",      \"scrollPointer\": null,        \"pageSize\": 40,       \"sortBy\": null,       \"password\": \"\"  },  \"query\": \"query GetPublicAlbumImagesV2($albumId: String!, $sortBy: Sorter, $scrollPointer: String, $pageSize: Int, $password: String) {  getPublicAlbumImagesV2(albumId: $albumId, sortBy: $sortBy, scrollPointer: $scrollPointer, pageSize: $pageSize, password: $password) {    scrollPointer    items {      ...ImageFragment      __typename    }    __typename  }}fragment ImageFragment on Image {  id  isBlurred  nsfw  title  username  image {    url    __typename  }  thumbnailImage {    url    width    height    __typename  }  originalImage {    url    width    height    isLandscape    size    __typename  }  livePhoto {    url    width    height    isLandscape    __typename  }  clarifaiTags  description  userTags  uploadDate  dateTaken  originalFilename  postProcess  status  isVideoType  albumName  albumId  __typename}\"}");
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> getPublicAlbumImagesV2 = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/getPublicAlbumImagesV2");
            scrollPointerImages = (String) getPublicAlbumImagesV2.get("scrollPointer");
            final List<Map<String, Object>> images = (List<Map<String, Object>>) getPublicAlbumImagesV2.get("items");
            for (final Map<String, Object> image : images) {
                final String contentURL = param.getCryptedUrl() + "/p/" + image.get("id");
                final DownloadLink dl = this.createDownloadlink(contentURL);
                final DownloadLink link = this.createDownloadlink(JavaScriptEngineFactory.walkJson(image, "originalImage/url").toString());
                jd.plugins.hoster.PhotobucketCom.parseFileInfo(link, image);
                dl.setContentUrl(contentURL);
                dl._setFilePackage(albumPackage);
                dl.setRelativeDownloadFolderPath(thisAlbumPath);
                ret.add(dl);
                distribute(dl);
            }
            logger.info("Crawled page " + pageImages + " | Found items so far: " + ret.size() + "/" + imageCount);
            if (this.isAbort()) {
                return ret;
            } else if (StringUtils.isEmpty(scrollPointerImages)) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (ret.size() >= imageCount) {
                /* Additional fail-safe */
                logger.info("Stopping because: Found all items");
                break;
            } else {
                pageImages++;
                continue;
            }
        } while (true);
        if (nestedAlbumsCount > 0) {
            logger.info("Crawling nested albums");
            /* Test pagination */
            /* Now find nested albums on this level */
            int pageNestedAlbums = 1;
            int foundNestedAlbums = 0;
            String scrollPointerNestedAlbums = null;
            do {
                br.postPageRaw(API_BASE, "{ \"operationName\": \"GetPublicSubAlbumsV2\",    \"variables\": {        \"albumId\": \"" + PluginJSonUtils.escape(albumID) + "\",      \"nestedImagesCount\": 0,       \"scrollPointer\": " + JSonStorage.serializeToJson(scrollPointerNestedAlbums) + ",        \"pageSize\": 14,       \"sortBy\": {           \"field\": \"TITLE\",           \"desc\": false     },      \"username\": \"" + PluginJSonUtils.escape(user)
                        + "\"    },  \"query\": \"query GetPublicSubAlbumsV2($albumId: String!, $sortBy: Sorter, $pageSize: Int, $scrollPointer: String, $nestedImagesCount: Int, $username: String!) {  getPublicSubAlbumsV2(albumId: $albumId, sortBy: $sortBy, pageSize: $pageSize, scrollPointer: $scrollPointer, nestedImagesCount: $nestedImagesCount, username: $username) {    scrollPointer    items {      id      title      privacyMode      imageCount      hasNestedAlbums      images {        id        thumbnailImage {          url          __typename        }        originalImage {          url          __typename        }        uploadDate        isVideoType        isBlurred        __typename      }      __typename    }    __typename  }}\"}");
                final Map<String, Object> subAlbumInfo0 = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                final Map<String, Object> subAlbumInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(subAlbumInfo0, "data/getPublicSubAlbumsV2");
                scrollPointerNestedAlbums = (String) subAlbumInfo.get("scrollPointer");
                final List<Map<String, Object>> nestedAlbums = (List<Map<String, Object>>) subAlbumInfo.get("items");
                for (final Map<String, Object> subAlbum : nestedAlbums) {
                    final DownloadLink dl = this.createDownloadlink(generateURLAlbum(user, subAlbum.get("id").toString()));
                    dl.setRelativeDownloadFolderPath(thisAlbumPath);
                    ret.add(dl);
                    distribute(dl);
                }
                foundNestedAlbums += nestedAlbums.size();
                logger.info("Crawled page " + pageNestedAlbums + " | Found items so far: " + foundNestedAlbums + "/" + nestedAlbumsCount);
                if (this.isAbort()) {
                    return ret;
                } else if (StringUtils.isEmpty(scrollPointerNestedAlbums)) {
                    logger.info("Stopping because: Reached last page");
                    break;
                } else if (foundNestedAlbums >= nestedAlbumsCount) {
                    /* Additional fail-safe */
                    logger.info("Stopping because: Found all items");
                    break;
                } else {
                    pageNestedAlbums++;
                    continue;
                }
            } while (true);
        }
        return ret;
    }

    public static void handleErrors(final Map<String, Object> json) throws PluginException {
        if (json.containsKey("errors")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static Browser prepBr(final Browser br) {
        final String host = getPluginDomains().get(0)[0];
        br.getHeaders().put("apollographql-client-name", "photobucket-web");
        // br.getHeaders().put("x-correlation-id", "NOT_NEEDED");
        // br.getHeaders().put("x-amzn-trace-id", "NOT_NEEDED");
        br.getHeaders().put("content-type", "application/json");
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("apollographql-client-version", "1.26.0");
        br.getHeaders().put("origin", "https://next." + host);
        br.getHeaders().put("referer", "https://next." + host + "/");
        br.setFollowRedirects(true);
        // br.setAllowedResponseCodes(400);
        return br;
    }

    private String generateURLAlbum(final String user, final String albumID) {
        return "https://" + this.getHost() + "/u/" + user + "/a/" + albumID;
    }
}
