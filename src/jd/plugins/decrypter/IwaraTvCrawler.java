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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.IwaraTvConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
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
import jd.plugins.hoster.IwaraTv;
import jd.plugins.hoster.YoutubeDashV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class IwaraTvCrawler extends PluginForDecrypt {
    public IwaraTvCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "iwara.tv", "trollvids.com" });
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
            ret.add("https?://(?:[A-Za-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/(?:users|profile)/[^/\\?]+(/videos)?");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_USER = "https?://[^/]+/users/([^/]+)(/videos)?";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_USER)) {
            return crawlChannel(param);
        } else {
            return crawlSingleVideo(param);
        }
    }

    /** Crawls all videos of a user/channel. */
    private ArrayList<DownloadLink> crawlChannel(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String usernameSlug = new Regex(param.getCryptedUrl(), TYPE_USER).getMatch(0);
        if (usernameSlug == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // usernameSlug = URLEncode.decodeURIComponent(usernameSlug);
        br.getPage(IwaraTv.WEBAPI_BASE + "/profile/" + usernameSlug);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> user = (Map<String, Object>) entries.get("user");
        final String userid = user.get("id").toString();
        final HashSet<String> dupes = new HashSet<String>();
        dupes.add("thumbnails");
        int page = 1;
        final UrlQuery query = new UrlQuery();
        query.add("sort", "date");
        query.add("user", userid);
        /* 2021-10-11: Not all user profiles have the "/videos" URL available! */
        // final String baseURL = "https://" + Browser.getHost(param.getCryptedUrl(), true) + "/users/" + usernameSlug + "/videos";
        final String usernameForFilename = user.get("name").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(usernameForFilename);
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        do {
            query.addAndReplace("page", Integer.toString(page - 1));
            br.getPage("/videos?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries2 = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> results = (List<Map<String, Object>>) entries2.get("results");
            int foundNumberofNewItemsThisPage = 0;
            for (final Map<String, Object> result : results) {
                final String videoID = result.get("id").toString();
                if (!dupes.add(videoID) || "thumbnails".equals(videoID)) {
                    continue;
                }
                /* Assume all items are selfhosted and thus do not have to go through this crawler again. */
                final String videoURL = "https://" + br.getHost(true) + "/video/" + videoID;
                /*
                 * Do not process these URLs again via hosterplugin! We know that it's selfhosted plugin thus we use use the constructor in
                 * which we can provide a PluginForHost.
                 */
                final DownloadLink dl = new DownloadLink(plg, this.getHost(), this.getHost(), videoURL, true);
                dl.setContentUrl(videoURL);
                dl.setProperty(IwaraTv.PROPERTY_VIDEOID, videoID);
                IwaraTv.parseFileInfo(dl, result);
                dl.setName(jd.plugins.hoster.IwaraTv.getFilename(dl));
                if (PluginJsonConfig.get(IwaraTvConfig.class).isProfileCrawlerEnableFastLinkcheck()) {
                    dl.setAvailable(true);
                }
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
                foundNumberofNewItemsThisPage++;
            }
            final int count = ((Number) entries2.get("count")).intValue();
            final int limit = ((Number) entries2.get("limit")).intValue();
            logger.info("Crawled page " + page + " | Found items on this page: " + foundNumberofNewItemsThisPage + " | Total so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (foundNumberofNewItemsThisPage == 0) {
                logger.info("Stopping because: Failed to find any items on current page");
                if (ret.isEmpty()) {
                    /* No items have been found before -> Looks like profile is empty. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    break;
                }
            } else if (count >= limit) {
                logger.info("Stopping because: Reached last page: " + page);
                break;
            } else {
                page++;
            }
        } while (true);
        return ret;
    }

    private ArrayList<DownloadLink> crawlSingleVideo(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
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
            ret.add(createDownloadlink(externID));
            return ret;
        }
        externID = br.getRegex("\"(?:https?:)?//(?:www\\.)?youtube(?:\\-nocookie)?\\.com/embed/([^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            ret.add(createDownloadlink(YoutubeDashV2.generateContentURL(externID)));
            return ret;
        }
        final String[] images = br.getRegex("class=\"field-item even\"><a href=\"([^<>\"]+/files/photos/imported/[^<>\"]+)\"").getColumn(0);
        if (images.length > 0) {
            /* 2020-04-20: New: Images */
            for (String image : images) {
                if (image.startsWith("//")) {
                    image = "https:" + image;
                }
                final DownloadLink dl = this.createDownloadlink(image);
                ret.add(dl);
            }
        } else {
            final String source_html = br.getRegex("<div class=\"watch_left\">(.*?)<div class=\"rating_container\">").getMatch(0);
            if (source_html != null) {
                externID = new Regex(source_html, "\"(https?[^<>\"]*?)\"").getMatch(0);
                if (externID != null) {
                    ret.add(createDownloadlink(externID));
                    return ret;
                }
            }
        }
        if (ret.isEmpty()) {
            /* Looks like content is selfhosted. */
            if (errorDuringAvailablecheck != null) {
                /* Most likely content is offline */
                throw errorDuringAvailablecheck;
            }
            selfhostedVideo.setAvailable(true);
            ret.add(selfhostedVideo);
            return ret;
        } else {
            /* Looks like we found embedded content. */
            return ret;
        }
    }
}
