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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "coub.com" }, urls = { "https?://(?:www\\.)?coub\\.com/(?!view)[^/]+" })
public class CoubComDecrypter extends PluginForDecrypt {
    public CoubComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Using API: http://coub.com/dev/docs */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.br.setLoadLimit(this.br.getLoadLimit() * 4);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String lid = new Regex(parameter, "/([^/]+)$").getMatch(0);
        final short max_entries_per_page = 500;
        short page = 1;
        short pages_total = 0;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(lid);
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break;
            }
            this.br.getPage("http://coub.com/api/v2/timeline/channel/" + lid + "?per_page=" + max_entries_per_page + "&permalink=" + lid + "&order_by=newest&page=" + page);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            if (page == 1) {
                pages_total = (short) JavaScriptEngineFactory.toLong(entries.get("total_pages"), 1);
            }
            final ArrayList<Object> ressources = (ArrayList) entries.get("coubs");
            for (final Object ressource : ressources) {
                entries = (LinkedHashMap<String, Object>) ressource;
                final String coub_type = (String) entries.get("type");
                final String fid = (String) entries.get("permalink");
                if (coub_type == null || fid == null) {
                    return null;
                }
                final String url_content = "https://coub.com/view/" + fid;
                final String filename = jd.plugins.hoster.CoubCom.getFilename(this, entries, fid);
                if (!coub_type.equals("Coub::Simple")) {
                    /* Do not decrypt re-coups (similar to re-tweets) - only decrypt content which the user itself posted! */
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink(url_content);
                dl.setContentUrl(url_content);
                dl.setName(filename);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    break;
                }
            }
            page++;
        } while (page <= pages_total);
        return decryptedLinks;
    }
}
