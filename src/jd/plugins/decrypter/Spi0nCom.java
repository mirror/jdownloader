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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spi0n.com" }, urls = { "http://www\\.spi0n\\.com/[a-z0-9\\-_]+" }, flags = { 0 })
public class Spi0nCom extends PluginForDecrypt {

    public Spi0nCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://www\\.spi0n\\.com/favicon";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (!br.containsHTML("id=\"container\"") || !br.containsHTML("class=\"headline\"")) {
            logger.info("Link offline (no video on this page?!): " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (br.containsHTML("Archives des catégorie:")) {
            logger.info("Link offline (invalid link!): " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String finallink = br.getRegex("\"(http://(www\\.)?dailymotion\\.com/video/[A-Za-z0-9\\-_]+)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("\"((http:)?//(www\\.)?youtube\\.com/embed/[^<>\"/]+)\"").getMatch(0);
            if (finallink != null && !finallink.startsWith("http:")) {
                finallink = "http:" + finallink;
            }
        }
        /* Sometimes they host videos on their own servers */
        if (finallink == null) {
            finallink = br.getRegex("\"file\":\"(http://(www\\.)?spi0n\\.com/wp\\-content/uploads[^<>\"]*?)\"").getMatch(0);
            if (finallink != null) {
                finallink = "directhttp://" + finallink.replace("http://www.", "http://");
            }
        }
        /* Maybe its a picture gallery */
        if (finallink == null) {
            final String fpName = br.getRegex("class=\"headline\">([^<>\"]*?)<").getMatch(0);
            final String[] pictures = br.getRegex("size\\-(large|full) wp\\-image\\-\\d+\" alt=\"[^<>\"/]+\" src=\"(http://(www\\.)?spi0n\\.com/wp\\-content/uploads/[^<>\"]*?)\"").getColumn(1);
            if (fpName == null || pictures == null || pictures.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String pic : pictures) {
                final DownloadLink fina = createDownloadlink("directhttp://" + pic);
                fina.setAvailable(true);
                decryptedLinks.add(fina);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}