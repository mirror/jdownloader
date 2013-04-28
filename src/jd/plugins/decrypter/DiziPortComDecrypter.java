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
import jd.plugins.PluginForDecrypt;

//Decrypts embedded videos from diziport.com
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "diziport.com" }, urls = { "http://(www\\.)?diziport\\.com/.*?/.*?/(\\d+)?" }, flags = { 0 })
public class DiziPortComDecrypter extends PluginForDecrypt {

    public DiziPortComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">sayfa bulunamadÄ±")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String sid = br.getRegex("\\&sid=(.*?)\"").getMatch(0);
        if (sid != null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://diziport.com/nesne-uye.php?olay=sayac&sid=" + sid);
            String externID = br.getRegex("file:\\'((www\\.)?youtube\\.com/watch\\?v=[^<>\"]*?)\\',").getMatch(0);
            if (externID != null) {
                if (externID.contains("watch?v=http")) {
                    externID = new Regex(externID, "(http.+)").getMatch(0);
                    decryptedLinks.add(createDownloadlink(externID));
                } else {
                    decryptedLinks.add(createDownloadlink("http://" + externID));
                }
                return decryptedLinks;
            }
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("diziport.com/", "diziportdecrypted.com/")));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}