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

import java.net.UnknownHostException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "general-files.com" }, urls = { "http://(www\\.)?(general\\-files\\.com|generalfiles\\.org|generalfiles\\.me|general\\-files\\.org|generalfiles\\.biz|generalfiles\\.pw|general\\-file\\.com)/download/[a-z0-9]+/[^<>\"/]*?\\.html" }, flags = { 0 })
public class GeneralFilesCom extends PluginForDecrypt {

    public GeneralFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String currenthost = "general-file.com";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString().replaceAll("(general\\-files\\.com|generalfiles\\.org|generalfiles\\.me|general\\-files\\.org|general\\-files\\.biz|generalfiles\\.biz)/", currenthost + "/");
        try {
            br.getPage(parameter);
        } catch (final UnknownHostException e) {
            logger.info("Link offline (server error): " + parameter);
            return decryptedLinks;
        }

        if (br.containsHTML(">File was removed from filehosting<|>The file no longer exists at this location|class=\"gf\\-removed\\-h\"") || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getURL().equals("http://www." + currenthost + "/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Cannot decrypt link (server error): " + parameter);
            return decryptedLinks;
        }

        br.setFollowRedirects(false);
        String fpName = br.getRegex("<h4 class=\"file\\-header\\-2\">([^<>\"]*?)</h4>").getMatch(0);
        if (fpName == null)
            fpName = new Regex(parameter, "/download/[a-z0-9]+/([^<>\"/]*?)\\.html").getMatch(0);
        fpName = Encoding.htmlDecode(fpName.trim());
        final String goLink = br.getRegex("\\'(/go/\\d+)(\\?ajax=1)?\\'").getMatch(0);
        if (goLink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (br.containsHTML(">Please enter captcha and")) {
            if (!br.containsHTML("/captcha/")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (int i = 1; i <= 3; i++) {
                final String c = getCaptchaCode("http://www." + currenthost + "/captcha/" + new Regex(goLink, "(\\d+)$").getMatch(0), param);
                br.postPage(goLink, "captcha=" + Encoding.urlEncode(c));
                if (br.getRedirectLocation() != null && br.getRedirectLocation().matches("http://(www\\.)?" + currenthost + "/download/[a-z0-9]+/[^<>\"/]*?\\.html")) {
                    br.getPage(br.getRedirectLocation());
                    continue;
                } else if (br.containsHTML(">Please enter captcha and"))
                    continue;
                break;
            }
            if (br.containsHTML(">Please enter captcha and"))
                throw new DecrypterException(DecrypterException.CAPTCHA);
        } else {
            br.getPage("http://www." + currenthost + goLink + "?ajax=1");
        }
        /* First try ajax regex */
        String finallink = br.getRegex("\"link\":\"(http:[^<>\"]*?)\"").getMatch(0);
        if (finallink == null)
            finallink = br.getRegex("window\\.location\\.replace\\(\\'(http[^<>\"]*?)\\'\\)").getMatch(0);
        if (finallink == null)
            finallink = br.getRedirectLocation();
        if (finallink == null || finallink.contains(currenthost)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        finallink = finallink.replace("\\", "");
        final DownloadLink dl = createDownloadlink(finallink);
        decryptedLinks.add(dl);

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}