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

//EmbedDecrypter 0.1
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "twitvid.com" }, urls = { "http://(www\\.)?twitvid\\.com/(?!awesome|post|best|funnyimpressions|http|https|index|javascript|redirect)[A-Z0-9]+" }, flags = { 0 })
public class TwitVidComDecrypter extends PluginForDecrypt {

    public TwitVidComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Decrypts embedded videos, if no embedded video is found the link gets
     * passed over to the hosterplugin!
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String externID = br.getRegex("property=\"og:image\" content=\"http://i(\\d+)?\\.ytimg\\.com/vi/([^<>\"/]*?)/hqdefault").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.youtube.com/watch?v=" + externID));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("twitvid.com/", "twitviddecrypted.com/")));
        return decryptedLinks;
    }

}
