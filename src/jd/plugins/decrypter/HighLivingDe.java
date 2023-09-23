//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hi-living.de" }, urls = { "https?://(?:www\\.)?hi\\-living\\.de/.+" })
public class HighLivingDe extends PluginForDecrypt {
    public HighLivingDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] albumIDs = br.getRegex("wppa-album=(\\d+)").getColumn(0);
        String albumID = null;
        if (albumIDs != null && albumIDs.length > 0) {
            /* Find correct albumID. */
            final HashMap<String, Integer> albumidcounter = new HashMap<String, Integer>();
            for (final String thisAlbumID : albumIDs) {
                if (!albumidcounter.containsKey(thisAlbumID)) {
                    albumidcounter.put(thisAlbumID, 1);
                } else {
                    albumidcounter.put(thisAlbumID, albumidcounter.get(thisAlbumID) + 1);
                }
            }
            int highestNumberOfOccurences = 0;
            for (final String thisAlbumID : albumIDs) {
                final int numberofOccurences = albumidcounter.get(thisAlbumID);
                if (numberofOccurences > highestNumberOfOccurences) {
                    highestNumberOfOccurences = numberofOccurences;
                    albumID = thisAlbumID;
                }
            }
        }
        final String postID = br.getRegex("wppa-fromp=(\\d+)").getMatch(0);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final HashSet<String> dupes = new HashSet<String>();
        String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = br._getURL().getPath();
        }
        title = Encoding.htmlDecode(title).trim();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        if (albumID != null) {
            fp.setPackageKey("hilivingde://album/" + albumID);
        }
        fp.addLinks(ret);
        int page = 1;
        do {
            final ArrayList<DownloadLink> newItemsThisPage = new ArrayList<DownloadLink>();
            final String[] imageurls = br.getRegex("(https?://[^/]+/wp-content/uploads/wppa/\\d+/[^\\'\"]*\\.jpg)").getColumn(0);
            for (final String imageurl : imageurls) {
                if (!dupes.add(imageurl)) {
                    continue;
                }
                final DownloadLink image = this.createDownloadlink(imageurl);
                newItemsThisPage.add(image);
            }
            final String[] thumbnailurls = br.getRegex("(https?://[^/]+/wp-content/uploads/wppa/thumbs/\\d+/[^\\'\"]*\\.jpg)").getColumn(0);
            if (thumbnailurls != null && thumbnailurls.length != 0) {
                int newThumbnailItems = 0;
                for (final String thumbnailurl : thumbnailurls) {
                    final String fullimageurl = thumbnailurl.replaceFirst("(?i)/wppa/thumbs/", "/wppa/");
                    if (!dupes.add(fullimageurl)) {
                        continue;
                    }
                    final DownloadLink image = this.createDownloadlink(fullimageurl);
                    ret.add(image);
                    newThumbnailItems++;
                    newItemsThisPage.add(image);
                }
                logger.info("Number of additional items found via thumbnailURL -> fullsizeURL: " + newThumbnailItems);
            }
            logger.info("Crawled page " + "" + " | Found items so far: " + ret.size());
            for (final DownloadLink result : newItemsThisPage) {
                result.setAvailable(true);
                result._setFilePackage(fp);
                distribute(result);
                ret.add(result);
            }
            if (newItemsThisPage.isEmpty()) {
                logger.info("Stopping because: Failed to find any new items on page " + page);
                break;
            } else if (albumID == null) {
                logger.info("Stopping because: Pagination impossible because albumID was not found in html");
                break;
            } else if (postID == null) {
                logger.info("Stopping because: Pagination impossible because postID was not found in html");
                break;
            } else if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else {
                /* Crawl next page */
                page++;
                if (!br.containsHTML("wppa-page=" + page)) {
                    logger.info("Stopping because: Reached end?");
                    break;
                }
                br.getPage("/wp-admin/admin-ajax.php?action=wppa&wppa-action=render&wppa-size=640&wppa-moccur=1&wppa-fromp=" + postID + "&lang=de&cover=0&album=" + albumID + "&occur=1&wppa-page=" + page + "&lang=de&resp=1");
            }
        } while (!this.isAbort());
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}