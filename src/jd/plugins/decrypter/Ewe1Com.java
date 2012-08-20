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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ewe1.com" }, urls = { "http://(www\\.)?ewe1\\.com/\\d+" }, flags = { 0 })
public class Ewe1Com extends PluginForDecrypt {

    public Ewe1Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("you followed a wrong link")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final Form decryptform = br.getForm(0);
        if (decryptform == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.submitForm(decryptform);
        final String dl = br.getRegex("onclick=\"popUp\\('(.*?)'\\)").getMatch(0);
        if (dl == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(dl));
        return decryptedLinks;
    }

}
