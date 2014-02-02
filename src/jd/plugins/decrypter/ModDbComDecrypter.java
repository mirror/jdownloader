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
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moddb.com" }, urls = { "http://(www\\.)?moddb\\.com/(games|mods|engines|groups)/.*?/(addons|downloads)/[0-9a-z-]+" }, flags = { 0 })
public class ModDbComDecrypter extends PluginForDecrypt {

    public ModDbComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private volatile boolean loaded = false;
    private static Object    LOCK   = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (loaded == false) {
            synchronized (LOCK) {
                if (loaded == false) {
                    /*
                     * we only have to load this once, to make sure its loaded
                     */
                    JDUtilities.getPluginForHost("moddb.com");
                }
                loaded = true;
            }
        }
        // Get pages with the mirrors
        jd.plugins.hoster.ModDbCom.getSinglemirrorpage(br);
        final String gameFrontmirror = br.getRegex("Mirror provided by Gamefront.*?<a href=\"(.*?)\"").getMatch(0);
        if (gameFrontmirror != null) {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            br2.getPage(gameFrontmirror.trim());
            String finalLink = br2.getURL();
            if (!finalLink.contains("gamefront.com/")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            // Fix invalid links
            finalLink = finalLink.replace("/files/files/", "/files/");
            decryptedLinks.add(createDownloadlink(finalLink));
        } else {
            decryptedLinks.add(createDownloadlink(parameter.replace("moddb.com/", "moddbdecrypted.com/")));
            return decryptedLinks;
        }
        if (br.containsHTML("(Mirror provided by Mod DB|Mirror provided by FDCCDN)")) decryptedLinks.add(createDownloadlink(parameter.replace("moddb.com/", "moddbdecrypted.com/")));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}