//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
//
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "revivelink.com" }, urls = { "http://(www\\.)?revivelink.com/\\??[A-Z0-9]+" }, flags = { 0 })
public class ReviveLinkCom extends PluginForDecrypt {

    public ReviveLinkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?revivelink.com/\\?(contact|register|forgot|login)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // Logger logDebug = JDLogger.getLogger();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String strParameter = param.toString();
        if (strParameter.matches(INVALIDLINKS)) {
            logger.info("Link offline: " + strParameter);
            return decryptedLinks;
        }

        // Get the package name
        String strName = "";

        br.setFollowRedirects(false);
        br.getPage(strParameter);

        if (br.containsHTML("(An error has occurred|The article cannot be found)") || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + strParameter);
            return decryptedLinks;
        }

        if (br.containsHTML("class=\"QapTcha\"")) {
            final String fid = new Regex(strParameter, "([A-Z0-9]+)$").getMatch(0);
            final String pass = generatePass();
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://revivelink.com/qcap/Qaptcha.jquery.php", "action=qaptcha&qaptcha_key=" + pass);
            if (!br.containsHTML("\"error\":false")) {
                logger.warning("Decrypter broken for link: " + strParameter);
                return null;
            }
            br.getPage("http://revivelink.com/liens.php?R=" + fid + "&" + Encoding.urlEncode(pass) + "=");
        }

        final String[] links = br.getRegex("<a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + strParameter);
            return null;
        }

        // Added links
        for (String alink : links) {
            if (!alink.contains("revievelink.com/")) decryptedLinks.add(createDownloadlink(alink));
        }

        // Add all link in a package
        final FilePackage fp = FilePackage.getInstance();
        if (strName != "") fp.setName(strName);
        fp.addLinks(decryptedLinks);
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
        return false;
    }

}