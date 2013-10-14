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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extreme-protect.net" }, urls = { "http://(www\\.)?extreme\\-protect\\.net/[a-z0-9\\-_]+" }, flags = { 0 })
public class ExtremeProtectNet extends PluginForDecrypt {

    public ExtremeProtectNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        try {
            br.setAllowedResponseCodes(new int[] { 500 });
        } catch (final Throwable e) {
            logger.info("Link can only be decrypted in JDownloader 2: " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getURL().equals("http://extreme-protect.net/404")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String pass = generatePass();

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Cache-Control", null);
        br.postPage("/requis/captcha_formulaire.php", "action=qaptcha&qaptcha_key=" + pass);

        br.getHeaders().put("Accept", "text/html, application/xhtml+xml, */*");
        br.getHeaders().put("X-Requested-With", null);
        br.postPage(parameter, pass + "=&submit_captcha=VALIDER");

        String[] links = br.getRegex("class=\"lien\" ><a target=\"_blank\" href=\"(http[^<>\"]*?)\"").getColumn(0);
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String generatePass() {
        int nb = 32;
        final String chars = "azertyupqsdfghjkmwxcvbn23456789AZERTYUPQSDFGHJKMWXCVBN_-#@";
        String pass = "";

        for (int i = 0; i < nb; i++) {
            long wpos = Math.round(Math.random() * (chars.length() - 1));
            int lool = (int) wpos;
            pass += chars.substring(lool, lool + 1);
        }
        return pass;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}