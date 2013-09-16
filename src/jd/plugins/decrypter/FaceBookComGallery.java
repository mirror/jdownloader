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

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "http(s)?://(www\\.)?(on\\.fb\\.me/[A-Za-z0-9]+\\+?|facebook\\.com/((media/set/\\?set=|[^<>\"/]*?/media_set\\?set=)a\\.\\d+\\.\\d+\\.\\d+|media/set/\\?set=vb\\.\\d+|[a-z0-9\\.]+/photos_albums))" }, flags = { 0 })
public class FaceBookComGallery extends PluginForDecrypt {

    public FaceBookComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* must be static so all plugins share same lock */
    private static Object       LOCK                   = new Object();
    private static final String FACEBOOKMAINPAGE       = "http://www.facebook.com";
    private static final String FBSHORTLINK            = "http(s)?://(www\\.)?on\\.fb\\.me/[A-Za-z0-9]+\\+?";
    private static final String SINGLEPHOTO            = "http(s)?://(www\\.)?facebook\\.com/photo\\.php\\?fbid=\\d+";
    private int                 DIALOGRETURN           = -1;
    private static final String FASTLINKCHECK_PICTURES = "FASTLINKCHECK_PICTURES";
    private static final String SET_LINK_PHOTO         = "http(s)?://(www\\.)?facebook\\.com/(media/set/\\?set=|[^<>\"/]*?/media_set\\?set=)a\\.\\d+\\.\\d+\\.\\d+";
    private static final String SET_LINK_VIDEO         = "https://(www\\.)?facebook\\.com/media/set/\\?set=vb\\.\\d+";
    private static final String ALBUMS_LINK            = "https?://(www\\.)?facebook\\.com/[a-z0-9\\.]+/photos_albums";

    private String              MAINPAGE               = "http://www.facebook.com";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        synchronized (LOCK) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            String parameter = param.toString().replace("#!/", "");
            br.getHeaders().put("User-Agent", jd.plugins.hoster.FaceBookComVideos.Agent);
            br.setFollowRedirects(false);
            if (parameter.matches(FBSHORTLINK)) {
                br.getPage(parameter);
                final String finallink = br.getRedirectLocation();
                if (br.containsHTML(">Something\\'s wrong here")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            } else if (parameter.matches(SINGLEPHOTO)) {
                br.setFollowRedirects(true);
                br.getPage(parameter);
                if (br.containsHTML(">Dieser Inhalt ist derzeit nicht verfügbar")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                final String finallink = br.getRegex("id=\"fbPhotoImage\" src=\"(https?://[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            } else if (parameter.matches(ALBUMS_LINK)) {
                final boolean loggedIN = login();
                if (!loggedIN) {
                    logger.info("Cannot decrypt link without valid account: " + parameter);
                    return decryptedLinks;
                }
                getpagefirsttime(parameter);
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                String fpName = br.getRegex("<title id=\"pageTitle\">([^<>\"]*?)\\- Photos \\| Facebook</title>").getMatch(0);
                final String profileID = br.getRegex("data\\-gt=\"\\&#123;\\&quot;profile_owner\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
                final String user = br.getRegex("\"user\":\"(\\d+)\"").getMatch(0);
                if (user == null || profileID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (fpName == null) fpName = "Facebook_video_albums_of_user_" + new Regex(parameter, "facebook\\.com/(.*?)/photos_albums").getMatch(0);
                fpName = Encoding.htmlDecode(fpName.trim());
                boolean dynamicLoadAlreadyDecrypted = false;

                for (int i = 1; i <= 50; i++) {
                    int currentMaxPicCount = 18;

                    String[] links;
                    if (i > 1) {
                        String currentLastAlbumid = br.getRegex("\"last_album_id\":\"(\\d+)\"").getMatch(0);
                        if (currentLastAlbumid == null) currentLastAlbumid = br.getRegex("\"last_album_id\":(\\d+)").getMatch(0);
                        // If we have exactly currentMaxPicCount pictures then we reload one
                        // time and got all, 2nd time will then be 0 more links
                        // -> Stop
                        if (currentLastAlbumid == null && dynamicLoadAlreadyDecrypted) {
                            break;
                        } else if (currentLastAlbumid == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            logger.info("Returning already decrypted links anyways...");
                            return decryptedLinks;
                        }
                        final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/TimelinePhotoAlbumsPagelet?data=%7B%22profile_id%22%3A" + profileID + "%2C%22tab_key%22%3A%22photos_albums%22%2C%22sk%22%3A%22photos_albums%22%2C%22page_index%22%3A" + i + "%2C%22last_album_id%22%3A%22" + currentLastAlbumid + "%22%7D&__user=" + user + "&__a=1&__dyn=&__req=a";
                        br.getPage(loadLink);
                        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                        links = br.getRegex("class=\"photoTextTitle\" href=\"(https?://(www\\.)?facebook\\.com/media/set/\\?set=a\\.[0-9\\.]+)\\&amp;type=\\d+\"").getColumn(0);
                        currentMaxPicCount = 12;
                        dynamicLoadAlreadyDecrypted = true;
                    } else {
                        links = br.getRegex("class=\"photoTextTitle\" href=\"(https?://(www\\.)?facebook\\.com/[^<>\"]*?)\"").getColumn(0);
                    }
                    if (links == null || links.length == 0) {
                        logger.warning("Decrypter broken for the following link or account needed: " + parameter);
                        return null;
                    }
                    boolean stop = false;
                    logger.info("Decrypting page " + i + " of ??");
                    for (final String link : links) {
                        decryptedLinks.add(createDownloadlink(link));
                    }
                    // currentMaxPicCount = max number of links per segment
                    if (links.length < currentMaxPicCount) stop = true;
                    if (stop) {
                        logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                        break;
                    }
                }

                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);

            } else if (parameter.matches(SET_LINK_PHOTO)) {
                final boolean loggedIN = login();
                if (!loggedIN) {
                    logger.info("Cannot decrypt link without valid account: " + parameter);
                    return decryptedLinks;
                }
                getpagefirsttime(parameter);
                if (br.containsHTML(">Dieser Inhalt ist derzeit nicht verfügbar</")) {
                    logger.info("The link is either offline or an account is needed to grab it: " + parameter);
                    return decryptedLinks;
                }
                final String setID = new Regex(parameter, "set=(.+)").getMatch(0);
                final String profileID = new Regex(parameter, "(\\d+)$").getMatch(0);
                String fpName = br.getRegex("id=\"pageTitle\">([^<>\"]*?)\\| Facebook</title>").getMatch(0);
                if (fpName == null) fpName = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
                final String ajaxpipeToken = br.getRegex("\"ajaxpipe_token\":\"([^<>\"]*?)\"").getMatch(0);
                final String user = br.getRegex("\"user\":\"(\\d+)\"").getMatch(0);
                if (ajaxpipeToken == null || user == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (fpName == null) fpName = "Facebook_album_of_user_" + user;
                fpName = Encoding.htmlDecode(fpName.trim());
                boolean dynamicLoadAlreadyDecrypted = false;
                String lastfirstID = "";

                for (int i = 1; i <= 50; i++) {
                    int currentMaxPicCount = 28;

                    String[] links;
                    if (i > 1) {
                        String currentLastFbid = br.getRegex("\"last_fbid\\\\\":\\\\\"(\\d+)\\\\\\\"").getMatch(0);
                        if (currentLastFbid == null) currentLastFbid = br.getRegex("\"last_fbid\\\\\":(\\d+)").getMatch(0);
                        // If we have exactly currentMaxPicCount pictures then we reload one
                        // time and got all, 2nd time will then be 0 more links
                        // -> Stop
                        if (currentLastFbid == null && dynamicLoadAlreadyDecrypted) {
                            break;
                        } else if (currentLastFbid == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/TimelinePhotosAlbumPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipeToken + "&no_script_path=1&data=%7B%22scroll_load%22%3Atrue%2C%22last_fbid%22%3A%22" + currentLastFbid + "%22%2C%22fetch_size%22%3A32%2C%22profile_id%22%3A" + profileID + "%2C%22viewmode%22%3Anull%2C%22set%22%3A%22" + setID + "%22%2C%22type%22%3A%223%22%2C%22pager_fired_on_init%22%3Atrue%7D&__user=" + user + "&__a=1&__dyn=798aD5z5CF-&__req=jsonp_" + i + "&__adt=" + i;
                        br.getPage(loadLink);
                        links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
                        currentMaxPicCount = 32;
                        dynamicLoadAlreadyDecrypted = true;
                    } else {
                        links = br.getRegex("id=\"pic_(\\d+)\"").getColumn(0);
                    }
                    if (links == null || links.length == 0) {
                        logger.warning("Decrypter broken for the following link or account needed: " + parameter);
                        return null;
                    }
                    boolean stop = false;
                    logger.info("Decrypting page " + i + " of ??");
                    for (final String picID : links) {
                        // Another fail safe to prevent loops
                        if (picID.equals(lastfirstID)) {
                            stop = true;
                        }
                        final DownloadLink dl = createDownloadlink("http://www.facebook.com/photo.php?fbid=" + picID);
                        if (!loggedIN) dl.setProperty("nologin", true);
                        if (SubConfiguration.getConfig("facebook.com").getBooleanProperty(FASTLINKCHECK_PICTURES, false)) dl.setAvailable(true);
                        // Set temp name, correct name will be set in hosterplugin later
                        dl.setName(fpName + "_" + picID + ".jpg");
                        lastfirstID = picID;
                        decryptedLinks.add(dl);
                    }
                    // currentMaxPicCount = max number of links per segment
                    if (links.length < currentMaxPicCount) stop = true;
                    if (stop) {
                        logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                        break;
                    }
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            } else if (parameter.matches(SET_LINK_VIDEO)) {
                final boolean loggedIN = login();
                if (!loggedIN) {
                    logger.info("Decrypting photo album without login: " + parameter);
                }
                getpagefirsttime(parameter);
                String fpName = br.getRegex("<title id=\"pageTitle\">([^<>\"]*?)videos }}| Facebook</title>").getMatch(0);

                final String[] links = br.getRegex("uiVideoLinkMedium\" href=\"(https?://(www\\.)?facebook\\.com/photo\\.php\\?v=\\d+)").getColumn(0);
                for (final String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                }

                if (fpName != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(fpName.trim()));
                    fp.addLinks(decryptedLinks);
                }
            } else {
                // Should never happen
                logger.warning("Unsupported linktype: " + parameter);
                return null;
            }
            return decryptedLinks;
        }
    }

    private void getpagefirsttime(final String parameter) throws IOException {
        // Redirects from "http" to "https" can happen
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie(FACEBOOKMAINPAGE, "locale", "en_GB");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String scriptRedirect = br.getRegex("<script>window\\.location\\.replace\\(\"(https?:[^<>\"]*?)\"\\);</script>").getMatch(0);
        if (scriptRedirect != null) {
            scriptRedirect = Encoding.htmlDecode(scriptRedirect.replace("\\", ""));
            br.getPage(scriptRedirect);
        }
        if (br.getURL().contains("https://")) {
            MAINPAGE = "https://www.facebook.com";
        }
    }

    private boolean login() throws Exception {
        /** Login stuff begin */
        final PluginForHost facebookPlugin = JDUtilities.getPluginForHost("facebook.com");
        Account aa = AccountController.getInstance().getValidAccount(facebookPlugin);
        boolean addAcc = false;
        if (aa == null) {
            SubConfiguration config = null;
            try {
                config = this.getPluginConfig();
                if (config.getBooleanProperty("infoShown", Boolean.FALSE) == false) {
                    if (config.getProperty("infoShown2") == null) {
                        showFreeDialog();
                    } else {
                        config = null;
                    }
                } else {
                    config = null;
                }
            } catch (final Throwable e) {
            } finally {
                if (config != null) {
                    config.setProperty("infoShown", Boolean.TRUE);
                    config.setProperty("infoShown2", "shown");
                    config.save();
                }
            }
            // User wants to use the account
            if (this.DIALOGRETURN == 0) {
                String username = UserIO.getInstance().requestInputDialog("Enter Loginname for facebook.com :");
                if (username == null) { return false; }
                String password = UserIO.getInstance().requestInputDialog("Enter password for facebook.com :");
                if (password == null) { return false; }
                aa = new Account(username, password);
                addAcc = true;
            }
        }
        if (aa != null) {
            try {
                ((jd.plugins.hoster.FaceBookComVideos) facebookPlugin).login(aa, false, this.br);
            } catch (final PluginException e) {
                aa.setEnabled(false);
                aa.setValid(false);
                logger.info("Account seems to be invalid, returnung empty linklist!");
                return false;
            }
            // New account is valid, let's add it to the premium overview
            if (addAcc) AccountController.getInstance().addAccount(facebookPlugin, aa);
        }
        return true;
        /** Login stuff end */
    }

    private void showFreeDialog() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        final String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        if ("de".equalsIgnoreCase(lng)) {
                            title = "Facebook.com Gallerie/Photo Download";
                            message = "Du versucht gerade, eine Facebook Gallerie/Photo zu laden.\r\n";
                            message += "Für die meisten dieser Links wird ein gültiger Facebook Account benötigt!\r\n";
                            message += "Deinen Account kannst du in den Einstellungen als Premiumaccount hinzufügen.\r\n";
                            message += "Solltest du dies nicht tun, kann JDownloader nur Facebook Links laden, die keinen Account benötigen!\r\n";
                            message += "Willst du deinen Facebook Account jetzt hinzufügen?\r\n";
                        } else {
                            title = "Facebook.com gallery/photo download";
                            message = "You're trying to download a Facebook gallery/photo.\r\n";
                            message += "For most of these links, a valid Facebook account is needed!\r\n";
                            message += "You can add your account as a premium account in the settings.\r\n";
                            message += "Note that if you don't do that, JDownloader will only be able to download Facebook links which do not need a login.\r\n";
                            message += "Do you want to enter your Facebook account now?\r\n";
                        }
                        DIALOGRETURN = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}