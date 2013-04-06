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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "antena3.com" }, urls = { "http://(www\\.)?antena3.com/videos/(?!programas\\.html)[\\-/\\w]+\\.html" }, flags = { 0 })
public class Antena3ComSalon extends PluginForDecrypt {

    public Antena3ComSalon(PluginWrapper wrapper) {
        super(wrapper);
    }

    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink link, ProgressController progress) throws Exception {
        br.getPage(link.toString());
        if (br.containsHTML("<h1>¡Uy\\! No encontramos la página que buscas\\.</h1>")) {
            logger.info("Link offline: " + link.toString());
            return decryptedLinks;
        }
        // No player -> Series link
        if (!br.containsHTML("var player_capitulo=")) {
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
                    for (final String video : episodeList) {
                        br.getPage("http://www.antena3.com" + video);
                        try {
                            decryptSingleVideo(link);
                        } catch (final DecrypterException e) {
                            if ("Offline".equals(e.getMessage())) return decryptedLinks;
                            throw e;
                        }
                    }
                }
                if (seriesList != null && seriesList.length != 0) {
                    for (final String seriesLink : seriesList) {
                        decryptedLinks.add(createDownloadlink("http://www.antena3.com" + seriesLink));
                    }
                }
            }
        } else {
            try {
                decryptSingleVideo(link);
            } catch (final DecrypterException e) {
                if ("Offline".equals(e.getMessage())) return decryptedLinks;
                throw e;
            }
        }

        return decryptedLinks;
    }

    private void decryptSingleVideo(final CryptedLink link) throws Exception {
        if (br.containsHTML("<title>Página Entrada Valores Premium</title>")) {
            logger.info("Found an onlypremium link...");
            return;
        }
        String name = br.getRegex("<title>ANTENA 3 TV \\- Vídeos de ([^<>\"]*?)</title>").getMatch(0);
        final String xmlstuff = getXML();
        if (xmlstuff == null || name == null) {
            logger.warning("Decrypter broken for link: " + link.toString());
            throw new DecrypterException("Decrypter broken");
        }
        name = Encoding.htmlDecode(name);

        // Offline1
        if (br.containsHTML(">El contenido al que estás intentando acceder ya no está disponible")) {
            logger.info("Link offline: " + link.toString());
            throw new DecrypterException("Offline");
        }
        // Offline2
        if (br.getURL().equals("http://www.antena3.comnull/")) {
            logger.info("Link offline: " + link.toString());
            throw new DecrypterException("Offline");
        }
        // Offline3
        if (br.containsHTML(">El contenido al que estás intentando acceder no existe<")) {
            logger.info("Link offline: " + link.toString());
            throw new DecrypterException("Offline");
        }
        final String[] links = br.getRegex("<archivo>(.*?)</archivo>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + link.toString());
            throw new DecrypterException("Decrypter broken");
        }
        int counter = 1;
        final DecimalFormat df = new DecimalFormat("00");
        for (String sdl : links) {
            if (sdl.contains(".mp4")) {
                sdl = "http://desprogresiva.antena3.com/" + sdl.replace("<![CDATA[", "").replace("]]>", "");
                final DownloadLink dl = createDownloadlink(sdl);
                dl.setFinalFileName(name + "_" + df.format(counter) + ".mp4");
                decryptedLinks.add(dl);
                counter++;
            }
        }
    }

    private String getXML() throws Exception {
        /** If it fails here, check if we still need those old regexes */
        // String urlxml =
        // br.getRegex("<link rel=\"video_src\" href=\"http://www.antena3.com/static/swf/A3Player.swf\\?xml=(.*?)\"/>").getMatch(0);
        // if (urlxml == null) urlxml =
        // br.getRegex("name=\"flashvars\" value=\"xml=(http://[^<>\"]*?)\"").getMatch(0);
        // if (urlxml == null) urlxml =
        // br.getRegex("player_capitulo\\.xml=\'([^\']+)\'").getMatch(0);
        String urlxml = br.getRegex("player_capitulo\\.xml=\'([^\']+)\'").getMatch(0);
        if (urlxml == null) return null;
        return br.getPage(urlxml);
    }

}