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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.KemonoPartyConfig;
import org.jdownloader.plugins.components.config.KemonoPartyConfig.TextCrawlMode;
import org.jdownloader.plugins.components.config.KemonoPartyConfigCoomerParty;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.KemonoParty;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KemonoPartyCrawler extends PluginForDecrypt {
    public KemonoPartyCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "coomer.party", "coomer.su" }); // onlyfans.com content
        ret.add(new String[] { "kemono.party", "kemono.su" }); // content of other websites such as patreon.com
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[^/]+/user/[^/]+(/post/\\d+)?");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_PROFILE = "(?:https?://[^/]+)?/([^/]+)/user/([^/\\?]+)(\\?o=(\\d+))?$";
    private final String TYPE_POST    = "(?:https?://[^/]+)?/([^/]+)/user/([^/]+)/post/(\\d+)$";
    private KemonoParty  hostPlugin   = null;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_PROFILE)) {
            return this.crawlProfile(param);
        } else if (param.getCryptedUrl().matches(TYPE_POST)) {
            return this.crawlPost(param);
        } else {
            /* Unsupported URL --> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private int getMaxPage(Browser br, final String portal, final String username, int maxPage) {
        final String[] pages = br.getRegex("href=\"/" + Pattern.quote(portal) + "/user/" + Pattern.quote(username) + "\\?o=\\d+\"[^>]*>\\s*(?:<b>)?\\s*(\\d+)").getColumn(0);
        int ret = maxPage;
        for (final String pageStr : pages) {
            final int page = Integer.parseInt(pageStr);
            if (page > ret) {
                ret = page;
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlProfile(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_PROFILE);
        if (!urlinfo.patternFind()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String portal = urlinfo.getMatch(0);
        final String username = urlinfo.getMatch(1);
        br.setFollowRedirects(true);
        /* Always begin on page 1 no matter which page param is given in users' added URL. */
        br.getPage("https://" + this.getHost() + "/" + portal + "/user/" + username);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().matches(TYPE_PROFILE)) {
            /* E.g. redirect to main page */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Find number of last page (for logging purposes) */
        int maxpage = getMaxPage(br, portal, username, 1);
        int totalNumberofItems = -1;
        String totalNumberofItemsStr = br.getRegex("Showing \\d+ - \\d+ of (\\d+)").getMatch(0);
        if (totalNumberofItemsStr != null) {
            totalNumberofItems = Integer.parseInt(totalNumberofItemsStr);
        } else {
            totalNumberofItemsStr = "unknown";
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setAllowMerge(true);
        fp.setAllowInheritance(true);
        fp.setName(portal + " - " + username);
        final HashSet<String> dupes = new HashSet<String>();
        int page = 1;
        do {
            final String[] posturls = br.getRegex("(?:https?://[^/]+)?/([^/]+)/user/([^/]+)/post/(\\d+)").getColumn(-1);
            int numberofAddedItems = 0;
            for (String posturl : posturls) {
                posturl = br.getURL(posturl).toString();
                if (dupes.add(posturl)) {
                    final DownloadLink result = this.createDownloadlink(posturl);
                    result._setFilePackage(fp);
                    ret.add(result);
                    distribute(result);
                    numberofAddedItems++;
                }
            }
            maxpage = getMaxPage(br, portal, username, maxpage);
            logger.info("Crawled page " + page + "/" + maxpage + " | Found items: " + ret.size() + "/" + totalNumberofItemsStr);
            final String nextpageurl = br.getRegex("(/[^\"]+\\?o=\\d+)\"[^>]*>(?:<b>)?\\s*" + (page + 1)).getMatch(0);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (ret.size() == totalNumberofItems) {
                logger.info("Stopping because: Found all items");
                break;
            } else if (nextpageurl == null) {
                /* Additional fail-safe */
                logger.info("Stopping because: Failed to find nextpageurl - last page is: " + br.getURL());
                break;
            } else if (numberofAddedItems == 0) {
                logger.info("Stopping because: Failed to find any [new] items on current page");
                break;
            } else {
                page++;
                br.getPage(nextpageurl);
            }
        } while (true);
        return ret;
    }

    private ArrayList<DownloadLink> crawlPost(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_POST);
        if (!urlinfo.matches()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String portal = urlinfo.getMatch(0);
        final String userID = urlinfo.getMatch(1);
        final String postID = urlinfo.getMatch(2);
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(500);// DDOS-GUARD
        int retry = 3;
        while (retry > 0) {
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 500 && !isAbort()) {
                sleep(1000, param);
                retry--;
            } else {
                break;
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().matches(TYPE_POST)) {
            /* E.g. redirect to main page of user because single post does not exist */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String postTitle = br.getRegex("class=\"post__title\">\\s*<span>([^<]+)</span>").getMatch(0);
        if (postTitle != null) {
            postTitle = Encoding.htmlDecode(postTitle).trim();
        }
        String publishedDateStr = br.getRegex("\"post__published\"[^>]*>\\s*<time[^>]*class\\s*=\\s*\"timestamp[^>]*datetime\\s*=\\s*\"\\s*([0-9\\-: ]+)").getMatch(0);
        if (publishedDateStr == null) {
            publishedDateStr = br.getRegex("<meta name\\s*=\\s*\"published\"\\s*content\\s*=\\s*\"\\s*([0-9\\-: ]+)").getMatch(0);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (postTitle != null) {
            fp.setName(portal + " - " + userID + " - " + postID + " - " + postTitle);
        } else {
            /* Fallback */
            fp.setName(portal + " - " + userID + " - " + postID);
        }
        final ArrayList<DownloadLink> kemonoResults = new ArrayList<DownloadLink>();
        final String[] directURLs = br.getRegex("\"((https?://[^/]+)?/data/[^\"]+)").getColumn(0);
        if (directURLs != null && directURLs.length > 0) {
            /* Remove duplicates from results so our index will be correct down below. */
            ensureInitHosterplugin();
            final ArrayList<String> videoItemsToSkip = new ArrayList<String>();
            final HashSet<String> dups = new HashSet<String>();
            int index = 0;
            final ArrayList<DownloadLink> videoItemsUnfiltered = new ArrayList<DownloadLink>();
            for (final String directURL : directURLs) {
                final String urlFull = br.getURL(directURL).toString();
                if (!dups.add(urlFull)) {
                    /* Skip dupes */
                    continue;
                }
                final DownloadLink media = new DownloadLink(this.hostPlugin, this.getHost(), urlFull);
                media.setProperty(KemonoParty.PROPERTY_POST_CONTENT_INDEX, index);
                boolean isFilenameFromHTML = false;
                String betterFilename = getBetterFilenameFromURL(urlFull);
                if (betterFilename == null) {
                    /* 2023-03-01 */
                    betterFilename = br.getRegex(Pattern.quote(directURL) + "\"\\s+download=\"([^\"]+)\"").getMatch(0);
                    isFilenameFromHTML = true;
                }
                if (!StringUtils.isEmpty(betterFilename)) {
                    betterFilename = Encoding.htmlDecode(betterFilename).trim();
                    media.setFinalFileName(betterFilename);
                    if (isFilenameFromHTML) {
                        media.setProperty(KemonoParty.PROPERTY_BETTER_FILENAME, betterFilename);
                    }
                    media.setProperty(DirectHTTP.FIXNAME, betterFilename);
                    final String internalVideoFilename = new Regex(urlFull, "(?i)([a-f0-9]{64}\\.(m4v|mp4))").getMatch(0);
                    if (internalVideoFilename != null) {
                        videoItemsToSkip.add(internalVideoFilename);
                    }
                }
                videoItemsUnfiltered.add(media);
                index++;
            }
            if (videoItemsToSkip.size() > 0) {
                logger.info("Filtering duplicated video items: " + videoItemsUnfiltered);
                for (final DownloadLink link : videoItemsUnfiltered) {
                    boolean filter = false;
                    for (final String videoItemToSkip : videoItemsToSkip) {
                        if (link.getPluginPatternMatcher().endsWith(videoItemToSkip)) {
                            filter = true;
                            break;
                        }
                    }
                    if (!filter) {
                        kemonoResults.add(link);
                    }
                }
            } else {
                kemonoResults.addAll(videoItemsUnfiltered);
            }
        }
        final String postTextContent = br.getRegex("<div\\s*class\\s*=\\s*\"post__content\"[^>]*>(.+)</div>\\s*<footer").getMatch(0);
        if (!StringUtils.isEmpty(postTextContent)) {
            final KemonoPartyConfig cfg = PluginJsonConfig.get(getConfigInterface());
            final TextCrawlMode mode = cfg.getTextCrawlMode();
            if (cfg.isCrawlHttpLinksFromPostContent()) {
                final String[] urls = HTMLParser.getHttpLinks(postTextContent, br.getURL());
                if (urls != null && urls.length > 0) {
                    for (final String url : urls) {
                        ret.add(this.createDownloadlink(url));
                    }
                }
            }
            if (mode == TextCrawlMode.ALWAYS || (mode == TextCrawlMode.ONLY_IF_NO_MEDIA_ITEMS_ARE_FOUND && kemonoResults.isEmpty())) {
                ensureInitHosterplugin();
                final DownloadLink textfile = new DownloadLink(this.hostPlugin, this.getHost(), param.getCryptedUrl());
                textfile.setProperty(KemonoParty.PROPERTY_TEXT, postTextContent);
                textfile.setFinalFileName(fp.getName() + ".txt");
                try {
                    textfile.setDownloadSize(postTextContent.getBytes("UTF-8").length);
                } catch (final UnsupportedEncodingException ignore) {
                    ignore.printStackTrace();
                }
                kemonoResults.add(textfile);
            }
        }
        for (final DownloadLink kemonoResult : kemonoResults) {
            if (postTitle != null) {
                kemonoResult.setProperty(KemonoParty.PROPERTY_TITLE, postTitle);
            }
            if (publishedDateStr != null) {
                kemonoResult.setProperty(KemonoParty.PROPERTY_DATE, publishedDateStr);
            }
            kemonoResult.setProperty(KemonoParty.PROPERTY_PORTAL, portal);
            kemonoResult.setProperty(KemonoParty.PROPERTY_USERID, userID);
            kemonoResult.setProperty(KemonoParty.PROPERTY_POSTID, postID);
            kemonoResult.setAvailable(true);
            ret.add(kemonoResult);
        }
        fp.addLinks(ret);
        return ret;
    }

    public static String getBetterFilenameFromURL(final String url) throws MalformedURLException {
        final UrlQuery query = UrlQuery.parse(url);
        final String betterFilename = query.get("f");
        if (betterFilename != null) {
            return Encoding.htmlDecode(betterFilename).trim();
        } else {
            return null;
        }
    }

    private void ensureInitHosterplugin() throws PluginException {
        if (this.hostPlugin == null) {
            this.hostPlugin = (KemonoParty) getNewPluginForHostInstance(this.getHost());
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* Try to avoid DDOS-GUARD */
        return 1;
    }

    @Override
    public Class<? extends KemonoPartyConfig> getConfigInterface() {
        if ("kemono.party".equalsIgnoreCase(getHost())) {
            return KemonoPartyConfig.class;
        } else {
            return KemonoPartyConfigCoomerParty.class;
        }
    }
}
