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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deredactie.be", "sporza.be" }, urls = { "http://(www\\.)?deredactie\\.be/(permalink/\\d\\.\\d+(\\?video=\\d\\.\\d+)?|cm/vrtnieuws([^/]+)?/(mediatheek|videozone).+)", "http://(www\\.)?sporza\\.be/(permalink/\\d\\.\\d+|cm/(vrtnieuws|sporza)([^/]+)?/(mediatheek|videozone).+)" }) 
public class DeredactieBe extends PluginForDecrypt {

    public DeredactieBe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // Link offline
        if (br.containsHTML("(>Pagina \\- niet gevonden<|>De pagina die u zoekt kan niet gevonden worden)")) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable t) {
                logger.info("Offline Link: " + parameter);
            }
        }
        HashMap<String, String> mediaValue = new HashMap<String, String>();
        for (String[] s : br.getRegex("data\\-video\\-([^=]+)=\"([^\"]+)\"").getMatches()) {
            mediaValue.put(s[0], s[1]);
        }
        final String finalurl = (mediaValue == null || mediaValue.size() == 0) ? null : mediaValue.get("src");
        if (finalurl == null) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable t) {
                logger.info("Offline Link: " + parameter);
            }
            return decryptedLinks;
        }
        if (finalurl.contains("youtube.com")) {
            decryptedLinks.add(createDownloadlink(finalurl));
        } else {
            decryptedLinks.add(createDownloadlink(parameter.replace(".be/", "decrypted.be/")));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}