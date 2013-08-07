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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "parteeey.de" }, urls = { "https?://(www\\.)?parteeey\\.de/Galerie/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class ParteeeyDeGallery extends PluginForDecrypt {

    public ParteeeyDeGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static Object LOCK = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString() + "?oF=f.date&oD=asc&eP=1000";
        if (!getUserLogin(param)) {
            logger.info("Invalid logindata, stopping... " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.containsHTML(">Seite nicht gefunden<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<div class=\"boxTitle\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
        int currentMaxPage = 1;
        final String[] pages = br.getRegex("p=\\d+\">(\\d+)</a>").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String page : pages) {
                final int p = Integer.parseInt(page);
                if (p > currentMaxPage) currentMaxPage = p;
            }
        }
        int counter = 1;
        final DecimalFormat df = new DecimalFormat("0000");
        for (int i = 1; i <= currentMaxPage; i++) {

            try {
                if (this.isAbort()) {
                    logger.info("User aborted decryption for link: " + parameter);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {

            }

            logger.info("Decrypting site " + i + " / " + currentMaxPage);
            if (i > 1) {
                br.getPage(parameter + "&p=" + i);
            }
            final String[] links = br.getRegex("\"(files/mul/galleries/\\d+/thumbnails/[^<>\"]*?\\.jpg)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                final String finallink = "directhttp://https://www.parteeey.de/" + singleLink.replace("/thumbnails/", "/");
                final String finalfilename = df.format(counter) + "_" + new Regex(finallink, "([^<>\"/]*?)$").getMatch(0);
                final DownloadLink dl = createDownloadlink(finallink);
                dl.setFinalFileName(finalfilename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                counter++;
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    @SuppressWarnings("unchecked")
    private boolean getUserLogin(final CryptedLink param) throws Exception {
        synchronized (LOCK) {
            final Object ret = this.getPluginConfig().getProperty("cookies", null);
            if (ret != null) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    this.br.setCookie("http://www.parteeey.de/", key, value);
                }
            } else {
                String username = this.getPluginConfig().getStringProperty("username", null);
                String password = this.getPluginConfig().getStringProperty("password", null);
                if (username == null || password == null) {
                    username = getUserInput("Username for parteeey.de?", param);
                    password = getUserInput("Password for parteeey.de?", param);
                }
                br.postPage("https://www.parteeey.de/Login", "loginData%5BauthsysAuthProvider%5D%5BrememberLogin%5D=on&sent=true&url=%2F&usedProvider=authsysAuthProvider&loginData%5BauthsysAuthProvider%5D%5Busername%5D=" + Encoding.urlEncode(username) + "&loginData%5BauthsysAuthProvider%5D%5Bpassword%5D=" + Encoding.urlEncode(password));
                if (br.containsHTML("Ihre Login\\-Daten sind ungültig") || br.getCookie("http://www.parteeey.de/", "identifier") == null) return false;
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://www.parteeey.de/");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                this.getPluginConfig().setProperty("cookies", cookies);
                this.getPluginConfig().setProperty("username", username);
                this.getPluginConfig().setProperty("password", password);
            }
            this.getPluginConfig().save();
            return true;
        }
    }
}
