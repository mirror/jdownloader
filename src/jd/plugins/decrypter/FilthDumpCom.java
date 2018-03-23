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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filthdump.com" }, urls = { "http://(www\\.)?filthdump\\.com/\\d+/.*?\\.html" })
public class FilthDumpCom extends PluginForDecrypt {
    public FilthDumpCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>(.*?) :: Amateur Porn </title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        }
        if (filename == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        filename = filename.trim();
        if (filename.equals("")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String tempID = br.getRegex("settings=(http://(www\\.)?(tube\\.)?watchgfporn\\.com/playerConfig\\.php\\?.*?)\"").getMatch(0);
        if (tempID != null) {
            br.setFollowRedirects(true);
            try {
                br.getPage(tempID);
                if (br.containsHTML("Page Not Found")) {
                    decryptedLinks.add(createOfflinelink(parameter));
                    return decryptedLinks;
                }
            } catch (Exception UnknownHostException) {
                logger.info("Embeded video provider DNS is down.! This is not a bug: " + parameter);
                // throw UnknownHostException;
                return decryptedLinks;
            }
            String finallink = br.getRegex("defaultVideo:(http://.*?);").getMatch(0);
            if (finallink == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("addParam\\(\\'flashvars\\',\\'\\&file=(http://video\\.teensexmovs\\.com/.*?)\\&").getMatch(0);
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("flvURL=(.*?)\\&destinationURL").getMatch(0);
        if (tempID != null) {
            tempID = Encoding.Base64Decode(tempID);
            if (tempID == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("(http://(www\\.)?mygirlfriendporn\\.com/playerConfig\\.php\\?[^<>\"/\\&]*?)\"").getMatch(0);
        if (tempID != null) {
            br.getPage(Encoding.htmlDecode(tempID));
            if (br.containsHTML("Page Not Found")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            tempID = br.getRegex("flvMask:(http://[^<>\"]*?);").getMatch(0);
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            if (tempID == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("\\&file=(http://[^<>\"]*?)\\&").getMatch(0);
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("<iframe[^<>]*?(http[^<>\"]*?)\"").getMatch(0);
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("config=http://www.dump1.com|<div id=\"mask\"></div>\\s*<div id=\"extras\">")) { // Dump1.com is for sale
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("\"http://(www\\.)?videos\\.cdn\\.filthmedia\\.net/hackedgfvideosf\\.php\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        throw new DecrypterException("Decrypter broken for link: " + parameter);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}