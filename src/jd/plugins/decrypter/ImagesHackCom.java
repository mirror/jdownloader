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
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imageshack.com" }, urls = { "https?://(?:www\\.)?imageshack\\.(?:com|us)/(?:user|a)/[A-Za-z0-9\\-_]+" }) 
public class ImagesHackCom extends PluginForDecrypt {

    public ImagesHackCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_USER                  = "https?://(?:www\\.)?imageshack\\.(?:com|us)/user/[A-Za-z0-9\\-_]+";
    private static final String TYPE_ALBUM                 = "https?://(?:www\\.)?imageshack\\.(?:com|us)/a/[A-Za-z0-9\\-_]+";

    private static final int    api_max_entries_per_offset = 200;

    /** Using API: https://api.imageshack.com/ */
    @SuppressWarnings({ "unused", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString().replace("imageshack.us", "imageshack.com").replace("http://", "https://");
        final String id_main = new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0);
        int offset = 0;
        int page_counter = 1;
        long images_total = 0;
        final boolean useAltHandling = false;
        String password = "";
        String pwcookie = null;

        final String get_URL;
        LinkedHashMap<String, Object> json;
        if (parameter.matches(TYPE_USER)) {
            /*
             * Get user information - count private images as well. TODO: Check if it actually returns IDs of private images - if not, we do
             * not even have to count them!
             *
             *
             * There are API calls to get all albums of a user but then the album objects only always contain 5 images meaning we'd have to
             * decrypt the album URLs and return them back into the decrypter. Instead we'll just decrypt all images, find their
             * corresponding album names (if existant) and set the correct packagenames.
             */
            this.br.getPage("https://api.imageshack.com/v2/user/" + id_main + "/usage?hide_empty=false&show_private=true&show_hidden=false");
            json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            images_total = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(json, "result/images_count"), 0);
            get_URL = "/v2/user/" + id_main + "/images?hide_folder_images=false&hide_empty=false&show_private=true&show_hidden=false&limit=%d&offset=%d&password=%s";
        } else {
            this.br.setAllowedResponseCodes(401);
            /* Get information about our album */
            this.br.getPage("https://api.imageshack.com/v2/albums/" + id_main + "?limit=1&offset=0&password=");
            if (this.br.getHttpConnection().getResponseCode() == 401) {
                /* Password protected album - not documented in API but works fine! */
                boolean failed = true;
                for (int i = 0; i <= 3; i++) {
                    this.br.getPage("https://api.imageshack.com/v2/albums/" + id_main + "?limit=1&offset=0&password=" + Encoding.urlEncode(password));
                    if (this.br.getHttpConnection().getResponseCode() == 401) {
                        password = getUserInput("Password?", param);
                        continue;
                    }
                    failed = false;
                    break;
                }
                if (failed) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                final Cookies add = this.br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    final String key = c.getKey();
                    final String value = c.getValue();
                    pwcookie = key + ":" + value;
                    /*
                     * Break because we only have one cookie for the valid password. Okay this might sound a bit glitchy but usually we'll
                     * nearly never run into any password protected case for this image host.
                     */
                    break;
                }
            }
            json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            images_total = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(json, "result/total"), 0);
            get_URL = "/v2/albums/" + id_main + "?limit=%d&offset=%d&password=%s";
            final String album_owner = (String) JavaScriptEngineFactory.walkJson(json, "result/owner/username");
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

            /* Old: */
            // if (useAltHandling) {
            // br.getPage("https://imageshack.com/rest_api/v2/images?username=" + username + "&limit=" + imagesPerOffset + "&offset=" +
            // offset + "&hide_empty=true&ts=" + System.currentTimeMillis());
            // } else {
            // br.getPage("https://imageshack.com/rest_api/v2/images?username=" + username + "&limit=10000&offset=0&hide_empty=true&ts="
            // + System.currentTimeMillis());
            // }

            this.br.getPage(String.format(get_URL, api_max_entries_per_offset, offset, Encoding.urlEncode(password)));
            json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(json, "result/images");
            for (final Object resource : ressourcelist) {
                json = (LinkedHashMap<String, Object>) resource;
                final String id = jd.plugins.hoster.ImagesHackCom.api_json_get_id(json);
                final String owner = jd.plugins.hoster.ImagesHackCom.api_json_get_username(json);
                final String album = jd.plugins.hoster.ImagesHackCom.api_json_get_album(json);
                final String url_content = "https://imageshack.com/i/" + id;
                final DownloadLink dl = createDownloadlink(url_content);
                final FilePackage fp = FilePackage.getInstance();
                dl.setAvailableStatus(jd.plugins.hoster.ImagesHackCom.apiImageGetAvailablestatus(this, dl, json));
                dl.setContentUrl(url_content);
                if (!inValidate(album) && !inValidate(owner)) {
                    fp.setName(owner + " - " + album);
                } else {
                    /* owner != available for private albums/photos! */
                    fp.setName(id_main);
                }
                if (!inValidate(password)) {
                    dl.setProperty("pass", password);
                    dl.setProperty("pwcookie", pwcookie);
                }
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

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    private boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}