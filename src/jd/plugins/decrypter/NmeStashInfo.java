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
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animestash.info" }, urls = { "http://(www\\.)?animestash\\.info/downloads/go/\\d+" }, flags = { 0 })
public class NmeStashInfo extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static final Object LOCK = new Object();

    public NmeStashInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String MAINPAGE = "http://animestash.info/";
    private static final String POSTPAGE = "http://animestash.info/forum/index.php?action=login2";
    private static final String DOMAIN   = "animestash.info";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(false);
        synchronized (LOCK) {
            if (!getUserLogin()) return null;
            br.getHeaders().put("Referer", MAINPAGE);
            br.setFollowRedirects(false);
            br.getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }

    }

    private boolean getUserLogin() throws IOException, DecrypterException {
        br.setFollowRedirects(true);
        String username = null;
        String password = null;
        synchronized (LOCK) {
            username = this.getPluginConfig().getStringProperty("user", null);
            password = this.getPluginConfig().getStringProperty("pass", null);
            if (username != null && password != null) {
                br.postPage(POSTPAGE, "user=" + Encoding.urlEncode(username) + "&passwrd=" + Encoding.urlEncode(password) + "&cookielength=2&hash_passwrd=");
            }
            for (int i = 0; i < 3; i++) {
                boolean valid = false;
                final Cookies allCookies = this.br.getCookies(MAINPAGE);
                for (final Cookie c : allCookies.getCookies()) {
                    if (c.getKey().contains("SMFCookie")) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + DOMAIN + " :");
                    if (username == null) return false;
                    password = UserIO.getInstance().requestInputDialog("Enter password for " + DOMAIN + " :");
                    if (password == null) return false;
                    br.postPage(POSTPAGE, "user=" + Encoding.urlEncode(username) + "&passwrd=" + Encoding.urlEncode(password) + "&cookielength=2&hash_passwrd=");
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
