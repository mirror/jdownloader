//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pimpandhost.com", "turboimagehost.com", "imagehyper.com", "imagebam.com", "photobucket.com", "freeimagehosting.net", "pixhost.org", "pixhost.info", "picturedumper.com", "imagetwist.com", "sharenxs.com" }, urls = { "http://(www\\.)?pimpandhost\\.com/image/(show/id/\\d+|\\d+\\-(original|medium|small)\\.html)", "http://(www\\.)?turboimagehost\\.com/p/\\d+/.*?\\.html", "http://(www\\.)?img\\d+\\.imagehyper\\.com/img\\.php\\?id=\\d+\\&c=[a-z0-9]+", "http://[\\w\\.]*?imagebam\\.com/(image|gallery)/[a-z0-9]+", "http://[\\w\\.]*?media\\.photobucket.com/image/.+\\..{3,4}\\?o=[0-9]+", "http://[\\w\\.]*?freeimagehosting\\.net/image\\.php\\?.*?\\..{3,4}", "http://(www\\.)?pixhost\\.org/show/\\d+/.+", "http://(www\\.)?pixhost\\.info/pictures/\\d+", "http://(www\\.)?picturedumper\\.com/picture/\\d+/[a-z0-9]+/",
        "http://(www\\.)?imagetwist\\.com/[a-z0-9]{12}", "http://(www\\.)?sharenxs\\.com/view/\\?id=[a-z0-9-]+" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class ImageHosterDecrypter extends PluginForDecrypt {

    public ImageHosterDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String finallink = null;
        if (parameter.contains("imagebam.com")) {
            /* Error handling */
            if (br.containsHTML("The gallery you are looking for")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            if (br.containsHTML("Image not found")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            if (parameter.contains("/gallery/")) {
                String name = new Regex(parameter, "/gallery/(.+)").getMatch(0);
                if (name == null) {
                    name = "ImageBamGallery";
                } else {
                    name = "ImageBamGallery_" + name;
                }
                FilePackage fp = FilePackage.getInstance();
                fp.setName(name);
                String pages[] = br.getRegex("class=\"pagination_(current|link)\">(\\d+)<").getColumn(1);
                if (pages != null && pages.length > 0) {
                    for (String page : pages) {
                        br.getPage(parameter + "/" + page);
                        if (br.containsHTML("The gallery you are looking for")) continue;
                        String links[] = br.getRegex("'(http://[\\w\\.]*?imagebam\\.com/image/[a-z0-9]+)'").getColumn(0);
                        for (String link : links) {
                            DownloadLink dl = createDownloadlink(Encoding.htmlDecode(link));
                            decryptedLinks.add(dl);
                        }
                    }
                } else {
                    String links[] = br.getRegex("'(http://[\\w\\.]*?imagebam\\.com/image/[a-z0-9]+)'").getColumn(0);
                    for (String link : links) {
                        DownloadLink dl = createDownloadlink(Encoding.htmlDecode(link));
                        decryptedLinks.add(dl);
                    }
                }
                if (decryptedLinks.size() > 0) {
                    fp.addLinks(decryptedLinks);
                    return decryptedLinks;
                } else {
                    return null;
                }
            }
            finallink = br.getRegex("(\\'|\")(http://\\d+\\.imagebam\\.com/download/.*?)(\\'|\")").getMatch(1);
            if (finallink == null) {
                finallink = br.getRegex("onclick=\"scale\\(this\\);\" src=\"(http://.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("media.photobucket.com")) {
            finallink = br.getRegex("mediaUrl':'(http.*?)'").getMatch(0);
        } else if (parameter.contains("freeimagehosting.net")) {
            /* Error handling */
            if (!br.containsHTML("uploads/")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            finallink = parameter.replace("image.php?", "uploads/");
        } else if (parameter.contains("pixhost.org")) {
            /* Error handling */
            if (!br.containsHTML("images/")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            finallink = br.getRegex("show_image\" src=\"(http.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://img[0-9]+\\.pixhost\\.org/images/[0-9]+/.*?)\"").getMatch(0);
        } else if (parameter.contains("pixhost.info/")) {
            finallink = br.getRegex("border=\\'0\\' src=\\'(http://.*?)\\'>").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\\'(http://pixhost\\.info/avaxhome/[0-9/]+\\.jpeg)\\'").getMatch(0);
        } else if (parameter.contains("picturedumper.com")) {
            finallink = br.getRegex("<img id=\"image\" src=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://picturedumper\\.com/data/.*?)\"").getMatch(0);
        } else if (parameter.contains("imagetwist.com/")) {
            finallink = br.getRegex("\"(http://img\\d+\\.imagetwist\\.com/i/\\d+/.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<p><img src=\"(http://.*?)\"").getMatch(0);
        } else if (parameter.contains("sharenxs.com/")) {
            finallink = br.getRegex("cache\\.sharenxs\\.com/thumbnails/(tn|nxs)-(.*?)\"").getMatch(1);
            if (finallink != null) finallink = "http://cache.sharenxs.com/images/images2/" + finallink;
            if (finallink == null && new Regex(parameter, "id=[0-9a-z]+-[0-9a-z]+").matches()) {
                br.getPage(parameter + "&pjk=l");
                finallink = br.getRegex("view/\\?.*?src=\"(http://cache\\.sharenxs\\.com/images/.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("imagehyper.com/img")) {
            finallink = br.getRegex("imagehyper: dudoso - DO NOT MODIFY \\-\\-></td><td>[\t\n\r ]+<img src=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://img\\d+\\.imagehyper\\.com/img/.*?)\"").getMatch(0);
        } else if (parameter.contains("turboimagehost.com/")) {
            finallink = br.getRegex("<a href=\"http://www\\.turboimagehost\\.com\"><img src=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://s\\d+d\\d+\\.turboimagehost\\.com/sp/[a-z0-9]+/.*?)\"").getMatch(0);
        } else if (parameter.contains("pimpandhost.com/")) {
            String picID = new Regex(parameter, "pimpandhost\\.com/image/show/id/(\\d+)").getMatch(0);
            if (picID == null) picID = new Regex(parameter, "pimpandhost\\.com/image/(\\d+)\\-").getMatch(0);
            br.getPage("http://pimpandhost.com/image/" + picID + "-original.html");
            finallink = br.getRegex("pointer;\" alt=\"\" id=\"image\" src=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://ist\\d+\\-\\d+\\.filesor\\.com/pimpandhost\\.com/.*?)\"").getMatch(0);
        }
        if (finallink == null) {
            logger.warning("Imagehoster-Decrypter broken for link: " + parameter);
            return null;
        }
        finallink = "directhttp://" + finallink;
        DownloadLink dl = createDownloadlink(Encoding.htmlDecode(finallink));
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

}
