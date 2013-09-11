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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagearn.com" }, urls = { "http://(www\\.)?imagearn\\.com//?(gallery|image)\\.php\\?id=\\d+" }, flags = { 0 })
public class ImagEarnCom extends PluginForDecrypt {

    public ImagEarnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String THUMBREGEX = "\"(http://thumbs\\d+\\.imagearn\\.com//?\\d+/\\d+\\.jpg)\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://imagearn.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h3 class=\"page-title\"><strong>([^<>\"/]+)</strong>").getMatch(0);
        if (parameter.contains("imagearn.com/gallery.php?id=")) {
            if (br.containsHTML(">There are no images in this gallery")) {
                logger.info("Link empty: " + parameter);
                return decryptedLinks;
            }
            if (fpName == null) fpName = br.getRegex("<title>(.*?) - Image Earn</title>").getMatch(0);
            if (fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpName = Encoding.htmlDecode(fpName.trim());
            final String[] links = br.getRegex("\"(http://imagearn\\.com/?/image\\.php\\?id=\\d+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            DecimalFormat df = new DecimalFormat("0000");
            int counter = 1;
            for (final String singleLink : links) {
                for (int i = 1; i <= 3; i++) {
                    br.getPage(singleLink);
                    if (br.containsHTML("Do not use autorefresh programs")) {
                        this.sleep(new Random().nextInt(4) * 1000l, param);
                        continue;
                    }
                    break;
                }
                if (br.containsHTML("Do not use autorefresh programs")) {
                    logger.warning("Stopped at: " + singleLink);
                    logger.warning("Decrypter broken for link: " + parameter);
                    logger.info("JD = blocked");
                    return null;
                }
                final String finallink = getDirectlink();
                if (finallink == null) {
                    logger.warning("Stopped at: " + singleLink);
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                dl.setAvailable(true);
                dl.setFinalFileName(fpName + "_" + df.format(counter) + ".jpg");
                decryptedLinks.add(dl);
                counter++;
                sleep(1500, param);
            }
        } else {
            if (fpName == null) fpName = br.getRegex("<title>Image - (.*?) \\- Image Earn</title>").getMatch(0);
            String finallink = getDirectlink();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getDirectlink() {
        String finallink = br.getRegex("<div id=\"image\"><center><a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        if (finallink == null) finallink = br.getRegex("\"(http://img\\.imagearn\\.com//?imags/\\d+/\\d+\\.jpg)\"").getMatch(0);
        return finallink;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}