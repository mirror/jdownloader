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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "clips-and-pics.org" }, urls = { "http://(www\\.)?clips\\-and\\-pics\\.org/hosted/media/.*?,\\d+\\.php" }, flags = { 0 })
public class ClipsAndPicsOrgDecrypter extends PluginForDecrypt {

    public ClipsAndPicsOrgDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String[] links = br.getRegex("\"(http://(www\\.)fvfileserver\\.com/cnp/.*?)\"").getColumn(0);
        if (links != null && links.length != 0) {
            String fpName = br.getRegex("<div id=\"mid\">[\t\n\r ]+<div class=\"navi_m_top\">(.*?)</div>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("<meta name=\"description\" content=\"(.*?)\">").getMatch(0);
            for (String dl : links)
                decryptedLinks.add(createDownloadlink("directhttp://" + dl));
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        } else {
            decryptedLinks.add(createDownloadlink(parameter.replace("clips-and-pics.org/", "clips-and-pics.orgdecrypted/")));
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}