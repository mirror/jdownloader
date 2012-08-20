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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flashmirrors.com" }, urls = { "http://(www\\.)?flashmirrors\\.com/files/[a-z0-9]+" }, flags = { 0 })
public class FlashMirrorsCom extends PluginForDecrypt {

    public FlashMirrorsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage("http://flashmirrors.com/mirrors/" + new Regex(parameter, "flashmirrors\\.com/files/(.+)").getMatch(0));
        if (!br.containsHTML("<img src=")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[] links = br.getRegex("/download\\.php\\?url=(https?://.*?)[ \r\n\t]*?(\"|\\')").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));

        return decryptedLinks;
    }
}
