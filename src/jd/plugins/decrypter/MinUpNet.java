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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "minup.net" }, urls = { "http://(www\\.)?minup\\.net/(?!about|api|contact|register|user|index|multiple|remote|login|forgot\\-password|contact|terms|dmca)[A-Za-z0-9]+" }, flags = { 0 })
public class MinUpNet extends PluginForDecrypt {

    public MinUpNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML(">No files found\\!<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[] links = br.getRegex("\"(http://minup.net/redirect/[A-Za-z0-9]+/\\d+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (br.containsHTML("for=\"direct_link\">Direct Link :</label>")) {
            try {
                br.getPage("http://minup.net/dfile.php?createlink=" + new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0));
                final String finallink = br.getRedirectLocation();
                if (finallink != null) decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            } catch (final Exception e) {
                // Prevent the decrypter from failing here
            }
        }
        for (final String singleLink : links) {
            br.getPage(singleLink);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

}
