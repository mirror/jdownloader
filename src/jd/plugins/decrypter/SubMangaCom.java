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

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "submanga.com" }, urls = { "http://(www\\.)?submanga\\.com/(c/\\d+|[\\w_\\-\\[\\]]+/\\d+/\\d+)" }) 
public class SubMangaCom extends PluginForDecrypt {

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
            logger.warning("Invalid URL or Offline link! : " + parameter);
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // We get the title
        final String title = br.getRegex("<title>(.*?) (?:-|—|&mdash;)[^<]+ (?:-|—|&mdash;) submanga</title>").getMatch(0);
        if (title == null || title.length() == 0) {
            logger.warning("Title not found! : " + parameter);
            return null;
        }
        final String useTitle = title.replace("Â·", ".");

        FilePackage fp = FilePackage.getInstance();
        fp.setName(useTitle);

        // we don't know how many pages anymore.. this will over come this.
        int pageNumber = 1;
        final ArrayList<String> imgs = new ArrayList<String>();
        while (true) {
            // grab the image source
            final String img = br.getRegex("https?://\\w+.submanga\\.com/pages/(\\d+/){1,}" + uid + "\\w+/\\d+\\.\\w{1,4}").getMatch(-1);
            if (img == null) {
                break;
            }
            imgs.add(img);
            pageNumber++;
            br.getPage(parameter + "/" + pageNumber);
        }
        // lets now format and return results
        DecimalFormat df_page = new DecimalFormat("00");
        if (imgs.size() > 999) {
            df_page = new DecimalFormat("0000");
        } else if (imgs.size() > 99) {
            df_page = new DecimalFormat("000");
        }
        pageNumber = 0;
        for (final String img : imgs) {
            pageNumber++;
            String extension = img.substring(img.lastIndexOf("."));
            DownloadLink link = createDownloadlink("directhttp://" + img);
            link.setFinalFileName((useTitle + "_–_page_" + df_page.format(pageNumber) + extension).replace(" ", "_"));
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