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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "4share.vn" }, urls = { "https?://(?:www\\.)?(?:up\\.)?4share\\.vn/(?:d|dlist)/[a-f0-9]{16}" })
public class FourShareVnFolder extends PluginForDecrypt {
    public FourShareVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("up.4share.vn/", "4share.vn/");
        br.setConnectTimeout(2 * 60 * 1000);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">\\s*Error: Not valid ID") && !br.containsHTML("up\\.4share\\.vn/f/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("File suspended:") || br.containsHTML(">\\s*ErrorWeb: Not found folder")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*Empty folder")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        } else if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fpName = br.getRegex("<b>Thư mục:\\s*(.*?)\\s*</b>").getMatch(0);
        final String[] filter = br.getRegex("<tr>\\s*<td>.*?</td></tr>").getColumn(-1);
        final String subfolder = this.getAdoptedCloudFolderStructure();
        if (filter != null && filter.length > 0) {
            for (final String f : filter) {
                String folder_path = null;
                final String url = new Regex(f, "('|\")((?:https?://(?:up\\.)?4share\\.vn)?/(?:d/[a-f0-9]{16}|f/[a-f0-9]{16}(?:/.*?)?))\\1").getMatch(1);
                if (url == null) {
                    continue;
                }
                /* 2019-08-28 */
                String item_name = new Regex(f, "title=\\'([^<>\"\\']+)\\'").getMatch(0);
                if (StringUtils.isEmpty(item_name)) {
                    item_name = new Regex(f, ">\\s*([^<]+)\\s*</a>").getMatch(0);
                }
                if (item_name != null) {
                    /* Old */
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
                    dl.setRelativeDownloadFolderPath(folder_path);
                }
                ret.add(dl);
            }
        }
        if (ret.isEmpty()) {
            // fail over
            final String[] links = br.getRegex("(?:https?://(?:up\\.)?4share\\.vn)?/(?:d/[a-f0-9]{16}|f/[a-f0-9]{16}/[^<>\"]{1,})").getColumn(-1);
            if (links == null || links.length == 0) {
                if (br.containsHTML("get_link_file_list_in_folder")) {
                    logger.info("Seems like we have an empty folder: " + parameter);
                    ret.add(this.createOfflinelink(parameter));
                    return ret;
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
                    ret.add(dll);
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(ret);
        }
        return ret;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}