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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "allmusicfind.com" }, urls = { "http://(www\\.)?allmusicfind\\.com/audio\\-id/[A-Za-z0-9]+" }, flags = { 0 })
public class AllMscFindCom extends PluginForDecrypt {

    public AllMscFindCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML(">File was removed from filehosting")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String goLink = br.getRegex("\"(/go/\\d+)\"").getMatch(0);
        if (goLink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        goLink = "http://www.allmusicfind.com" + goLink;
        final String captchaLink = br.getRegex("\"(/captcha/\\d+)\"").getMatch(0);
        if (captchaLink == null && br.containsHTML("class=\"captcha\"")) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (captchaLink != null) {
            for (int i = 0; i <= 3; i++) {
                final String code = getCaptchaCode("http://www.allmusicfind.com" + captchaLink, param);
                br.postPage(goLink, "file_id=" + new Regex(goLink, "http://www\\.allmusicfind\\.com/go/(\\d+)").getMatch(0) + "&captcha=" + Encoding.urlEncode(code));
                if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/audio-id/")) {
                    br.getPage(parameter);
                    continue;
                }
                break;
            }

        } else {
            br.getPage(goLink);
        }
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

}
