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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "atv.at" }, urls = { "http://(www\\.)?atv\\.at/contentset/\\d+([a-z0-9\\-]+/\\d+)?" }, flags = { 0 })
public class AtvAt extends PluginForDecrypt {

    public AtvAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Important note: Via browser the videos are streamed via RTMP (maybe even in one part) but with this method we get HTTP links which is
     * fine.
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">404 Nicht gefunden<")) {
            logger.info("Link offline (404 error): " + parameter);
            return decryptedLinks;
        }
        if (!br.containsHTML("<div id=\"player_area\">")) {
            logger.info("There is no downloadable content: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Dieses Video wird derzeit encodiert und ist in Kürze verfügbar")) {
            logger.info("Video temporarily unavailable: " + parameter);
            return decryptedLinks;
        }
        final String activeClipID = br.getRegex("active_clip_id%22%3A(\\d+)%2C").getMatch(0);
        if (activeClipID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage("http://atv.at/getclip/" + activeClipID);
        if (br.containsHTML("\"duration\":0")) {
            logger.info("Link offline/no downlodable content found: " + parameter);
            return decryptedLinks;
        }
        if (br.toString().trim().equals("false")) {
            logger.info("This video is not available in your country: " + parameter);
            return decryptedLinks;
        }
        String name = br.getRegex("\"title\":\"([^<>\"]*?)\"").getMatch(0);
        String allLinks = br.getRegex("video_urls\":\\[(\".*?\")\\],").getMatch(0);
        final String videoProgressiveUrls = br.getRegex("video_progressive_urls\":\\[(\".*?\")\\],").getMatch(0);
        if (allLinks != null && allLinks.contains("playlist.m3u8")) allLinks = videoProgressiveUrls;
        if (name == null || allLinks == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        name = Encoding.htmlDecode(name.trim());
        name = decodeUnicode(name);
        final String episodeNr = br.getRegex("folge\\-(\\d+)").getMatch(0);
        final String[] links = new Regex(allLinks, "\"(http[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final DecimalFormat df = new DecimalFormat("000");
        final DecimalFormat episodeFormat = new DecimalFormat("00");
        int counter = 1;
        for (String singleLink : links) {
            final DownloadLink dl = createDownloadlink("directhttp://" + singleLink.replace("\\", ""));
            if (episodeNr != null) {
                dl.setFinalFileName(name + "_E" + episodeFormat.format(Integer.parseInt(episodeNr)) + "_" + df.format(counter) + ".mp4");
            } else {
                dl.setFinalFileName(name + "_part_" + df.format(counter) + ".mp4");
            }
            decryptedLinks.add(dl);
            counter++;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}