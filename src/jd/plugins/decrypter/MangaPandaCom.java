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

import java.text.DecimalFormat;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangapanda.com" }, urls = { "https?://(www\\.)?mangapanda\\.com/([a-z0-9\\-]+/\\d+|\\d+\\-\\d+\\-1/[a-z0-9\\-]+/chapter\\-\\d+\\.html)" })
public class MangaPandaCom extends PluginForDecrypt {
    public MangaPandaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // Nearly the same as mangareader.net
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        // website issue not JD.
        if (br.getRequest().getContentLength() == 0) {
            return decryptedLinks;
        }
        if (br.containsHTML("is not released yet")) {
            logger.info("No downloadable content available for link: " + parameter);
            return decryptedLinks;
        }
        final String maxPage = br.getRegex("</select>\\s*of\\s*(\\d+)</div>").getMatch(0);
        String fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null || maxPage == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final Regex splitLink = new Regex(parameter, "(https?://(www\\.)?.*?/\\d+\\-\\d+\\-)1(/.+)");
        final String part1 = splitLink.getMatch(0);
        final String part2 = splitLink.getMatch(2);
        fpName = Encoding.htmlDecode(fpName.trim());
        final DecimalFormat df = new DecimalFormat("0000");
        for (int i = 1; i <= Integer.parseInt(maxPage); i++) {
            if (i > 1) {
                if (parameter.matches("https?://(www\\.)?.*?/\\d+\\-\\d+\\-1/[a-z0-9\\-]+/chapter\\-\\d+\\.html")) {
                    br.getPage(part1 + i + part2);
                } else {
                    br.getPage(parameter + "/" + i);
                }
            }
            final String finallink = br.getRegex("\"(https://i\\d+\\.(p\\.)?(mangapanda\\.com|mangacdn\\.com)/[a-z0-9\\-]+/\\d+/[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(fpName + "_" + df.format(i) + finallink.substring(finallink.lastIndexOf(".")));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}