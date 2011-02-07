//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagefap.com" }, urls = { "http://[\\w\\.]*?imagefap\\.com/(gallery\\.php\\?gid=.+|gallery/.+|pictures/\\d+/.{1})" }, flags = { 0 })
public class MgfpCm extends PluginForDecrypt {

    public MgfpCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String THISID;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        parameter = parameter.replaceAll("view\\=[0-9]+", "view=2");
        if (!parameter.contains("view=2")) parameter += "&view=2";
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("/pictures/")) {
                parameter = br.getRedirectLocation();
                logger.info("New parameter is set: " + parameter);
                br.getPage(parameter);
            } else {
                logger.warning("Getting unknown redirect page");
                br.getPage(br.getRedirectLocation());
            }
        }
        // First find all the information we need (name of the gallery, name of
        // the galleries author)
        String galleryName = br.getRegex("<title>Porn pics of (.*?) \\(Page 1\\)</title>").getMatch(0);
        if (galleryName == null) {
            galleryName = br.getRegex("<font face=\"verdana\" color=\"white\" size=\"4\"><b>(.*?)</b></font>").getMatch(0);
            if (galleryName == null) galleryName = br.getRegex("<meta name=\"description\" content=\"Airplanes porn pics - Imagefap\\.com\\. The ultimate social porn pics site\" />").getMatch(0);
        }
        String authorsName = br.getRegex("<b><font size=\"4\" color=\"#CC0000\">(.*?)\\'s gallery</font></b>").getMatch(0);
        if (authorsName == null) {
            authorsName = br.getRegex("<td class=\"mnu0\"><a href=\"/profile\\.php\\?user=(.*?)\"").getMatch(0);
            if (authorsName == null) {
                authorsName = br.getRegex("jQuery\\.BlockWidget\\(\\d+,\"(.*?)\",\"left\"\\);").getMatch(0);
                if (authorsName == null) {
                    authorsName = br.getRegex("<title>Porn pics of (.*?) (\\(Page 1\\))?</title>").getMatch(0);
                    if (authorsName == null) {
                        authorsName = br.getRegex("<a class=\"title\" href=\"/gallery/\\d+\">(.*?)</a>").getMatch(0);
                    }
                }
            }
        }
        if (galleryName == null) {
            logger.warning("Gallery name could not be found!");
            return null;
        }
        if (authorsName == null) {
            logger.warning("Author's name could not be found!");
            return null;
        }
        THISID = new Regex(parameter, "imagefap\\.com/pictures/(\\d+)/").getMatch(0);
        boolean done = false;
        int currentLastPages = 0;
        int lastLastPage = 0;
        // Find out how many pages the link has
        if (THISID != null) {
            getSpecifiedPage(0);
            for (int i = 0; i <= 20; i++) {
                currentLastPages = findLastPage(currentLastPages);
                if (currentLastPages == lastLastPage) {
                    done = true;
                    break;
                    // if a link has 9(10) or more pages it doesn't show them
                    // all so we have to access the last page and see how many
                    // more pages we have.
                    // By repeating this till we reach the end we know how many
                    // pages the link has.
                } else if (currentLastPages >= 9) {
                    getSpecifiedPage(currentLastPages);
                    lastLastPage = currentLastPages;
                    continue;
                }
                done = true;
                break;
            }
            if (currentLastPages > 0) logger.info("Found " + (currentLastPages + 1) + " pages, starting to decrypt...");
            if (!done) {
                logger.warning("Couldn't find all pages, stopping...");
            }
        }
        // Decrypt all pages, find all links
        progress.setRange(currentLastPages);
        double counter = 0.0001;
        for (int i = 0; i <= currentLastPages; i++) {
            if (currentLastPages > 0) {
                logger.info("Decrypting page " + i + " of " + currentLastPages);
                getSpecifiedPage(i);
            }
            if (!br.containsHTML("<b>Could not find gallery</b>")) {
                String links[] = br.getRegex("<a name=\"\\d+\" href=\"/image\\.php\\?id=(\\d+)\\&").getColumn(0);
                if (links == null || links.length == 0) return null;
                for (String element : links) {
                    String orderID = new Regex(String.format("&orderid=%.4f&", counter), "\\&orderid=0\\.(\\d+)").getMatch(0);
                    DownloadLink link = createDownloadlink("http://imagefap.com/image.php?id=" + element);
                    link.setProperty("orderid", orderID);
                    link.setProperty("galleryname", galleryName);
                    link.setProperty("authorsname", authorsName);
                    link.setName(orderID);
                    decryptedLinks.add(link);
                    counter += 0.0001;
                }
            }
            progress.increase(1);
        }
        // Finally set the packagename even if its set again in the linkgrabber
        // available check of the imagefap hosterplugin
        FilePackage fp = FilePackage.getInstance();
        fp.setName(authorsName.trim() + " - " + galleryName.trim());
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private int findLastPage(int currentLastPage) {
        String[] allpages = br.getRegex("<a class=link3 href=\"\\?pgid=\\&amp;gid=\\d+\\&amp;page=(\\d+)\\&amp;").getColumn(0);
        if (allpages != null && allpages.length != 0) {
            for (String pageText : allpages) {
                if (Integer.parseInt(pageText) > currentLastPage) currentLastPage = Integer.parseInt(pageText);
            }
        }
        return currentLastPage;
    }

    private void getSpecifiedPage(int page) throws IOException {
        br.getPage("http://www.imagefap.com/pictures/" + THISID + "/bla?pgid=&gid=" + THISID + "&page=" + page + "&view=0");
    }

}
