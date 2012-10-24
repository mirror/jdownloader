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

import java.io.IOException;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animeratio.com" }, urls = { "http://(www\\.)?animeratio\\.com/anime/[^<>\"/]+/[^<>\"/]+/" }, flags = { 0 })
public class AnimeRtioCom extends PluginForDecrypt {

    public AnimeRtioCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        // Link offline
        if (br.containsHTML("<title>Nothing found for")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Link abused
        if (br.containsHTML(">Link Removed by Request of")) {
            logger.info("Link abused: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h2 class=\"post\\-title\">([^<>\"]*?)</h2>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>([^<>\"]*?) \\&raquo; AnimeRatio\\.com</title>").getMatch(0);
        final String[] vidEntries = br.getRegex("<div id=\"v\\d+\">(.*?)</div>").getColumn(0);
        if (vidEntries == null || vidEntries.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String vidEntry : vidEntries) {
            final String finallink = findLink(vidEntry);
            if (finallink == null) {
                logger.info("Link of one entry is not found for link: " + parameter);
            } else {
                decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String findLink(final String vidEntry) throws IOException {
        /**
         * Important dev notice: here we got all regexes to decrypt links.
         * Probably we ll never have all to decrypt 100% of the mirrors but
         * before adding new regexes please check if the one you want to
         * implement already exists but is broken!!
         */
        String tempID = new Regex(vidEntry, "dailymotion\\.com/swf/video/([a-z0-9\\-_]+)\"").getMatch(0);
        if (tempID != null) return "http://www.dailymotion.com/video/" + tempID + "_" + System.currentTimeMillis();
        tempID = new Regex(vidEntry, "\\'(http://(www\\.)?veevr\\.com/embed/[A-Za-z0-9\\-_]+)").getMatch(0);
        if (tempID != null) return tempID;
        tempID = new Regex(vidEntry, "sapo\\.php\\?id=([^<>\"]*?)\"").getMatch(0);
        if (tempID != null) {
            br.getPage("http://www.animeratio.com/files/streaming/sapo/?id=" + tempID);
            tempID = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
            return tempID;
        }
        tempID = new Regex(vidEntry, "glumbo\\.php\\?id=([a-z0-9]{12})").getMatch(0);
        if (tempID != null) return "http://glumbouploads.com/" + tempID;
        tempID = new Regex(vidEntry, "streaming/veoh/v2\\.php\\?id=([^<>\"]*?)\"").getMatch(0);
        if (tempID != null) return "http://www.veoh.com/watch/" + tempID;
        tempID = new Regex(vidEntry, "putlockers\\.php\\?id=([^<>\"]*?)\"").getMatch(0);
        if (tempID != null) return "http://putlocker.com/file/" + tempID;
        tempID = new Regex(vidEntry, "/files/embed/filebox\\.php\\?id=([a-z0-9]{12})\"").getMatch(0);
        if (tempID != null) return "http://filebox.com/" + tempID;
        tempID = new Regex(vidEntry, "yourupload\\.php\\?id=([a-z0-9]+)\"").getMatch(0);
        if (tempID != null) return "http://yourupload.com/file/" + tempID;
        tempID = new Regex(vidEntry, "(files/embed/myspace\\.php\\?id=\\d+)\"").getMatch(0);
        if (tempID != null) {
            br.getPage("http://www.animeratio.com/" + tempID);
            tempID = br.getRegex("file: \"(http://[^<>\"]*?)\",").getMatch(0);
            return "directhttp://" + tempID;
        }
        tempID = new Regex(vidEntry, "data=\"http://(www\\.)?facebook\\.com/v/(\\d+)\"").getMatch(0);
        if (tempID != null) return "http://www.facebook.com/video/video.php?v=" + tempID;
        tempID = new Regex(vidEntry, "frameborder=\"0\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (tempID != null) return tempID;
        tempID = new Regex(vidEntry, "data=\"(http://(www\\.)?(videozer\\.com|videobb\\.com|video\\.rutube\\.ru)/[^<>\"]*?)\"").getMatch(0);
        if (tempID != null) return tempID;
        return null;
    }
}
