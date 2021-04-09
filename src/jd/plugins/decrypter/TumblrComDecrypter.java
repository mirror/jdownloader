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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.TumblrComConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.TumblrCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tumblr.com" }, urls = { "https?://(?!\\d+\\.media\\.tumblr\\.com/.+)[\\w\\.\\-]+?tumblr\\.com(?:/(audio|video)_file/\\d+/tumblr_[A-Za-z0-9]+|/image/\\d+|/post/\\d+(?:\\?password=.+)?|/?$|/blog/view/[^/]+)(?:\\?password=.+)?" })
public class TumblrComDecrypter extends PluginForDecrypt {
    public TumblrComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String GENERALOFFLINE         = "(?i)>\\s*Not found\\.<";
    private static final String TYPE_INVALID           = "https?://(?:(?:platform|embed|assets)\\.)tumblr\\.com/.+";
    /* TODO: Check if such URLs still exist and if we need crawler support for those */
    private static final String TYPE_FILE              = ".+tumblr\\.com/(audio|video)_file/\\d+/tumblr_[A-Za-z0-9]+";
    private static final String TYPE_POST              = "https?://(\\w+)\\.[^/]+/post/(\\d+)";
    private static final String TYPE_IMAGE             = ".+tumblr\\.com/image/\\d+";
    private static final String TYPE_USER_LOGGEDIN     = "https?://(?:www\\.)?tumblr\\.com/blog/view/([^/]+)";
    private static final String TYPE_USER_LOGGEDOUT    = "https?://([^/]+)\\.tumblr\\.com/.*";
    private static final String urlpart_passwordneeded = "/blog_auth";
    private static final String PROPERTY_TAGS          = "tags";
    private String              parameter              = null;
    private String              passCode               = null;
    private boolean             useOriginalFilename    = false;

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return TumblrComConfig.class;
    }

    private boolean isBlog(final String url) {
        return url.matches(TYPE_USER_LOGGEDIN) || url.matches(TYPE_USER_LOGGEDOUT);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (param.toString().matches(TYPE_INVALID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        parameter = param.toString().replace("www.", "");
        passCode = new Regex(parameter, "\\?password=(.+)").getMatch(0);
        if (passCode != null) {
            /* Remove this from our url as it is only needed for this decrypter internally. */
            parameter = parameter.replace("?password=" + passCode, "");
        }
        useOriginalFilename = PluginJsonConfig.get(TumblrComConfig.class).isUseOriginalFilenameEnabled();
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        if (aa != null) {
            /* Login whenever possible to be able to download account-only-stuff */
            final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.TumblrCom) plg).login(aa, false);
            if (parameter.matches(TYPE_POST)) {
                return decryptPostAPI(param);
            } else if (isBlog(param.getCryptedUrl())) {
                return decryptUserAPI(param);
            } else {
                logger.warning("Unsupported URL");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                br.setFollowRedirects(true);
                br.getPage(param.getCryptedUrl());
                br.getPage("https://www.tumblr.com/");
                final String apikey = PluginJSonUtils.getJson(br, "API_TOKEN");
                if (StringUtils.isEmpty(apikey)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("Authorization", "Bearer " + apikey);
            }
        }
        if (parameter.matches(TYPE_POST)) {
            return decryptPostWebsite(param);
        } else if (parameter.matches(TYPE_IMAGE)) {
            return processImageWebsite(parameter, null, null);
        } else {
            /*
             * 2016-08-26: Seems like when logged in, users now get the same view they have when not logged in. Using the "old" logged-in
             * method, the crawler will not find all entries which is why we now use the normal method (again).
             */
            // if (loggedin) {
            // decryptUserLoggedIn();
            // } else {
            // parameter = convertUserUrlToLoggedOutUser();
            // decryptUser();
            // }
            parameter = convertUserUrlToLoggedOutUser();
            return decryptUser(param);
        }
    }
    // private String convertUserUrlToLoggedInUser() {
    // final String url;
    // if (this.parameter.matches(TYPE_USER_ARCHIVE)) {
    // /* Do not modify these urls */
    // url = this.parameter;
    // } else {
    // url = "https://www.tumblr.com/dashboard/blog/" + getUsername(this.parameter);
    // }
    // return url;
    // }

    private String convertUserUrlToLoggedOutUser() {
        return "https://" + getUsername(this.parameter) + ".tumblr.com/";
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
    // private void decryptFile() throws Exception {
    // br.setFollowRedirects(false);
    // br.getPage(parameter);
    // if (br.containsHTML(GENERALOFFLINE) || br.containsHTML(">Die angeforderte URL konnte auf dem Server")) {
    // this.decryptedLinks.add(this.createOfflinelink(parameter));
    // return;
    // }
    // String finallink = br.getRedirectLocation();
    // // if (parameter.matches(".+tumblr\\.com/video_file/\\d+/tumblr_[A-Za-z0-9]+")) {
    // // getPage(finallink);
    // // finallink = br.getRedirectLocation();
    // // }
    // if (finallink == null) {
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // decryptedLinks.add(createDownloadlink(finallink));
    // }

    private ArrayList<DownloadLink> decryptPostWebsite(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // lets identify the unique id for this post, only use it for tumblr hosted content
        final String puid = new Regex(parameter, TYPE_POST).getMatch(0);
        // Single posts
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.handlePassword(param)) {
            br.getPage(parameter);
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
        decryptedLinks.addAll(processGeneric(br, postBody, fpName, puid));
        return decryptedLinks;
    }

    /** https://www.tumblr.com/docs/en/api/v2#postspost-id---fetching-a-post-neue-post-format */
    private ArrayList<DownloadLink> decryptPostAPI(final CryptedLink param) throws Exception {
        // lets identify the unique id for this post, only use it for tumblr hosted content
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String username = new Regex(parameter, TYPE_POST).getMatch(0);
        final String puid = new Regex(parameter, TYPE_POST).getMatch(1);
        br.getPage(TumblrCom.API_BASE + "/blog/" + username + "/posts/" + puid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("response");
        this.crawlSinglePostJsonAPI(ret, entries);
        // final String fpName = ((String) entries.get("slug")).replace("-", " ").trim();
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(fpName);
        return ret;
    }

    private void crawlSinglePostJsonAPI(final ArrayList<DownloadLink> ret, Map<String, Object> entries) {
        // final String postURL = (String) entries.get("post_url");
        final String blogName = (String) entries.get("blog_name");
        final String fpName = blogName + " - " + ((String) entries.get("slug")).replace("-", " ").trim();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        // final int arraySizeBefore = ret.size();
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
                    /* TODO: Maybe optionally also crawl content of type "link" (== external links) */
                    continue;
                }
                final DownloadLink dl;
                if (type.equals("video")) {
                    /* Videos only have 1 version available */
                    String url = (String) JavaScriptEngineFactory.walkJson(entries, "media/url");
                    url = convertDirectVideoUrltoHD(url);
                    dl = this.createDownloadlink(url);
                } else {
                    final List<Object> versions = (List<Object>) entries.get("media");
                    /* First version = best version */
                    entries = (Map<String, Object>) versions.get(0);
                    final String url = (String) entries.get("url");
                    dl = this.createDownloadlink(url);
                    /* 2021-04-09: url can contain up to 2 MD5 hashes but neither of those is the actual file-hash! */
                    // final String md5 = new Regex(url, "([a-f0-9]{32})\\.\\w+$").getMatch(0);
                    // if (md5 != null) {
                    // dl.setMD5Hash(md5);
                    // }
                }
                dl.setAvailable(true);
                // link.setContentUrl(postURL);
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
            }
        }
        // int numberofFoundItems = ret.size() - arraySizeBefore;
        // if (numberofFoundItems == 0) {
        // logger.info("Failed to find anything: Post most likely only contained unsupported media: " + postURL);
        // }
    }

    private String getGoogleCarousel(Browser br) {
        final String result = br.getRegex("<!-- GOOGLE CAROUSEL --><script.*?</script>").getMatch(-1);
        return result;
    }

    private String findPostBody() {
        final String postBody = br.getRegex("<section id=\"posts\" class=\"content clearfix.*?</(section|article)>\\s*<section class=\"related-posts-wrapper\">").getMatch(-1);
        return postBody;
    }

    private ArrayList<DownloadLink> processGeneric(final Browser ibr, final String postBody, final String name, final String puid) throws Exception {
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
                if (!useOriginalFilename) {
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
        // is, when we need to customise / fixup results into proper url format. -raztoki20160211
        final String iframe = new Regex(string, "<iframe [^>]*src=(\"|')((?![^>]*//assets\\.tumblr\\.com/[^>]+?).*?)\\1").getMatch(1);
        if (iframe != null) {
            decryptedLinks.add(createDownloadlink(iframe));
            return decryptedLinks;
        }
        // PICTURE
        /* Access link if possible to get higher qualities e.g. *1280 --> Only needed/possible for single links. */
        final String imagelink = new Regex(string, "class=\"photo-wrapper-inner\">\\s*<a href=('|\")(https?://[a-z0-9\\-]+\\.tumblr\\.com/image/\\d+)\\1").getMatch(1);
        if (imagelink != null) {
            decryptedLinks.addAll(processImageWebsite(imagelink, filename, puid));
        } else {
            // I've only ever seen a single pic / post page!!
            String pic = br.getRegex("property=\"og:image\" content=(\"|')(https?://\\d+\\.media\\.tumblr\\.com/.*?)\\1").getMatch(1);
            if (pic == null) {
                // difference between this regex and picturelink is no <a href !! same url here as previous "pic" regex above.
                pic = new Regex(string, "class=\"photo-wrapper-inner\">\\s*<img src=('|\")(https?://\\d+\\.media\\.tumblr\\.com/.*?)\\1").getMatch(1);
            }
            if (pic != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + pic);
                if (this.passCode != null) {
                    dl.setDownloadPassword(this.passCode);
                }
                dl.setAvailable(true);
                // determine file extension
                final String ext = getFileNameExtensionFromURL(pic);
                if (!useOriginalFilename) {
                    dl.setFinalFileName(filename + ext);
                } else {
                    dl.setName(extractFileNameFromURL(pic));
                }
                setImageLinkID(dl, pic, puid);
                if (hashTagsStr != null) {
                    dl.setProperty(PROPERTY_TAGS, hashTagsStr);
                }
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        }
        logger.info("Found nothing here so the decrypter is either broken or there isn't anything to decrypt. Link: " + parameter);
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
            ArrayList<Object> results = null;
            try {
                results = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(json, "image/@list");
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
                    final DownloadLink dl = createDownloadlink("directhttp://" + url);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    // cleanup... im
                    final String name = fpname == null ? "" : fpname;
                    final String filename = setFileName(cleanupName(df.format(count) + " - " + name), puid) + getFileNameExtensionFromString(url);
                    if (!useOriginalFilename) {
                        dl.setFinalFileName(filename);
                    } else {
                        dl.setName(extractFileNameFromURL(url));
                    }
                    dl.setContentUrl(postURL);
                    dl.setAvailable(true);
                    setImageLinkID(dl, url, puid);
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

    private void setImageLinkID(final DownloadLink dl, final String url, final String puid) {
        if (puid != null) {
            final String iuid = new Regex(url, "/tumblr_([A-Za-z0-9_]+)_\\d+\\.(?:jpe?g|gif|png)").getMatch(0);
            if (iuid != null) {
                dl.setLinkID(this.getHost() + "://" + puid + "/" + iuid);
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
    private ArrayList<DownloadLink> processImageWebsite(final String url, final String name, final String ppuid) throws Exception {
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
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(url));
            return decryptedLinks;
        }
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
            externID = br.getRegex("data\\-src=\"(https?://(www\\.)?tumblr\\.com/photo/[^<>\"]*?)\"").getMatch(0);
        } else {
            externID = getBiggestPictureWebsite(br);
        }
        if (externID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink dl = createDownloadlink("directhttp://" + externID);
        if (this.passCode != null) {
            dl.setDownloadPassword(this.passCode);
        }
        // determine file extension
        final String ext = getFileNameExtensionFromURL(externID);
        if (!useOriginalFilename) {
            dl.setFinalFileName(filename + ext);
        } else {
            dl.setName(extractFileNameFromURL(externID));
        }
        setImageLinkID(dl, externID, puid);
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

    private ArrayList<DownloadLink> decryptUser(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String nextPage = "";
        int counter = 1;
        final boolean decryptSingle = parameter.matches("/page/\\d+");
        br.getPage(parameter);
        br.followRedirect(true);
        if (br.containsHTML(GENERALOFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        handlePassword(param);
        br.followRedirect(true);
        final String maxAgeParam = new Regex(parameter, "(#|%23)maxage=(\\d+(d|w)?)").getMatch(1);
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
        String fpName = new Regex(parameter, "//(.+?)\\.tumblr").getMatch(0);
        fp.setName(fpName);
        do {
            if (!"".equals(nextPage)) {
                br.getPage(nextPage);
            }
            if (parameter.contains("/archive")) {
                // archive we will need todo things differently!
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
                // identify all posts on page then filter accordingly. best way todo this is via google carousel, due to different page
                // layouts(templates) can be hard to find what you're looking for.
                final String gc = getGoogleCarousel(br);
                if (gc != null) {
                    final String JSON = new Regex(gc, "<script type=\"application/ld\\+json\">(.*?)</script>").getMatch(0);
                    final Map<String, Object> json = JavaScriptEngineFactory.jsonToJavaMap(JSON);
                    final ArrayList<Object> results = (ArrayList<Object>) json.get("itemListElement");
                    for (final Object result : results) {
                        final LinkedHashMap<String, Object> j = (LinkedHashMap<String, Object>) result;
                        final String url = (String) j.get("url");
                        final DownloadLink dl = createDownloadlinkTumblr(url);
                        fp.add(dl);
                        distribute(dl);
                        decryptedLinks.add(dl);
                    }
                }
            }
            if (decryptSingle) {
                break;
            }
            nextPage = parameter.contains("/archive") ? br.getRegex("\"(/archive(?:/[^\"]*)?\\?before_time=\\d+)\">Next Page").getMatch(0) : br.getRegex("\"(/page/" + ++counter + ")\"").getMatch(0);
        } while (!this.isAbort() && nextPage != null);
        logger.info("Decryption done - last 'nextPage' value was: " + nextPage);
        return decryptedLinks;
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
        /* 2021-04-08: TODO: Limit fields we request so we only get what we need in order to prevent wasting resources (?) */
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
            for (final Object postO : postsO) {
                this.crawlSinglePostJsonAPI(ret, (Map<String, Object>) postO);
            }
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

    private boolean handlePassword(final CryptedLink param) throws DecrypterException, IOException {
        final boolean passwordRequired;
        if ((this.br.getRedirectLocation() != null && this.br.getRedirectLocation().contains(urlpart_passwordneeded)) || this.br.getURL().contains(urlpart_passwordneeded)) {
            logger.info("Blog password needed");
            passwordRequired = true;
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
            for (int i = 0; i <= 2; i++) {
                if (this.passCode == null) {
                    this.passCode = getUserInput("Password?", param);
                }
                Form form = br.getFormbyKey("auth");
                if (form == null) {
                    form = br.getFormbyKey("password");
                }
                form.put("password", Encoding.urlEncode(this.passCode));
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
            this.br.setFollowRedirects(false);
        } else {
            passwordRequired = false;
        }
        return passwordRequired;
    }

    /** For urls which will go back into the decrypter. */
    private DownloadLink createDownloadlinkTumblr(String url) {
        final String url_corrected = new Regex(url, "(.+/post/\\d+)").getMatch(0);
        if (url_corrected != null) {
            /*
             * 2017-04-26: Sometimes, URLs to single posts contain invalid stuff at the end so let's RegEx them here so that the "password"
             * handling below fots the plugins' RegEx.
             */
            url = url_corrected;
        }
        if (this.passCode != null) {
            url += "?password=" + this.passCode;
        }
        final DownloadLink dl = super.createDownloadlink(url);
        if (this.passCode != null) {
            dl.setDownloadPassword(this.passCode);
        }
        return dl;
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

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}