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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "photobucket.com" }, urls = { "https?://(?:next|app)\\.photobucket\\.com/u/[^/]+/a/[a-f0-9\\-]+" })
public class PhotobucketComAlbum extends PluginForDecrypt {
    public PhotobucketComAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private final String TYPE_ALBUM = "https?://(?:next|app)\\.photobucket\\.com/u/([^/]+)/a/([a-f0-9\\-]+)";
    private final String TYPE_USER  = "https?://(?:next|app)\\.photobucket\\.com/u/([^/]+)$";
    private final String API_BASE   = "https://app.photobucket.com/api/graphql/v2";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_ALBUM)) {
            return crawlAlbum(param);
        } else {
            /* TODO: Add support to crawl all images of a user */
            return null;
        }
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
        br.postPageRaw(API_BASE, "{\"operationName\":\"GetPublicAlbumDetails\",\"variables\":{\"owner\":\"" + user + "\",\"albumId\":\"" + albumID + "\"},\"query\":\"query GetPublicAlbumDetails($albumId: String!, $owner: String!, $password: String) {  getPublicAlbumDetails(albumId: $albumId, owner: $owner, password: $password) {    id    title    privacyMode    description    nestedAlbumsCount    parentAlbumId    totalUserUsedSpace    totalUserImageCount    imageCountIncludeSubAlbums    imageCount    sorting {      field      desc      __typename    }    __typename  }}\"}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> albumInfo0 = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> albumInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(albumInfo0, "data/getPublicAlbumDetails");
        final int imageCount = ((Number) albumInfo.get("imageCount")).intValue();
        if (imageCount == 0) {
            /* Empty album */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(albumInfo.get("title").toString());
        final String description = (String) albumInfo.get("description");
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        /* TODO: Add pagination */
        /* Crawl album images */
        br.postPageRaw(API_BASE, "{  \"operationName\": \"GetPublicAlbumImagesV2\",  \"variables\": {        \"albumId\": \"" + albumID
                + "\",      \"pageSize\": 40    },  \"query\": \"fragment MediaFragment on Image {  id  title  dateTaken  uploadDate  isVideoType  username  isBlurred  image {    width    size    height    url    isLandscape    __typename  }  thumbnailImage {    width    size    height    url    isLandscape    __typename  }  originalImage {    width    size    height    url    isLandscape    __typename  }  livePhoto {    width    size    height    url    isLandscape    __typename  }  albumId  description  userTags  clarifaiTags  uploadDate  originalFilename  isMobileUpload  albumName  attributes  __typename}query GetPublicAlbumImagesV2($albumId: String!, $sortBy: Sorter, $scrollPointer: String, $pageSize: Int, $password: String) {  getPublicAlbumImagesV2(    albumId: $albumId    sortBy: $sortBy    scrollPointer: $scrollPointer    pageSize: $pageSize    password: $password  ) {    scrollPointer    items {      ...MediaFragment      __typename    }    __typename  }}\"}");
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Map<String, Object>> images = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "data/getPublicAlbumImagesV2/items");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Map<String, Object> image : images) {
            final DownloadLink dl = this.createImageDownloadLink(image);
            dl._setFilePackage(fp);
            ret.add(dl);
            distribute(dl);
        }
        return ret;
    }

    private Browser prepBr(final Browser br) {
        br.getHeaders().put("apollographql-client-name", "photobucket-web");
        // br.getHeaders().put("x-correlation-id", "NOT_NEEDED");
        // br.getHeaders().put("x-amzn-trace-id", "NOT_NEEDED");
        br.getHeaders().put("content-type", "application/json");
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("apollographql-client-version", "1.26.0");
        br.getHeaders().put("origin", "https://next." + this.getHost());
        br.getHeaders().put("referer", "https://next." + this.getHost() + "/");
        // br.setAllowedResponseCodes(400);
        return br;
    }

    private DownloadLink createImageDownloadLink(final Map<String, Object> image) {
        final DownloadLink dl = this.createDownloadlink(JavaScriptEngineFactory.walkJson(image, "originalImage/url").toString());
        final String description = (String) image.get("description");
        dl.setFinalFileName(image.get("originalFilename").toString());
        dl.setAvailable(true);
        if (!StringUtils.isEmpty(description)) {
            dl.setComment(description);
        }
        return dl;
    }
}
