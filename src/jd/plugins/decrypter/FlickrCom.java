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
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "http://(www\\.)?flickr\\.com/(photos/([^<>\"/]+/(\\d+|favorites)|[^<>\"/]+(/galleries)?/(page\\d+|sets/\\d+)|[^<>\"/]+)|groups/[^<>\"/]+/[^<>\"/]+)" }, flags = { 0 })
public class FlickrCom extends PluginForDecrypt {

    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String MAINPAGE     = "http://flickr.com/";
    private static final String FAVORITELINK = "http://(www\\.)?flickr\\.com/photos/[^<>\"/]+/favorites";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<Integer> pages = new ArrayList<Integer>();
        ArrayList<String> addLinks = new ArrayList<String>();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.setCookie(MAINPAGE, "localization", "en-us%3Bde%3Bde");
        pages.add(1);
        String parameter = param.toString();
        // Check if link is for hosterplugin
        if (parameter.matches("http://(www\\.)?flickr\\.com/photos/[^<>\"/]+/\\d+")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("flickr.com/", "flickrdecrypted.com/"));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.containsHTML("Page Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        /** Login is not always needed but we force it to get all pictures */
        getUserLogin();
        br.getPage(parameter);
        String fpName = null;
        if (parameter.matches(FAVORITELINK)) {
            fpName = br.getRegex("<title>([^<>\"]*?) \\| Flickr</title>").getMatch(0);
        } else {
            fpName = br.getRegex("<title>Flickr: ([^<>\"/]+)</title>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("\"search_default\":\"Search ([^<>\"/]+)\"").getMatch(0);
        }
        /**
         * Handling for albums/sets Only decrypt all pages if user did NOT add a
         * direct page link
         * */
        if (!parameter.contains("/page")) {
            final String[] picpages = br.getRegex("data\\-track=\"page\\-(\\d+)\"").getColumn(0);
            if (picpages != null && picpages.length != 0) {
                for (String picpage : picpages)
                    pages.add(Integer.parseInt(picpage));
            }
        }
        final int lastPage = pages.get(pages.size() - 1);
        for (int i = 1; i <= lastPage; i++) {
            if (i != 1) br.getPage(parameter + "/page" + i);
            final String[] regexes = { "data\\-track=\"photo\\-click\" href=\"(/photos/[^<>\"\\'/]+/\\d+)" };
            for (String regex : regexes) {
                String[] links = br.getRegex(regex).getColumn(0);
                if (links != null && links.length != 0) {
                    for (String singleLink : links) {
                        // Regex catches links twice, correct that here
                        if (!addLinks.contains(singleLink)) addLinks.add(singleLink);
                    }
                }
            }
        }
        if (addLinks == null || addLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String aLink : addLinks) {
            final DownloadLink dl = createDownloadlink("http://www.flickrdecrypted.com" + aLink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost flickrPlugin = JDUtilities.getPluginForHost("flickr.com");
        Account aa = AccountController.getInstance().getValidAccount(flickrPlugin);
        boolean addAcc = false;
        if (aa == null) {
            String username = UserIO.getInstance().requestInputDialog("Enter Loginname for flickr.com :");
            if (username == null) throw new DecrypterException(JDL.L("plugins.decrypt.flickrcom.nousername", "Username not entered!"));
            String password = UserIO.getInstance().requestInputDialog("Enter password for flickr.com :");
            if (password == null) throw new DecrypterException(JDL.L("plugins.decrypt.flickrcom.nopassword", "Password not entered!"));
            aa = new Account(username, password);
            addAcc = true;
        }
        try {
            ((jd.plugins.hoster.FlickrCom) flickrPlugin).login(aa, false, this.br);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            logger.info("Account seems to be invalid!");
            return false;
        }
        // Account is valid, let's just add it
        if (addAcc) AccountController.getInstance().addAccount(flickrPlugin, aa);
        return true;
    }

}
