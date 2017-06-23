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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1fichier.com" }, urls = { "https?://(www\\.)?1fichier\\.com/((en|cn)/)?dir/[A-Za-z0-9]+" })
public class OneFichierComFolder extends PluginForDecrypt {

    public OneFichierComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = "https://1fichier.com/dir/" + new Regex(param.toString(), "([A-Za-z0-9]+)$").getMatch(0);
        prepareBrowser(br);
        br.setLoadLimit(Integer.MAX_VALUE);
        br.getPage(parameter + "?e=1");
        if (br.toString().equals("bad") || br.containsHTML("No htmlCode read")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        handlePassword(param, parameter);
        final String fpName = br.getRegex(">Shared folder (.*?)</").getMatch(0);
        final String[][] linkInfo = getLinkInfo();
        if (linkInfo == null || linkInfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLinkInfo[] : linkInfo) {
            final DownloadLink dl = createDownloadlink(singleLinkInfo[1]);
            dl.setFinalFileName(Encoding.htmlDecode(singleLinkInfo[2]));
            dl.setDownloadSize(SizeFormatter.getSize(singleLinkInfo[3]));
            if (passCode != null) {
                dl.setDownloadPassword(passCode);
            }
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private final String[][] getLinkInfo() {
        // some reason the e=1 reference now spews html not deliminated results.
        // final String[][] linkInfo = br.getRegex("(https?://[a-z0-9\\-]+\\..*?);([^;]+);([0-9]+)").getMatches();
        final String[][] linkInfo = br.getRegex("<a href=(\"|')(" + JDUtilities.getPluginForHost("1fichier.com").getSupportedLinks() + ")\\1[^>]*>([^\r\n\t]+)</a>\\s*</td>\\s*<td[^>]*>([^\r\n\t]+)</td>").getMatches();
        return linkInfo;
    }

    private String passCode = null;

    private final String handlePassword(final CryptedLink param, final String parameter) throws Exception {
        if (br.containsHTML("password")) {
            if (passCode == null) {
                passCode = param.getDecrypterPassword();
            }
            final int repeat = 3;
            for (int i = 0; i <= repeat; i++) {
                if (passCode == null) {
                    passCode = getUserInput(null, param);
                }
                br.postPage(parameter + "?e=1", "pass=" + Encoding.urlEncode(passCode));
                if (br.containsHTML("password")) {
                    if (i + 1 >= repeat) {
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                    passCode = null;
                    continue;
                }
                return passCode;
            }
        }
        return null;
    }

    private void prepareBrowser(final Browser br) {
        if (br == null) {
            return;
        }
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.16) Gecko/20110323 Ubuntu/10.10 (maverick) Firefox/3.6.16");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Cache-Control", null);
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        // we want ENGLISH!
        br.setCookie(this.getHost(), "LG", "en");
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}