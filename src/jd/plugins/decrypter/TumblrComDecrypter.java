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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tumblr.com" }, urls = { "https?://(?!\\d+\\.media\\.tumblr\\.com/.+)[\\w\\.\\-]+?tumblr\\.com(?:/(audio|video)_file/\\d+/tumblr_[A-Za-z0-9]+|/image/\\d+|/post/\\d+|/?$|/archive(?:/.*?)?)" }, flags = { 0 })
public class TumblrComDecrypter extends PluginForDecrypt {

    public TumblrComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     GENERALOFFLINE = ">Not found\\.<";

    private static final String     TYPE_FILE      = ".+tumblr\\.com/(audio|video)_file/\\d+/tumblr_[A-Za-z0-9]+";
    private static final String     TYPE_POST      = ".+tumblr\\.com/post/\\d+";
    private static final String     TYPE_IMAGE     = ".+tumblr\\.com/image/\\d+";

    private static final String     PLUGIN_DEFECT  = "PLUGINDEFECT";
    private static final String     OFFLINE        = "OFFLINE";

    private ArrayList<DownloadLink> decryptedLinks = null;
    private LinkedHashSet<String>   dupe           = null;
    private String                  parameter      = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        decryptedLinks = new ArrayList<DownloadLink>();
        dupe = new LinkedHashSet<String>();
        parameter = param.toString().replace("www.", "");
        try {
            if (parameter.matches(TYPE_FILE)) {
                decryptFile();
            } else if (parameter.matches(TYPE_POST)) {
                decryptPost();
            } else if (parameter.matches(TYPE_IMAGE)) {
                decryptedLinks.addAll(processImage(parameter, null, null));
            } else {
                decryptUser();
            }
        } catch (final BrowserException e) {
            logger.info("Server error, couldn't decrypt link: " + parameter);
            return decryptedLinks;
        } catch (final UnknownHostException eu) {
            logger.info("UnknownHostException, couldn't decrypt link: " + parameter);
            return decryptedLinks;
        } catch (final DecrypterException d) {
            if (StringUtils.equals(d.getCause().toString(), PLUGIN_DEFECT)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            } else if (StringUtils.equals(d.getCause().toString(), OFFLINE)) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            throw d;
        }
        return decryptedLinks;
    }

    private void decryptFile() throws Exception {
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML(GENERALOFFLINE) || br.containsHTML(">Die angeforderte URL konnte auf dem Server")) {
            this.decryptedLinks.add(this.createOfflinelink(parameter));
            return;
        }
        String finallink = br.getRedirectLocation();
        // if (parameter.matches(".+tumblr\\.com/video_file/\\d+/tumblr_[A-Za-z0-9]+")) {
        // getPage(finallink);
        // finallink = br.getRedirectLocation();
        // }
        if (finallink == null) {
            throw new DecrypterException(PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(finallink));
    }

    private void decryptPost() throws Exception {
        // lets identify the unique id for this post, only use it for tumblr hosted content
        final String puid = new Regex(parameter, "/post/(\\d+)").getMatch(0);

        // Single posts
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline (error 404): " + parameter);
            return;
        }
        /* Workaround for bad redirects --> Redirectloop */
        String redirect = br.getRedirectLocation();
        if (br.getRedirectLocation() != null) {
            br.getPage(redirect);
        }
        String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fpName == null) {
            // use google carousel json...
            fpName = PluginJSonUtils.getJson(getGoogleCarousel(br), "articleBody");
            if (fpName == null) {
                // this can get false positives. eg. "the funniest posts on tumblr," thenwhatyouwanthere.
                fpName = br.getRegex("<meta name=\"description\" content=\"([^/\"]+)").getMatch(0);
                if (fpName == null) {
                    fpName = "Tumblr post " + puid;
                }
            }
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        fpName = fpName.replace("\n", "");
        // isolate post html
        final String postBody = findPostBody();
        decryptedLinks.addAll(processGeneric(br, postBody, fpName, puid));
    }

    private String getGoogleCarousel(Browser br) {
        final String result = br.getRegex("<!-- GOOGLE CAROUSEL --><script.*?</script>").getMatch(-1);
        return result;
    }

    private String findPostBody() {
        final String postBody = br.getRegex("<section id=\"posts\" class=\"content clearfix.*?</section>\\s*<section class=\"related-posts-wrapper\">").getMatch(-1);
        return postBody;
    }

    private ArrayList<DownloadLink> processGeneric(final Browser ibr, final String input, final String name, final String puid) throws Exception {
        final String string = input != null ? input : ibr.toString();
        // some generic cleanup of fpName!
        final String fpName = cleanupName(name);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final Browser br = ibr.cloneBrowser();
        String externID = new Regex(string, "(https?://(www\\.)?gasxxx\\.com/media/player/config_embed\\.php\\?vkey=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = new Regex(string, "<src>(https?://.+\\.flv)</src>").getMatch(0);
            if (externID == null) {
                throw new DecrypterException(PLUGIN_DEFECT);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(fpName + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = new Regex(string, "\"(https?://video\\.vulture\\.com/video/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            br.getPage(Encoding.htmlDecode(externID));
            String cid = br.getRegex("\\&media_type=video\\&content=([A-Z0-9]+)\\&").getMatch(0);
            if (cid == null) {
                throw new DecrypterException(PLUGIN_DEFECT);
            }
            br.getPage("//video.vulture.com/item/player_embed.js/" + cid);
            externID = br.getRegex("(https?://videos\\.cache\\.magnify\\.net/[^<>\"]*?)\\'").getMatch(0);
            if (externID == null) {
                throw new DecrypterException(PLUGIN_DEFECT);
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
        externID = new Regex(string, "name=\"twitter:player\" content=\"(https?://(www\\.)?youtube\\.com/v/[A-Za-z0-9\\-_]+)\\&").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = new Regex(string, "class=\"vine\\-embed\" src=\"(https?://vine\\.co/[^<>\"]*?)\"").getMatch(0);
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
                externID = br.getRegex("\"(https?://(www\\.)?tumblr\\.com/video_file/[^<>\"]*?)\"").getMatch(0);
            }
        }
        if (externID != null) {
            if (externID.matches(".+tumblr\\.com/video_file/.+")) {
                br.setFollowRedirects(false);
                // the puid stays the same throughout all these requests
                br.getPage(externID);
                externID = br.getRedirectLocation();
                if (externID != null && externID.matches("https?://www\\.tumblr\\.com/video_file/.+")) {
                    br.getPage(externID);
                    externID = br.getRedirectLocation();
                }
                externID = externID.replace("#_=_", "");
                final DownloadLink dl = createDownloadlink(externID);
                String extension = getFileNameExtensionFromURL(externID);
                if (extension == null) {
                    extension = ".mp4"; // DirectHTTP
                }
                dl.setLinkID(getHost() + "://" + puid);
                dl.setFinalFileName(filename + extension);
                decryptedLinks.add(dl);
            } else {
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setLinkID(getHost() + "://" + puid);
                decryptedLinks.add(dl);
            }
            return decryptedLinks;
        }
        // FINAL FAILOVER FOR UNSUPPORTED CONTENT, this way we wont have to keep making updates to this plugin! only time we would need to
        // is, when we need to customise / fixup results into proper url format. -raztoki20160211

        final String iframe = new Regex(string, "<iframe [^>]*src=(\"|')((?![^>]+assets\\.tumblr\\.com/[^>]+).*?)\\1").getMatch(1);
        if (iframe != null) {
            // multiple images in a single post show up as photoset within iframe!
            if (iframe.contains("/photoset_iframe/")) {
                // ok we don't need to process the iframe src link as best images which we are interested in are within google
                // getGoogleCarousel!
                processPhotoSet(decryptedLinks, puid);
                return decryptedLinks;
            }
            decryptedLinks.add(createDownloadlink(iframe));
            return decryptedLinks;
        }

        // PICTURE

        /* Access link if possible to get higher qualities e.g. *1280 --> Only needed/possible for single links. */
        final String imagelink = new Regex(string, "class=\"photo-wrapper-inner\">\\s*<a href=('|\")(https?://[a-z0-9\\-]+\\.tumblr\\.com/image/\\d+)\\1").getMatch(1);
        if (imagelink != null) {
            decryptedLinks.addAll(processImage(imagelink, filename, puid));
        } else {
            // I've only ever seen a single pic / post page!!
            String pic = br.getRegex("property=\"og:image\" content=(\"|')(https?://\\d+\\.media\\.tumblr\\.com/.*?)\\1").getMatch(1);
            if (pic == null) {
                // difference between this regex and picturelink is no <a href !! same url here as previous "pic" regex above.
                pic = new Regex(string, "class=\"photo-wrapper-inner\">\\s*<img src=('|\")(https?://\\d+\\.media\\.tumblr\\.com/.*?)\\1").getMatch(1);
            }
            if (pic != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + pic);
                dl.setAvailable(true);
                // determine file extension
                final String ext = getFileNameExtensionFromURL(pic);
                dl.setFinalFileName(filename + ext);
                setMD5Hash(dl, pic);
                setImageLinkID(dl, pic, puid);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        }
        logger.info("Found nothing here so the decrypter is either broken or there isn't anything to decrypt. Link: " + parameter);
        return decryptedLinks;
    }

    private void processPhotoSet(final ArrayList<DownloadLink> decryptedLinks, final String puid) throws Exception {
        final String gc = getGoogleCarousel(br);
        if (gc != null) {
            FilePackage fp = null;
            final String JSON = new Regex(gc, "<script type=\"application/ld\\+json\">(.*?)</script>").getMatch(0);
            final Map<String, Object> json = jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaMap(JSON);
            final String articleBody = (String) json.get("articleBody");
            if (articleBody != null) {
                fp = FilePackage.getInstance();
                fp.setName(articleBody.replaceAll("[\r\n]+", "").trim());
            }
            final List<Object> results = (List<Object>) jd.plugins.hoster.DummyScriptEnginePlugin.walkJson(json, "image/@list");
            if (results != null) {
                for (final Object result : results) {
                    final String url = (String) result;
                    final DownloadLink dl = createDownloadlink("directhttp://" + url);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    setMD5Hash(dl, url);
                    setImageLinkID(dl, url, puid);
                    decryptedLinks.add(dl);
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

    private void setMD5Hash(final DownloadLink dl, final String url) {
        // nullified at this stage.
        if (true) {
            return;
        }
        // the 32hex is actually the file md5, it also shows up on download under the ETAG header response -raztoki20160215
        final String md5 = new Regex(url, "/([a-f0-9]{32})/").getMatch(0);
        dl.setMD5Hash(md5);
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
    private ArrayList<DownloadLink> processImage(final String url, final String name, final String ppuid) throws Exception {
        if (url == null) {
            throw new DecrypterException(PLUGIN_DEFECT);
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
            externID = br.getRegex("data\\-src=\"(http://(www\\.)?tumblr\\.com/photo/[^<>\"]*?)\"").getMatch(0);
        } else {
            externID = getBiggestPicture(br);
        }
        if (externID == null) {
            throw new DecrypterException(PLUGIN_DEFECT);
        }
        final DownloadLink dl = createDownloadlink("directhttp://" + externID);
        // determine file extension
        final String ext = getFileNameExtensionFromURL(externID);
        dl.setFinalFileName(filename + ext);
        setMD5Hash(dl, externID);
        setImageLinkID(dl, externID, puid);
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
        ;
        return result;
    }

    private void decryptUser() throws Exception {
        String nextPage = "";
        int counter = 1;
        boolean decryptSingle = parameter.matches("/page/\\d+");
        br.getPage(parameter);
        if (br.containsHTML(GENERALOFFLINE)) {
            logger.info("Link offline: " + parameter);
            return;
        }
        final FilePackage fp = FilePackage.getInstance();
        String fpName = new Regex(parameter, "//(.+?)\\.tumblr").getMatch(0);
        fp.setName(fpName);
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return;
            }
            if (!"".equals(nextPage)) {
                br.getPage(nextPage);
            }
            if (parameter.contains("/archive")) {
                // archive we will need todo things differently!
                final String[] posts = br.getRegex("<a target=\"_blank\" class=\"hover\" title=\"[^\"]*\" href=\"(.*?)\"").getColumn(0);
                if (posts != null) {
                    for (final String post : posts) {
                        final DownloadLink dl = createDownloadlink(post);
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
                    final Map<String, Object> json = jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaMap(JSON);
                    final ArrayList<Object> results = (ArrayList<Object>) json.get("itemListElement");
                    for (final Object result : results) {
                        final LinkedHashMap<String, Object> j = (LinkedHashMap<String, Object>) result;
                        final String url = (String) j.get("url");
                        final DownloadLink dl = createDownloadlink(url);
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
        } while (nextPage != null);
        logger.info("Decryption done - last 'nextPage' value was: " + nextPage);

    }

    /**
     * Improved best picture finder, using for loop with finding select i value == slow as shit and wasteful on CPU cycles.
     *
     * @param br
     * @return
     */
    private String getBiggestPicture(final Browser br) {
        String image = null;
        // faster method to find best picture.
        final String pattern = "(http://\\d+\\.media\\.tumblr\\.com(?:/[a-z0-9]{32})?/tumblr_[A-Za-z0-9_]+_(\\d+)\\.(?:jpe?g|gif|png))";
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}