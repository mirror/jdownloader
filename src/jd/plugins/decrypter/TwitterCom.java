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
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "twitter.com", "t.co" }, urls = { "https?://(?:www\\.|mobile\\.)?twitter\\.com/[A-Za-z0-9_\\-]+/status/\\d+|https?://(?:www\\.|mobile\\.)?twitter\\.com/(?!i/)[A-Za-z0-9_\\-]{2,}(?:/media)?|https://twitter\\.com/i/cards/tfw/v1/\\d+|https?://(?:www\\.)?twitter\\.com/i/videos/tweet/\\d+", "https?://t\\.co/[a-zA-Z0-9]+" })
public class TwitterCom extends PornEmbedParser {
    public TwitterCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_CARD      = "https?://(?:www\\.)?twitter\\.com/i/cards/tfw/v1/\\d+";
    private static final String TYPE_USER_ALL  = "https?://(?:www\\.)?twitter\\.com/[A-Za-z0-9_\\-]+(?:/media)?";
    private static final String TYPE_USER_POST = "https?://(?:www\\.)?twitter\\.com.*?status/\\d+.*?";
    private static final String TYPE_REDIRECT  = "https?://t\\.co/[a-zA-Z0-9]+";

    protected DownloadLink createDownloadlink(final String link, final String tweetid) {
        final DownloadLink ret = super.createDownloadlink(link);
        ret.setProperty("tweetid", tweetid);
        return ret;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setAllowedResponseCodes(new int[] { 429 });
        final String parameter = param.toString().replaceAll("https?://(www\\.|mobile\\.)?twitter\\.com/", "https://twitter.com/");
        final String urlfilename = getUrlFname(parameter);
        final String user = new Regex(parameter, "https?://[^/]+/([A-Za-z0-9_\\-]+)").getMatch(0);
        final FilePackage fp;
        if ("i".equals(user)) {
            fp = null;
        } else {
            fp = FilePackage.getInstance();
            fp.setName(user);
        }
        String tweet_id = null;
        if (parameter.matches(TYPE_REDIRECT)) {
            this.br.setFollowRedirects(false);
            getPage(parameter);
            String finallink = this.br.getRedirectLocation();
            if (finallink == null) {
                finallink = this.br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+;URL=(https?[^<>\"]*?)(#_=_)?\"").getMatch(0);
            }
            if (br.getRequest().getHttpConnection().getResponseCode() == 403 || br.getRequest().getHttpConnection().getResponseCode() == 404 || finallink == null) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                offline.setFinalFileName(urlfilename);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            decryptedLinks.add(this.createDownloadlink(finallink));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        /* Some profiles can only be accessed if they accepted others as followers --> Log in if the user has added his twitter account */
        if (getUserLogin(false)) {
            logger.info("Account available and we're logged in");
        } else {
            logger.info("No account available or login failed");
        }
        getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 403 || br.getRequest().getHttpConnection().getResponseCode() == 404) {
            if (parameter.contains("/cards/")) {
                return decryptedLinks;
            }
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(urlfilename);
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (br.containsHTML("class=\"ProtectedTimeline\"")) {
            logger.info("This tweet timeline is protected (private)");
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName("This tweet timeline is protected_" + urlfilename);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_CARD)) {
            tweet_id = new Regex(parameter, "(\\d+)$").getMatch(0);
            /* First check for external urls */
            decryptedLinks.addAll(this.findEmbedUrls(null));
            String externID = this.br.getRegex("u\\-linkClean js\\-openLink\" href=\"(https?://t\\.co/[^<>\"]*?)\"").getMatch(0);
            if (externID == null) {
                externID = this.br.getRegex("\"card_ur(?:i|l)\"[\t\n\r ]*?:[\t\n\r ]*?\"(https?[^<>\"]*?)\"").getMatch(0);
            }
            if (externID != null) {
                decryptedLinks.add(this.createDownloadlink(externID));
                return decryptedLinks;
            }
            if (decryptedLinks.isEmpty()) {
                String dllink = br.getRegex("playlist\\&quot;:\\[\\{\\&quot;source\\&quot;:\\&quot;(https[^<>\"]*?\\.(?:webm|mp4))").getMatch(0);
                if (dllink == null) {
                    logger.info("dllink == null, abend ");
                    return null;
                }
                dllink = dllink.replace("\\", "");
                final String filename = tweet_id + "_" + new Regex(dllink, "([^/]+\\.[a-z0-9]+)$").getMatch(0);
                final DownloadLink dl = this.createDownloadlink(dllink, tweet_id);
                if (fp != null) {
                    fp.add(dl);
                }
                dl.setProperty("decryptedfilename", filename);
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else if (parameter.matches(jd.plugins.hoster.TwitterCom.TYPE_VIDEO_EMBED)) {
            final LinkedHashMap<String, Object> entries = getPlayerData(this.br);
            final String sourcetype = (String) entries.get("source_type");
            if (sourcetype.equals("consumer")) {
                /* Video uploaded by user, hosted on Twitter --> Download via Twitter hosterplugin */
                decryptedLinks.add(this.createDownloadlink(parameter));
            } else {
                /* E.g. embedded Vine.co video */
                final String url_extern = (String) entries.get("player_url");
                if (StringUtils.isEmpty(url_extern)) {
                    return null;
                }
                decryptedLinks.add(this.createDownloadlink(url_extern));
            }
        } else if (parameter.matches(TYPE_USER_POST)) {
            /* Single Tweet */
            tweet_id = new Regex(parameter, "/status/(\\d+)").getMatch(0);
            final String twitter_text = this.br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (br.containsHTML("data\\-autoplay\\-src=|video:url")) {
                final DownloadLink dl = createDownloadlink(createVideourl(tweet_id));
                decryptedLinks.add(dl);
            } else {
                final String[] regexes = { "property=\"og:image\" content=\"(https?://[^<>\"]+/media/[A-Za-z0-9\\-_]+\\.(?:jpg|png|gif):large)\"", "(?:<source video\\-src=|video_url\":)\"(https?://[^<>\"]*?)\"" };
                for (final String regex : regexes) {
                    final String[] alllinks = br.getRegex(regex).getColumn(0);
                    if (alllinks != null && alllinks.length > 0) {
                        for (String alink : alllinks) {
                            final Regex fin_al = new Regex(alink, "https?://[^<>\"]+/[^/]+/([A-Za-z0-9\\-_]+)\\.([a-z0-9]+)(?::large)?$");
                            final String servername = fin_al.getMatch(0);
                            final String ending = fin_al.getMatch(1);
                            final String final_filename = tweet_id + "_" + servername + "." + ending;
                            alink = Encoding.htmlDecode(alink.trim());
                            final DownloadLink dl = createDownloadlink(alink, tweet_id);
                            dl.setAvailable(true);
                            dl.setProperty("decryptedfilename", final_filename);
                            dl.setName(final_filename);
                            decryptedLinks.add(dl);
                        }
                    }
                }
                if (decryptedLinks.size() == 0 && this.br.containsHTML("class=\"modal\\-title embed\\-video\\-title\"|<meta[^<>]*?property=\"og:type\"[^<>]*?content=\"video\"[^<>]*?>")) {
                    /* Seems like we have a single video */
                    final DownloadLink dl = createDownloadlink(createVideourl(tweet_id));
                    decryptedLinks.add(dl);
                }
                if (decryptedLinks.size() == 0 && twitter_text != null) {
                    /* Maybe the tweet only consists of text which maybe contains URLs which maybe lead to content. */
                    final String[] urls_in_text = HTMLParser.getHttpLinks(twitter_text, "");
                    if (urls_in_text != null) {
                        for (final String url : urls_in_text) {
                            decryptedLinks.add(createDownloadlink(url));
                        }
                    }
                }
                if (decryptedLinks.size() == 0) {
                    /* Probably Tweet does not contain any media */
                    logger.info("Could not find any media in this Tweet");
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
            }
        } else {
            /* All posts / reposts / media of a user */
            final String twitter_reload_url_format;
            if (parameter.endsWith("/media")) {
                twitter_reload_url_format = "https://twitter.com/i/profiles/show/" + user + "/media_timeline?include_available_features=1&include_entities=1&max_position=%s&reset_error_state=false";
            } else {
                twitter_reload_url_format = "https://twitter.com/i/profiles/show/" + user + "/timeline/tweets?include_available_features=1&include_entities=1&max_position=%s&reset_error_state=false";
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* 2016-11-30: Seems like twitter limits their website to a max "load more" calls of 40. */
            int reloadNumber = 0;
            /* Get start-id */
            String maxid = br.getRegex("data\\-min\\-position=\"(\\d+)\"").getMatch(0);
            DownloadLink dl = null;
            do {
                if (this.isAbort()) {
                    logger.info("Decryption aborted at reload " + reloadNumber);
                    return decryptedLinks;
                }
                if (reloadNumber == 1) {// For testing only!
                    // break;
                }
                logger.info("Decrypting reloadnumber " + reloadNumber + ", found " + decryptedLinks.size() + " links till now");
                if (reloadNumber > 0) {
                    maxid = br.getRegex("\"min_position\"\\s*?:\\s*?\"(\\d+)").getMatch(0);
                }
                int addedlinks_all = 0;
                final String[] tweetsources = this.br.getRegex("li class=\"js\\-stream\\-item stream\\-item stream\\-item([^の]+?)ProfileTweet\\-actionCount").getColumn(0);
                if (tweetsources == null || tweetsources.length == 0) {
                    logger.info("tweetsources == null || tweetsources.length == 0, regex is broken eventually");
                    break;
                }
                for (final String tweetsource : tweetsources) {
                    // logger.info("tweetsource: " + tweetsource);
                    Boolean found = false;
                    tweet_id = new Regex(tweetsource, "id=\"stream\\-item\\-tweet\\-(\\d+)\"").getMatch(0);
                    if (tweet_id == null) {
                        logger.info("tweet_id == null");
                        return null;
                    }
                    final String[] embedurl_regexes = new String[] { "\"(https?://(?:www\\.)?(youtu\\.be/|youtube\\.com/embed/)[A-Za-z0-9\\-_]+)\"", "data\\-expanded\\-url=\"(https?://(?:www\\.)?vine\\.co/v/[A-Za-z0-9]+)\"" };
                    for (final String regex : embedurl_regexes) {
                        final String[] embed_links = new Regex(tweetsource, regex).getColumn(0);
                        if (embed_links != null && embed_links.length > 0) {
                            found = true;
                            for (final String single_embed_ink : embed_links) {
                                dl = createDownloadlink(single_embed_ink, tweet_id);
                                if (fp != null) {
                                    fp.add(dl);
                                }
                                distribute(dl);
                                decryptedLinks.add(dl);
                                addedlinks_all++;
                            }
                        }
                    }
                    final String[] directlink_regexes = new String[] { "data-image-url=(?:\\&quot;|\")(https?://[a-z0-9]+\\.twimg\\.com/[^<>\"]*?\\.(?:jpg|png|gif))", "\"(https://amp\\.twimg\\.com/[^<>\"]*?)\"", "data-url=\"(https?://[a-z0-9]+\\.twimg\\.com/[^<>\"]*?)\"", "(?:data\\-img\\-src=|img src=)\"(https?://[a-z0-9]+\\.twimg\\.com/[^<>\"]*?)\"" };
                    for (final String regex : directlink_regexes) {
                        final String[] piclinks = new Regex(tweetsource, regex).getColumn(0);
                        if (piclinks != null && piclinks.length > 0) {
                            found = true;
                            for (String singleLink : piclinks) {
                                final String remove = new Regex(singleLink, "(:[a-z0-9]+)").getMatch(0);
                                if (remove != null) {
                                    singleLink = singleLink.replace(remove, "");
                                }
                                singleLink = Encoding.htmlDecode(singleLink.trim());
                                dl = createDownloadlink(singleLink, tweet_id);
                                if (fp != null) {
                                    fp.add(dl);
                                }
                                dl.setAvailable(true);
                                distribute(dl);
                                decryptedLinks.add(dl);
                                addedlinks_all++;
                            }
                        }
                    }
                    /* Video #1 */
                    final String stream_id = new Regex(tweetsource, "\"/i/cards/tfw/v1/(\\d+)\\?cardname=__entity_video").getMatch(0);
                    if (stream_id != null) {
                        found = true;
                        /* Our post is a stream */
                        /* Usually stream_id == tweet_id */
                        dl = createDownloadlink(createVideourl(stream_id), tweet_id);
                        if (fp != null) {
                            fp.add(dl);
                        }
                        distribute(dl);
                        decryptedLinks.add(dl);
                        addedlinks_all++;
                    }
                    /* Video #2 */
                    final Regex vinfo = new Regex(tweetsource, "(video data-media-id=\"[0-9]+\".*?source video-src=\"[^\"]+\")");
                    // logger.info("vinfo: " + vinfo);
                    final String vid = vinfo.getMatch(0);
                    final String vsrc = vinfo.getMatch(1);
                    if (vid != null && vsrc != null) {
                        found = true;
                        dl = createDownloadlink(vsrc, tweet_id);
                        if (fp != null) {
                            fp.add(dl);
                        }
                        dl.setContentUrl(vsrc);
                        dl.setLinkID(vid);
                        dl.setName(vid + ".mp4");
                        dl.setAvailable(true);
                        distribute(dl);
                        decryptedLinks.add(dl);
                        addedlinks_all++;
                    } else if (tweetsource.contains("is-generic-video")) {
                        /* Video #3 2016-10-21 */
                        found = true;
                        dl = this.createDownloadlink(createVideourl(tweet_id));
                        distribute(dl);
                        decryptedLinks.add(dl);
                        addedlinks_all++;
                    }
                    if (!found) {
                        dl = createDownloadlink(createVideourl(tweet_id), tweet_id);
                        if (fp != null) {
                            fp.add(dl);
                        }
                        decryptedLinks.add(dl);
                        addedlinks_all++;
                    }
                }
                if (addedlinks_all == 0 || maxid == null) {
                    break;
                }
                getPage(String.format(twitter_reload_url_format, maxid));
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                reloadNumber++;
            } while (br.containsHTML("\"has_more_items\":true"));
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Could not find any media, decrypter might be broken");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    public static LinkedHashMap<String, Object> getPlayerData(final Browser br) {
        LinkedHashMap<String, Object> entries = null;
        try {
            String json_source = br.getRegex("<div id=\"playerContainer\"[^<>]*?data\\-config=\"([^<>\"]+)\"").getMatch(0);
            json_source = Encoding.htmlDecode(json_source);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
        } catch (final Throwable e) {
        }
        return entries;
    }

    protected void getPage(final Browser br, final String url) throws Exception {
        super.getPage(br, url);
        if (br.getHttpConnection().getResponseCode() == 429) {
            logger.info("Error 429 too many requests - add less URLs and/or perform a reconnect!");
        }
    }

    protected void getPage(final String url) throws Exception {
        getPage(this.br, url);
    }

    private String createVideourl(final String stream_id) {
        return String.format("https://twitter.com/i/videos/tweet/%s", stream_id);
    }

    private String getUrlFname(final String parameter) {
        String urlfilename;
        if (parameter.matches(TYPE_USER_ALL)) {
            urlfilename = new Regex(parameter, "twitter\\.com/([A-Za-z0-9_\\-]+)/media").getMatch(0);
        } else {
            urlfilename = new Regex(parameter, "twitter\\.com/status/(\\d+)").getMatch(0);
        }
        return urlfilename;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings({ "deprecation", "static-access" })
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("twitter.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            return false;
        }
        try {
            ((jd.plugins.hoster.TwitterCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }
}
