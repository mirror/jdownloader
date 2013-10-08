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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cybersansar.com" }, urls = { "http://(www\\.)?cybersansar\\.com/(thumbnail_view\\.php\\?gal_id=\\d+|wallpaper_download\\.php\\?wid=\\d+|video_download\\.php\\?vid=\\d+)" }, flags = { 0 })
public class CyberSansarCom extends PluginForDecrypt {

    public CyberSansarCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String VIDEOLINK     = "http://(www\\.)?cybersansar\\.com/video_download\\.php\\?vid=\\d+";
    private static final String THUMBNAILLINK = "http://(www\\.)?cybersansar\\.com/thumbnail_view\\.php\\?gal_id=\\d+";
    private static final String WALLPAPERLINK = "http://(www\\.)?cybersansar\\.com/wallpaper_download\\.php\\?wid=\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (parameter.matches(WALLPAPERLINK)) {
            final String wallpaper1 = br.getRegex("\"(graphics/wallpaper/model/[^<>\"]*?\\.jpg)\"").getMatch(0);
            final String wallpaper2 = br.getRegex("\"(product_thumb\\.php\\?img=[^<>\"]*?\\.jpg\\&amp;w=\\d+\\&amp;h=\\d+)\"").getMatch(0);
            final String wallpaper_finalfilename = new Regex(wallpaper2, "(\\d+\\.jpg)").getMatch(0);
            DownloadLink w2 = createDownloadlink("directhttp://http://cybersansar.com/" + Encoding.htmlDecode(wallpaper2));
            if (wallpaper_finalfilename != null) w2.setFinalFileName(wallpaper_finalfilename);
            decryptedLinks.add(createDownloadlink("directhttp://http://cybersansar.com/" + Encoding.htmlDecode(wallpaper1)));
            decryptedLinks.add(w2);
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode("Wallpapers_" + new Regex(parameter, "(\\d+)$").getMatch(0)));
            fp.addLinks(decryptedLinks);
        } else if (parameter.matches(VIDEOLINK)) {
            String externID = br.getRegex("(youtube\\.com/embed/[^<>\"/]*?)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://" + externID));
                return decryptedLinks;
            }
            externID = br.getRegex("\"(videos/[^<>\"/]*?)\">Download Now</a>").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.cybersansar.com" + externID));
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        } else {
            final Regex fpName = br.getRegex(">Gallery </span><span class=\"model\\-title\\-grey\">(\\d+)</span> <span class=\"model\\-title\\-grey\\-small\">of</span> <span class=\"model\\-title\\-grey\">([^<>\"]*?)</span>");
            final String model_name = fpName.getMatch(1);
            final String gallery_num = fpName.getMatch(0);
            final String[] thumbnails = br.getRegex("class=\"photolink\"><img src=\"(graphics/[^<>\"]*?\\.jpg)\"").getColumn(0);
            if ((thumbnails == null || thumbnails.length == 0) && (model_name != null && gallery_num != null)) {
                logger.info("Gallery is empty: " + parameter);
                return decryptedLinks;
            }
            if (thumbnails == null || thumbnails.length == 0 || model_name == null || gallery_num == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            DecimalFormat df = new DecimalFormat("0000");
            int counter = 1;
            for (String singleLink : thumbnails) {
                final DownloadLink dl = createDownloadlink("directhttp://http://cybersansar.com/" + Encoding.htmlDecode(singleLink).replace("/thumb/", "/"));
                dl.setFinalFileName(model_name + "_" + gallery_num + "_" + df.format(counter) + ".jpg");
                decryptedLinks.add(dl);
                counter++;
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}