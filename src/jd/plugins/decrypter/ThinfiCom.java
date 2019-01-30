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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "thinfi.com" }, urls = { "https?://(?:www\\.)?thinfi\\.com/[a-z0-9]+" })
public class ThinfiCom extends PluginForDecrypt {
    public ThinfiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().endsWith("/404")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        String finallink = null;
        if (br.containsHTML("name=\"password\"")) {
            /* Password protected */
            boolean success = false;
            for (int i = 0; i <= 2; i++) {
                final String passCode = getUserInput("Password?", param);
                br.postPage(br.getURL(), "submit=submit&password=" + Encoding.urlEncode(passCode));
                finallink = br.getRedirectLocation();
                if (!br.containsHTML("name=\"password\"") || finallink != null && new Regex(finallink, this.getSupportedLinks()).matches()) {
                    success = true;
                    break;
                }
            }
            if (!success) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        } else {
            finallink = this.br.getRegex("<META HTTP\\-EQUIV=\"Refresh\" CONTENT=\"\\d+;URL=(https[^\"]+)\">").getMatch(0);
        }
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
