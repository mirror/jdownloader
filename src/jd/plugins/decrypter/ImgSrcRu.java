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
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgsrc.ru" }, urls = { "http://(www\\.)?imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html" }, flags = { 2 })
public class ImgSrcRu extends PluginForDecrypt {

    private static final String MAINPAGE = "http://imgsrc.ru";

    public ImgSrcRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> allPages = new ArrayList<String>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "lang", "en");
        br.getPage(parameter + "?per_page=48");
        if (br.containsHTML(">Album foreword:")) {
            final String newLink = br.getRegex(">shortcut\\.add\\(\"Right\",function\\(\\) \\{window\\.location=\\'(http://imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html\\?pwd=)\\'").getMatch(0);
            if (newLink == null) return null;
            parameter = newLink;
            br.getPage(parameter);
        }
        if (br.containsHTML("(>Search for better photos|No htmlCode read)") || br.getURL().contains("imgsrc.ru/main/user.php")) return decryptedLinks;
        final String fpName = br.getRegex("from \\'<strong>([^<>\"']+)</strong>").getMatch(0);
        final String username = new Regex(parameter, "imgsrc\\.ru/([^<>\"\\'/]+)/").getMatch(0);
        String[] pages = br.getRegex("href=(/" + username + "/\\d+\\.html)>\\d+</a>").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (String page : pages)
                allPages.add(page);
        }
        allPages.add(parameter.replaceAll("http://(www\\.)?imgsrc.ru", ""));
        for (String page : allPages) {
            br.getPage(MAINPAGE + page);
            // Get the picture we're currently viewing
            String singlePic = br.getRegex("abuse\\.php\\?id=(\\d+)\\&").getMatch(0);
            if (singlePic == null) singlePic = br.getRegex("onclick=\"t\\(\\'down_(\\d+)\\'\\)").getMatch(0);
            if (singlePic != null) {
                DownloadLink dlink = getDownloadLink();
                if (dlink != null) decryptedLinks.add(dlink);
            }
            final String[] allPics = br.getRegex("<a href=\\'(/" + username + "/\\d+\\.html)").getColumn(0);
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
                DownloadLink dlink = getDownloadLink();
                if (dlink != null) decryptedLinks.add(dlink);
                counter++;
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String name = "";
        if (username != null) {
            name = username + " ";
        }
        if (fpName != null) {
            name = name + fpName;
        }
        if (name != null && name.length() > 0) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(name.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private DownloadLink getDownloadLink() {
        final String finallink = br.getRegex("style=\\'\\{width:610;\\}\\' value=\\'\\&lt;a href=http://imgsrc\\.ru>\\&lt;img src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) return null;
        return createDownloadlink("directhttp://" + finallink);
    }
}
