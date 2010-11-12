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
import java.util.Random;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chomikuj.pl" }, urls = { "http://(www\\.)?chomikuj\\.pl/.+" }, flags = { 0 })
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
        String viewState = br.getRegex("id=\"__VIEWSTATE\" value=\"(.*?)\"").getMatch(0);
        String treeExpandLog = br.getRegex("id=\"ctl00_CT_TW_TreeExpandLog\" value=\"(.*?)\"").getMatch(0);
        String chomikId = br.getRegex("id=\"ctl00_CT_ChomikID\" value=\"(.*?)\"").getMatch(0);
        String subFolderID = br.getRegex("id=\"ctl00_CT_FW_SubfolderID\" value=\"(.*?)\"").getMatch(0);
        if (subFolderID == null) subFolderID = br.getRegex("name=\"ChomikSubfolderId\" type=\"hidden\" value=\"(.*?)\"").getMatch(0);
        if (subFolderID == null || fpName == null || chomikId == null || treeExpandLog == null || viewState == null) {
            logger.warning(error);
            return null;
        }
        subFolderID = subFolderID.trim();
        // Prepare browser
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("Accept-Encoding", "gzip,deflate");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getHeaders().put("Cache-Control", "no-cache, no-cache");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8r ");
        br.getHeaders().put("X-MicrosoftAjax", "Delta=true");
        br.getHeaders().put("Pragma", "no-cache");
        // Das sind die wichtigen Postdaten
        String postdata = "ctl00%24SM=ctl00%24CT%24FW%24FoldersUp%7Cctl00%24CT%24FW%24RefreshButton&__EVENTTARGET=ctl00%24CT%24FW%24RefreshButton&__EVENTARGUMENT=&__VIEWSTATE=" + Encoding.urlEncode(viewState) + "&PageCmd=&PageArg=undefined&ctl00%24LoginTop%24LoginChomikName=&ctl00%24LoginTop%24LoginChomikPassword=&ctl00%24SearchInputBox=&ctl00%24SearchFileBox=&ctl00%24SearchType=all&SType=0&ctl00%24CT%24ChomikID=" + chomikId + "&ctl00%24CT%24PermW%24LoginCtrl%24PF=&ctl00%24CT%24TW%24TreeExpandLog=" + Encoding.urlEncode(treeExpandLog) + "&ChomikSubfolderId=" + subFolderID + "&ctl00%24CT%24FW%24SubfolderID=" + subFolderID + "&FVSortType=1&FVSortDir=1&FVSortChange=&ctl00%24CT%24FW%24inpFolderAddress=" + Encoding.urlEncode(parameter) + "&FrGroupId=0&__ASYNCPOST=true&ctl00%24CT%24FrW%24FrPage=";
        logger.info("Looking how many pages we got here for folder " + subFolderID + " ...");
        // Herausfinden wie viele Seiten der Link hat
        int pageCount = getPageCount(postdata, parameter);
        if (pageCount == -1) {
            logger.warning("Error, couldn't successfully find the number of pages for link: " + parameter);
            return null;
        }
        logger.info("Found " + pageCount + " pages. Starting to decrypt them now.");
        progress.setRange(pageCount);
        // Alle Seiten decrypten
        for (int i = 0; i < pageCount; ++i) {
            logger.info("Decrypting page " + i + " of folder " + subFolderID + " now...");
            String postThatData = postdata + i;
            br.postPage(parameter, postThatData);
            // Every full page has 30 links (pictures)
            // This regex finds all links to PICTUREs, the site also got .rar
            // files but the regex doesn't find them because you need to be
            // logged in to download them anyways
            String[] fileIds = br.getRegex("href=\"/Image\\.aspx\\?id=(\\d+)\"").getColumn(0);
            if (fileIds == null || fileIds.length == 0) fileIds = br.getRegex("class=\"FileName\" onclick=\"return ch\\.Download\\.dnFile\\((.*?)\\);\"").getColumn(0);
            if (fileIds == null || fileIds.length == 0) {
                // If the last page only contains a file or fileS the regexes
                // don't work but the decrypter isn't broken so the user should
                // get the links!
                if (br.containsHTML("\\.zip")) {
                    logger.info("Stopping at page " + i + " because there were no pictures found on the page but i did find a .zip file!");
                    return decryptedLinks;
                }
                logger.warning(error);
                return null;
            }

            for (String id : fileIds) {
                String finalLink = String.format("&id=%s&gallerylink=%s&", id, param.toString().replace("chomikuj.pl", "60423fhrzisweguikipo9re"));
                DownloadLink dl = createDownloadlink(finalLink);
                dl.setName(String.valueOf(new Random().nextInt(1000000)));
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
            String pagePiece = br.getRegex("class=\"navigation\"><table id=\"ctl00_CT_FW_FV_NavTop_NavTable\" border=\"0\">(.*?)</table></div>").getMatch(0);
            String[] lolpages = null;
            if (pagePiece != null) lolpages = new Regex(pagePiece, "=\"javascript:;\">(\\d+)<").getColumn(0);
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
