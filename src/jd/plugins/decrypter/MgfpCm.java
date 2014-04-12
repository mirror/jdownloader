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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagefap.com" }, urls = { "http://(www\\.)?imagefap\\.com/(gallery\\.php\\?p?gid=.+|gallery/.+|pictures/\\d+/.{1}|photo/\\d+)" }, flags = { 0 })
public class MgfpCm extends PluginForDecrypt {

    public MgfpCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> allPages = new ArrayList<String>();
        allPages.add("0");
        br.setFollowRedirects(false);
        String parameter = param.toString();
        if (parameter.matches("http://(www\\.)?imagefap\\.com/photo/\\d+")) {
            final DownloadLink link = createDownloadlink("http://imagefap.com/imagedecrypted/" + new Regex(parameter, "(\\d+)$").getMatch(0));
            decryptedLinks.add(link);
        } else {
            parameter = parameter.replaceAll("view\\=[0-9]+", "view=2");
            if (new Regex(parameter, "imagefap\\.com/gallery\\.php\\?pgid=").matches()) {
                /**
                 * Workaround to get all images on one page for private galleries (site buggy)
                 */
                br.getPage("http://www.imagefap.com/gallery.php?view=2");
            } else if (!parameter.contains("view=2")) parameter += "?view=2";
            try {
                br.getPage(parameter);
            } catch (final BrowserException e) {
                final DownloadLink link = createDownloadlink("http://imagefap.com/imagedecrypted/" + new Random().nextInt(1000000));
                link.setFinalFileName(new Regex(parameter, "imagefap\\.com/(.+)").getMatch(0));
                link.setAvailable(false);
                link.setProperty("offline", true);
                decryptedLinks.add(link);
                return decryptedLinks;
            }

            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().contains("/pictures/")) {
                    parameter = br.getRedirectLocation();
                    parameter += "?view=2";
                    logger.info("New parameter is set: " + parameter);
                    br.getPage(parameter);
                } else {
                    logger.warning("Getting unknown redirect page");
                    br.getPage(br.getRedirectLocation());
                }
            }

            if (br.getURL().contains("imagefap.com/404.php") || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Could not find gallery<")) {
                final DownloadLink link = createDownloadlink("http://imagefap.com/imagedecrypted/" + new Random().nextInt(1000000));
                link.setFinalFileName(new Regex(parameter, "imagefap\\.com/(.+)").getMatch(0));
                link.setAvailable(false);
                link.setProperty("offline", true);
                decryptedLinks.add(link);
                return decryptedLinks;
            }

            // First find all the information we need (name of the gallery, name of
            // the galleries author)
            String galleryName = br.getRegex("<title>Porn pics of (.*?) \\(Page 1\\)</title>").getMatch(0);
            if (galleryName == null) {
                galleryName = br.getRegex("<font face=\"verdana\" color=\"white\" size=\"4\"><b>(.*?)</b></font>").getMatch(0);
                if (galleryName == null) galleryName = br.getRegex("<meta name=\"description\" content=\"Airplanes porn pics - Imagefap\\.com\\. The ultimate social porn pics site\" />").getMatch(0);
            }
            String authorsName = br.getRegex("<b><font size=\"3\" color=\"#CC0000\">Uploaded by ([^<>\"]+)</font></b>").getMatch(0);
            if (authorsName == null) authorsName = br.getRegex("<td class=\"mnu0\"><a href=\"http://(www\\.)?imagefap\\.com/profile\\.php\\?user=([^<>\"]+)\"").getMatch(0);
            if (galleryName == null) {
                logger.warning("Gallery name could not be found!");
                return null;
            }
            if (authorsName == null) authorsName = "Anonymous";
            galleryName = Encoding.htmlDecode(galleryName.trim());
            authorsName = Encoding.htmlDecode(authorsName.trim());

            /**
             * Max number of images per page = 1000, if we got more we always have at least 2 pages
             */
            final String[] pages = br.getRegex("<a class=link3 href=\"\\?(pgid=\\&amp;gid=\\d+\\&amp;page=|gid=\\d+\\&amp;page=)(\\d+)").getColumn(1);
            if (pages != null && pages.length != 0) for (final String page : pages)
                if (!allPages.contains(page)) allPages.add(page);
            int counter = 1;
            DecimalFormat df = new DecimalFormat("0000");
            for (final String page : allPages) {
                if (!page.equals("0")) br.getPage(parameter + "&page=" + page);
                final String info[][] = br.getRegex("<span id=\"img_(\\d+)_desc\">.*?<font face=verdana color=\"#000000\"><i>([^<>\"]*?)</i>").getMatches();
                if (info == null || info.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String elements[] : info) {
                    final String orderID = df.format(counter);
                    final DownloadLink link = createDownloadlink("http://imagefap.com/imagedecrypted/" + elements[0]);
                    link.setProperty("orderid", orderID);
                    link.setProperty("galleryname", galleryName);
                    link.setProperty("authorsname", authorsName);
                    link.setName(authorsName + " - " + galleryName + " - " + df.format(counter) + Encoding.htmlDecode(elements[1].trim()));
                    link.setAvailable(true);
                    decryptedLinks.add(link);
                    counter++;
                }
            }
            // Finally set the packagename even if its set again in the linkgrabber
            // available check of the imagefap hosterplugin
            FilePackage fp = FilePackage.getInstance();
            fp.setName(authorsName + " - " + galleryName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}