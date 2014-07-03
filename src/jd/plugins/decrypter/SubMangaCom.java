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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "submanga.com" }, urls = { "http://(www\\.)?submanga\\.com/(c/\\d+|[\\w_\\-\\[\\]]+/\\d+/\\d+)" }, flags = { 0 })
public class SubMangaCom extends PluginForDecrypt {

    /**
     * @author raztoki
     */
    public SubMangaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // DEV NOTES
    // other: decided to write like unixmanga.

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String uid = new Regex(param.toString(), "(?:/[\\w_\\-\\[\\]]+/\\d+/|/c/)(\\d+)").getMatch(0);
        if (uid == null) {
            logger.warning("Could not find 'uid'");
            return null;
        }
        final String parameter = "http://submanga.com/c/" + uid;
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/404")) {
            logger.warning("Invalid URL! : " + parameter);
            return null;
        }
        // We get the title
        final String[] title = br.getRegex("<strong>submanga\\.com</strong></a> \\&rsaquo; <a href=\"[^>]+>[^<]+</a> \\&rsaquo; <a href=\"[^>]+>([^<]+)</a> \\&rsaquo; <a href=\"[^>]+>([^<]+)</a></td><th width=\"1%\">").getRow(0);
        if (title == null || title.length == 0) {
            logger.warning("Title not found! : " + parameter);
            return null;
        }
        String useTitle = (title[0] + "_" + title[1]).replace("Â·", ".");
        // grab the total pages within viewer
        String totalPages = br.getRegex("(?i)<option value=\"(\\d+)\">\\d+</option></select>").getMatch(0);
        if (totalPages == null) {
            logger.warning("'totalPages' not found! : " + parameter);
            return null;
        }
        int numberOfPages = Integer.parseInt(totalPages);
        DecimalFormat df_page = new DecimalFormat("00");
        if (numberOfPages > 999) {
            df_page = new DecimalFormat("0000");
        } else if (numberOfPages > 99) {
            df_page = new DecimalFormat("000");
        }

        FilePackage fp = FilePackage.getInstance();
        fp.setName(useTitle);

        // We load each page and retrieve the URL of the picture
        for (int i = 1; i <= numberOfPages; i++) {
            String pageNumber = df_page.format(i);
            // grab the image source
            String img = br.getRegex("https?://\\w+.submanga\\.com/pages/(\\d+/){1,}" + uid + "\\w+/\\d+\\.\\w{1,4}").getMatch(-1);
            if (img == null) {
                logger.warning("No images found for page : " + pageNumber + " : " + parameter);
                logger.warning("Continuing...");
                if (i != numberOfPages) {
                    // load next page for the 'for' loop.
                    br.getPage(parameter + "/" + (i + 1));
                }
                continue;
            }
            String extension = img.substring(img.lastIndexOf("."));
            DownloadLink link = createDownloadlink("directhttp://" + img);
            link.setFinalFileName((useTitle + "_–_page_" + pageNumber + extension).replace(" ", "_"));
            link.setAvailable(true); // fast add
            fp.add(link);
            try {
                distribute(link);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(link);
            if (i != numberOfPages) {
                // load next page for the 'for' loop.
                br.getPage(parameter + "/" + (i + 1));
            }
        }
        logger.warning("Task Complete! : " + parameter);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}