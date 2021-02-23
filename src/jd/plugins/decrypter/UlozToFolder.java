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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UlozToFolder extends PluginForDecrypt {
    public UlozToFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /* ulozto.net = the english version of the site */
        ret.add(new String[] { "uloz.to", "ulozto.sk", "ulozto.cz", "ulozto.net", "zachowajto.pl" });
        ret.add(new String[] { "pornfile.cz", "pornfile.ulozto.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:download|file)-tracking/[a-f0-9]{96}|folder/[A-Za-z0-9]+.*)");
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    /** 2021-02-11: This host does not like many requests in a short time! */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /** Special (temporary/IP bound) URLs available when searching for files directly on the website of this file-hoster. */
    private static final String TYPE_TRACKING = "https?://[^/]+/(?:download|file)-tracking/[a-f0-9]{96}";
    private static final String TYPE_FOLDER   = "https?://[^/]+/folder/([A-Za-z0-9]+).*";

    /** 2021-02-11: This host is GEO-blocking german IPs! */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_TRACKING)) {
            return crawlSingleURL(param);
        } else {
            return crawlFolder(param);
        }
    }

    private ArrayList<DownloadLink> crawlSingleURL(final CryptedLink param) throws IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.getPage(param.getCryptedUrl());
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        } else {
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    private static final String API_BASE = "https://apis.uloz.to/v5";

    /**
     * 2021-02-16: This uses their API. This will even work in countries they've GEO-blocked as their API does not have this
     * GEO-restriction.
     */
    private ArrayList<DownloadLink> crawlFolder(final CryptedLink param) throws IOException, PluginException, DecrypterException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* TODO */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Auth-Token", "TODO");
        final String rootFolderID = new Regex(param.getCryptedUrl(), TYPE_FOLDER).getMatch(0);
        final String folderID;
        /**
         * Added URLs can contain multiple folderIDs and names each starting from the root e.g.:
         * ulozto.net/folder/<itRoot>/<idSubfolder>/name/<rootFolderName>/<subFolderName>
         */
        final Regex folderURLInfoWithNames = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/folder/([A-Za-z0-9/]*)/name/(.+)");
        final String folderIDsURL;
        // final String folderNameURL;
        if (folderURLInfoWithNames.matches()) {
            folderIDsURL = folderURLInfoWithNames.getMatch(0);
            final String[] folderIDsList = folderIDsURL.split("/");
            /* Last ID = ID of current (sub-)folder */
            folderID = folderIDsList[folderIDsList.length - 1];
            /* Last name = name of current (sub-)folder */
            // folderNameURL = folderNamesURL.getMatch(2);
        } else {
            final Regex folderURLInfoWithoutNames = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/folder/([A-Za-z0-9/]+)");
            folderIDsURL = folderURLInfoWithoutNames.getMatch(0);
            final String[] folderIDsList = folderIDsURL.split("/");
            /* Last ID = ID of current (sub-)folder */
            folderID = folderIDsList[folderIDsList.length - 1];
            /* Internal root folder name */
            // folderNameURL = "";
        }
        br.getPage(API_BASE + "/folder/" + folderID + "/parents-up-to/" + rootFolderID + "?cacheBusting=" + System.currentTimeMillis());
        if (br.getHttpConnection().getResponseCode() == 404) {
            /** 2021-02-16: {"error":30001,"code":404,"message":"Folder 'Detective' not found."} */
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("folder");
        final String folderName = (String) entries.get("name");
        if (StringUtils.isEmpty(folderName)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String passCode = param.getDecrypterPassword();
        final boolean isPasswordProtectedFolder = ((Boolean) entries.get("is_password_protected")).booleanValue();
        final int maxItemsPerPage = 50;
        boolean alreadyAccessedFirstFilesIndex = false;
        if (isPasswordProtectedFolder) {
            /* TODO: Test this */
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) {
                /*
                 * Use password we got when we crawled a folder level above --> We can be sure that this one is correct for all subfolders
                 * too.
                 */
                this.setPasswordHeader(passCode);
            } else {
                boolean success = false;
                for (int i = 0; i <= 2; i++) {
                    passCode = getUserInput("Password?", param);
                    this.setPasswordHeader(passCode);
                    this.accessFolderFiles(folderID, 0, 50);
                    entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    if (br.getHttpConnection().getResponseCode() != 403) {
                        success = true;
                        break;
                    }
                }
                if (!success) {
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                }
                alreadyAccessedFirstFilesIndex = true;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderName);
        // if (!StringUtils.isEmpty(folderName)) {
        // fp.setName(folderName);
        // } else {
        // /* Fallback */
        // fp.setName(folderID);
        // }
        int offset = 0;
        int page = 0;
        logger.info("Crawling files");
        String subfolderPath = this.getAdoptedCloudFolderStructure();
        if (StringUtils.isEmpty(subfolderPath)) {
            subfolderPath = folderName;
        }
        do {
            page += 1;
            logger.info("Crawling files page: " + page);
            /* Do not perform this request if we've already done that before. */
            if (page > 1 || !alreadyAccessedFirstFilesIndex) {
                this.accessFolderFiles(folderID, offset, maxItemsPerPage);
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            }
            final List<Object> ressourcelist = (List<Object>) entries.get("items");
            for (final Object fileO : ressourcelist) {
                entries = (Map<String, Object>) fileO;
                final String filename = (String) entries.get("name");
                final long filesize = ((Number) entries.get("filesize")).longValue();
                final String description = (String) entries.get("description");
                final String url = (String) entries.get("url");
                final boolean isPasswordProtectedFile = ((Boolean) entries.get("password_protected_file")).booleanValue();
                if (StringUtils.isEmpty(url)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + url);
                dl.setFinalFileName(filename);
                dl.setVerifiedFileSize(filesize);
                if (!StringUtils.isEmpty(description)) {
                    dl.setComment(description);
                }
                dl.setAvailable(true);
                if (isPasswordProtectedFile) {
                    dl.setPasswordProtected(true);
                    if (passCode != null) {
                        /* Let's assume that the file password is the same as the folder password */
                        dl.setDownloadPassword(passCode);
                    }
                }
                dl._setFilePackage(fp);
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderPath);
                distribute(dl);
                decryptedLinks.add(dl);
                offset += 1;
            }
            if (ressourcelist.size() < maxItemsPerPage) {
                logger.info("Stopping files crawling because: Reached end");
                break;
            } else if (this.isAbort()) {
                return decryptedLinks;
            }
        } while (true);
        logger.info("Crawling subfolders");
        /* Recycle these previously used variables. */
        page = 0;
        offset = 0;
        do {
            page += 1;
            logger.info("Crawling subfolders page: " + page);
            this.accessFolderSubfolders(folderID, offset, maxItemsPerPage);
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final List<Object> ressourcelist = (List<Object>) entries.get("subfolders");
            for (final Object folderO : ressourcelist) {
                entries = (Map<String, Object>) folderO;
                final String thisfolderName = (String) entries.get("name");
                final String thisfolderID = (String) entries.get("slug");
                if (StringUtils.isEmpty(thisfolderName) || StringUtils.isEmpty(thisfolderID)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink dl = this.createDownloadlink("https://ulozto.net/folder/" + folderIDsURL + "/" + thisfolderID);
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderPath + "/" + thisfolderName);
                if (passCode != null) {
                    /* Important e.g. if main folder is password protected, subfolders will usually be password protected too! */
                    dl.setDownloadPassword(passCode);
                }
                distribute(dl);
                decryptedLinks.add(dl);
                offset += 1;
            }
            if (ressourcelist.size() < maxItemsPerPage) {
                logger.info("Stopping subfolders crawling because: Reached end");
                break;
            } else if (this.isAbort()) {
                return decryptedLinks;
            }
        } while (true);
        return decryptedLinks;
    }

    private void accessFolderFiles(final String folderID, final int offset, final int maxItemsPerPage) throws IOException {
        br.getPage(API_BASE + "/folder/" + folderID + "/file-list?sort=-created&offset=" + offset + "&limit=" + maxItemsPerPage + "&cacheBusting=" + System.currentTimeMillis());
    }

    private void accessFolderSubfolders(final String folderID, final int offset, final int maxItemsPerPage) throws IOException {
        br.getPage(API_BASE + "/folder/" + folderID + "/folder-list?sort=-created&offset=" + offset + "&limit=" + maxItemsPerPage + "&depth=1&cacheBusting=" + System.currentTimeMillis());
    }

    private void setPasswordHeader(final String password) {
        this.br.getHeaders().put("X-Password", password);
    }
}
