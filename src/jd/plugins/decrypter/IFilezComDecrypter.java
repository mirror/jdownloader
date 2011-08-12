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

//This decrypter is there to seperate folder- and hosterlinks as hosterlinks look the same as folderlinks
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "i-filez.com" }, urls = { "http://(www\\.)?i\\-filez\\.com/downloads/i/\\d+/f/[^\"\\']+" }, flags = { 0 })
public class IFilezComDecrypter extends PluginForDecrypt {

    public IFilezComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String IFILEZDECRYPTED = "i-filezdecrypted.com/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // Set English language
        br.setCookie("http://i-filez.com/", "sdlanguageid", "2");
        br.getPage(parameter);
        String[] links = br.getRegex("onClick=\"window\\.open\\(\\'(http://i\\-filez\\.com/downloads/i/\\d+/f/.*?)\\'\\);").getColumn(0);
        if (links != null && links.length != 0) {
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl.replace("i-filez.com/", IFILEZDECRYPTED)));
        } else {
            if (br.containsHTML(">Description of the downloaded folder")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(parameter.replace("i-filez.com/", IFILEZDECRYPTED)));
        }
        return decryptedLinks;
    }

}
