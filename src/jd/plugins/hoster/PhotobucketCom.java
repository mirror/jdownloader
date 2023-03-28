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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.PhotobucketComAlbum;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { PhotobucketComAlbum.class })
public class PhotobucketCom extends PluginForHost {
    public PhotobucketCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://support.photobucket.com/hc/en-us/requests/new";
    }

    private static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.PhotobucketComAlbum.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("^https?://(?:(?:next|app)\\.)?" + buildHostsPatternPart(domains) + "/u/([^/]+)/a/([a-f0-9\\-]+)/p/([a-f0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME        = true;
    private static final int     FREE_MAXCHUNKS     = 1;
    private static final int     FREE_MAXDOWNLOADS  = -1;
    private static final String  PROPERTY_VIDEO     = "video";
    private static final String  PROPERTY_DIRECTURL = "directlink";

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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(fid);
        }
        this.setBrowserExclusive();
        PhotobucketComAlbum.prepBr(br);
        br.postPageRaw(PhotobucketComAlbum.API_BASE, "{\"operationName\":\"GetPublicImage\",\"variables\":{\"imageId\":\"" + PluginJSonUtils.escape(fid) + "\",\"password\":\"\"},\"query\":\"query GetPublicImage($imageId: String!, $password: String) {  getPublicImage(imageId: $imageId, password: $password) {    id    username    status    nsfw    title    image {      url      __typename    }    thumbnailImage {      url      width      height      __typename    }    originalImage {      url      width      height      __typename    }    description    userTags    clarifaiTags    uploadDate    dateTaken    originalFilename    isVideoType    albumName    albumId    livePhoto {      url      width      height      isLandscape      __typename    }    __typename  }}\"}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        PhotobucketComAlbum.handleErrors(entries);
        final Map<String, Object> image = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/getPublicImage");
        parseFileInfo(link, image);
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfo(final DownloadLink link, final Map<String, Object> image) {
        final String description = (String) image.get("description");
        link.setFinalFileName(image.get("originalFilename").toString());
        link.setAvailable(true);
        if (!StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        if (((Boolean) image.get("isVideoType")).booleanValue()) {
            link.setProperty(PROPERTY_VIDEO, true);
        } else {
            link.removeProperty(PROPERTY_VIDEO);
            link.setProperty(PROPERTY_DIRECTURL, JavaScriptEngineFactory.walkJson(image, "originalImage/url").toString());
        }
    }

    private boolean isVideo(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_VIDEO)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        if (!attemptStoredDownloadurlDownload(link, PROPERTY_DIRECTURL)) {
            requestFileInformation(link);
            if (isVideo(link)) {
                br.postPageRaw(PhotobucketComAlbum.API_BASE, "{\"operationName\":\"GetDirectVideoLinks\",\"variables\":{\"ids\":[\"" + PluginJSonUtils.escape(this.getFID(link)) + "\"]},\"query\":\"query GetDirectVideoLinks($ids: [String]!, $password: String) {  getDirectVideoLinks(ids: $ids, password: $password)}\"}");
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                PhotobucketComAlbum.handleErrors(entries);
                final String directurl = JavaScriptEngineFactory.walkJson(entries, "data/getDirectVideoLinks/{0}").toString();
                if (StringUtils.isEmpty(directurl)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setProperty(PROPERTY_DIRECTURL, directurl);
            }
            if (!link.hasProperty(PROPERTY_DIRECTURL)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!attemptStoredDownloadurlDownload(link, PROPERTY_DIRECTURL)) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, FREE_RESUME, FREE_MAXCHUNKS);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}