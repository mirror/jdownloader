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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

/**
 * DON'T ADD GREEDY REGEX!!!
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "potlocker.me" }, urls = { "http://(www\\.)?potlocker\\.(net/[a-z0-9\\-]+/\\d{4}/[a-z0-9\\-]+\\.html|(re|me)/[a-z0-9\\-_]+_[a-f0-9]{9}\\.html|me/embed\\.php\\?vid=[a-z0-9\\-]+)" })
public class PotlockerRe extends antiDDoSForDecrypt {

    public PotlockerRe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("potlocker.re/", "potlocker.me/");
        String finallink = null;
        if (parameter.matches(".+/embed\\.php\\?vid=[a-z0-9\\-]+")) {
            br.setFollowRedirects(false);
            getPage(parameter.replace("/embed.php", "/videos.php"));
            finallink = br.getRedirectLocation();
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        } else {
            getPage(parameter);
            final String potlockerdirect = br.getRegex("file:\\s*'(http://potlocker\\.(?:re|me|net)/videos\\.php\\?vid=[a-z0-9]+)'").getMatch(0);
            if (potlockerdirect != null) {
                getPage(potlockerdirect);
                finallink = br.getRedirectLocation();
            } else {
                if (br.getRedirectLocation() != null) {
                    getPage(br.getRedirectLocation());
                }
                finallink = br.getRegex("<IFRAME[^>]*\\s+SRC=\"(http[^\"]+)\"").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }

}
