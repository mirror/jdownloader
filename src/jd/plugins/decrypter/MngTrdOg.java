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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: 26023 $", interfaceVersion = 2, names = { "mangatraders.org" }, urls = { "http://(www\\.)?mangatraders\\.org/manga/\\?series=\\w+" }, flags = { 0 })
public class MngTrdOg extends PluginForDecrypt {

    private PluginForHost plugin = null;

    private Browser prepBrowser(Browser prepBr) {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("mangatraders.org");
            if (plugin == null) {
                throw new IllegalStateException("mangatraders.org hoster plugin not found!");
            }
        }
        // set cross browser support
        ((jd.plugins.hoster.MangaTradersOrg) plugin).setBrowser(br);
        return prepBr;
    }

    public MngTrdOg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        prepBrowser(br);
        // pages now are just links to download files. We need to be logged in to get this information.
        if (!login()) {
            try {
                decryptedLinks.add(createOfflinelink(parameter, "In order to use this website you need an Account!"));
            } catch (final Throwable t) {
                logger.info("Login Required! " + parameter);
            }
            return decryptedLinks;
        }

        br.getPage(parameter);
        // return error message for invalid url
        if (br.containsHTML(">Error - Page Not Found<|This series is on our <a") || br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(createOfflinelink(parameter));
            } catch (final Throwable t) {
                logger.warning("Invalid URL: " + parameter);
            }
            return decryptedLinks;
        }

        // Set package name and prevent null field from creating plugin errors
        String fpName = br.getRegex("<title>(.*?) - MangaTraders</title>").getMatch(0);
        if (fpName == null) {
            fpName = "Untitled";
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);

        // First getPage listening regex
        String[] links = br.getRegex("href=\"[^\"]*(/manga/download\\.php\\?id=[a-f0-9]{10,})\">").getColumn(0);

        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (String dl : links) {
                decryptedLinks.add(createDownloadlink("http://mangatraders.org" + dl));
            }
        }

        if (decryptedLinks.size() > 0) {
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        } else {
            return null;
        }
    }

    private final boolean login() throws Exception {
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa != null) {
            try {
                synchronized (jd.plugins.hoster.MangaTradersOrg.ACCLOCK) {
                    ((jd.plugins.hoster.MangaTradersOrg) plugin).login(aa, false);
                }
            } catch (final PluginException e) {
                aa.setValid(false);
                logger.info("Account seems to be invalid!");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}