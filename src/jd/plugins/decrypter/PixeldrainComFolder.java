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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;

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
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.PixeldrainCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { PixeldrainCom.class })
public class PixeldrainComFolder extends PluginForDecrypt {
    public PixeldrainComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return PixeldrainCom.getPluginDomains();
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

    private static final String PATTERN_LIST   = "/l/([A-Za-z0-9]+)((?:\\?embed)?#item=(\\d+))?";
    private static final String PATTERN_FOLDER = "/d/(([A-Za-z0-9]{8})(/.*)?)";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_FOLDER + "|" + PATTERN_LIST + ")");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex listregex = new Regex(param.getCryptedUrl(), PATTERN_LIST);
        final Regex folderregex = new Regex(param.getCryptedUrl(), PATTERN_FOLDER);
        final PluginForHost hosterplugin = this.getNewPluginForHostInstance(getHost());
        /* Use same browser settings/headers as hosterplugin. */
        this.br = hosterplugin.createNewBrowserInstance();
        if (listregex.patternFind()) {
            final String listID = listregex.getMatch(0);
            br.getPage(PixeldrainCom.API_BASE + "/list/" + listID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* 2020-10-01: E.g. {"success":false,"value":"not_found","message":"The entity you requested could not be found"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> folder = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String folderName = (String) folder.get("title");
            final List<Map<String, Object>> files = (List<Map<String, Object>>) folder.get("files");
            if (files.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_LIST_l_" + listID, "This list exists but is empty.", null);
            }
            final Number targetIndex;
            final String targetIndexStr = listregex.getMatch(2);
            if (targetIndexStr != null) {
                targetIndex = Integer.parseInt(targetIndexStr);
            } else {
                targetIndex = null;
            }
            int index = 0;
            for (final Map<String, Object> file : files) {
                final DownloadLink dl = this.createDownloadlink(generateFileURL(file.get("id").toString()));
                dl.setContentUrl(generateContentURL(listID, index));
                dl.setContainerUrl(param.getCryptedUrl());
                PixeldrainCom.setDownloadLinkInfo(this, dl, null, file);
                if (targetIndex != null && index == targetIndex.intValue()) {
                    /* User wants only one item within that folder */
                    logger.info("Found target-file at index: " + index + " | " + dl.getFinalFileName() + " | Returning only this file!");
                    ret.clear();
                    ret.add(dl);
                    break;
                } else {
                    ret.add(dl);
                    index += 1;
                }
            }
            final FilePackage fp = FilePackage.getInstance();
            if (!StringUtils.isEmpty(folderName)) {
                fp.setName(folderName);
            } else {
                /* Fallback */
                fp.setName(listID);
            }
            fp.addLinks(ret);
        } else if (folderregex.patternFind()) {
            /**
             * 2024-06-10: This is new. It's not yet documented in their API docs. </br>
             * User docs: https://pixeldrain.com/filesystem
             */
            final String urlWithoutParams = URLHelper.getUrlWithoutParams(param.getCryptedUrl());
            final Regex urlWithoutParamsRegex = new Regex(urlWithoutParams, PATTERN_FOLDER);
            final String folderPath = urlWithoutParamsRegex.getMatch(0);
            if (folderPath == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Append "?stat" so even if we got a direct-URL we will get a json response and not a file. */
            br.getPage(PixeldrainCom.API_BASE + "/filesystem/" + folderPath + "?stat");
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* E.g. {"success":false,"value":"not_found","message":"The entity you requested could not be found"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> folder = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> children = (List<Map<String, Object>>) folder.get("children");
            final List<Map<String, Object>> pathlist = (List<Map<String, Object>>) folder.get("path");
            final List<Map<String, Object>> fileitems;
            final boolean lookForSingleFile;
            if (children.isEmpty() && pathlist.size() > 0) {
                /* Given URL goes to a single file so there are no children - the item itself is "the next upper item". */
                fileitems = pathlist;
                lookForSingleFile = true;
            } else {
                fileitems = children;
                lookForSingleFile = false;
            }
            if (fileitems.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_FOLDER_" + folderPath, "This folder exists but is empty.", null);
            }
            final PluginForHost pixeldrainHosterplugin = this.getNewPluginForHostInstance(getHost());
            final String folderID = urlWithoutParamsRegex.getMatch(1);
            final String thisUrlPath = urlWithoutParamsRegex.getMatch(2);
            String rootFolderName = null;
            /* Find root folder name */
            for (final Map<String, Object> pathitem : pathlist) {
                final String type = pathitem.get("type").toString();
                final String path = pathitem.get("path").toString();
                if (type.equalsIgnoreCase("dir") && path.equals("/" + folderID)) {
                    rootFolderName = pathitem.get("name").toString();
                    break;
                }
            }
            logger.info("FolderID: " + folderID + " | Numberof children: " + children.size() + " | Root folder name: " + rootFolderName);
            final Map<String, FilePackage> fpmap = new HashMap<String, FilePackage>();
            int numberofSkippedFolders = 0;
            for (final Map<String, Object> fileitem : fileitems) {
                final String pathUnchanged = fileitem.get("path").toString();
                final DownloadLink dl;
                final boolean isFile = fileitem.get("type").toString().equalsIgnoreCase("file");
                if (isFile) {
                    /* File */
                    /* Get path to folder without filename. */
                    String pathToThisFile = pathUnchanged.substring(0, pathUnchanged.lastIndexOf("/"));
                    if (rootFolderName != null) {
                        /* If available, we want to use the real root folder name as root folder name and not the root folder ID. */
                        pathToThisFile = pathToThisFile.replaceFirst("/" + folderID, rootFolderName);
                    }
                    FilePackage fp = fpmap.get(pathToThisFile);
                    if (fp == null && !StringUtils.isEmpty(pathToThisFile)) {
                        fp = FilePackage.getInstance();
                        fp.setName(pathToThisFile);
                        fpmap.put(pathToThisFile, fp);
                    }
                    /* Generate direct-downloadable URL. */
                    String fileurl = pathUnchanged;
                    if (fileurl.startsWith("/")) {
                        fileurl = "/api/filesystem" + fileurl;
                    }
                    // fileurl = br.getURL(fileurl).toExternalForm() + "?attach";
                    fileurl = br.getURL(fileurl).toExternalForm();
                    dl = this.createDownloadlink(fileurl);
                    dl.setFinalFileName(fileitem.get("name").toString());
                    dl.setDownloadSize(((Number) fileitem.get("file_size")).longValue());
                    dl.setSha256Hash(fileitem.get("sha256_sum").toString());
                    if (!StringUtils.isEmpty(pathToThisFile)) {
                        dl.setRelativeDownloadFolderPath(pathToThisFile);
                    }
                    /* We know that this file is online so we can set the AvailableStatus right away. */
                    dl.setAvailable(true);
                    /* Make this item be handled by our pixeldrain hoster plugin. */
                    dl.setDefaultPlugin(pixeldrainHosterplugin);
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                    if (lookForSingleFile && thisUrlPath != null && pathUnchanged.endsWith(thisUrlPath)) {
                        /* We want to have a specific single file of this folder. */
                        ret.clear();
                        ret.add(dl);
                        break;
                    }
                } else {
                    /* Subfolder - will go back into this crawler for processing. */
                    String folderurl = pathUnchanged;
                    if (folderurl.startsWith("/")) {
                        folderurl = "/d" + folderurl;
                    }
                    folderurl = br.getURL(folderurl).toExternalForm();
                    dl = this.createDownloadlink(folderurl);
                    if (lookForSingleFile) {
                        /* We want to have a specific single file -> Skip folders */
                        numberofSkippedFolders += 1;
                        continue;
                    }
                }
                ret.add(dl);
            }
            logger.info("Results: " + ret.size() + " | Skipped folders: " + numberofSkippedFolders);
            if (lookForSingleFile && ret.isEmpty()) {
                /* Single desired file was not found */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private String generateFileURL(final String fileID) {
        return "https://" + getHost() + "/u/" + fileID;
    }

    private String generateContentURL(final String folderID, final int folderIndex) {
        return "https://" + getHost() + "/l/" + folderID + "#item=" + folderIndex;
    }
}
