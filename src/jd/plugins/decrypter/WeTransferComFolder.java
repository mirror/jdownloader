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
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.WeTransferCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wetransfer.com" }, urls = { WeTransferComFolder.patternShort + "|" + WeTransferComFolder.patternNormal })
public class WeTransferComFolder extends PluginForDecrypt {
    public WeTransferComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected static final String patternShort  = "https?://(?:we\\.tl|shorturls\\.wetransfer\\.com|go\\.wetransfer\\.com)/([\\w\\-]+)";
    protected static final String patternNormal = "https?://(?:\\w+\\.)?wetransfer\\.com/downloads/(?:[a-f0-9]{46}/[a-f0-9]{46}/[a-f0-9]{4,12}|[a-f0-9]{46}/[a-f0-9]{4,12})";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String contenturl = param.getCryptedUrl();
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
        final Regex urlregex = new Regex(contenturl, "/downloads/([a-f0-9]+)/([a-f0-9]{46})?/?([a-f0-9]+)");
        final String id_main = urlregex.getMatch(0);
        final String recipient_id = urlregex.getMatch(1);
        final String security_hash = urlregex.getMatch(2);
        if (security_hash == null || id_main == null) {
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
        final String refererValue = this.br.getURL();
        final PostRequest post = new PostRequest(br.getURL(("/api/v4/transfers/" + id_main + "/prepare-download")));
        post.getHeaders().put("Accept", "application/json");
        post.getHeaders().put("Content-Type", "application/json");
        post.getHeaders().put("Origin", "https://wetransfer.com");
        post.getHeaders().put("X-Requested-With", " XMLHttpRequest");
        if (csrfToken != null) {
            post.getHeaders().put("X-CSRF-Token", csrfToken);
        }
        post.setPostDataString(JSonStorage.toString(jsonMap));
        br.getPage(post);
        Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        final String state = (String) map.get("state");
        if (!"downloadable".equals(state)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (shortID == null) {
            /* Fallback */
            final String shortened_url = (String) map.get("shortened_url");
            if (shortened_url != null && shortened_url.matches(patternShort)) {
                shortID = new Regex(shortened_url, patternShort).getMatch(0);
            }
        }
        final List<Object> ressourcelist = map.get("files") != null ? (List<Object>) map.get("files") : (List) map.get("items");
        /* TODO: Handle this case */
        // final boolean per_file_download_available = map.containsKey("per_file_download_available") &&
        // Boolean.TRUE.equals(map.get("per_file_download_available"));
        /* TODO: Handle this case */
        // final boolean password_protected = map.containsKey("password_protected") && Boolean.TRUE.equals(map.get("password_protected"));
        /* E.g. okay would be "downloadable" */
        // final String state = (String) map.get("state");
        for (final Object fileo : ressourcelist) {
            final Map<String, Object> entry = (Map<String, Object>) fileo;
            final String id_single = (String) entry.get("id");
            final String absolutePath = (String) entry.get("name");
            final long filesize = JavaScriptEngineFactory.toLong(entry.get("size"), 0);
            if (StringUtils.isEmpty(id_single) || StringUtils.isEmpty(absolutePath) || filesize == 0) {
                continue;
            }
            final DownloadLink dl = this.createDownloadlink("http://wetransferdecrypted/" + id_main + "/" + security_hash + "/" + id_single);
            dl.setReferrerUrl(refererValue);
            String filename = null;
            /* Add folderID as root of the path because otherwise files could be mixed up - there is no real "base folder name" given! */
            String pathForPlugin = null;
            if (absolutePath.contains("/")) {
                /* Looks like we got a subfolder-structure -> Build path */
                if (shortID != null) {
                    pathForPlugin = shortID + "/";
                } else {
                    /* Fallback */
                    pathForPlugin = id_main + "/";
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
                dl.setRelativeDownloadFolderPath(pathForPlugin);
            }
            dl.setFinalFileName(filename);
            dl.setVerifiedFileSize(filesize);
            dl.setContentUrl(contenturl);
            dl.setAvailable(true);
            ret.add(dl);
            /* Set individual packagename per URL because every item can have a totally different file-structure! */
            if (pathForPlugin != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(pathForPlugin);
                dl._setFilePackage(fp);
            }
        }
        return ret;
    }
}
