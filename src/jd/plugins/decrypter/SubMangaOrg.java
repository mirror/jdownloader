//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

/**
 * they seem to rip images from other manga sites, eg. mangapark scans within bleach scans.
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "submanga.com" }, urls = { "http://(?:www\\.)?submanga\\.org/(leer/[a-zA-Z0-9]+-\\d+\\.html|r/[a-z]+/[a-z]+/\\d+/\\d+)" }) 
public class SubMangaOrg extends antiDDoSForDecrypt {

    public SubMangaOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        if (parameter.contains("submanga.org/leer/")) {
            // we need another page grab to goto /r/ link
            getPage(parameter);
            parameter = br.getRegex("https?://(?:www\\.)?submanga\\.org/r/[a-z]+/[a-z]+/\\d+/").getMatch(-1);
        } else {
            parameter = new Regex(parameter, "https?://(?:www\\.)?submanga\\.org/r/[a-z]+/[a-z]+/\\d+/").getMatch(-1);
        }
        if (parameter == null) {
            return null;
        }
        // first page will contain adblock static image, and not the true cover which users might want.
        // they use logical page numbers in sequence, so we just need to find the image pattern and page numbers and construct image url
        getPage(parameter.concat("2"));
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/404")) {
            logger.warning("Invalid URL or Offline link! : " + parameter);
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // We get the title
        final String title = br.getRegex("<title>(.*?) (?:-|—|&mdash;)[^<]+</title>").getMatch(0);
        if (title == null || title.length() == 0) {
            logger.warning("Title not found! : " + parameter);
            return null;
        }
        final String useTitle = title.replace("Â·", ".");

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(useTitle);

        // how many pages?
        final String pages = br.getRegex("<option[^>]+>(\\d+)</option>\\s+</select>").getMatch(0);
        if (pages == null) {
            return null;
        }
        final int pageNumber = Integer.parseInt(pages);
        final ArrayList<String> imgs = new ArrayList<String>();
        boolean coverfix = false;
        for (int i = 2; i <= pageNumber; i++) {
            if (i > 2) {
                getPage(parameter + i);
            }
            // grab the image source
            final String img = br.getRegex("<img src=\"(https?://(?:\\w+\\.)?submanga\\.org/[^\"]+)\" class=\"chapter-img\">").getMatch(0);
            if (img == null) {
                return null;
            }
            if (i == 2 && img.contains("2.jpg")) {
                // first page can adblock warning instead of proper cover, lets try and download correct one. but since there isn't any
                // logical file patterns to images we can try the following.
                final String image1 = img.replace("2.jpg", "1.jpg");
                imgs.add(image1);
                coverfix = true;
            }
            imgs.add(img);
        }

        // lets now format and return results
        DecimalFormat df_page = new DecimalFormat("00");
        if (pageNumber > 999) {
            df_page = new DecimalFormat("0000");
        } else if (pageNumber > 99) {
            df_page = new DecimalFormat("000");
        }
        int i = coverfix ? 0 : 1;
        for (final String img : imgs) {
            DownloadLink link = createDownloadlink("directhttp://" + img);
            link.setFinalFileName((useTitle + "_–_page_" + df_page.format(++i) + ".jpg").replace(" ", "_"));
            link.setAvailable(true); // fast add
            fp.add(link);
            decryptedLinks.add(link);
        }
        logger.info("Task Complete! : " + parameter);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}