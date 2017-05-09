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
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "minhateca.com.br" }, urls = { "https?://([a-z0-9]+\\.)?minhateca\\.com\\.br/.+" })
public class MinhatecaComBr extends PluginForDecrypt {
    public MinhatecaComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private DownloadLink getDecryptedDownloadlink() {
        return createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String passCode = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("http://", "https://");
        br.setAllowedResponseCodes(new int[] { 401 });
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Você não tem permissão para ver este arquivo<"))
        /* No permission to see file/folder */{
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form loginToProtectedWindow = br.getFormbyActionRegex("LoginToProtectedWindow");
        if (loginToProtectedWindow != null) {
            final String protectPassword = Plugin.getUserInput("Account password?", param);
            if (protectPassword == null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            loginToProtectedWindow.put("Password", Encoding.urlEncode(protectPassword));
            br.submitForm(loginToProtectedWindow);
            loginToProtectedWindow = br.getFormbyActionRegex("LoginToProtectedWindow");
            if (loginToProtectedWindow != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            br.getPage(parameter);
        }
        final String chomikid = br.getRegex("type=\"hidden\" name=\"ChomikId\" value=\"(\\d+)\"").getMatch(0);
        final String folderid = br.getRegex("name=\"FolderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
        final String reqtoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (br.containsHTML("class=\"LoginToFolderForm\"")) {
            // folder password
            final String foldername = br.getRegex("id=\"FolderName\" name=\"FolderName\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (reqtoken == null || chomikid == null || folderid == null || foldername == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            boolean success = false;
            for (int i = 0; i <= 2; i++) {
                if (i == 0) {
                    passCode = this.getPluginConfig().getStringProperty("last_used_password", null);
                }
                if (passCode == null) {
                    passCode = Plugin.getUserInput("Folder password?", param);
                }
                br.postPageRaw("http://minhateca.com.br/action/Files/LoginToFolder", "Remember=true&Remember=false&ChomikId=" + chomikid + "&FolderId=" + folderid + "&FolderName=" + Encoding.urlEncode(foldername) + "&Password=" + Encoding.urlEncode(passCode) + "&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
                if (br.containsHTML("\"IsSuccess\":false")) {
                    this.getPluginConfig().setProperty("last_used_password", Property.NULL);
                    continue;
                }
                success = true;
                this.getPluginConfig().setProperty("last_used_password", passCode);
                break;
            }
            if (!success) {
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
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Password protected link --> Not yet supported */
        if (br.containsHTML(">Digite senha:</label>")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
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
            final DownloadLink dl = getDecryptedDownloadlink();
            dl.setProperty("plain_filename", filename);
            dl.setProperty("plain_filesize", filesize);
            dl.setProperty("plain_fid", fid);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("pass", passCode);
            dl.setContentUrl(parameter);
            dl.setLinkID(getHost() + "://" + fid);
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
            final boolean specificPage;
            if (maxpage != null) {
                specificPage = true;
                startnumber = Integer.parseInt(maxpage);
            } else {
                specificPage = false;
                maxpage = br.getRegex("title=\"\\d+\">(\\d+)</a></li></ul><div").getMatch(0);
                if (maxpage == null) {
                    maxpage = br.getRegex("title=\"\\d+ \\.\\.\\.\">(\\d+) \\.\\.\\.</a></li></ul><div").getMatch(0);
                }
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
                    maxpage = br.getRegex("title=\"\\d+\">(\\d+)</a></li></ul><div").getMatch(0);
                    if (maxpage == null) {
                        maxpage = br.getRegex("title=\"\\d+ \\.\\.\\.\">(\\d+) \\.\\.\\.</a></li></ul><div").getMatch(0);
                    }
                    if (maxpage != null) {
                        lastpage = Math.max(lastpage, Integer.parseInt(maxpage));
                    }
                }
                String[] linkinfo = br.getRegex("<div class=\"fileinfo tab\">(.*?)<span class=\"filedescription\"").getColumn(0);
                if (linkinfo == null || linkinfo.length == 0) {
                    linkinfo = br.getRegex("<p class=\"filename\">(.*?)class=\"fileActionsFacebookSend\"").getColumn(0);
                }
                if (linkinfo == null || linkinfo.length == 0) {
                    linkinfo = br.getRegex("<div class=\"filerow fileItemContainer\">(.*?)class=\"fileCommentsAction\"").getColumn(0);
                }
                if (linkinfo == null || linkinfo.length == 0) {
                    linkinfo = br.getRegex("class=\"filename\"(.*?)class=\"showSharedOptions\"").getColumn(0);
                }
                if (linkinfo == null || linkinfo.length == 0 || fpName == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String lnkinfo : linkinfo) {
                    String url_filename = null;
                    String content_url = new Regex(lnkinfo, "\"(/[^<>\"]*?)\" class=\"downloadAction").getMatch(0);
                    if (content_url == null) {
                        content_url = new Regex(lnkinfo, "\"(/[^<>\"]*?)\"").getMatch(0);
                    }
                    if (content_url != null) {
                        content_url = br.getURL(content_url).toString();
                        url_filename = new Regex(content_url, "/([^<>\"/]+)$").getMatch(0);
                    } else {
                        content_url = parameter;
                    }
                    String filename = new Regex(lnkinfo, "/([^<>\"/]*?)\" class=\"downloadAction").getMatch(0);
                    if (filename == null) {
                        filename = url_filename;
                    }
                    String fid = new Regex(filename, ",(\\d+)\\..+$").getMatch(0);
                    if (fid == null) {
                        fid = new Regex(lnkinfo, "rel=\"(\\d+)\"").getMatch(0);
                    }
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
                    String ext = null;
                    final String finfoName;
                    if (finfo.getMatch(0) != null && finfo.getMatch(1) != null) {
                        finfoName = Encoding.htmlDecode(finfo.getMatch(0)).trim() + Encoding.htmlDecode(finfo.getMatch(1)).trim();
                    } else {
                        finfoName = null;
                    }
                    if (filename != null) {
                        if (filename.contains("*") && finfoName != null) {
                            filename = finfoName;
                        } else {
                            filename = filename.replace("," + fid, "");
                            filename = Encoding.htmlDecode(filename);
                            if (filename.contains(".")) {
                                final String old_ext = filename.substring(filename.lastIndexOf("."));
                                if (!old_ext.matches("\\.[A-Za-z0-9]+")) {
                                    ext = new Regex(old_ext, "(\\.[A-Za-z0-9]+)").getMatch(0);
                                    if (ext != null) {
                                        filename = filename.replace(old_ext, ext);
                                    }
                                }
                            }
                        }
                    } else {
                        filename = finfoName;
                    }
                    if (filename == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final DownloadLink dl = getDecryptedDownloadlink();
                    dl.setProperty("plain_filename", filename);
                    dl.setProperty("plain_filesize", filesize);
                    dl.setProperty("plain_fid", fid);
                    dl.setProperty("mainlink", parameter);
                    dl.setProperty("pass", passCode);
                    dl.setContentUrl(content_url);
                    dl.setLinkID(getHost() + "://" + fid);
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.ChomikujPlScript;
    }
}
