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
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.OkRu;
import jd.plugins.hoster.YoutubeDashV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ok.ru" }, urls = { "https?://(?:[A-Za-z0-9]+\\.)?(?:ok\\.ru|odnoklassniki\\.ru)/(?:video|videoembed|web-api/video/moviePlayer|live)/(\\d+(-\\d+)?)|https?://ok\\.ru/video/c(\\d+)|https://(?:www\\.)?ok\\.ru/profile/\\d+/video/c\\d+" })
public class OkRuDecrypter extends PluginForDecrypt {
    public OkRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_CHANNEL  = "https?://[^/]+/video/c(\\d+)";
    private static final String TYPE_PLAYLIST = "https?://[^/]+/profile/(\\d+)/video/(c\\d+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final PluginForHost plugin = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.OkRu) plugin).login(account, false);
        } else {
            OkRu.prepBR(this.br);
        }
        if (param.getCryptedUrl().matches(TYPE_CHANNEL)) {
            /* Crawl channel -> Assume that all videos are selfhosted */
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final ArrayList<String> dupes = new ArrayList<String>();
            final String channelID = new Regex(param.getCryptedUrl(), TYPE_CHANNEL).getMatch(0);
            String fpName = br.getRegex("class=\"album-info_name textWrap\">([^<>\"]+)").getMatch(0);
            if (StringUtils.isEmpty(fpName)) {
                /* Fallback */
                fpName = channelID;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            final String gwtHash = PluginJSonUtils.getJson(br, "gwtHash");
            int page = 0;
            String lastElementID = br.getRegex("data-last-element=\"(-?\\d+)\"").getMatch(0);
            do {
                page++;
                logger.info("Crawling page: " + page);
                if (page > 1) {
                    if (StringUtils.isEmpty(gwtHash)) {
                        /* This should never happen */
                        logger.info("Stopping because gwtHash is missing");
                        break;
                    } else if (lastElementID == null) {
                        /* E.g. only one page available */
                        logger.info("Stopping because lastElementID is missing");
                        break;
                    }
                    UrlQuery query = new UrlQuery();
                    query.add("fetch", "false");
                    query.add("st.page", Integer.toString(page));
                    query.add("st.lastelem", lastElementID);
                    query.add("gwt.requested", gwtHash);
                    br.postPage("https://ok.ru/video/c" + channelID + "?st.cmd=anonymVideo&st.m=ALBUM&st.aid=c" + channelID + "&st.ft=album&cmd=VideoAlbumBlock", query);
                    lastElementID = br.getRequest().getResponseHeader("lastelem").toString();
                }
                final String[] videoIDs = br.getRegex("/video/(\\d+)").getColumn(0);
                int addedItems = 0;
                for (final String videoID : videoIDs) {
                    if (dupes.contains(videoID)) {
                        continue;
                    }
                    addedItems += 1;
                    dupes.add(videoID);
                    final DownloadLink dl = this.createDownloadlink("https://ok.ru/video/" + videoID);
                    /* Try to find a meaningful title right away */
                    final String title = br.getRegex("/video/" + videoID + "[^\"]+\" title=\"([^<>\"]+)\"").getMatch(0);
                    if (title != null) {
                        dl.setName(title + ".mp4");
                    } else {
                        dl.setName(videoID + ".mp4");
                    }
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    distribute(dl);
                    ret.add(dl);
                }
                if (addedItems == 0) {
                    logger.info("Stopping because failed to find any new items on current page");
                    break;
                }
                logger.info("Found " + addedItems + " items on current page");
            } while (!this.isAbort());
        } else if (param.getCryptedUrl().matches(TYPE_PLAYLIST)) {
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final ArrayList<String> dupes = new ArrayList<String>();
            final String albumID = new Regex(param.getCryptedUrl(), TYPE_PLAYLIST).getMatch(1);
            String fpName = br.getRegex("class=\"channel-panel_n ellip\">([^<>\"]+)<").getMatch(0);
            if (StringUtils.isEmpty(fpName)) {
                /* Fallback */
                fpName = albumID;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            final String[] albumVideoIDs = br.getRegex("/video/(\\d+)").getColumn(0);
            final boolean crawlPlaylist = true;
            if (crawlPlaylist) {
                final String gwtHash = PluginJSonUtils.getJson(br, "gwtHash");
                if (StringUtils.isEmpty(gwtHash) || albumVideoIDs.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("/dk?cmd=PopLayerVideo", "st.vpl.id=" + albumVideoIDs[0] + "&st._aid=FriendVideo_open_album&st.vpl.vs=album&st.vpl.albumId=" + albumID + "&gwt.requested=" + gwtHash);
                /* Remove "Similar videos" section from html code... */
                final String stuffWeDontWant = br.getRegex("id=\"vp_rel_list\"(.+)").getMatch(0);
                if (stuffWeDontWant != null) {
                    br.getRequest().setHtmlCode(br.toString().replace(stuffWeDontWant, ""));
                }
                final String[] playlistVideoIDs = br.getRegex("/video/(\\d+)\" class=\"vp-layer_video").getColumn(0);
                final String[] playlistVideoNames = br.getRegex("href=\"/video/\\d+\".*?class=\"vp-layer_video_n\">([^<>\"]+)</div>").getColumn(0);
                int playlistVideoIndex = 0;
                for (final String videoID : playlistVideoIDs) {
                    playlistVideoIndex += 1;
                    if (dupes.contains(videoID)) {
                        continue;
                    }
                    // addedItems += 1;
                    dupes.add(videoID);
                    final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/video/" + videoID);
                    /* Try to find a meaningful title right away */
                    final String title = playlistVideoNames.length == playlistVideoIDs.length ? playlistVideoNames[playlistVideoIndex] : null;
                    if (title != null) {
                        dl.setName(title + ".mp4");
                    } else {
                        dl.setName(videoID + ".mp4");
                    }
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    distribute(dl);
                    ret.add(dl);
                }
                // lastElementID = br.getRequest().getResponseHeader("lastelem").toString();
            } else {
                /* Simple way: Only add videos of "channel overview" (this may not find all items) */
                // int addedItems = 0;
                for (final String videoID : albumVideoIDs) {
                    if (dupes.contains(videoID)) {
                        continue;
                    }
                    // addedItems += 1;
                    dupes.add(videoID);
                    final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/video/" + videoID);
                    /* Try to find a meaningful title right away */
                    final String title = br.getRegex("openMovie\\([^\"]+" + videoID + "[^\"]+\"[^>]*title=\"([^\"]+)\"").getMatch(0);
                    if (title != null) {
                        dl.setName(title + ".mp4");
                    } else {
                        dl.setName(videoID + ".mp4");
                    }
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    distribute(dl);
                    ret.add(dl);
                }
            }
        } else {
            /* Crawl single video -> Check for embedded content on external websites */
            final String vid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
            final String contenturl = "https://ok.ru/video/" + vid;
            br.getPage(contenturl);
            if (jd.plugins.hoster.OkRu.isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String externID = null;
            String provider = null;
            final Map<String, Object> entries = jd.plugins.hoster.OkRu.getFlashVars(br);
            if (entries != null) {
                provider = (String) entries.get("provider");
                externID = (String) JavaScriptEngineFactory.walkJson(entries, "movie/contentId");
            }
            if ("USER_YOUTUBE".equalsIgnoreCase(provider) && !StringUtils.isEmpty(externID)) {
                ret.add(createDownloadlink(YoutubeDashV2.generateContentURL(externID)));
                return ret;
            }
            /* 2019-10-15: TODO: Check if this is still working */
            externID = this.br.getRegex("coubID=([A-Za-z0-9]+)").getMatch(0);
            if (externID == null) {
                externID = this.br.getRegex("coub\\.com%2Fview%2F([A-Za-z0-9]+)").getMatch(0);
            }
            if (externID != null) {
                ret.add(createDownloadlink(String.format("https://coub.com/view/%s", externID)));
                return ret;
            }
            /* No external hosting provider found --> Content should be hosted by ok.ru --> Pass over to hosterplugin. */
            final DownloadLink main = createDownloadlink(contenturl);
            main.setName(vid);
            if (jd.plugins.hoster.OkRu.isOffline(this.br)) {
                main.setAvailable(false);
            }
            ret.add(main);
        }
        return ret;
    }
}
