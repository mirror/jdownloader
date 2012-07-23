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
import jd.plugins.PluginForDecrypt;

//Decrypts embedded videos from liveleak.com
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "liveleak.com" }, urls = { "http://(www\\.)?liveleak\\.com/view\\?i=[a-z0-9]+_\\d+" }, flags = { 0 })
public class LiveLeakComDecrypter extends PluginForDecrypt {

    public LiveLeakComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String externID = br.getRegex("\"(http://(www\\.)?prochan\\.com/embed\\?f=[^<>\"/]*?)\"").getMatch(0);
        if (externID != null) {
            br.getPage(Encoding.htmlDecode(externID));
            externID = br.getRegex("\\'config\\': \\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return decryptedLinks;
            }
            br.getPage(Encoding.htmlDecode(externID));
            externID = br.getRegex("<link>(http://[^<>\"]*?)</link>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return decryptedLinks;
            }
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("liveleak.com/", "liveleakdecrypted.com/")));
        return decryptedLinks;
    }

}
