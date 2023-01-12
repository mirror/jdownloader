package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.parser.UrlQuery;

import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GofileIo;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gofile.io" }, urls = { "https?://(?:www\\.)?gofile\\.io/(?:#download#|\\?c=|d/)([A-Za-z0-9\\-]+)$" })
public class GoFileIoCrawler extends PluginForDecrypt {
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Browser brc = br.cloneBrowser();
        final String token = jd.plugins.hoster.GofileIo.getToken(this, brc);
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final UrlQuery query = new UrlQuery();
        query.add("contentId", folderID);
        query.add("websiteToken", GofileIo.getWebsiteToken(this, br));
        query.add("token", Encoding.urlEncode(token));
        query.add("cache", "true");
        String passCode = param.getDecrypterPassword();
        boolean passwordCorrect = true;
        boolean passwordRequired = false;
        int attempt = 0;
        Map<String, Object> response = null;
        do {
            if (passwordRequired || passCode != null) {
                /* Pre-given password was wrong -> Ask user for password */
                if (attempt > 0) {
                    passCode = getUserInput("Password?", param);
                }
                query.addAndReplace("password", JDHash.getSHA256(passCode));
            }
            final GetRequest req = br.createGetRequest("https://api." + this.getHost() + "/getContent?" + query.toString());
            req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://" + this.getHost()));
            req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://" + this.getHost()));
            brc.getPage(req);
            response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            if ("error-passwordRequired".equals(response.get("status")) || "error-passwordWrong".equals(response.get("status"))) {
                passwordRequired = true;
                passwordCorrect = false;
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
            if ("error-notPremium".equals(response.get("status"))) {
                // {"status":"error-notPremium","data":{}}
            }
            /* Assume that folder is offline. */
            /* E.g. {"status":"error-notFound","data":{}} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> data = (Map<String, Object>) response.get("data");
        final String currentFolderName = (String) data.get("name");
        String path = this.getAdoptedCloudFolderStructure();
        FilePackage fp = null;
        if (path == null && !StringUtils.isEmpty(currentFolderName) && !currentFolderName.matches("^quickUpload_.+") && !currentFolderName.equals(folderID)) {
            /* No path given yet --> Use current folder name as root */
            path = currentFolderName;
        }
        if (path != null) {
            fp = FilePackage.getInstance();
            fp.setName(path);
        }
        final Map<String, Map<String, Object>> files = (Map<String, Map<String, Object>>) data.get("contents");
        for (final Entry<String, Map<String, Object>> item : files.entrySet()) {
            final Map<String, Object> entry = item.getValue();
            final String type = (String) entry.get("type");
            if (type.equals("file")) {
                final String fileID = item.getKey();
                final DownloadLink file = createDownloadlink("https://" + this.getHost() + "/?c=" + folderID + "#file=" + fileID);
                GofileIo.parseFileInfo(file, entry);
                file.setAvailable(true);
                if (passCode != null) {
                    file.setDownloadPassword(passCode);
                }
                if (path != null) {
                    file.setRelativeDownloadFolderPath(path);
                }
                ret.add(file);
            } else if (type.equals("folder")) {
                /* Subfolder containing more files/folders */
                final DownloadLink folder = this.createDownloadlink("https://" + this.getHost() + "/d/" + entry.get("code"));
                final String folderName = (String) entry.get("name");
                if (passCode != null) {
                    folder.setDownloadPassword(passCode);
                }
                if (path != null) {
                    folder.setRelativeDownloadFolderPath(path + "/" + folderName);
                }
                ret.add(folder);
            } else {
                /* This should never happen */
                logger.warning("Unsupported type: " + type);
                continue;
            }
        }
        if (fp != null && ret.size() > 1) {
            fp.addLinks(ret);
        }
        return ret;
    }
}
