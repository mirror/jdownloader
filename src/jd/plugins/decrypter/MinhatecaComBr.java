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

import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "minhateca.com.br" }, urls = { "http://([a-z0-9]+\\.)?minhateca\\.com\\.br/.+" }, flags = { 0 })
public class MinhatecaComBr extends PluginForDecrypt {

    public MinhatecaComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String passCode = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML(">Você não tem permissão para ver este arquivo<"))
        /* No permission to see file/folder */{
            final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String chomikid = br.getRegex("type=\"hidden\" name=\"ChomikId\" value=\"(\\d+)\"").getMatch(0);
        final String folderid = br.getRegex("name=\"FolderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
        final String reqtoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (br.containsHTML("class=\"LoginToFolderForm\"")) {
            final String foldername = br.getRegex("id=\"FolderName\" name=\"FolderName\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (reqtoken == null || chomikid == null || folderid == null || foldername == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (int i = 1; i <= 3; i++) {
                passCode = Plugin.getUserInput("Password?", param);
                br.postPageRaw("http://minhateca.com.br/action/Files/LoginToFolder", "Remember=true&Remember=false&ChomikId=" + chomikid + "&FolderId=" + folderid + "&FolderName=" + Encoding.urlEncode(foldername) + "&Password=" + Encoding.urlEncode(passCode) + "&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
                if (br.containsHTML("\"IsSuccess\":false")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("\"IsSuccess\":false")) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            /* We don't want to work with the encoded json bla html response */
            br.getPage(parameter);
        }
        final String server = new Regex(parameter, "(http://[a-z0-9]+\\.minhateca\\.com\\.br/)").getMatch(0);
        if (server != null) {
            /* Find normal links of online viewable doc links */
            parameter = br.getRegex("\"(http://minhateca\\.com\\.br/[^<>\"]*?)\" id=\"dnLink\"").getMatch(0);
            if (parameter == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(parameter);
        }

        /* empty folder | no folder */
        if (br.containsHTML("class=\"noFile\"") || !br.containsHTML("name=\"FolderId\"|id=\"fileDetails\"")) {
            final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        /* Password protected link --> Not yet supported */
        if (br.containsHTML(">Digite senha:</label>")) {
            final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        /* Differ between single links and folders */
        if (br.containsHTML("id=\"fileDetails\"")) {
            String filename = br.getRegex("Baixar: <b>([^<>\"]*?)</b>").getMatch(0);
            final String filesize = br.getRegex("class=\"fileSize\">([^<>\"]*?)</p>").getMatch(0);
            final String fid = br.getRegex("name=\"FileId\" value=\"(\\d+)\"").getMatch(0);
            if (filename == null || filesize == null || fid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename).trim();
            final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));

            dl.setProperty("plain_filename", filename);
            dl.setProperty("plain_filesize", filesize);
            dl.setProperty("plain_fid", fid);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("pass", passCode);
            dl.setProperty("LINKDUPEID", fid + filename);

            try {
                dl.setContentUrl(parameter);
            } catch (final Throwable e) {
                dl.setBrowserUrl(parameter);
            }

            dl.setName(filename);
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setAvailable(true);

            decryptedLinks.add(dl);
        } else {
            final String fpName = br.getRegex("class=\"T_selected\">([^<>\"]*?)<").getMatch(0);
            int startnumber = 1;
            int lastpage = 1;
            /* First check if maybe user wants only a specific page */
            String maxpage = new Regex(parameter, ",(\\d+)$").getMatch(0);
            if (maxpage != null) {
                startnumber = Integer.parseInt(maxpage);
            } else {
                maxpage = br.getRegex("title=\"\\d+\">(\\d+)</a></li></ul><div").getMatch(0);
            }
            if (maxpage != null) {
                lastpage = Integer.parseInt(maxpage);
            }
            for (int i = startnumber; i <= lastpage; i++) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                logger.info("Decrypting page " + i + " of " + lastpage);
                if (i > 1) {
                    br.postPage("http://minhateca.com.br/action/Files/FilesList", "chomikId=" + chomikid + "&folderId=" + folderid + "&fileListSortType=Date&fileListAscending=False&gallerySortType=Name&galleryAscending=False&pageNr=" + i + "&isGallery=False&requestedFolderMode=&folderChanged=false&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
                }
                String[] linkinfo = br.getRegex("<div class=\"fileinfo tab\">(.*?)<span class=\"filedescription\"").getColumn(0);
                if (linkinfo == null || linkinfo.length == 0) {
                    linkinfo = br.getRegex("<p class=\"filename\">(.*?)class=\"fileActionsFacebookSend\"").getColumn(0);
                }
                if (linkinfo == null || linkinfo.length == 0 || fpName == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String lnkinfo : linkinfo) {
                    String content_url = new Regex(linkinfo, "\"(/[^<>\"]*?)\"").getMatch(0);
                    if (content_url != null) {
                        content_url = "http://minhateca.com.br" + content_url;
                    } else {
                        content_url = parameter;
                    }
                    final String fid = new Regex(lnkinfo, "rel=\"(\\d+)\"").getMatch(0);
                    final Regex finfo = new Regex(lnkinfo, "<span class=\"bold\">([^<>\"]*?)</span>([^<>\"]*?)</a>");
                    String filesize = new Regex(lnkinfo, "<li><span>([^<>\"]*?)</span></li>").getMatch(0);
                    if (filesize == null) {
                        filesize = new Regex(lnkinfo, "<li>([^<>\"]*?)</li>[\t\n\r ]+<li><span class=\"date\"").getMatch(0);
                    }
                    if (fid == null || filesize == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    filesize = Encoding.htmlDecode(filesize).trim();
                    String filename = new Regex(lnkinfo, "/([^<>\"/]*?)\" class=\"downloadAction\"").getMatch(0);
                    if (filename == null) {
                        filename = new Regex(lnkinfo, "title=\"([^<>\"]*?)\">").getMatch(0);
                    }
                    if (filename != null) {
                        filename = filename.replace("," + fid, "");
                        filename = Encoding.htmlDecode(filename);
                    } else {
                        if (filename == null) {
                            filename = finfo.getMatch(0);
                        }
                        final String ext = finfo.getMatch(1);
                        if (ext == null || filename == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        filename = Encoding.htmlDecode(filename).trim() + Encoding.htmlDecode(ext).trim();
                    }

                    final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));

                    dl.setProperty("plain_filename", filename);
                    dl.setProperty("plain_filesize", filesize);
                    dl.setProperty("plain_fid", fid);
                    dl.setProperty("mainlink", parameter);
                    dl.setProperty("pass", passCode);
                    dl.setProperty("LINKDUPEID", fid + filename);

                    try {
                        dl.setContentUrl(content_url);
                    } catch (final Throwable e) {
                        dl.setBrowserUrl(content_url);
                    }

                    dl.setName(filename);
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    dl.setAvailable(true);

                    decryptedLinks.add(dl);
                }
            }

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
