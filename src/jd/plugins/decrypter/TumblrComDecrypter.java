//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.TumblrComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.TumblrCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tumblr.com" }, urls = { "https?://(?![a-z0-9]+\\.media\\.tumblr\\.com/.+)[\\w\\.\\-]+?tumblr\\.com(?:/image/\\d+|/post/\\d+|/likes|/archive|/?$|/blog/view/[^/]+(?:/\\d+)?)(?:\\?password=.+)?" })
public class TumblrComDecrypter extends PluginForDecrypt {
    public TumblrComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String GENERALOFFLINE                      = "(?i)>\\s*Not found\\.<";
    private static final String TYPE_INVALID                        = "https?://(?:(?:platform|embed|assets)\\.)tumblr\\.com/.+";
    private static final String TYPE_POST                           = "https?://([\\w\\-]+)\\.[^/]+/post/(\\d+)(/([a-z0-9\\-]+))?";
    private static final String TYPE_POST_LOGGEDIN                  = "https?://[^/]+/blog/view/([^/]+)/(\\d+)";
    private static final String TYPE_IMAGE                          = ".+tumblr\\.com/image/\\d+";
    private static final String TYPE_USER_LOGGEDIN                  = "https?://(?:www\\.)?tumblr\\.com/blog/view/([^/]+)";
    private static final String TYPE_USER_LOGGEDOUT                 = "https?://([^/]+)\\.tumblr\\.com/.*";
    private static final String TYPE_USER_OWN_LIKES                 = "https?://[^/]+/likes$";
    private static final String urlpart_passwordneeded              = "/blog_auth";
    private static final String PROPERTY_TAGS                       = "tags";
    private static final String PROPERTY_DATE                       = "date";
    private static String       anonymousApikey                     = "";
    private static long         anonymousApikeyLastRefreshTimestamp = 0;
    private TumblrComConfig     cfg                                 = null;

    private boolean isBlog(final String url) {
        return url.matches(TYPE_USER_LOGGEDIN) || url.matches(TYPE_USER_LOGGEDOUT);
    }

    private boolean isSinglePost(final String url) {
        return url.matches(TYPE_POST) || url.matches(TYPE_POST_LOGGEDIN);
    }

    private boolean isAccountRequired(final String url) {
        return url.matches(TYPE_USER_OWN_LIKES);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (param.toString().matches(TYPE_INVALID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.cfg = PluginJsonConfig.get(TumblrComConfig.class);
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        if (aa != null) {
            /* Login whenever possible to be able to download account-only-stuff */
            final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.TumblrCom) plg).login(aa, false);
            if (isSinglePost(param.getCryptedUrl())) {
                return decryptPostAPI(param);
            } else if (param.getCryptedUrl().matches(TYPE_USER_OWN_LIKES)) {
                return decryptUsersOwnLikes(param);
            } else if (isBlog(param.getCryptedUrl())) {
                return decryptUserAPI(param);
            } else {
                logger.warning("Unsupported URL");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            if (isAccountRequired(param.getCryptedUrl())) {
                throw new AccountRequiredException();
            }
            br.setFollowRedirects(true);
            /* 2021-04-13: We need this cookie! Seems like any random value is fine. */
            br.setCookie(this.getHost(), "euconsent-v2", "true");
            if (param.getCryptedUrl().matches(TYPE_POST)) {
                /*
                 * 2021-04-13: TODO: I was unable to use the public API for single posts -> Using old website crawler for now (still
                 * working).
                 */
                return decryptPostWebsite(param);
            } else if (this.isBlog(param.getCryptedUrl())) {
                this.getAndSetAnonymousApikey(this.br);
                return decryptUserAPI(param);
            } else {
                logger.warning("Unsupported URL");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private void getAndSetAnonymousApikey(final Browser brc) throws IOException, PluginException {
        synchronized (anonymousApikey) {
            if (anonymousApikey.isEmpty() || System.currentTimeMillis() - anonymousApikeyLastRefreshTimestamp > 60 * 60 * 1000) {
                logger.info("Obtaining apikey from website");
                /* 2021-04-13: Not all sub-pages contain the apikey -> Use mainpage */
                // br.getPage(param.getCryptedUrl());
                brc.getPage("https://www." + this.getHost() + "/");
                final String apikey = PluginJSonUtils.getJson(brc, "API_TOKEN");
                if (StringUtils.isEmpty(apikey)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                anonymousApikey = apikey;
                anonymousApikeyLastRefreshTimestamp = System.currentTimeMillis();
            }
        }
        brc.getHeaders().put("Authorization", "Bearer " + anonymousApikey);
    }

    private String getUsername(final String source) {
        final String url_username;
        if (source.matches(TYPE_USER_LOGGEDIN)) {
            url_username = new Regex(source, TYPE_USER_LOGGEDIN).getMatch(0);
        } else {
            url_username = new Regex(source, "^https?://([^/]+)\\.tumblr\\.com/.*").getMatch(0);
        }
        return url_username;
    }

    @Deprecated
    private ArrayList<DownloadLink> decryptPostWebsite(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        /* lets identify the unique id for this post, only use it for tumblr hosted content */
        final String puid;
        final String blog;
        if (param.getCryptedUrl().matches(TYPE_POST)) {
            blog = new Regex(param.getCryptedUrl(), TYPE_POST).getMatch(0);
            puid = new Regex(param.getCryptedUrl(), TYPE_POST).getMatch(1);
            br.getPage(param.getCryptedUrl());
        } else {
            blog = new Regex(param.getCryptedUrl(), TYPE_POST_LOGGEDIN).getMatch(0);
            puid = new Regex(param.getCryptedUrl(), TYPE_POST_LOGGEDIN).getMatch(1);
            /*
             * We need to change that URL and hope that we're able to access these as they're only used in browser when the user is logged
             * in.
             */
            br.getPage("https://" + blog + "." + this.getHost() + "/post/" + puid);
        }
        checkErrorsWebsite(this.br);
        if (this.handlePassword(param)) {
            br.getPage(param.getCryptedUrl());
            br.followRedirect(true);
        }
        String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fpName == null) {
            // use google carousel json...
            fpName = PluginJSonUtils.getJsonValue(getGoogleCarousel(br), "articleBody");
            if (fpName != null && fpName.length() > 250) {
                fpName = fpName.substring(0, 250) + "...";
            }
            if (fpName == null) {
                // this can get false positives. eg. "the funniest posts on tumblr," thenwhatyouwanthere.
                fpName = br.getRegex("<meta name=\"description\" content=\"([^/\"]+)").getMatch(0);
                if (fpName == null) {
                    fpName = "Tumblr post " + puid;
                }
            }
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        fpName = fpName.replaceAll("\\s+", " ");
        // isolate post html
        final String postBody = findPostBody();
        decryptedLinks.addAll(processGeneric(param, br, postBody, fpName, puid));
        return decryptedLinks;
    }

    /** https://www.tumblr.com/docs/en/api/v2#postspost-id---fetching-a-post-neue-post-format */
    private ArrayList<DownloadLink> decryptPostAPI(final CryptedLink param) throws Exception {
        // lets identify the unique id for this post, only use it for tumblr hosted content
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String puid;
        final String blog;
        if (param.getCryptedUrl().matches(TYPE_POST)) {
            blog = new Regex(param.getCryptedUrl(), TYPE_POST).getMatch(0);
            puid = new Regex(param.getCryptedUrl(), TYPE_POST).getMatch(1);
        } else {
            blog = new Regex(param.getCryptedUrl(), TYPE_POST_LOGGEDIN).getMatch(0);
            puid = new Regex(param.getCryptedUrl(), TYPE_POST_LOGGEDIN).getMatch(1);
        }
        br.getPage(TumblrCom.API_BASE + "/blog/" + blog + "/posts/" + puid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        entries = (Map<String, Object>) entries.get("response");
        this.crawlSinglePostJsonAPI(ret, entries);
        // final String fpName = ((String) entries.get("slug")).replace("-", " ").trim();
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(fpName);
        return ret;
    }

    /** Wrapper */
    private void crawlMultiplePostsArrayJsonAPI(final ArrayList<DownloadLink> ret, final List<Object> postsO) {
        for (final Object postO : postsO) {
            this.crawlSinglePostJsonAPI(ret, (Map<String, Object>) postO);
        }
    }

    private void crawlSinglePostJsonAPI(final ArrayList<DownloadLink> ret, Map<String, Object> entries) {
        // final String postURL = (String) entries.get("post_url");
        final String blogName = (String) entries.get("blog_name");
        final String fpName = blogName + " - " + ((String) entries.get("slug")).replace("-", " ").trim();
        String dateFormatted = null;
        final Object timestampO = entries.get("timestamp");
        if (timestampO != null && timestampO instanceof Number) {
            final long timestamp = ((Number) timestampO).longValue();
            dateFormatted = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date(timestamp * 1000));
        } else {
            logger.warning("Timestamp missing for: " + fpName + "|" + timestampO);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        final List<Object> repostList = (List<Object>) entries.get("trail");
        final List<Object> contentArrays = new ArrayList<Object>();
        contentArrays.add(entries.get("content"));
        for (final Object repostO : repostList) {
            entries = (Map<String, Object>) repostO;
            if (entries.containsKey("content")) {
                contentArrays.add(entries.get("content"));
            }
        }
        for (final Object contentArrayO : contentArrays) {
            final List<Object> ressourcelist = (List<Object>) contentArrayO;
            for (final Object contentO : ressourcelist) {
                entries = (Map<String, Object>) contentO;
                /* Possible types: image, text, link, video */
                final String type = (String) entries.get("type");
                /* 2021-04-07: Only allow images for now */
                if (StringUtils.isEmpty(type)) {
                    /* This should never happen */
                    continue;
                } else if (!type.matches("image|video")) {
                    /* Skip unsupported content e.g. "text" */
                    continue;
                }
                /* E.g. "tumblr"(or null!) = selfhosted, "instagram" or others = extern content/embedded" */
                final String provider = (String) entries.get("provider");
                final DownloadLink dl;
                if (type.equals("video")) {
                    /* Videos only have 1 version available */
                    String url;
                    if (entries.containsKey("url")) {
                        url = (String) entries.get("url");
                    } else {
                        url = (String) JavaScriptEngineFactory.walkJson(entries, "media/url");
                    }
                    if (StringUtils.isEmpty(url)) {
                        logger.warning("Bad video object");
                        continue;
                    }
                    url = convertDirectVideoUrltoHD(url);
                    dl = this.createDownloadlink(url);
                } else {
                    final List<Object> versionsO = (List<Object>) entries.get("media");
                    if (versionsO.isEmpty()) {
                        /* This should never happen */
                        logger.warning("Found empty media versions array");
                        continue;
                    }
                    /*
                     * Look for best/original version (Array is usually sorted by quality, first is best).
                     */
                    final Map<String, Object> versionInfo = (Map<String, Object>) versionsO.get(0);
                    // for (final Object versionO : versionsO) {
                    // final Map<String, Object> versionInfoTmp = (Map<String, Object>) versionO;
                    // if (versionInfoTmp.containsKey("has_original_dimensions")) {
                    // versionInfo = versionInfoTmp;
                    // }
                    // }
                    // if (versionInfo == null) {
                    // /* Fallback: Use first version */
                    // versionInfo = (Map<String, Object>) versionsO.get(0);
                    // }
                    String url = (String) versionInfo.get("url");
                    if (url.endsWith(".gifv") && cfg.isPreferMp4OverGifv()) {
                        /*
                         * 2022-02-16: All gifv content should be available as mp4 files too --> Download mp4 instead of gifv files if
                         * wished by user.
                         */
                        url = url.substring(0, url.lastIndexOf(".")) + ".mp4";
                    }
                    dl = this.createDownloadlink(url);
                    /* 2021-04-09: url can contain up to 2 MD5 hashes but neither of those is the actual file-hash! */
                    // final String md5 = new Regex(url, "([a-f0-9]{32})\\.\\w+$").getMatch(0);
                    // if (md5 != null) {
                    // dl.setMD5Hash(md5);
                    // }
                }
                /* Allow external content to go back into another crawler (e.g. Instagram URLs). */
                if (provider == null || provider.equalsIgnoreCase("tumblr")) {
                    dl.setAvailable(true);
                    try {
                        final String urlName = Plugin.getFileNameFromURL(new URL(dl.getPluginPatternMatcher()));
                        if (urlName != null) {
                            dl.setName(urlName);
                        }
                    } catch (final Throwable e) {
                    }
                }
                // if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && !StringUtils.isEmpty(postURL)) {
                // dl.setContentUrl(postURL);
                // }
                if (dateFormatted != null) {
                    dl.setProperty(PROPERTY_DATE, dateFormatted);
                }
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
            }
        }
    }

    private String getGoogleCarousel(Browser br) {
        final String result = br.getRegex("<!-- GOOGLE CAROUSEL --><script.*?</script>").getMatch(-1);
        return result;
    }

    private String findPostBody() {
        final String postBody = br.getRegex("<section id=\"posts\" class=\"content clearfix.*?</(section|article)>\\s*<section class=\"related-posts-wrapper\">").getMatch(-1);
        return postBody;
    }

    @Deprecated
    private ArrayList<DownloadLink> processGeneric(final CryptedLink param, final Browser ibr, final String postBody, final String name, final String puid) throws Exception {
        final String string = postBody != null ? postBody : ibr.toString();
        // some generic cleanup of fpName!
        final String fpName = cleanupName(name);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final Browser br = ibr.cloneBrowser();
        final String[] hashTags = new Regex(postBody, "<a class=\"meta\\-item tag\\-link\" href=\"https?://[^<>\"]+\">([^<>\"]+)</a>").getColumn(0);
        String hashTagsStr = null;
        if (hashTags != null && hashTags.length > 0) {
            hashTagsStr = Arrays.toString(hashTags);
        }
        String externID = new Regex(string, "\"(https?://video\\.vulture\\.com/video/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            br.getPage(Encoding.htmlDecode(externID));
            String cid = br.getRegex("\\&media_type=video\\&content=([A-Z0-9]+)\\&").getMatch(0);
            if (cid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("//video.vulture.com/item/player_embed.js/" + cid);
            externID = br.getRegex("(https?://videos\\.cache\\.magnify\\.net/[^<>\"]*?)\\'").getMatch(0);
            if (externID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(fpName + externID.substring(externID.lastIndexOf(".")));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = new Regex(string, "\"(https?://(www\\.)?facebook\\.com/v/\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID.replace("/v/", "/video/video.php?v="));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = new Regex(string, "\"(https?://(www\\.)?instagram\\.com/p/[A-Za-z0-9_-]+[^\"]*)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = new Regex(string, "(?:id|name)=\"twitter:player\" (?:src|content)=\"(https?://(www\\.)?youtube\\.com/v/[A-Za-z0-9\\-_]+)\\&").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = new Regex(string, "(?:id|name)=\"youtube_iframe\" (?:src|content)=\"(https?://(www\\.)?youtube\\.com/embed/.*?)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = new Regex(string, "(?:id|name)=\"vine\\-embed\" (?:src|content)=\"(https?://vine\\.co/[^<>\"]*?)\"").getMatch(0);
        if (externID == null) {
            // currently within iframe src -raztoki20160208
            externID = new Regex(string, "(\"|')(https?://vine\\.co/v/.*?)\\1").getMatch(1);
        }
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // INTERNAL LINKS ON TUMBLR
        // we can set a name here!
        final String filename = setFileName(fpName, puid);
        // VIDEO
        externID = new Regex(string, "\\\\x3csource src=\\\\x22(https?://[^<>\"]*?)\\\\x22").getMatch(0);
        if (externID == null) {
            externID = new Regex(string, "'(https?://(www\\.)?tumblr\\.com/video/[^<>\"']*?)'").getMatch(0);
            if (externID != null) {
                // the puid stays the same throughout all these requests
                br.getPage(externID);
                externID = br.getRegex("\"(https?://[^<>\"]*?tumblr\\.com/video_file/[^<>\"]*?)\"").getMatch(0);
            }
        }
        if (externID != null) {
            final DownloadLink dl;
            if (externID.matches(".+tumblr\\.com/video_file/.+")) {
                br.setFollowRedirects(false);
                // the puid stays the same throughout all these requests
                final String url_hd = convertDirectVideoUrltoHD(externID);
                if (url_hd != null) {
                    /* Yey we have an HD url ... */
                    externID = url_hd;
                } else {
                    /* Let's download the stream ... */
                    br.getPage(externID);
                    externID = br.getRedirectLocation();
                    if (externID != null && externID.matches("https?://www\\.tumblr\\.com/video_file/.+")) {
                        br.getPage(externID);
                        externID = br.getRedirectLocation();
                    }
                    externID = externID.replace("#_=_", "");
                }
                dl = createDownloadlink(externID);
                String extension = getFileNameExtensionFromURL(externID);
                if (extension == null) {
                    extension = ".mp4";
                }
                dl.setLinkID(getHost() + "://" + puid);
                if (!cfg.isUseOriginalFilenameEnabled()) {
                    dl.setFinalFileName(filename + extension);
                }
            } else {
                dl = createDownloadlink("directhttp://" + externID);
                dl.setLinkID(getHost() + "://" + puid);
            }
            if (hashTagsStr != null) {
                dl.setProperty(PROPERTY_TAGS, hashTagsStr);
            }
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (isPhotoSetWebsite(br, puid)) {
            // getGoogleCarousel!
            processPhotoSetWebsite(decryptedLinks, br, puid, fpName, hashTagsStr);
            return decryptedLinks;
        }
        // FINAL FAILOVER FOR UNSUPPORTED CONTENT, this way we wont have to keep making updates to this plugin! only time we would need to
        // is, when we need to customize / fixup results into proper url format. -raztoki20160211
        final String iframe = new Regex(string, "<iframe [^>]*src=(\"|')((?![^>]*//assets\\.tumblr\\.com/[^>]+?).*?)\\1").getMatch(1);
        if (iframe != null) {
            decryptedLinks.add(createDownloadlink(iframe));
            return decryptedLinks;
        }
        // PICTURE
        /* Access link if possible to get higher qualities e.g. *1280 --> Only needed/possible for single links. */
        final String imagelink = new Regex(string, "class=\"photo-wrapper-inner\">\\s*<a href=('|\")(https?://[a-z0-9\\-]+\\.tumblr\\.com/image/\\d+)\\1").getMatch(1);
        if (imagelink != null) {
            decryptedLinks.addAll(processImageWebsite(param, imagelink, filename, puid));
        } else {
            // I've only ever seen a single pic / post page!!
            String pic = br.getRegex("property=\"og:image\" content=(\"|')(https?://\\d+\\.media\\.tumblr\\.com/.*?)\\1").getMatch(1);
            if (pic == null) {
                // difference between this regex and picturelink is no <a href !! same url here as previous "pic" regex above.
                pic = new Regex(string, "class=\"photo-wrapper-inner\">\\s*<img src=('|\")(https?://\\d+\\.media\\.tumblr\\.com/.*?)\\1").getMatch(1);
            }
            if (pic != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + pic);
                dl.setDownloadPassword(param.getDecrypterPassword());
                dl.setAvailable(true);
                // determine file extension
                final String ext = getFileNameExtensionFromURL(pic);
                if (cfg.isUseOriginalFilenameEnabled()) {
                    dl.setName(extractFileNameFromURL(pic));
                } else {
                    dl.setFinalFileName(filename + ext);
                }
                if (hashTagsStr != null) {
                    dl.setProperty(PROPERTY_TAGS, hashTagsStr);
                }
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        }
        logger.info("Found nothing here so the decrypter is either broken or there isn't anything to crawl.");
        return decryptedLinks;
    }

    private String convertDirectVideoUrltoHD(final String source) {
        if (source == null) {
            return null;
        }
        if (source.matches("https?://vt\\.tumblr\\.com/.+")) {
            /* We already have an HD url */
            return source;
        } else {
            final String hd_final_url_part = new Regex(source, "/(tumblr_[^/]+)/\\d+$").getMatch(0);
            if (hd_final_url_part != null) {
                /* Yey we have an HD url ... */
                return "https://vt.tumblr.com/" + hd_final_url_part + ".mp4";
            } else {
                /* We can't create an HD url. */
                return source;
            }
        }
    }

    private boolean isPhotoSetWebsite(final Browser br, final String puid) {
        // find photo set iframe... can be outside of 'string' source
        // ok we don't need to process the iframe src link as best images which we are interested in are within google
        final String photoset = br.getRegex("<iframe [^>]*src=(\"|')([^<>]+?/post/\\d+/photoset_iframe/[^<>]+?)\\1").getMatch(1);
        // note the /post/\d+ uid isn't same as /post/\d+/photoset_iframe
        if (photoset != null || br.containsHTML("<article class=\"post-photoset\" id=\"" + puid + "\">|<div id=\"photoset_" + puid + "\" class=\"html_photoset\">|<article id=\"post-" + puid + "139711801296\" class=\"post\\s+[^\"]*type-photoset|<div class=\"photo-slideshow\" id=\"photoset_" + puid + "\"|")) {
            return true;
        }
        return false;
    }

    private void processPhotoSetWebsite(final ArrayList<DownloadLink> decryptedLinks, final Browser br, final String puid, final String fpname, final String hashTagsStr) throws Exception {
        final String gc = getGoogleCarousel(br);
        if (gc != null) {
            FilePackage fp = null;
            final String JSON = new Regex(gc, "<script type=\"application/ld\\+json\">(.*?)</script>").getMatch(0);
            final Map<String, Object> json = JavaScriptEngineFactory.jsonToJavaMap(JSON);
            final String articleBody = (String) json.get("articleBody");
            final String postURL = (String) json.get("url");
            if (fpname != null) {
                fp = FilePackage.getInstance();
                fp.setName(fpname);
                if (articleBody != null) {
                    fp.setComment(articleBody.replaceAll("[\r\n]+", "").trim());
                }
            }
            // single entry objects are not in 'list'
            List<Object> results = null;
            try {
                results = (List<Object>) JavaScriptEngineFactory.walkJson(json, "image/@list");
            } catch (Throwable t) {
                // single entry ?
                final String[] a = new String[] { (String) json.get("image") };
                results = new ArrayList<Object>(Arrays.asList(a));
            }
            if (results != null) {
                int count = 1;
                final DecimalFormat df = new DecimalFormat(results.size() < 100 ? "00" : "000");
                for (final Object result : results) {
                    final String url = (String) result;
                    final DownloadLink dl = createDownloadlink(url);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    // cleanup... im
                    final String name = fpname == null ? "" : fpname;
                    final String filename = setFileName(cleanupName(df.format(count) + " - " + name), puid) + getFileNameExtensionFromString(url);
                    if (cfg.isUseOriginalFilenameEnabled()) {
                        dl.setName(extractFileNameFromURL(url));
                    } else {
                        dl.setFinalFileName(filename);
                    }
                    dl.setContentUrl(postURL);
                    dl.setAvailable(true);
                    if (hashTagsStr != null) {
                        dl.setProperty(PROPERTY_TAGS, hashTagsStr);
                    }
                    dl.setComment(hashTagsStr);
                    decryptedLinks.add(dl);
                    count++;
                }
            }
        }
    }

    /**
     * image url contains the puid!
     *
     * @author raztoki
     * @param url
     * @param name
     * @param puid
     * @return
     * @throws Exception
     */
    private ArrayList<DownloadLink> processImageWebsite(final CryptedLink param, final String url, final String name, final String ppuid) throws Exception {
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String puid;
        if (ppuid == null) {
            puid = new Regex(url, "(\\d+)$").getMatch(0);
        } else {
            puid = ppuid;
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        final Browser br = this.br.cloneBrowser();
        br.getPage(url);
        checkErrorsWebsite(this.br);
        br.followRedirect(true);
        final String filename;
        if (name != null) {
            filename = name;
        } else {
            // we can find a name on this page also!
            String n = br.getRegex("<title>(.*?) : Photo</title>").getMatch(0);
            if (n == null) {
                n = br.getRegex("<h1 class=\"blog_title\">(.*?)</h1>").getMatch(0);
            }
            // cleanup...
            filename = setFileName(cleanupName(n), puid);
        }
        String externID = null;
        if (url.contains("demo.tumblr.com/image/")) {
            externID = br.getRegex("data\\-src=\"(https?://(?:www\\.)?tumblr\\.com/photo/[^<>\"]*?)\"").getMatch(0);
        } else {
            externID = getBiggestPictureWebsite(br);
        }
        if (externID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink dl = createDownloadlink("directhttp://" + externID);
        dl.setDownloadPassword(param.getDecrypterPassword());
        // determine file extension
        final String ext = getFileNameExtensionFromURL(externID);
        if (cfg.isUseOriginalFilenameEnabled()) {
            dl.setName(extractFileNameFromURL(externID));
        } else {
            dl.setFinalFileName(filename + ext);
        }
        dl.setContentUrl(url);
        dl.setAvailable(true);
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    private String setFileName(final String fpName, final String puid) {
        String filename = (puid != null ? puid : "");
        filename += (filename.length() > 0 && fpName != null ? " - " : "") + (fpName != null ? fpName : "");
        return filename;
    }

    private String cleanupName(final String name) {
        final String result = name != null ? name.replaceFirst("\\.{1,}$", "") : null;
        return result;
    }

    @Deprecated
    private ArrayList<DownloadLink> decryptUserWebsite(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String nextPage = "";
        int counter = 1;
        final boolean decryptSingle = param.getCryptedUrl().matches("/page/\\d+");
        br.getPage(param.getCryptedUrl());
        br.followRedirect(true);
        if (br.containsHTML(GENERALOFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        handlePassword(param);
        br.followRedirect(true);
        final String maxAgeParam = new Regex(param.getCryptedUrl(), "(#|%23)maxage=(\\d+(d|w)?)").getMatch(1);
        final long maxAgeTimestamp;
        if (maxAgeParam != null) {
            final String number = new Regex(maxAgeParam, "(\\d+)").getMatch(0);
            if (maxAgeParam.contains("w")) {
                maxAgeTimestamp = System.currentTimeMillis() - Integer.parseInt(number) * 7 * 24 * 60 * 60 * 1000l;
            } else {
                maxAgeTimestamp = System.currentTimeMillis() - Integer.parseInt(number) * 24 * 60 * 60 * 1000l;
            }
        } else {
            maxAgeTimestamp = -1;
        }
        final FilePackage fp = FilePackage.getInstance();
        String fpName = new Regex(param.getCryptedUrl(), "//(.+?)\\.tumblr").getMatch(0);
        fp.setName(fpName);
        do {
            if (!"".equals(nextPage)) {
                br.getPage(nextPage);
            }
            if (param.getCryptedUrl().contains("/archive")) {
                // archive we will need to do things differently!
                final boolean urlsOnly;
                String[] postSrcs = this.br.getRegex("<div class=\"post post_micro[^\"]+\".*?</span>\\s*</div>").getColumn(-1);
                if (postSrcs == null || postSrcs.length == 0) {
                    urlsOnly = true;
                    postSrcs = br.getRegex("<a target=\"_blank\" class=\"hover\" title=\"[^\"]*\" href=\"(http[^<>\"]+)\"").getColumn(0);
                } else {
                    urlsOnly = false;
                }
                if (postSrcs != null) {
                    for (String postSrc : postSrcs) {
                        String postDate = urlsOnly ? null : new Regex(postSrc, "class=\"post_date\">([^<>\"]+)</span>").getMatch(0);
                        final String postUrl = urlsOnly ? postSrc : new Regex(postSrc, "(https?://[A-Za-z0-9\\-_]+\\.tumblr\\.com/post/[^<>\"]+)").getMatch(0);
                        if (postUrl == null) {
                            continue;
                        } else if (maxAgeTimestamp > 0) {
                            if (postDate == null) {
                                logger.warning("User limited post age via maxage parameter but plugin failed to find source date");
                                return decryptedLinks;
                            } else {
                                postDate = postDate.trim();
                                final long postDateLong = TimeFormatter.getMilliSeconds(postDate, "MMMM dd, yyyy", Locale.ENGLISH);
                                if (postDateLong != -1 && postDateLong < maxAgeTimestamp) {
                                    logger.info("Stopping as posts are older than what the user wants");
                                    return decryptedLinks;
                                }
                            }
                        }
                        final DownloadLink dl = createDownloadlink(postUrl);
                        fp.add(dl);
                        distribute(dl);
                        decryptedLinks.add(dl);
                    }
                }
            } else {
                // identify all posts on page then filter accordingly. best way to do this is via google carousel, due to different page
                // layouts(templates) can be hard to find what you're looking for.
                final String gc = getGoogleCarousel(br);
                if (gc != null) {
                    final String JSON = new Regex(gc, "<script type=\"application/ld\\+json\">(.*?)</script>").getMatch(0);
                    final Map<String, Object> json = JavaScriptEngineFactory.jsonToJavaMap(JSON);
                    final List<Object> results = (List<Object>) json.get("itemListElement");
                    for (final Object result : results) {
                        final Map<String, Object> j = (Map<String, Object>) result;
                        String url = (String) j.get("url");
                        final String url_corrected = new Regex(url, "(.+/post/\\d+)").getMatch(0);
                        if (url_corrected != null) {
                            /*
                             * 2017-04-26: Sometimes, URLs to single posts contain invalid stuff at the end so let's RegEx them here so that
                             * the "password" handling below fits the plugins' RegEx.
                             */
                            url = url_corrected;
                        }
                        if (param.getDecrypterPassword() != null) {
                            url += "?password=" + Encoding.urlEncode(param.getDecrypterPassword());
                        }
                        final DownloadLink dl = super.createDownloadlink(url);
                        dl.setDownloadPassword(param.getDecrypterPassword());
                        fp.add(dl);
                        distribute(dl);
                        decryptedLinks.add(dl);
                    }
                }
            }
            if (decryptSingle) {
                break;
            }
            nextPage = param.getCryptedUrl().contains("/archive") ? br.getRegex("\"(/archive(?:/[^\"]*)?\\?before_time=\\d+)\">Next Page").getMatch(0) : br.getRegex("\"(/page/" + ++counter + ")\"").getMatch(0);
        } while (!this.isAbort() && nextPage != null);
        logger.info("Decryption done - last 'nextPage' value was: " + nextPage);
        return decryptedLinks;
    }

    /** https://www.tumblr.com/docs/en/api/v2#userlikes--retrieve-a-users-likes */
    private ArrayList<DownloadLink> decryptUsersOwnLikes(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* TODO: Add pagination */
        br.getPage(TumblrCom.API_BASE + "/user/likes");
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (Map<String, Object>) entries.get("response");
        final int likedCount = ((Number) entries.get("liked_count")).intValue();
        if (likedCount <= 0) {
            logger.info("Currently loggedIN user has no likes at all");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        logger.info("Crawling " + likedCount + " liked posts...");
        final List<Object> likedPosts = (List<Object>) entries.get("liked_posts");
        this.crawlMultiplePostsArrayJsonAPI(ret, likedPosts);
        return ret;
    }

    /**
     * Crawls all posts of a blog via API: </br>
     * https://www.tumblr.com/docs/en/api/v2#posts--retrieve-published-posts
     */
    private ArrayList<DownloadLink> decryptUserAPI(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String username = getUsername(param.getCryptedUrl());
        if (username == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // br.getPage(TumblrCom.API_BASE + "/blog/" + Encoding.urlEncode(username) + ".tumblr.com/info");
        br.getPage(TumblrCom.API_BASE + "/blog/" + Encoding.urlEncode(username) + ".tumblr.com/posts");
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> blog = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "response/blog");
        final int postsCount = ((Number) blog.get("posts")).intValue();
        if (postsCount == 0) {
            logger.info("This blog doesn't contain any posts");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(username);
        int pageIndex = 0;
        int crawledPosts = 0;
        do {
            logger.info("Crawling page: " + (pageIndex + 1) + ": " + this.br.getURL());
            final List<Object> postsO = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "response/posts");
            final ArrayList<DownloadLink> nextPosts = new ArrayList<DownloadLink>();
            this.crawlMultiplePostsArrayJsonAPI(nextPosts, postsO);
            ret.addAll(nextPosts);
            final String nextPageURL = (String) JavaScriptEngineFactory.walkJson(entries, "response/_links/next/href");
            crawledPosts += postsO.size();
            logger.info("Crawled posts: " + crawledPosts + " / " + postsCount);
            if (this.isAbort()) {
                break;
            } else if (StringUtils.isEmpty(nextPageURL)) {
                logger.info("Stopping because: nextURL is not given -> Probably reached end");
                break;
            } else if (crawledPosts >= postsCount) {
                logger.info("Stopping because: Found all items");
                break;
            } else {
                pageIndex++;
                br.getPage(TumblrCom.API_BASE_WITHOUT_VERSION + nextPageURL);
                entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            }
        } while (true);
        return ret;
    }

    @Deprecated
    private boolean handlePassword(final CryptedLink param) throws DecrypterException, IOException {
        if ((this.br.getRedirectLocation() != null && this.br.getRedirectLocation().contains(urlpart_passwordneeded)) || this.br.getURL().contains(urlpart_passwordneeded)) {
            logger.info("Blog password needed");
            // final String password_required_url;
            // if (this.br.getRedirectLocation() != null) {
            // password_required_url = this.br.getRedirectLocation();
            // } else {
            // password_required_url = this.br.getURL();
            // }
            // final String blog_user = new Regex(password_required_url, "/blog_auth/(.+)").getMatch(0);
            // if (blog_user != null) {
            // this.br = prepBR(new Browser());
            // this.br.setFollowRedirects(true);
            // this.br.getPage("https://www.tumblr.com/blog_auth/" + blog_user);
            // } else {
            // this.br.setFollowRedirects(true);
            // }
            boolean success = false;
            String passCode = null;
            for (int i = 0; i <= 2; i++) {
                if (param.getDecrypterPassword() != null && i == 0) {
                    /* Try saved/given password if possible. */
                    passCode = param.getDecrypterPassword();
                } else {
                    passCode = getUserInput("Password?", param);
                }
                Form form = br.getFormbyKey("auth");
                if (form == null) {
                    form = br.getFormbyKey("password");
                }
                form.put("password", Encoding.urlEncode(passCode));
                br.submitForm(form);
                form = br.getFormbyKey("auth");
                if (form != null) {
                    form.put("password", Encoding.urlEncode(passCode));
                    br.submitForm(form);
                }
                if (this.br.getURL().contains(urlpart_passwordneeded)) {
                    passCode = null;
                    continue;
                }
                success = true;
                break;
            }
            if (!success) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            param.setDecrypterPassword(passCode);
            this.br.setFollowRedirects(false);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Improved best picture finder, using for loop with finding select i value == slow as shit and wasteful on CPU cycles.
     *
     * @param br
     * @return
     */
    private String getBiggestPictureWebsite(final Browser br) {
        String image = null;
        // faster method to find best picture.
        final String pattern = "(https?://\\d+\\.media\\.tumblr\\.com(?:/[a-z0-9]{32})?/tumblr_[A-Za-z0-9_]+_(\\d+)\\.(?:jpe?g|gif|png))";
        final String[] images = br.getRegex("(\"|')" + pattern + "\\1").getColumn(1);
        int largest = -1;
        if (images != null) {
            for (final String img : images) {
                final String qual = new Regex(img, pattern).getMatch(1);
                final int quality = Integer.parseInt(qual);
                if (quality > largest) {
                    largest = quality;
                    image = img;
                }
            }
        }
        return image;
    }

    private void checkErrorsWebsite(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().contains("/login_required/")) {
            throw new AccountRequiredException();
        }
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}