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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "faphub.xxx" }, urls = { "http://(www\\.)?faphub\\.xxx/video/\\d+" }) 
public class FaphubXxxDecrypter extends PornEmbedParser {

    public FaphubXxxDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String externID = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter, "Offline Content"));
            return decryptedLinks;
        }
        externID = this.br.getRedirectLocation();
        if (externID != null && !externID.contains("faphub.xxx/")) {
            decryptedLinks.add(this.createDownloadlink(externID));
            return decryptedLinks;
        } else if (externID != null) {
            this.br.getPage(externID);
            externID = null;
        }
        String filename = br.getRegex("<title>([^<>\"]*?) at faphub\\.xxx</title>").getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        decryptedLinks = new ArrayList<DownloadLink>();

        decryptedLinks.add(this.createDownloadlink(parameter.replace("faphub.xxx/", "faphubdecrypted.xxx/")));
        return decryptedLinks;
    }

}
