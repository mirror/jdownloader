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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chomikuj.pl" }, urls = { "http://(www\\.)?chomikuj\\.pl/.+" }, flags = { 0 })
public class ChoMikujPl extends PluginForDecrypt {

    public ChoMikujPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PASSWORDWRONG = ">Nieprawidłowe hasło<";
    private static final String PASSWORDTEXT  = "Ten folder jest (<b>)?zabezpieczony oddzielnym hasłem";
    private ArrayList<Integer>  REGEXSORT     = new ArrayList<Integer>();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String problem = null;
        try {
            problem = parameter.substring(parameter.lastIndexOf(","));
        } catch (Exception e) {
        }
        if (problem != null) parameter = parameter.replace(problem, "");
        parameter = parameter.replace("www.", "");
        // The message used on errors in this plugin
        String error = "Error while decrypting link: " + parameter;
        // If a link is password protected we have to save and use those data in the hosterplugin
        String savePost = null;
        String saveLink = null;
        String password = null;
        br.setFollowRedirects(false);
        br.getPage(parameter);
        // If we have a new link we have to use it or we'll have big problems later when POSTing things to the server
        if (br.getRedirectLocation() != null) {
            parameter = br.getRedirectLocation();
            br.getPage(br.getRedirectLocation());
        }
        // Check if the link directly wants to access a specified page of the gallery, if so remove it to avoid problems
        String checkPage = new Regex(parameter, "chomikuj\\.pl/.*?(,\\d+)$").getMatch(0);
        if (checkPage != null) {
            br.getPage(parameter.replace(checkPage, ""));
            if (br.getRedirectLocation() == null) {
                parameter = parameter.replace(checkPage, "");
            } else {
                br.getPage(parameter);
            }
        }
        String fpName = br.getRegex("<title>(.*?) \\- .*? \\- Chomikuj\\.pl.*?</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("class=\"T_selected\">(.*?)</span>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<span id=\"ctl00_CT_FW_SelectedFolderLabel\" style=\"font\\-weight:bold;\">(.*?)</span>").getMatch(0);
            }
        }
        String chomikID = br.getRegex("name=\"chomikId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
        if (chomikID == null) {
            chomikID = br.getRegex("id=\"__accno\" name=\"__accno\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
            if (chomikID == null) {
                chomikID = br.getRegex("name=\"friendId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
                if (chomikID == null) {
                    chomikID = br.getRegex("\\&amp;chomikId=(\\d+)\"").getMatch(0);
                }
            }
        }
        String folderID = br.getRegex("type=\"hidden\" name=\"FolderId\" value=\"(\\d+)\"").getMatch(0);
        if (folderID == null) folderID = br.getRegex("name=\"folderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
        final String requestVerificationToken = br.getRegex("<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"\\']+)\"").getMatch(0);
        if (folderID == null || fpName == null || requestVerificationToken == null) {
            logger.warning(error);
            return null;
        }
        fpName = fpName.trim();
        // Alle Haupt-POSTdaten
        String postdata = "chomikId=" + chomikID + "&folderId=" + folderID + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken);
        // Password handling
        if (br.containsHTML(PASSWORDTEXT)) {
            prepareBrowser(parameter, br);
            Form pass = br.getFormbyProperty("id", "LoginToFolder");
            if (pass == null) {
                logger.warning(error + " :: Can't find Password Form!");
                return null;
            }
            for (int i = 0; i <= 3; i++) {
                password = getUserInput(null, param);
                pass.put("Password", password);
                br.submitForm(pass);
                if (br.containsHTML("\\{\"IsSuccess\":true"))
                    break;
                else
                    continue;
            }
            if (!br.containsHTML("\\{\"IsSuccess\":true")) {
                logger.warning("Wrong password!");
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            saveLink = parameter;
        }
        logger.info("Looking how many pages we got here for link " + parameter + " ...");
        // Herausfinden wie viele Seiten der Link hat
        int pageCount = getPageCount(postdata, parameter);
        if (pageCount == -1) {
            logger.warning("Error, couldn't successfully find the number of pages for link: " + parameter);
            return null;
        } else if (pageCount == 0) pageCount = 1;
        logger.info("Found " + pageCount + " pages. Starting to decrypt them now.");
        progress.setRange(pageCount);
        final String linkPart = new Regex(parameter, "chomikuj\\.pl(/.+)").getMatch(0);
        /** Decrypt all pages, start with 1 (not 0 as it was before) */
        for (int i = 1; i <= pageCount; ++i) {
            logger.info("Decrypting page " + i + " of link: " + parameter);
            prepareBrowser(parameter, br);
            accessPage(postdata, br, i);
            // Every full page has 30 links (pictures)
            /** For photos */
            String[][] fileIds = br.getRegex("<div class=\"left\">[\t\n\r ]+<p class=\"filename\">[\t\n\r ]+<a class=\"downloadAction\" href=\"[^<>\"\\']+\"> +<span class=\"bold\">(.{1,300})</span>(\\..{1,20})</a>[\t\n\r ]+</p>[\t\n\r ]+<div class=\"thumbnail\">.*?title=\"([^<>\"]*?)\".*?</div>[\t\n\r ]+<div class=\"smallTab\">[\t\n\r ]+<ul class=\"tabGradientBg borderRadius\">[\t\n\r ]+<li>([^<>\"\\'/]+)</li>.*?class=\"galeryActionButtons visibleOpt fileIdContainer\" rel=\"(\\d+)\"").getMatches();
            addRegexInt(0, 1, 3, 4, 2);
            if (fileIds == null || fileIds.length == 0) {
                /** Specified for videos */
                fileIds = br.getRegex("<ul class=\"borderRadius tabGradientBg\">[\t\n\r ]+<li><span>([^<>\"\\']+)</span></li>[\t\n\r ]+<li><span class=\"date\">[^<>\"\\']+</span></li>[\t\n\r ]+</ul>[\t\n\r ]+</div>[\t\n\r ]+<div class=\"fileActionsButtons clear visibleButtons  fileIdContainer\" rel=\"(\\d+)\" style=\"visibility: hidden;\">.*?class=\"expanderHeader downloadAction\" href=\"[^<>\"\\']+\" title=\"[^<>\"\\']+\">[\t\n\r ]+<span class=\"bold\">([^<>\"\\']+)</span>([^<>\"\\']+)</a>[\t\n\r ]+<img alt=\"pobierz\" class=\"downloadArrow visibleArrow\" src=\"").getMatches();
                addRegexInt(2, 3, 0, 1, 0);
                /** Last attempt, only get IDs (no pre-available-check possible) */
                if (fileIds == null || fileIds.length == 0) {
                    fileIds = br.getRegex("fileIdContainer\" rel=\"(\\d+)\"").getMatches();
                }
            }
            String[][] allFolders = null;
            final String folderTable = br.getRegex("<div id=\"foldersList\">[\t\n\r ]+<table>(.*?)</table>[\t\n\r ]+</div>").getMatch(0);
            if (folderTable != null) {
                allFolders = new Regex(folderTable, "<a href=\"(/[^<>\"]*?)\" rel=\"\\d+\" title=\"([^<>\"]*?)\"").getMatches();
            }
            if ((fileIds == null || fileIds.length == 0) && (allFolders == null || allFolders.length == 0)) {
                if (br.containsHTML("class=\"noFile\">Nie ma plik\\&#243;w w tym folderze</p>")) {
                    logger.info("The following link is offline: " + parameter);
                    return decryptedLinks;
                }
                // If the last page only contains a file or fileS the regex's don't work but the decrypter isn't broken so the user should
                // get the links!
                if (br.containsHTML("\\.zip")) {
                    logger.info("Stopping at page " + i + " because there were no pictures found on the page but i did find a .zip file!");
                    return decryptedLinks;
                }
                logger.warning(error);
                return null;
            }
            if (fileIds != null && fileIds.length != 0) {
                for (String[] id : fileIds) {
                    String finalLink = String.format("&id=%s&gallerylink=%s&", id[REGEXSORT.get(3)], param.toString().replace("chomikuj.pl", "60423fhrzisweguikipo9re"));
                    DownloadLink dl = createDownloadlink(finalLink);
                    if (id.length > 1) {
                        if (id.length == 5) {
                            dl.setName(Encoding.htmlDecode(id[REGEXSORT.get(4)].trim()));
                        } else {
                            dl.setName(Encoding.htmlDecode(id[REGEXSORT.get(0)].trim()) + id[REGEXSORT.get(1)].trim());
                        }
                        dl.setDownloadSize(SizeFormatter.getSize(id[REGEXSORT.get(2)].replace(",", ".")));
                        dl.setAvailable(true);
                        /**
                         * If the link is a video it needs other download handling
                         */
                        if (id[REGEXSORT.get(1)].trim().matches("\\.(avi|flv|mp4|mpg|rmvb|divx|wmv|mkv)")) dl.setProperty("video", "true");
                    } else {
                        dl.setName(String.valueOf(new Random().nextInt(1000000)));
                    }
                    if (saveLink != null && savePost != null && password != null) {
                        dl.setProperty("savedlink", saveLink);
                        dl.setProperty("savedpost", savePost);
                        // Not needed yet but might me useful in the future
                        dl.setProperty("password", password);
                    }
                    dl.setProperty("requestverificationtoken", requestVerificationToken);
                    decryptedLinks.add(dl);
                }
            }
            if (allFolders != null && allFolders.length != 0) {
                for (String[] folder : allFolders) {
                    String folderLink = folder[0];
                    folderLink = "http://chomikuj.pl" + folderLink;
                    if (folderLink.contains(linkPart) && !folderLink.equals(parameter)) {
                        decryptedLinks.add(createDownloadlink(folderLink));
                    }
                }
            }
            progress.increase(1);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private void addRegexInt(int filename, int filenameExt, int filesize, int fileid, int fullfilename) {
        REGEXSORT.clear();
        REGEXSORT.add(filename);
        REGEXSORT.add(filenameExt);
        REGEXSORT.add(filesize);
        REGEXSORT.add(fileid);
        REGEXSORT.add(fullfilename);
    }

    public int getPageCount(String postdata, String theParameter) throws NumberFormatException, DecrypterException, IOException {
        Browser br2 = br.cloneBrowser();
        prepareBrowser(theParameter, br2);
        accessPage(postdata, br2, 1);
        int pageCount = 0;
        int tempint = 0;
        // Loop limited to 20 in case something goes seriously wrong
        for (int i = 0; i <= 20; i++) {
            // Find number of pages
            String pagePiece = br2.getRegex("(class=\"current\">1</li><li><a .*?) class=\"clear\"></div></div>").getMatch(0);
            /** This changes after accessing a page */
            if (pagePiece == null) pagePiece = br2.getRegex("class=\"paginator clear fileListPage\">(<a href=\".*?)class=\"clear\">").getMatch(0);
            if (pagePiece == null) {
                logger.info("pagePiece is null so we should only have one page for this link...");
                pageCount = 0;
                break;
            }
            String[] lolpages = null;
            if (pagePiece != null) lolpages = new Regex(pagePiece, "title=\"\\d+\">(\\d+)</a>").getColumn(0);
            if (lolpages == null || lolpages.length == 0) return -1;
            // Find highest number of page
            for (String page : lolpages) {
                if (Integer.parseInt(page) > tempint) tempint = Integer.parseInt(page);
            }
            if (tempint == pageCount) break;
            // If we find less than 8 pages there are no more so we have to stop this here before getting an error!
            if (tempint < 8) {
                pageCount = tempint;
                break;
            }
            accessPage(postdata, br2, tempint);
            pageCount = tempint;
        }
        return pageCount;
    }

    private void accessPage(String postData, Browser pageBR, int pageNum) throws IOException {
        pageBR.postPage("http://chomikuj.pl/action/Files/FilesList", postData);
    }

    private void prepareBrowser(String parameter, Browser bro) {
        // Not needed but has been implemented so lets use it
        bro.getHeaders().put("Referer", parameter);
        bro.getHeaders().put("Accept", "*/*");
        bro.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        bro.getHeaders().put("Accept-Encoding", "gzip,deflate");
        bro.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        bro.getHeaders().put("Cache-Control", "no-cache");
        bro.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        bro.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        bro.getHeaders().put("Pragma", "no-cache");
    }
}
