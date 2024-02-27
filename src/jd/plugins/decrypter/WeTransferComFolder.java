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
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.WeTransferCom;
import jd.plugins.hoster.WeTransferCom.WetransferConfig;
import jd.plugins.hoster.WeTransferCom.WetransferConfig.CrawlMode;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wetransfer.com" }, urls = { WeTransferComFolder.patternShort + "|" + WeTransferComFolder.patternNormal })
public class WeTransferComFolder extends PluginForDecrypt {
    public WeTransferComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    protected static final String patternShort       = "https?://(?:we\\.tl|shorturls\\.wetransfer\\.com|go\\.wetransfer\\.com)/([\\w\\-]+)";
    protected static final String patternNormal      = "https?://(?:\\w+\\.)?wetransfer\\.com/downloads/(?:[a-f0-9]{46}/[a-f0-9]{46}/[a-f0-9]{4,12}|[a-f0-9]{46}/[a-f0-9]{4,12})";
    // TODO: Add crawler support for boards
    private static final Pattern  PATTERN_COLLECTION = Pattern.compile("(?i)https?://(boards|collect)\\.wetransfer\\.com/board/([a-z0-9]+)");

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String contenturl = param.getCryptedUrl();
        final Regex collection = new Regex(contenturl, PATTERN_COLLECTION);
        if (collection.patternFind()) {
            // TODO: Add support
            if (true) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String collectionID = collection.getMatch(0);
            String token = this.getPluginConfig().getStringProperty("api_token");
            int counter = 0;
            boolean authed = false;
            do {
                /* Clear old headers and cookies each loop */
                counter++;
                WeTransferCom.prepBRAPI(br);
                if (token == null) {
                    /* Only generate new token if needed */
                    synchronized ("TODO") {
                        /*
                         * 2019-09-30: E.g. {"device_token":
                         * "wt-android-<hash-length-8>-<hash-length-4>-<hash-length-4>-<hash-length-4>-<hash-length-12>"}
                         */
                        br.postPageRaw(WeTransferCom.API_BASE_AUTH + "/authorize", "{\"device_token\":\"wt-android-\"}");
                        token = PluginJSonUtils.getJson(br, "token");
                        if (StringUtils.isEmpty(token)) {
                            logger.warning("Failed to authorize");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        /* Save token for to eventually re-use it later */
                        this.getPluginConfig().setProperty("api_token", token);
                    }
                }
                br.getHeaders().put("Authorization", "Bearer " + token);
                br.getPage(WeTransferCom.API_BASE_NORMAL + "/mobile/collections/" + collectionID);
                if (br.getHttpConnection().getResponseCode() == 401) {
                    /* 2019-09-30 {"error":"Invalid JWT token : Signature Verification Error"} */
                    token = null;
                    continue;
                }
                authed = true;
                break;
            } while (!this.isAbort());
            if (!authed) {
                logger.warning("Authorization error");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                /* 2019-09-30 e.g. {"success":false,"message":"This collection does not exist","error_key":"board_deleted"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        WeTransferCom.prepBRWebsite(this.br);
        String shortID = new Regex(contenturl, patternShort).getMatch(0);
        final boolean accessURL;
        if (shortID != null) {
            /* Short link */
            br.getPage(contenturl);
            /* Redirects to somewhere */
            if (!this.canHandle(br.getURL())) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            contenturl = br.getURL();
            accessURL = false;
        } else {
            accessURL = true;
        }
        final Regex urlregex = new Regex(contenturl, "(?i)/downloads/([a-f0-9]+)/([a-f0-9]{46})?/?([a-f0-9]+)");
        final String folder_id = urlregex.getMatch(0);
        final String recipient_id = urlregex.getMatch(1);
        final String security_hash = urlregex.getMatch(2);
        if (security_hash == null || folder_id == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (accessURL) {
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 410 || br.getHttpConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String csrfToken = br.getRegex("name\\s*=\\s*\"csrf-token\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
        // final String domain_user_id = br.getRegex("user\\s*:\\s*\\{\\s*\"key\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final Map<String, Object> jsonMap = new HashMap<String, Object>();
        jsonMap.put("security_hash", security_hash);
        if (recipient_id != null) {
            jsonMap.put("recipient_id", recipient_id);
        }
        final String refererValue = br.getURL();
        final PostRequest post = new PostRequest(br.getURL(("/api/v4/transfers/" + folder_id + "/prepare-download")));
        post.getHeaders().put("Accept", "application/json");
        post.getHeaders().put("Content-Type", "application/json");
        post.getHeaders().put("Origin", "https://wetransfer.com");
        post.getHeaders().put("X-Requested-With", " XMLHttpRequest");
        if (csrfToken != null) {
            post.getHeaders().put("X-CSRF-Token", csrfToken);
        }
        post.setPostDataString(JSonStorage.serializeToJson(jsonMap));
        br.getPage(post);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String state = (String) entries.get("state");
        if (!"downloadable".equals(state)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String thisFolderName = (String) entries.get("display_name");
        final String description = (String) entries.get("description");
        if (shortID == null) {
            /* Fallback */
            final String shortened_url = (String) entries.get("shortened_url");
            if (shortened_url != null && shortened_url.matches(patternShort)) {
                shortID = new Regex(shortened_url, patternShort).getMatch(0);
            }
        }
        final WetransferConfig cfg = PluginJsonConfig.get(WetransferConfig.class);
        final CrawlMode mode = cfg.getCrawlMode();
        final DownloadLink zip = this.createDownloadlink(createDummylinkForHosterplugin(folder_id, security_hash, "ffffffffffffffffffffffffffffffffffffffffffffff"));
        final String zipFilename = (String) entries.get("recommended_filename");
        if (zipFilename != null) {
            zip.setName(zipFilename);
        }
        zip.setDownloadSize(((Number) entries.get("size")).longValue());
        zip.setAvailable(true);
        zip.setProperty(WeTransferCom.PROPERTY_SINGLE_ZIP, true);
        final FilePackage fpZIP = FilePackage.getInstance();
        if (thisFolderName != null) {
            fpZIP.setName(thisFolderName);
        } else if (zipFilename != null) {
            fpZIP.setName(zipFilename);
        }
        if (description != null) {
            fpZIP.setComment(description);
        }
        zip._setFilePackage(fpZIP);
        if (mode == CrawlMode.ZIP) {
            /* Return zip only */
            ret.add(zip);
        } else {
            final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) entries.get("items");
            /* TODO: Handle this case */
            // final boolean per_file_download_available = map.containsKey("per_file_download_available") &&
            // Boolean.TRUE.equals(map.get("per_file_download_available"));
            /* TODO: Handle this case */
            // final boolean password_protected = map.containsKey("password_protected") &&
            // Boolean.TRUE.equals(map.get("password_protected"));
            /* E.g. okay would be "downloadable" */
            // final String state = (String) map.get("state");
            final Map<String, FilePackage> packagemap = new HashMap<String, FilePackage>();
            FilePackage lastFilePackage = null;
            String lastPathForPlugin = null;
            for (final Map<String, Object> resource : ressourcelist) {
                final String file_id = resource.get("id").toString();
                final String absolutePath = (String) resource.get("name");
                final long filesize = ((Number) resource.get("size")).longValue();
                final DownloadLink link = this.createDownloadlink(createDummylinkForHosterplugin(folder_id, security_hash, file_id));
                String filename = null;
                /*
                 * Add folderID as root of the path because otherwise files could be mixed up - there is no real "base folder name" given!
                 */
                String pathForPlugin = null;
                if (absolutePath.contains("/")) {
                    /* Looks like we got a subfolder-structure -> Build path */
                    if (shortID != null) {
                        pathForPlugin = shortID + "/";
                    } else {
                        /* Fallback */
                        pathForPlugin = folder_id + "/";
                    }
                    final String[] urlSegments = absolutePath.split("/");
                    filename = urlSegments[urlSegments.length - 1];
                    pathForPlugin += absolutePath.substring(0, absolutePath.lastIndexOf("/"));
                } else {
                    /* In this case given name/path really is only the filename without path -> File of root folder */
                    filename = absolutePath;
                    /* Path == root */
                }
                if (pathForPlugin != null) {
                    link.setRelativeDownloadFolderPath(pathForPlugin);
                    lastPathForPlugin = pathForPlugin;
                }
                link.setFinalFileName(filename);
                link.setVerifiedFileSize(filesize);
                link.setContentUrl(contenturl);
                link.setAvailable(true);
                ret.add(link);
                /* Set individual packagename per URL because every item can have a totally different file-structure! */
                if (pathForPlugin != null) {
                    FilePackage fp = packagemap.get(pathForPlugin);
                    if (fp == null) {
                        fp = FilePackage.getInstance();
                        fp.setName(pathForPlugin);
                        if (description != null) {
                            fp.setComment(description);
                        }
                        fp.setAllowMerge(true);
                        packagemap.put(pathForPlugin, fp);
                    }
                    link._setFilePackage(fp);
                    lastFilePackage = fp;
                }
            }
            if (mode == CrawlMode.ALL) {
                /* Add single .zip */
                // if (lastFilePackage != null) {
                // zip._setFilePackage(lastFilePackage);
                // }
                // if (lastPathForPlugin != null) {
                // zip.setRelativeDownloadFolderPath(lastPathForPlugin);
                // }
                ret.add(zip);
            }
        }
        /* Add some properties */
        for (final DownloadLink result : ret) {
            result.setReferrerUrl(refererValue);
        }
        return ret;
    }

    private String createDummylinkForHosterplugin(final String folder_id, final String security_hash, final String file_id) {
        return "http://wetransferdecrypted/" + folder_id + "/" + security_hash + "/" + file_id;
    }
}
