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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { ".com" }, urls = { "https?://(?:www\\.)?alphatv\\.gr/shows/.+" })
public class AlphatvGr extends PluginForDecrypt {

    public AlphatvGr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final DownloadLink main = this.createDownloadlink(parameter.replace("alphatv.gr/", "alphatvdecrypted.gr/"));
        jd.plugins.hoster.AlphatvGr.prepBR(this.br);
        br.getPage(parameter);
        /* First check for embedded content e.g. YouTube. */
        final String externID = this.br.getRegex("<embed\\s*?src=\"(http[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        if (jd.plugins.hoster.AlphatvGr.isOffline(this.br)) {
            main.setAvailable(false);
        }
        decryptedLinks.add(main);

        return decryptedLinks;
    }

}
