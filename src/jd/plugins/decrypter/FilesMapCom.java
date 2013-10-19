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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmap.com" }, urls = { "http://(www\\.)?((es|ar|en|pt|ru|ja|de|fr|tr|pl)?\\.)?filesmap\\.com/(mp3/[A-Za-z0-9]+/[a-z0-9\\-]+|file/[A-Za-z0-9]+/[^<>\"/]+/[^<>\"/]+)" }, flags = { 0 })
public class FilesMapCom extends PluginForDecrypt {

    public FilesMapCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("((es|ar|en|pt|ru|ja|de|fr|tr|pl)\\.)?filesmap\\.com/", "filesmap.com/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setCookie("http://filesmap.com/", "Language", "en");
        if (br.getURL().matches("http://(www\\.)?((es|ar|en|pt|ru|ja|de|fr|tr|pl)?\\.)?filesmap\\.com/?") || br.containsHTML("<title>Your files search engine \\- FilesMap\\.com</title>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches(".*?filesmap\\.com/mp3/.*?")) {
            /** Handling for MP3 links */
            String dlID = br.getRegex("\"http://(www\\.)?goear\\.com/files/local\\.swf\\?file=([A-Za-z0-9]+)\"").getMatch(1);
            if (dlID == null) dlID = br.getRegex("filesmap\\.com/mp3s/GoEarDownload/([A-Za-z0-9]+)/\\'").getMatch(0);
            if (dlID == null) {
                if (br.getRegex("onclick=\"window\\.open\\(\\'(http://(es|ar|en|pt|ru|ja|de|fr|tr|pl)\\.filesmap\\.com/mp3s/dobdDownload/[^<>\"]*?)\\'\\)").getMatch(0) != null) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink("http://www.goear.com/listen/" + dlID + "/" + "x"));
        } else {
            if (br.getURL().contains("/files/search/")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            /** Handling for all others */
            String fpName = br.getRegex("property=\"og:title\" content=\"(.*?) \\(\\d+\\.?.{1,6}\\)").getMatch(0);
            if (fpName == null) fpName = br.getRegex("<title>(.*?) \\(\\d+\\.?.{1,6}\\)").getMatch(0);
            final String[] links = br.getRegex("\"http://(www\\.)?((es|ar|en|pt|ru|ja|de|fr|tr|pl)?\\.)?filesmap\\.com/redirect/\\?url=(http[^<>\"]*?)\"").getColumn(3);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links)
                decryptedLinks.add(createDownloadlink(singleLink));
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}