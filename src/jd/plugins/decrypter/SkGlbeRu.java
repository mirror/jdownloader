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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "skyglobe.ru" }, urls = { "http://(www\\.)?skyglobe\\.ru/(mp3/album/\\d+|soft/section/[^<>\"/]+/[^<>\"/]+/\\d+|video/movie\\.html\\?rt_movie_id=\\d+|clip/clip/\\d+|gallery/photo/\\d+/\\d+\\.html|games/game/\\d+\\.html|wallpapers/picture/\\d+\\.html)" }, flags = { 0 })
public class SkGlbeRu extends PluginForDecrypt {

    public SkGlbeRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCustomCharset("windows-1251");
        br.setFollowRedirects(true);
        if (parameter.matches("http://(www\\.)?skyglobe\\.ru/mp3/album/\\d+")) {
            br.getPage(parameter);
            String fpName = br.getRegex("<TITLE>Скачать альбом(.*?)бесплатной в формате mp3").getMatch(0);
            String[] linkinformation = br.getRegex("<tr(.*?class=\"right_border\".*?)</tr>").getColumn(0);
            boolean failed = false;
            if (linkinformation == null || linkinformation.length == 0) {
                failed = true;
                linkinformation = br.getRegex("\"(/mp3/track/\\d+)").getColumn(0);
            }
            if (linkinformation.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String data : linkinformation) {
                if (failed) {
                    decryptedLinks.add(createDownloadlink("http://skyglobe.ru" + data));
                } else {
                    String filename = new Regex(data, "href=\"/mp3/track.*?\">(.*?)</a>").getMatch(0);
                    String filesize = new Regex(data, "<td class=\"right_border\">(.*?)</td>").getMatch(0);
                    String dlink = new Regex(data, "\"(/mp3/track/\\d+)").getMatch(0);
                    if (dlink == null) return null;
                    DownloadLink aLink = createDownloadlink("http://skyglobe.ru" + dlink);
                    if (filename != null) aLink.setName(filename.trim() + ".mp3");
                    if (filesize != null) aLink.setDownloadSize(SizeFormatter.getSize(filesize));
                    if (filename != null && filesize != null) aLink.setAvailable(true);
                    decryptedLinks.add(aLink);
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        } else if (parameter.matches("http://(www\\.)?skyglobe\\.ru/soft/section/[^<>\"/]+/[^<>\"/]+/\\d+")) {
            br.getPage("http://skyglobe.ru/soft/download/" + new Regex(parameter, "(\\d+)$").getMatch(0));
            String finallink = br.getRegex("<META http\\-equiv=\"refresh\" content=\"\\d+; url=((http|ftp)[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("If downloading not start after \\d+ seconds, press </FONT><A href=\"((http|ftp)[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                if (br.containsHTML(">If downloading not start after")) {
                    logger.info("Link offline (server error): " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (parameter.matches("http://(www\\.)?skyglobe\\.ru/video/movie\\.html\\?rt_movie_id=\\d+")) {
            decryptedLinks.add(createDownloadlink("http://rutube.ru/tracks/" + new Regex(parameter, "(\\d+)$").getMatch(0) + ".html"));
        } else if (parameter.matches("http://(www\\.)?skyglobe\\.ru/clip/clip/\\d+")) {
            br.getPage(parameter);
            final String filename = br.getRegex("<TITLE>([^<>\"]*?), скачай бесплатно видеоклип на сайте SkyGlobe\\.Ru</TITLE>").getMatch(0);
            final String finallink = br.getRegex("addVariable\\(\"MediaLink\", \"(http://[^<>\"]*?)\"\\)").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            if (filename != null) dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
            decryptedLinks.add(dl);
        } else if (parameter.matches("http://(www\\.)?skyglobe\\.ru/gallery/photo/\\d+/\\d+\\.html")) {
            br.getPage(parameter.replace(".html", "/full_screen.html"));
            final String finallink = br.getRegex("\"(/r/photogallery/big/[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink("directhttp://http://skyglobe.ru" + finallink));
        } else if (parameter.matches("http://(www\\.)?skyglobe\\.ru/games/game/\\d+\\.html")) {
            br.getPage(parameter);
            final String finallink = br.getRegex("var sx = new SWFObject\\(\\'(/r/games_full/[a-z0-9]+\\.swf)\\'").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink("directhttp://http://skyglobe.ru" + finallink));
        } else if (parameter.matches("http://(www\\.)?skyglobe\\.ru/wallpapers/picture/\\d+\\.html")) {
            br.getPage(parameter);
            final String[] allResolutions = br.getRegex("\"(/wallpapers/picture/\\d+/\\d+\\.html)\"").getColumn(0);
            if (allResolutions == null || allResolutions.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String resolution : allResolutions) {
                br.getPage("http://skyglobe.ru" + resolution);
                final String finallink = br.getRegex("\"(http://[a-z0-9]+\\.skyglobe\\.ru/r/walls_full//[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            }
            FilePackage fp = FilePackage.getInstance();
            fp.setName("skyglobe.ru gallery - " + new Regex(parameter, "(\\d+)\\.html").getMatch(0));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}