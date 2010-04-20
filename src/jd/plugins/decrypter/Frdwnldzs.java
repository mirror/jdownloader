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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision", interfaceVersion = 2, names = { "freedownloadz.us" }, urls = { "http://[\\w\\.]*?(v2\\.)?freedownloadz\\.us/download\\.php\\?id=\\d+" }, flags = { 0 })
public class Frdwnldzs extends PluginForDecrypt {

    public Frdwnldzs(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String[] links = br.getRegex("<a href=\"(http://.*?)\" target=\"_blank\" title=\".*?\"><font color=\"#ff9966\">Link \\d+</font></a>").getColumn(0);
        if (links.length == 0) return null;
        String pass = br.getRegex("Passwort:</font>.*?<font color=\"red\">(.*?)</font>").getMatch(0);
        ArrayList<String> passwords = new ArrayList<String>();
        /* default password */
        passwords.add("www.freedownloadz.us");
        /* add additional password */
        if ((pass != null) && !pass.equals(passwords.get(0))) {
            passwords.add(pass);
        }

        for (String dl : links) {
            decryptedLinks.add(createDownloadlink(dl).setSourcePluginPasswordList(passwords));
        }

        return decryptedLinks;
    }

    // @Override

}
