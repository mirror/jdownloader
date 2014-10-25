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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "opendrive.com" }, urls = { "https?://(www\\.)?opendrive\\.com/folders\\?[A-Za-z0-9]+" }, flags = { 0 })
public class OpenDriveComDecrypter extends PluginForDecrypt {

    public OpenDriveComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Avoid https because of old 0.9.581 Stable & this host does not force https (maybe only for login) */
        final String parameter = param.toString().replace("https://", "http://");
        br.getPage(parameter);
        if (br.getURL().contains("?e=")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String fpName = br.getRegex("class=\"selected root\\-folder\" href=\"/folders\\?[A-Za-z0-9]+\" title=\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) {
            fpName = parameter.substring(parameter.lastIndexOf("?") + 1);
        }
        // div class="grid-file one-item draggable "
        final String[] info = br.getRegex("<div class=\"grid\\-file one\\-item draggable \"(.*?)</small>[\t\n\r ]+</div>").getColumn(0);
        if (info == null || info.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleinfo : info) {
            final String fid = new Regex(singleinfo, "id=\"file\\-([A-Za-z0-9\\-_]+)\"").getMatch(0);
            final String filename = new Regex(singleinfo, "class=\"editable\\-file\" title=\"([^<>\"]*?)\"").getMatch(0);
            final String filesize = new Regex(singleinfo, "<small>(.+)").getMatch(0);
            if (fid == null || filename == null || filesize == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink fina = createDownloadlink("https://www.opendrive.com/files?" + fid);
            fina.setName(Encoding.htmlDecode(filename));
            fina.setDownloadSize(SizeFormatter.getSize(filesize));
            fina.setAvailable(true);
            decryptedLinks.add(fina);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
