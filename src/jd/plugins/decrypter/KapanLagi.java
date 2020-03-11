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
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kapanlagi.com" }, urls = { "http://(www\\.)?kapanlagi\\.com/foto/selebriti/[a-z]+/[a-z]/[a-z0-9\\_]+/[a-z0-9\\-]+\\.html" })
public class KapanLagi extends PluginForDecrypt {

    public KapanLagi(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Put error message here")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches(".+/foto/selebriti/.+")) {
            // getMore();
            // getLess();
            int x = 1;
            while (x < 100) {
                String left = br.getRegex("\"pagination\">\\s*?<a href=\"([^<>\"]+)\" class=\"arrow\">").getMatch(0);
                if (left != null) {
                    // http://cdn.klimg.com/kapanlagi.com/selebriti/([a-z]+/)?[a-z0-9\\_]+/p/[a-z0-9\\-]+\\.jpg - preview pic
                    /*
                     * String pic = br.getRegex("besar\"><img src=\"([^<>\"]+\\.jpg)").getMatch(0); if (pic == null) {
                     * logger.warning("Decrypter broken 1 for link: " + parameter); return null; } logger.info("pic: " + pic);
                     * decryptedLinks.add(createDownloadlink(pic));
                     */
                    String bigl = br.getRegex("<a href=\"(http://foto[^<>\"]+)\" title=\"Klik").getMatch(0);
                    br.getPage(bigl);
                    String bigp = br.getRegex("bigphoto\" src=\"([^<>\"]+)\"").getMatch(0);
                    if (bigp == null) {
                        logger.warning("Decrypter broken 2 for link: " + parameter);
                        return null;
                    }
                    logger.info("bigp: " + bigp);
                    decryptedLinks.add(createDownloadlink(bigp));
                    left = "http://www.kapanlagi.com" + left;
                    br.getPage(left);
                    logger.info("left: " + left);
                    logger.info("x = " + x);
                    x++;
                    Thread.sleep(100);
                    // decryptedLinks.add(createDownloadlink(bigp));
                } else {
                    x = 1000;
                    logger.info("left: " + left);
                    logger.info("x = " + x);
                }
            }
        }
        return decryptedLinks;
    }

    private void getMore() throws Exception {
        int x = 1;
        do {
            String left0 = br.getRegex("pagination\"><a href=\"(#)\" class=\"arrow\">").getMatch(0);
            logger.info("left0: " + left0);
            if (left0 != null) {
                x = 1000;
            } else {
                // <a href=\"(/foto/selebriti/-country-/./[a-z0-9\\_]+/[a-z0-9\\-]+\\.html)\">\\d+</a>"
                String more = br.getRegex("<a href=\"([^<>\"]+)\">\\d+</a>").getMatch(0);
                more = "http://www.kapanlagi.com" + more;
                br.getPage(more);
                logger.info("x was " + x);
                logger.info("more(?) was " + more);
                x++;
                Thread.sleep(100);
            }
        } while (x < 1000);
    }

    private void getLess() throws Exception {
        int x = 1;
        do {
            String less = br.getRegex("href=\"([^<>\"]+)\">\\d+</a> <a href=\"[^<>\"]+\" class=\"arrow\">").getMatch(0);
            if (less != null) {
                less = "http://www.kapanlagi.com" + less;
                br.getPage(less);
                logger.info("x was " + x);
                logger.info("less was " + less);
                x++;
                Thread.sleep(100);
            } else {
                x = 1000;
            }
        } while (x < 1000);
    }

}