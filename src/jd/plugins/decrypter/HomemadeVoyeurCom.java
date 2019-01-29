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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "homemade-voyeur.com" }, urls = { "https?://(?:www\\.)?(homemade\\-voyeur|yourvoyeurvideos)\\.com/(?:(?:tube/)?video/|tube/gallery/|\\d+/)[A-Za-z0-9\\-]+\\.html" })
public class HomemadeVoyeurCom extends PluginForDecrypt {
    public HomemadeVoyeurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String tempID = br.getRedirectLocation();
        // Invalid link
        if ("http://www.homemade-voyeur.com/".equals(tempID) || br.containsHTML(">404 Not Found<") || br.containsHTML("<title>Homemade Voyeur - Hosted Voyeur Videos - Biggest Voyeur Vids Archive on the Net</title>") || this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // Offline link
        if (br.containsHTML("This video does not exist\\!< | >\\s+Video Not Found\\s+<")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink(tempID));
            return decryptedLinks;
        }
        String filename = br.getRegex("<meta name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Your Voyeur (Videos|Pics) \\-\\s*(.*?)\\s*</title>").getMatch(1);
            if (filename == null) {
                filename = br.getRegex("<title>(.+) \\- Voyeur (Videos|Pics) \\- .+</title>").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<div class=\"titlerr\"[^>]+>([^\r\n]+)</div>").getMatch(0);
            }
        }
        if (filename == null) {
            /* Fallback to url-filename */
            filename = new Regex(parameter, "([A-Za-z0-9\\-]+)\\.html$").getMatch(0);
        }
        if (parameter.contains("/tube/gallery/")) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(filename.trim());
            final DecimalFormat df = new DecimalFormat("0000");
            String[] images = br.getRegex("(https?://(www\\.)?homemade\\-voyeur\\.com/tube/images/galleries/\\d+/\\d+/[a-z0-9]{32}\\.jpg)").getColumn(0);
            for (String image : images) {
                final DownloadLink dl = createDownloadlink("directhttp://" + image);
                dl.setFinalFileName(filename + " - " + df.format(decryptedLinks.size() + 1) + image.substring(image.lastIndexOf(".")));
                fp.add(dl);
                decryptedLinks.add(dl);
            }
            return decryptedLinks;
        } else {
            tempID = br.getRegex("\"(http://api\\.slutdrive\\.com/homemadevoyeur\\.php\\?id=\\d+\\&type=v)\"").getMatch(0);
            if (tempID != null) {
                br.getPage(tempID);
                if (br.containsHTML(">404 Not Found<")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Cannot handle link: " + tempID);
                logger.warning("Mainlink: " + parameter);
                return null;
            }
            tempID = br.getRegex("var playlist = \\[ \\{ url: escape\\(\\'(http://[^<>\"]*?)\\'\\) \\} \\]").getMatch(0);
            if (tempID == null) {
                tempID = br.getRegex("var playlist = \\[ [^\\]]+(http://[^<>\"\\]\\}]+)").getMatch(0);
            }
            if (tempID == null) {
                tempID = br.getRegex("(\\'|\")(http://(hosted\\.yourvoyeurvideos\\.com/videos/\\d+\\.flv|[a-z0-9]+\\.yourvoyeurvideos\\.com/mp4/\\d+\\.mp4))(\\'|\")").getMatch(1);
            }
            if (tempID == null) {
                tempID = br.getRegex("file=(http[^&\"]+)").getMatch(0);
            }
            if (tempID != null && tempID.contains(".jpg")) {
                logger.info("This url is only advertising --> Offline");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            /* Last chance - directurl */
            if (tempID == null) {
                tempID = br.getRegex("<source type=\"video/mp4\" src=\"([^\"]+)\"").getMatch(0);
            }
            if (tempID == null) {
                tempID = br.getRegex("<source src=\"([^\"]+)\" type=\"video/mp4\"").getMatch(0);
            }
            if (tempID == null || filename == null) {
                logger.info("filename: " + filename + ", tempID: " + tempID);
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename.trim() + tempID.substring(tempID.lastIndexOf(".")));
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}