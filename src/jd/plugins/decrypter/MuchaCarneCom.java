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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "muchacarne.xxx" }, urls = { "http://(www\\.)?muchacarne\\.(com|xxx)/hosted(\\-id\\d+\\-.*?\\.html|/media/[a-z0-9\\-]+,\\d+\\.php)" }) 
public class MuchaCarneCom extends PornEmbedParser {

    public MuchaCarneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String externID = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString().replace("muchacarne.com/", "muchacarne.xxx/");
        br.getPage(parameter);
        if ("http://www.muchacarne.com/".equals(br.getURL()) || br.containsHTML("http\\-equiv=\"refresh\" content=\"\\d+;url=http://muchacarne\\.xxx/\">")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("<div id=\"playercontent\" style=\"z\\-index:1;\" align=\"center\">[\t\n\r ]+<center>[\t\n\r ]+</center>")) {
            logger.info("Link offline (empty): " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        String filename = br.getRegex("<title>([^<>\"]*?)\\- MuchaCarne\\.xxx</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
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
        if (br.containsHTML("share\\-image\\.com/gallery/")) {
            final String[] galleryLinks = br.getRegex("\"(http://www\\.share\\-image\\.com/pictures/thumb/[^<>\"]*?)\"").getColumn(0);
            if (galleryLinks == null || galleryLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String galLink : galleryLinks) {
                decryptedLinks.add(createDownloadlink(galLink.replace("www.share-image.com/", "pictures.share-image.com/").replace("/thumb/", "/big/")));
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
        externID = br.getRegex("\\'(http://promo\\.isharemycash\\.com/embeddedflash2?\\.php\\?[^<>\"]*?)\\'").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("\\'file\\': \\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (externID == null) {
                logger.info("Link probably offline: " + parameter);
                return decryptedLinks;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("latinteencash\\.com/flash_video\\.swf\" width=\"\\d+\" height=\"\\d+\" flashvars=\"file=(http[^<>\"]*?\\.flv)\\&").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("(http://(www\\.)?scafy\\.com/flv_player/data/playerConfigEmbed/\\d+\\.xml)").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("path=\"(http://(www\\.)?scafy\\.com/vidfiles/[^<>\"]*?\\.flv)\"").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        }
        if (br.containsHTML("megaporn\\.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("\\'http://promo\\.isharemycash\\.com/embeddedflash\\.php")) {
            logger.info("Link offline: " + parameter);
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