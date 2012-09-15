//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jheberg.com" }, urls = { "http://(www\\.)?jheberg\\.net/captcha_dl\\.php\\?id=[A-Z0-9]+" }, flags = { 0 })
public class JBbergCom extends PluginForDecrypt {

    public JBbergCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String  COOKIE_HOST  = "http://jheberg.net/";
    /* must be static so all plugins share same lock */
    private static Object LOCK         = new Object();
    private final Integer MAXCOOKIEUSE = 50;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        boolean loggedIn = false;
        if (!this.getPluginConfig().getBooleanProperty("skiplogin")) loggedIn = getUserLogin();
        br.getPage(parameter);
        if (br.containsHTML("(<p><strong>Fichier :</strong> </p>|<p><strong>Date d\\&#146;ajout du fichier :</strong> 01/01/1970 01:00</p>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (!loggedIn) {
            if (!br.containsHTML("capt_img\\.php")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (int i = 0; i <= 3; i++) {
                String code = getCaptchaCode("http://www.jheberg.net/capt_img.php", param);
                br.postPage(parameter, "captcha=" + code);
                if (br.containsHTML("capt_img\\.php")) continue;
                break;
            }
            if (br.containsHTML("capt_img\\.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        final String fpName = br.getRegex("<p><strong>Fichier :</strong> ([^<>\"\\']+)</p>").getMatch(0);
        String[] links = br.getRegex("<a href=\"([A-Za-z0-9]+_[A-Z0-9]+)\" target=\"_blank\">").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String singleLink : links) {
            br.getPage(COOKIE_HOST + singleLink);
            final String finallink = br.getRegex("<META HTTP\\-EQUIV=\"Refresh\" CONTENT=\"\\d+; URL=(http://[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    @SuppressWarnings("unchecked")
    private boolean getUserLogin() throws Exception {
        br.setFollowRedirects(true);
        String username = null;
        String password = null;
        synchronized (LOCK) {
            int loginCounter = this.getPluginConfig().getIntegerProperty("logincounter");
            // Only login every x th time to prevent getting banned | else just
            // set cookies (re-use them)
            if (loginCounter > MAXCOOKIEUSE || loginCounter == -1) {
                username = this.getPluginConfig().getStringProperty("user", null);
                password = this.getPluginConfig().getStringProperty("pass", null);
                for (int i = 0; i < 3; i++) {
                    if (username == null || password == null) {
                        username = UserIO.getInstance().requestInputDialog(JDL.L("plugins.decrypter.jbergcom.login", "Enter login for jheberg.com or enter nothing if you don't wish to use an account."));
                        if (username == null) return false;
                        /**
                         * User doesn't want login, skip it completely next time
                         */
                        if (username.equals("")) {
                            this.getPluginConfig().setProperty("skiplogin", "true");
                            return false;
                        }
                        password = UserIO.getInstance().requestInputDialog(JDL.L("plugins.decrypter.jbergcom.password", "Enter password for jheberg.com:"));
                        if (password == null) return false;
                    }
                    if (!loginSite(username, password)) {
                        break;
                    } else {
                        if (loginCounter > MAXCOOKIEUSE) {
                            loginCounter = 0;
                        } else {
                            loginCounter++;
                        }
                        this.getPluginConfig().setProperty("user", username);
                        this.getPluginConfig().setProperty("pass", password);
                        this.getPluginConfig().setProperty("logincounter", loginCounter);
                        final HashMap<String, String> cookies = new HashMap<String, String>();
                        final Cookies add = this.br.getCookies(COOKIE_HOST);
                        for (final Cookie c : add.getCookies()) {
                            cookies.put(c.getKey(), c.getValue());
                        }
                        this.getPluginConfig().setProperty("cookies", cookies);
                        this.getPluginConfig().save();
                        return true;
                    }

                }
            } else {
                final Object ret = this.getPluginConfig().getProperty("cookies", null);
                if (ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    for (Map.Entry<String, String> entry : cookies.entrySet()) {
                        this.br.setCookie(COOKIE_HOST, entry.getKey(), entry.getValue());
                    }
                    loginCounter++;
                    this.getPluginConfig().setProperty("logincounter", loginCounter);
                    this.getPluginConfig().save();
                    return true;
                }
            }
        }
        this.getPluginConfig().setProperty("user", Property.NULL);
        this.getPluginConfig().setProperty("pass", Property.NULL);
        this.getPluginConfig().setProperty("logincounter", "-1");
        this.getPluginConfig().setProperty("cookies", Property.NULL);
        this.getPluginConfig().save();
        throw new DecrypterException("Login or/and password for " + COOKIE_HOST + " is wrong!");
    }

    private boolean loginSite(String username, String password) throws Exception {
        br.postPage("http://www.jheberg.net/login.html", "pseudo=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password));
        br.getPage("http://www.jheberg.net/account.html");
        if (!br.containsHTML("<h1>Bonjour " + username)) return false;
        return true;
    }
}
