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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhost.com" }, urls = { "http://(www\\.)?pornhost\\.com/([0-9]+|embed/\\d+)" }, flags = { 0 })
public class PrnHstComFldr extends PluginForDecrypt {

    public PrnHstComFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("gallery not found") || br.containsHTML("You will be redirected to")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("(moviecontainer|flashmovie|play this movie|createPlayer|>The movie needs to be converted first)") || br.getURL().contains(".com/embed/")) {
            String finallink = br.getURL();
            decryptedLinks.add(createDownloadlink(finallink.replace("pornhost.com/", "pornhostdecrypted.com/")));
        } else {
            String[] links = br.getRegex("class=\"thumb\">.*?<img src=.*?.*?<a href=\"(.*?)\">").getColumn(0);
            if (links.length == 0) links = br.getRegex("\"(http://(www\\.)?pornhost\\.com/[0-9]+/[0-9]+\\.html)\"").getColumn(0);
            if (links.length == 0) return null;
            String fpName = br.getRegex("<title>pornhost\\.com - free file hosting with a twist - gallery(.*?)</title>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("id=\"url\" value=\"http://(www\\.)?pornhost\\.com/(.*?)/\"").getMatch(1);

            for (String dl : links) {
                decryptedLinks.add(createDownloadlink(dl.replace("pornhost.com/", "pornhostdecrypted.com/")));
            }
            // If the plugin knows the name/number of the gallery we can
            // add all pics to one package...looks nicer and makes it easier
            // for the user
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName("Gallery " + fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}