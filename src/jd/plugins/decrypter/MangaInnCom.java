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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangainn.com" }, urls = { "http://(www\\.)?mangainn\\.com/manga/chapter/\\d+_" }, flags = { 0 })
public class MangaInnCom extends PluginForDecrypt {

    public MangaInnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("class=\"gomangainfo\"> \\-([^<>\"]*?)</span>").getMatch(0);
        final String[] pages = br.getRegex("value=\"(\\d+)\">\\d+</option>").getColumn(0);
        if (pages == null || pages.length == 0 || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName);
        final DecimalFormat df = new DecimalFormat("0000");
        for (final String page : pages) {
            if (!page.equals("1")) br.getPage(parameter + "/page_" + page);
            final String finallink = br.getRegex("\"(http://static\\d+\\.mangainn\\.com/mangas/\\d+/\\d+/[^<>\"]*?)\"").getMatch(0);
            final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(fpName + "_" + df.format(Integer.parseInt(page)) + finallink.substring(finallink.lastIndexOf(".")));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
