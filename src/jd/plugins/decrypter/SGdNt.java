//    jDownloader - Downloadmanager
//    Copyright (C) 2009 JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sogood.net" }, urls = { "http://[\\w\\.]*?sogood\\.net/.+" }, flags = { 0 })
public class SGdNt extends PluginForDecrypt {

    public SGdNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (br.containsHTML("This URL expired!")) throw new DecrypterException("File expired");
        if (br.containsHTML("This URL is protected!")) {
            boolean okay = true;
            for (int i = 0; i < 4; i++) {

                String pass = getUserInput(JDL.L("plugins.hoster.general.passwordprotectedinput", "The links are protected by a password. Please enter the password:"), param.getDecrypterPassword(), param);
                String postData = "pass=" + Encoding.urlEncode(pass);
                br.postPage(parameter, postData);
                if (br.containsHTML("Wrong password!")) {
                    okay = false;
                } else {
                    okay = true;
                    break;
                }
            }
            if (!okay) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        String declink = br.getRedirectLocation();
        if (declink == null) return null;
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(declink)));

        return decryptedLinks;
    }

}
