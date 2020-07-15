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
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "seedr.cc" }, urls = { "https?://(?:www\\.)?seedr\\.cc/files/\\d+" })
public class SeedrCc extends PluginForDecrypt {
    public SeedrCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(plg);
        final String folderid = new Regex(parameter, "(\\d+)$").getMatch(0);
        if (aa == null) {
            logger.info("Account needed to use this crawler");
            return decryptedLinks;
        }
        ((jd.plugins.hoster.SeedrCc) plg).login(this.br, aa, false);
        jd.plugins.hoster.SeedrCc.prepAjaxBr(this.br);
        this.br.postPage("https://www." + this.getHost() + "/content.php?action=list_contents", "content_type=folder&content_id=" + folderid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<Object> ressourcelist_files = (ArrayList<Object>) entries.get("files");
        final ArrayList<Object> ressourcelist_folders = (ArrayList<Object>) entries.get("folders");
        final String full_path = (String) entries.get("fullname");
        if (ressourcelist_files != null) {
            /* Crawl files --> Urls go into host plugin */
            for (final Object itemo : ressourcelist_files) {
                entries = (LinkedHashMap<String, Object>) itemo;
                final String filename = (String) entries.get("name");
                final String folder_file_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("folder_file_id"), 0));
                final String hash = (String) entries.get("hash");
                final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                if (filename == null || folder_file_id == null) {
                    /* This should never happen! */
                    continue;
                }
                final DownloadLink dl = createDownloadlink("http://seedrdecrypted.cc/" + folder_file_id);
                dl.setFinalFileName(filename);
                dl.setDownloadSize(filesize);
                if (hash != null) {
                    dl.setMD5Hash(hash);
                }
                dl.setAvailable(true);
                dl.setContentUrl(parameter);
                if (!StringUtils.isEmpty(full_path)) {
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, full_path);
                }
                decryptedLinks.add(dl);
            }
        }
        if (ressourcelist_folders != null) {
            /* Crawl folders --> These urls go back into decrypter */
            for (final Object itemo : ressourcelist_folders) {
                entries = (LinkedHashMap<String, Object>) itemo;
                final String id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
                // final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                if (id.equals("0")) {
                    /* This should never happen! */
                    continue;
                }
                decryptedLinks.add(createDownloadlink("https://www." + this.getHost() + "/files/" + id));
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Found nothing - probably only empty folder(s)");
        }
        return decryptedLinks;
    }
}
