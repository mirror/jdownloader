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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amateurdumper.com" }, urls = { "http://(www\\.)?amateurdumper\\.com/(index\\.php\\?ctr=view\\&id=\\d+|\\d+/.*?\\.html)" }, flags = { 0 })
public class AmateurDumperCom extends PluginForDecrypt {

    public AmateurDumperCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Link broken: " + parameter);
            return decryptedLinks;
        }
        String tempID = br.getRedirectLocation();
        if (tempID != null) {
            DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String filename = br.getRegex("<div class=\"video\\-hed hed3\">[\t\n\r ]+<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\" />").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>Homemade Sex :: (.*?)</title>").getMatch(0);
            }
        }
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = filename.trim();
        tempID = br.getRegex("\"(http://(www\\.)?myxvids\\.com/embed/\\d+)\"").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink(tempID));
            return decryptedLinks;
        }
        tempID = br.getRegex("flash\\.serious\\-cash\\.com/flvplayer\\.swf\".*?flashvars=\"file=([^<>\"]*?)\\&").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://http://flash.serious-cash.com/" + tempID + ".flv");
            decryptedLinks.add(dl);
            dl.setFinalFileName(filename + ".flv");
            return decryptedLinks;
        }
        tempID = br.getRegex("file=(http://(www\\.)?hostave\\d+\\.net/.*?)\\&screenfile").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        tempID = br.getRegex("var urlAddress = \"(http://.*?)\"").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("\\&file=(http://static\\.mofos\\.com/.*?)\\&enablejs").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        tempID = br.getRegex("addVariable\\(\\'file\\',\\'(http://.*?)\\'\\)").getMatch(0);
        if (tempID == null) tempID = br.getRegex("\\'(http://(www\\.)?amateurdumper\\.com/videos/.*?)\\'").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        tempID = br.getRegex("src=\"http://videos\\.allelitepass\\.com/txc/([^<>\"/]*?)\\.swf\"").getMatch(0);
        if (tempID != null) {
            br.getPage("http://videos.allelitepass.com/txc/player.php?video=" + Encoding.htmlDecode(tempID));
            tempID = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
            if (tempID != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }

        }
        if (br.containsHTML("\"http://(www\\.)?seemybucks\\.com/flvexporter/flvplayer\\.swf\"")) {
            logger.info("Link broken: " + parameter);
            return decryptedLinks;
        }
        if (tempID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
