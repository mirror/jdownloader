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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "degoo.com" }, urls = { "https?://(?:cloud|app)\\.degoo\\.com/share/([A-Za-z0-9\\-_]+)(.*\\?ID=\\d+)?" })
public class DegooCom extends PluginForDecrypt {
    public DegooCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String fileID = UrlQuery.parse(param.getCryptedUrl()).get("ID");
        if (folderID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setAllowedResponseCodes(new int[] { 400 });
        String path = this.getAdoptedCloudFolderStructure();
        if (path == null) {
            path = "";
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setAllowMerge(true);
        if (!StringUtils.isEmpty(path)) {
            fp.setName(path);
        } else {
            fp.setName(folderID);
        }
        String nextPageToken = null;
        final List<String> dupeList = new ArrayList<String>();
        do {
            final Map<String, Object> params = new HashMap<String, Object>();
            params.put("HashValue", folderID);
            params.put("FileID", fileID);
            params.put("Limit", 100);
            params.put("JWT", null);
            if (nextPageToken != null) {
                params.put("NextToken", nextPageToken);
            }
            br.postPageRaw("https://rest-api.degoo.com/shared", JSonStorage.serializeToJson(params));
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
                /* Offline folder e.g.: {"Error": "Got empty result!"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            nextPageToken = (String) entries.get("NextToken");
            final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) entries.get("Items");
            if (ressourcelist.isEmpty()) {
                if (ret.isEmpty()) {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderID + "_" + path);
                } else {
                    /* Maybe pagination failed -> Allow this to happen */
                    logger.info("Stopping because current page does not contain any items at all");
                    break;
                }
            }
            int page = 0;
            for (final Map<String, Object> resource : ressourcelist) {
                page++;
                logger.info("Crawling page: " + page);
                final String title = (String) resource.get("Name");
                final int filesize = ((Number) resource.get("Size")).intValue();
                final boolean isFolder = ((Boolean) resource.get("IsContainer")).booleanValue();
                // final int categoryID = ((Integer) entries.get("Category")).intValue();
                final String id = Long.toString(((Number) resource.get("ID")).longValue());
                if (StringUtils.isEmpty(title) || id.equals("0")) {
                    /* Skip invalid items */
                    continue;
                }
                if (!isFolder) {
                    /* File */
                    final String directurl = (String) resource.get("URL");
                    final String contentURL = "https://cloud.degoo.com/share/" + folderID + "?ID=" + id;
                    final DownloadLink dl = this.createDownloadlink(contentURL);
                    dl.setContentUrl(contentURL);
                    dl.setAvailable(true);
                    dl.setFinalFileName(title);
                    if (!StringUtils.isEmpty(directurl)) {
                        dl.setProperty(jd.plugins.hoster.DegooCom.PROPERTY_DIRECTURL, directurl);
                    }
                    if (filesize > 0) {
                        dl.setDownloadSize(filesize);
                    }
                    if (!StringUtils.isEmpty(path)) {
                        dl.setRelativeDownloadFolderPath(path);
                    }
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                    ret.add(dl);
                    distribute(dl);
                } else {
                    /* Folder --> Goes back into crawler */
                    final String contentURL = "https://app.degoo.com/share/" + folderID + "?ID=" + id;
                    final DownloadLink dl = this.createDownloadlink(contentURL);
                    if (!StringUtils.isEmpty(title)) {
                        if (StringUtils.isEmpty(path)) {
                            dl.setRelativeDownloadFolderPath(title);
                        } else {
                            dl.setRelativeDownloadFolderPath(path + "/" + title);
                        }
                    }
                    ret.add(dl);
                    distribute(dl);
                }
            }
            if (StringUtils.isEmpty(nextPageToken)) {
                logger.info("Stopping because reached end");
                break;
            } else if (dupeList.contains(nextPageToken)) {
                logger.info("Stopping because current nextPageToken has already been crawled");
                break;
            } else {
                dupeList.add(nextPageToken);
            }
        } while (!this.isAbort());
        return ret;
    }
}
