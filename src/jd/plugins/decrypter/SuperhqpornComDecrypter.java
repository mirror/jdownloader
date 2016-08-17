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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "superhqporn.com" }, urls = { "http://www\\.superhqporn\\.com/\\?v=[A-Z0-9]+" }) 
public class SuperhqpornComDecrypter extends PornEmbedParser {

    public SuperhqpornComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String externID = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final DownloadLink main = createDownloadlink(parameter.replace("superhqporn.com/", "superhqporndecrypted.com/"));
        main.setContentUrl(parameter);

        if (br.containsHTML(">Video Not Exists<|>Requested video not exist") || br.getHttpConnection().getResponseCode() == 404) {
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        externID = br.getRedirectLocation();
        if (externID != null && !externID.contains("superhqporn.com/")) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        } else if (externID != null) {
            br.getPage(externID);
            externID = null;
        }
        decryptedLinks.addAll(findEmbedUrls(null));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        /* No extern id found --> Add main-link */
        decryptedLinks.add(main);
        return decryptedLinks;
    }

}
