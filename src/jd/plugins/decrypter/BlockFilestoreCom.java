//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blockfilestore.com" }, urls = { "https?://(?:www\\.)?blockfilestore\\.com/folder/([a-f0-9\\-]+)" })
public class BlockFilestoreCom extends PluginForDecrypt {
    public BlockFilestoreCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString();
        final String uid = new Regex(parameter, getSupportedLinks()).getMatch(0);
        br.getPage("https://www." + this.getHost() + "/api/folder/get-public-folder?folderId=" + uid + "&originalFolderId=" + uid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object resultO = root.get("result");
        if (resultO == Boolean.FALSE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> result = (Map<String, Object>) resultO;
        final Map<String, Object> detail = (Map<String, Object>) result.get("detail");
        final List<Map<String, Object>> resources = (List<Map<String, Object>>) result.get("items");
        if (resources.isEmpty()) {
            logger.info("Folder is empty: " + param.getCryptedUrl());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String currentFolderName = detail.get("name").toString();
        String path = this.getAdoptedCloudFolderStructure();
        if (path == null) {
            path = currentFolderName;
        } else {
            path += "/" + currentFolderName;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(path);
        for (final Map<String, Object> resource : resources) {
            if ((Boolean) resource.get("isFolder") == Boolean.TRUE) {
                final DownloadLink dl = this.createDownloadlink("https://www." + this.getHost() + "/folder/" + resource.get("id"));
                dl.setRelativeDownloadFolderPath(path);
                decryptedLinks.add(dl);
            } else {
                final DownloadLink dl = this.createDownloadlink("directhttp://" + resource.get("url"));
                dl.setFinalFileName(resource.get("name").toString());
                dl.setVerifiedFileSize(((Number) resource.get("size")).longValue());
                dl.setAvailable(true);
                dl.setRelativeDownloadFolderPath(path);
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}