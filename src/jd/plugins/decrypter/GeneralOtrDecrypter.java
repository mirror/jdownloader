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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tivootix.co.cc", "otr.seite.com" }, urls = { "http://(www\\.)?tivootix\\.co\\.cc/\\?file=[^<>\"\\']+", "http://(www\\.)?otr\\.seite\\.com/get\\.php\\?file=[^<>\"\\']+" }, flags = { 0, 0 })
public class GeneralOtrDecrypter extends PluginForDecrypt {

    public GeneralOtrDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        if (parameter.contains("tivootix.co.cc/")) {
            br.getPage(parameter);
            final String tmplink = br.getRegex("\"(go2\\.php\\?id=\\d+)\"").getMatch(0);
            if (tmplink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage("http://www.tivootix.co.cc/" + tmplink);
            String[] ochLinks = br.getRegex("title=\"Datei von [^<>\"\\']+ herunterladen\" src=\"[^<>\"\\']+\" alt=\"\" /> [^<>\"\\']+:</b></td><td><a href=\"(http://[^<>\"\\']+)\"").getColumn(0);
            if (ochLinks == null || ochLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String ochLink : ochLinks) {
                if (!ochLink.contains("tivootix.co.cc/")) {
                    decryptedLinks.add(createDownloadlink(ochLink));
                }
            }
            FilePackage fp = FilePackage.getInstance();
            fp.setName(new Regex(parameter, "tivootix\\.co\\.cc/\\?file=(.+)").getMatch(0));
            fp.addLinks(decryptedLinks);
        } else if (parameter.contains("otr.seite.com/")) {
            br.setReadTimeout(3 * 60 * 1000);
            br.getPage(parameter);
            final String finallink = br.getRegex("name=\"Downloadpage\" src=\"([^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}