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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xxxaporn.com" }, urls = { "http://(www\\.)?xxxaporn\\.com/\\d+/[A-Za-z0-9\\-_]+\\.html" }, flags = { 0 })
public class XXXAPornComDecrypter extends PluginForDecrypt {

    public XXXAPornComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("No htmlCode read")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("xxxaporn.com/", "xxxaporndecrypted.com/"));
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String externID = br.getRedirectLocation();
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?submityourflicks\\.com/embedded/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("flashvars=\"config=(http://(www\\.)?book\\-mark\\.net/playerconfig/\\d+/\\d+\\.xml)\"").getMatch(0);
        if (externID == null) externID = br.getRegex("value=\\'conf(ig)?=(http://media\\.amateurcumshots\\.org/flv_player/data/playerConfigEmbed/\\d+\\.xml)").getMatch(1);
        if (externID != null) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("xxxaporn.com/", "xxxaporndecrypted.com/")));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}