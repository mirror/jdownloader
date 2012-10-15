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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mcanime.net" }, urls = { "http://(www\\.)?[a-z0-9]+\\.mcanime\\.net/manga_enlinea/[a-z0-9\\-_]+/realidad/\\d+/1" }, flags = { 0 })
public class McAnimeNet extends PluginForDecrypt {

    public McAnimeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Oops\\! \\- El archivo específicado no existe")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<div class=\"manga\\-title\">([^<>\"]*?)<span").getMatch(0);
        final String picNum = br.getRegex("</select>[\t\n\r ]+de (\\d+)\\&nbsp;").getMatch(0);
        if (picNum == null || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());

        final String linkPart = new Regex(parameter, "(http://(www\\.)?[a-z0-9]+\\.mcanime\\.net/manga_enlinea/[a-z0-9\\-_]+/realidad/\\d+/)1").getMatch(0);
        final DecimalFormat df = new DecimalFormat("0000");
        for (int i = 1; i <= Integer.parseInt(picNum); i++) {
            if (i > 1) br.getPage(linkPart + i);
            final String finallink = br.getRegex("\"(http://manga\\.mcanime\\.net/manga/\\d+/\\d+/\\d+/" + (i - 1) + "\\.(jpg|png))\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink(finallink);
            // In the links they have different extensions but .jpg is always
            // the real extension (checked)
            dl.setFinalFileName(fpName + "_" + df.format(i) + ".jpg");
            dl.setAvailable(true);
            try {
                distribute(dl);
            } catch (final Exception e) {
                // Not available in 0.9.851 Stable
            }
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
