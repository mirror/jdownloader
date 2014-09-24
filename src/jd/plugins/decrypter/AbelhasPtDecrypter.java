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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abelhas.pt" }, urls = { "http://([a-z0-9]+\\.)?abelhas\\.pt/.+" }, flags = { 0 })
public class AbelhasPtDecrypter extends PluginForDecrypt {

    public AbelhasPtDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);

        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            try {
                dl.setContentUrl(param.getCryptedUrl());
            } catch (Throwable e1) {
                // jd09
            }
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String server = new Regex(parameter, "(http://[a-z0-9]+\\.abelhas\\.pt/)").getMatch(0);
        if (server != null) {
            /* Find normal links of online viewable doc links */
            parameter = br.getRegex("\"(http://abelhas\\.pt/[^<>\"]*?)\" id=\"dnLink\"").getMatch(0);
            if (parameter == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(parameter);
        }

        /* empty folder | no folder */
        if (br.containsHTML("class=\"noFile\"") || !br.containsHTML("name=\"FolderId\"|id=\"fileDetails\"")) {
            final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            try {
                dl.setContentUrl(parameter);
            } catch (Throwable e1) {
                // jd09
            }
            try {
                dl.setContentUrl(param.getCryptedUrl());
            } catch (Throwable e1) {
                // jd09
            }
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML("ico/adult_medium\\.png\"")) {
            /* Adult link */
            final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            try {
                dl.setContentUrl(param.getCryptedUrl());
            } catch (Throwable e1) {
                // jd09
            }
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.getURL().length() <= 30) {
            /* Link redirected to mainpage or category page --> Offline */
            final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            try {
                dl.setContentUrl(param.getCryptedUrl());
            } catch (Throwable e1) {
                // jd09
            }
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML("id=\"ProtectedFolderChomikLogin\"")) {
            /* Password protected link --> Not yet supported --> Offline */
            final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            try {
                dl.setContentUrl(param.getCryptedUrl());
            } catch (Throwable e1) {
                // jd09
            }
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        /* Differ between single links and folders */
        if (br.containsHTML("id=\"fileDetails\"")) {
            String filename = br.getRegex("Download: <b>([^<>\"]*?)</b>").getMatch(0);
            final String filesize = br.getRegex("class=\"fileSize\">([^<>\"]*?)</p>").getMatch(0);
            final String fid = br.getRegex("name=\"FileId\" value=\"(\\d+)\"").getMatch(0);
            if (filename == null || filesize == null || fid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename).trim();
            final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            try {
                dl.setContentUrl(param.getCryptedUrl());
            } catch (Throwable e1) {
                // jd09
            }
            dl.setProperty("plain_filename", filename);
            dl.setProperty("plain_filesize", filesize);
            dl.setProperty("plain_fid", fid);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("LINKDUPEID", fid + filename);

            dl.setName(filename);
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setAvailable(true);

            decryptedLinks.add(dl);
        } else {
            int maxPage = 1;
            final String[] pageNums = br.getRegex("rel=\"(\\d+)\" title=\"página seguinte").getColumn(0);
            for (final String pageNum : pageNums) {
                final int curpgnum = Integer.parseInt(pageNum);
                if (curpgnum > maxPage) {
                    maxPage = curpgnum;
                }
            }
            String fpName = br.getRegex("class=\"T_selected\">([^<>\"]*?)<").getMatch(0);
            if (fpName == null) {
                fpName = new Regex(parameter, "abelhas\\.pt/(.+)").getMatch(0);
            }
            for (int i = 1; i <= maxPage; i++) {
                logger.info("Decrypting page " + i + " of " + maxPage);
                if (i > 1) {
                    br.getPage(parameter + "," + i);
                }
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }

                String[] linkinfo = br.getRegex(" class=\"fileinfo tab\">(.*?)<span class=\"filedescription\"").getColumn(0);
                if (linkinfo == null || linkinfo.length == 0) {
                    linkinfo = br.getRegex("class=\"filename\">(.*?)class=\"directFileLink\"").getColumn(0);
                }
                if (linkinfo == null || linkinfo.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String lnkinfo : linkinfo) {
                    final String fid = new Regex(lnkinfo, "rel=\"(\\d+)\"").getMatch(0);
                    final Regex finfo = new Regex(lnkinfo, "<span class=\"bold\">([^<>\"]*?)</span>([^<>\"]*?)</a>");
                    String filename = finfo.getMatch(0);
                    final String ext = finfo.getMatch(1);
                    String filesize = new Regex(lnkinfo, "<li><span>([^<>\"]*?)</span></li>").getMatch(0);
                    if (filesize == null) {
                        filesize = new Regex(lnkinfo, "<li>([^<>\"]*?)</li>").getMatch(0);
                    }
                    String mainlink = new Regex(lnkinfo, "class=\"directFileLink\" rel=\"(http://[^<>\"]*?)\"").getMatch(0);
                    if (mainlink == null) {
                        mainlink = new Regex(lnkinfo, "downloadAction\" href=\"(/[^<>\"]*?)\"").getMatch(0);
                    }
                    if (mainlink == null) {
                        mainlink = br.getRegex("\"(/[^<>\"]*?)\" class=\"downloadAction\"").getMatch(0);
                    }
                    if (fid == null || filename == null || ext == null || filesize == null || mainlink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    if (mainlink.startsWith("/")) {
                        mainlink = "http://abelhas.pt" + mainlink;
                    }
                    filesize = Encoding.htmlDecode(filesize).trim();
                    filename = Encoding.htmlDecode(filename).trim() + Encoding.htmlDecode(ext).trim();

                    final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new Random().nextInt(1000000));
                    try {
                        dl.setContentUrl(mainlink);
                        dl.setContainerUrl(param.getCryptedUrl());
                    } catch (Throwable e1) {
                        // jd09
                    }
                    dl.setProperty("plain_filename", filename);
                    dl.setProperty("plain_filesize", filesize);
                    dl.setProperty("plain_fid", fid);
                    dl.setProperty("mainlink", mainlink);
                    dl.setProperty("LINKDUPEID", fid + filename);
                    try {/* JD2 only */
                        dl.setContentUrl(mainlink);
                    } catch (Throwable e) {/* Stable */
                        dl.setBrowserUrl(mainlink);
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
