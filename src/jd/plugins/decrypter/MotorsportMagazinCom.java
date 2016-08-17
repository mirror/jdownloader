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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "motorsport-magazin.com" }, urls = { "https?://(?:www\\.)?motorsport\\-magazin\\.com/.+" }) 
public class MotorsportMagazinCom extends PluginForDecrypt {

    public MotorsportMagazinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Most times they simply embed (YouTube) videos. Note that visiting their swebsite without adblocker might crash your browser ... */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String externID = this.br.getRegex("var videoFile = \\'(//[^<>\"]*?)\\',").getMatch(0);
        if (externID == null) {
            externID = this.br.getRegex("value=\"(http[^<>\"]*?)\" name=\"video_file\"").getMatch(0);
        }
        if (externID != null) {
            if (!externID.startsWith("http")) {
                externID = "http" + externID;
            }
            decryptedLinks.add(createDownloadlink(externID));
        }

        return decryptedLinks;
    }

}
