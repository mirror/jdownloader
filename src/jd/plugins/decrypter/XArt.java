//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Iterator;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: $", interfaceVersion = 2, names = { "x-art.com" }, urls = { "^https?://x\\-art\\.com/members/videos/[a-zA0-9\\-\\_]+/$" }, flags = { 0 })
public class XArt extends PluginForDecrypt {

    public XArt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        boolean prem = false;
        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("x-art.com");
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    prem = this.login(n);
                    break;
                }
            }
        }
        if (!prem) {
            logger.warning("You need to use an Account with this provider");
            return ret;
        }
        br.setFollowRedirects(true);

        br.getPage(parameter.toString());

        String links[] = br.getRegex("href=\"([a-zA-Z0-9\\_\\-]*\\.(mp4|wmv|mov))\"").getColumn(0);

        if (links == null || links.length == 0) {
            logger.warning("Possible plugin defect, please confirm in browser. If their are links present please report to JDownloader Development Team : " + parameter.toString());
            return null;
        }

        for (String link : links) {
            String fulllink = br.getURL() + link;
            DownloadLink dl = createDownloadlink(Encoding.htmlDecode(fulllink));
            ret.add(dl);
        }
        String title = br.getRegex("<h1>([a-zA-Z0-9\\_\\-\\ ]*)<\\/h1>").getMatch(0);
        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName("XArt Movie: " + title);
            fp.addLinks(ret);
        }
        return ret;
    }

    private boolean login(final Account account) throws Exception {
        this.setBrowserExclusive();
        final PluginForHost plugin = JDUtilities.getPluginForHost("x-art.com");
        try {
            if (plugin != null) {
                ((jd.plugins.hoster.XArtCom) plugin).login(account, br, false);
            } else {
                return false;
            }
        } catch (final PluginException e) {
            account.setEnabled(false);
            account.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}