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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "peeplink.in", "alfalink.info" }, urls = { "http://(www\\.)?peeplink\\.in/[a-z0-9]+", "http://(www\\.)?alfalink\\.info/[a-z0-9]+" })
public class PrrpLinkIn extends PluginForDecrypt {
    public PrrpLinkIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            br.getPage(redirect);
        }
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            final DownloadLink dead = createDownloadlink("directhttp://" + parameter);
            dead.setAvailable(false);
            dead.setProperty("offline", true);
            decryptedLinks.add(dead);
            return decryptedLinks;
        } else if (br.containsHTML("value=\"Enter Access Password\"")) {
            logger.info("Password protected links are not yet supported");
            final DownloadLink dead = createDownloadlink("directhttp://" + parameter);
            dead.setAvailable(false);
            dead.setProperty("offline", true);
            decryptedLinks.add(dead);
            return decryptedLinks;
        }
        if (br.containsHTML(">Shoot</a></li>")) {
            br.postPage("/qaptcha/php/Qaptcha.jquery.php", "action=qaptcha");
            br.postPage(parameter, "iQapTcha=");
        }
        String finallink = br.getRegex("<article.*?>(.*?)</article").getMatch(0);
        if (finallink != null) {
            final String[] finallinks = HTMLParser.getHttpLinks(finallink, "");
            if (finallinks != null && finallinks.length != 0) {
                for (final String aLink : finallinks) {
                    decryptedLinks.add(createDownloadlink(aLink));
                }
                return decryptedLinks;
            } else {
                finallink = null;
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Out of date: " + parameter);
            return null;
        }
        return decryptedLinks;
    }
}
