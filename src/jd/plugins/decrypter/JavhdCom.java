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

import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "javhd.com" }, urls = { "https?://(?:www\\.)?javhd\\.com/[a-z]{2}/id/(\\d+)/([a-z0-9\\-]+)" })
public class JavhdCom extends PluginForDecrypt {
    public JavhdCom(PluginWrapper wrapper) {
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
        String fpName = br.getRegex("var item_title = \"([^\"]+)\"").getMatch(0);
        final String[] links = br.getRegex("<img alt=\"[^\"]+\"\\s*?src=\"(https[^<>\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink(singleLink);
            String filename = fpName;
            final String name_url = getFileNameFromURL(new URL(singleLink));
            if (name_url != null) {
                filename += "_" + name_url;
            } else {
                /* Assume fileextension */
                filename += ".jpg";
            }
            dl.setAvailable(true);
            dl.setFinalFileName(filename);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        /* Add main URL for hosterplugin to download video */
        decryptedLinks.add(this.createDownloadlink(parameter));
        return decryptedLinks;
    }
}
