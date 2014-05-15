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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up.4share.vn" }, urls = { "http://(www\\.)?(up\\.)?4share\\.vn/(d|dlist)/[a-z0-9]+" }, flags = { 0 })
public class Up4ShareVnFolderdecrypter extends PluginForDecrypt {

    public Up4ShareVnFolderdecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("up.4share.vn/", "4share.vn/");
        br.getPage(parameter);
        if (br.containsHTML(">Error: Not valid ID") && !br.containsHTML("up\\.4share\\.vn/f/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[] links = br.getRegex("(http://(up\\.)?4share\\.vn/(d/[a-z0-9]{1,}|f/[a-z0-9]+/[^<>\"]{1,}))").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String dl : links) {
            decryptedLinks.add(createDownloadlink(dl));
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}