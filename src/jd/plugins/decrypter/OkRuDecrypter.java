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
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ok.ru" }, urls = { "https?://(?:[A-Za-z0-9]+\\.)?(?:ok\\.ru|odnoklassniki\\.ru)/(?:video|videoembed|web-api/video/moviePlayer|live)/(\\d+(-\\d+)?)|https?://ok\\.ru/video/c(\\d+)" })
public class OkRuDecrypter extends PluginForDecrypt {
    public OkRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_CHANNEL = "https?://ok\\.ru/video/c(\\d+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches(TYPE_CHANNEL)) {
            /* Crawl channel -> Assume that all videos are selfhosted */
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
                return decryptedLinks;
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
                    dl.setName(videoID + ".mp4");
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    distribute(dl);
                    decryptedLinks.add(dl);
                }
                if (addedItems == 0) {
                    logger.info("Stopping because failed to find any new items on current page");
                    break;
                }
                logger.info("Found " + addedItems + " items on current page");
            } while (!this.isAbort());
        } else {
            /* Crawl single video -> Check for embedded content on external websites */
            final String vid = new Regex(param.toString(), this.getSupportedLinks()).getMatch(0);
            final String parameter = "https://ok.ru/video/" + vid;
            param.setCryptedUrl(parameter);
            jd.plugins.hoster.OkRu.prepBR(this.br);
            br.getPage("https://ok.ru/video/" + vid);
            if (jd.plugins.hoster.OkRu.isOffline(br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String externID = null;
            String provider = null;
            final LinkedHashMap<String, Object> entries = jd.plugins.hoster.OkRu.getFlashVars(br);
            if (entries != null) {
                provider = (String) entries.get("provider");
                externID = (String) JavaScriptEngineFactory.walkJson(entries, "movie/contentId");
            }
            if ("USER_YOUTUBE".equalsIgnoreCase(provider) && !StringUtils.isEmpty(externID)) {
                decryptedLinks.add(createDownloadlink("https://www.youtube.com/watch?v=" + externID));
                return decryptedLinks;
            }
            /* 2019-10-15: TODO: Check if this is still working */
            externID = this.br.getRegex("coubID=([A-Za-z0-9]+)").getMatch(0);
            if (externID == null) {
                externID = this.br.getRegex("coub\\.com%2Fview%2F([A-Za-z0-9]+)").getMatch(0);
            }
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(String.format("https://coub.com/view/%s", externID)));
                return decryptedLinks;
            }
            /* No external hosting provider found --> Content should be hosted by ok.ru --> Pass over to hosterplugin. */
            final DownloadLink main = createDownloadlink(param.toString());
            main.setName(vid);
            if (jd.plugins.hoster.OkRu.isOffline(this.br)) {
                main.setAvailable(false);
            }
            decryptedLinks.add(main);
        }
        return decryptedLinks;
    }
}
