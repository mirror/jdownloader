//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gogoanime.com", "goodanime.net", "gooddrama.net" }, urls = { "http://(www\\.)?gogoanime\\.com/(?!category|thumbs|sitemap|img|xmlrpc|fav|images|ads)[a-z0-9\\-_]+(/\\d+)?", "http://(www\\.)?goodanime\\.net/(?!category|thumbs|sitemap|img|xmlrpc|fav|images|ads)[a-z0-9\\-_]+(/\\d+)?", "http://(www\\.)?gooddrama\\.net/(?!category|thumbs|sitemap|img|xmlrpc|fav|images|ads)([a-z0-9\\-_]+/?){2}([\\d\\-]+)?" }, flags = { 0, 0, 0 })
public class GogoanimeCom extends PluginForDecrypt {

    public GogoanimeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final Exception e) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        // Offline
        if (br.containsHTML("Oops\\! Page Not Found<")) {
            logger.info("This link is offline: " + parameter);
            return decryptedLinks;
        }
        // Offline2
        if (br.containsHTML(">404 Not Found<")) {
            logger.info("This link is offline: " + parameter);
            return decryptedLinks;
        }
        // Invalid link
        if (br.containsHTML("No htmlCode read")) {
            logger.info("This link is invalid: " + parameter);
            return decryptedLinks;
        }

        String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>([^<>\"]*?)( \\w+ Sub.*?)?</title>").getMatch(0);

        final String[] links = br.getRegex("<iframe.*?src=(\"|\\')(http[^<>\"]+)(\"|\\')").getColumn(1);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            // lets prevent returning of links which contain itself.
            if (!singleLink.matches(".+(" + this.getHost().replace(".", "\\.") + "|imgur\\.com).+")) {
                final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(singleLink));
                if (dl != null) decryptedLinks.add(dl);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
            fp.setProperty("ALLOW_MERGE", true);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}