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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nk.pl" }, urls = { "http://(www\\.)?nk\\.pl/#?profile/\\d+/gallery(/album/\\d+(/\\d+)?|/\\d+|#\\!q\\?album=\\d+)" }, flags = { 0 })
public class NkPlGallery extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static Object       LOCK            = new Object();

    private static final String MAINPAGE        = "http://nk.pl/";
    private static final String POSTPAGE        = "https://nk.pl/login";
    private static final String DOMAIN          = "nk.pl";
    private static final String FINALLINKREGEX1 = "<img id=\"photo_img\" alt=\"zdjęcie\" src=\"(http://.*?)\"";
    private static final String FINALLINKREGEX2 = "\"(http://photos\\.nasza-klasa\\.pl/\\d+/\\d+/main/.*?)\"";

    public NkPlGallery(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String correctCryptedLink(final String parameter) {
        return parameter.replaceAll("(\\!q\\?album=\\d+|#)", "");
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setCookiesExclusive(true);
        synchronized (LOCK) {
            if (!getUserLogin()) { return null; }
            // Access the page
            br.getPage(correctCryptedLink(parameter));
            final String basicAuth = br.getCookie("nk.pl", "basic_auth");
            if (parameter.matches(".*?nk\\.pl/#?profile/\\d+/gallery/([0-9]+|album/\\d+/\\d+)")) {
                String finallink = br.getRegex(FINALLINKREGEX1).getMatch(0);
                if (finallink == null || finallink.contains("other/std")) {
                    finallink = br.getRegex(FINALLINKREGEX2).getMatch(0);
                }
                if (finallink == null) {
                    logger.warning("Failed to find the finallink(s) for gallery: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            } else if (parameter.matches(".*?nk\\.pl/#profile/\\d+/gallery#\\!q\\?album=\\d+")) {
                br.setFollowRedirects(true);
                final String galleryID = new Regex(parameter, "album=(\\d+)").getMatch(0);
                final String galleryCount = br.getRegex("data-count=\"(\\d+)\" data-album-id=\"" + galleryID + "\"").getMatch(0);
                if (galleryCount == null) {
                    logger.warning("Gallery not found for url: " + parameter);
                    return null;
                }
                final String profileNumber = new Regex(parameter, "nk.pl/#profile/(\\d+)").getMatch(0);
                String profilName = br.getRegex("<h3><a href=\"/profile/" + profileNumber + "\">(.*?)</a></h3>").getMatch(0);
                profilName = Encoding.htmlDecode(profilName.trim());
                String galleryName = br.getRegex("album_name\" title=\"(.*?)\"").getMatch(0);
                galleryName = galleryName == null ? "Album" : galleryName;
                if (profilName == null) {
                    galleryName += "_" + galleryID + "_profile" + profileNumber;
                } else {
                    galleryName += "_" + galleryID + "_" + profilName + "_profile" + profileNumber;
                }
                galleryName = Encoding.htmlDecode(galleryName.replaceAll("\\s+", "_"));

                // calculating ajax requests
                final int count = Integer.parseInt(galleryCount);
                final int reqNum = (count - count % 16) / 16;
                final String link = correctCryptedLink(parameter);

                progress.setRange(count);
                final DecimalFormat df = new DecimalFormat("0000");
                int c = 1;
                for (int i = 0; i <= reqNum; i++) {
                    br.getPage(link + "/album/" + galleryID + "/ajax/0/" + i * 16 + "?t=" + basicAuth);
                    final String picID = br.getRegex("\\{\"id\":\\[(.*?)\\]").getMatch(0);
                    if (picID == null) {
                        continue;
                    }
                    final String[] pictureID = picID.split(",");
                    if (pictureID == null || pictureID.length == 0) {
                        continue;
                    }
                    for (final String id : pictureID) {
                        final DownloadLink dl = createDownloadlink(link.replaceAll("nk\\.pl/", "nk.decryptednaszaplasa/") + "/album/" + galleryID + "/" + id + "?naszaplasalink");
                        dl.setFinalFileName(profileNumber + "_" + galleryID + "_" + df.format(c++) + ".jpeg");
                        dl.setAvailable(true);
                        decryptedLinks.add(dl);
                        progress.increase(1);
                    }
                    sleep(1000l, param);
                }

                final FilePackage fp = FilePackage.getInstance();
                fp.setName(galleryName.trim());
                fp.addLinks(decryptedLinks);
            } else {
                logger.warning("Failed to find the finallink(s) for gallery: " + parameter);
            }
            if (decryptedLinks == null || decryptedLinks.size() == 0) {
                logger.warning("Decrypter out of date for link: " + parameter);
                return null;
            }
            return decryptedLinks;
        }

    }

    private boolean getUserLogin() throws IOException, DecrypterException {
        br.setFollowRedirects(true);
        String username = null;
        String password = null;
        br.getPage(MAINPAGE);
        synchronized (LOCK) {
            final PluginForHost vkPlugin = JDUtilities.getPluginForHost("nk.pl");
            final Account aa = AccountController.getInstance().getValidAccount(vkPlugin);
            if (aa != null) {
                username = aa.getUser();
                password = aa.getPass();
            } else {
                username = getPluginConfig().getStringProperty("user", null);
                password = getPluginConfig().getStringProperty("pass", null);
            }
            if (username != null && password != null) {
                br.postPage(POSTPAGE, "login=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password) + "&remember=1&form_name=login_form&target=&manual=1");
            }
            for (int i = 0; i < 3; i++) {
                if (br.getCookie(MAINPAGE, "remember_me") == null || br.getCookie(MAINPAGE, "lltkck") == null) {
                    getPluginConfig().setProperty("user", Property.NULL);
                    getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + DOMAIN + " :");
                    if (username == null) { return false; }
                    password = UserIO.getInstance().requestInputDialog("Enter password for " + DOMAIN + " :");
                    if (password == null) { return false; }
                    br.postPage(POSTPAGE, "login=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password) + "&remember=1&form_name=login_form&target=&manual=1");
                } else {
                    getPluginConfig().setProperty("user", username);
                    getPluginConfig().setProperty("pass", password);
                    final Account acc = new Account(username, password);
                    final PluginForHost nkHosterplugin = JDUtilities.getPluginForHost("nk.pl");
                    final HashMap<String, String> cookies = new HashMap<String, String>();
                    final Cookies add = br.getCookies(MAINPAGE);
                    for (final Cookie c : add.getCookies()) {
                        cookies.put(c.getKey(), c.getValue());
                    }
                    getPluginConfig().setProperty("cookies", cookies);
                    acc.setProperty("name", acc.getUser());
                    acc.setProperty("pass", acc.getPass());
                    acc.setProperty("cookies", cookies);
                    // Add account for the nk.pl hosterplugin so user doesn't
                    // have to do it
                    AccountController.getInstance().addAccount(nkHosterplugin, acc);
                    getPluginConfig().save();
                    return true;
                }
            }
        }
        throw new DecrypterException("Login or/and password for \"Nasza Klasa\" is wrong!");
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}