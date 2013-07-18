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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "revision3.com" }, urls = { "http://(www\\.)?revision3\\.com/(?!blog|api|content|category|search|shows|login|forum|episodes|host|network)[a-z0-9]+/(?!feed|about)[a-z0-9\\-]+" }, flags = { 0 })
public class RevisionThreeCom extends PluginForDecrypt {

    public RevisionThreeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<title>(.*?) \\- ").getMatch(0);
        if (fpName != null) fpName = fpName.replace("...", "");
        // Probably this handling is also good for more links but that's not
        // tested yet!
        if (parameter.matches("http://(www\\.)?revision3\\.com/rev3gamesoriginals/[a-z0-9\\-]+")) {
            final String[] directLinks = br.getRegex("<a class=\"sizename\" href=\"(http://[^<>\"]*?)\">").getColumn(0);
            if (directLinks == null || directLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : directLinks) {
                final DownloadLink fina = createDownloadlink("directhttp://" + singleLink);
                if (fpName != null) fina.setFinalFileName(fpName + singleLink.substring(singleLink.length() - 4, singleLink.length()));
                decryptedLinks.add(fina);
            }
        } else {
            if (br.containsHTML("ey there\\! You look a little lo|404: Page Not Found<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String videoID = br.getRegex("\\'video_id\\', (\\d+)\\);").getMatch(0);
            if (videoID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage("http://revision3.com/api/getPlaylist.json?api_key=ba9c741bce1b9d8e3defcc22193f3651b8867e62&codecs=h264,vp8,theora&video_id=" + videoID + "&jsonp=parseResponse&_=" + System.currentTimeMillis());
            final String[] allLinks = br.getRegex("\"url\":\"(http:[^<>\"]*?)\"").getColumn(0);
            if (allLinks == null || allLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String finallink : allLinks) {
                finallink = finallink.replace("\\", "");
                final DownloadLink fina = createDownloadlink("directhttp://" + finallink);
                if (fpName != null) {
                    String ext = finallink.substring(finallink.lastIndexOf("."));
                    if (ext != null && ext.length() < 5) fina.setFinalFileName(fpName + ext);
                }
                decryptedLinks.add(fina);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}