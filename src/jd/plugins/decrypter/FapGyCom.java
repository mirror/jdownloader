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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fapgay.com" }, urls = { "http://(www\\.)?fapgay\\.com/[a-z0-9\\-]+" }, flags = { 0 })
public class FapGyCom extends PluginForDecrypt {

    public FapGyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().contains("fapgay.com/404") || br.containsHTML("(> Snap\\! The page you were looking for isn\\'t here anymore\\. <|<title>Page Not Found \\- fapgay</title>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // Fox xtube.com
        String tempID = new Regex(Encoding.htmlDecode(br.toString()), "value=\"(http://(www\\.)?xtube\\.com/watch\\.php\\?v=[A-Za-z0-9_\\-]+)\"").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink(tempID));
            return decryptedLinks;
        }
        // For gaytube.com
        tempID = new Regex(Encoding.htmlDecode(br.toString()), "playerConfig/(\\d+)\\.xml").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink("http://www.gaytube.com/media/" + tempID));
            return decryptedLinks;
        }
        // For pornhub.com, can't get normal pornhub links here but directlink
        // is available
        tempID = new Regex(Encoding.htmlDecode(br.toString()), "pornhub\\.com/embed_player\\.php\\?id=(\\d+)\"").getMatch(0);
        if (tempID != null) {
            br.getPage("http://www.pornhub.com/embed_player.php?id=" + tempID);
            String finallink = br.getRegex("<link_url>(http://.*?)</link_url>").getMatch(0);
            if (finallink == null) {
                logger.warning("Error in pornhub handling for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }
        if (tempID == null) {
            logger.warning("Decrypt failed for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}