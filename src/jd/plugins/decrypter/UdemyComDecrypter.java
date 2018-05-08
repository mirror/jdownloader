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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "udemy.com" }, urls = { "https?://(?:www\\.)?udemy\\.com/.+" })
public class UdemyComDecrypter extends PluginForDecrypt {
    public UdemyComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     decrypter_domain = "udemydecrypted.com";
    private String                  course_id        = null;
    private ArrayList<DownloadLink> decryptedLinks   = new ArrayList<DownloadLink>();

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.toString();
        if (parameter.matches(jd.plugins.hoster.UdemyCom.TYPE_SINGLE_PREMIUM_WEBSITE)) {
            /* Single links --> Host plugin */
            decryptedLinks.add(this.createDownloadlink(parameter.replace(this.getHost() + "/", decrypter_domain + "/")));
            return decryptedLinks;
        }
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost("udemy.com"));
        if (aa == null) {
            logger.info("Account needed to download urls of this website");
            return decryptedLinks;
        }
        try {
            jd.plugins.hoster.UdemyCom.login(this.br, aa, false);
        } catch (final Throwable e) {
        }
        jd.plugins.hoster.UdemyCom.prepBRAPI(this.br);
        br.getPage(parameter);
        br.followRedirect();
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        course_id = jd.plugins.hoster.UdemyCom.getCourseIDFromHtml(this.br);
        if (course_id == null) {
            logger.info("Could not find any downloadable content");
            return decryptedLinks;
        }
        this.br.getPage("https://www.udemy.com/api-2.0/courses/" + course_id + "/subscriber-curriculum-items?fields%5Basset%5D=@min,title,filename,asset_type,external_url,length&fields%5Bchapter%5D=@min,description,object_index,title,sort_order&fields%5Blecture%5D=@min,object_index,asset,supplementary_assets,sort_order,is_published,is_free&fields%5Bquiz%5D=@min,object_index,title,sort_order,is_published&page_size=9999");
        if (this.br.getHttpConnection().getResponseCode() == 403) {
            logger.info("User tried to download content which he did not pay for --> Impossible");
            return decryptedLinks;
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = new Regex(parameter, "udemy\\.com/([^/]+)").getMatch(0);
        // final String[] links = br.getRegex("\"(/[^/]+/learn/[^<>\"]+/lecture/\\d+)\"").getColumn(0);
        // if (links == null || links.length == 0) {
        // logger.warning("Decrypter broken for link: " + parameter);
        // return null;
        // }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final LinkedHashMap<String, Object> page_info = (LinkedHashMap<String, Object>) entries.get("");
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("results");
        ArrayList<Object> ressourcelist_2 = null;
        for (final Object courseo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) courseo;
            final String lecture_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
            final String _class = (String) entries.get("_class");
            final Object supplementary_assets = entries.get("supplementary_assets");
            if (lecture_id.equals("0") || !_class.equalsIgnoreCase("lecture")) {
                /* Hm maybe some type we don't support (yet). */
                continue;
            }
            entries = (LinkedHashMap<String, Object>) entries.get("asset");
            if (entries == null) {
                continue;
            }
            decryptAsset(entries, lecture_id);
            if (supplementary_assets != null) {
                /* Most likely files ... */
                ressourcelist_2 = (ArrayList<Object>) supplementary_assets;
                for (final Object supplementary_asseto : ressourcelist_2) {
                    entries = (LinkedHashMap<String, Object>) supplementary_asseto;
                    decryptAsset(entries, lecture_id);
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private void decryptAsset(final LinkedHashMap<String, Object> entries, final String lecture_id) {
        String asset_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
        String title = (String) entries.get("title");
        final String filename = (String) entries.get("filename");
        /* E.g. Video, Article, File */
        final String asset_type = (String) entries.get("asset_type");
        if (asset_id.equals("0") || asset_type == null || asset_type.equals("")) {
            return;
        }
        String filename_temp;
        if (filename != null && !filename.equals("")) {
            filename_temp = filename;
            filename_temp = course_id + "_" + lecture_id + "_" + asset_id + "_" + filename_temp;
        } else if (title != null && !title.equals("")) {
            filename_temp = title;
            filename_temp = course_id + "_" + lecture_id + "_" + asset_id + "_" + filename_temp;
        } else {
            filename_temp = course_id + "_" + lecture_id + "_" + asset_id;
            if ("Article".equalsIgnoreCase(asset_type)) {
                filename_temp += ".txt";
            }
        }
        final DownloadLink dl;
        if (asset_type.equalsIgnoreCase("ExternalLink")) {
            /* Add external urls as our plugins might be able to parse some of them. */
            /* TODO: Check if normal (e.g. "Video") assets can also contain an "external_url" object. */
            final String external_url = (String) entries.get("external_url");
            if (external_url == null || external_url.equals("")) {
                return;
            }
            dl = createDownloadlink(external_url);
        } else {
            dl = createDownloadlink("http://" + decrypter_domain + "/lecture_id/" + asset_id);
            dl.setName(filename_temp);
            dl.setContentUrl("https://www." + this.getHost() + "/learn/v4/t/lecture/" + lecture_id);
            dl.setAvailable(true);
            dl.setProperty("asset_type", asset_type);
            dl.setProperty("filename_decrypter", filename_temp);
            dl.setProperty("lecture_id", lecture_id);
            dl.setProperty("course_id", course_id);
            dl.setLinkID(asset_id);
        }
        decryptedLinks.add(dl);
    }
}
