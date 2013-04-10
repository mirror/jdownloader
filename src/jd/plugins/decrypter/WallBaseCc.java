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
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wallbase.cc" }, urls = { "http://(www\\.)?wallbase.cc/user/favorites(/[\\-\\d]+)?" }, flags = { 0 })
public class WallBaseCc extends PluginForDecrypt {

    public WallBaseCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (!getUserLogin(false)) return decryptedLinks;
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex(">You\'re browsing: <span style=\"[^\"]+\">([^<]+)</span></").getMatch(0);
        /* json */
        for (String Id : br.getRegex("wall_id\":\"(\\d+)\"").getColumn(0)) {
            DownloadLink dl = createDownloadlink("http://wallbase.cc/wallpaper/" + Id);
            decryptedLinks.add(dl);
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            /* http */
            for (String Id : br.getRegex("<a href=\"(http://wallbase.cc/wallpaper/\\d+)\" (class=\"sub\\-img\\-a\"|id=\"drg_thumb\\d+\")").getColumn(0)) {
                DownloadLink dl = createDownloadlink("http://wallbase.cc/wallpaper/" + Id);
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost plg = JDUtilities.getPluginForHost("wallbase.cc");
        final Account aa = AccountController.getInstance().getValidAccount(plg);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.WallBaseCc) plg).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(plg, aa);
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}