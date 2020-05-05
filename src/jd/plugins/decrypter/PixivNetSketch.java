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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
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
import jd.plugins.hoster.PixivNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sketch.pixiv.net" }, urls = { "https?://sketch\\.pixiv\\.net/(@[^/]+)" })
public class PixivNetSketch extends PluginForDecrypt {
    public PixivNetSketch(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // final String parameter = param.toString();
        PixivNet.prepBR(br);
        br.setFollowRedirects(true);
        return crawlSketch(param);
    }

    private ArrayList<DownloadLink> crawlSketch(final CryptedLink param) throws IOException {
        logger.info("Crawling sketches");
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> dupes = new ArrayList<String>();
        final String username = new Regex(param.toString(), this.getSupportedLinks()).getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        br.getHeaders().put("x-requested-with", "https://sketch.pixiv.net/" + username);
        br.getHeaders().put("accept", "application/vnd.sketch-v1+json");
        /* 2020-05-05: Website uses 10, we use 20. */
        final int max_items_per_request = 20;
        String next = "https://sketch.pixiv.net/api/walls/" + username + "/posts/public.json?count=" + max_items_per_request;
        int page = 0;
        do {
            page++;
            logger.info("Crawling page: " + page);
            logger.info("Crawling url: " + next);
            br.getPage(next);
            if (br.getHttpConnection().getResponseCode() == 404) {
                /*
                 * 2020-05-05: E.g.
                 * {"data":{},"errors":[{"message":"user: \"@offlineUserTestInvalidUser\" is not found","code":null}],"rand":"CENSORED"}
                 */
                if (decryptedLinks.size() == 0) {
                    decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
                }
                return decryptedLinks;
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            next = (String) JavaScriptEngineFactory.walkJson(entries, "_links/next/href");
            final ArrayList<Object> imgObjects = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "data/items");
            for (final Object imgO : imgObjects) {
                entries = (Map<String, Object>) imgO;
                final long sketch_id = JavaScriptEngineFactory.toLong(entries.get("id"), 0);
                final String created_at = (String) entries.get("created_at");
                final String date_formatted = !StringUtils.isEmpty(created_at) ? new Regex(created_at, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0) : null;
                if (sketch_id == 0 || StringUtils.isEmpty(date_formatted)) {
                    /* Skip invalid items */
                    continue;
                }
                // Map<String, Object> userInfo = (Map<String, Object>) entries.get("user");
                final ArrayList<Object> imgObjectsInner = (ArrayList<Object>) entries.get("media");
                for (final Object imgObjectInner : imgObjectsInner) {
                    Map<String, Object> imgInfoInner = (Map<String, Object>) imgObjectInner;
                    final long img_id = JavaScriptEngineFactory.toLong(imgInfoInner.get("id"), 0);
                    final String type = (String) imgInfoInner.get("type");
                    final String url_original = (String) JavaScriptEngineFactory.walkJson(imgInfoInner, "photo/original/url");
                    if (img_id == 0 || StringUtils.isEmpty(type) || StringUtils.isEmpty(url_original)) {
                        /* Skip invalid items */
                        continue;
                    }
                    /* Fail-safe */
                    if (dupes.contains(img_id + "")) {
                        logger.info("Stopping because: Found dupe");
                        return decryptedLinks;
                    }
                    dupes.add(img_id + "");
                    String ext = Plugin.getFileNameExtensionFromURL(url_original);
                    if (ext == null) {
                        ext = ".png";
                    }
                    final String filename = date_formatted + "_" + username + "_" + sketch_id + "_" + img_id + ext;
                    final DownloadLink dl = this.createDownloadlink("directhttp://" + url_original);
                    dl.setFinalFileName(filename);
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    distribute(dl);
                    decryptedLinks.add(dl);
                }
            }
        } while (!this.isAbort() && next != null);
        return decryptedLinks;
    }
}
