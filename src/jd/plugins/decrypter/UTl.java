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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up.tl" }, urls = { "http://[\\w\\.]*?up\\.tl/download/[a-z0-9]+/.*?\\.html" }, flags = { 0 })
public class UTl extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static final Object LOCK = new Object();

    public UTl(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String MAINPAGE  = "http://up.tl/";
    private static final String POSTPAGE  = "http://up.tl/login/efetuar";
    private static final String DOMAIN    = "up.tl";
//    private static final String LOGINPAGE = "http://up.tl/login";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        synchronized (LOCK) {
            if (!getUserLogin()) return null;
            br.setFollowRedirects(false);
            // Access the page
            br.getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Failed to find the finallink(s) for gallery: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }

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
                br.postPage(POSTPAGE, "login=" + Encoding.urlEncode(username) + "&senha=" + Encoding.urlEncode(password) + "&salvar=1&entrar=Entrar");
            }
            for (int i = 0; i < 3; i++) {
                if (br.getCookie(MAINPAGE, "u") == null || br.getCookie(MAINPAGE, "token") == null) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + DOMAIN + " :");
                    if (username == null) return false;
                    password = UserIO.getInstance().requestInputDialog("Enter password for " + DOMAIN + " :");
                    if (password == null) return false;
                    br.postPage(POSTPAGE, "login=" + Encoding.urlEncode(username) + "&senha=" + Encoding.urlEncode(password) + "&salvar=1&entrar=Entrar");
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

}
