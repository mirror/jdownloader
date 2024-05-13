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
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.EPornerCom;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { EPornerCom.class })
public class EPornerComGallery extends PluginForDecrypt {
    public EPornerComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return EPornerCom.getPluginDomains();
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/gallery/([A-Za-z0-9]+)/([\\w\\-]+)/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String gallerySlug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String numberofPhotosStr = br.getRegex("Photos:\\s*(\\d+)").getMatch(0);
        final int numberofPhotos = Integer.parseInt(numberofPhotosStr);
        final boolean crawlByThumbnail = false;
        final HashSet<String> dupes = new HashSet<String>();
        if (crawlByThumbnail) {
            /* Superfast crawling: Grab thumbnail URLs and modify them so they point to the full images. */
            final String[] thumbnails = br.getRegex("id=\"t\\d+\"[^>]*src=\"(https?://[^\"]+)\"").getColumn(0);
            if (thumbnails == null || thumbnails.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String thumbnailURL : thumbnails) {
                if (!dupes.add(thumbnailURL)) {
                    continue;
                }
                final String fullImageURL = convertThumbnailUrlToFullsize(thumbnailURL);
                if (fullImageURL.equals(thumbnailURL)) {
                    logger.warning("Can't fix thumbnail URL: " + thumbnailURL);
                }
                final DownloadLink image = createDownloadlink(DirectHTTP.createURLForThisPlugin(fullImageURL));
                image.setAvailable(true);
                ret.add(image);
            }
        } else {
            final String[] photoURLsRelative = br.getRegex("href=\"(/photo/[A-Za-z0-9]+/[\\w\\-]+/)").getColumn(0);
            int photoIndex = 1;
            int padLength = StringUtils.getPadLength(photoURLsRelative.length);
            for (final String photoURLRelative : photoURLsRelative) {
                if (!dupes.add(photoURLRelative)) {
                    continue;
                }
                final String[] urlParts = photoURLRelative.split("/");
                final String photoTitle = urlParts[urlParts.length - 1];
                final DownloadLink image = createDownloadlink(br.getURL(photoURLRelative).toString());
                final String thumbnailURL = br.getRegex("href=\"" + Pattern.quote(photoURLRelative) + "\"[^>]*>\\s*<img id=\"t\\d+\" src=\"(https?://[^\"]+)").getMatch(0);
                String ext = null;
                if (thumbnailURL != null) {
                    /* Save directurl as property so later on we can start downloading instantly. */
                    final String fullImageURL = convertThumbnailUrlToFullsize(thumbnailURL);
                    if (fullImageURL.equals(thumbnailURL)) {
                        logger.warning("Can't fix thumbnail URL: " + thumbnailURL);
                    } else {
                        image.setProperty(EPornerCom.PROPERTY_DIRECTURL, fullImageURL);
                        ext = Plugin.getFileNameExtensionFromURL(fullImageURL);
                    }
                }
                if (ext == null) {
                    ext = ".jpg";
                }
                image.setFinalFileName(StringUtils.formatByPadLength(padLength, photoIndex++) + "_" + photoTitle + ext);
                image.setAvailable(true);
                ret.add(image);
            }
        }
        final String albumDescription = br.getRegex("class=\"video-description\">([^<]+)").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(gallerySlug).replace("-", " ").trim());
        if (!StringUtils.isEmpty(albumDescription)) {
            fp.setComment(Encoding.htmlDecode(albumDescription).trim());
        }
        fp.addLinks(ret);
        if (ret.size() < numberofPhotos) {
            logger.warning("Failed to find some photos or website contained duplicates");
        }
        return ret;
    }

    private static String convertThumbnailUrlToFullsize(final String thumbnailURL) {
        String fullImageURL = thumbnailURL.replaceFirst("_\\d+x\\d+.jpg", ".jpg");
        fullImageURL = fullImageURL.replaceFirst("\\d+x\\d+\\.gif$", ".mp4");
        return fullImageURL;
    }
}
