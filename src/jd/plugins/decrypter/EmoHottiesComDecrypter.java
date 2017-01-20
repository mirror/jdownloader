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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "emohotties.com" }, urls = { "http://(www\\.)?emohotties\\.com/videos/[a-z0-9\\-]+\\-\\d+\\.html" })
public class EmoHottiesComDecrypter extends PornEmbedParser {

    public EmoHottiesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        getPage(parameter);
        if ("http://www.badjojo.com/".equals(br.getRedirectLocation()) || br.getRequest().getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter, "Offline Content"));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        if (br.getRedirectLocation() != null) {
            getPage(br.getRedirectLocation());
        }
        String filename = jd.plugins.hoster.EmoHottiesCom.getTitle(this.br);
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }

        decryptedLinks = new ArrayList<DownloadLink>();
        final DownloadLink dl = createDownloadlink(parameter.replace("emohotties.com/", "decryptedemohotties.com/"));
        if (jd.plugins.hoster.EmoHottiesCom.isOffline(this.br)) {
            dl.setAvailable(false);
        }
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}