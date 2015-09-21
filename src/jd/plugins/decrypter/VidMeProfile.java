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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vid.me" }, urls = { "https?://(?:www\\.)?vid\\.me/[^/]+" }, flags = { 0 })
public class VidMeProfile extends PluginForDecrypt {

    public VidMeProfile(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = parameter.substring(parameter.lastIndexOf("/") + 1);
        boolean failed = false;
        try {
            br.getPage(parameter);
        } catch (final Throwable e) {
            failed = true;
        }
        if (failed || (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (this.br.containsHTML("property=\"og:video\"")) {
            /* Single video */
            decryptedLinks.add(this.createDownloadlink("https://viddecrypted.me/" + fid));
        } else {
            /* Find all videos of a user profile */
            final String fpName = fid;
            final String[] links = br.getRegex("data\\-video\\-url=\"https?://(?:www\\.)?vid\\.me/([A-Za-z0-9]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                /* Probably offline */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            for (final String fuid : links) {
                decryptedLinks.add(this.createDownloadlink("https://viddecrypted.me/" + fuid));
            }

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
