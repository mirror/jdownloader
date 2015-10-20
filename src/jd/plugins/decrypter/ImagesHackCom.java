//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imageshack.com" }, urls = { "https?://(?:www\\.)?imageshack\\.(?:com|us)/(?:user|a)/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class ImagesHackCom extends PluginForDecrypt {

    public ImagesHackCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // private static final String TYPE_PHOTO = ".*?imageshack\\.(us|com)/photo/.+";
    private static final String TYPE_USER                  = "https?://(?:www\\.)?imageshack\\.(?:com|us)/user/[A-Za-z0-9\\-_]+";
    private static final String TYPE_ALBUM                 = "https?://(?:www\\.)?imageshack\\.(?:com|us)/a/[A-Za-z0-9\\-_]+";

    // private static final String TYPE_ALL =
    // "https?://(www\\.)?(img[0-9]{1,4}\\.imageshack\\.us/(g/|my\\.php\\?image=[a-z0-9]+|i/[a-z0-9]+)\\.[a-zA-Z0-9]{2,4}|imageshack\\.us/photo/[^<>\"\\'/]+/\\d+/[^<>\"\\'/]+|imageshack\\.(com|us)/user/[A-Za-z0-9\\-_]+)";
    private static final int    api_max_entries_per_offset = 200;

    @SuppressWarnings({ "unused", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString().replace("imageshack.us", "imageshack.com").replace("http://", "https://");
        final String id_main = new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        int offset = 0;
        int page_counter = 1;
        long images_total = 0;
        final boolean useAltHandling = false;

        final String get_URL;
        LinkedHashMap<String, Object> json;
        if (parameter.matches(TYPE_USER)) {
            /*
             * Get user information - count private images as well. TODO: Check if it actually returns IDs of private images - if not, we do
             * not even have to count them!
             */
            this.br.getPage("https://api.imageshack.com/v2/user/" + id_main + "/usage?hide_empty=false&show_private=true&show_hidden=false");
            json = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            images_total = DummyScriptEnginePlugin.toLong(DummyScriptEnginePlugin.walkJson(json, "result/images_count"), 0);
            get_URL = "/v2/user/" + id_main + "/images?hide_folder_images=false&hide_empty=false&show_private=true&show_hidden=false&limit=%d&offset=%d";
            fp.setName(id_main);
        } else {
            /* Get information about our album */
            this.br.getPage("https://api.imageshack.com/v2/albums/" + id_main + "?limit=1&offset=0");
            json = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            images_total = DummyScriptEnginePlugin.toLong(DummyScriptEnginePlugin.walkJson(json, "result/total"), 0);
            get_URL = "/v2/albums/" + id_main + "?limit=%d&offset=%d";
            final String album_owner = (String) DummyScriptEnginePlugin.walkJson(json, "result/owner/username");
            fp.setName(album_owner + " - " + id_main);
        }

        if (images_total == 0) {
            /* User has no pictures or album is empty */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        do {
            if (this.isAbort()) {
                logger.info("User aborted decryption");
                return decryptedLinks;
            }
            String jsarray = null;

            /* Old: */
            // if (useAltHandling) {
            // br.getPage("https://imageshack.com/rest_api/v2/images?username=" + username + "&limit=" + imagesPerOffset + "&offset=" +
            // offset + "&hide_empty=true&ts=" + System.currentTimeMillis());
            // } else {
            // br.getPage("https://imageshack.com/rest_api/v2/images?username=" + username + "&limit=10000&offset=0&hide_empty=true&ts="
            // + System.currentTimeMillis());
            // }

            this.br.getPage(String.format(get_URL, api_max_entries_per_offset, offset));
            json = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList) DummyScriptEnginePlugin.walkJson(json, "result/images");
            for (final Object resource : ressourcelist) {
                json = (LinkedHashMap<String, Object>) resource;
                final String id = jd.plugins.hoster.ImagesHackCom.api_json_get_id(json);
                final String url_content = "https://imageshack.com/i/" + id;
                final DownloadLink dl = createDownloadlink(url_content);
                dl.setAvailableStatus(jd.plugins.hoster.ImagesHackCom.apiImageGetAvailablestatus(dl, json));
                dl.setContentUrl(url_content);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                offset++;
            }
            logger.info("Decrypted page " + page_counter);
            logger.info("Found " + decryptedLinks.size() + " of " + images_total + " images");
            if (ressourcelist.size() < api_max_entries_per_offset) {
                /* Fail safe */
                break;
            }
            page_counter++;
        } while (decryptedLinks.size() < images_total);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}