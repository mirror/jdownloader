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
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kbagi.com" }, urls = { "http://(?:k-?bagi\\.com|kumpulbagi\\.(?:id|com))/[a-z0-9\\-_\\.]+/[a-z0-9\\-_]+(?:/[^\\s]+)?" })
public class KumpulbagiId extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public KumpulbagiId(PluginWrapper wrapper) {
        super(wrapper);
    }

    private DownloadLink getDecryptedDownloadlink() {
        return createDownloadlink("http://kumpulbagidecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000));
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String passCode = null;
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceAll("(?:k-?bagi\\.com|kumpulbagi\\.(?:id|com))/", "k-bagi.com/");
        br.setAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Você não tem permissão para ver este arquivo<"))
        /* No permission to see file/folder */{
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* empty folder | no folder */
        if (!br.containsHTML("name=\"fileId\"")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Differ between single links and folders */
        if (br.containsHTML("id=\"fileDetails\"")) {
            String filename = br.getRegex("Gratis:</span>([^<>\"]*?)</h2>").getMatch(0);
            final String filesize = br.getRegex("class=\"file_size\">\\s*([^<>\"]*?)\\s*</div>").getMatch(0);
            final String fid = br.getRegex("name=\"fileId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
            if (filename == null || filesize == null || fid == null) {
                logger.info("filename: " + filename + ", filesize: " + filesize + ", fid: " + fid);
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
            String fpName = br.getRegex("scrollTop\">([^<>\"]*?)</a>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            }
            String nextPage = "";
            int currentPage = 1;
            do {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
                logger.info("Decrypting page " + currentPage + " of ??");
                String[] linkinfo = br.getRegex("(<div class=\"list_row\".*?class=\"desc\")").getColumn(0);
                if (linkinfo == null || linkinfo.length == 0 || fpName == null) {
                    linkinfo = br.getRegex("(<li data-file-id=\"\\d+\".*?</div>[\t\n\r ]*?</li>)").getColumn(0);
                }
                if (linkinfo == null || linkinfo.length == 0 || fpName == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                // insert ajax stuff
                br.getHeaders().put("Accept", "*/*");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                final Browser org = br.cloneBrowser();
                for (final String lnkinfo : linkinfo) {
                    String filename = null;
                    String filename_url = null;
                    String content_url = new Regex(lnkinfo, "class=\"name\">[\t\n\r ]*?<a href=\"(/[^<>\"]+)\"").getMatch(0);
                    if (content_url != null) {
                        content_url = Request.getLocation(content_url, br.getRequest());
                    } else {
                        content_url = parameter;
                    }
                    final String fid = new Regex(lnkinfo, "data-file-id=\"(\\d+)\"").getMatch(0);
                    final Regex finfo = new Regex(lnkinfo, "<span class=\"bold\">([^<>\"]*?)</span>([^<>\"]*?)</a>");
                    String filesize = new Regex(lnkinfo, "<li><span>([^<>\"]*?)</span></li>").getMatch(0);
                    if (filesize == null) {
                        filesize = new Regex(lnkinfo, "<li>([^<>\"]*?)</li>[\t\n\r ]+<li><span class=\"date\"").getMatch(0);
                        if (filesize == null) {
                            filesize = new Regex(lnkinfo, "class=\"file_size\">([^<>\"]*?)<").getMatch(0);
                            if (filesize == null) {
                                filesize = new Regex(lnkinfo, "class=\"size\">[\t\n\r ]+<p>([^<>\"]*?)</p>").getMatch(0);
                                if (filesize == null) {
                                    filesize = new Regex(lnkinfo, "(\\d+(?:\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
                                }
                            }
                        }
                    }
                    if (fid == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    filename_url = new Regex(content_url, "/([^/]+)$").getMatch(0);
                    if (filename_url != null) {
                        final String trash = new Regex(filename_url, "(," + fid + ",gallery,\\d+,\\d+)\\.[A-Za-z0-9]{2,5}$").getMatch(0);
                        if (trash != null) {
                            filename_url = filename_url.replace(trash, "");
                        }
                    }
                    // String filename = new Regex(lnkinfo, "/([^<>\"/]*?)\" class=\"downloadAction\"").getMatch(0);
                    filename = new Regex(lnkinfo, "\"preview\">([^<>]+)</a>").getMatch(0);
                    String filename_currected = null;
                    if (filename != null) {
                        filename_currected = filename.replace("...", "");
                    }
                    if (filename != null && filename_url != null && (filename_url.length() > filename.length() || filename_url.length() > filename_currected.length()) && !filename_url.contains(",list,")) {
                        filename = filename_url;
                    }
                    logger.info("lnkinfo: " + lnkinfo);
                    if (filename != null) {
                        filename = Encoding.htmlDecode(filename);
                    } else {
                        filename = finfo.getMatch(0);
                        final String ext = finfo.getMatch(1);
                        if (ext == null || filename == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        filename = Encoding.htmlDecode(filename).trim() + Encoding.htmlDecode(ext).trim();
                    }
                    final DownloadLink dl = getDecryptedDownloadlink();
                    dl.setProperty("plain_filename", filename);
                    if (filesize != null) {
                        filesize = Encoding.htmlDecode(filesize).trim();
                        dl.setProperty("plain_filesize", filesize);
                    }
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
                nextPage = br.getRegex("class=\"pageSplitterBorder\" data-nextpage-number=\"" + (currentPage + 1) + "\" data-nextpage-url=\"(.*?)\"").getMatch(0);
                if (nextPage != null) {
                    br = org.cloneBrowser();
                    br.getPage(nextPage);
                    currentPage++;
                }
            } while (nextPage != null);
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
