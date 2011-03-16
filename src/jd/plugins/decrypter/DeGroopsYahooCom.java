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

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

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
        br.setCookiesExclusive(false);
        br.getPage("https://login.yahoo.com/config/login?");
        synchronized (LOCK) {
            if (!getUserLogin()) return null;
            br.getPage(parameter);
            br.setFollowRedirects(false);
            if (parameter.contains("/files/")) {
                // Handling for filelinks
                String[] allLinks = br.getRegex("class=\"title\">[\t\n\r ]+<a href=\"(.*?)\"").getColumn(0);
                if (allLinks == null || allLinks.length == 0) {
                    logger.warning("Failed to find the finallink(s) for link: " + parameter);
                    return null;
                }
                for (String aLink : allLinks) {
                    if (aLink.contains("/group/")) {
                        aLink = "http://de.groups.yahoo.com" + aLink;
                    } else {
                        aLink = "directhttp://" + aLink;
                    }
                    decryptedLinks.add(createDownloadlink(aLink));
                }
            } else {
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
                    progress.setRange(photos.length);
                    for (String photo : photos) {
                        photo = "http://de.groups.yahoo.com" + photo;
                        br.getPage(photo);
                        String finallink = br.getRegex("class=\"ygrp-photos-body-image\" style=\"height: 430px;\"><img src=\"(http://.*?)\"").getMatch(0);
                        if (finallink == null) finallink = br.getRegex("\"(http://xa\\.yimg\\.com/kq/groups/\\d+/.*?)\"").getMatch(0);
                        if (finallink == null) {
                            logger.warning("Couldn't find final-photolink for detailed-link: " + photo + " for mainlink: " + parameter);
                            return null;
                        }
                        decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                        progress.increase(1);
                    }
                }
            }
            FilePackage fp = FilePackage.getInstance();
            fp.setName(new Regex(parameter, "yahoo\\.com/group/(.*?)/").getMatch(0));
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        }

    }

    private boolean getUserLogin() throws Exception {
        br.setFollowRedirects(true);
        String username = null;
        String password = null;
        synchronized (LOCK) {
            username = this.getPluginConfig().getStringProperty("user", null);
            password = this.getPluginConfig().getStringProperty("pass", null);
            Form loginForm = br.getFormbyProperty("name", "login_form");
            if (loginForm == null) {
                logger.warning("Login broken!");
                return false;
            }
            if (username != null && password != null) {
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
                    this.getPluginConfig().setProperty("user", username);
                    this.getPluginConfig().setProperty("pass", password);
                    this.getPluginConfig().save();
                    // br.getPage(parameter);
                    return true;
                }

            }
        }
        throw new DecrypterException("Login or/and password for " + DOMAIN + " is wrong!");
    }

}
