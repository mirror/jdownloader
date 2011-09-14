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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wupload.com" }, urls = { "http://(www\\.)?wupload\\..*?/folder/[0-9a-z]+" }, flags = { 0 })
public class WUploadComFolder extends PluginForDecrypt {

    public WUploadComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // It can happen that domain changes (.com->.de)
        br.setFollowRedirects(true);
        br.setCookie("http://www.wupload.com", "lang", "en");
        br.getPage(parameter);
        if (br.containsHTML("(>Error 9001|>The requested folder do not exist or was deleted by the owner|>If you want, you can contact the owner of the referring site to tell him about this mistake|>No links to show<)")) return decryptedLinks;
        if (br.containsHTML("(>Error 9002|>The requested folder is not public|>If you own this folder, make it public by editing the)")) throw new DecrypterException("Folder is not public");
        String[] links = br.getRegex("class=\"passwordIcon\" title=\"\"></span><a href=\"(http://.*?)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(http://(www\\.)?wupload\\..*?/file/\\d+/.*?)\"").getColumn(0);
        String[] folders = br.getRegex("\"(http://(www\\.)?wupload\\..*?/folder/[0-9a-z]+)\"").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
        if (folders != null && folders.length != 0) {
            for (String folderlink : folders)
                decryptedLinks.add(createDownloadlink(folderlink));
        }
        return decryptedLinks;
    }

}
