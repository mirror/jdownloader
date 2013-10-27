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
import java.util.HashSet;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jheberg.com" }, urls = { "http://(www\\.)?jheberg\\.net/(captcha|download|mirrors)/[A-Z0-9a-z\\.\\-_]+" }, flags = { 0 })
public class JBbergCom extends PluginForDecrypt {

    public JBbergCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String  COOKIE_HOST  = "http://jheberg.net/";
    /* must be static so all plugins share same lock */
    private static Object LOCK         = new Object();
    private final Integer MAXCOOKIEUSE = 50;
    private String        agent        = null;

    private Browser prepBrowser(Browser prepBr) {
        prepBr.setFollowRedirects(true);
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent);
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        HashSet<String> filter = new HashSet<String>();

        String parameter = param.toString();
        prepBrowser(br);
        // boolean loggedIn = false;
        // Login function is probably broken, maybe not even needed anymore
        // if (!this.getPluginConfig().getBooleanProperty("skiplogin")) loggedIn = getUserLogin();
        br.getPage(parameter);
        if (br.containsHTML(">Oh non, vous avez tué Kenny|>Dommage, la page demandée n'existe pas")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        } else if (br.containsHTML("<div class=\"title\">Erreur 404</div>|<div id=\"http-error\">404\\.</div>")) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }

        final String fpName = br.getRegex("file-name\">([^<>\"]+)</h1>").getMatch(0);
        final String linkID = new Regex(parameter, "/(captcha|download|mirrors)/(.*)").getMatch(1);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(linkID);
        if (fpName != null) fp.setName(Encoding.htmlDecode(fpName.trim()));

        br.getPage(parameter.replaceFirst("/(captcha|download)/", "/mirrors/"));
        String[] results = br.getRegex("(<div class=\"host_dl\".*?</span>[\r\n\t ]+</div>)").getColumn(0);
        if (results == null || results.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String result : results) {
            String host = new Regex(result, "class=\"host_dl\" id=\"([^\"]+)\">").getMatch(0);
            if (host == null) {
                continue;
            }
            final String hoster = "/redirect/" + linkID + "/" + host + "/";
            if (filter.add(hoster) == false) continue;
            if (result.contains("Hébergeur indisponible")) continue;
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(false);
            br2.getPage(hoster);
            final String finallink = br2.getRedirectLocation();
            // not sure of best action here, but seems some are either down or require account?. Continue with the results
            if (br2.containsHTML("url\"\\s*:\\s*\"not authorized\"") || br2.containsHTML("\"url\"\\s*:\\s*\"\"") || br2.containsHTML("<title>404 Not Found</title>")) {
                continue;
            }
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink(finallink.replace("\\", ""));
            dl._setFilePackage(fp);
            try {
                distribute(dl);
            } catch (final Throwable e) {
                // Not available in old Stable 0.9851
            }
            decryptedLinks.add(dl);
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}