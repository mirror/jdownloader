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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangavolume.com" }, urls = { "http://(www\\.)?mangavolume\\.com/[a-z0-9\\-]+/chapter-[a-z0-9\\-]+" }, flags = { 0 })
public class MangaVolumeCom extends PluginForDecrypt {

    public MangaVolumeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // Offline link
        if (br.containsHTML("name=\"title\" content=\"404\"|<title>404</title>") || br.containsHTML(">403 Forbidden<") || br.containsHTML("id=\"LicenseWarning\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Broken link
        if (br.containsHTML("><img src=\"http://media\\d+\\.mangavolume\\.com\" alt=\"CLICK TO VIEW NEXT PAGE")) {
            logger.info("Link broken (no images shown): " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("\">([^<>\"]*?)</a></h1>").getMatch(0);
        final String[] pages = br.getRegex(">Page #(\\d+)<").getColumn(0);
        final Regex info = br.getRegex("\"(http://(www\\.)?media\\d+\\.mangavolume\\.com/images/manga/normal/\\d+/\\d+/[a-z0-9]+_)(\\d+)(\\.[a-z]{3,6})\"");
        if (pages == null || pages.length == 0 || fpName == null || info.getMatches().length != 1) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());

        final String firstPart = info.getMatch(0);
        final String extension = info.getMatch(3);
        int internalCounter = Integer.parseInt(info.getMatch(2));
        final DecimalFormat df = new DecimalFormat("0000");

        for (int i = 1; i <= pages.length; i++) {
            final DownloadLink dl = createDownloadlink("directhttp://" + firstPart + internalCounter + extension);
            dl.setAvailable(true);
            dl.setFinalFileName(fpName + "_" + df.format(i) + extension);
            decryptedLinks.add(dl);
            internalCounter++;
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