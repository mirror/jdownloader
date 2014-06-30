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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rabbitfile.com" }, urls = { "http://(www\\.)?rabbitfile\\.com/download_regular\\.php\\?file=[A-Za-z0-9]+" }, flags = { 0 })
public class RabbitFileComDecrypter extends PluginForDecrypt {

    public RabbitFileComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String code = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        final DownloadLink main = createDownloadlink("http://rabbitfiledecrypted.com/" + code + "&part=0");
        main.setProperty("plain_code", code);
        main.setProperty("mainlink", parameter);

        br.getPage(parameter);
        if (br.toString().length() < 300) {
            main.setFinalFileName(code);
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        String folderName = br.getRegex("fa\\-cloud\\-download\"></i> ([^<>\"]*?)</a>").getMatch(0);
        if (folderName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        folderName = Encoding.htmlDecode(folderName.trim());

        final String[] links = br.getRegex("(<a class=\\'list\\-group\\-item\\'.*?</a>)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        int part = 1;
        for (final String singleinfo : links) {
            // final String dllink = new Regex(singleinfo, "href=\\'(http://[^<>\"]*?)\\'").getMatch(0);
            String filename = new Regex(singleinfo, "</i> DOWNLOAD ([^<>\"]*?)</a>").getMatch(0);
            final DownloadLink dl = createDownloadlink("http://rabbitfiledecrypted.com/" + code + "&part=" + part);
            if (filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            dl.setFinalFileName(filename);
            dl.setProperty("plain_name", filename);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("plain_part", Integer.toString(part));
            dl.setProperty("plain_code", code);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            part++;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String getFID(final String link) {
        return new Regex(link, "([A-Za-z0-9]+)$").getMatch(0);
    }

}
