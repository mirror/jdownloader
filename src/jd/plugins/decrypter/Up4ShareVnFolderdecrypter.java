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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "up.4share.vn" }, urls = { "https?://(?:www\\.)?(?:up\\.)?4share\\.vn/(?:d|dlist)/[a-f0-9]{16}" })
public class Up4ShareVnFolderdecrypter extends PluginForDecrypt {

    public Up4ShareVnFolderdecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("up.4share.vn/", "4share.vn/");
        br.setConnectTimeout(2 * 60 * 1000);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if ((br.containsHTML(">Error: Not valid ID") && !br.containsHTML("up\\.4share\\.vn/f/")) || br.containsHTML("File suspended:") || br.containsHTML("\\[Empty Folder\\]") || !this.br.getURL().matches(".+[a-f0-9]{16}$")) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<b>Thư mục:\\s*(.*?)\\s*</b>").getMatch(0);
        final String[] filter = br.getRegex("<tr>\\s*<td>.*?</td></tr>").getColumn(-1);

        final CrawledLink source = getCurrentLink().getSourceLink();
        final String subfolder;
        if (source != null && source.getDownloadLink() != null && canHandle(source.getURL())) {
            final DownloadLink downloadLink = source.getDownloadLink();
            subfolder = downloadLink.getStringProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, null);
        } else {
            subfolder = null;
        }
        if (filter != null && filter.length > 0) {
            for (final String f : filter) {
                String folder_path = null;
                final String url = new Regex(f, "('|\")((?:https?://(?:up\\.)?4share\\.vn)?/(?:d/[a-f0-9]{16}|f/[a-f0-9]{16}/.*?))\\1").getMatch(1);
                if (url == null) {
                    continue;
                }
                String item_name = new Regex(f, ">\\s*([^<]+)\\s*</a>").getMatch(0);
                if (item_name != null) {
                    item_name = Encoding.htmlDecode(item_name).trim();
                }
                final String size = new Regex(f, ">\\s*(\\d+(?:\\.\\d+)?\\s*(?:B(?:yte)?|KB|MB|GB))\\s*<").getMatch(0);
                final DownloadLink dl = createDownloadlink(Request.getLocation(url, br.getRequest()));
                if (url.contains("f/")) {
                    final String temp_filename;
                    if (item_name != null) {
                        temp_filename = item_name;
                    } else {
                        temp_filename = url.substring(url.lastIndexOf("/") + 1);
                    }
                    dl.setName(temp_filename);
                    if (size != null) {
                        dl.setDownloadSize(SizeFormatter.getSize(size));
                    }
                    dl.setAvailableStatus(AvailableStatus.TRUE);

                    if (subfolder != null) {
                        folder_path = subfolder;
                    }
                } else {
                    if (item_name != null) {
                        if (subfolder != null) {
                            folder_path = subfolder + "/" + item_name;
                        } else {
                            folder_path = "/" + item_name;
                        }
                    }
                }
                if (folder_path != null) {
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, folder_path);
                }
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.isEmpty()) {
            // fail over
            final String[] links = br.getRegex("(?:https?://(?:up\\.)?4share\\.vn)?/(?:d/[a-f0-9]{16}|f/[a-f0-9]{16}/[^<>\"]{1,})").getColumn(-1);
            if (links == null || links.length == 0) {
                if (br.containsHTML("get_link_file_list_in_folder")) {
                    logger.info("Seems like we have an empty folder: " + parameter);
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String dl : links) {
                dl = Request.getLocation(dl, br.getRequest()) + dl;
                final String fid = new Regex(dl, "f/([a-f0-9]{16})/").getMatch(0);
                if (fid != null) {
                    final DownloadLink dll = createDownloadlink(dl);
                    dll.setLinkID(fid);
                    decryptedLinks.add(dll);
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}