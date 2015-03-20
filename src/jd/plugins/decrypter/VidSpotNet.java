//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

/**
 * decrypter to find xfs links on there front end.
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 29542 $", interfaceVersion = 2, names = { "vidspot.net" }, urls = { "https?://(?:www\\.)?vidspot\\.net/\\d+/v-[A-Za-z0-9]{6}" }, flags = { 0 })
public class VidSpotNet extends PluginForDecrypt {

    public VidSpotNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        // find the link we need
        final String link = br.getRegex("https?://(www\\.)?vidspot\\.net/((vid)?embed-|builtin-)?[a-z0-9]{12}").getMatch(-1);
        if (link != null) {
            decryptedLinks.add(createDownloadlink(link));
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}