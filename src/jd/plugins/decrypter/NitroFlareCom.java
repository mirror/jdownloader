//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nitroflare.com" }, urls = { "https?://(?:www\\.)?nitroflare\\.com/folder/(\\d+)/([A-Za-z0-9=]+)" })
public class NitroFlareCom extends antiDDoSForDecrypt {

    public NitroFlareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String userid = new Regex(parameter, this.getSupportedLinks().pattern()).getMatch(0);
        final String folderid = new Regex(parameter, this.getSupportedLinks().pattern()).getMatch(1);
        postPage("https://nitroflare.com/ajax/folder.php", "userId=" + userid + "&folder=" + Encoding.urlEncode(folderid) + "&fetchAll=1");
        final String fpName = PluginJSonUtils.getJsonValue(br, "name");
        final String filesArray = PluginJSonUtils.getJsonArray(br, "files");
        if (!inValidate(filesArray)) {
            String[] files = new Regex(filesArray, "\\{\"name\":.*?\\}(?:,|\\])").getColumn(-1);
            if (files != null && files.length > 0) {
                for (String file : files) {
                    // for now just return uid, nitroflare mass linkcheck shows avialable status and other values we need!
                    final String uid = PluginJSonUtils.getJsonValue(file, "url");
                    if (!inValidate(uid)) {
                        decryptedLinks.add(createDownloadlink("https://nitroflare.com/" + uid));
                    }
                }
            }
        }
        if (br.containsHTML(">This folder is empty<")) {
            logger.info("Link offline (folder empty): " + parameter);
            return decryptedLinks;
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}