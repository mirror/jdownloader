//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

/**
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "audiocastle.net" }, urls = { "https?://(www\\.)?(?:audiocastle\\.(?:net|co|me|is)|audiocastle2\\.biz)/(tracks|albums|mixtapes|immortals|videos)/view/\\d+" })
public class AudCtlNt extends antiDDoSForDecrypt {

    public AudCtlNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceFirst("://(?:www\\.)?[^/]+/", "://www.audiocastle2.biz/");
        br.setFollowRedirects(true);
        getPage(parameter);
        String dl = null;
        if (dl == null && parameter.contains("/videos/")) {
            dl = br.getRegex("<source[^>]+src=\"([^\"]+)\"").getMatch(0);
        } else {
            dl = br.getRegex("<a href=\"([^\"]+)\"[^>]+>Download \\w+</a>").getMatch(0);
        }

        if (dl != null) {
            decryptedLinks.add(createDownloadlink(dl));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}