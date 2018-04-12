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
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DeluxemusicTv.DeluxemusicTvConfigInterface;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deluxemusic.tv" }, urls = { "https?://(?:www\\.)?deluxemusic\\.tv/.*?\\.html|https?://deluxetv\\-vimp\\.mivitec\\.net/(?!video/|getMedium)[a-z0-9\\-]+(?:/\\d+)?" })
public class DeluxemusicTv extends PluginForDecrypt {
    public DeluxemusicTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    @SuppressWarnings({ "deprecation", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        if (parameter.matches(".+deluxemusic\\.tv/.+")) {
            this.br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String playlist_embed_id = this.br.getRegex("playlist_id=\"(\\d+)\"").getMatch(0);
            if (playlist_embed_id == null) {
                logger.info("Seems like this page does not contain any playlist");
                return decryptedLinks;
            }
            /*
             * 2018-04-11: Old url + same parameters still working:
             * https://deluxetv-vimp.mivitec.net/playlist_embed_3/search_playlist_videos.php ... but we'll use the new one for now.
             */
            /* Important header! */
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            this.br.postPage("https://deluxetv-vimp.mivitec.net/playlist_tag//search_playlist_videos.php", "playlist_id=" + playlist_embed_id);
            LinkedHashMap<String, Object> entries = null;
            final ArrayList<Object> mediaObjects = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            for (final Object mediaObj : mediaObjects) {
                entries = (LinkedHashMap<String, Object>) mediaObj;
                String title = (String) entries.get("title");
                final String description = (String) entries.get("description");
                final String mediakey = (String) entries.get("mediakey");
                if (StringUtils.isEmpty(mediakey)) {
                    /* Skip empty items - this should never happen! */
                    continue;
                }
                final String url = generateVideoURL(title, mediakey);
                final DownloadLink dl = this.createDownloadlink(generateVideoURL(title, mediakey));
                final String filename;
                if (StringUtils.isEmpty(title)) {
                    /* Fallback */
                    filename = jd.plugins.hoster.DeluxemusicTv.nicerDicerFilename(mediakey);
                } else {
                    filename = jd.plugins.hoster.DeluxemusicTv.nicerDicerFilename(title);
                }
                dl.setContentUrl(url);
                dl.setLinkID(mediakey);
                dl.setName(filename);
                dl.setAvailable(true);
                if (description != null && !description.equalsIgnoreCase(title)) {
                    dl.setComment(description);
                }
                decryptedLinks.add(dl);
            }
        } else {
            crawlCategory(parameter);
        }
        return decryptedLinks;
    }

    /**
     * Experimental! <br />
     * E.g. 'https://deluxetv-vimp.mivitec.net/category/dlx-ama/18'
     */
    private void crawlCategory(final String parameter) throws IOException {
        final DeluxemusicTvConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.DeluxemusicTv.DeluxemusicTvConfigInterface.class);
        if (!cfg.isEnableCategoryCrawler()) {
            logger.info("Category crawler disabled --> Doing nothing");
            return;
        }
        final String category_name = new Regex(parameter, "https?://[^/]+/(.+)").getMatch(0);
        final ArrayList<String> dupeList = new ArrayList<String>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(category_name);
        br.getPage(parameter);
        int counter = 0;
        int dupe_counter = 0;
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String ajax_url = this.br.getRegex("(/media/ajax/component/boxList/type/video/[^<>\"\\']*?)/page/").getMatch(0);
        if (ajax_url != null) {
            ajax_url = "https://deluxetv-vimp.mivitec.net" + ajax_url + "/page_only/1/page/";
        } else {
            /* Use static URL */
            ajax_url = "https://deluxetv-vimp.mivitec.net/media/ajax/component/boxList/type/video/filter/all/limit/all/layout/thumb/vars/a%253A33%253A%257Bs%253A3%253A%2522act%2522%253Bs%253A7%253A%2522boxList%2522%253Bs%253A3%253A%2522mod%2522%253Bs%253A5%253A%2522media%2522%253Bs%253A4%253A%2522mode%2522%253Bs%253A3%253A%2522all%2522%253Bs%253A7%253A%2522context%2522%253Bi%253A0%253Bs%253A10%253A%2522context_id%2522%253BN%253Bs%253A11%253A%2522show_filter%2522%253Bb%253A1%253Bs%253A6%253A%2522filter%2522%253Bs%253A3%253A%2522all%2522%253Bs%253A10%253A%2522show_limit%2522%253Bb%253A0%253Bs%253A5%253A%2522limit%2522%253Bs%253A3%253A%2522all%2522%253Bs%253A11%253A%2522show_layout%2522%253Bb%253A1%253Bs%253A6%253A%2522layout%2522%253Bs%253A5%253A%2522thumb%2522%253Bs%253A11%253A%2522show_search%2522%253Bb%253A0%253Bs%253A6%253A%2522search%2522%253Bs%253A0%253A%2522%2522%253Bs%253A10%253A%2522show_pager%2522%253Bb%253A1%253Bs%253A10%253A%2522pager_mode%2522%253Bs%253A7%253A%2522loading%2522%253Bs%253A9%253A%2522page_name%2522%253Bs%253A4%253A%2522page%2522%253Bs%253A9%253A%2522page_only%2522%253Bs%253A1%253A%25221%2522%253Bs%253A9%253A%2522show_more%2522%253Bb%253A0%253Bs%253A9%253A%2522more_link%2522%253Bs%253A10%253A%2522media%252Flist%2522%253Bs%253A11%253A%2522show_upload%2522%253Bb%253A0%253Bs%253A11%253A%2522show_header%2522%253Bb%253A1%253Bs%253A8%253A%2522per_page%2522%253Ba%253A3%253A%257Bs%253A8%253A%2522thumbBig%2522%253Bi%253A12%253Bs%253A5%253A%2522thumb%2522%253Bi%253A24%253Bs%253A4%253A%2522list%2522%253Bi%253A8%253B%257Ds%253A14%253A%2522show_empty_box%2522%253Bb%253A1%253Bs%253A9%253A%2522save_page%2522%253Bb%253A1%253Bs%253A9%253A%2522thumbsize%2522%253Bs%253A7%253A%2522160x120%2522%253Bs%253A2%253A%2522id%2522%253Bs%253A16%253A%2522videos-media-box%2522%253Bs%253A7%253A%2522caption%2522%253Bs%253A11%253A%2522Alle%2BMedien%2522%253Bs%253A4%253A%2522user%2522%253BN%253Bs%253A4%253A%2522text%2522%253BN%253Bs%253A13%253A%2522captionParams%2522%253Ba%253A0%253A%257B%257Ds%253A4%253A%2522page%2522%253Bs%253A1%253A%25222%2522%253Bs%253A9%253A%2522component%2522%253Bs%253A7%253A%2522boxList%2522%253Bs%253A4%253A%2522type%2522%253Bs%253A5%253A%2522video%2522%253B%257D/page_only/1/page/";
        }
        do {
            logger.info("Decrypting page: " + counter);
            if (this.isAbort()) {
                return;
            }
            if (counter > 0) {
                br.getPage(ajax_url + counter);
            }
            final String[] articles = this.br.getRegex("<article>(.*?)</article>").getColumn(0);
            if (articles == null || articles.length == 0) {
                logger.info("Possibly reached the end");
                break;
            }
            for (final String article : articles) {
                String url = new Regex(article, "\"(/video/[^/]+/[a-f0-9]{32})\"").getMatch(0);
                final String title = new Regex(article, "h3 title=\"(.*?)\">").getMatch(0);
                final String mediakey = url != null ? new Regex(url, "([a-f0-9]{32})$").getMatch(0) : null;
                if (url == null) {
                    continue;
                }
                if (dupeList.contains(mediakey)) {
                    logger.info("Found dupe");
                    dupe_counter++;
                    continue;
                }
                url = "https://deluxetv-vimp.mivitec.net" + url;
                final DownloadLink dl = this.createDownloadlink(generateVideoURL(title, mediakey));
                final String filename;
                if (StringUtils.isEmpty(title)) {
                    /* Last chance fallback */
                    filename = jd.plugins.hoster.DeluxemusicTv.nicerDicerFilename(mediakey);
                } else {
                    filename = jd.plugins.hoster.DeluxemusicTv.nicerDicerFilename(title);
                }
                dl.setContentUrl(url);
                dl._setFilePackage(fp);
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                dupeList.add(mediakey);
            }
            counter++;
        } while (dupe_counter <= 49);
    }

    private String generateVideoURL(String url_title, final String mediakey) {
        if (!StringUtils.isEmpty(url_title)) {
            url_title = Encoding.urlEncode(url_title);
        } else {
            url_title = "discodeluxe_set";
        }
        return String.format("https://deluxetv-vimp.mivitec.net/video/%s/%s", url_title, mediakey);
    }
}
