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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "http://(www\\.)?flickr\\.com/(photos/([^<>\"/]+/(\\d+|favorites)|[^<>\"/]+(/galleries)?/(page\\d+|sets/\\d+)|[^<>\"/]+)|groups/[^<>\"/]+/[^<>\"/]+(/[^<>\"/]+)?)" }, flags = { 0 })
public class FlickrCom extends PluginForDecrypt {

    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String MAINPAGE     = "http://flickr.com/";
    private static final String FAVORITELINK = "http://(www\\.)?flickr\\.com/photos/[^<>\"/]+/favorites";
    private static final String GROUPSLINK   = "http://(www\\.)?flickr\\.com/groups/[^<>\"/]+/[^<>\"/]+(/[^<>\"/]+)?";
    private static final String PHOTOLINK    = "http://(www\\.)?flickr\\.com/photos/.*?";
    private static final String SETLINK      = "http://(www\\.)?flickr\\.com/photos/[^<>\"/]+/sets/\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> addLinks = new ArrayList<String>();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.setCookie(MAINPAGE, "localization", "en-us%3Bde%3Bde");
        br.setCookie(MAINPAGE, "fldetectedlang", "en-us");
        String parameter = param.toString();
        int lastPage = 1;
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

        // Some stuff which is different from link to link
        String picCount = br.getRegex("\"total\":(\")?(\\d+)").getMatch(1);
        int maxEntriesPerPage = 72;
        String fpName = br.getRegex("<title>Flickr: ([^<>\"/]+)</title>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("\"search_default\":\"Search ([^<>\"/]+)\"").getMatch(0);
        if (parameter.matches(SETLINK)) {
            picCount = br.getRegex("class=\"Results\">\\((\\d+) in set\\)</div>").getMatch(0);
            fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) fpName = br.getRegex("<title>([^<>\"]*?) \\- a set on Flickr</title>").getMatch(0);
        } else if (parameter.matches(PHOTOLINK)) {
            maxEntriesPerPage = 18;
        } else if (parameter.matches(FAVORITELINK)) {
            fpName = br.getRegex("<title>([^<>\"]*?) \\| Flickr</title>").getMatch(0);
        } else {
        }
        if (picCount == null) {
            logger.warning("Couldn't find total number of pictures, aborting...");
            return null;
        }

        final int totalEntries = Integer.parseInt(picCount);

        /**
         * Handling for albums/sets: Only decrypt all pages if user did NOT add
         * a direct page link
         * */
        int lastPageCalculated = 0;
        if (!parameter.contains("/page")) {
            logger.info("Decrypting all available pages.");
            // Removed old way of finding page number on the 27.07.12
            // Add 2 extra pages because usually the decrypter should already
            // stop before
            lastPageCalculated = (int) StrictMath.ceil(totalEntries / maxEntriesPerPage);
            lastPage = lastPageCalculated + 2;
            logger.info("Found " + lastPageCalculated + " pages using the calculation method.");
        }

        String getPage = parameter + "/page%s";
        if (parameter.matches(GROUPSLINK)) {
            // Try other way of loading more pictures for groups links
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage = parameter + "/page%s/?fragment=1";
        }
        for (int i = 1; i <= lastPage; i++) {
            int addedLinksCounter = 0;
            if (i != 1) br.getPage(String.format(getPage, i));
            final String[] regexes = { "data\\-track=\"photo\\-click\" href=\"(/photos/[^<>\"\\'/]+/\\d+)" };
            for (String regex : regexes) {
                String[] links = br.getRegex(regex).getColumn(0);
                if (links != null && links.length != 0) {
                    for (String singleLink : links) {
                        // Regex catches links twice, correct that here
                        if (!addLinks.contains(singleLink)) {
                            addLinks.add(singleLink);
                            addedLinksCounter++;
                        }
                    }
                }
            }
            logger.info("Found " + addedLinksCounter + " links on page " + i + " of approximately " + lastPage + " pages.");
            logger.info("Found already " + addLinks.size() + " of " + totalEntries + " entries, so we still have to decrypt " + (totalEntries - addLinks.size()) + " entries!");
            if (addedLinksCounter == 0 || addLinks.size() == totalEntries) {
                logger.info("Stopping at page " + i + " because it seems like we got everything decrypted.");
                break;
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
