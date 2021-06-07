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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gofile.io" }, urls = { "https?://(?:www\\.)?gofile\\.io/(?:#download#|\\?c=|d/)([A-Za-z0-9\\-]+)$" })
public class GoFileIo extends PluginForDecrypt {
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderID = new Regex(parameter.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final UrlQuery query = new UrlQuery();
        query.add("folderId", folderID);
        String passCode = null;
        boolean passwordCorrect = true;
        boolean passwordRequired = false;
        int attempt = 0;
        Map<String, Object> response = null;
        final Browser brc = br.cloneBrowser();
        do {
            if (passwordRequired) {
                passCode = getUserInput("Password?", parameter);
                query.addAndReplace("password", JDHash.getSHA256(passCode));
            }
            final GetRequest req = br.createGetRequest("https://api." + this.getHost() + "/getFolder?" + query.toString());
            req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
            req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://gofile.io"));
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
            /* Assume that folder is offline. */
            /* E.g. {"status":"error-notFound","data":{}} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> data = (Map<String, Object>) response.get("data");
        final String folderName = (String) data.get("name");
        FilePackage fp = null;
        if (!StringUtils.isEmpty(folderName) && !folderName.matches("quickUpload_.*")) {
            fp = FilePackage.getInstance();
            fp.setName(folderName);
        }
        final Map<String, Map<String, Object>> files = (Map<String, Map<String, Object>>) data.get("contents");
        for (final Entry<String, Map<String, Object>> file : files.entrySet()) {
            final String fileID = file.getKey();
            final DownloadLink link = createDownloadlink("https://gofile.io/?c=" + folderID + "#file=" + fileID);
            final Map<String, Object> entry = file.getValue();
            final String type = (String) entry.get("type");
            if (!type.equals("file")) {
                logger.info("Unsupported type: " + type);
                continue;
            }
            final String description = (String) entry.get("description");
            jd.plugins.hoster.GofileIo.parseFileInfo(link, entry);
            link.setAvailable(true);
            if (passCode != null) {
                link.setDownloadPassword(passCode);
            }
            if (!StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
            ret.add(link);
        }
        if (fp != null && ret.size() > 1) {
            fp.addLinks(ret);
        }
        return ret;
    }
}
