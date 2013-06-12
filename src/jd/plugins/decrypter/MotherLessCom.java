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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "motherless.com" }, urls = { "http://(www\\.)?(members\\.)?motherless\\.com/(?!privacy|popular|register|premium|members|galleries|contact)(g/[\\w\\-]+/[A-Z0-9]{7}|[A-Z0-9]{7,9}(/[A-Z0-9]{7})?)" }, flags = { 0 })
public class MotherLessCom extends PluginForDecrypt {

    private String fpName = null;

    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // DEV NOTES:
    //
    // REGEX run down, allows the following
    // motherless.com/g/WHAT_EVER_HERE/UID
    // motherless.com/UID1/UID
    // motherless.com/UID
    //
    // - don't support: groups(/g[a-z]{1}/), images or videos they can have over 1000 pages
    // - supports spanning pages and galleries with submenu (MORE links).
    // - set same UA from hoster plugin, making it harder to distinguish.
    // - Server issues can return many 503's in high load situations.
    // - Server also punishes user who downloads with too many connections. This is a linkchecking issue also, as grabs info from headers.
    // - To reduce server loads associated with linkchecking, I've set 'setAvailable(true) for greater than 5 pages.

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        JDUtilities.getPluginForHost("motherless.com");
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MotherLessCom.ua);
        // alters 'domain/(g/name/)uid' by removing all but uid
        String parameter = param.toString().replaceAll("motherless\\.com/g/[\\w\\-]+/", "motherless.com/");
        br.getPage(parameter);
        if (br.containsHTML("Not Available") || br.containsHTML("not found") || br.containsHTML("You will be redirected to") || br.containsHTML("The page you\\'re looking for cannot be found")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("motherless.com/", "motherlessvideos.com/"));
            dl.setProperty("dltype", "offline");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("class=\"red\\-pill\\-button rounded\\-corners\\-r5\">Reply</a>")) {
            logger.info("This is a forum link without any downloadable content: " + parameter);
            return decryptedLinks;
        }
        // Common bug: It can happen that the texts that we use to differ between the kinds of links change so the decrypter breaks down,
        // always check that first!
        if (br.containsHTML("The member uploaded this image for subscribers only")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("motherless", "premiummotherlesspictures"));
            dl.setProperty("dltype", "image");
            dl.setProperty("onlyregistered", "true");
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML("The member uploaded this video for subscribers only")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("motherless.com/", "motherlessvideos.com/"));
            dl.setProperty("dltype", "video");
            dl.setProperty("onlyregistered", "true");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String[] SubGal = br.getRegex("<a href=\"(/[A-Z0-9]+)\" title=\"More [^ ]+ in this gallery\" class=\"pop plain more\">See More \\&raquo\\;</a>").getColumn(0);
        if (SubGal != null && SubGal.length != 0) {
            for (String subuid : SubGal) {
                br.getPage("http://motherless.com" + subuid);
                Gallery(decryptedLinks, parameter, progress);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
        if (br.containsHTML("(jwplayer\\(|jwplayer_playing|jwplayer_position)")) {
            DownloadLink dlink = createDownloadlink(parameter.replace("motherless.com/", "motherlessvideos.com/"));
            dlink.setProperty("dltype", "video");
            dlink.setBrowserUrl(parameter);
            dlink.setName(new Regex(parameter, "motherless\\.com/(.+)").getMatch(0));
            decryptedLinks.add(dlink);
        } else if (!br.containsHTML("<strong>Uploaded</strong>")) {
            Gallery(decryptedLinks, parameter, progress);
        } else {
            DownloadLink fina = createDownloadlink(parameter.replace("motherless.com/", "motherlesspictures.com/"));
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

    // finds the uid within the grouping
    private String formLink(String singlelink) {
        if (singlelink.startsWith("/")) singlelink = "http://motherless.com" + singlelink;
        String ID = new Regex(singlelink, "http://motherless\\.com/[A-Z0-9]+/([A-Z0-9]+)").getMatch(0);
        if (ID != null) singlelink = "http://motherless.com/" + ID;
        return singlelink;
    }

    private void Gallery(ArrayList<DownloadLink> ret, String parameter, ProgressController progress) throws IOException {
        if (fpName == null) {
            fpName = br.getRegex("<title>MOTHERLESS\\.COM \\- Go Ahead She Isn\\'t Looking\\! :  (.*?)</title>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<div class=\"member\\-bio\\-username\">.*?\\'s Gallery \\&bull; (.*?)</div>").getMatch(0);
                if (fpName == null) fpName = br.getRegex("<title>([^<>\"]*?) \\- MOTHERLESS\\.COM</title>").getMatch(0);
            }
        }
        // grabs final page as count.
        String totalpages = br.getRegex("<a href=\"/[A-Z0-9]{9}\\?page=\\d+\"[^>]+>(\\d+)</a><a href=\"/[A-Z0-9]{9}\\?page=\\d+\"[^>]+>NEXT").getMatch(0);
        if (totalpages == null) {
            totalpages = br.getRegex("<a href=\"/[A-Z0-9]{9}\\?page=(\\d+)\"[^>]+>\\d+</a><a href=\"/[A-Z0-9]{9}\\?page=\\d+\"[^>]+>NEXT").getMatch(0);
            if (totalpages == null) totalpages = "1";
        }

        int numberOfPages = Integer.parseInt(totalpages);
        progress.setRange(numberOfPages);
        logger.info("Found " + numberOfPages + " page(s), decrypting now...");
        for (int i = 1; i <= numberOfPages; i++) {
            String[] picturelinks = br.getRegex("<[^>]+data-mediatype=\"image\"[^>]+>[\r\n\t ]+<a href=\"(/[a-zA-Z0-9]+){1,2}\" class=\"img-container\"").getColumn(0);
            // stupid site jumps URLS for NextPage depending on parameter
            String NextPage = br.getRegex("<a href=\"(/[A-Z0-9]{7,9}\\?page=\\d+)\"[^>]+>NEXT").getMatch(0);
            if (picturelinks != null && picturelinks.length != 0) {
                logger.info("Decrypting page " + i + " which contains " + picturelinks.length + " links.");
                for (String singlelink : picturelinks) {
                    singlelink = formLink(singlelink);
                    DownloadLink dl = createDownloadlink(singlelink.replace("motherless.com/", "motherlesspictures.com/"));
                    if (fpName != null) dl.setProperty("package", fpName);
                    dl.setProperty("dltype", "image");
                    // fast add.
                    if (picturelinks.length >= 10) dl.setAvailable(true);
                    ret.add(dl);
                }
            }
            String[] videolinks = br.getRegex("<[^>]+data-mediatype=\"video\"[^>]+>[\r\n\t ]+<a href=\"(/[a-zA-Z0-9]+){1,2}\" class=\"img-container\"").getColumn(0);
            if (videolinks != null && videolinks.length != 0) {
                for (String singlelink : videolinks) {
                    String linkID = new Regex(singlelink, "/g/.*?/([A-Z0-9]+$)").getMatch(0);
                    if (linkID != null) singlelink = "http://motherless.com/" + linkID;
                    singlelink = formLink(singlelink);
                    DownloadLink dl = createDownloadlink(singlelink.replace("motherless.com/", "motherlessvideos.com/"));
                    dl.setProperty("dltype", "video");
                    if (fpName != null) dl.setProperty("package", fpName);
                    // fast add.
                    if (videolinks.length >= 10) dl.setAvailable(true);
                    ret.add(dl);
                }
            }
            if ((picturelinks == null || picturelinks.length == 0) && (videolinks == null || videolinks.length == 0)) {
                logger.warning("Decrypter failed for link: " + parameter);
                return;
            }
            if (i != numberOfPages && NextPage != null) br.getPage(NextPage);
            progress.increase(1);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}