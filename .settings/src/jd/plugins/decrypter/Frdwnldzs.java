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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 16179 $", interfaceVersion = 2, names = { "freedownloadz.us" }, urls = { "http://(www\\.)?(v2\\.)?freedownloadz\\.us/index\\.php\\?run=viewupload\\&groupid=\\d+\\&catid=\\d+\\&uploadid=\\d+" }, flags = { 0 })
public class Frdwnldzs extends PluginForDecrypt {

    public Frdwnldzs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String fpName = br.getRegex("class=\"title\" align=\"right\">([^<>\"]*?)</span>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("class=\"title\">Download</span><br /><span class=\"subtitle\">([^<>\"]*?)</span>").getMatch(0);
        String[] links = br.getRegex("\"(index\\.php\\?run=redirect\\&uploadid=\\d+\\&url=[^<>\"/]*?)\"").getColumn(0);
        if (links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String pass = br.getRegex(">Passwort:</strong></td>[\t\n\r ]+<td style=\"width:80%;\"><font color=\"red\"><input type=\"text\" value=\"([^<>\"/]*?)\"").getMatch(0);
        ArrayList<String> passwords = new ArrayList<String>();
        /* add additional password if found */
        if (pass != null) passwords.add(pass);
        /* default password */
        passwords.add("www.freedownloadz.us");
        passwords.add("freedownloadz.us");

        for (String redirectLink : links) {
            br.getPage("http://freedownloadz.us/" + redirectLink);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink).setSourcePluginPasswordList(passwords));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
