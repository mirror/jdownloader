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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "telly.com" }, urls = { "http://(www\\.)?(telly|twitvid)\\.com/(?!awesome|post|best|funnyimpressions|http|https|index|javascript|redirect)[A-Za-z0-9\\-]+" })
public class TwitVidComDecrypter extends PluginForDecrypt {

    public TwitVidComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Decrypts embedded videos, if no embedded video is found the link gets passed over to the hosterplugin!
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("twitvid.com/", "telly.com/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">No videos yet") || br.getURL().contains("telly.com/?s=trending&err=1")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String externID = br.getRegex("property=\"og:image\" content=\"https?://i(\\d+)?\\.ytimg\\.com/vi/([^<>\"/]*?)/hqdefault").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.youtube.com/watch?v=" + externID));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}