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
import java.util.HashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "degoo.com" }, urls = { "https?://(?:cloud|app)\\.degoo\\.com/share/([A-Za-z0-9]+)" })
public class DegooCom extends PluginForDecrypt {
    public DegooCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.setAllowedResponseCodes(new int[] { 400 });
        br.postPageRaw("https://rest-api.degoo.com/shared", String.format("{\"HashValue\":\"%s\",\"Limit\":100,\"FileID\":null,\"JWT\":null}", folderID));
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("Items");
        for (final Object ressourceO : ressourcelist) {
            entries = (HashMap<String, Object>) ressourceO;
            final String filename = (String) entries.get("Name");
            final int filesize = ((Integer) entries.get("Size")).intValue();
            final int categoryID = ((Integer) entries.get("Category")).intValue();
            final String id = Long.toString(((Long) entries.get("ID")).longValue());
            if (StringUtils.isEmpty(filename) || id.equals("0")) {
                /* Skip invalid items */
                continue;
            }
            if (categoryID != 0) {
                /* 2020-07-21: (Nested) Subfolders are not (yet) supported */
                continue;
            }
            final String directurl = (String) entries.get("URL");
            final String contentURL = "https://app.degoo.com/share/" + folderID + "?ID=" + id;
            final DownloadLink dl = this.createDownloadlink(contentURL);
            dl.setContentUrl(contentURL);
            dl.setAvailable(true);
            dl.setFinalFileName(filename);
            if (!StringUtils.isEmpty(directurl)) {
                dl.setProperty("directurl", directurl);
            }
            if (filesize > 0) {
                dl.setDownloadSize(filesize);
            }
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
