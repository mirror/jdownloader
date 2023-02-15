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

import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.IwaraTvConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.YoutubeDashV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "iwara.tv" }, urls = { "https?://(?:[A-Za-z0-9]+\\.)?(?:trollvids\\.com|iwara\\.tv)/((?:videos|node)/[A-Za-z0-9]+|users/[^/\\?]+(/videos)?)" })
public class IwaraTvCrawler extends PluginForDecrypt {
    public IwaraTvCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_USER = "https?://[^/]+/users/([^/]+)(/videos)?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Try to make old URLs work too. */
        param.setCryptedUrl(param.getCryptedUrl().replace("trollvids.com/", "iwara.tv/"));
        if (param.getCryptedUrl().matches(TYPE_USER)) {
            return crawlChannel(param);
        } else {
            return crawlSingleVideo(param);
        }
    }

    /** Crawls all videos of a user/channel. */
    private ArrayList<DownloadLink> crawlChannel(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String usernameSlug = new Regex(param.getCryptedUrl(), TYPE_USER).getMatch(0);
        if (usernameSlug == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean firstRequestHasAlreadyBeenDone = false;
        if (param.getCryptedUrl().matches("https?://[^/]+/users/[^/]+$")) {
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Change "/user/<usernameSlug>" url to "/user/<usernameSlug>/videos" if the user has a certain amount of videos. */
            final String viewMore = br.getRegex("class\\s*=\\s*\"more-link\"\\s*>\\s*<a href\\s*=\\s*\"(/users/[^/]+/videos).*\"").getMatch(0);
            if (viewMore != null) {
                logger.info("Seems like the user has videos spread over multiple pages -> Using: " + viewMore);
                param.setCryptedUrl(br.getURL(viewMore).toString());
            } else {
                logger.info("Seems like user only has one page of videos");
                /* We do not need to do this request again below! */
                firstRequestHasAlreadyBeenDone = true;
            }
        }
        final HashSet<String> dupes = new HashSet<String>();
        dupes.add("thumbnails");
        int page = 1;
        final UrlQuery query = new UrlQuery();
        query.add("language", "en");
        /* 2021-10-11: Not all user profiles have the "/videos" URL available! */
        // final String baseURL = "https://" + Browser.getHost(param.getCryptedUrl(), true) + "/users/" + usernameSlug + "/videos";
        final String usernameForFilename;
        String usernameReal = br.getRegex("href=\"/messages/new\\?user=([^\"]+)\"").getMatch(0);
        if (usernameReal != null) {
            usernameReal = URLEncode.decodeURIComponent(usernameReal);
            usernameForFilename = usernameReal;
        } else {
            usernameForFilename = usernameSlug;
        }
        final String baseURL = param.getCryptedUrl();
        usernameSlug = URLEncode.decodeURIComponent(usernameSlug);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(usernameForFilename);
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        do {
            if (page > 1 || !firstRequestHasAlreadyBeenDone) {
                /* Website starts page-counting at 0. */
                query.addAndReplace("page", Integer.toString(page - 1));
                br.getPage(baseURL + "?" + query.toString());
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] videoIDs = br.getRegex("/videos/([A-Za-z0-9]+)").getColumn(0);
            int foundNumberofNewItemsThisPage = 0;
            for (final String videoID : videoIDs) {
                if (!dupes.add(videoID) || "thumbnails".equals(videoID)) {
                    continue;
                }
                /* Assume all items are selfhosted and thus do not have to go through this crawler again. */
                final String videoURL = "https://" + br.getHost(true) + "/videos/" + videoID;
                /*
                 * Do not process these URLs again via hosterplugin! We know that it's selfhosted plugin thus we use use the constructor in
                 * which we can provide a PluginForHost.
                 */
                final DownloadLink dl = new DownloadLink(plg, this.getHost(), this.getHost(), videoURL, true);
                dl.setContentUrl(videoURL);
                String videoTitle = br.getRegex("<a\\s*href\\s*=\\s*\"/videos/" + videoID + "[^\"]*\"\\s*>\\s*<img[^>]*?title\\s*=\\s*\"([^\"]+).*?</a>\\s*</div>").getMatch(0);
                if (videoTitle == null) {
                    videoTitle = br.getRegex("/videos/" + videoID + "[^\"]*\">([^<>\"]+)</a></h3>").getMatch(0);
                }
                if (videoTitle != null) {
                    videoTitle = Encoding.htmlOnlyDecode(Encoding.htmlOnlyDecode(videoTitle));
                    if (videoTitle != null) {
                        videoTitle = videoTitle.trim().replaceAll("([\\(\\s\\._]+)$", "");
                        dl.setProperty(jd.plugins.hoster.IwaraTv.PROPERTY_TITLE, videoTitle);
                    }
                }
                dl.setProperty(jd.plugins.hoster.IwaraTv.PROPERTY_VIDEOID, videoID);
                dl.setProperty(jd.plugins.hoster.IwaraTv.PROPERTY_USER, usernameForFilename);
                dl.setName(jd.plugins.hoster.IwaraTv.getFilename(dl));
                if (PluginJsonConfig.get(IwaraTvConfig.class).isProfileCrawlerEnableFastLinkcheck()) {
                    dl.setAvailable(true);
                }
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                foundNumberofNewItemsThisPage++;
            }
            logger.info("Crawled page " + page + " | Found items on this page: " + foundNumberofNewItemsThisPage + " | Total so far: " + decryptedLinks.size());
            final boolean nextPageAvailable = br.containsHTML("page=" + page);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (foundNumberofNewItemsThisPage == 0) {
                logger.info("Stopping because: Failed to find any items on current page");
                if (decryptedLinks.isEmpty()) {
                    /* No items have been found before -> Looks like profile is empty. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    break;
                }
            } else if (!nextPageAvailable) {
                logger.info("Stopping because: Reached last page");
                break;
            }
            page++;
        } while (true);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlSingleVideo(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        final DownloadLink selfhostedVideo = this.createDownloadlink(param.getCryptedUrl());
        PluginException errorDuringAvailablecheck = null;
        try {
            ((jd.plugins.hoster.IwaraTv) hostPlugin).requestFileInformation(selfhostedVideo, account, false);
        } catch (final PluginException e) {
            errorDuringAvailablecheck = e;
        }
        String externID = br.getRegex("\"(https?://docs\\.google\\.com/file/d/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(?:https?:)?//(?:www\\.)?youtube(?:\\-nocookie)?\\.com/embed/([^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(YoutubeDashV2.generateContentURL(externID)));
            return decryptedLinks;
        }
        final String[] images = br.getRegex("class=\"field-item even\"><a href=\"([^<>\"]+/files/photos/imported/[^<>\"]+)\"").getColumn(0);
        if (images.length > 0) {
            /* 2020-04-20: New: Images */
            for (String image : images) {
                if (image.startsWith("//")) {
                    image = "https:" + image;
                }
                final DownloadLink dl = this.createDownloadlink(image);
                decryptedLinks.add(dl);
            }
        } else {
            final String source_html = br.getRegex("<div class=\"watch_left\">(.*?)<div class=\"rating_container\">").getMatch(0);
            if (source_html != null) {
                externID = new Regex(source_html, "\"(https?[^<>\"]*?)\"").getMatch(0);
                if (externID != null) {
                    decryptedLinks.add(createDownloadlink(externID));
                    return decryptedLinks;
                }
            }
        }
        if (decryptedLinks.isEmpty()) {
            /* Looks like content is selfhosted. */
            if (errorDuringAvailablecheck != null) {
                /* Most likely content is offline */
                throw errorDuringAvailablecheck;
            }
            selfhostedVideo.setAvailable(true);
            decryptedLinks.add(selfhostedVideo);
            return decryptedLinks;
        } else {
            /* Looks like we found embedded content. */
            return decryptedLinks;
        }
    }
}
