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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "accuratefiles.com" }, urls = { "http://(www\\.)?accuratefiles\\.com/fileinfo/[a-z0-9]+" }, flags = { 0 })
public class AccurateFilesCom extends PluginForDecrypt {

    public AccurateFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String currenthost = "accuratefiles.com";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">File was removed from filehosting")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        final String goLink = br.getRegex("\"(/go/\\d+)\"").getMatch(0);
        if (goLink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (br.containsHTML("\"/captcha/")) {
            for (int i = 1; i <= 3; i++) {
                final String c = getCaptchaCode("http://www." + currenthost + "/captcha/" + new Regex(goLink, "(\\d+)$").getMatch(0), param);
                br.postPage(goLink, "captcha=" + Encoding.urlEncode(c));
                if (br.getRedirectLocation() != null && br.getRedirectLocation().matches("http://(www\\.)?accuratefiles\\.com/fileinfo/[a-z0-9]+")) {
                    br.getPage(br.getRedirectLocation());
                    continue;
                } else if (br.containsHTML("\"/captcha/")) continue;
                break;
            }
            if (br.containsHTML("\"/captcha/")) throw new DecrypterException(DecrypterException.CAPTCHA);
        } else {
            br.getPage("http://www." + currenthost + goLink);
        }
        String finallink = br.getRegex("window\\.location\\.replace\\(\\'(http[^<>\"]*?)\\'\\)").getMatch(0);
        if (finallink == null) finallink = br.getRedirectLocation();
        if (finallink == null || finallink.contains(currenthost)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
