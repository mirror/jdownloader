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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.AdultempireCom;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.GenericM3u8;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AdultempireComCrawler extends PluginForDecrypt {
    public AdultempireComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.BUBBLE_NOTIFICATION };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "adultempire.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((unlimited/)?\\d+/[a-z0-9\\-]+\\.html|gw/player/[^/]*item_id=\\d+.*)");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_EMBED     = "(?i)https?://[^/]+/gw/player/[^/]*item_id=(\\d+).*";
    private final String PROPERTY_TITLE = "title";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        boolean isPremiumUser = false;
        if (account != null) {
            final AdultempireCom hosterplugin = (AdultempireCom) this.getNewPluginForHostInstance(this.getHost());
            hosterplugin.login(account, false);
            if (AccountType.PREMIUM == account.getType()) {
                isPremiumUser = true;
            }
        }
        String internalIDStr;
        String[][] scenesData = null;
        Browser sceneBR = null;
        if (param.getCryptedUrl().matches(TYPE_EMBED)) {
            /* Internal ID given inside URL. */
            internalIDStr = new Regex(param.getCryptedUrl(), TYPE_EMBED).getMatch(0);
        } else {
            /* Internal ID needs to be parsed via HTML code. */
            sceneBR = br.cloneBrowser();
            sceneBR.getPage(param.getCryptedUrl());
            if (sceneBR.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            internalIDStr = sceneBR.getRegex("item:\\s*'(\\d+)'").getMatch(0);
            if (internalIDStr == null) {
                internalIDStr = sceneBR.getRegex("item:\\s*(\\d+)").getMatch(0);
            }
            scenesData = sceneBR.getRegex("ShowMoreScreens2017\\((\\d+),(\\d+),(\\d+),(\\d+), \\'(/\\d+/[^\\']+)'").getMatches();
        }
        if (internalIDStr == null) {
            /* Assume that content is offline or no trailer is available. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Website will include more parameters but really only "item_id" is required! */
        br.getPage("https://www." + this.getHost() + "/gw/player/?type=trailer&item_id=" + internalIDStr);
        final String thumbnailUrl = PluginJSonUtils.getJson(br, "thumbnailUrl");
        final String httpStreamingURL = PluginJSonUtils.getJson(br, "contentUrl");
        if (!StringUtils.isEmpty(thumbnailUrl)) {
            final DownloadLink thumbnail = this.createDownloadlink(thumbnailUrl);
            thumbnail.setAvailable(true);
            ret.add(thumbnail);
        }
        final ArrayList<DownloadLink> trailerResults = this.crawlVideo(br.cloneBrowser(), internalIDStr, null, "trailer", "trailer");
        ret.addAll(trailerResults);
        final String title = trailerResults.get(0).getStringProperty(PROPERTY_TITLE);
        if (!StringUtils.isEmpty(httpStreamingURL)) {
            final DownloadLink httpStream = this.createDownloadlink(httpStreamingURL);
            httpStream.setFinalFileName(title + "_http.mp4");
            httpStream.setAvailable(true);
            ret.add(httpStream);
        }
        /* Add snapshots + trailer of each scene */
        if (scenesData != null && scenesData.length > 0) {
            int sceneCounter = 1;
            for (final String[] sceneInfo : scenesData) {
                // String sceneTitle = new Regex(scenesHTML, "class=\"modal-title\">([^<]+)</h3>").getMatch(0);
                String sceneTitle = "Scene " + sceneCounter;
                if (sceneTitle != null) {
                    sceneTitle = Encoding.htmlDecode(sceneTitle).trim();
                }
                final String item = sceneInfo[0], start = sceneInfo[1], end = sceneInfo[2], sceneID = sceneInfo[3];
                logger.info("Crawling scene " + sceneCounter + "/" + scenesData.length + " | Title: " + sceneTitle);
                final ArrayList<String> allScreenshotURLs = new ArrayList<String>();
                final String[] snapshotHTMLs = sceneBR.getRegex("scene_id='" + sceneID + "'><img src=\"(.*?)class=\"fancy screen\"").getColumn(0);
                for (final String snapshotHTML : snapshotHTMLs) {
                    final String url = new Regex(snapshotHTML, "href=\"(https://[^\"]+)\"").getMatch(0);
                    if (url != null) {
                        allScreenshotURLs.add(url);
                    } else {
                        logger.warning("Plugin outdated or invalid image source: " + snapshotHTML);
                    }
                }
                if (allScreenshotURLs.size() > 0) {
                    logger.info("Crawling more screenshots");
                    final Browser br3 = br.cloneBrowser();
                    br3.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br3.getPage("/Item/LoadSceneScreenshots?item=" + item + "&start=" + start + "&end=" + end + "&sceneID=" + sceneID + "&_=" + System.currentTimeMillis());
                    final String[] scenesSnapshotURLs2 = br3.getRegex("a rel=\"(?:scenescreenshots|morescreenshots)\"\\s*href=\"(https?://[^\"]+)\"").getColumn(0);
                    logger.info("Found " + scenesSnapshotURLs2.length + " more scene snapshots");
                    allScreenshotURLs.addAll(Arrays.asList(scenesSnapshotURLs2));
                    int snapshotCounter = 1;
                    final int padLength = StringUtils.getPadLength(allScreenshotURLs.size());
                    for (final String scenesSnapshotURL : allScreenshotURLs) {
                        final DownloadLink image = this.createDownloadlink(scenesSnapshotURL);
                        final String ext = Plugin.getFileNameExtensionFromURL(scenesSnapshotURL);
                        if (sceneTitle != null && ext != null) {
                            final String filename = title + " - " + sceneTitle + "_" + StringUtils.formatByPadLength(padLength, snapshotCounter) + ext;
                            image.setFinalFileName(filename);
                            image.setProperty(DirectHTTP.FIXNAME, filename);
                        }
                        image.setAvailable(true);
                        ret.add(image);
                        snapshotCounter++;
                    }
                }
                /* Add scene preview video if available */
                final boolean hasScenePreviewVideo = sceneBR.containsHTML("data-video=\"scenePreview_" + sceneID);
                logger.info("Scene: " + sceneCounter + " |  Images: " + allScreenshotURLs.size() + " | hasScenePreviewVideo: " + hasScenePreviewVideo);
                if (hasScenePreviewVideo) {
                    /* */
                    logger.info("Crawling scene preview");
                    ret.addAll(this.crawlVideo(sceneBR.cloneBrowser(), internalIDStr, sceneID, "preview", sceneTitle));
                } else {
                    logger.warning("Found scene without preview video(?): " + sceneID);
                }
                if (this.isAbort()) {
                    logger.info("Aborted by user");
                    break;
                } else {
                    sceneCounter++;
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        if (isPremiumUser) {
            /* Display information message to user */
            this.displayBubbleNotification("Premium items cannot be crawled", title + "\r\nPremium items cannot be crawled/downloaded because they are DRM protected.");
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlVideo(final Browser br, final String itemID, final String sceneID, final String stream_type, final String subtitle) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("Origin", "https://www." + this.getHost());
        final Map<String, Object> postData = new HashMap<String, Object>();
        postData.put("encrypted_customer_id", null);
        postData.put("forcehd", false);
        postData.put("initiate_tracking", false);
        postData.put("item_id", Integer.parseInt(itemID));
        if (sceneID != null) {
            postData.put("scene_id", Integer.parseInt(sceneID));
        }
        postData.put("signature", null);
        postData.put("stream_type", stream_type);
        postData.put("timestamp", null);
        br.postPageRaw("https://player.digiflix.video/verify", JSonStorage.serializeToJson(postData));
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> item_detail = (Map<String, Object>) entries.get("item_detail");
        final String title = (String) item_detail.get("title");
        if (stream_type.equalsIgnoreCase("trailer")) {
            /* Only add extra images for trailer */
            final String back_cover = (String) item_detail.get("back_cover");
            final String front_cover = (String) item_detail.get("front_cover");
            final String posterURL = (String) item_detail.get("poster");
            if (!StringUtils.isEmpty(posterURL) && !posterURL.contains("/nophoto_")) {
                final DownloadLink poster = this.createDownloadlink(posterURL);
                poster.setProperty(PROPERTY_TITLE, title);
                poster.setAvailable(true);
                ret.add(poster);
            }
            if (!StringUtils.isEmpty(back_cover)) {
                final DownloadLink backcover = this.createDownloadlink(back_cover);
                backcover.setProperty(PROPERTY_TITLE, title);
                backcover.setAvailable(true);
                ret.add(backcover);
            }
            if (!StringUtils.isEmpty(front_cover)) {
                final DownloadLink frontcover = this.createDownloadlink(front_cover);
                frontcover.setProperty(PROPERTY_TITLE, title);
                frontcover.setAvailable(true);
                ret.add(frontcover);
            }
        }
        /* Add main trailer */
        final String hlsMaster = (String) entries.get("playlist_url");
        br.getPage(hlsMaster);
        final List<HlsContainer> containers = HlsContainer.getHlsQualities(br);
        for (final HlsContainer container : containers) {
            final DownloadLink video = this.createDownloadlink(GenericM3u8.createURLForThisPlugin(container.getDownloadurl()));
            if (subtitle != null) {
                video.setFinalFileName(title + " - " + subtitle + "_hls_" + container.getHeight() + "p.mp4");
            } else {
                video.setFinalFileName(title + "_hls_" + container.getHeight() + "p.mp4");
            }
            video.setProperty(PROPERTY_TITLE, title);
            video.setAvailable(true);
            ret.add(video);
        }
        return ret;
    }
}
