//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ifolder.ru" }, urls = { "http://[\\w\\.]*?(yapapka|rusfolder|ifolder)\\.(net|ru|com)/f\\d+" }, flags = { 0 })
public class IFolderFolder extends PluginForDecrypt {

    public IFolderFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("Запрашиваемая вами папка не существует или у вас нет прав для просмотра данной папки")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("Название: <b>(.*?)</b>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<h1>Просмотр папки(.*?)</h").getMatch(0);
        // Get the links of the first page
        String collectedlinks1[] = getLinks();
        if (collectedlinks1 == null || collectedlinks1.length == 0) return null;
        for (String dl : collectedlinks1) {
            decryptedLinks.add(createDownloadlink("http://rusfolder.ru/" + dl));
        }
        // Find pages if there is more than one page
        String[] pages = br.getRegex("page(=\\d+\">\\d+)</a>").getColumn(0);
        if (pages != null && pages.length != 0) {
            progress.setRange(pages.length);
            for (String page : pages) {
                String pagenumber = new Regex(page, "=(\\d+)\"").getMatch(0);
                br.getPage(parameter + "?page=" + pagenumber);
                String[] links = getLinks();
                if (links == null || links.length == 0) return null;
                for (String dl : links) {
                    decryptedLinks.add(createDownloadlink("http://rusfolder.ru" + dl));
                }
            }
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public String[] getLinks() throws NumberFormatException, DecrypterException, IOException {
        String[] links = br.getRegex("\\&#8470;[ \t]*?(\\d+)(</span>.*?)? <a href=").getColumn(0);
        return links;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}