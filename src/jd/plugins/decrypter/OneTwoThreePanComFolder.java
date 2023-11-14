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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.OneTwoThreePanCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OneTwoThreePanComFolder extends PluginForDecrypt {
    public OneTwoThreePanComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String API_BASE = "https://www.123pan.com/a/api";
    // public static final String API_BASE_2 = "https://www.123pan.com/b/api";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "123pan.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/s/([A-Za-z0-9\\-_]+)(#fileID=(\\d+))?");
        }
        return ret.toArray(new String[0]);
    }

    private final int maxItemsPerPage = 100;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String shareKey = urlinfo.getMatch(0);
        final String preGivenFileID = urlinfo.getMatch(2);
        Map<String, Object> entries = null;
        String path = this.getAdoptedCloudFolderStructure();
        String currentFolderName = null;
        // TODO: Add support for password protected items
        String passCode = param.getDecrypterPassword();
        boolean isPasswordProtected = false;
        if (preGivenFileID != null) {
            /* Access WebAPI right away. */
            entries = accessPageViaWebAPI(br, shareKey, preGivenFileID, null);
        } else {
            /* Access website/HTML */
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String json = br.getRegex("window\\.g_initialProps = (\\{.+\\});\\s+").getMatch(0);
            final Map<String, Object> root = restoreFromString(json, TypeRef.MAP);
            final Map<String, Object> folderinfomap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "res/data");
            if (Boolean.TRUE.equals(folderinfomap.get("HasPwd"))) {
                // TODO: Add support for password protected items
                logger.info("Password protected items are not yet supported");
                isPasswordProtected = true;
                throw new DecrypterRetryException(RetryReason.PLUGIN_DEFECT, "PASSWORD_PROTECTED_ITEMS_ARE_NOT_YET_SUPPORTED_" + shareKey, "Contact JDownloader support and ask for implementation.");
            }
            /* If we have a single file only, this is the name of the single file. */
            currentFolderName = folderinfomap.get("ShareName").toString();
            entries = (Map<String, Object>) root.get("reslist");
            if (path == null) {
                path = currentFolderName;
            } else {
                path = "/" + currentFolderName;
            }
        }
        if (isPasswordProtected) {
            // TODO: Add support for password protected items
        }
        FilePackage fp = null;
        if (!StringUtils.isEmpty(path)) {
            fp = FilePackage.getInstance();
            fp.setName(path);
        }
        int page = 0;
        do {
            page++;
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            final List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("InfoList");
            for (final Map<String, Object> item : items) {
                final DownloadLink link = this.createDownloadlink("https://www.123pan.com/s/" + shareKey + "#fileID=" + item.get("FileId"));
                final String itemTitle = item.get("FileName").toString();
                if (((Number) item.get("Type")).intValue() == 0) {
                    /* File */
                    final long filesizeBytes = ((Number) item.get("Size")).longValue();
                    final String directurl = (String) item.get("DownloadUrl");
                    link.setFinalFileName(itemTitle);
                    link.setVerifiedFileSize(filesizeBytes);
                    link.setAvailable(true);
                    OneTwoThreePanCom.setEtag(link, item.get("Etag").toString());
                    link.setProperty(OneTwoThreePanCom.PROPERTY_FILENAME, itemTitle);
                    link.setProperty(OneTwoThreePanCom.PROPERTY_S3KEYFLAG, item.get("S3KeyFlag").toString());
                    link.setProperty(OneTwoThreePanCom.PROPERTY_SIZEBYTES, filesizeBytes);
                    link.setProperty(OneTwoThreePanCom.PROPERTY_PARENT_FILE_ID, item.get("ParentFileId"));
                    /* 2023-06-07: Directurl is not given here atm but the field exists so we're taking advantage of it. */
                    if (!StringUtils.isEmpty(directurl)) {
                        link.setProperty(OneTwoThreePanCom.PROPERTY_DIRECTURL, directurl);
                    }
                    link.setMD5Hash(item.get("Etag").toString());
                    /* Do not set relative path and FilePackage for single file items without a folder. */
                    if (!StringUtils.isEmpty(path) && !StringUtils.equals(itemTitle, currentFolderName)) {
                        link.setRelativeDownloadFolderPath(path);
                        link._setFilePackage(fp);
                    }
                } else {
                    /* Folder */
                    if (!StringUtils.isEmpty(path) && !StringUtils.equals(itemTitle, currentFolderName)) {
                        /*
                         * X-th item in crawl-chain: Remember folder-structure as API always only provides title of folders we can currently
                         * see but not the previous folder structure.
                         */
                        link.setRelativeDownloadFolderPath(path + "/" + itemTitle);
                    } else {
                        /* First folder in crawl-chain. */
                        link.setRelativeDownloadFolderPath(itemTitle);
                    }
                }
                if (passCode != null) {
                    link.setDownloadPassword(passCode, Boolean.TRUE);
                }
                ret.add(link);
                distribute(link);
            }
            logger.info("Crawled page " + page + " | Items found on this page: " + items.size() + " | Total: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (items.size() < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains less items than max per page: " + items.size() + "/" + maxItemsPerPage);
                break;
            } else {
                // TODO: Add pagination support
                break;
            }
        } while (true);
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_FOLDER_" + shareKey);
        }
        return ret;
    }

    private Map<String, Object> accessPageViaWebAPI(final Browser br, final String shareKey, final String parentFileID, final String password) throws IOException, PluginException {
        final UrlQuery query = new UrlQuery();
        query.add("limit", Integer.toString(maxItemsPerPage));
        query.add("next", "1");
        query.add("orderBy", "file_name");
        query.add("orderDirection", "asc");
        query.add("shareKey", shareKey);
        if (password != null) {
            query.add("SharePwd", Encoding.urlEncode(password));
        }
        query.add("ParentFileId", "0");
        query.add("Page", "1");
        query.add("event", "homeListFile");
        query.add("operateType", "1");
        br.getPage(API_BASE + "/share/get?limit=" + maxItemsPerPage + "&next=1&orderBy=file_name&orderDirection=asc&shareKey=" + shareKey + "&ParentFileId=" + parentFileID + "&Page=1&event=homeListFile&operateType=4");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
    }
}
