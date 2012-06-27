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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileswap.com" }, urls = { "https?://(www\\.)?fileswap\\.com/folder/[a-zA-Z0-9]+/" }, flags = { 0 })
public class FileSwapComFolder extends PluginForDecrypt {

    public FileSwapComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookie("http://fileswap.com", "language", "english");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("<b>The folder you requested has not been found or may no longer be available</b>|Sorry\\, the page you requested could no longer be found\\.")) return decryptedLinks;
        String fpName = br.getRegex("<span class=\"textheader\">[\r\n\t]+Shared Folder &#187; (.*?)[\r\n\t]+</span>").getMatch(0);
        String[] links = br.getRegex("<a class=\"item\\_text\\_color\" href=\"(https?://www\\.fileswap\\.com/dl/[a-zA-Z0-9]+/.*?)\"").getColumn(0);
        String[] folders = br.getRegex("<a class=\"item\\_text\\_color\" href=\"(https?://www\\.fileswap\\.com/folder/[a-zA-Z0-9]+/)").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(https?://www.fileswap.com/dl/[a-zA-Z0-9]+/.*?)\"").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl));
        }
        if (folders != null && folders.length != 0) {
            String id = new Regex(parameter, "fileswap\\.com/folder/([a-zA-Z0-9]+)/").getMatch(0);
            for (String aFolder : folders)
                if (!aFolder.contains(id)) decryptedLinks.add(createDownloadlink(aFolder));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
