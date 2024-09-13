package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.GofileIo;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.parser.UrlQuery;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gofile.io" }, urls = { "https?://(?:www\\.)?gofile\\.io/(?:#download#|\\?c=|d/)([A-Za-z0-9\\-]+)" })
public class GoFileIoCrawler extends PluginForDecrypt {

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 350);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Browser brc = br.cloneBrowser();
        final String token = GofileIo.getAndSetToken(this, brc);
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final UrlQuery query = new UrlQuery();
        query.add("contentId", folderID);
        query.add("token", Encoding.urlEncode(token));
        query.add("wt", GofileIo.getWebsiteToken(this, br));
        String passCode = param.getDecrypterPassword();
        boolean passwordCorrect = true;
        boolean passwordRequired = false;
        int attempt = 0;
        Map<String, Object> response = null;
        Map<String, Object> response_data = null;
        do {
            if (passwordRequired || passCode != null) {
                /* Pre-given password was wrong -> Ask user for password */
                if (attempt > 0 || passCode == null) {
                    passCode = getUserInput("Password?", param);
                }
                query.addAndReplace("password", Hash.getSHA256(passCode));
            }
            final GetRequest req = br.createGetRequest("https://api." + this.getHost() + "/contents/" + folderID + "?" + query.toString());
            req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://" + this.getHost()));
            req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://" + this.getHost()));
            GofileIo.getPage(this, brc, req);
            response = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            response_data = (Map<String, Object>) response.get("data");
            final String passwordStatus = (String) response_data.get("passwordStatus");
            if (passwordStatus != null && (passwordStatus.equalsIgnoreCase("passwordRequired") || passwordStatus.equalsIgnoreCase("passwordWrong"))) {
                passwordRequired = true;
                passwordCorrect = false;
                passCode = null;
                attempt += 1;
                if (attempt >= 3) {
                    break;
                } else {
                    continue;
                }
            } else {
                passwordCorrect = true;
                break;
            }
        } while (!this.isAbort());
        if (passwordRequired && !passwordCorrect) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        if (!"ok".equals(response.get("status"))) {
            final String statustext = (String) response.get("status");
            if ("error-notPremium".equals(statustext)) {
                // {"status":"error-notPremium","data":{}}
                throw new AccountRequiredException();
            } else {
                /* Assume that folder is offline. */
                /* E.g. {"status":"error-notFound","data":{}} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        PluginForHost hosterplugin = null;
        String currentFolderName = (String) response_data.get("name");
        if (currentFolderName.matches("^quickUpload_.+") || currentFolderName.equals(folderID)) {
            /* Invalid value */
            currentFolderName = null;
        }
        String path = this.getAdoptedCloudFolderStructure();
        FilePackage fp = null;
        if (path == null) {
            if (!StringUtils.isEmpty(currentFolderName)) {
                /* No path given yet --> Use current folder name as root folder name */
                path = currentFolderName;
            }
        } else {
            if (!StringUtils.isEmpty(currentFolderName)) {
                path += "/" + currentFolderName;
            }
        }
        if (path != null) {
            fp = FilePackage.getInstance();
            fp.setName(path);
        }
        final String parentFolderShortID = response_data.get("code").toString();
        Map<String, Map<String, Object>> files = (Map<String, Map<String, Object>>) response_data.get("contents");
        if (files == null) {
            /* 2024-03-11 */
            files = (Map<String, Map<String, Object>>) response_data.get("children");
        }
        for (final Entry<String, Map<String, Object>> item : files.entrySet()) {
            final Map<String, Object> entry = item.getValue();
            final String type = (String) entry.get("type");
            if (type.equals("file")) {
                final String fileID = item.getKey();
                final String url = "https://" + getHost() + "/?c=" + folderID + "#file=" + fileID;
                if (hosterplugin == null) {
                    /* Init hosterplugin */
                    hosterplugin = this.getNewPluginForHostInstance(this.getHost());
                }
                final DownloadLink file = new DownloadLink(hosterplugin, null, this.getHost(), url, true);
                GofileIo.parseFileInfo(file, entry);
                file.setProperty(GofileIo.PROPERTY_PARENT_FOLDER_SHORT_ID, parentFolderShortID);
                file.setAvailable(true);
                if (passCode != null) {
                    file.setDownloadPassword(passCode);
                }
                if (path != null) {
                    file.setRelativeDownloadFolderPath(path);
                    file._setFilePackage(fp);
                }
                ret.add(file);
            } else if (type.equals("folder")) {
                /* Subfolder containing more files/folders */
                final DownloadLink folder = this.createDownloadlink("https://" + this.getHost() + "/d/" + entry.get("code"));
                if (passCode != null) {
                    folder.setDownloadPassword(passCode);
                }
                if (path != null) {
                    folder.setRelativeDownloadFolderPath(path);
                }
                ret.add(folder);
            } else {
                /* This should never happen */
                logger.warning("Unsupported type: " + type);
                continue;
            }
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2024-01-26: Try to prevent running into rate-limit. */
        return 1;
    }
}
