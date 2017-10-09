//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

/**
 *
 * @version raz_Template
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dl-protecte.com" }, urls = { "https?://(?:www\\.)?(?:dl-protecte\\.(?:com|org)|protect-lien\\.com|protect-zt\\.com)/\\S+" })
public class DlPrteCom extends antiDDoSForDecrypt {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "dl-protecte.com", "dl-protecte.org", "protect-lien.com", "protect-zt.com" };
    }

    public DlPrteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (br.containsHTML("Page Not Found") || br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // some weird form that does jack
        final Form f = br.getFormbyProperty("class", "magic");
        if (f == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        // insert some magic
        final String magic = getSoup();
        f.put(magic, "");
        // ajax stuff
        {
            final Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            postPage(ajax, "/php/Qaptcha.jquery.php", "action=qaptcha&qaptcha_key=" + magic);
            // should say error false.
        }
        submitForm(f);
        // link
        final String link = br.getRegex("<div class=\"lienet\"><a href=\"(.*?)\">").getMatch(0);
        if (link == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        final DownloadLink dl = createDownloadlink(link);
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    private String getSoup() {
        final Random r = new Random();
        final String soup = "azertyupqsdfghjkmwxcvbn23456789AZERTYUPQSDFGHJKMWXCVBN_-#@";
        String v = "";
        for (int i = 0; i < 31; i++) {
            v = v + soup.charAt(r.nextInt(soup.length()));
        }
        return v;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}