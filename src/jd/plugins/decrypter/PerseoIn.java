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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "perseo.in" }, urls = { "http://(www\\.)?perseo\\.in/(f/[A-Za-z0-9]+|[a-z0-9]+)" }, flags = { 0 })
public class PerseoIn extends PluginForDecrypt {

    public PerseoIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PERSEOFOLDER = "http://(www\\.)?perseo\\.in/f/[A-Za-z0-9]+";
    private static final String INVALIDLINKS = "http://(www\\.)?perseo\\.in/(analytics|contact)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);
        if (parameter.matches(PERSEOFOLDER)) {
            if (br.containsHTML("<table class=\"linktable\">[\t\n\r ]+</table>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String linkTable = br.getRegex("<table class=\"linktable\">(.*?)</table>").getMatch(0);
            if (linkTable == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String[] links = new Regex(linkTable, "href=\"(https?://[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links)
                decryptedLinks.add(createDownloadlink(singleLink));
            final FilePackage fp = FilePackage.getInstance();
            fp.setName("perseo.in folder - " + new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0));
            fp.addLinks(decryptedLinks);
        } else {
            if (br.getURL().contains("/error.php?e=")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("<title>Index of")) {
                logger.info("Link invalid: " + parameter);
                return decryptedLinks;
            }
            br.getHeaders().put("Referer", parameter);
            br.getPage("http://www.perseo.in/perseo.php");
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}