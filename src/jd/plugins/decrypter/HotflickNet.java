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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hotflick.net" }, urls = { "https?://(?:www\\.)?hotflick\\.net/u/g/(index\\.php)?\\?d=\\d+" })
public class HotflickNet extends PluginForDecrypt {
    public HotflickNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String lid = new Regex(parameter, "d=(\\d+)$").getMatch(0);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("window\\.location =")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        int next = -1;
        while (!isAbort()) {
            final String[] links = br.getRegex("\"https?://(?:www\\.)?hotflick\\.net/u/v/\\?q=\\d+\\.([^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                break;
            }
            for (String singleLink : links) {
                singleLink = "directhttp://http://www.hotflick.net/u/n/" + lid + "/" + singleLink;
                final DownloadLink dl = createDownloadlink(singleLink);
                decryptedLinks.add(dl);
            }
            final String nextPage = br.getRegex("NEXT<br\\s*/>\\s*PAGE\\s*(\\d+)").getMatch(0);
            if (nextPage == null || Integer.parseInt(nextPage) < next) {
                break;
            } else {
                next = Integer.parseInt(nextPage);
                br.getPage("/u/g/index.php?d=" + lid + "&p=" + next + ".page");
            }
        }
        if (decryptedLinks.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
