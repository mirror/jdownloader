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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tinyurl.com" }, urls = { "https?://(?:www\\.)?tinyurl\\.com/[a-z0-9]+(?:/[^/]+){0,}" })
public class TinyurlCom extends PluginForDecrypt {
    public TinyurlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceFirst("preview\\.tinyurl\\.com", "tinyurl\\.com");
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("tinyurl.com/errorb\\.php\\?") || br.containsHTML(">Error: TinyURL redirects to a TinyURL|>The URL you followed redirects back to a TinyURL") || br.containsHTML(">Error: Unable to find site\\'s URL to redirect to|>Please check that the URL entered is correct")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
