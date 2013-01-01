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
import java.util.HashSet;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgsrc.ru" }, urls = { "http://(www\\.)?imgsrc\\.(ru|su|ro)/(main/passchk\\.php\\?ad=\\d+|main/preword\\.php\\?ad=\\d+|[^<>\"\\'/]+/[a-z0-9]+\\.html)" }, flags = { 2 })
public class ImgSrcRu extends PluginForDecrypt {

    private static final String MAINPAGE = "http://imgsrc.ru";

    public ImgSrcRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String PASSWORD = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> allPages = new ArrayList<String>();
        String parameter = param.toString().replaceAll("https?://(www\\.)?imgsrc\\.(ru|ro|su)/", "http://imgsrc.ru/");
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE + "/", "lang", "en");
        br.setCookie(MAINPAGE + "/", "iamlegal", "yeah");
        br.setCookie(MAINPAGE + "/", "per_page", "48");

        boolean passwordprotected = false;
        if (parameter.matches("http://(www\\.)?imgsrc\\.ru/main/passchk\\.php\\?ad=\\d+")) {
            br.getPage(parameter);
            passwordprotected = true;
            if (br.containsHTML(">Album foreword:")) {
                final String newLink = br.getRegex(">shortcut\\.add\\(\"Right\",function\\(\\) \\{window\\.location=\\'(http://imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html\\?pwd=)\\'").getMatch(0);
                if (newLink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                parameter = newLink;
                br.getPage(parameter + "?per_page=48");
            }
            parameter = handlePassword(parameter, param);
        } else {
            br.getPage(parameter + "?per_page=48");
            if (br.containsHTML(">Album foreword:")) {
                final String newLink = br.getRegex(">shortcut\\.add\\(\"Right\",function\\(\\) \\{window\\.location=\\'(http://imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html\\?pwd=)\\'").getMatch(0);
                if (newLink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                parameter = newLink;
                br.getPage(parameter + "?per_page=48");
            }
            if (br.containsHTML("No htmlCode read")) {
                logger.info("Server error, cannot continue: " + parameter);
                return decryptedLinks;
            }
            passwordprotected = isPasswordProtected();
        }

        if (br.containsHTML("(>Search for better photos|No htmlCode read)") || br.getURL().contains("imgsrc.ru/main/user.php")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("from \\'<strong>([^<>\']+)</strong>").getMatch(0);
        final String username = new Regex(parameter, "imgsrc\\.ru/([^<>\"\\'/]+)/").getMatch(0);
        final String[] pages = br.getRegex("href=(/" + username + "/\\d+\\.html)>\\d+</a>").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (String page : pages)
                allPages.add(page);
        }
        allPages.add(parameter.replaceAll("http://(www\\.)?imgsrc.ru", ""));
        String name = "";
        if (username != null && fpName != null) {
            name = username.trim() + " @ " + fpName.trim();
        } else if (username != null) {
            name = username.trim();
        } else if (fpName != null) {
            name = fpName.trim();
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName("Gallery: " + new Regex(parameter, "([a-z0-9]+)\\.html$").getMatch(0));
        if (name != null && name.length() > 0) fp.setName(Encoding.htmlDecode(name.trim()));
        int pageCounter = 1;
        for (final String page : allPages) {
            final String currentPage = MAINPAGE + page;
            logger.info("Decrypting page " + pageCounter + " of " + allPages.size() + " and working on line: " + currentPage);
            br.getPage(currentPage);
            // Check password again, because they don't set any cookies for correctly entered passwords we have to enter them again for each
            // page
            if (isPasswordProtected()) handlePassword(parameter, param);
            // Get the picture we're currently viewing
            String singlePic = br.getRegex("abuse\\.php\\?id=(\\d+)\\&").getMatch(0);
            if (singlePic == null) singlePic = br.getRegex("onclick=\"t\\(\\'down_(\\d+)\\'\\)").getMatch(0);
            if (singlePic != null) {
                DownloadLink dlink = getDownloadLink();
                fp.add(dlink);
                if (dlink != null) decryptedLinks.add(dlink);
            }
            // Password protected links contain the "?pwd?" string
            final String[] allPics = br.getRegex("<a href=\\'(/" + username + "/\\d+\\.html(\\?pwd=[a-z0-9]{32})?)").getColumn(0);
            if (allPics == null || allPics.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            int counter = 0;
            HashSet<String> dups = new HashSet<String>();
            for (String pic : allPics) {
                if (!dups.add(pic)) continue;
                if (counter > 10 && decryptedLinks.size() == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                br.getPage(MAINPAGE + pic);
                final DownloadLink dlink = getDownloadLink();
                if (dlink != null) {
                    fp.add(dlink);
                    try {
                        distribute(dlink);
                    } catch (final Exception e) {
                        // Not available in old Stable
                    }
                }
                decryptedLinks.add(dlink);
            }
            counter++;
            pageCounter++;
        }

        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private DownloadLink getDownloadLink() {
        final String finallink = br.getRegex("style=\\'\\{width:610;\\}\\' value=\\'\\&lt;a href=http://imgsrc\\.ru>\\&lt;img src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) return null;
        final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
        dl.setAvailable(true);
        return dl;
    }

    private String handlePassword(String parameter, CryptedLink param) throws Exception {
        Form pwForm = br.getFormbyProperty("name", "passchk");
        if (pwForm == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (int i = 0; i <= 3; i++) {
            if (PASSWORD == null) PASSWORD = this.getPluginConfig().getStringProperty("lastusedpassword");
            if (PASSWORD == null) PASSWORD = getUserInput("Enter password for link: " + param.getCryptedUrl(), param);
            pwForm.put("pwd", PASSWORD);
            br.submitForm(pwForm);
            pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm != null) {
                this.getPluginConfig().setProperty("lastusedpassword", Property.NULL);
                PASSWORD = null;
                continue;
            }
            this.getPluginConfig().setProperty("lastusedpassword", PASSWORD);
            break;
        }
        this.getPluginConfig().save();
        if (pwForm != null) throw new DecrypterException(DecrypterException.PASSWORD);
        // Now we got the correct url
        parameter = br.getURL();
        return parameter;
    }

    private boolean isPasswordProtected() {
        return br.containsHTML(">Album owner has protected his work from unauthorized access");
    }
}
