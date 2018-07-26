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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ehow.com" }, urls = { "https?://(?:www\\.)?ehow\\.com/video_\\d+_.*?\\.html" })
public class EhowComDecrypter extends PluginForDecrypt {
    public EhowComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:");
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getRedirectLocation().contains("ehow.com/video")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String externID = this.br.getRegex("videoId[\n\t\r ]*?:[\n\t\r ]*?\"([^\"]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(this.createDownloadlink("https://www.youtube.com/watch?v=" + externID));
            return decryptedLinks;
        }
        decryptedLinks.add(this.createDownloadlink(parameter.replace("ehow.com/", "ehowdecrypted.com/")));
        return decryptedLinks;
    }
}
