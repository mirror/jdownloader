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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "copiapop.com" }, urls = { "http://([a-z0-9]+\\.)?copiapop\\.(?:es|com)/.+" })
public class CopiapopComDecrypter extends PluginForDecrypt {
    public CopiapopComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String fixCryptedLink(final String input) {
        return input.replace("copiapop.es/", "copiapop.com/");
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = fixCryptedLink(param.toString());
        br.getPage(parameter);
        final String server = new Regex(parameter, "(http://[a-z0-9]+\\.copiapop\\.es/)").getMatch(0);
        if (server != null) {
            /* Find normal links of online viewable doc links */
            parameter = br.getRegex("\"(http://copiapop\\.com/[^<>\"]*?)\" id=\"dnLink\"").getMatch(0);
            if (parameter == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(parameter);
        }
        /* empty folder | no folder */
        if (br.containsHTML("class=\"noFile\"") || !br.containsHTML("class=\"tiles_container\"|id=\"fileDetails\"")) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        /* Password protected link --> Not yet supported --> And this code is not yet tested either :D */
        // if (br.containsHTML(">Digite senha:</label>")) {
        // final DownloadLink dl = createDownloadlink("http://copiapopdecrypted.es/" + System.currentTimeMillis() + new
        // Random().nextInt(1000000));
        // dl.setFinalFileName(parameter);
        // dl.setProperty("mainlink", parameter);
        // dl.setProperty("offline", true);
        // decryptedLinks.add(dl);
        // return decryptedLinks;
        // }
        /* Differ between single links and folders */
        if (br.containsHTML("id=\"fileDetails\"")) {
            String filename = br.getRegex("<span>Gratis:</span>([^<>\"]*?)<").getMatch(0);
            final String filesize = br.getRegex("class=\"file_size\">([^<>\"]*?)<").getMatch(0);
            final String fid = br.getRegex("name=\"fileId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
            if (filename == null || filesize == null || fid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename).trim();
            final DownloadLink dl = createDownloadLink(fid, filename, parameter);
            dl.setProperty("plain_filesize", filesize);
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        } else {
            String fpName = br.getRegex("class=\"scrollTop\">([^<>\"]*?)</a></h1>").getMatch(0);
            if (fpName == null) {
                fpName = new Regex(parameter, "copiapop\\.com/(.+)").getMatch(0);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            final short max_entries_per_page = 24;
            int addedlinks = 0;
            int page = 1;
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            boolean stop_after_one_page = false;
            String nextpage = null;
            final String decrypt_specified_page_only_str = new Regex(this.br.getURL(), "(/gallery,\\d+,\\d+)").getMatch(0);
            if (decrypt_specified_page_only_str != null) {
                page = Integer.parseInt(new Regex(decrypt_specified_page_only_str, "gallery,\\d+,(\\d+)").getMatch(0));
                if (page == 1) {
                    /* Remove that part of the url for our calls later! */
                    this.br.getPage(this.br.getURL().replace(decrypt_specified_page_only_str, ""));
                } else {
                    stop_after_one_page = true;
                }
            }
            do {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return decryptedLinks;
                }
                addedlinks = 0;
                if (decryptedLinks.size() > 0 && nextpage != null) {
                    this.br.getPage(nextpage);
                }
                final String[] linkinfo = br.getRegex("(<li data\\-file\\-id=\"\\d+\".*?</li>)").getColumn(0);
                if (linkinfo == null || linkinfo.length == 0) {
                    break;
                }
                for (final String lnkinfo : linkinfo) {
                    String filename = null;
                    final String fid = new Regex(lnkinfo, "data\\-file\\-id=\"(\\d+)\"").getMatch(0);
                    String filesize = new Regex(lnkinfo, "class=\"file_size\">([^<>\"]*?)<").getMatch(0);
                    filename = new Regex(lnkinfo, "/([^<>\"/]*?)\" data-action-before").getMatch(0);
                    if (filename != null) {
                        final String remove_me = new Regex(filename, "(,\\d+,gallery,\\d+,\\d+)\\.[A-Za-z0-9]{2,5}$").getMatch(0);
                        if (remove_me != null) {
                            filename = filename.replace(remove_me, "");
                        }
                    } else {
                        filename = new Regex(lnkinfo, ">([^<>\"]*?)</a>").getMatch(0);
                    }
                    if (fid == null || filename == null || filesize == null) {
                        break;
                    }
                    filesize = Encoding.htmlDecode(filesize).trim();
                    filename = Encoding.htmlDecode(filename).trim();
                    final DownloadLink dl = createDownloadLink(fid, filename, parameter);
                    dl.setProperty("plain_filesize", filesize);
                    dl._setFilePackage(fp);
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    addedlinks++;
                }
                page++;
                nextpage = this.br.getRegex("data-nextpage-number=\"" + page + "\" data\\-nextpage\\-url=\"(/[^<>\"]*?)\"").getMatch(0);
            } while (addedlinks >= max_entries_per_page && !stop_after_one_page && nextpage != null);
            if (decryptedLinks.size() == 0) {
                return null;
            }
        }
        return decryptedLinks;
    }

    public DownloadLink createDownloadLink(final String fid, final String filename, final String main_url) {
        final DownloadLink dl = super.createDownloadlink("http://copiapopdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000));
        dl.setProperty("plain_filename", filename);
        dl.setProperty("plain_fid", fid);
        dl.setProperty("mainlink", main_url);
        dl.setLinkID(fid + filename);
        dl.setName(filename);
        dl.setContentUrl(main_url);
        return dl;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.ChomikujPlScript;
    }
}
