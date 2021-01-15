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
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.PixeldrainCom;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pixeldrain.com" }, urls = { "https?://(?:www\\.)?pixeldrain\\.com/l/([A-Za-z0-9]+)" })
public class PixeldrainComFolder extends PluginForDecrypt {
    public PixeldrainComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        PixeldrainCom.prepBR(this.br);
        br.getPage(PixeldrainCom.API_BASE + "/list/" + folderID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* 2020-10-01: E.g. {"success":false,"value":"not_found","message":"The entity you requested could not be found"} */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final ArrayList<Object> files = (ArrayList<Object>) entries.get("files");
        for (final Object fileO : files) {
            entries = (Map<String, Object>) fileO;
            final String fileID = (String) entries.get("id");
            if (StringUtils.isEmpty(fileID)) {
                /* Skip invalid items */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/u/" + fileID);
            PixeldrainCom.setDownloadLinkInfo(this, dl, entries);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
