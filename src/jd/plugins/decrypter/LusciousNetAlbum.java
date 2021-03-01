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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "luscious.net" }, urls = { "https?://(?:(?:www|members)\\.)?luscious\\.net/albums/([a-z0-9\\-_]+)_(\\d+)/" })
public class LusciousNetAlbum extends PluginForDecrypt {
    public LusciousNetAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String albumSlug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String albumID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1);
        int page = 1;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(albumSlug);
        int maxItemsPerPage = 50;
        do {
            br.getPage("https://api." + this.getHost()
                    + "/graphql/nobatch/?operationName=AlbumListOwnPictures&query=+query+AlbumListOwnPictures%28%24input%3A+PictureListInput%21%29+%7B+picture+%7B+list%28input%3A+%24input%29+%7B+info+%7B+...FacetCollectionInfo+%7D+items+%7B+...PictureStandardWithoutAlbum+%7D+%7D+%7D+%7D+fragment+FacetCollectionInfo+on+FacetCollectionInfo+%7B+page+has_next_page+has_previous_page+total_items+total_pages+items_per_page+url_complete+%7D+fragment+PictureStandardWithoutAlbum+on+Picture+%7B+__typename+id+title+description+created+like_status+number_of_comments+number_of_favorites+moderation_status+width+height+resolution+aspect_ratio+url_to_original+url_to_video+is_animated+position+tags+%7B+category+text+url+%7D+permissions+url+thumbnails+%7B+width+height+size+url+%7D+%7D+&variables=%7B%22input%22%3A%7B%22filters%22%3A%5B%7B%22name%22%3A%22album_id%22%2C%22value%22%3A%22" + albumID
                    + "%22%7D%5D%2C%22display%22%3A%22position%22%2C%22page%22%3A" + page + "%7D%7D");
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/picture/list");
            final Map<String, Object> listInfo = (Map<String, Object>) entries.get("info");
            final List<Object> ressourcelist = (List<Object>) entries.get("items");
            for (final Object pictureO : ressourcelist) {
                entries = (Map<String, Object>) pictureO;
                final String directurl = (String) entries.get("url_to_original");
                final String url = (String) entries.get("url");
                final long position = ((Number) entries.get("position")).longValue();
                if (StringUtils.isEmpty(directurl) || StringUtils.isEmpty(url)) {
                    /* Skip invalid items */
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink(URLHelper.createURL(URLHelper.parseLocation(new URL("https://" + this.getHost()), url)).toString());
                final String filename = Plugin.getFileNameFromURL(new URL(directurl));
                if (filename != null) {
                    dl.setName(filename);
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                dl.setProperty("directlink", directurl);
                /* Save Packagizer properties */
                dl.setProperty("position", position);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            logger.info("Crawled page " + page + " / " + listInfo.get("total_pages") + " |  + items: " + decryptedLinks.size() + " / " + listInfo.get("total_items"));
            if (ressourcelist.isEmpty() && page == 1) {
                /* No items on first page --> Offline/invalid URL */
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
                return decryptedLinks;
            } else if (!((Boolean) listInfo.get("has_next_page")).booleanValue()) {
                logger.info("Stopping because: reached last page");
                break;
            } else if (ressourcelist.size() < maxItemsPerPage) {
                /* Fail-safe */
                logger.info("Stopping because: Current page contains less items than: " + maxItemsPerPage);
                break;
            } else {
                page += 1;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }
}
