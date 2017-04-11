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

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wiadomosci.pless.pl" }, urls = { "http://(www\\.)?wiadomosci\\.pless\\.pl/galeria/\\d+/\\d+" }) 
public class WiadomosciPlessPl extends PluginForDecrypt {

    public WiadomosciPlessPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // creates HTTP GET to listening URLs
        br.getPage(parameter + "/?p=lista");
        // look for a name for the package
        final String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        // now look for all the images we want..
        final String[] links = br.getRegex("data-url=\"(https?://gal\\.pless\\.pl/ib/[a-f0-9]{32}/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        // this does task for each link found
        for (final String singleLink : links) {
            // create downloadlink for each image
            final DownloadLink dl = createDownloadlink("directhttp://" + singleLink);
            // set fast linkcheck otherwise it will take for ever!
            dl.setAvailable(true);
            // add downloadlink to array which returns
            decryptedLinks.add(dl);
        }
        // set file package name, this places all links into one package in JDownloader.
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}