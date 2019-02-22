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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ffetish.photos" }, urls = { "https?://(?:www\\.)?ffetish\\.photos/\\d+[a-z0-9\\-]+\\.html" })
public class FfetishPhotos extends antiDDoSForDecrypt {
    public FfetishPhotos(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String dl_id = new Regex(parameter, "photos/(\\d+)").getMatch(0);
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("engine/modules/antibot/antibot\\.php")) {
            boolean success = false;
            br.getHeaders().put("x-requested-with", "XMLHttpRequest");
            for (int i = 0; i <= 3; i++) {
                final String code = this.getCaptchaCode("https://" + this.getHost() + "/engine/modules/antibot/antibot.php?rndval=" + System.currentTimeMillis(), param);
                postPage("https://" + this.getHost() + "/engine/ajax/getlink.php", "sec_code=" + Encoding.urlEncode(code) + "&id=" + dl_id + "&skin=ffphotos");
                if (br.toString().length() > 100) {
                    success = true;
                    break;
                }
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        String finallink = this.br.getRegex("(https?://ffetish\\.photos/video/[^\"\\']+)").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.setFollowRedirects(false);
        getPage(finallink);
        finallink = br.getRedirectLocation();
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
