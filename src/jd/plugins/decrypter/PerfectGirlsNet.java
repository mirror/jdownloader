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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "perfectgirls.net" }, urls = { "http://([a-z]+\\.)?(perfectgirls\\.net/\\d+/|(www|ipad|m)\\.perfectgirls\\.net/gal/\\d+/.{0,1})" })
public class PerfectGirlsNet extends PornEmbedParser {
    public PerfectGirlsNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceAll("(ipad|m)\\.perfectgirls\\.net/", "perfectgirls.net/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("No htmlCode read")) {
            decryptedLinks.add(createOfflinelink(parameter, "Offline Content"));
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<>\"]*?) ::: PERFECT GIRLS</title>").getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        final DownloadLink main = createDownloadlink(parameter.replace("perfectgirls.net/", "perfectgirlsdecrypted.net/"));
        if (br.containsHTML("src=\"http://(www\\.)?dachix\\.com/flashplayer/flvplayer\\.swf\"|\"http://(www\\.)?deviantclip\\.com/flashplayer/flvplayer\\.swf\"|thumbs/misc/not_available\\.gif")) {
            main.setAvailable(false);
            main.setProperty("offline", true);
        } else {
            main.setAvailable(true);
            main.setName(filename + ".mp4");
        }
        decryptedLinks.add(main);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}