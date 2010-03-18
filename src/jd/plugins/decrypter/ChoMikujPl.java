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

import java.io.IOException;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chomikuj.pl" }, urls = { "http://[\\w\\.]*?chomikuj\\.pl/.+" }, flags = { 0 })
public class ChoMikujPl extends PluginForDecrypt {

    public ChoMikujPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // The message used on errors in this plugin
        String error = "Error while decrypting link: " + parameter;
        br.getPage(parameter);
        String fpName = br.getRegex("<title>(.*?) - .*? - Chomikuj\\.pl.*?</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("class=\"T_selected\">(.*?)</span>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<span id=\"ctl00_CT_FW_SelectedFolderLabel\" style=\"font-weight:bold;\">(.*?)</span>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("").getMatch(0);
                }
            }
        }
        String subFolderID = br.getRegex("id=\"ctl00_CT_FW_SubfolderID\" value=\"(.*?)\"").getMatch(0);
        if (subFolderID == null) subFolderID = br.getRegex("name=\"ChomikSubfolderId\" type=\"hidden\" value=\"(.*?)\"").getMatch(0);
        if (subFolderID == null || fpName == null) {
            logger.warning(error);
            return null;
        }
        subFolderID = subFolderID.trim();
        String postdata = "ctl00%24CT%24FW%24SubfolderID=" + subFolderID + "&GalSortType=0&__EVENTTARGET=ctl00%24CT%24FW%24RefreshButton&__ASYNCPOST=true&GalPage=";
        logger.info("Looking how many pages we got here for folder " + subFolderID + " ...");
        int pageCount = getPageCount(postdata, parameter);
        if (pageCount == -1) {
            logger.warning("Error, couldn't successfully find the number of pages for link: " + parameter);
            return null;
        }
        logger.info("Found " + pageCount + " pages. Starting to decrypt them now.");
        progress.setRange(pageCount);
        for (int i = 0; i < pageCount; ++i) {
            logger.info("Decrypting page " + i + " of folder " + subFolderID + " now...");
            String postThatData = postdata + i;
            br.postPage(parameter, postThatData);
            // Every full page has 24 links (pictures)
            // This regex finds all links to PICTUREs, the site also got .rar
            // files but the regex doesn't find them because you need to be
            // logged in to download them anyways
            String[] fileId = br.getRegex("href=\"/Image\\.aspx\\?id=(\\d+)\"").getColumn(0);
            String[] links = br.getRegex("(<a class=\"gallery\" href=\".*?\".*?<a class=\"photoLnk\" href=\".*?\")").getColumn(0);
            if (fileId == null || fileId.length == 0 || links == null || links.length == 0 || links.length != fileId.length) {
                // If the last page only contains a file or fileS the regexes
                // don't work but the decrypter isn't broken so the user should
                // get the links!
                if (br.containsHTML(".zip")) {
                    logger.info("Stopping at page " + i + " because there were no pictures found on the page but i did find a .zip file!");
                    return decryptedLinks;
                }
                logger.warning(error);
                return null;
            }

            for (int j = 0; j < fileId.length; ++j) {
                String id = fileId[j];
                String fileEnding = new Regex(links[j], "class=\"photoLnk\" href=\".*?(\\..{2,4})\"").getMatch(0);
                String finalLink = String.format("&id=%s&gallerylink=%s&", id, param.toString().replace("chomikuj.pl", "60423fhrzisweguikipo9re"));
                DownloadLink dl = createDownloadlink(finalLink);
                dl.setFinalFileName(fpName + "_" + fileId[j] + fileEnding);
                // All found links are 99,99% available as long as the decrypter
                // reaches this line of code^^
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            progress.increase(1);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName.trim());
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    public int getPageCount(String postdata, String theParameter) throws NumberFormatException, DecrypterException, IOException {
        int pageCount = 0;
        int tempint = 0;
        // Loop limited to 20 in case something goes seriously wrong
        for (int i = 0; i <= 20; i++) {
            // Find number of pages
            String[] lolpages = br.getRegex("href=\"javascript:;\">(\\d+)<").getColumn(0);
            if (lolpages == null || lolpages.length == 0) return -1;
            // Find highest number of page
            for (String page : lolpages) {
                if (Integer.parseInt(page) > tempint) tempint = Integer.parseInt(page);
            }
            if (tempint == pageCount) break;
            // If we find less than 6-7 pages there are no more so we have to
            // stop this here before getting an error!
            if (tempint < 7) {
                pageCount = tempint;
                break;
            }
            String postThoseData = postdata + tempint;
            br.postPage(theParameter, postThoseData);
            pageCount = tempint;
        }
        return pageCount;
    }
}
