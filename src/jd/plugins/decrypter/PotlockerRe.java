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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "potlocker.re" }, urls = { "http://(www\\.)?potlocker\\.(net/[a-z0-9\\-]+/\\d{4}/[a-z0-9\\-]+|re/[a-z0-9\\-_]+)\\.html" }, flags = { 0 })
public class PotlockerRe extends PluginForDecrypt {

    public PotlockerRe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String finallink = null;
        if (parameter.matches("http://(www\\.)?potlocker\\.(re|net)/(newvideos|browse\\-.*?|login|index|contact_us|register|topvideos)\\.html")) {
            logger.info("Link invalid (unsupported): " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);
        final String potlockerdirect = br.getRegex("file: \\'(http://potlocker\\.re/videos\\.php\\?vid=[a-z0-9]+)\\'").getMatch(0);
        if (potlockerdirect != null) {
            br.getPage(potlockerdirect);
            finallink = br.getRedirectLocation();
        } else {
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            finallink = br.getRegex("<IFRAME SRC=\"(http[^<>\"]*?)\" FRAMEBORDER=0").getMatch(0);
        }
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
