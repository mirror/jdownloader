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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hitomi.la" }, urls = { "https?://(www\\.)?hitomi\\.la/galleries/\\d+\\.html" }) 
public class HitomiLa extends antiDDoSForDecrypt {

    public HitomiLa(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Avoid https, prefer http */
        final String parameter = param.toString().replace("https://", "http://");
        final String gid = new Regex(parameter, "galleries/(\\d+)").getMatch(0);
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?) \\| Hitomi\\.la</title>").getMatch(0);
        final String[] links = br.getRegex("(/" + gid + "/[^<>\"]*?\\.[a-z]+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink("directhttp://http://g.hitomi.la/galleries" + singleLink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
