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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "amateurdumper.com" }, urls = { "http://(www\\.)?amateurdumper\\.com/(index\\.php\\?ctr=view\\&id=\\d+|\\d+/.*?\\.html)" })
public class AmateurDumperCom extends PornEmbedParser {
    public AmateurDumperCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("No htmlCode read|>404 The page was not found!<")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String externID = br.getRedirectLocation();
        if (externID != null) {
            DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String filename = br.getRegex("<div class=\"video\\-hed hed3\">[\t\n\r ]+<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\" />").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(?:Homemade Sex :: )?(.*?)( - Videos - Amateur Dumper)?</title>").getMatch(0);
            }
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        if (filename == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        filename = filename.trim();
        externID = br.getRegex("flash\\.serious\\-cash\\.com/flvplayer\\.swf\".*?flashvars=\"(\\&)?file=([^<>\"]*?)\\&").getMatch(1);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("directhttp://http://flash.serious-cash.com/" + externID + ".flv");
            decryptedLinks.add(dl);
            dl.setFinalFileName(filename + ".flv");
            return decryptedLinks;
        }
        externID = br.getRegex("file=(http://(www\\.)?hostave\\d+\\.net/.*?)\\&screenfile").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("var urlAddress = \"(http://.*?)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?xxxhdd\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\\&file=(http://static\\.mofos\\.com/.*?)\\&enablejs").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("addVariable\\(\\'file\\',\\'(http://.*?)\\'\\)").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("\\'(http://(www\\.)?amateurdumper\\.com/videos/.*?)\\'").getMatch(0);
        }
        if (externID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("\"http://(www\\.)?seemybucks\\.com/flvexporter/flvplayer\\.swf\"")) {
            logger.info("Link broken: " + parameter);
            return decryptedLinks;
        }
        externID = br.getRegex("<iframe[^<>]*?src=\"(https?://.*?)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            dl.setProperty("filename", filename);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        throw new DecrypterException("Decrypter broken for link: " + parameter);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}