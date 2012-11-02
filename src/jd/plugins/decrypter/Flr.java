//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.net" }, urls = { "http://(www\\.)?filer.net/folder/[a-z0-9]+" }, flags = { 0 })
public class Flr extends PluginForDecrypt {

    public Flr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);

        br.getPage(parameter);
        if (br.containsHTML(">Error: Folder not found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[][] links = br.getRegex("\"(/get/[a-z0-9]+)\">([^<>\"]*?)</a>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatches();
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link:" + parameter);
            return decryptedLinks;
        }

        for (String element[] : links) {
            final DownloadLink link = createDownloadlink("http://filer.net" + element[0]);
            link.setFinalFileName(element[1]);
            link.setDownloadSize(SizeFormatter.getSize(element[2]));
            decryptedLinks.add(link);
        }

        return decryptedLinks;
    }
}
