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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nitrobits.com" }, urls = { "http://(www\\.)?nitrobits\\.com/folder/[A-Za-z0-9]+" }, flags = { 0 })
public class NitroBitsComFolder extends PluginForDecrypt {

    public NitroBitsComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Folder was not found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[] entries = br.getRegex("<td class=\"link colored\">(.*?)</td>").getColumn(0);
        if (entries == null || entries.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String fpName = br.getRegex("<title>Nitrobits\\.com \\-([^<>\"]*?)</title>").getMatch(0);
        if (fpName == null) fpName = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        for (final String entry : entries) {
            final Regex info = new Regex(entry, "\"(https?://nitrobits\\.com/file/[A-Za-z0-9]+/([^<>\"/]*?))\"");
            final String url = info.getMatch(0);
            final String fname = info.getMatch(1);
            final String fsize = new Regex(entry, "style=\"[^<>\"/]*?\">([^<>\"]*?)</span>").getMatch(0);
            if (url == null || fname == null || fsize == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink fina = createDownloadlink(url);
            fina.setName(fname);
            fina.setDownloadSize(SizeFormatter.getSize(fsize));
            fina.setAvailable(true);
            decryptedLinks.add(fina);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
