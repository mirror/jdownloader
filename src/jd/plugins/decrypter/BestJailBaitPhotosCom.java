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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bestjailbaitphotos.com" }, urls = { "http://(www\\.)?bestjailbaitphotos\\.com/index\\.php/component/joomgallery/user\\-gallerys/[a-z0-9\\-_]+/[a-z0-9\\-_]+(\\?page=\\d+)?" }, flags = { 0 })
public class BestJailBaitPhotosCom extends PluginForDecrypt {

    public BestJailBaitPhotosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<meta name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        String page = new Regex(parameter, "\\?page=(\\d+)").getMatch(0);
        boolean stop = false;
        int maxPage = 1;
        if (page != null) {
            // Stop after first page
            maxPage = Integer.parseInt(page);
            if (maxPage > 1)
                stop = true;
            else
                maxPage = getMaxpage();
        } else {
            getMaxpage();
        }
        final DecimalFormat df = new DecimalFormat("0000");
        int counter = 1;
        final String pagePiece = parameter.replaceAll("\\?page=\\d+", "");
        for (int i = 1; i <= maxPage; i++) {
            if (i > 1) br.getPage(pagePiece + "?page=" + i);
            final String[] links = br.getRegex("gf\\-\\d+\\-(\\d+)\" class=\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String id : links) {
                final DownloadLink dl = createDownloadlink("directhttp://http://bestjailbaitphotos.com/index.php/component/joomgallery/image?view=image&format=raw&type=orig&id=" + id);
                dl.setFinalFileName(fpName + "_" + df.format(counter) + ".jpeg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                counter++;
            }
            if (stop) break;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private int getMaxpage() {
        String page = br.getRegex("\\?page=(\\d+)#category\" title=\"End").getMatch(0);
        if (page != null)
            return Integer.parseInt(page);
        else
            return 1;
    }
}
