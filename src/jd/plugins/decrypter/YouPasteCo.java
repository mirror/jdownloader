//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

/**
 * notes: using cloudflare
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youpaste.co" }, urls = { "http://(www\\.)?youpaste\\.co/index\\.php/paste/[a-zA-Z0-9_/\\+\\=\\-]+" }, flags = { 0 })
public class YouPasteCo extends antiDDoSForDecrypt {

    public YouPasteCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        getPage(parameter);
        if (br.containsHTML(">Lo sentimos, este paste no existe")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        if (br.containsHTML("name=\"capcode\" id=\"capcode\"")) {
            final int repeat = 3;
            // using dummie DownloadLink for auto retry code within handleKeyCaptcha
            final DownloadLink dummie = createDownloadlink(parameter);
            for (int i = 0; i < repeat; i++) {
                String result = null;
                final PluginForDecrypt keycplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                try {
                    final jd.plugins.decrypter.LnkCrptWs.KeyCaptcha kc = ((jd.plugins.decrypter.LnkCrptWs) keycplug).getKeyCaptcha(br);
                    result = kc.handleKeyCaptcha(parameter, dummie);
                } catch (final Throwable e) {
                    result = null;
                }
                if ("CANCEL".equals(result)) {
                    return decryptedLinks;
                }
                if (result == null) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                postPage(br.getURL(), "capcode=" + Encoding.urlEncode(result));
                if (br.containsHTML("name=\"capcode\" id=\"capcode\"")) {
                    if (i + 1 == repeat) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                    continue;
                } else {
                    break;
                }
            }
        }

        final String[] links = br.getRegex("<p style=\"text-align:center;\">(https?://.*?)</p>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String link : links) {
            decryptedLinks.add(createDownloadlink(link));
        }
        // Will always be empty because we added the links via cnl2
        return decryptedLinks;
    }

}