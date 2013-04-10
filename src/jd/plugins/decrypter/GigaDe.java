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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "giga.de" }, urls = { "http://(www\\.)?giga\\.de/tv/(?!live|alle\\-videos|downloads)[a-z0-9\\-]+/" }, flags = { 0 })
public class GigaDe extends PluginForDecrypt {

    public GigaDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://www.giga.de/tv/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h1 class=\"entry\\-title\">([^<>\"/]+)</h1>").getMatch(0);
        /** Add embedded videos if there are */
        final String youtubeLink = br.getRegex("<embed src=\"(http://(www\\.)?youtube\\.com/v/[^<>\"]*?)\"").getMatch(0);
        if (youtubeLink != null) {
            decryptedLinks.add(createDownloadlink(youtubeLink));
            return decryptedLinks;
        }
        final String[][] links = br.getRegex("id=\"NVBPlayer(\\d+\\-\\d+)\">.*?<span property=\"media:title\" content=\"([^<>\"/]+)\".*?<source src=\"(http://video\\.giga\\.de/data/[a-z0-9\\-]+\\-normal\\.mp4)\"").getMatches();
        final String[] otherLinks = br.getRegex("rel=\"media:video\" resource=\"(http://(www\\.)?video\\.giga\\.de/data/[^<>/\"]*?\\.mp4)\"").getColumn(0);
        if ((links == null || links.length == 0) && (otherLinks == null || otherLinks.length == 0) || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        if (links != null && links.length != 0) {
            for (String[] singleLink : links) {
                /**
                 * Change normal links to HD links, this will work as long as they always look the same. If this is changed we can use the
                 * ID (singleLink[0]) to get the HD link
                 */
                final DownloadLink dl = createDownloadlink("directhttp://" + singleLink[2].replace("-normal.mp4", "-hd.mp4"));
                dl.setFinalFileName(Encoding.htmlDecode(singleLink[1].trim()) + ".mp4");
                decryptedLinks.add(dl);
            }
        }
        if (otherLinks != null && otherLinks.length != 0) {
            for (String singleLink : otherLinks) {
                /**
                 * Change normal links to HD links, this will work as long as they always look the same. If this is changed we can use the
                 * ID (singleLink[0]) to get the HD link
                 */
                final DownloadLink dl = createDownloadlink("directhttp://" + singleLink.replace("-normal.mp4", "-hd.mp4"));
                dl.setFinalFileName(fpName + ".mp4");
                decryptedLinks.add(dl);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}