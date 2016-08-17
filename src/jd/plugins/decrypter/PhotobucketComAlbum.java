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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "photobucket.com" }, urls = { "https?://(?:www\\.)?s\\d+\\.photobucket\\.com/user/[^/]+/library.+" }) 
public class PhotobucketComAlbum extends PluginForDecrypt {

    public PhotobucketComAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        LinkedHashMap<String, Object> json;
        final String parameter = param.toString();
        br = new Browser();
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.97");
        br.getHeaders().put("Accept-Language", "en-AU,en;q=0.8");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String token = this.br.getRegex("name=\"token\" id=\"token\" value=\"([^<>\"]*?)\"").getMatch(0);

        final FilePackage fp = FilePackage.getInstance();

        long image_count_total = 0;
        int page = 1;

        final String albumdeds = br.getRegex("collectionData:\\s*(\\{.*?\\}),\\s*collectionId:").getMatch(0);
        if (albumdeds == null) {
            return null;
        }
        json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(albumdeds);
        if (image_count_total == 0) {
            image_count_total = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(json, "items/total"), 0);
        }
        /* Don't try more than 24 per page - it won't work - state 2015-10-28 */
        // final long max_entries_per_page = 24;
        final long max_entries_per_page = JavaScriptEngineFactory.toLong(json.get("pageSize"), 24);

        final String fpName = (String) json.get("albumName");

        if (fpName != null) {
            fp.setName(Encoding.htmlDecode(fpName.trim()));
        }
        final String currentAlbumPath = (String) json.get("currentAlbumPath");
        final String libraryUrl = br.getRegex("'libraryUrl'\\s*,\\s*'(https?://s\\d+\\.photobucket\\.com)/user/").getMatch(0);
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            if (!decryptedLinks.isEmpty()) {
                // only required over the first page!
                if (token == null || currentAlbumPath == null) {
                    return null;
                }
                final Browser br = this.br.cloneBrowser();
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("application/json", "text/javascript, */*; q=0.01");
                final String url = libraryUrl + "/component/Common-PageCollection-Album-AlbumPageCollection?filters[album]=" + currentAlbumPath + "&filters[album_content]=2&sort=3&limit=" + max_entries_per_page + "&page=" + page + "&linkerMode=&json=1&hash=" + token + "&_=" + System.currentTimeMillis();
                br.getPage(url);
                json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            }
            final ArrayList<Object> ressourcelist = (ArrayList) (decryptedLinks.isEmpty() ? JavaScriptEngineFactory.walkJson(json, "items/objects") : JavaScriptEngineFactory.walkJson(json, "body/objects"));
            for (final Object pico : ressourcelist) {
                json = (LinkedHashMap<String, Object>) pico;
                final String fname = (String) json.get("name");
                final String dlink = (String) json.get("linkUrl");
                final String userid = Long.toString(JavaScriptEngineFactory.toLong(json.get("userId"), -1));
                if (fname == null || dlink == null || userid.equals("-1")) {
                    return null;
                }
                final DownloadLink dl = createDownloadlink(dlink);
                dl.setContentUrl(dlink);
                dl.setName(fname);
                dl.setAvailable(true);
                fp.add(dl);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            if (ressourcelist.size() < max_entries_per_page) {
                /* Fail safe */
                break;
            }
            page++;
        } while (decryptedLinks.size() < image_count_total);

        return decryptedLinks;
    }
}
