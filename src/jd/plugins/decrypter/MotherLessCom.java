//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "motherless.com" }, urls = { "https?://(?:www\\.|members\\.)?motherless\\.com/(g(i|v)?/[\\w\\-_]+/[A-Z0-9]{7}|[A-Z0-9]{6,9}(/[A-Z0-9]{7})?)|https?://(?:www\\.)?motherless\\.com/f/[^/]+/(?:images|videos|galleries)" })
public class MotherLessCom extends PluginForDecrypt {
    private String      fpName = null;
    private FilePackage fp;

    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // DEV NOTES:
    //
    // REGEX run down, allows the following
    // motherless.com/g/WHAT_EVER_HERE/UID
    // motherless.com/UID1/UID
    // motherless.com/UID
    //
    // - don't support: groups(/g[a-z]{1}/), images or videos they can have over 1000 pages
    // - supports spanning pages and galleries with submenu (MORE links).
    // - set same User-Agent from hoster plugin, making it harder to distinguish.
    // - Server issues can return many 503's in high load situations.
    // - Server also punishes user who downloads with too many connections. This is a linkchecking issue also, as grabs info from headers.
    // - To reduce server loads associated with linkchecking, I've set 'setAvailable(true) for greater than 5 pages.
    private static final String TYPE_INVALID           = "https?://(?:www\\.)?(members\\.)?motherless\\.com/(?:privacy|popular|register|premium|members|galleries|contact).*?";
    private static final String TYPE_FAVOURITES_VIDEOS = "https?://(?:www\\.)?motherless\\.com/f/[^/]+/videos";
    private static final String TYPE_FAVOURITES_ALL    = ".+/f/([^/]+)/(.+)";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(DownloadLink e) {
                if (fp != null) {
                    fp.add(e);
                }
                distribute(e);
                return super.add(e);
            }
        };
        br.setLoadLimit(4194304);
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 503 });
        JDUtilities.getPluginForHost(this.getHost());
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MotherLessCom.ua);
        // alters 'domain/(g/name/)uid' by removing all but uid
        String parameter = param.toString().replaceAll("motherless\\.com/g/[\\w\\-]+/", "motherless.com/");
        if (parameter.matches(TYPE_INVALID)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (jd.plugins.hoster.MotherLessCom.isDownloadPremiumOnly(br)) {
            final DownloadLink dl = createDownloadlink(parameter.replace("motherless.com/", "motherlessvideos.com/"));
            dl.setProperty("dltype", "registered");
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML("class=\"red-pill-button rounded-corners-r5\">Reply</a>")) {
            logger.info("This is a forum link without any downloadable content: " + parameter);
            return decryptedLinks;
        } else if (br.containsHTML(jd.plugins.hoster.MotherLessCom.html_contentFriendsOnly)) {
            logger.warning("Unsupported format for " + parameter);
            return decryptedLinks;
        }
        // Common bug: It can happen that the texts that we use to differ between the kinds of links change so the decrypter breaks down,
        // always check that first!
        else if (br.containsHTML(jd.plugins.hoster.MotherLessCom.html_contentSubscriberImage) && jd.plugins.hoster.MotherLessCom.isWatchSubscriberPremiumOnly(br)) {
            final DownloadLink dl = createDownloadlink(parameter.replace("motherless", "premiummotherlesspictures"));
            dl.setContentUrl(parameter);
            dl.setProperty("dltype", "image");
            dl.setProperty("onlyregistered", "true");
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML(jd.plugins.hoster.MotherLessCom.html_contentSubscriberVideo) && (jd.plugins.hoster.MotherLessCom.isWatchSubscriberPremiumOnly(br) && br.containsHTML(jd.plugins.hoster.MotherLessCom.html_contentSubscriberVideo))) {
            final DownloadLink dl = createDownloadlink(parameter.replace("motherless.com/", "motherlessvideos.com/"));
            dl.setContentUrl(parameter);
            dl.setProperty("dltype", "video");
            dl.setProperty("onlyregistered", "true");
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(jd.plugins.hoster.MotherLessCom.html_OFFLINE) || br.containsHTML("<img src=\"/images/icons.*/exclamation\\.png\" style=\"margin-top: -5px;\" />[\t\n\r ]+404") || this.br.getHttpConnection().getResponseCode() == 503) {
            // this can have text which could be contained in previous if statements... has to be last!
            final DownloadLink dl = createDownloadlink(parameter.replace("motherless.com/", "motherlessvideos.com/"));
            dl.setContentUrl(parameter);
            dl.setProperty("dltype", "offline");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_FAVOURITES_ALL)) {
            fpName = this.br.getRegex("<title>([^<>\"]*?)\\- MOTHERLESS\\.COM</title>").getMatch(0);
            gallery(decryptedLinks, parameter, progress);
        } else {
            String[] SubGal = br.getRegex("<a href=\"(/[A-Z0-9]+)\" title=\"More [^ ]+ in this gallery\" class=\"pop plain more\">See More &raquo;</a>").getColumn(0);
            if (SubGal != null && SubGal.length != 0) {
                for (String subuid : SubGal) {
                    br.getPage("://motherless.com" + subuid);
                    gallery(decryptedLinks, parameter, progress);
                }
                return decryptedLinks;
            }
            if (isVideo(br)) {
                final DownloadLink dlink = createDownloadlink(parameter.replace("motherless.com/", "motherlessvideos.com/"));
                dlink.setContentUrl(parameter);
                if (br.containsHTML(jd.plugins.hoster.MotherLessCom.html_notOnlineYet)) {
                    jd.plugins.hoster.MotherLessCom.notOnlineYet(dlink, false, false);
                    dlink.setAvailable(false);
                }
                dlink.setProperty("dltype", "video");
                dlink.setName(new Regex(parameter, "motherless\\.com/(.+)").getMatch(0));
                decryptedLinks.add(dlink);
            } else if (!br.containsHTML("<strong>Uploaded</strong>")) {
                gallery(decryptedLinks, parameter, progress);
            } else {
                final DownloadLink fina = createDownloadlink(parameter.replace("motherless.com/", "motherlesspictures.com/"));
                fina.setContentUrl(parameter);
                fina.setProperty("dltype", "image");
                decryptedLinks.add(fina);
            }
        }
        return decryptedLinks;
    }

    public static final boolean isVideo(final Browser br) {
        return br.containsHTML("(mediatype\\s*=\\s*'video'|" + jd.plugins.hoster.MotherLessCom.html_notOnlineYet + ")");
    }

    // finds the uid within the grouping
    private String formLink(String singlelink) {
        if (singlelink.startsWith("/")) {
            singlelink = "https://" + this.getHost() + singlelink;
        }
        String ID = new Regex(singlelink, "https?://motherless\\.com/[A-Z0-9]+/([A-Z0-9]+)").getMatch(0);
        if (ID != null) {
            singlelink = "https://" + this.getHost() + "/" + ID;
        }
        return singlelink;
    }

    /** Supports all kinds of multiple page urls that motherless.com has. */
    @SuppressWarnings("deprecation")
    private void gallery(ArrayList<DownloadLink> ret, String parameter, ProgressController progress) throws IOException {
        String relative_path = null;
        ArrayList<String> dupes = new ArrayList<String>();
        if (parameter.matches(TYPE_FAVOURITES_ALL)) {
            final String username = new Regex(parameter, TYPE_FAVOURITES_ALL).getMatch(0);
            /* images or videos */
            final String media_type = new Regex(parameter, TYPE_FAVOURITES_ALL).getMatch(1);
            fpName = "Favorites of " + username + " - " + media_type;
            relative_path = username + "/" + media_type;
        } else {
            if (fpName == null) {
                fpName = br.getRegex("<title>MOTHERLESS\\.COM - Go Ahead She Isn't Looking!\\s*:(.*?)</title>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<div class=\"member-bio-username\">.*?'s Gallery &bull; (.*?)</div>").getMatch(0);
                    if (fpName == null) {
                        fpName = br.getRegex("<title>([^<>\"]*?) -\\s*MOTHERLESS\\.COM</title>").getMatch(0);
                    }
                }
            }
            if (fpName == null) {
                /* Try to fallback to ID */
                fpName = new Regex(parameter, "https?://[^/]+/(.+)").getMatch(0);
            }
        }
        if (fpName != null) {
            fpName = fpName.trim();
            fp = FilePackage.getInstance();
            fp.setName(fpName);
        }
        String GM = br.getRegex("<a href=\"(/GM\\w+)\"").getMatch(0); // Gallery Mixed
        if (GM != null) {
            br.getPage(GM);
        }
        // grabs final page as count.
        String totalpages = br.getRegex("<a href=\"/[A-Z0-9]{9}\\?page=\\d+\"[^>]+>(\\d+)</a><a href=\"/[A-Z0-9]{9}\\?page=\\d+\"[^>]+>NEXT").getMatch(0);
        if (totalpages == null) {
            totalpages = br.getRegex("<a href=\"/[A-Z0-9]{9}\\?page=(\\d+)\"[^>]+>\\d+</a><a href=\"/[A-Z0-9]{9}\\?page=\\d+\"[^>]+>NEXT").getMatch(0);
        }
        if (totalpages == null) {
            /* Wide open RegEx */
            totalpages = br.getRegex("<a href=\"/[^<>\"]+\\?page=\\d+\"[^>]+>(\\d+)</a><a href=\"/[^<>\"]+\\?page=\\d+\"[^>]+>NEXT").getMatch(0);
        }
        if (totalpages == null) {
            totalpages = "1";
        }
        int numberOfPages = Integer.parseInt(totalpages);
        logger.info("Found " + numberOfPages + " page(s), decrypting now...");
        for (int i = 1; i <= numberOfPages; i++) {
            // stupid site jumps URLS for NextPage depending on parameter
            String nextPage = br.getRegex("<a href=\"(/[A-Z0-9]{7,9}\\?page=\\d+)\"[^>]+>NEXT").getMatch(0);
            if (nextPage == null) {
                /* Wide open RegEx */
                nextPage = br.getRegex("<a href=\"(/[^<>\"]+\\?page=\\d+)\"[^>]+>NEXT").getMatch(0);
            }
            if (parameter.contains("/galleries")) {
                /*
                 * 2020-01-21: Add all favorite galleries of a user. These will go back into the decrypter to crawl the picture/video
                 * content.
                 */
                /* Do not set packagename here. Each gallery should go into a separate package. */
                fp = null;
                final String[] galleries = br.getRegex("data-gallery-codename=\"([A-Z0-9]+)").getColumn(0);
                if (galleries == null || galleries.length == 0) {
                    return;
                }
                for (final String galleryID : galleries) {
                    ret.add(this.createDownloadlink("https://" + this.getHost() + "/G" + galleryID));
                }
            } else {
                String[] videolinks = br.getRegex("<a href=\"(/[^\"]+)\" class=\"img-container\" target=\"_self\">\\s*<span class=\"currently-playing-icon\"").getColumn(0);
                if (videolinks != null && videolinks.length != 0) {
                    for (String singlelink : videolinks) {
                        final String contentID = new Regex(singlelink, "/g/.*?/([A-Z0-9]+$)").getMatch(0);
                        if (contentID != null) {
                            singlelink = "https://" + this.getHost() + "/" + contentID;
                        }
                        singlelink = formLink(singlelink);
                        final DownloadLink dl = createDownloadlink(singlelink.replace("motherless.com/", "motherlessvideos.com/"));
                        dl.setContentUrl(singlelink);
                        dl.setProperty("dltype", "video");
                        // fast add.
                        dl.setAvailable(true);
                        if (relative_path != null) {
                            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, relative_path);
                        }
                        dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                        ret.add(dl);
                    }
                }
                String[] picturelinks = br.getRegex("<a href=\"(?:https?://[^/]+)?(/[a-zA-Z0-9]+){1,2}\"[^>]*class=\"img-container\"").getColumn(0);
                if (picturelinks != null && picturelinks.length != 0) {
                    logger.info("Decrypting page " + i + " which contains " + picturelinks.length + " links.");
                    for (String singlelink : picturelinks) {
                        final String contentID = new Regex(singlelink, "/g/.*?/([A-Z0-9]+$)").getMatch(0);
                        if (!dupes.add(contentID)) {
                            /* Already added as video content --> Skip */
                            continue;
                        }
                        singlelink = formLink(singlelink);
                        final DownloadLink dl = createDownloadlink(singlelink.replace("motherless.com/", "motherlesspictures.com/"));
                        dl.setContentUrl(singlelink);
                        dl.setProperty("dltype", "image");
                        // fast add.
                        dl.setAvailable(true);
                        if (relative_path != null) {
                            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, relative_path);
                        }
                        dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
                        ret.add(dl);
                    }
                }
                if ((picturelinks == null || picturelinks.length == 0) && (videolinks == null || videolinks.length == 0)) {
                    logger.warning("Decrypter failed for link: " + parameter);
                    return;
                }
            }
            if (i != numberOfPages && nextPage != null) {
                br.getPage(nextPage);
            }
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break;
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-21: Avoid overloading the website */
        return 1;
    }
}