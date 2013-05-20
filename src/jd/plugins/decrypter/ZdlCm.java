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
import jd.gui.UserIO;
import jd.nutils.JDHash;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zoodl.com" }, urls = { "http://[\\w\\.]*?zoodl\\.com/\\d+" }, flags = { 0 })
public class ZdlCm extends PluginForDecrypt {

    public ZdlCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String param = parameter.toString();
        String decLink;

        br.getPage(param);

        if (JDHash.getMD5(br.toString()).equals("0055ef2a0f3890e671813d45084142f8")) {
            // results is zoodl.com, effectively returning back to there own domain
            return decryptedLinks;
        }

        if (br.containsHTML("is password protected</td>")) {
            for (int retry = 1; retry <= 5; retry++) {
                Form form = br.getForm(1);
                String pass = UserIO.getInstance().requestInputDialog("Password");
                form.put("p", pass);
                br.submitForm(form);
                if (!br.containsHTML("Not valid password!")) break;
                logger.warning("Wrong password!");
            }
        }

        decLink = br.getRedirectLocation();
        if (decLink == null) decLink = br.getRegex("<FRAME src=\"(.*?)\">").getMatch(0);
        if (decLink == null) return null;

        decryptedLinks.add(createDownloadlink(decLink));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}