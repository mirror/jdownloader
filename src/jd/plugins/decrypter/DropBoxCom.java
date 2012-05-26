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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropbox.com" }, urls = { "https?://(www\\.)?dropbox\\.com/sh/.+" }, flags = { 0 })
public class DropBoxCom extends PluginForDecrypt {

    public DropBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("?dl=1", "");
        br.setCookie("http://dropbox.com", "locale", "en");
        br.getPage(parameter);
        // Handling for single links
        if (br.containsHTML(new Regex(parameter, ".*?(dropbox\\.com/.+)").getMatch(0) + "\\?dl=1\"")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("dropbox.com/", "dropboxdecrypted.com/"));
            dl.setProperty("decrypted", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // Decrypt file- and folderlinks
        final String fpName = br.getRegex("<h3 id=\"folder\\-title\" class=\"shmodel\\-filename\">([^<>\"]*?)</h3>").getMatch(0);
        final String[] folderLinks = br.getRegex("alt=\"\" class=\"sprite s_folder_32 icon\" />[\t\n\r ]+</a>[\t\n\r ]+<div class=\"filename\">[\t\n\r ]+<a href=\"(https?://(www\\.)?dropbox\\.com/[^<>\"]*?)\"").getColumn(0);
        final String[][] fileLinks = br.getRegex("<div class=\"filename\">[\t\n\r ]+<a href=\"(https?://(www\\.)?dropbox\\.com/[^<>\"]*?)\".*?class=\"filename\\-link\">([^<>\"]*?)</a>.*?class=\"filesize\\-col\">[\t\n\r ]+<span class=\"size\">(\\d+[^<>\"]*?)</span>").getMatches();
        if ((fileLinks == null || fileLinks.length == 0) && (folderLinks == null || folderLinks.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (folderLinks != null && folderLinks.length != 0) {
            for (String singleLink : folderLinks)
                decryptedLinks.add(createDownloadlink(singleLink));
        }
        if (fileLinks != null && fileLinks.length != 0) {
            // Set filenames, sizes and availablestatus for super fast adding
            for (String fileInfo[] : fileLinks) {
                final DownloadLink dl = createDownloadlink(fileInfo[0].replace("dropbox.com/", "dropboxdecrypted.com/"));
                dl.setName(fileInfo[2]);
                dl.setDownloadSize(SizeFormatter.getSize(fileInfo[3].replace(",", ".")));
                dl.setProperty("decrypted", true);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
