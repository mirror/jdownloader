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
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
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
            ret.add("https?://\\w+\\." + buildHostsPatternPart(domains) + "/(?:post/\\d+)?");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_USER_PROFILE = "https?://(\\w+)\\.[^/]+/?$";
    private static final String TYPE_POST         = "https?://(\\w+)\\.[^/]+/post/(\\d+)$";
    private static final String PROPERTY_POST_ID  = "post_id";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc == null) {
            throw new AccountRequiredException();
        }
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        ((jd.plugins.hoster.BdsmlrCom) hostPlugin).login(acc, false);
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
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)This blog doesn't exist\\.\\s*<br>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        decryptedLinks.addAll(crawlPosts(br, fp));
        if (decryptedLinks.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String csrftoken = br.getRegex("name=\"csrf-token\" content=\"([^\"]+)\"").getMatch(0);
        if (csrftoken == null) {
            logger.warning("Pagination failed");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            logger.info("Stopping because: Pagination hasn't been implemented yet");
            return decryptedLinks;
        }
        final String infinitescrollDate = br.getRegex("class=\"infinitescroll\" data-time=\"(\\d{4}[^\"]+)\"").getMatch(0);
        if (infinitescrollDate == null) {
            logger.info("Stopping because: Pagination not available");
            return decryptedLinks;
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Origin", "https://" + username + "." + this.getHost());
        br.getHeaders().put("Referer", "https://" + username + "." + this.getHost() + "/");
        br.getHeaders().put("X-CSRF-TOKEN", csrftoken);
        br.postPage("/loadfirst", "scroll=5&timenow=" + Encoding.urlEncode(infinitescrollDate));
        decryptedLinks.addAll(crawlPosts(br, fp));
        if (decryptedLinks.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final HashSet<String> dupes = new HashSet<String>();
        final int maxItemsPerPage = 20;
        int index = 0;
        int page = 1;
        String lastPostID = decryptedLinks.get(decryptedLinks.size() - 1).getStringProperty(PROPERTY_POST_ID);
        profileLoop: do {
            final UrlQuery query = new UrlQuery();
            query.add("scroll", Integer.toString(index));
            query.add("timenow", Encoding.urlEncode(infinitescrollDate));
            query.add("last", lastPostID);
            br.postPage("/infinitepb2/" + username, query);
            final ArrayList<DownloadLink> results = crawlPosts(br, fp);
            if (results.isEmpty()) {
                logger.info("Stopping because: Failed to find any results on current page: " + page);
                break;
            }
            for (final DownloadLink result : results) {
                final String postID = result.getStringProperty(PROPERTY_POST_ID);
                if (!dupes.add(postID)) {
                    logger.info("Stopping because: Found dupe: " + postID);
                    break profileLoop;
                }
                lastPostID = postID;
                decryptedLinks.add(result);
            }
            logger.info("Crawled page " + page + " | Found items so far: " + decryptedLinks.size() + " | lastPostID: " + lastPostID);
            if (this.isAbort()) {
                break;
            }
            // if (results.size() < maxItemsPerPage) {
            // logger.info("Stopping because: Current page contains only " + results.size() + " of max. " + maxItemsPerPage + " items");
            // break;
            // }
            index += maxItemsPerPage;
            page++;
        } while (true);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlPosts(final Browser br, final FilePackage fp) throws PluginException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String[] posts = br.getRegex("(<div class=\"wrap-post del\\d+\\s*(?:pubvideo|typeimage)\\s*\">.*data-orgpost=)").getColumn(0);
        for (final String post : posts) {
            final Regex postInfo = new Regex(post, "(https?://(\\w+)\\.[^/]+/post/(\\d+))");
            final String postURL = postInfo.getMatch(0);
            final String username = postInfo.getMatch(1);
            final String postID = postInfo.getMatch(2);
            if (postURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Regex direct = new Regex(post, "\"(https?://[^/]+/uploads/(?:videos|photos)/(\\d{4})/(\\d{2})[^\"]+\\.[a-zA-Z0-9]{2,5})");
            if (!direct.matches()) {
                logger.warning("Failed to find any media for post: " + postURL);
                continue;
            }
            final String directurl = direct.getMatch(0);
            final String year = direct.getMatch(1);
            final String month = direct.getMatch(2);
            final DownloadLink dl = this.createDownloadlink(directurl);
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                dl.setContentUrl(directurl);
            } else {
                dl.setContentUrl(postURL);
            }
            dl.setFinalFileName(username + "_" + year + "_" + month + "_" + postID + Plugin.getFileNameExtensionFromURL(directurl));
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            dl.setAvailable(true);
            dl.setProperty(PROPERTY_POST_ID, postID);
            decryptedLinks.add(dl);
            distribute(dl);
        }
        return decryptedLinks;
    }
}
