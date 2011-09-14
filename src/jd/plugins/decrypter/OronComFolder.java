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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "oron.com" }, urls = { "http://(www\\.)?oron\\.com/folder/[a-z0-9]+" }, flags = { 0 })
public class OronComFolder extends PluginForDecrypt {

    public OronComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookie("http://oron.com", "lang", "english");
        br.getPage(parameter);
        if (br.containsHTML(">No such folder exist<")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        String[] links = br.getRegex("<td style=\"padding-left: 5px\"><a href=\"(http://.*?)\"").getColumn(0);
        String[] folders = br.getRegex("\"(http://oron\\.com/folder/[a-z0-9]+)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(http://oron\\.com/[a-z0-9]{12})\"").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) return null;
        if (links != null && links.length != 0) {
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl));
        }
        if (folders != null && folders.length != 0) {
            String id = new Regex(parameter, "oron\\.com/folder/([a-z0-9]+)").getMatch(0);
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
