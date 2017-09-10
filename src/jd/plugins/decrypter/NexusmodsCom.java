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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nexusmods.com" }, urls = { "https?://(?:www\\.)?nexusmods\\.com/[^/]+/mods/\\d+/" })
public class NexusmodsCom extends PluginForDecrypt {
    public NexusmodsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = jd.plugins.hoster.NexusmodsCom.getFID(parameter);
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        if (aa != null) {
            jd.plugins.hoster.NexusmodsCom.login(this.br, aa, false);
        }
        br.getPage(parameter);
        if (jd.plugins.hoster.NexusmodsCom.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^>]+)</title>").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = fid;
        }
        final Browser br2 = br.cloneBrowser();
        br2.getPage(String.format("/skyrim/ajax/modfiles/?id=%s&gid=110", fid));
        String[] links = br2.getRegex("(https?://(?:www\\.)?nexusmods\\.com+/[^/]+/ajax/downloadfile\\?id=\\d+)").getColumn(0);
        if (jd.plugins.hoster.NexusmodsCom.isOffline(br2)) {
            links = br.getRegex("href=\"([^\"]+)\" onclick=").getColumn(0);
        }
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
