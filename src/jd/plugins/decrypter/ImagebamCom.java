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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

import org.jdownloader.plugins.controller.LazyPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ImagebamCom extends PluginForDecrypt {
    public ImagebamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "imagebam.com" });
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
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:image|gallery)/[a-z0-9]+";
            regex += "|" + "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/view/[A-Z0-9]+";
            regex += "|" + "https?://thumbs\\d+\\." + buildHostsPatternPart(domains) + "/\\d+/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+\\.[a-z]{3,5}";
            regex += "|" + "https?://thumbs\\d+\\." + buildHostsPatternPart(domains) + "/\\d+/[a-z0-9]+/[a-z0-9]+/[A-Z0-9]+_t\\.[a-z]{3,5}";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_THUMBNAIL     = "https?://thumbs\\d+\\.[^/]+/\\d+/[a-z0-9]+/[a-z0-9]+/([a-z0-9]+)\\.[a-z]{3,5}";
    private static final String TYPE_THUMBNAIL_NEW = "https?://thumbs\\d+\\.[^/]+/\\d+/[a-z0-9]+/[a-z0-9]+/([A-Z0-9]+)_t\\.[a-z]{3,5}";
    private static final String TYPE_IMAGE         = "https?://(?:www\\.)?[^/]+/image/([a-z0-9]+)";
    private static final String TYPE_VIEW          = "https?://(?:www\\.)?[^/]+/view/([A-Za-z0-9]+)";
    private static final String TYPE_GALLERY       = "https?://(?:www\\.)?[^/]+/gallery/([a-z0-9]+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_THUMBNAIL)) {
            /* Rewrite thumbnail to fullImage link */
            final String id = new Regex(param.getCryptedUrl(), TYPE_THUMBNAIL).getMatch(0);
            final String newURL = "https://www." + this.getHost() + "/image/" + id;
            decryptedLinks.add(this.createDownloadlink(newURL));
            return decryptedLinks;
        } else if (param.getCryptedUrl().matches(TYPE_THUMBNAIL_NEW)) {
            /* Rewrite thumbnail to fullImage link */
            final String id = new Regex(param.getCryptedUrl(), TYPE_THUMBNAIL_NEW).getMatch(0);
            final String newURL = "https://www." + this.getHost() + "/view/" + id;
            decryptedLinks.add(this.createDownloadlink(newURL));
            return decryptedLinks;
        } else if (param.getCryptedUrl().matches(TYPE_GALLERY)) {
            return crawlGallery(param);
        } else if (param.getCryptedUrl().matches(TYPE_VIEW)) {
            return crawlGalleryNew(param);
        } else {
            /* TYPE_IMAGE */
            decryptedLinks.add(crawlSingleImage(param));
            return decryptedLinks;
        }
    }

    private ArrayList<DownloadLink> crawlGallery(final CryptedLink param) throws PluginException, IOException {
        final String galleryID = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(0);
        if (galleryID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(param.getCryptedUrl());
        errorHandling(br, param);
        if (br.containsHTML("(?i)>\\s*Continue to your image")) {
            /* Reload page */
            br.getPage(param.getCryptedUrl());
        }
        return crawlProcessGallery(param, this.br);
    }

    private Browser prepBR(final Browser br) {
        /* 2022-03-23: This will skip some "Continue to image" pages. */
        br.setCookie(this.getHost(), "nsfw_inter", "1");
        return br;
    }

    /**
     * Handles new style "gallery" URLs which can either lead to a gallery or a single image.
     *
     * @throws InterruptedException
     * @throws NumberFormatException
     */
    private ArrayList<DownloadLink> crawlGalleryNew(final CryptedLink param) throws PluginException, IOException, NumberFormatException, InterruptedException {
        final String galleryID = new Regex(param.getCryptedUrl(), TYPE_VIEW).getMatch(0);
        if (galleryID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBR(br);
        br.getPage(param.getCryptedUrl());
        errorHandling(br, param);
        if (br.containsHTML("(?i)>\\s*Continue to your image")) {
            /* Reload page */
            final boolean skipWaittime = true;
            final String waitMillisStr = br.getRegex("show\\(\\);\\s*\\},\\s*(\\d+)\\);").getMatch(0);
            if (waitMillisStr != null && !skipWaittime) {
                this.sleep(Long.parseLong(waitMillisStr), param);
            }
            br.getPage(br.getURL());
        }
        if (br.containsHTML("class=\"links gallery\"")) {
            return this.crawlProcessGallery(param, this.br);
        } else {
            /* Single image - very similar to "crawlSingleImage". */
            final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            final String finallink = br.getRegex("class=\"image-loader\"[^>]*>\\s*<img src=\"(https?://[^\"]+)\"").getMatch(0);
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String originalFilename = br.getRegex(galleryID + "\\?full=1\"[^<>]*title=\"([^\"]+)\"").getMatch(0);
            final DownloadLink direct = this.createDownloadlink(finallink);
            direct.setContentUrl(param.getCryptedUrl());
            final String filenameURL = Plugin.getFileNameFromURL(new URL(finallink));
            if (originalFilename != null) {
                direct.setFinalFileName(originalFilename);
                direct.setProperty(DirectHTTP.FIXNAME, originalFilename);
            } else if (filenameURL != null) {
                direct.setFinalFileName(filenameURL);
            }
            direct.setAvailable(true);
            decryptedLinks.add(direct);
            return decryptedLinks;
        }
    }

    private ArrayList<DownloadLink> crawlProcessGallery(final CryptedLink param, final Browser br) throws IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final boolean isNewGallery;
        String galleryID;
        if (param.getCryptedUrl().matches(TYPE_GALLERY)) {
            isNewGallery = false;
            galleryID = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(0);
        } else {
            isNewGallery = true;
            galleryID = new Regex(param.getCryptedUrl(), TYPE_VIEW).getMatch(0);
        }
        final String galleryTitle = br.getRegex("id=\"gallery-name\"[^>]*>([^<>\"]+)<").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (galleryTitle != null) {
            fp.setName(galleryID + " - " + Encoding.htmlDecode(galleryTitle).trim());
        } else {
            fp.setName(galleryID);
        }
        int page = 1;
        do {
            logger.info("Crawling page: " + page);
            boolean foundNewItems = false;
            if (isNewGallery) {
                final String links[] = br.getRegex(TYPE_VIEW).getColumn(-1);
                for (final String link : links) {
                    final String imageID = new Regex(link, TYPE_VIEW).getMatch(0);
                    /* Don't re-add previously added URL! */
                    if (imageID.equals(galleryID)) {
                        continue;
                    } else {
                        final DownloadLink dl = this.createDownloadlink(link);
                        dl._setFilePackage(fp);
                        decryptedLinks.add(dl);
                        distribute(dl);
                        foundNewItems = true;
                    }
                }
            } else {
                final String links[] = br.getRegex(TYPE_IMAGE).getColumn(-1);
                for (final String link : links) {
                    final String imageID = new Regex(link, TYPE_IMAGE).getMatch(0);
                    /* Don't re-add previously added URL! */
                    if (imageID.equals(galleryID)) {
                        continue;
                    } else {
                        final DownloadLink dl = this.createDownloadlink(link);
                        dl._setFilePackage(fp);
                        decryptedLinks.add(dl);
                        distribute(dl);
                        foundNewItems = true;
                    }
                }
            }
            final String nextPage = br.getRegex("(/[^\"]+\\?page=" + (page + 1) + ")\"").getMatch(0);
            if (this.isAbort()) {
                break;
            } else if (!foundNewItems) {
                logger.info("Stopping because: Failed to find new items on current page");
                break;
            } else if (nextPage == null) {
                logger.info("Stopping because: No nextPage given");
                break;
            } else {
                /* Next page available --> Continue crawl process */
                page += 1;
                br.getPage(nextPage);
                continue;
            }
        } while (true);
        return decryptedLinks;
    }

    private void errorHandling(Browser br, CryptedLink param) throws PluginException {
        /* Error handling */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Image not found|>\\s*Image violated our terms of service|>\\s*The requested image could not be located|>\\s*The image has been deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)The gallery you are looking for")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private DownloadLink crawlSingleImage(final CryptedLink param) throws Exception {
        final String imageID = new Regex(param.getCryptedUrl(), TYPE_IMAGE).getMatch(0);
        if (imageID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBR(br);
        br.getPage(param.getCryptedUrl());
        errorHandling(br, param);
        if (br.containsHTML("(?i)>\\s*Continue to your image")) {
            /* Reload page */
            br.getPage(param.getCryptedUrl());
        }
        String finallink = br.getRegex("('|\")(https?://\\d+\\.imagebam\\.com/download/[^<>\\s]+)\\1").getMatch(1);
        if (finallink == null) {
            finallink = br.getRegex("('|\")(https?://images\\d+\\.imagebam\\.com/[^<>\\s]+\\.(jpe?g|png))\\1").getMatch(1);
        }
        if (finallink == null) {
            throw new DecrypterException("Decrypter broken for link: " + br.getURL());
        }
        finallink = Encoding.htmlDecode(finallink);
        final DownloadLink dl = createDownloadlink(finallink);
        dl.setContentUrl(param.getCryptedUrl());
        String originalFilename = br.getRegex(imageID + "\\?full=1\"[^<>]*title=\"([^\"]+)\"").getMatch(0);
        String urlFilename = extractFileNameFromURL(finallink);
        if (urlFilename != null) {
            if (originalFilename != null && getFileNameExtensionFromString(originalFilename) != null) {
                urlFilename = originalFilename;
            }
            urlFilename = Encoding.htmlDecode(urlFilename);
            /* If has extension don't set, if hasn't extension set default one. */
            urlFilename += getFileNameExtensionFromString(urlFilename) != null ? "" : ".jpg";
            dl.setFinalFileName(urlFilename);
        }
        dl.setAvailable(true);
        return dl;
    }
}
