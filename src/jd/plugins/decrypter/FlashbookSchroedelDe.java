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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flashbook.schroedel.de" }, urls = { "http://(www\\.)?flashbook\\.schroedel\\.de/elemente\\-[0-9\\-]+" }, flags = { 0 })
public class FlashbookSchroedelDe extends PluginForDecrypt {

    public FlashbookSchroedelDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<title>Flashbook: ([^<>\"/]+)</title>").getMatch(0);
        if (fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        br.getPage(parameter.replace("flashbook.schroedel.de/", "flashbook.schroedel.de/xml/"));
        String[] links = br.getRegex("position=\"left top\" hires=\"\\.\\.(/bookpages/[^<>\"\\']+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        DecimalFormat df = new DecimalFormat("0000");
        int page = 1;
        for (String singleLink : links) {
            DownloadLink dl = createDownloadlink("directhttp://http://flashbook.schroedel.de" + singleLink);
            dl.setFinalFileName(fpName + " - " + df.format(page) + ".jpeg");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            page++;
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
