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
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.EroProfileCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eroprofile.com" }, urls = { "http://(www\\.)?eroprofile\\.com/m/photos/album/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class EroProfileComGallery extends PluginForDecrypt {

    public EroProfileComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* must be static so all plugins share same lock */
    private static Object LOCK = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> correctedpages = new ArrayList<String>();
        correctedpages.add("1");
        String parameter = param.toString();
        br.setReadTimeout(3 * 60 * 1000);
        br.setCookiesExclusive(false);
        br.setCookie("http://eroprofile.com/", "lang", "en");
        boolean loggedin = false;
        synchronized (LOCK) {
            /** Login process */
            loggedin = getUserLogin(false);
        }
        br.getPage(parameter);
        // Check if account needed but none account entered
        if (br.containsHTML(jd.plugins.hoster.EroProfileCom.NOACCESS) && !loggedin) {
            logger.info("Account needed to decrypt link: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("Browse photos from album \\&quot;([^<>\"]*?)\\&quot;<").getMatch(0);
        final String[] pages = br.getRegex("\\?pnum=(\\d+)\"").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String page : pages)
                if (!correctedpages.contains(page)) correctedpages.add(page);
        }
        for (final String page : correctedpages) {
            if (!page.equals("1")) br.getPage(parameter + "?pnum=" + page);
            String[][] links = br.getRegex("<table cellspacing=\"0\"><tr><td><a href=\"(/m/photos/view/([A-Za-z0-9\\-_]+))\"").getMatches();
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink[] : links) {
                final DownloadLink dl = createDownloadlink("http://www.eroprofile.com" + singleLink[0]);
                // final filename is set later in hosterplugin
                dl.setName(singleLink[1] + ".jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("eroprofile.com");
        Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) { return false; }
        try {
            ((EroProfileCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(hostPlugin, aa);
        return true;
    }

}
