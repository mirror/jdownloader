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
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "degoo.com" }, urls = { "https?://(?:cloud|app)\\.degoo\\.com/share/([A-Za-z0-9]+)(.*\\?ID=\\d+)?" })
public class DegooCom extends PluginForDecrypt {
    public DegooCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String fileID = UrlQuery.parse(parameter).get("ID");
        br.setAllowedResponseCodes(new int[] { 400 });
        String path = this.getAdoptedCloudFolderStructure();
        if (path == null) {
            path = "";
        }
        FilePackage fp = null;
        if (!StringUtils.isEmpty(path)) {
            fp = FilePackage.getInstance();
            fp.setProperty("ALLOW_MERGE", true);
            fp.setName(path);
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
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            nextPageToken = (String) entries.get("NextToken");
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("Items");
            if (ressourcelist.size() == 0) {
                if (decryptedLinks.size() == 0) {
                    logger.info("Empty folder");
                    final DownloadLink offline = this.createOfflinelink(parameter);
                    if (!StringUtils.isEmpty(path)) {
                        offline.setName(path);
                    }
                    decryptedLinks.add(offline);
                    return decryptedLinks;
                } else {
                    /* Maybe pagination failed -> Account for this */
                    logger.info("Stopping because current page does not contain any items at all");
                    break;
                }
            }
            int page = 0;
            for (final Object ressourceO : ressourcelist) {
                page++;
                logger.info("Crawling page: " + page);
                entries = (HashMap<String, Object>) ressourceO;
                final String title = (String) entries.get("Name");
                final int filesize = ((Integer) entries.get("Size")).intValue();
                final boolean isFolder = ((Boolean) entries.get("IsContainer")).booleanValue();
                // final int categoryID = ((Integer) entries.get("Category")).intValue();
                final String id = Long.toString(((Long) entries.get("ID")).longValue());
                if (StringUtils.isEmpty(title) || id.equals("0")) {
                    /* Skip invalid items */
                    continue;
                }
                if (!isFolder) {
                    /* File */
                    final String directurl = (String) entries.get("URL");
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
                        dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path);
                    }
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                    decryptedLinks.add(dl);
                    distribute(dl);
                } else {
                    /* Folder --> Goes back into crawler */
                    final String contentURL = "https://app.degoo.com/share/" + folderID + "?ID=" + id;
                    final DownloadLink dl = this.createDownloadlink(contentURL);
                    if (!StringUtils.isEmpty(title)) {
                        if (StringUtils.isEmpty(path)) {
                            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, title);
                        } else {
                            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path + "/" + title);
                        }
                    }
                    decryptedLinks.add(dl);
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
        return decryptedLinks;
    }
}
