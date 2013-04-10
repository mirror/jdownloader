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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangas.xpg.com.br" }, urls = { "http://(www\\.)?mangas\\.xpg\\.com\\.br/reader/\\d+" }, flags = { 0 })
public class MangasXpgComBr extends PluginForDecrypt {

    public MangasXpgComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://mangas.xpg.com.br/reader/listPages.php", "update=0&rel=" + new Regex(parameter, "(\\d+)$").getMatch(0));
        String fpName = getData("series");
        final String[] links = br.getRegex("\"\\d+\":\\{\"path\":\"([^<>\"/]*?\\.(jpg|png))\"").getColumn(0);
        String path = getData("path");
        if (links == null || links.length == 0 || fpName == null || path == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        path = Encoding.htmlDecode(path).replace("\\", "");
        fpName = Encoding.htmlDecode(fpName.trim());

        final DecimalFormat df = new DecimalFormat("0000");
        int counter = 1;
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink("directhttp://" + path + singleLink);
            dl.setFinalFileName(fpName + "_" + df.format(counter) + singleLink.substring(singleLink.lastIndexOf(".")));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            counter++;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getData(final String parameter) {
        return br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}