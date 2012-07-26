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
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.FaceBookComVideos;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "http(s)?://(www\\.)?(on\\.fb\\.me/[A-Za-z0-9]+\\+?|facebook\\.com/(#\\!/)?media/set/\\?set=a\\.\\d+\\.\\d+\\.\\d+)" }, flags = { 0 })
public class FaceBookComGallery extends PluginForDecrypt {

    public FaceBookComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* must be static so all plugins share same lock */
    private static final Object LOCK             = new Object();
    private static String       FACEBOOKMAINPAGE = "http://www.facebook.com";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        synchronized (LOCK) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            String parameter = param.toString().replace("#!/", "");
            br.getHeaders().put("User-Agent", jd.plugins.hoster.FaceBookComVideos.Agent);
            br.setFollowRedirects(false);
            if (parameter.contains("on.fb.me/")) {
                br.getPage(parameter);
                String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            } else {
                ArrayList<String> picPages = new ArrayList<String>();
                picPages.add(parameter);
                br.setCookie(FACEBOOKMAINPAGE, "locale", "en_GB");
                final PluginForHost facebookPlugin = JDUtilities.getPluginForHost("facebook.com");
                Account aa = AccountController.getInstance().getValidAccount(facebookPlugin);
                boolean addAcc = false;
                if (aa == null) {
                    String username = UserIO.getInstance().requestInputDialog("Enter Loginname for facebook.com :");
                    if (username == null) throw new DecrypterException(JDL.L("plugins.decrypt.facebookcomgallery.nousername", "Username not entered!"));
                    String password = UserIO.getInstance().requestInputDialog("Enter password for facebook.com :");
                    if (password == null) throw new DecrypterException(JDL.L("plugins.decrypt.facebookcomgallery.nopassword", "Password not entered!"));
                    aa = new Account(username, password);
                    addAcc = true;
                }
                try {
                    ((FaceBookComVideos) facebookPlugin).login(aa, false, this.br);
                } catch (final PluginException e) {
                    aa.setEnabled(false);
                    aa.setValid(false);
                    logger.info("Account seems to be invalid, returnung empty linklist!");
                    return decryptedLinks;
                }
                // Account is valid, let's add it to the premium overview
                if (addAcc) AccountController.getInstance().addAccount(facebookPlugin, aa);
                br.getPage(parameter);
                String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
                final String setID = new Regex(parameter, "facebook\\.com/media/set/\\?set=(.+)").getMatch(0);
                final String secondPage = br.getRegex("\"(http://(www\\.)?facebook\\.com/media/set/\\?set=" + setID + "\\&amp;type=\\d+\\&amp;aft=\\d+)\"").getMatch(0);
                if (secondPage != null) picPages.add(secondPage);
                // Redirects from "http" to "https" can happen
                br.setFollowRedirects(true);
                for (final String picPage : picPages) {
                    br.getPage(Encoding.htmlDecode(picPage.trim()));
                    boolean doExtended = false;
                    // Try to grab direct links
                    String[] links = br.getRegex("src=(http[^<>\"]*?_n\\.jpg)\\&amp;size=\\d+%2C\\d+\\&amp;theater\" rel=\"theater\"").getColumn(0);
                    if (links == null || links.length == 0) {
                        // Can't grab direct links? Try extended mode
                        links = br.getRegex("ajaxify=(\"|\\\\\")https?(://|:\\\\/\\\\/)www\\.facebook\\.com(/|\\\\/)photo\\.php\\?fbid=(\\d+)\\&amp;").getColumn(3);
                        doExtended = true;
                    }
                    if (links == null || links.length == 0) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    if (doExtended) {
                        logger.info("Doing extended decrypt, this takes time...");
                        // Old way
                        for (String fbid : links) {
                            br.getPage("http://www.facebook.com/photo.php?fbid=" + fbid + "&set=" + setID + "&type=1");
                            String finallink = br.getRegex("class=\"imageStage\"><img class=\"fbPhotoImage img\" id=\"fbPhotoImage\" src=\"(https?://[^<>\"]*?)\"").getMatch(0);
                            if (finallink == null) finallink = br.getRegex("\"(https?://fbcdn\\-sphotos\\-a\\.akamaihd\\.net/[^<>\"]*?)\"").getMatch(0);
                            if (finallink == null) {
                                logger.warning("Decrypter broken for link: " + parameter);
                                return null;
                            }
                            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                        }
                    } else {
                        logger.info("Doing fast decrypt...");
                        for (String directlink : links) {
                            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(directlink.trim()));
                            dl.setAvailable(true);
                            decryptedLinks.add(dl);
                        }
                    }
                }
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(decryptedLinks);
                }
            }
            return decryptedLinks;
        }
    }
}
