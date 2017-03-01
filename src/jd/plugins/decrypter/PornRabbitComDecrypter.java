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

//Mods: removed pornrabbit decrypt, added youporn.com decrypt
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornrabbit.com" }, urls = { "http://(www\\.)?pornrabbit\\.com/(\\d+/[a-z0-9_\\-]+\\.html|video/\\d+/)" })
public class PornRabbitComDecrypter extends PornEmbedParser {

    public PornRabbitComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // Link offline? Add it to the hosterplugin so the user can see it.
        if (br.containsHTML("(>Page Not Found<|>Sorry but the page you are looking for has|>Sorry, this video was not found|video_removed_dmca\\.jpg\")")) {
            decryptedLinks.add(createOfflinelink(parameter, "Offline Content"));
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<>\"]*?): Porn Rabbit</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        decryptedLinks = new ArrayList<DownloadLink>();
        decryptedLinks.add(createDownloadlink(parameter.replace("pornrabbit.com/", "pornrabbitdecrypted.com/")));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}