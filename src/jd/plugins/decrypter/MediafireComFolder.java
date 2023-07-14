//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.MediafireCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class MediafireComFolder extends PluginForDecrypt {
    public MediafireComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mediafire.com", "mfi.re" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static final String  TYPE_DIRECT        = "(?i)https?://download\\d+.mediafire(?:cdn)?\\.com/[^/]+/([a-z0-9]+)/([^/\"']+)";
    private static final String PATTERN_CONTENT_ID = "[a-z0-9]+";

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            final String hostPatternPart = buildHostsPatternPart(domains);
            String pattern = "https?://(?:www\\.)?" + hostPatternPart + "/.+";
            pattern += "|" + TYPE_DIRECT;
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    public static boolean isFolderID(final String contentid) {
        if (contentid.matches("[A-Za-z0-9]{13}")) {
            return true;
        } else {
            return false;
        }
    }

    // https://www.mediafire.com/developers/core_api/1.5/getting_started/#error_codes
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        String directurl = null;
        final String fid = getFileIDFRomURL(parameter);
        final Regex direct = new Regex(parameter, TYPE_DIRECT);
        if (direct.matches()) {
            directurl = parameter;
        }
        final String multipleContentIDsCommaSeparated = new Regex(param.getCryptedUrl(), "https?://[^/]+/\\?([a-z0-9,]+)").getMatch(0);
        if (multipleContentIDsCommaSeparated != null) {
            /* Multiple files in one link */
            final String[] contentIDs = multipleContentIDsCommaSeparated.split(",");
            for (final String contentID : contentIDs) {
                final DownloadLink link;
                if (isFolderID(contentID)) {
                    link = this.createDownloadlink("https://www.mediafire.com/folder/" + contentID);
                } else {
                    link = createSingleFileDownloadlink(contentID, null);
                }
                ret.add(link);
            }
            return ret;
        } else if (fid != null) {
            /* Single file */
            final String filenameFromURL = Plugin.getFileNameFromURL(param.getCryptedUrl());
            final DownloadLink file = createSingleFileDownloadlink(fid, filenameFromURL);
            ret.add(file);
            if (directurl != null) {
                MediafireCom.storeDirecturl(file, null, directurl);
            }
            return ret;
        } else {
            return crawlFolder(param);
        }
    }

    /** Returnes fileID if given URL is any supported single file download URL. */
    public static String getFileIDFRomURL(final String url) throws MalformedURLException {
        final UrlQuery query = UrlQuery.parse(url);
        String fid = query.get("quickkey");
        if (fid == null) {
            fid = new Regex(url, "(?i)https?://[^/]+/(?:download|file|file_premium|listen|watch|view)/(" + PATTERN_CONTENT_ID + ")").getMatch(0);
            if (fid == null) {
                fid = new Regex(url, "(?i)https?://[^/]+/i\\?(" + PATTERN_CONTENT_ID + ")").getMatch(0);
                if (fid == null) {
                    fid = new Regex(url, "(?i)https?://[^/]+/\\?(" + PATTERN_CONTENT_ID + ")").getMatch(0);
                    if (fid == null) {
                        fid = new Regex(url, TYPE_DIRECT).getMatch(0);
                    }
                }
            }
        }
        return fid;
    }

    private ArrayList<DownloadLink> crawlFolder(final CryptedLink param) throws Exception {
        try {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            this.setBrowserExclusive();
            final String folderurl = param.getCryptedUrl();
            final String sharekeyPattern = "[a-z0-9]+";
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            final MediafireCom hosterplugin = (MediafireCom) this.getNewPluginForHostInstance(this.getHost());
            if (account != null) {
                hosterplugin.login(br, account, false);
            }
            String newSharekey = new Regex(folderurl, "(?i)/folder/(" + sharekeyPattern + ")").getMatch(0);
            if (newSharekey == null) {
                logger.info("Detected old sharekey --> Trying to get corresponding new sharekey");
                br.setFollowRedirects(true);
                br.getPage(folderurl);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                newSharekey = br.getRegex("sharekey=(" + sharekeyPattern + ")").getMatch(0);
                if (newSharekey == null) {
                    newSharekey = br.getRegex("afI\\s*=\\s*\"(" + sharekeyPattern + ")\"").getMatch(0);
                }
                if (newSharekey == null) {
                    logger.warning("Unable to find new 'quick_key' --> URL must be offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    logger.info("Found new 'sharekey': " + newSharekey);
                }
            }
            final UrlQuery fileFolderQuery = new UrlQuery().add("folder_key", Encoding.urlEncode(newSharekey));
            // apiRequest(this.br, "https://www.mediafire.com/api/folder/get_info.php", fquery);
            final Map<String, Object> foldermap = hosterplugin.apiCommand(param.getDownloadLink(), account, "folder/get_info.php", fileFolderQuery);
            final Map<String, Object> folder_info = (Map<String, Object>) foldermap.get("folder_info");
            final String folderDescription = (String) folder_info.get("description");
            final String currentFolderName = folder_info.get("name").toString();
            final int filesNum = Integer.parseInt(folder_info.get("file_count").toString());
            final int foldersNum = Integer.parseInt(folder_info.get("folder_count").toString());
            if (filesNum == 0 && foldersNum == 0) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
            }
            String subFolderPath = getAdoptedCloudFolderStructure();
            if (subFolderPath == null) {
                subFolderPath = currentFolderName;
            } else {
                subFolderPath += "/" + currentFolderName;
            }
            /* Crawl files */
            if (filesNum > 0) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(subFolderPath);
                if (!StringUtils.isEmpty(folderDescription)) {
                    fp.setComment(folderDescription);
                }
                final ArrayList<DownloadLink> filearray = new ArrayList<DownloadLink>();
                int page = 1;
                fileFolderQuery.addAndReplace("content_type", "files");
                do {
                    fileFolderQuery.addAndReplace("chunk", Integer.toString(page));
                    final Map<String, Object> resp = hosterplugin.apiCommand(param.getDownloadLink(), account, "folder/get_content.php", fileFolderQuery);
                    final Map<String, Object> folder_content = (Map<String, Object>) resp.get("folder_content");
                    final List<Map<String, Object>> files = (List<Map<String, Object>>) folder_content.get("files");
                    for (final Map<String, Object> file : files) {
                        final String url = JavaScriptEngineFactory.walkJson(file, "links/normal_download").toString();
                        final DownloadLink link = createDownloadlink(url);
                        MediafireCom.parseFileInfo(link, file);
                        link.setProperty(MediafireCom.PROPERTY_FILE_ID, file.get("quickkey"));
                        link.setRelativeDownloadFolderPath(subFolderPath);
                        link._setFilePackage(fp);
                        filearray.add(link);
                        distribute(link);
                    }
                    logger.info("Crawled files page " + page + " | Found files: " + filearray.size() + "/" + filesNum);
                    if (this.isAbort()) {
                        logger.info("Stopping because: Aborted by user");
                        break;
                    } else if (!"yes".equalsIgnoreCase(folder_content.get("more_chunks").toString())) {
                        logger.info("Stopping because: Reached last page");
                        break;
                    } else {
                        page++;
                    }
                } while (true);
                ret.addAll(filearray);
            }
            /* Crawl subfolders */
            if (foldersNum > 0) {
                final ArrayList<DownloadLink> folderarray = new ArrayList<DownloadLink>();
                int page = 1;
                fileFolderQuery.addAndReplace("content_type", "folders");
                do {
                    fileFolderQuery.addAndReplace("chunk", Integer.toString(page));
                    final Map<String, Object> resp = hosterplugin.apiCommand(param.getDownloadLink(), account, "folder/get_content.php", fileFolderQuery);
                    final Map<String, Object> folder_content = (Map<String, Object>) resp.get("folder_content");
                    final List<Map<String, Object>> folders = (List<Map<String, Object>>) folder_content.get("folders");
                    for (final Map<String, Object> folder : folders) {
                        final DownloadLink link = createDownloadlink("https://www.mediafire.com/folder/" + folder.get("folderkey"));
                        link.setRelativeDownloadFolderPath(subFolderPath);
                        folderarray.add(link);
                        distribute(link);
                    }
                    logger.info("Crawled folders page " + page + " | Found subfolders: " + folderarray.size() + "/" + foldersNum);
                    if (this.isAbort()) {
                        logger.info("Stopping because: Aborted by user");
                        break;
                    } else if (!"yes".equalsIgnoreCase(folder_content.get("more_chunks").toString())) {
                        logger.info("Stopping because: Reached last page");
                        break;
                    } else {
                        page++;
                    }
                } while (true);
                ret.addAll(folderarray);
            }
            return ret;
        } catch (final PluginException e) {
            /* Treat any errormessage as "folder offline". */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private DownloadLink createSingleFileDownloadlink(final String fileID, final String filename) {
        if (StringUtils.isEmpty(fileID)) {
            return null;
        }
        String url = "https://www.mediafire.com/file/" + fileID;
        if (!StringUtils.isEmpty(filename)) {
            url += "/" + Encoding.urlEncode(filename);
        }
        final DownloadLink link = createDownloadlink(url);
        link.setProperty(MediafireCom.PROPERTY_FILE_ID, fileID);
        return link;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}