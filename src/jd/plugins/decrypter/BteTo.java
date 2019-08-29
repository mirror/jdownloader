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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "byte.to" }, urls = { "https?://(?:www\\.)?byte\\.to/(?:category/[A-Za-z0-9\\-]+/[A-Za-z0-9\\-]+\\.html|\\?id=\\d+)|https?://byte\\.to/go\\.php\\?hash=.+" })
public class BteTo extends PluginForDecrypt {
    public BteTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This is a modified CMS site
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(".+go\\.php.+")) {
            /* 2019-08-29: Single redirect URLs */
            br.setFollowRedirects(false);
            br.getPage(parameter);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                return null;
            }
            decryptedLinks.add(this.createDownloadlink(finallink));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Es existiert kein Eintrag mit der ID")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>Byte\\.to \\-([^<>\"]*?)</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<TITLE>\\s*(.*?)</TITLE>").getMatch(0);
        }
        final String[] links = br.getRegex("<A HREF=\"(https?[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links) {
            singleLink = singleLink.replace("http://www.dereferer.org/?", "");
            singleLink = Encoding.htmlDecode(singleLink);
            if (!singleLink.matches("https?://(www\\.)?byte\\.to/category/[A-Za-z0-9\\-]+/[A-Za-z0-9\\-]+\\.html")) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
