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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "http://[\\w\\.]*?filesmonster\\.com/folders\\.php\\?fid=[0-9a-zA-Z_-]+" }, flags = { 0 })
public class FilesMonsterComFolder extends PluginForDecrypt {

    public FilesMonsterComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.getPage(parameter);
        String fpName = br.getRegex("class=\"xx_big em arial lightblack\">Folder:(.*?)</span>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("class=\"xx_big\">Folder:(.*?)</span>").getMatch(0);
        }
        if (fpName == null) {
            fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        if (fpName != null) fp.setName(fpName);
        String[] links = br.getRegex("green em\" href=\"(.*?)\"").getColumn(0);
        if (links.length == 0) links = br.getRegex("green\" href=\"(.*?)\"").getColumn(0);
        if (links.length == 0) return null;
        for (String dl : links) {
            decryptedLinks.add(createDownloadlink(dl));
        }
        if (fpName != null) {
            fpName = fpName.trim();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
