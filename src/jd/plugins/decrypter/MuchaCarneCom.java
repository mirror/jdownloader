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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "muchacarne.com" }, urls = { "http://(www\\.)?muchacarne\\.com/hosted\\-id\\d+\\-.*?\\.html" }, flags = { 0 })
public class MuchaCarneCom extends PluginForDecrypt {

    public MuchaCarneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        br.getPage(parameter);
        if ("http://www.muchacarne.com/".equals(br.getURL()) || br.containsHTML("http\\-equiv=\"refresh\" content=\"\\d+;url=http://muchacarne\\.xxx/\">")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        String filename = br.getRegex("<title>([^<>\"]*?) at MuchaCarne\\.com</title>").getMatch(0);
        String externID = br.getRedirectLocation();
        if (externID != null) {
            DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\\&id=(\\d+)\\&").getMatch(0);
        if (externID == null) externID = br.getRegex("\\&url=/videos/(\\d+)/").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.isharemybitch.com/videos/" + externID + "/oh-lol" + new Random().nextInt(10000) + ".html");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("xvideos\\.com/(embedframe|embedcode)/(\\d+)").getMatch(1);
        if (externID == null) externID = br.getRegex("id_video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("isharemybitch\\-gallery\\-(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.isharemybitch.com/galleries/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        externID = br.getRegex("(https?://[^/]+dailymotion\\.com/swf/[a-z0-9\\-_]+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?oneclicktube\\.com/flvPlayer\\.swf\\?id=[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://college\\-girls\\.com/\\?ctr=get_embed\\&id=\\d+)\"").getMatch(0);
        if (externID != null) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        externID = br.getRegex("(https?://www\\.keezmovies\\.com/embed_player\\.php\\?v?id=\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        // For all following ids, a filename is needed
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        // Check if it's a picture gallery
        if (br.containsHTML("<div id=\"gallery\">")) {
            final String[] galleryLinks = br.getRegex("class=\"galthumb\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getColumn(0);
            if (galleryLinks == null || galleryLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String galLink : galleryLinks) {
                decryptedLinks.add(createDownloadlink(galLink));
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(filename);
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        }
        externID = br.getRegex("<iframe id=\"preview\" src=\"(http://gallys\\.nastydollars\\.com/[^<>\"]+)").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("<iframe src=\"(http://[^<>\"]+)").getMatch(0);
            if (externID != null) {
                br.getPage(externID);
                externID = br.getRegex("<a href=\"(http://[^<>]+\\.flv)\" id=\"media\"").getMatch(0);
                if (externID != null) {
                    final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                    dl.setFinalFileName(filename + ".flv");
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
            }
        }
        externID = br.getRegex("url: \\'(http://static\\.crakmembers\\.com/[^<>\"]*?)\\'").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("xhamster\\.com/xembed\\.php\\?video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://xhamster.com/movies/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        if (externID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}