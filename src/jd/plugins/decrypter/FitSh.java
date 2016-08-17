//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * HAHAHAHA
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 30210 $", interfaceVersion = 2, names = { "fit.sh" }, urls = { "https?://(?:www\\.)?fit\\.sh/[a-zA-Z0-9]{3,}" }) 
@SuppressWarnings("deprecation")
public class FitSh extends PluginForDecrypt {

    public FitSh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br = new Browser();
        br.getPage(parameter);
        if ("fitshr.com".equalsIgnoreCase(br.getRedirectLocation())) {
            return decryptedLinks;
        }
        // this is base64
        final String token = br.getRegex("token\\s*=\\s*\"([a-zA-Z0-9_/\\+\\=\\-]+)\"").getMatch(0);
        if (token == null) {
            logger.info("Could not find token");
            return null;
        }
        // for our interest; DecToken: af5e25be1f3aef75f8953c3da05a2d36|1435056473|1Hey == md5|timestamp of request?|uid
        // final String[] infos = Encoding.Base64Decode(token).split("\\|");

        // waits required
        sleep(10000, param);
        br.getHeaders().put("Pragma", null);
        // fa is static.
        br.getPage("http://fit.sh/go/" + token + "?fa=15466&a=");
        final String redir = br.getRegex("document\\.location\\s*=\\s*(\"|')((?:ftp|https?)://.*?)\\1").getMatch(1);
        if (redir == null) {
            logger.warning("Decrypter broken for link (no redirect): " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(redir));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}