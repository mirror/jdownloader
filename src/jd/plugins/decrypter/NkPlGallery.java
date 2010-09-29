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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nk.pl" }, urls = { "http://[\\w\\.]*?nk\\.pl/profile/\\d+/gallery(/album/\\d+|\\d+)" }, flags = { 0 })
public class NkPlGallery extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static final Object LOCK = new Object();

    public NkPlGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String MAINPAGE        = "http://nk.pl/";
    private static final String POSTPAGE        = "https://nk.pl/login";
    private static final String DOMAIN          = "nk.pl";
    private static final String FINALLINKREGEX1 = "<img id=\"photo_img\" alt=\"zdjęcie\" src=\"(http://.*?)\"";
    private static final String FINALLINKREGEX2 = "\"(http://photos\\.nasza-klasa\\.pl/\\d+/\\d+/main/.*?)\"";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        synchronized (LOCK) {
            if (!getUserLogin()) return null;
            br.setFollowRedirects(false);
            // Access the page
            br.getPage(parameter);
            if (parameter.matches(".*?nk\\.pl/profile/\\d+/gallery/[0-9]+")) {
                String finallink = br.getRegex(FINALLINKREGEX1).getMatch(0);
                if (finallink == null) finallink = br.getRegex(FINALLINKREGEX2).getMatch(0);
                if (finallink == null) {
                    logger.warning("Failed to find the finallink(s) for gallery: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            } else {
                br.setFollowRedirects(true);
                String allLinks[] = br.getRegex("\"((/profile/\\d+)?/gallery/(\\d+|album/\\d+/\\d+))\"").getColumn(0);
                if (allLinks == null || allLinks.length == 0) return null;
                progress.setRange(allLinks.length);
                for (String singleLink : allLinks) {
                    br.getPage("http://nk.pl" + singleLink);
                    String finallink = br.getRegex(FINALLINKREGEX1).getMatch(0);
                    if (finallink == null) finallink = br.getRegex(FINALLINKREGEX2).getMatch(0);
                    if (finallink == null) {
                        logger.warning("Failed to find the finallink(s) for gallery: " + parameter);
                        logger.warning("Failed on link: " + singleLink);
                        return null;
                    }
                    decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                    progress.increase(1);
                }
                FilePackage fp = FilePackage.getInstance();
                fp.setName("Gallery " + new Regex(parameter, "nk.pl/profile/(\\d+)").getMatch(0).trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }

    }

    private boolean getUserLogin() throws IOException, DecrypterException {
        br.setFollowRedirects(true);
        // br.getPage(url);
        String username = null;
        String password = null;
        br.getPage(MAINPAGE);
        synchronized (LOCK) {
            username = this.getPluginConfig().getStringProperty("user", null);
            password = this.getPluginConfig().getStringProperty("pass", null);
            if (username != null && password != null) {
                br.postPage(POSTPAGE, "login=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password) + "&remember=1&ssl=1");
            }
            for (int i = 0; i < 3; i++) {
                if (br.getCookie(MAINPAGE, "remember_me") == null || br.getCookie(MAINPAGE, "lltkck") == null) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + DOMAIN + " :");
                    if (username == null) return false;
                    password = UserIO.getInstance().requestInputDialog("Enter password for " + DOMAIN + " :");
                    if (password == null) return false;
                    br.postPage(POSTPAGE, "login=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password) + "&remember=1&ssl=1");
                } else {
                    this.getPluginConfig().setProperty("user", username);
                    this.getPluginConfig().setProperty("pass", password);
                    this.getPluginConfig().save();
                    return true;
                }

            }
        }
        throw new DecrypterException("Login or/and password wrong");
    }

}
