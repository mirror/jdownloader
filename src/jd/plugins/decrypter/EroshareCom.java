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
import java.util.LinkedHashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eroshare.com" }, urls = { "https?://(?:www\\.)?eroshare\\.com/[a-z0-9]{8}" })
public class EroshareCom extends PluginForDecrypt {

    public EroshareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = parameter.substring(parameter.lastIndexOf("/") + 1);
        /* 2017-04-21 */
        final String postbody = br.getRegex("(<div class=\"item-list\">.*?)<\\!\\-\\-item\\-list\\-\\->").getMatch(0);
        if (postbody == null) {
            return null;
        }
        final String[] links = new Regex(postbody, "(//(?:i|v)\\.eroshare\\.com/[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        for (final String singleLink : links) {
            if (singleLink.contains("_thumb")) {
                continue;
            }
            final String link = Request.getLocation(singleLink, br.getRequest());
            if (!dupe.add(link)) {
                continue;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + link);
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
