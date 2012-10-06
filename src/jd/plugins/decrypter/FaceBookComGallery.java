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
    private static Object       LOCK             = new Object();
    private static final String FACEBOOKMAINPAGE = "http://www.facebook.com";

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
                final String setID = new Regex(parameter, "facebook\\.com/media/set/\\?set=(.+)").getMatch(0);
                final String profileID = new Regex(parameter, "(\\d+)$").getMatch(0);
                String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
                final String ajaxpipeToken = br.getRegex("\"ajaxpipe_token\":\"([^<>\"]*?)\"").getMatch(0);
                final String lastFbid = br.getRegex("\"fbPhotosRedesignMetadata(\\d+)\"").getMatch(0);
                final String user = br.getRegex("\"user\":\"(\\d+)\"").getMatch(0);
                if (ajaxpipeToken == null || lastFbid == null || user == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }

                ArrayList<String> allLinks = new ArrayList<String>();

                // Redirects from "http" to "https" can happen
                br.setFollowRedirects(true);
                for (int i = 1; i <= 50; i++) {
                    int currentMaxPicCount = 28;

                    String[] links;
                    if (i > 1) {
                        br.getPage("http://www.facebook.com/ajax/pagelet/generic.php/TimelinePhotosAlbumPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipeToken + "&no_script_path=1&data=%7B%22scroll_load%22%3Atrue%2C%22last_fbid%22%3A" + lastFbid + "%2C%22fetch_size%22%3A32%2C%22profile_id%22%3A" + profileID + "%2C%22viewmode%22%3Anull%2C%22set%22%3A%22" + setID + "%22%2C%22type%22%3A%221%22%7D&__user=" + user + "&__a=1&__adt=" + i);
                        links = br.getRegex("data\\-starred\\-src=\\\\\"(http:[^<>\"]*?)\\\\\"").getColumn(0);
                        currentMaxPicCount = 32;
                    } else {
                        links = br.getRegex("src=(http[^<>\"]*?_n\\.jpg)\\&amp;size=\\d+%2C\\d+\\&amp;theater\" rel=\"theater\"").getColumn(0);
                    }
                    boolean doExtended = false;
                    if (links == null || links.length == 0) {
                        // Can't grab direct links? Try extended mode
                        links = br.getRegex("ajaxify=(\"|\\\\\")https?(://|:\\\\/\\\\/)www\\.facebook\\.com(/|\\\\/)photo\\.php\\?fbid=(\\d+)\\&amp;").getColumn(3);
                        doExtended = true;
                    }
                    if (links == null || links.length == 0) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    boolean stop = false;
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
                        // This way only works on the first page
                        stop = true;
                    } else {
                        logger.info("Decrypting page " + i + " of??");
                        for (final String directlink : links) {
                            final String finallink = Encoding.htmlDecode(directlink.trim().replace("\\", ""));
                            if (allLinks.contains(finallink)) {
                                logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                                stop = true;
                                break;
                            } else {
                                allLinks.add(finallink);
                            }
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
