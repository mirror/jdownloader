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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "reddit.com" }, urls = { "https?://(?:www\\.)?reddit\\.com/r/([^/]+)/comments/([a-z0-9]+)/[A-Za-z0-9\\-_]+" })
public class RedditCom extends PluginForDecrypt {
    public RedditCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-07-21: Let's be gentle and avoid doing too many API requests. */
        return 1;
    }

    @Override
    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink dl = super.createDownloadlink(url);
        if (fp != null) {
            dl._setFilePackage(fp);
        }
        return dl;
    }

    private FilePackage         fp                    = null;
    private static final String TYPE_SELFHOSTED_VIDEO = "https?://v\\.redd\\.it/[a-z0-9]+.*";
    private static final String TYPE_SELFHOSTED_IMAGE = "https?://i\\.redd\\.it/[a-z0-9]+.*";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String subredditTitle = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String commentID = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        // final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        // if (acc == null) {
        // throw new AccountRequiredException();
        // }
        // final PluginForHost plugin = JDUtilities.getPluginForHost(this.getHost());
        // plugin.setBrowser(this.br);
        // ((jd.plugins.hoster.RedditCom) plugin).login(acc, false);
        /* According to: https://www.reddit.com/r/redditdev/comments/b8yd3r/reddit_api_possible_to_get_posts_by_id/ */
        br.getPage("https://www.reddit.com/comments/" + commentID + "/.json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries;
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        /* https://www.reddit.com/dev/api/#fullnames */
        /* [0] = post/"first comment" */
        /* [1] = Comments */
        for (final Object postO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) postO;
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/children/{0}/data");
            final long createdTimestamp = JavaScriptEngineFactory.toLong(entries.get("created"), 0) * 1000;
            final Date theDate = new Date(createdTimestamp);
            final String dateFormatted = new SimpleDateFormat("yyy-MM-dd").format(theDate);
            String title = (String) entries.get("title");
            if (StringUtils.isEmpty(title)) {
                /* Fallback */
                title = commentID;
            }
            title = dateFormatted + "_" + subredditTitle + " - " + title;
            fp = FilePackage.getInstance();
            fp.setName(title);
            /* 2020-07-23: TODO: This field might indicate selfhosted content: is_reddit_media_domain */
            /* Look for single URLs e.g. single pictures (e.g. often imgur.com URLs, can also be selfhosted content) */
            boolean addedRedditSelfhostedVideo = false;
            final String externalURL = (String) entries.get("url");
            if (!StringUtils.isEmpty(externalURL)) {
                logger.info("Found external URL");
                final DownloadLink dl = this.createDownloadlink(externalURL);
                if (externalURL.matches(TYPE_SELFHOSTED_VIDEO)) {
                    addedRedditSelfhostedVideo = true;
                    dl.setFinalFileName(title + ".mp4");
                } else if (externalURL.matches(TYPE_SELFHOSTED_IMAGE)) {
                    dl.setFinalFileName(title + ".jpg");
                }
                decryptedLinks.add(dl);
            }
            /* Look for embedded content from external sources - the object is always given but can be empty */
            final Object embeddedMediaO = entries.get("media_embed");
            if (embeddedMediaO != null) {
                LinkedHashMap<String, Object> embeddedMediaInfo = (LinkedHashMap<String, Object>) embeddedMediaO;
                if (!embeddedMediaInfo.isEmpty()) {
                    logger.info("Found media_embed");
                    String media_embedStr = (String) embeddedMediaInfo.get("content");
                    final String[] links = HTMLParser.getHttpLinks(media_embedStr, this.br.getURL());
                    for (final String url : links) {
                        decryptedLinks.add(this.createDownloadlink(url));
                    }
                }
            }
            /* Look for selfhosted video content. Prefer content without https */
            if (!addedRedditSelfhostedVideo) {
                final String[] mediaTypes = new String[] { "media", "secure_media" };
                for (final String mediaType : mediaTypes) {
                    final Object mediaO = entries.get(mediaType);
                    if (mediaO != null) {
                        final LinkedHashMap<String, Object> mediaInfo = (LinkedHashMap<String, Object>) mediaO;
                        if (!mediaInfo.isEmpty()) {
                            logger.info("Found mediaType '" + mediaType + "'");
                            /* This is not always given */
                            final Object redditVideoO = mediaInfo.get("reddit_video");
                            if (redditVideoO != null) {
                                final LinkedHashMap<String, Object> redditVideo = (LinkedHashMap<String, Object>) redditVideoO;
                                String hls_url = (String) redditVideo.get("hls_url");
                                if (!StringUtils.isEmpty(hls_url)) {
                                    if (Encoding.isHtmlEntityCoded(hls_url)) {
                                        hls_url = Encoding.htmlDecode(hls_url);
                                    }
                                    decryptedLinks.add(this.createDownloadlink(hls_url));
                                }
                            }
                        }
                    }
                    /* Stop once one type has been found! */
                    break;
                }
            }
            /* Look for selfhosted photo content, Only add image if nothing else is found */
            if (decryptedLinks.size() == 0) {
                final ArrayList<Object> imagesO = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "preview/images");
                if (imagesO != null) {
                    logger.info(String.format("Found %d selfhosted images", imagesO.size()));
                    for (final Object imageO : imagesO) {
                        final LinkedHashMap<String, Object> imageInfo = (LinkedHashMap<String, Object>) imageO;
                        final String bestImage = (String) JavaScriptEngineFactory.walkJson(imageInfo, "source/url");
                        if (bestImage != null) {
                            final DownloadLink dl = this.createDownloadlink("directhttp://" + bestImage);
                            dl.setAvailable(true);
                            decryptedLinks.add(dl);
                        }
                    }
                } else {
                    logger.info("Failed to find selfhosted image(s)");
                }
            }
            /* Look for URLs inside post text. selftext is always present but empty when not used. */
            final String postText = (String) entries.get("selftext");
            if (!StringUtils.isEmpty(postText)) {
                logger.info("Looking for URLs in selftext");
                String[] urls = HTMLParser.getHttpLinks(postText, null);
                if (urls.length > 0) {
                    logger.info(String.format("Found %d URLs in selftext", urls.length));
                    for (final String url : urls) {
                        decryptedLinks.add(this.createDownloadlink(url));
                    }
                } else {
                    logger.info("Failed to find any URLs in selftext");
                }
            }
            /* Only grab first post - stop then! */
            break;
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Failed to find any downloadable content");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        return decryptedLinks;
    }
}
