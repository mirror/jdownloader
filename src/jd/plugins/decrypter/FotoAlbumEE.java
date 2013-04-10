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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fotoalbum.ee" }, urls = { "http://(www\\.)?(pseudaholic\\.|nastazzy\\.)?fotoalbum\\.ee/photos/[^<>\"\\'/]+(/sets|/[0-9]+)?(/[0-9]+)?" }, flags = { 0 })
public class FotoAlbumEE extends PluginForDecrypt {

    public FotoAlbumEE(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> picLinks = new ArrayList<String>();
        br.setFollowRedirects(true);
        final String parameter = param.toString();
        br.getPage(parameter);

        if (br.containsHTML(">Pilti ei leitud v\\&otilde;i on see kustutatud|\"/img/404\\.png\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        String nextPage = null;
        String[] sets = null;
        final String setName = br.getRegex("<a href=\"/photos/[^<>\"/]*?/sets/\\d+\">[^<>\"]*?</a>([^<>\"]*?)</h1>").getMatch(0);
        FilePackage fp = null;
        if (setName != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(setName.trim()));
        }
        if (parameter.matches(".*?fotoalbum\\.ee/photos/[^<>\"\\'/]+(/sets(/)?)?")) {
            sets = br.getRegex("\"(/photos/[^<>\"\\'/]+/sets/\\d+)\"").getColumn(0);
            for (String set : sets) {
                decryptedLinks.add(createDownloadlink("http://fotoalbum.ee" + set));
            }
            return decryptedLinks;
        }
        if (!parameter.contains("/sets/")) {
            picLinks.add(parameter); // add single picture link
        } else {
            // Get all thumbnail-links and change them to direct links->Very
            // effective
            String[] thumbnails = null;
            do {
                thumbnails = br.getRegex("\"(http://static\\d+\\.fotoalbum\\.ee/fotoalbum/\\d+/\\d+/[a-z0-9]+\\.jpg)\"").getColumn(0);
                if (thumbnails == null || thumbnails.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (String thumbnail : thumbnails) {
                    final Regex linkParts = new Regex(thumbnail, "(http://static\\d+\\.fotoalbum\\.ee/fotoalbum/\\d+/\\d+/)(.+)");
                    final DownloadLink dl = createDownloadlink("directhttp://" + linkParts.getMatch(0) + "0" + linkParts.getMatch(1));
                    dl.setAvailable(true);
                    if (fp != null) fp.add(dl);
                    decryptedLinks.add(dl);
                }
                nextPage = br.getRegex("class=\"active\">\\d+</a></li><li><a href=\"(\\?page=\\d+)").getMatch(0);
                if (nextPage == null) break;
                br.getPage(parameter + nextPage);
            } while (true);
            return decryptedLinks;
        }
        Regex linkInfo = null;
        String pictureURL = null;
        String filename = null;
        // String filename = null; //some filenames are not correct in albums
        // TODO: maybe find a workaround later
        DownloadLink dlLink;
        progress.setRange(picLinks.size());
        for (String picLink : picLinks) {
            br.getPage(picLink);
            linkInfo = br.getRegex("<div class=\"photo\\-full\"> [\t\n\r ]+<span>[\t\n\r ]+<img src=\"(http://[^<>\"\\']*?)\" border=\"0\" alt=\"([^<>\"/]*?)\"");
            pictureURL = linkInfo.getMatch(0);
            filename = linkInfo.getMatch(1);
            if (pictureURL == null || filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String ext = pictureURL.substring(pictureURL.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".jpg";
            dlLink = createDownloadlink("directhttp://" + pictureURL);
            dlLink.setFinalFileName(filename + ext);
            dlLink.setAvailable(true);
            if (fp != null) fp.add(dlLink);
            decryptedLinks.add(dlLink);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}