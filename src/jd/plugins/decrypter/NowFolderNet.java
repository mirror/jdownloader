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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nowfolder.net" }, urls = { "http://(www\\.)?nowfolder\\.net/[a-z0-9]+/" }, flags = { 0 })
public class NowFolderNet extends PluginForDecrypt {

    public NowFolderNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final String lid = new Regex(parameter, "([a-z0-9]+)/$").getMatch(0);
        final String finallink = br.getRedirectLocation();
        if (finallink != null) {
            if (finallink.contains("/index.php")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            } else if (finallink.contains(lid)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<table style=\"margin: 0 auto; text\\-align: left;\">[\t\n\r ]+<strong>([^<>\"]*?)</strong>").getMatch(0);
        final String[] links = br.getRegex("\"(http://nowfolder\\.net/[a-z0-9]+/)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (!singleLink.contains(lid)) {
                br.getPage(singleLink);
                final String finalink = br.getRedirectLocation();
                if (finalink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink(singleLink);
                try {
                    distribute(dl);
                } catch (final Exception e) {
                    // Not available in old 0.9.581 Stable
                }
                decryptedLinks.add(dl);
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
