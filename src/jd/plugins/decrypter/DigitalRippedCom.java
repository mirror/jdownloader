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
import jd.http.Browser.BrowserException;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "digitalripped.com" }, urls = { "http://(www\\.)?digitaldripped\\.com/[^<>\"\\']{2,}" }, flags = { 0 })
public class DigitalRippedCom extends PluginForDecrypt {

    public DigitalRippedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?digitaldripped\\.com/(ads\\d+.+|disclaimer|terms|contact|advertise|ajax.+|js.+|\\?p=.+|archives|lilb\\.htm)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            logger.info("Cannot decrypt link (server error or connection problems): " + parameter);
            return decryptedLinks;
        } catch (final IllegalStateException e) {
            logger.info("Cannot decrypt link (unsupported link): " + parameter);
            return decryptedLinks;
        }
        if (br.getRequest().getHttpConnection().getContentType().contains("text/plain")) {
            final String[] links = HTMLParser.getHttpLinks(br.toString(), "");
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links)
                if (!singleLink.contains("digitalripped.com/")) decryptedLinks.add(createDownloadlink(singleLink));
        } else {
            String finallink = null;
            if (br.containsHTML("http\\-equiv=\"refresh\">") || br.containsHTML(">404 Not Found<") || parameter.matches(INVALIDLINKS)) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            } else {
                finallink = br.getRegex("<a class=\"download\\-btn\" target=\"[^<>\"\\']+\" href=\"(http://[^<>\"\\']+)\"").getMatch(0);
                if (finallink == null) finallink = br.getRegex("<source src=\"(http[^<>\"]*?)\"").getMatch(0);
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
