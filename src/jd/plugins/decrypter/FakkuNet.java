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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fakku.net" }, urls = { "https?://(?:www\\.)?fakku\\.net/(?:(?:viewmanga|viewonline)\\.php\\?id=\\d+|[a-z0-9\\-_]+/[a-z0-9\\-_]+/read)" })
public class FakkuNet extends antiDDoSForDecrypt {

    public FakkuNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Forced HTTPS */
        String parameter = param.toString().replace("http://", "https://");
        final String url_filename = new Regex(parameter, "fakku\\.net/manga/([^<>\"]*?)/read").getMatch(0);

        boolean loggedin = false;
        final PluginForHost plugin = JDUtilities.getPluginForHost("fakku.net");
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa != null) {
            try {
                loggedin = ((jd.plugins.hoster.FakkuNet) plugin).login(br, aa, false);
            } catch (final Throwable e) {
                logger.info("Login failed - continuing without login");
            }
        }

        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 410) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("id=\"error\"")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final DecimalFormat df = new DecimalFormat("000");
        int counter = 1;
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        final String json_array = br.getRegex("window\\.params\\.thumbs = (\\[.*?\\]);").getMatch(0);
        String main_part = br.getRegex("('|\")((?:https?:)?//t\\.fakku\\.net/images/[^<>\"]+/images/)\\1").getMatch(1);
        if (json_array == null && main_part == null) {
            if (!loggedin) {
                logger.info("Cannot decrypt this subscription-only gallery without account");
                return decryptedLinks;
            }
            /* Handling for subscription URLs */
            getPage("https://books.fakku.net/manga/" + url_filename + "/read");
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            if (fpName == null) {
                fpName = (String) JavaScriptEngineFactory.walkJson(entries, "content/content_name");
            }
            long content_pages = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "content/content_pages"), 0);
            entries = (LinkedHashMap<String, Object>) entries.get("pages");

            if (fpName == null || content_pages == 0 || entries == null) {
                return null;
            }

            for (counter = 1; counter <= content_pages; counter++) {
                LinkedHashMap<String, Object> entries_temp = (LinkedHashMap<String, Object>) entries.get(Long.toString(counter));
                final String directlink = (String) entries_temp.get("image");
                final DownloadLink dl = createDownloadlink(directlink);
                final String final_filename = fpName + " - " + df.format(counter) + ".jpg";
                dl.setFinalFileName(final_filename);
                dl.setAvailable(true);
                dl.setContentUrl(parameter);
                dl.setProperty("mainlink", "https://www.fakku.net/manga/" + url_filename + "/read");
                dl.setProperty("decrypterfilename", final_filename);
                decryptedLinks.add(dl);
            }
        } else {
            if (fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            main_part = Request.getLocation(main_part, br.getRequest());
            fpName = Encoding.htmlDecode(fpName.trim());
            final String allThumbs[] = PluginJSonUtils.getJsonResultsFromArray(json_array);
            if (allThumbs == null || allThumbs.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String thumb : allThumbs) {
                thumb = PluginJSonUtils.unescape(thumb);
                final String thumb_number = new Regex(thumb, "/thumbs/(\\d+)\\.thumb\\.jpg").getMatch(0);
                final DownloadLink dl = createDownloadlink("directhttp://" + main_part + thumb_number + ".jpg");
                dl.setFinalFileName(fpName + " - " + df.format(counter) + ".jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                counter++;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}