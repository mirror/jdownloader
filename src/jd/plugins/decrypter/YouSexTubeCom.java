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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

//EmbedDecrypter 0.1.2
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "you-sex-tube.com" }, urls = { "http://(www\\.)?you\\-sex\\-tube\\.com/(video|porn)/.+" }, flags = { 0 })
public class YouSexTubeCom extends PluginForDecrypt {

    public YouSexTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (!parameter.contains(".html")) parameter += ".html";
        br.setFollowRedirects(false);
        String externID = null;
        boolean run = true;
        String redirect = null;
        do {
            final String fid = new Regex(parameter, "you\\-sex\\-tube\\.com/(video|porn)/(.*?)\\.html").getMatch(1);
            externID = Encoding.Base64Decode(fid);
            if (externID != null && !externID.equals(fid)) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            br.getPage(parameter);
            redirect = br.getRedirectLocation();
            if (redirect != null && redirect.contains("you-sex-tube.com/")) {
                parameter = redirect;
            } else {
                run = false;
            }
        } while (run);
        if (redirect != null && !redirect.contains("you-sex-tube.com/")) {
            final DownloadLink dl = createDownloadlink(redirect);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("redtube\\.com/player/\"><param name=\"FlashVars\" value=\"id=(\\d+)\\&").getMatch(0);
        if (externID == null) externID = br.getRegex("embed\\.redtube\\.com/player/\\?id=(\\d+)\\&").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.redtube.com/" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("(\"|\\')(http://(www\\.)?tube8\\.com/embed/[^<>\"/]*?/[^<>\"/]*?/\\d+/?)(\"|\\')").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID.replace("tube8.com/embed/", "tube8.com/")));
            return decryptedLinks;
        }
        if (br.getURL().contains("xvideos.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        logger.warning("Decrypter broken for link: " + parameter);
        return null;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}