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

//EmbedDecrypter 0.1.9
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tuberealm.com" }, urls = { "http://(www\\.)?tuberealm\\.com/tube\\-sex\\-movies/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class TubeRealmCom extends PluginForDecrypt {

    public TubeRealmCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String externID = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("Sorry, this page was deleted")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        externID = br.getRedirectLocation();
        if (externID != null && !externID.contains("tuberealm.com/")) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        decryptedLinks = jd.plugins.decrypter.PornEmbedParser.findEmbedUrls(this.br, filename);
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            return null;
        }
        return decryptedLinks;
    }

}
