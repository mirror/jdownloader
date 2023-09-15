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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "4share.vn" }, urls = { "https?://(?:www\\.)?(?:up\\.)?4share\\.vn/(?:d|dlist)/([a-f0-9]{16})" })
public class FourShareVnFolder extends PluginForDecrypt {
    public FourShareVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replace("up.4share.vn/", "4share.vn/");
        final String folderID = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        if (folderID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setConnectTimeout(2 * 60 * 1000);
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*Error: Not valid ID") && !br.containsHTML("up\\.4share\\.vn/f/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("File suspended:") || br.containsHTML(">\\s*ErrorWeb: Not found folder")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*Empty folder")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        } else if (!this.canHandle(br.getURL())) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] filter = br.getRegex("<tr>\\s*<td>.*?</td></tr>").getColumn(-1);
        String currentFolderTitle = br.getRegex("<h1[^>]*>\\s*Folder: ([^<]+)</h1>").getMatch(0);
        if (currentFolderTitle == null) {
            currentFolderTitle = br.getRegex("(?i)<b>\\s*Thư mục:\\s*(.*?)\\s*</b>").getMatch(0);
        }
        if (currentFolderTitle == null) {
            /* Fallback */
            currentFolderTitle = folderID;
        }
        currentFolderTitle = Encoding.htmlDecode(currentFolderTitle).trim();
        String subfolderpath = this.getAdoptedCloudFolderStructure();
        if (subfolderpath == null) {
            subfolderpath = currentFolderTitle;
        } else {
            subfolderpath += "/" + currentFolderTitle;
        }
        FilePackage fp = null;
        if (subfolderpath != null) {
            fp = FilePackage.getInstance();
            fp.setName(subfolderpath);
        }
        if (filter != null && filter.length > 0) {
            for (final String f : filter) {
                final String url = new Regex(f, "(/(d/[a-f0-9]{16}|f/[a-f0-9]{16}(?:/.*?)?))('|\")").getMatch(0);
                if (url == null) {
                    continue;
                }
                /* 2019-08-28 */
                String item_title = new Regex(f, "title=\\'([^<>\"\\']+)\\'").getMatch(0);
                if (StringUtils.isEmpty(item_title)) {
                    item_title = new Regex(f, ">\\s*([^<]+)\\s*</a>").getMatch(0);
                }
                if (item_title != null) {
                    item_title = Encoding.htmlDecode(item_title).trim();
                }
                final DownloadLink dl = createDownloadlink(br.getURL(url).toExternalForm());
                if (url.contains("/f/")) {
                    /* File */
                    final String filesizeStr = new Regex(f, ">\\s*(\\d+(?:\\.\\d+)?\\s*(?:B(?:yte)?|KB|MB|GB))\\s*<").getMatch(0);
                    final String temp_filename;
                    if (item_title != null) {
                        temp_filename = item_title;
                    } else {
                        /* Fallback */
                        temp_filename = url.substring(url.lastIndexOf("/") + 1);
                    }
                    dl.setName(temp_filename);
                    if (filesizeStr != null) {
                        dl.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                    }
                    dl.setAvailableStatus(AvailableStatus.TRUE);
                    if (subfolderpath != null) {
                        dl.setRelativeDownloadFolderPath(subfolderpath);
                        dl._setFilePackage(fp);
                    }
                } else {
                    /* Subfolder */
                    if (item_title != null) {
                        dl.setRelativeDownloadFolderPath(subfolderpath);
                    }
                }
                ret.add(dl);
            }
        }
        if (ret.isEmpty()) {
            // fail over
            final String[] links = br.getRegex("(?:https?://(?:up\\.)?4share\\.vn)?/(?:d/[a-f0-9]{16}|f/[a-f0-9]{16}/[^<>\"]{1,})").getColumn(-1);
            if (links == null || links.length == 0) {
                if (br.containsHTML("get_link_file_list_in_folder")) {
                    logger.info("Seems like we have an empty folder: " + contenturl);
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            for (String dl : links) {
                dl = Request.getLocation(dl, br.getRequest()) + dl;
                final String fid = new Regex(dl, "f/([a-f0-9]{16})/").getMatch(0);
                if (fid != null) {
                    final DownloadLink file = createDownloadlink(dl);
                    ret.add(file);
                }
            }
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}