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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kumpulbagi.com" }, urls = { "http://kumpulbagi\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+" }, flags = { 0 })
public class KumpulbagiCom extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public KumpulbagiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* ChomikujPlScript */

    private DownloadLink getDecryptedDownloadlink() {
        return createDownloadlink("http://kumpulbagidecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000));
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String passCode = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable ethr) {
                /* Not available in 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        if (br.containsHTML(">Você não tem permissão para ver este arquivo<"))
        /* No permission to see file/folder */{
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable ethr) {
                /* Not available in 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        final String chomikid = br.getRegex("type=\"hidden\" name=\"ChomikId\" value=\"(\\d+)\"").getMatch(0);
        final String folderid = br.getRegex("name=\"FolderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
        final String reqtoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        // if (br.containsHTML("class=\"LoginToFolderForm\"")) {
        // final String foldername = br.getRegex("id=\"FolderName\" name=\"FolderName\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        // if (reqtoken == null || chomikid == null || folderid == null || foldername == null) {
        // logger.warning("Decrypter broken for link: " + parameter);
        // return null;
        // }
        // for (int i = 1; i <= 3; i++) {
        // passCode = Plugin.getUserInput("Password?", param);
        // br.postPageRaw("http://minhateca.com.br/action/Files/LoginToFolder", "Remember=true&Remember=false&ChomikId=" + chomikid +
        // "&FolderId=" + folderid + "&FolderName=" + Encoding.urlEncode(foldername) + "&Password=" + Encoding.urlEncode(passCode) +
        // "&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
        // if (br.containsHTML("\"IsSuccess\":false")) {
        // continue;
        // }
        // break;
        // }
        // if (br.containsHTML("\"IsSuccess\":false")) {
        // throw new DecrypterException(DecrypterException.PASSWORD);
        // }
        // /* We don't want to work with the encoded json bla html response */
        // br.getPage(parameter);
        // }

        /* empty folder | no folder */
        if (!br.containsHTML("id=\"fileId\"")) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable ethr) {
                /* Not available in 0.9.581 Stable */
            }
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
            final String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            int lastpage = 1;
            int tempmaxpage = 0;
            do {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                lastpage = tempmaxpage;
                logger.info("Decrypting page " + lastpage + " of ??");
                if (lastpage > 1) {
                    br.getPage(parameter + "/gallery,1," + lastpage);
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
                if (linkinfo == null || linkinfo.length == 0) {
                    linkinfo = br.getRegex("(<li data-file-id=.*?)</li>").getColumn(0);
                }
                if (linkinfo == null || linkinfo.length == 0 || fpName == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String lnkinfo : linkinfo) {
                    String content_url = new Regex(lnkinfo, "class=\"name\"><a href=\"(/[^<>\"]*?)\"").getMatch(0);
                    if (content_url != null) {
                        content_url = "http://" + this.getHost() + content_url;
                    } else {
                        content_url = parameter;
                    }
                    final String fid = new Regex(lnkinfo, "data\\-file\\-id=\"(\\d+)\"").getMatch(0);
                    final Regex finfo = new Regex(lnkinfo, "<span class=\"bold\">([^<>\"]*?)</span>([^<>\"]*?)</a>");
                    String filesize = new Regex(lnkinfo, "<li><span>([^<>\"]*?)</span></li>").getMatch(0);
                    if (filesize == null) {
                        filesize = new Regex(lnkinfo, "<li>([^<>\"]*?)</li>[\t\n\r ]+<li><span class=\"date\"").getMatch(0);
                    }
                    if (filesize == null) {
                        filesize = new Regex(lnkinfo, "class=\"file_size\">([^<>\"]*?)<").getMatch(0);
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
                    if (filename == null) {
                        filename = new Regex(content_url, "/([^<>\"/]+)$").getMatch(0);
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

                    final DownloadLink dl = getDecryptedDownloadlink();

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
                final String[] pagenumbers = br.getRegex("lass=\"pageSplitter\"><a href=\"/[^<>\"]*?\">(\\d+)</a>").getColumn(0);
                for (final String pge : pagenumbers) {
                    final int pageint = Integer.parseInt(pge);
                    if (pageint > lastpage) {
                        tempmaxpage = pageint;
                        break;
                    }
                }
            } while (tempmaxpage > lastpage);

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }
}
