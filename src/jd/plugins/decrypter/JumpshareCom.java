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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jumpshare.com" }, urls = { "https?://(?:www\\.)?(?:jmp\\.sh/(?!v/)[A-Za-z0-9]+|jumpshare\\.com/b/[A-Za-z0-9]+)" })
public class JumpshareCom extends PluginForDecrypt {

    public JumpshareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_FOLDER = "https?://(?:www\\.)?jumpshare\\.com/b/[A-Za-z0-9]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        if (parameter.matches(TYPE_FOLDER)) {
            /* TODO! */
            return null;
            // String fpName = br.getRegex("").getMatch(0);
            // final String[] links = br.getRegex("").getColumn(0);
            // if (links == null || links.length == 0) {
            // logger.warning("Decrypter broken for link: " + parameter);
            // return null;
            // }
            // for (final String singleLink : links) {
            // decryptedLinks.add(createDownloadlink(singleLink));
            // }
            //
            // if (fpName != null) {
            // final FilePackage fp = FilePackage.getInstance();
            // fp.setName(Encoding.htmlDecode(fpName.trim()));
            // fp.addLinks(decryptedLinks);
            // }
        } else {
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String finallink = this.br.getRedirectLocation();
            if (finallink == null) {
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }

}
