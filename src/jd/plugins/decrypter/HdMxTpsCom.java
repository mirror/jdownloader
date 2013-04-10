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

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hdmixtapes.com" }, urls = { "http://(www\\.)?hdmixtapes\\.com/(newsingles|mixtape(s)?)/.+" }, flags = { 0 })
public class HdMxTpsCom extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static Object LOCK = new Object();

    public HdMxTpsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String POSTPAGE     = "http://hdmixtapes.com/Sign_in";
    private static final String DOMAIN       = "hdmixtapes.com";
    private static final String INVALIDLOGIN = "(;Invalid username or password<|>The following errors have ocured while proccesing your request:<)";

    // private static final String LOGINPAGE = "http://up.tl/login";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setReadTimeout(60 * 1000);
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        String finallink = null;
        if (parameter.contains("/login/") || br.containsHTML("hdmixtapes\\.com/Create_account")) {
            synchronized (LOCK) {
                if (!getUserLogin()) {
                    logger.info("Invalid logindata!");
                    return decryptedLinks;
                }
                br.setFollowRedirects(false);
                // Access the page
                br.getPage(parameter);
                finallink = br.getRegex("<br class=\"clearfloat\">[\t\n\r ]+<a href=\"(http.*?)\"").getMatch(0);
            }
        }
        if (br.containsHTML("<title> \\-  // Free Download @ HDMixtapes\\.com </title>")) return decryptedLinks;
        if (finallink == null) {
            finallink = br.getRegex("<div style=\"margin-top:40px;\">[\t\n\r ]+<a href=\"(http.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<\\!--<a href=\"(http://.*?)\"").getMatch(0);
                if (finallink == null) finallink = br.getRegex("\\d+\\&url=(http://.*?)\"").getMatch(0);
            }
        }
        if (finallink == null) {
            logger.warning("Failed to find the finallink(s) for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    private boolean getUserLogin() throws IOException, DecrypterException {
        br.setFollowRedirects(true);
        // br.getPage(url);
        String username = null;
        String password = null;
        // br.getPage(LOGINPAGE);
        synchronized (LOCK) {
            username = this.getPluginConfig().getStringProperty("user", null);
            password = this.getPluginConfig().getStringProperty("pass", null);
            if (username != null && password != null) {
                br.postPage(POSTPAGE, "go_login=true&x=&y=&username=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password));
            }
            for (int i = 0; i < 3; i++) {
                if (br.containsHTML(INVALIDLOGIN) || username == null && password == null) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + DOMAIN + " :");
                    if (username == null) return false;
                    password = UserIO.getInstance().requestInputDialog("Enter password for " + DOMAIN + " :");
                    if (password == null) return false;
                    br.postPage(POSTPAGE, "go_login=true&x=&y=&username=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password));
                } else {
                    this.getPluginConfig().setProperty("user", username);
                    this.getPluginConfig().setProperty("pass", password);
                    this.getPluginConfig().save();
                    return true;
                }

            }
        }
        throw new DecrypterException("Login or/and password for " + DOMAIN + " is wrong!");
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}