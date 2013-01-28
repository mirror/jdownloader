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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediahide.com" }, urls = { "http://(www\\.)?mediahide\\.com/(\\?[A-Za-z0-9]+|paste/\\?p=\\d+|paste/[A-Za-z0-9\\-]+)" }, flags = { 0 })
public class MediaHideCom extends PluginForDecrypt {

    public MediaHideCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public int getMaxConcurrentProcessingInstances() {
        return 2;
    }

    private static final String PASSCODETEXT = ">This post is password protected";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches("http://(www\\.)?mediahide\\.com/\\?[A-Za-z0-9]+")) {
            br.setFollowRedirects(false);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://mediahide.com/get.php?do=getlink", "url=" + new Regex(parameter, "mediahide\\.com/\\?(.+)").getMatch(0) + "&pass=");
            String finallink = br.getRegex("req\":\"(http:.*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            finallink = finallink.replace("\\", "");
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.containsHTML(PASSCODETEXT)) {
                for (int i = 0; i <= 3; i++) {
                    String passCode = getUserInput("Enter password for: " + parameter, param);
                    br.postPage("http://mediahide.com/paste/wp-pass.php", "post_password=" + Encoding.urlEncode(passCode) + "&Submit=Submit");
                    if (br.containsHTML(PASSCODETEXT)) continue;
                    break;
                }
                if (br.containsHTML(PASSCODETEXT)) throw new DecrypterException(DecrypterException.PASSWORD);
            }
            String fpName = br.getRegex("<h2>Protected: ([^<>\"]*?)</h2>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("<title>([^<>\"]*?) \\- MediaHide Paste[\t\n\r ]+</title>").getMatch(0);
            String[] links = HTMLParser.getHttpLinks(br.toString(), "");
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links) {
                if (singleLink.matches("http://(www\\.)?mediahide\\.com/paste/[A-Za-z0-9\\-]+(/.*)?")) continue;
                if (singleLink.matches("http://(www\\.)?mediahide\\.com/paste/\\?p=\\d+(.*)?")) continue;
                if (singleLink.matches("http://(www\\.)?mediahide\\.com/\\?[A-Za-z0-9]+(.*)?")) continue;
                decryptedLinks.add(createDownloadlink(singleLink));
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

}
