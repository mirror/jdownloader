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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "motherless.com" }, urls = { "http://(www\\.)?(members\\.)?motherless\\.com/((?!movies|thumbs|uploads|gi/)g/[A-Za-z0-9\\-_]+/[A-Z0-9]+|[A-Z0-9]+)" }, flags = { 0 })
public class MotherLessCom extends PluginForDecrypt {

    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String fpName = parameter.getStringProperty("package");
        br.setFollowRedirects(true);
        String param = parameter.toString().replaceAll("motherless\\.com/g/[A-Za-z0-9_\\-]+/", "motherless.com/");
        br.getPage(param);
        if (br.containsHTML("Not Available") || br.containsHTML("not found") || br.containsHTML("You will be redirected to")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // Common bug: It can happen that the texts that we use to differ
        // between the kinds of links change so the decrypter breaks down,
        // always check that first!
        if (br.containsHTML("The member uploaded this image for subscribers only")) {
            DownloadLink dl = createDownloadlink(param.replace("motherless", "premiummotherlesspictures"));
            dl.setProperty("dltype", "image");
            dl.setProperty("onlyregistered", "true");
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML("The member uploaded this video for subscribers only")) {
            DownloadLink dl = createDownloadlink(param.replace("motherless.com/", "motherlessvideos.com/"));
            dl.setProperty("dltype", "video");
            dl.setProperty("onlyregistered", "true");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("player\\.swf")) {
            DownloadLink dlink = createDownloadlink(param.replace("motherless.com/", "motherlessvideos.com/"));
            dlink.setProperty("dltype", "video");
            dlink.setBrowserUrl(param);
            dlink.setName(new Regex(param, "motherless\\.com/(.+)").getMatch(0));
            decryptedLinks.add(dlink);
        } else if (!br.containsHTML("<strong>Uploaded</strong>")) {
            ArrayList<String> pages = new ArrayList<String>();
            pages.add("currentPage");
            String pagenumbers[] = br.getRegex("page=(\\d+)\"").getColumn(0);
            if (!(pagenumbers == null) && !(pagenumbers.length == 0)) {
                for (String aPageNumber : pagenumbers) {
                    if (!pages.contains(aPageNumber) && !aPageNumber.equals("1")) pages.add(aPageNumber);
                }
            }
            logger.info("Found " + pages.size() + " pages, decrypting now...");
            progress.setRange(pages.size());
            for (String getthepage : pages) {
                if (!getthepage.equals("currentPage")) br.getPage(param + "?page=" + getthepage);
                fpName = br.getRegex("<title>MOTHERLESS\\.COM \\- Go Ahead She Isn\\'t Looking\\! :  (.*?)</title>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<div class=\"member\\-bio\\-username\">.*?\\'s Gallery \\&bull; (.*?)</div>").getMatch(0);
                }
                String[] picturelinks = br.getRegex("class=\"thumbnail mediatype_image\" rel=\"[A-Z0-9]+\">[\t\n\r ]+<div class=\"thumbnail\\-img-wrap\" id=\"wrapper_[A-Z0-9]+\">[\t\n\r ]+<a href=\"(http://motherless\\.com/[A-Z0-9]+)\"").getColumn(0);
                if (picturelinks != null && picturelinks.length != 0) {
                    logger.info("Decrypting page " + getthepage + " which contains " + picturelinks.length + " links.");
                    for (String singlelink : picturelinks) {
                        DownloadLink dl = createDownloadlink(singlelink.replace("motherless.com/", "motherlesspictures.com/"));
                        if (fpName != null) dl.setProperty("package", fpName);
                        decryptedLinks.add(dl);
                        dl.setProperty("dltype", "image");
                    }
                }
                String[] videolinks = br.getRegex("class=\"thumbnail mediatype_video\" rel=\"[A-Z0-9]+\">[\t\n\r ]+<div class=\"thumbnail\\-img-wrap\" id=\"wrapper_[A-Z0-9]+\">[\t\n\r ]+<a href=\"(http://motherless\\.com/.*?)\"").getColumn(0);
                if (videolinks != null && videolinks.length != 0) {
                    for (String singlelink : videolinks) {
                        String linkID = new Regex(singlelink, "/g/.*?/([A-Z0-9]+$)").getMatch(0);
                        if (linkID != null) singlelink = "http://motherless.com/" + linkID;
                        DownloadLink dl = createDownloadlink(singlelink.replace("motherless.com/", "motherlessvideos.com/"));
                        dl.setProperty("dltype", "video");
                        if (fpName != null) dl.setProperty("package", fpName);
                        decryptedLinks.add(dl);
                    }
                }
                if ((picturelinks == null || picturelinks.length == 0) && (videolinks == null || videolinks.length == 0)) {
                    logger.warning("Decrypter failed for link: " + param);
                    return null;
                }
                progress.increase(1);
            }
        } else {
            DownloadLink fina = createDownloadlink(param.replace("motherless.com/", "motherlesspictures.com/"));
            fina.setProperty("dltype", "image");
            decryptedLinks.add(fina);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
