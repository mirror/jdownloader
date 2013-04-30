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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "http(s)?://(www\\.)?(on\\.fb\\.me/[A-Za-z0-9]+\\+?|facebook\\.com/.*?set=a\\.\\d+\\.\\d+\\.\\d+|facebook\\.com/photo\\.php\\?fbid=\\d+)" }, flags = { 0 })
public class FaceBookComGallery extends PluginForDecrypt {

    public FaceBookComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* must be static so all plugins share same lock */
    private static Object       LOCK             = new Object();
    private static final String FACEBOOKMAINPAGE = "http://www.facebook.com";
    private static final String FBSHORTLINK      = "http(s)?://(www\\.)?on\\.fb\\.me/[A-Za-z0-9]+\\+?";
    private static final String SINGLEPHOTO      = "http(s)?://(www\\.)?facebook\\.com/photo\\.php\\?fbid=\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        synchronized (LOCK) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            String parameter = param.toString().replace("#!/", "");
            br.getHeaders().put("User-Agent", jd.plugins.hoster.FaceBookComVideos.Agent);
            br.setFollowRedirects(false);
            if (parameter.matches(FBSHORTLINK)) {
                br.getPage(parameter);
                final String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            } else if (parameter.matches(SINGLEPHOTO)) {
                br.getPage(parameter);
                final String finallink = br.getRegex("id=\"fbPhotoImage\" src=\"(https?://[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    if (br.containsHTML(">Dieser Inhalt ist derzeit nicht verfügbar")) {
                        logger.info("Link offline: " + parameter);
                        return decryptedLinks;
                    }
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            } else {
                br.setCookie(FACEBOOKMAINPAGE, "locale", "en_GB");
                final PluginForHost facebookPlugin = JDUtilities.getPluginForHost("facebook.com");
                Account aa = AccountController.getInstance().getValidAccount(facebookPlugin);
                boolean addAcc = false;
                if (aa == null) {
                    String username = UserIO.getInstance().requestInputDialog("Enter Loginname for facebook.com :");
                    if (username == null) {
                        logger.info("FacebookDecrypter: No username entered while decrypting link: " + parameter);
                        return decryptedLinks;
                    }
                    String password = UserIO.getInstance().requestInputDialog("Enter password for facebook.com :");
                    if (password == null) {
                        logger.info("FacebookDecrypter: No password entered while decrypting link: " + parameter);
                        return decryptedLinks;
                    }
                    aa = new Account(username, password);
                    addAcc = true;
                }
                try {
                    ((jd.plugins.hoster.FaceBookComVideos) facebookPlugin).login(aa, false, this.br);
                } catch (final PluginException e) {
                    aa.setEnabled(false);
                    aa.setValid(false);
                    logger.info("Account seems to be invalid, returnung empty linklist!");
                    return decryptedLinks;
                }
                // Account is valid, let's add it to the premium overview
                if (addAcc) AccountController.getInstance().addAccount(facebookPlugin, aa);
                // Redirects from "http" to "https" can happen
                br.setFollowRedirects(true);
                br.getPage(parameter);
                String mainpage = "http://www.facebook.com";
                if (br.getURL().contains("https://")) {
                    mainpage = "https://www.facebook.com";
                }
                final String setID = new Regex(parameter, "set=(.+)").getMatch(0);
                final String profileID = new Regex(parameter, "(\\d+)$").getMatch(0);
                String fpName = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
                final String ajaxpipeToken = br.getRegex("\"ajaxpipe_token\":\"([^<>\"]*?)\"").getMatch(0);
                final String user = br.getRegex("\"user\":\"(\\d+)\"").getMatch(0);
                if (ajaxpipeToken == null || user == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (fpName == null) fpName = "Facebook_album_of_user_" + user;
                fpName = Encoding.htmlDecode(fpName.trim());
                boolean dynamicLoadAlreadyDecrypted = false;
                final ArrayList<String> allLinks = new ArrayList<String>();

                for (int i = 1; i <= 50; i++) {
                    int currentMaxPicCount = 28;

                    String[] links;
                    if (i > 1) {
                        String currentLastFbid = br.getRegex("\"last_fbid\\\\\":\\\\\"(\\d+)\\\\\\\"").getMatch(0);
                        if (currentLastFbid == null) currentLastFbid = br.getRegex("\"last_fbid\\\\\":(\\d+)").getMatch(0);
                        // If we have exactly 60 pictures then we reload one
                        // time and got all, 2nd time will then be 0 more links
                        // -> Stop
                        if (currentLastFbid == null && dynamicLoadAlreadyDecrypted) {
                            break;
                        } else if (currentLastFbid == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        final String loadLink = mainpage + "/ajax/pagelet/generic.php/TimelinePhotosAlbumPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipeToken + "&no_script_path=1&data=%7B%22scroll_load%22%3Atrue%2C%22last_fbid%22%3A%22" + currentLastFbid + "%22%2C%22fetch_size%22%3A32%2C%22profile_id%22%3A" + profileID + "%2C%22viewmode%22%3Anull%2C%22set%22%3A%22" + setID + "%22%2C%22type%22%3A%223%22%2C%22pager_fired_on_init%22%3Atrue%7D&__user=" + user + "&__a=1&__dyn=798aD5z5CF-&__req=jsonp_" + i + "&__adt=" + i;
                        br.getPage(loadLink);
                        links = br.getRegex("data\\-non\\-starred-src=\\\\\"(https?:[^<>\"]*?)\\\\\"").getColumn(0);
                        currentMaxPicCount = 32;
                        dynamicLoadAlreadyDecrypted = true;
                    } else {
                        links = br.getRegex("data\\-starred\\-src=\"(https?://[^<>\"]*?)\"").getColumn(0);
                    }
                    if (links == null || links.length == 0) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    boolean stop = false;
                    logger.info("Decrypting page " + i + " of ??");
                    for (final String directlink : links) {
                        final String finallink = Encoding.htmlDecode(directlink.trim().replace("\\", ""));
                        allLinks.add(finallink);
                        final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                        dl.setAvailable(true);
                        decryptedLinks.add(dl);
                    }
                    // 28 links = max number of links per segment
                    if (links.length < currentMaxPicCount) stop = true;
                    if (stop) {
                        logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                        break;
                    }
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}