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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.BdsmlrCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { BdsmlrCom.class })
public class BdsmlrComCrawler extends PluginForDecrypt {
    public BdsmlrComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(jd.plugins.hoster.BdsmlrCom.getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(jd.plugins.hoster.BdsmlrCom.getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(jd.plugins.hoster.BdsmlrCom.getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://[\\w\\-]+\\." + buildHostsPatternPart(domains) + "/(?:post/\\d+)?$");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_USER_PROFILE = "https?://([\\w\\-]+)\\.[^/]+/?$";
    private static final String TYPE_POST         = "https?://([\\w\\-]+)\\.[^/]+/post/(\\d+)$";
    private static final String PROPERTY_POST_ID  = "post_id";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null) {
            final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.BdsmlrCom) hostPlugin).login(acc, false);
        }
        if (param.getCryptedUrl().matches(TYPE_USER_PROFILE)) {
            return crawlUser(param);
        } else {
            return crawlPost(param);
        }
    }

    private ArrayList<DownloadLink> crawlPost(final CryptedLink param) throws IOException, PluginException {
        final String username = new Regex(param.getCryptedUrl(), TYPE_POST).getMatch(0);
        final String postID = new Regex(param.getCryptedUrl(), TYPE_POST).getMatch(1);
        if (username == null) {
            /* Developer mistake! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setAllowedResponseCodes(new int[] { 500 });
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username + "_" + postID);
        return crawlPosts(br, fp);
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param) throws IOException, PluginException {
        final String username = new Regex(param.getCryptedUrl(), TYPE_USER_PROFILE).getMatch(0);
        if (username == null) {
            /* Developer mistake! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)This blog doesn't exist\\.\\s*<br>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        /* First check if there is already some downloadable content in the html of the current page. */
        ret.addAll(crawlPosts(br, fp));
        if (ret.isEmpty()) {
            logger.info("Didn't find anything in HTML ");
        }
        final String csrftoken = br.getRegex("name=\"csrf-token\" content=\"([^\"]+)\"").getMatch(0);
        if (csrftoken == null) {
            if (br.containsHTML("\"sorry\"\\s*>\\s*Sorry, please login")) {
                throw new AccountRequiredException();
            }
            logger.warning("Pagination failed");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String infinitescrollDate = br.getRegex("class=\"infinitescroll\" data-time=\"(\\d{4}[^\"]+)\"").getMatch(0);
        if (infinitescrollDate == null) {
            logger.info("Stopping because: Pagination parameter 'infinitescroll' is not available");
            return ret;
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Origin", "https://" + username + "." + this.getHost());
        br.getHeaders().put("Referer", "https://" + username + "." + this.getHost() + "/");
        br.getHeaders().put("X-CSRF-TOKEN", csrftoken);
        br.postPage("/loadfirst", "scroll=5&timenow=" + Encoding.urlEncode(infinitescrollDate));
        ret.addAll(crawlPosts(br, fp));
        final HashSet<String> dupes = new HashSet<String>();
        final int maxItemsPerPage = 20;
        int index = 0;
        int page = 1;
        profileLoop: do {
            final UrlQuery query = new UrlQuery();
            query.add("scroll", Integer.toString(index));
            query.add("timenow", Encoding.urlEncode(infinitescrollDate));
            query.add("last", lastPostID);
            br.postPage("/infinitepb2/" + username, query);
            final ArrayList<DownloadLink> results = crawlPosts(br, fp);
            int numberofNewItems = 0;
            int numberofSkippedDuplicates = 0;
            if (results.isEmpty()) {
                logger.info("Failed to find any results on current page -> Probably it only contains offline items or text content");
            } else {
                for (final DownloadLink result : results) {
                    final String postID = result.getStringProperty(PROPERTY_POST_ID);
                    if (!dupes.add(postID)) {
                        /**
                         * 2023-03-31: This should never happen but it looks like it can happen. </br>
                         * As long as the current page we're crawling contains at least one new item, the crawler will continue even if
                         * there were some dupes.
                         */
                        logger.info("Skipping dupe: " + postID);
                        numberofSkippedDuplicates++;
                        continue;
                    } else {
                        ret.add(result);
                        numberofNewItems++;
                    }
                }
            }
            logger.info("Crawled page " + page + " | Index: " + index + " | New crawled items on this page: " + numberofNewItems + " | Crawled supported items total: " + ret.size() + " | lastPostID: " + lastPostID);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (lastPostID == null) {
                logger.info("Stopping because: lastPostID is null");
                break profileLoop;
            } else if (numberofNewItems == 0) {
                if (numberofSkippedDuplicates > 0) {
                    logger.info("Stopping because: Current page contained ONLY duplicates");
                    break profileLoop;
                } else {
                    logger.info("Current page contained ONLY deleted posts or unsupported posts which have been skipped: " + lastNumberofPosts);
                }
            }
            index += maxItemsPerPage;
            page++;
        } while (true);
        return ret;
    }

    private String            lastPostID        = null;
    private int               lastNumberofPosts = 0;
    private ArrayList<String> lastPostIDList    = new ArrayList<String>();

    private ArrayList<DownloadLink> crawlPosts(final Browser br, final FilePackage fp) throws PluginException, IOException {
        lastPostID = null;
        lastNumberofPosts = 0;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] posts = br.getRegex("(<div class=\"wrap-post del\\d+\\s*(?:pubvideo|typeimage|pubimage|typetext)\\s*\">.*?class=\"countinf\")").getColumn(0);
        for (final String post : posts) {
            final Regex postInfo = new Regex(post, "(https?://([\\w\\-]+)\\.[^/]+/post/(\\d+))");
            String postID = postInfo.getMatch(2);
            if (postID == null) {
                postID = new Regex(post, "<div class=\\\"wrap-post del(\\d+)").getMatch(0);
            }
            lastPostID = postID;
            final String type = new Regex(post, "<div class=\"wrap-post del\\d+\\s*([a-z]+)").getMatch(0);
            if (!type.matches("pubvideo|typeimage|pubimage")) {
                logger.info("Skipping unsupported post type " + type + " | ID: " + postID);
                continue;
            }
            String username = postInfo.getMatch(1);
            if (username == null) {
                username = new Regex(br.getURL(), "https?://(.*?)\\.bdsmlr\\.com").getMatch(0);
            }
            String postURL = postInfo.getMatch(0);
            if (postURL == null && StringUtils.isAllNotEmpty(postID, username)) {
                postURL = br.getURL("/post/" + postID).toString();
            }
            if (postURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Regex matches;
            /* Video posts will also contain URLs to video-thumbnails so let's make sure we only grab exactly what we want. */
            if ("pubvideo".equalsIgnoreCase(type)) {
                matches = new Regex(post, "(?:\"|\\')(https?://[^/]+/uploads/videos/(\\d{4})/(\\d{2})[^\"\\']+\\.mp4)(?:\"|\\')");
            } else {
                matches = new Regex(post, "(?:\"|\\')(https?://[^/]+/uploads/photos/(\\d{4})/(\\d{2})[^\"\\']+\\.[a-zA-Z0-9]{2,5})(?:\"|\\')");
            }
            if (!matches.matches()) {
                logger.warning("Failed to find any media for post: " + postURL + " type:" + type);
                continue;
            }
            final String[][] directs = matches.getMatches();
            final HashSet<String> dups = new HashSet<String>();
            for (final String direct[] : directs) {
                final String directurl = direct[0];
                if (dups.add(directurl)) {
                    final String year = direct[1];
                    final String month = direct[2];
                    final DownloadLink dl = this.createDownloadlink(directurl);
                    if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        dl.setContentUrl(directurl);
                    } else {
                        dl.setContentUrl(postURL);
                    }
                    if (dups.size() > 1) {
                        dl.setFinalFileName(username + "_" + year + "_" + month + "_" + postID + "_" + dups.size() + Plugin.getFileNameExtensionFromURL(directurl));
                    } else {
                        dl.setFinalFileName(username + "_" + year + "_" + month + "_" + postID + Plugin.getFileNameExtensionFromURL(directurl));
                    }
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                    dl.setAvailable(true);
                    dl.setProperty(PROPERTY_POST_ID, postID);
                    ret.add(dl);
                    distribute(dl);
                }
            }
        }
        lastNumberofPosts = posts.length;
        lastPostIDList.add(this.lastPostID);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* 2023-03-31: Debug statement to easily compare pagination IDs of plugin vs. browser. */
            logger.info("lastPostIDList: " + lastPostIDList);
        }
        return ret;
    }
}
