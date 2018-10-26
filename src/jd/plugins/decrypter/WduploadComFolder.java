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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wdupload.com" }, urls = { "https?://(?:www\\.)?wdupload\\.com/folder/.+" })
public class WduploadComFolder extends PluginForDecrypt {
    public WduploadComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">This link only for premium")) {
            final DownloadLink link = createOfflinelink(parameter);
            link.setName("PremiumOnly:" + link.getName());
            decryptedLinks.add(link);
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("file-remove-|is empty...<")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<h2>Folder :([^<>\"]+): \\d+ Files </h2>").getMatch(0);
        final String[] links = br.getRegex("(https?://(?:www\\.)?wdupload\\.com/file/[^<>\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
