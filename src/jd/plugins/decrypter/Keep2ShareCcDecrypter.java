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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.utils.DebugMode;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.hoster.K2SApi;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class Keep2ShareCcDecrypter extends PluginForDecrypt {
    public Keep2ShareCcDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String[] domainsK2s                     = new String[] { "k2s.cc", "keep2share.cc", "k2share.cc", "keep2s.cc", "keep2.cc" };
    public static final String[] domainsFileboom                = new String[] { "fileboom.me", "fboom.me" };
    public static final String[] domainsTezfilesAndPublish2     = new String[] { "tezfiles.com", "publish2.me" };
    public static final String   SUPPORTED_LINKS_PATTERN_FILE   = "(?i)/(?:f|file|preview)/(?:info/)?([a-z0-9_\\-]{13,})(/([^/\\?]+))?.*";
    private static final String  SUPPORTED_LINKS_PATTERN_FOLDER = "(?i)/folder(?:/info)?/([a-z0-9]{13,}).*";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(domainsK2s);
        ret.add(domainsFileboom);
        ret.add(domainsTezfilesAndPublish2);
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
            ret.add("https?://(?:[a-z0-9\\-]+\\.)?" + buildHostsPatternPart(domains) + "(" + SUPPORTED_LINKS_PATTERN_FILE + "|" + SUPPORTED_LINKS_PATTERN_FOLDER + ")");
        }
        return ret.toArray(new String[0]);
    }

    public static String fixContentID(final String content_id) {
        if (content_id.length() == 13 && !K2SApi.isSpecialFileID(content_id)) {
            /* Default IDs: Regex allows uppercase but we know that they are lowercase. */
            return content_id.toLowerCase(Locale.ENGLISH);
        } else {
            /* Do not touch ID */
            return content_id;
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final boolean looksLikeSingleFileItem;
        String contentidFromURL = new Regex(param.getCryptedUrl(), SUPPORTED_LINKS_PATTERN_FOLDER).getMatch(0);
        if (contentidFromURL != null) {
            looksLikeSingleFileItem = false;
        } else {
            contentidFromURL = new Regex(param.getCryptedUrl(), SUPPORTED_LINKS_PATTERN_FILE).getMatch(0);
            looksLikeSingleFileItem = true;
        }
        if (contentidFromURL == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String contentid = fixContentID(contentidFromURL);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        // TODO: Add plugin setting for this
        final boolean handleFileLinksInCrawler = DebugMode.TRUE_IN_IDE_ELSE_FALSE;
        if (looksLikeSingleFileItem && !handleFileLinksInCrawler) {
            /* URL looks like single file URL -> Pass to hosterplugin so we can make use of mass-linkchecking feature. */
            ret.add(this.createDownloadlink(param.getCryptedUrl().replaceFirst(Pattern.quote(contentidFromURL), contentid)));
            return ret;
        }
        final K2SApi plugin = (jd.plugins.hoster.K2SApi) getNewPluginForHostInstance(this.getHost());
        br = plugin.createNewBrowserInstance();
        // set cross browser support
        plugin.setBrowser(br);
        // final List<DownloadLink> unavailableFolders = new ArrayList<DownloadLink>();
        // final List<DownloadLink> emptyFolders = new ArrayList<DownloadLink>();
        final String referer = K2SApi.getRefererFromURL(param.getCryptedUrl());
        final boolean addRefererToRequest = false;
        final DownloadLink dummy = this.createDownloadlink(generateFileUrl(contentidFromURL, null, referer));
        final String refererForHttpRequest = plugin.getCustomReferer(dummy);
        Map<String, Object> response = null;
        List<Map<String, Object>> items = null;
        FilePackage fp = null;
        String thisFolderTitle = null;
        try {
            if (looksLikeSingleFileItem) {
                /**
                 * This handling is supposed to be for single files but can also be used for small folders. </br>
                 * Using the folder handling down below for single files will prohibit our special referer handling from working since it
                 * eems to flag the current IP so th referer will be ignored later and users who have configured a special referer will not
                 * get better download speeds anymore. </br>
                 * More detailed explanation: https://board.jdownloader.org/showthread.php?t=94515
                 */
                logger.info("Link looks like single file link -> Jumping into single file handling");
                final HashMap<String, Object> postdataGetfilesinfo = new HashMap<String, Object>();
                postdataGetfilesinfo.put("ids", Arrays.asList(new String[] { contentid }));
                /**
                 * What this returns: </br>
                 * ID leads to a single file: Single file information </br>
                 * ID leads to a single SMALL(!) folder: All folder file items </br>
                 * ID leads to a big folder: Only folder meta-information -> Folder items need to be crawled using a separate request down
                 * below.
                 */
                response = plugin.postPageRaw(br, "https://" + this.getHost() + "/api/v2/getfilesinfo", postdataGetfilesinfo, null);
                if (!"success".equals(response.get("status"))) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                items = (List<Map<String, Object>>) response.get("files");
                if (items.isEmpty()) {
                    logger.info("Empty object -> Empty or invalid folder");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (items != null) {
                    for (Map<String, Object> item : items) {
                        final String id = (String) item.get("id");
                        final String fileOrFolderName = (String) item.get("name");
                        final Boolean isFolder = (Boolean) item.get("is_folder");
                        if (isFolder == null) {
                            /* Most likely we only got one result and that is an offline item. */
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        if (Boolean.FALSE.equals(isFolder)) {
                            final DownloadLink file = createDownloadlink(generateFileUrl(id, fileOrFolderName, referer));
                            K2SApi.parseFileInfo(file, item, contentidFromURL);
                            if (!file.isNameSet()) {
                                /* Fallback */
                                file.setName(id);
                            }
                            ret.add(file);
                        } else {
                            if (id.equals(contentidFromURL)) {
                                logger.info("Looks like this might be a bigger folder -> Jumping into folder crawler");
                                break;
                            } else {
                                // TODO
                            }
                        }
                    }
                }
            }
            final Set<String> dups = new HashSet<String>();
            final int maxItemsPerPage = 50;
            int offset = 0;
            int page = 1;
            boolean isSingleFile = false;
            String sourceFileID = null;
            String path = this.getAdoptedCloudFolderStructure();
            do {
                final HashMap<String, Object> postdataGetfilestatus = new HashMap<String, Object>();
                postdataGetfilestatus.put("id", contentid);
                postdataGetfilestatus.put("limit", maxItemsPerPage);
                postdataGetfilestatus.put("offset", offset);
                if (refererForHttpRequest != null && addRefererToRequest) {
                    postdataGetfilestatus.put("url_referrer", refererForHttpRequest);
                    /* "referer" and "site" are the Browser params */
                    postdataGetfilestatus.put("referer", refererForHttpRequest);
                    postdataGetfilestatus.put("site", "vipergirls.to");
                }
                response = plugin.postPageRaw(br, "/getfilestatus", postdataGetfilestatus, null);
                items = (List<Map<String, Object>>) response.get("files");
                if (items == null && response.containsKey("is_available") && !response.containsKey("id")) {
                    /* Root map contains single loose file. */
                    isSingleFile = true;
                    sourceFileID = contentid;
                    items = new ArrayList<Map<String, Object>>();
                    items.add(response);
                } else {
                    /* Root map is folder containing files */
                    if (fp == null) {
                        fp = FilePackage.getInstance();
                        thisFolderTitle = response.get("name").toString();
                        if (path == null) {
                            path = thisFolderTitle;
                        } else {
                            path += "/" + thisFolderTitle;
                        }
                        fp.setName(path);
                    }
                }
                if (items.isEmpty()) {
                    if (ret.isEmpty()) {
                        if (thisFolderTitle != null) {
                            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, contentid + "_" + thisFolderTitle);
                        } else {
                            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, contentid);
                        }
                    } else {
                        logger.info("Stopping because: Failed to find any items on current page");
                        break;
                    }
                }
                int numberofNewItems = 0;
                for (final Map<String, Object> item : items) {
                    final String id;
                    if (isSingleFile) {
                        id = contentid;
                    } else {
                        id = item.get("id").toString();
                    }
                    if (!dups.add(id)) {
                        continue;
                    }
                    numberofNewItems++;
                    final String filenameOrFoldername = (String) item.get("name");
                    if (Boolean.TRUE.equals(item.get("is_folder"))) {
                        final DownloadLink folder = createDownloadlink(generateFolderUrl(id, filenameOrFoldername, referer));
                        if (path != null) {
                            folder.setRelativeDownloadFolderPath(path);
                        }
                        ret.add(folder);
                        distribute(folder);
                        // if (Boolean.FALSE.equals(item.get("is_available"))) {
                        // unavailableFolders.add(folder);
                        // }
                    } else {
                        /* File */
                        final DownloadLink file = createDownloadlink(generateFileUrl(id, filenameOrFoldername, referer));
                        K2SApi.parseFileInfo(file, item, sourceFileID);
                        if (!file.isNameSet()) {
                            /* Fallback */
                            file.setName(id);
                        }
                        if (fp != null) {
                            file._setFilePackage(fp);
                        }
                        if (path != null) {
                            file.setRelativeDownloadFolderPath(path);
                        }
                        ret.add(file);
                        distribute(file);
                    }
                }
                logger.info("Crawled page " + page + " | Offset: " + offset + " Found items so far: " + ret.size());
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else if (isSingleFile) {
                    logger.info("Stopping because: This item is a single file");
                    break;
                } else if (numberofNewItems == 0) {
                    logger.info("Stopping because: Failed to find any new items on current page: " + page);
                    break;
                } else if (numberofNewItems < maxItemsPerPage) {
                    logger.info("Stopping because: Current page contains less items than " + maxItemsPerPage);
                    break;
                } else {
                    offset += maxItemsPerPage;
                    page++;
                }
            } while (!this.isAbort());
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) {
                /*
                 * Most likely http response 400 bad request -> Due to invalid contentID format -> Effectively this means that the content
                 * we want to access is offline.
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
            } else {
                throw e;
            }
        }
        return ret;
    }

    private String generateFolderUrl(final String folderid, final String foldername, final String referer) {
        String url = "https://" + this.getHost() + "/folder/" + folderid;
        if (foldername != null) {
            url += "/" + Encoding.urlEncode(foldername);
        }
        if (referer != null) {
            url += "?site=" + Encoding.urlEncode(referer);
        }
        return url;
    }

    private String generateFileUrl(final String fileid, final String filename, final String referer) {
        String url = "https://" + this.getHost() + "/file/" + fileid;
        if (filename != null) {
            url += "/" + Encoding.urlEncode(filename);
        }
        if (referer != null) {
            url += "?site=" + Encoding.urlEncode(referer);
        }
        return url;
    }
}
