//    By Highfredo
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "antena3.com" }, urls = { "http://(www\\.)?antena3\\.com/(?!programas\\.html)[^<>\"]*?\\.html" }, flags = { 0 })
public class Antena3ComSalon extends PluginForDecrypt {

    public Antena3ComSalon(PluginWrapper wrapper) {
        super(wrapper);
    }

    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink link, ProgressController progress) throws Exception {
        br.getPage(link.toString());
        if (br.containsHTML("<h1>¡Uy\\! No encontramos la página que buscas\\.</h1>") || br.containsHTML(">El contenido al que estás intentando acceder no existe<")) {
            final DownloadLink dl = createDownloadlink(link.toString().replace("antena3.com/", "antena3decrypted.com/"));
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // No player -> Series link
        if (!br.containsHTML("var player_capitulo=") && link.toString().contains("antena3.com/videos/")) {
            final String linkpart = new Regex(link.toString(), "videos/(.*?)\\.html").getMatch(0);
            final String[] videoPages = br.getRegex("<ul class=\"page\\d+\">(.*?)</ul>").getColumn(0);
            for (final String vList : videoPages) {
                final String[] episodeList = new Regex(vList, "alt=\"[^<>\"/]+\"[\t\n\r ]+href=\"(/videos/[^<>\"]*?)\"").getColumn(0);
                final String[] seriesList = new Regex(vList, "<a title=\"[^<>\"/]*?\" href=\"(/videos/" + linkpart + "[^<>\"]*?)\"").getColumn(0);
                if ((episodeList == null || episodeList.length == 0) && (seriesList == null || seriesList.length == 0)) {
                    logger.warning("Decrypter broken for link: " + link.toString());
                    return null;
                }
                if (episodeList != null && episodeList.length != 0) {
                    for (String video : episodeList) {
                        video = "http://www.antena3.com" + video;
                        decryptedLinks.add(createDownloadlink(video));
                    }
                }
                if (seriesList != null && seriesList.length != 0) {
                    for (final String seriesLink : seriesList) {
                        decryptedLinks.add(createDownloadlink("http://www.antena3.com" + seriesLink));
                    }
                }
            }
        } else {
            ArrayList<String> done = new ArrayList<String>();
            String[] allXMLs = br.getRegex("(http://(www\\.)?antena3\\.com/videoxml/\\d+/\\d{4}/\\d{2}/\\d{2}/\\d+\\.xml)").getColumn(0);
            if (allXMLs == null || allXMLs.length == 0) allXMLs = br.getRegex("player_capitulo\\.xml=\'([^\']+)\'").getColumn(0);
            if (allXMLs == null || allXMLs.length == 0) {
                if (br.containsHTML("class=\"publi_horizontal\"")) {
                    logger.info("There is nothing to decrypt: " + link.toString());
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + link.toString());
                return null;
            }
            for (String singleXML : allXMLs) {
                if (!singleXML.startsWith("http://")) singleXML = "http://www.antena3.com" + singleXML;
                if (done.contains(singleXML)) continue;
                br.getPage(singleXML);
                final String finallink = br.getRegex("<urlShared><\\!\\[CDATA\\[(http://[^<>\"]*?\\.html)\\]\\]></urlShared>").getMatch(0);
                if (finallink != null) decryptedLinks.add(createDownloadlink(finallink.replace("antena3.com/", "antena3decrypted.com/")));
                done.add(singleXML);
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}