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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protect-url.net" }, urls = { "http://(www\\.)?protect\\-url\\.net/[a-z0-9]+\\-lnk\\.html" }, flags = { 0 })
public class ProtectUrlNet extends PluginForDecrypt {

    public ProtectUrlNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PASSWRONG = "window\\.location = \"linkcheck\\.php\\?linkid=[a-z0-9]+\\&message=wrong\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // Such links can also be decrypted (still...)
        // if
        // (br.containsHTML("(>ACCÈS REFUSÉ<|<td valign=top align=center><img src=images/icon_stop\\.gif)"))
        // {
        // logger.info("Can't decrypt link: " + parameter);
        // logger.info("...because link unavailable or invalid referer!");
        // return decryptedLinks;
        // }
        for (int i = 0; i <= 3; i++) {
            String postData = null;
            if (br.containsHTML(">Sécurité Anti\\-Robot:") || br.getURL().contains("protect-url.net/check.")) {
                postData = "captx=ok&linkid=" + new Regex(parameter, "protect\\-url\\.net/([^<>\"]*?)\\-lnk\\.html").getMatch(0) + "&";
            }
            if (br.containsHTML(">Mot de Passe:<")) {
                final String passCode = getUserInput("Enter password for: " + parameter, param);
                postData += "password=" + passCode;
            }
            if (postData != null) {
                br.postPage("http://protect-url.net/linkid.php", postData);
                if (br.containsHTML(PASSWRONG)) {
                    br.getPage(parameter);
                    continue;
                }
            }
            break;
        }
        if (br.containsHTML(PASSWRONG)) throw new DecrypterException(DecrypterException.PASSWORD);
        String fpName = br.getRegex("<b>Titre:</b>[\t\n\r ]+</td>[\t\n\r ]+<td style=\\'border:1px;font\\-weight:bold;font\\-size:90%;font\\-family:Arial,Helvetica,sans\\-serif;\\'>([^<>\"]*?)</td>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<img id=\\'gglload\\' src=\\'images/icon\\-magnify\\.png\\' style=\"vertical\\-align: middle;\"></span>([^<>\"]*?) </td>").getMatch(0);
        String[] links = HTMLParser.getHttpLinks(br.toString(), null);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            if (!this.canHandle(singleLink)) decryptedLinks.add(createDownloadlink(singleLink));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}