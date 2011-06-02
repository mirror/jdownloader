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
import java.util.HashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "de.groups.yahoo.com" }, urls = { "http://(www\\.)?de\\.groups\\.yahoo\\.com/group/[a-z0-9]+/(files/([A-Za-z0-9/]+)?|photos/album/\\d+(/pic)?/list)" }, flags = { 0 })
public class DeGroopsYahooCom extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static final Object LOCK = new Object();

    public DeGroopsYahooCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String MAINPAGE = "www.yahoo.com";
    private static final String DOMAIN   = "yahoo.com";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        if (parameter.contains("photos/album/")) parameter += "?mode=tn&count=10000";
        br.setCookiesExclusive(false);
        br.getPage("https://login.yahoo.com/config/login?");
        synchronized (LOCK) {
            if (!getUserLogin()) return null;
            br.getPage(parameter);
            if (br.containsHTML("login\\.yahoo\\.com/login")) {
                this.getPluginConfig().setProperty("cookies", Property.NULL);
                this.getPluginConfig().save();
                logger.warning("Cookies not valid anymore, retrying!");
                decryptedLinks.add(createDownloadlink(parameter));
                return decryptedLinks;
            }
            br.setFollowRedirects(false);
            if (parameter.contains("/files/")) {
                // Handling for file/folderlinks and their names(because they
                // aren't always correect in the links
                String fpName = br.getRegex("\\&gt;[\t\n\r ]+(?!<a href=)(.*?) </h4>").getMatch(0);
                String[][] allLinks = br.getRegex("class=\"title\">[\t\n\r ]+<a href=\"(.*?)\">(.*?)</a>").getMatches();
                if (allLinks == null || allLinks.length == 0) {
                    logger.warning("Failed to find the finallink(s) for link: " + parameter);
                    return null;
                }
                for (String aLink[] : allLinks) {
                    DownloadLink dl = null;
                    if (aLink[0].contains("/group/")) {
                        dl = createDownloadlink("http://de.groups.yahoo.com" + aLink[0]);
                    } else {
                        dl = createDownloadlink("directhttp://" + aLink[0]);
                        if (fpName != null) {
                            FilePackage fp = FilePackage.getInstance();
                            fp.setName(fpName.trim());
                            dl.setFilePackage(fp);
                        } else {
                            logger.warning("fpName regex failed...");
                        }
                        dl.setFinalFileName(aLink[1]);
                    }
                    decryptedLinks.add(dl);
                }
            } else {
                String fpName = br.getRegex("<div class=\"ygrp-box\\-content\">[\t\n\r ]+<h3>(.*?)</h3>").getMatch(0);
                // Handling for photos and albums
                String[] galleries = br.getRegex("<div class=\"ygrp-right-album\">[\t\n\r ]+<div>[\t\n\r]+<a href=\"(/.*?)\"").getColumn(0);
                if (galleries == null || galleries.length == 0) galleries = br.getRegex("<a href=\"(/group/[a-z0-9]+/photos/album/\\d+/pic/list)\"").getColumn(0);
                if (galleries != null && galleries.length != 0) {
                    for (String gallery : galleries) {
                        gallery = "http://de.groups.yahoo.com" + gallery;
                        decryptedLinks.add(createDownloadlink(gallery));
                    }
                }
                String[] photos = br.getRegex("<div class=\"photo\">[\t\n\r ]+<a href=\"(/.*?)\"").getColumn(0);
                if ((photos == null || photos.length == 0) && (galleries == null || galleries.length == 0)) {
                    logger.warning("Failed to find the photolink(s) for link: " + parameter);
                    return null;
                }
                if (photos != null && photos.length != 0) {
                    for (String photo : photos) {
                        photo = "http://de.groups.decryptedhahoo.com" + photo.replaceAll("(picmode=.*?)\\&", "original") + "&picmode=original" + "yahoolink";
                        DownloadLink dl = createDownloadlink(photo);
                        dl.setAvailable(true);
                        // Set this as name else we get problems with simultan
                        // downloads (every link same name), read names are set
                        // on downloadstart
                        dl.setName(Integer.toString(new Random().nextInt(1000000)));
                        if (fpName != null) {
                            FilePackage fp = FilePackage.getInstance();
                            fp.setName(fpName.trim());
                            dl.setFilePackage(fp);
                        } else {
                            logger.warning("fpName regex failed...");
                        }
                        decryptedLinks.add(dl);
                    }
                }
            }
            return decryptedLinks;
        }

    }

    @SuppressWarnings("unchecked")
    private boolean getUserLogin() throws Exception {
        br.setFollowRedirects(true);
        String username = null;
        String password = null;
        Form loginForm = null;
        synchronized (LOCK) {
            username = this.getPluginConfig().getStringProperty("user", null);
            password = this.getPluginConfig().getStringProperty("pass", null);
            if (username != null && password != null) {
                // Load cookies
                final Object ret = this.getPluginConfig().getProperty("cookies", null);
                boolean acmatch = username.matches(this.getPluginConfig().getStringProperty("user"));
                if (acmatch) acmatch = password.matches(this.getPluginConfig().getStringProperty("pass"));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("SSL")) {
                        for (final String key : cookies.keySet()) {
                            this.br.setCookie(MAINPAGE, key, cookies.get(key));
                        }
                        return true;
                    }
                }
                loginForm = br.getFormbyProperty("name", "login_form");
                if (loginForm == null) {
                    logger.warning("Login broken!");
                    return false;
                }
                loginForm.put("login", username);
                loginForm.put("passwd", password);
                br.submitForm(loginForm);
            }
            for (int i = 0; i < 3; i++) {
                if (br.getCookie(MAINPAGE, "PH") == null || br.getCookie(MAINPAGE, "SSL") == null) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + DOMAIN + " :");
                    if (username == null) return false;
                    password = UserIO.getInstance().requestInputDialog("Enter password for " + DOMAIN + " :");
                    if (password == null) return false;
                    loginForm = br.getFormbyProperty("name", "login_form");
                    if (loginForm == null) {
                        logger.warning("Login broken!");
                        return false;
                    }
                    loginForm.put("login", username);
                    loginForm.put("passwd", password);
                    br.submitForm(loginForm);
                } else {
                    // Save login/pass, save cookies, add account to
                    // accountoverview
                    this.getPluginConfig().setProperty("user", username);
                    this.getPluginConfig().setProperty("pass", password);
                    Account acc = new Account(username, password);
                    final PluginForHost yahooHosterplugin = JDUtilities.getPluginForHost("yahoo.com");
                    final HashMap<String, String> cookies = new HashMap<String, String>();
                    final Cookies add = this.br.getCookies(MAINPAGE);
                    for (final Cookie c : add.getCookies()) {
                        cookies.put(c.getKey(), c.getValue());
                    }
                    this.getPluginConfig().setProperty("cookies", cookies);
                    acc.setProperty("name", acc.getUser());
                    acc.setProperty("pass", acc.getPass());
                    acc.setProperty("cookies", cookies);
                    // Add account for the yahoo hosterplugin so user doesn't
                    // have to do it
                    AccountController.getInstance().addAccount(yahooHosterplugin, acc);
                    this.getPluginConfig().save();
                    return true;
                }

            }
        }
        throw new DecrypterException("Login or/and password for " + DOMAIN + " is wrong!");
    }

}
