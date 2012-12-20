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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "antena3.com" }, urls = { "http://(www\\.)?antena3.com/videos/[-/\\dA-Za-c]+.html" }, flags = { 0 })
public class Antena3ComSalon extends PluginForDecrypt {

    public Antena3ComSalon(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink link, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String html = br.getPage(link.toString());
        if (html.contains("<h1>¡Uy\\! No encontramos la página que buscas\\.</h1>")) return decryptedLinks;
        String xmlURL = "http://www.antena3.com" + new Regex(html, "player_capitulo\\.xml=\\'(.*?)\\';").getMatch(0);
        br.getPage(xmlURL);
        // Offline1
        if (br.containsHTML(">El contenido al que estás intentando acceder ya no está disponible")) {
            logger.info("Link offline: " + link.toString());
            return decryptedLinks;
        }
        // Offline2
        if (br.getURL().equals("http://www.antena3.comnull/")) {
            logger.info("Link offline: " + link.toString());
            return decryptedLinks;
        }
        // Offline3
        if (br.containsHTML(">El contenido al que estás intentando acceder no existe<")) {
            logger.info("Link offline: " + link.toString());
            return decryptedLinks;
        }
        final String[] links = br.getRegex("<archivo>(.*?)</archivo>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + link.toString());
            return null;
        }
        for (String sdl : links) {
            if (sdl.contains(".mp4")) {
                sdl = "http://desprogresiva.antena3.com/" + sdl.replace("<![CDATA[", "").replace("]]>", "");
                final DownloadLink dl = createDownloadlink(sdl);
                dl.setName(br.getRegex("<descripcion>(.*?)</descripcion>").getMatch(0).replace("<![CDATA[", "").replace("]]>", "") + " - " + dl.getName());

                decryptedLinks.add(dl);
            }
        }

        return decryptedLinks;
    }
}
