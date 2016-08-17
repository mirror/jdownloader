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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bs.to" }, urls = { "http://(www\\.)?bs\\.to/serie/[^/]+/\\d+/[^/]+(/[^/]+)?" }) 
public class BsTo extends PluginForDecrypt {

    public BsTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE = "http://(www\\.)?bs\\.to/serie/[^/]+/\\d+/[^/]+/[^/]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        final String urlpart = new Regex(parameter, "(serie/.+)").getMatch(0);
        if (parameter.matches(TYPE_SINGLE)) {
            final String finallink = br.getRegex("\"(http[^<>\"]*?)\" target=\"_blank\"><span class=\"icon link_go\"").getMatch(0);
            if (finallink == null) {
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            String fpName = null;
            final String[] links = br.getRegex("class=\"v\\-centered icon [^<>\"]+\"[\t\n\r ]+href=\"(" + urlpart + "/[^/]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink("http://bs.to/" + singleLink));
            }

            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

}
