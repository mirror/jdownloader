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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangaeden.com" }, urls = { "http://(www\\.)?mangaeden\\.com/[a-z0-9\\-]+/[a-z0-9\\-]+/\\d+/1/" }, flags = { 0 })
public class MangaEdenCom extends PluginForDecrypt {

    public MangaEdenCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> cryptedLinks = new ArrayList<String>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("404 NOT FOUND")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String thisLinkpart = new Regex(parameter, "mangaeden\\.com(/.*?)1/$").getMatch(0);
        String fpName = br.getRegex("<title>([^<>\"]*?) \\- [\t\n\r ]+Manga Eden").getMatch(0);
        final String[] pages = br.getRegex("class=\"ui\\-state\\-default\" href=\"(" + thisLinkpart + "\\d+/)\"").getColumn(0);
        if (pages == null || pages.length == 0 || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());

        cryptedLinks.add(parameter);
        for (final String currentPage : pages) {
            if (!cryptedLinks.contains(currentPage)) cryptedLinks.add(currentPage);
        }

        // decrypt all pages
        final DecimalFormat df = new DecimalFormat("0000");
        int counter = 1;
        for (final String currentPage : cryptedLinks) {
            if (!currentPage.equals(parameter)) br.getPage("http://www.mangaeden.com/" + currentPage);
            final String decryptedlink = getSingleLink();
            final DownloadLink dd = createDownloadlink("directhttp://" + decryptedlink);
            dd.setAvailable(true);
            dd.setFinalFileName(fpName + "_" + df.format(counter) + decryptedlink.substring(decryptedlink.lastIndexOf(".")));
            decryptedLinks.add(dd);
            counter++;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getSingleLink() {
        String finallink = br.getRegex("id=\"mainImg\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) finallink = br.getRegex("\"(http://(www\\.)?cdn\\.mangaeden\\.com/[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) return null;
        return finallink;
    }
}
