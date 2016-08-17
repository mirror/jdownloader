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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "animegalleries.net" }, urls = { "http://(?:www\\.)?animegalleries\\.net/album/\\d+" }) 
public class AnimegalleriesNet extends PluginForDecrypt {

    public AnimegalleriesNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("\">([^<>\"]+)</h2>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        String next = null;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        do {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            if (next != null) {
                this.br.getPage(next);
            }
            final String[] linkids = br.getRegex("\"/img/(\\d+)\"").getColumn(0);
            if (linkids == null || linkids.length == 0) {
                break;
            }
            for (String linkid : linkids) {
                final String singleLink = "http://www.animegalleries.net/img/" + linkid;
                final DownloadLink dl = createDownloadlink(singleLink);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
                dl.setName(linkid + ".jpg");
                decryptedLinks.add(dl);
                distribute(dl);
            }
            next = this.br.getRegex("class=\"tableb_compact\".*?class=\"navmenu\"><a href=\"(/album/\\d+/page/\\d+)\"").getMatch(0);
        } while (next != null);

        return decryptedLinks;
    }
}
