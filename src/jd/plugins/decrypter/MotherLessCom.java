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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "motherless.com" }, urls = { "http://([\\w\\.]*?|members\\.)motherless\\.com/((?!movies|thumbs|uploads|gi/)\\w).+" }, flags = { 0 })
public class MotherLessCom extends PluginForDecrypt {

    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String REPLACE = "motherlessmedia";

    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String fpName = parameter.getStringProperty("package");
        br.setFollowRedirects(true);
        br.getPage(parameter.toString());
        if (br.containsHTML("Not Available") || br.containsHTML("not found") || br.containsHTML("You will be redirected to")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // Common bug: It can happen that the texts that we use to differ
        // between the kinds of links change so the decrypter breaks down,
        // always check that first!
        if (br.containsHTML("The member uploaded this image for subscribers only")) {
            DownloadLink dl = createDownloadlink(parameter.toString().replace("motherless", "premiummotherlesspictures"));
            dl.setProperty("kind", "picture");
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML("The member uploaded this video for subscribers only")) {
            DownloadLink dl = createDownloadlink(parameter.toString().replace("motherless", "premiummotherlessvideos"));
            dl.setProperty("kind", "video");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("player\\.swf")) {
            String parm = parameter.toString();
            String filelink = br.getRegex("s1\\.addParam\\(\\'flashvars\\',\\'file=(http://.*?\\.flv/[a-z0-9]+/[A-Z0-9]+\\.flv)").getMatch(0);
            if (filelink != null) {
                filelink = filelink.replace(REPLACE, "motherless");
                filelink = filelink.replace("motherless", "motherlessvideos");
            }
            if (filelink == null) {
                filelink = br.getRegex("(http://s\\d+\\.motherlessmedia\\.com/dev[0-9/]+\\.flv/[a-z0-9]+/[A-Z0-9]+\\.flv)").getMatch(0);
            }
            if (filelink == null) return null;
            DownloadLink dlink = createDownloadlink(filelink);
            dlink.setBrowserUrl(parm);
            String finalName = new Regex(filelink, "([A-Za-z0-9]+\\.flv)$").getMatch(0);
            if (finalName != null) dlink.setFinalFileName(finalName);
            decryptedLinks.add(dlink);
        } else if (!br.containsHTML("<strong>Uploaded</strong>")) {
            ArrayList<String> pages = new ArrayList<String>();
            pages.add("currentPage");
            String pagenumbers[] = br.getRegex("page=(\\d+)\"").getColumn(0);
            if (!(pagenumbers == null) && !(pagenumbers.length == 0)) {
                for (String aPageNumer : pagenumbers) {
                    if (!pages.contains(aPageNumer)) pages.add(aPageNumer);
                }
            }
            logger.info("Found " + pages.size() + " pages, decrypting now...");
            for (String getthepage : pages) {
                if (!getthepage.equals("currentPage")) br.getPage(parameter.toString() + "?page=" + getthepage);
                fpName = br.getRegex("<title>MOTHERLESS\\.COM - Moral Free Hosting : Galleries :(.*?)</title>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<h1 style=\"font-size: 1.4em; font-weight: bold;\">(.*?)</h1>").getMatch(0);
                    if (fpName == null) fpName = br.getRegex("<title>MOTHERLESS\\.COM - Go Ahead She Isn\\'t Looking\\! : Galleries : (.*?)</title>").getMatch(0);
                }
                String[] links = br.getRegex("id=\"wrapper_[A-Z0-9]+\">[\t\n\r ]+<a href=\"(/[A-Z0-9]+/[A-Z0-9]+)\"").getColumn(0);
                progress.setRange(links.length);
                if (links == null || links.length == 0) return null;
                logger.info("Decrypting page " + getthepage + " which contains " + links.length + " links.");
                for (String singlelink : links) {
                    br.getPage("http://motherless.com" + singlelink);
                    String finallink = getSingleLink();
                    if (finallink == null) return null;
                    DownloadLink dl = createDownloadlink(finallink.replace("motherless", "motherlesspictures"));
                    if (fpName != null) dl.setProperty("package", fpName);
                    decryptedLinks.add(dl);
                    progress.increase(1);
                }
            }
        } else {
            String finallink = getSingleLink();
            if (finallink == null) return null;
            DownloadLink fina = createDownloadlink(finallink.replace("motherless", "motherlesspictures"));
            decryptedLinks.add(fina);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getSingleLink() {
        String finallink = br.getRegex("\"(http://members\\.motherless\\.com/img/.*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("full_sized\\.jpg\" (.*?)\"(http://s\\d+\\.motherless\\.com/dev\\d+/\\d+/\\d+/\\d+/\\d+.*?)\"").getMatch(1);
            if (finallink == null) {
                finallink = br.getRegex("<div style=\"clear: left;\"></div>[\t\r\n ]+<img src=\"(http://.*?)\"").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("\\?full\">[\n\t\r ]+<img src=\"(?!http://motherless\\.com/images/full_sized\\.jpg)(http://.*?)\"").getMatch(0);
                    if (finallink == null) {
                        finallink = br.getRegex("\"(http://s\\d+\\.motherlessmedia\\.com/dev[0-9/]+\\..{3,4})\"").getMatch(0);
                    }
                }
            }
        }
        if (finallink != null) finallink = finallink.replace(REPLACE, "motherless");
        return finallink;
    }
}
