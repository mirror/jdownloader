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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abelhas.pt" }, urls = { "http://(?!blog)([a-z0-9]+\\.)?abelhas\\.pt/.+" }, flags = { 0 })
public class AbelhasPtDecrypter extends PluginForDecrypt {

    public AbelhasPtDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);
        return ret;
    }

    private static final String TYPE_INVALID = "";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String passCode = null;
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

        final String chomikid = br.getRegex("type=\"hidden\" name=\"ChomikId\" value=\"(\\d+)\"").getMatch(0);
        final String folderid = br.getRegex("name=\"FolderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
        final String reqtoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (br.containsHTML("class=\"LoginToFolderForm\"")) {
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
                    passCode = Plugin.getUserInput("Password?", param);
                }
                br.postPageRaw("http://abelhas.pt/action/Files/LoginToFolder", "Remember=true&Remember=false&ChomikId=" + chomikid + "&FolderId=" + folderid + "&FolderName=" + Encoding.urlEncode(foldername) + "&Password=" + Encoding.urlEncode(passCode) + "&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
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

        /* empty folder | no folder */
        if (br.containsHTML("class=\"noFile\"") || !br.containsHTML("name=\"FolderId\"|id=\"fileDetails\"")) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML("ico/adult_medium\\.png\"")) {
            /* Adult link */
            final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            dl.setContentUrl(param.getCryptedUrl());
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (!br.containsHTML("class=\"fileinfo tab\"|id=\"fileDetails\"")) {
            /* Link redirected to mainpage or category page --> Offline */
            final DownloadLink dl = this.createOfflinelink(parameter);
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
            dl.setProperty("pass", passCode);

            dl.setName(filename);
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setAvailable(true);

            decryptedLinks.add(dl);
        } else {
            int currentpage = 0;
            int maxPage = 0;
            String param_current_page = new Regex(parameter, ",(\\d+)$").getMatch(0);
            if (param_current_page != null) {
                maxPage = Integer.parseInt(param_current_page);
                currentpage = maxPage;
                parameter = parameter.substring(0, parameter.lastIndexOf(","));
            } else {
                final String[] pageNums = br.getRegex(",(\\d+)\" class=\"\" rel=\"\\d+\" title=\"\\d+\">\\d+</a>").getColumn(0);
                for (final String pageNum : pageNums) {
                    final int curpgnum = Integer.parseInt(pageNum);
                    if (curpgnum > maxPage) {
                        maxPage = curpgnum;
                    }
                }
            }
            String fpName = br.getRegex("class=\"T_selected\">(.*?)</span></a").getMatch(0);
            if (fpName != null) {
                fpName = fpName.replaceAll("<span.*?>\\s*</span>", "");
            }
            if (fpName == null) {
                fpName = new Regex(parameter, "abelhas\\.pt/(.+)").getMatch(0);
            }
            do {
                logger.info("Decrypting page " + currentpage + " of " + maxPage);
                if (currentpage > 1) {
                    br.getPage(parameter + "," + currentpage);
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
                    linkinfo = br.getRegex("class=\"filerow fileItemContainer\"(.*?)</ul>[\t\n\r ]*?</div>[\t\n\r ]*?</div>").getColumn(0);
                }
                if (linkinfo == null || linkinfo.length == 0) {
                    linkinfo = br.getRegex("class=\"filerow fileItemContainer\"(.*?)class=\"fileCommentsAction\"").getColumn(0);
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
                    dl.setProperty("pass", passCode);
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
                currentpage++;
            } while (currentpage <= maxPage);

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }
}
