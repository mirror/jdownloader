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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.DispositionHeader;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionObject;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { jd.plugins.hoster.GoogleDriveDirectoryIndex.class })
public class GoogleDriveDirectoryIndex extends antiDDoSForDecrypt {
    private static final String PROPERTY_FOLDER_USE_OLD_POST_REQUEST = "folder_use_old_post_request";

    private static List<String[]> getPluginDomains() {
        return jd.plugins.hoster.GoogleDriveDirectoryIndex.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Crawler plugin that can handle instances of this project:
     * https://gitlab.com/ParveenBhadooOfficial/Google-Drive-Index/-/blob/master/README.md or:</br> https://github.com/alx-xlx/goindex </br>
     * Be sure to add all domains to host plugin GoogleDriveDirectoryIndex.java too!
     */
    public GoogleDriveDirectoryIndex(PluginWrapper wrapper) {
        super(wrapper);
    }

    public int getMaxConcurrentProcessingInstances() {
        /* Without this we'll run into Cloudflare rate limits (error 500) */
        return 1;
    }

    private final static ThreadLocal<String> atobResult = new ThreadLocal<String>();

    public static String atob(String input) {
        final String ret = Encoding.Base64Decode(input);
        atobResult.set(ret);
        return ret;
    }

    private String decodeJSON(final String string) throws Exception {
        if (string != null && string.matches("(?s)^\\s*\\{.*\\}\\s*$")) {
            return string;
        } else {
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            final Context jsContext = Context.enter();
            atobResult.set(null);
            try {
                engine.eval(IO.readInputStreamToString(getClass().getResourceAsStream("/org/jdownloader/plugins/components/GoogleDriveDirectoryIndex.js")));
                final Method atob = getClass().getMethod("atob", new Class[] { String.class });
                engine.put("atob", new FunctionObject("atob", atob, jsContext.initStandardObjects()));
                engine.eval("var result=gdidecode(read(\"" + string + "\"));");
                final String result = StringUtils.valueOfOrNull(engine.get("result"));
                return result;
            } catch (Exception e) {
                final String result = atobResult.get();
                if (result != null) {
                    logger.log(e);
                    return result;
                } else {
                    throw e;
                }
            } finally {
                Context.exit();
                atobResult.set(null);
            }
        }
    }

    private boolean useOldPostRequest(final CryptedLink param) {
        return false;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.toString().contains("?")) {
            /* Remove all URL parameters */
            param.setCryptedUrl(param.getCryptedUrl().substring(0, param.getCryptedUrl().lastIndexOf("?")));
        }
        br.setAllowedResponseCodes(new int[] { 500 });
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null) {
            final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.GoogleDriveDirectoryIndex) plg).login(acc, false);
        }
        boolean useOldPostRequest;
        /* Check if we maybe already know which request type is the right one so we need less http requests. */
        if (param.getDownloadLink() != null && param.getDownloadLink().hasProperty(PROPERTY_FOLDER_USE_OLD_POST_REQUEST)) {
            useOldPostRequest = true;
        } else {
            useOldPostRequest = useOldPostRequest(param);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* Older versions required urlquery, newer expect json POST body */
        URLConnectionAdapter con = null;
        try {
            con = openAntiDDoSRequestConnection(br, br.createGetRequest(param.getCryptedUrl()));
            if (!looksLikeDownloadableContent(con)) {
                // initial get request (same as in browser) can set cookies/referer that might avoid 401
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
            } else {
                con.disconnect();
            }
            if (useOldPostRequest) {
                con = openAntiDDoSRequestConnection(br, br.createPostRequest(param.getCryptedUrl(), this.getPaginationPostDataQuery(0, "")));
            } else {
                con = openAntiDDoSRequestConnection(br, br.createPostRequest(param.getCryptedUrl(), this.getPaginationPostDataJson(0, "")));
                if (con.getResponseCode() == 500) {
                    try {
                        br.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    logger.info("Error 500 -> Trying again via old POST request method");
                    con = openAntiDDoSRequestConnection(br, br.createPostRequest(param.getCryptedUrl(), this.getPaginationPostDataQuery(0, "")));
                    useOldPostRequest = true;
                }
            }
            if (con.getResponseCode() == 401) {
                // POST request failed but simple GET may work fine
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                con = openAntiDDoSRequestConnection(br, br.createGetRequest(param.getCryptedUrl()));
            }
            if (looksLikeDownloadableContent(con)) {
                con.disconnect();
                final DownloadLink direct = this.createDownloadlink(con.getURL().toString());
                final DispositionHeader dispositionHeader = Plugin.parseDispositionHeader(con);
                if (dispositionHeader != null && StringUtils.isNotEmpty(dispositionHeader.getFilename())) {
                    direct.setFinalFileName(dispositionHeader.getFilename());
                    if (dispositionHeader.getEncoding() == null) {
                        try {
                            direct.setFinalFileName(URLEncode.decodeURIComponent(dispositionHeader.getFilename(), "UTF-8", true));
                        } catch (final IllegalArgumentException ignore) {
                        } catch (final UnsupportedEncodingException ignore) {
                        }
                    }
                }
                if (con.getCompleteContentLength() > 0) {
                    direct.setVerifiedFileSize(con.getCompleteContentLength());
                }
                direct.setLinkID(getLinkidFromURL(direct.getPluginPatternMatcher()));
                decryptedLinks.add(direct);
                return decryptedLinks;
            } else {
                br.followConnection();
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        if (br.getHttpConnection().getResponseCode() == 401) {
            if (acc != null) {
                /* We cannot check accounts so the only way we can find issues is by just trying with the login credentials here ... */
                logger.info("Existing account is invalid (?)");
                acc.setError(AccountError.INVALID, 5 * 60, null);
            } else {
                throw new AccountRequiredException();
            }
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        } else if (br.containsHTML("\"rateLimitExceeded\"")) {
            throw new DecrypterRetryException(RetryReason.HOST, "Rate Limit Exceeded");
        }
        return crawlFolder(param, useOldPostRequest);
    }

    private ArrayList<DownloadLink> crawlFolder(final CryptedLink param, final boolean useOldPOSTRequest) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        final boolean isParameterFile = !param.getCryptedUrl().endsWith("/");
        String subFolderPath = getAdoptedCloudFolderStructure();
        /*
         * If the user imports a link just by itself should it also be placed into the correct package. We can determine this via url
         * structure, else base folder with files wont be packaged together just based on filename....
         */
        if (subFolderPath == null) {
            final Regex typicalUrlStructure = new Regex(param.getCryptedUrl(), "https?://[^/]+/0:(/.*)");
            if (typicalUrlStructure.matches()) {
                /*
                 * Set correct (root) folder structure e.g. https://subdomain.example.site/0:/subfolder1/subfolder2 --> Path:
                 * /subfolder1/subfolder2 /subfolder1/subfolder2
                 */
                subFolderPath = Encoding.urlDecode(typicalUrlStructure.getMatch(0), false);
            } else {
                final String[] split = param.getCryptedUrl().split("/");
                subFolderPath = Encoding.urlDecode(split[split.length - (isParameterFile ? 2 : 1)], false);
            }
            fp.setName(subFolderPath.replaceAll("(^/)|(/$)", ""));
        } else {
            final String fpName = subFolderPath.substring(subFolderPath.lastIndexOf("/") + 1);
            fp.setName(fpName.replaceAll("(^/)|(/$)", ""));
        }
        final String baseUrl;
        /* urls can already be encoded which breaks stuff, only encode non-encoded content */
        if (!new Regex(param.getCryptedUrl(), "%[a-z0-9]{2}").matches()) {
            baseUrl = URLEncode.encodeURIComponent(param.getCryptedUrl());
        } else {
            baseUrl = param.getCryptedUrl();
        }
        int page = 0;
        do {
            logger.info("Crawling page: " + (page + 1));
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(decodeJSON(br.toString()));
            final String nextPageToken = (String) entries.get("nextPageToken");
            final List<Object> ressourcelist;
            Object filesArray = JavaScriptEngineFactory.walkJson(entries, "data/files");
            if (filesArray == null) {
                filesArray = JavaScriptEngineFactory.walkJson(entries, "files");
            }
            if (filesArray != null) {
                /* Multiple files */
                ressourcelist = (List<Object>) filesArray;
                if (ressourcelist.isEmpty()) {
                    if (ret.isEmpty()) {
                        throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, subFolderPath);
                    } else {
                        logger.info("Stopping because: Current page doesn't contain any items");
                        break;
                    }
                }
            } else {
                /* Probably single file */
                ressourcelist = new ArrayList<Object>();
                ressourcelist.add(entries);
            }
            for (final Object fileO : ressourcelist) {
                final Map<String, Object> entry = (Map<String, Object>) fileO;
                final String name = (String) entry.get("name");
                final String type = (String) entry.get("mimeType");
                final long filesize = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
                if (StringUtils.isEmpty(name) || StringUtils.isEmpty(type)) {
                    /* Skip invalid objects */
                    continue;
                }
                String url = baseUrl;
                if (type.endsWith(".folder")) {
                    // folder urls have to END in "/" this is how it works in browser no need for workarounds
                    url += URLEncode.encodeURIComponent(name) + "/";
                } else if (!isParameterFile) {
                    // do not this if base is a file!
                    url += URLEncode.encodeURIComponent(name);
                }
                final DownloadLink dl;
                if (type.endsWith(".folder")) {
                    dl = this.createDownloadlink(url);
                    final String thisfolder = subFolderPath + "/" + name;
                    dl.setRelativeDownloadFolderPath(thisfolder);
                    /* Save this so we need less requests for the next subfolder levels... */
                    if (useOldPOSTRequest) {
                        dl.setProperty(PROPERTY_FOLDER_USE_OLD_POST_REQUEST, true);
                    }
                } else {
                    dl = new DownloadLink(null, name, this.getHost(), url, true);
                    dl.setAvailable(true);
                    dl.setFinalFileName(name);
                    if (filesize > 0) {
                        dl.setVerifiedFileSize(filesize);
                    }
                    if (StringUtils.isNotEmpty(subFolderPath)) {
                        dl.setRelativeDownloadFolderPath(subFolderPath);
                    }
                    dl.setLinkID(getLinkidFromURL(dl.getPluginPatternMatcher()));
                }
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
            }
            if (this.isAbort()) {
                break;
            } else if (StringUtils.isEmpty(nextPageToken)) {
                logger.info("Stopping because: Reached end");
                break;
            } else {
                page += 1;
                /* Older versions required urlquery, newer expect json POST body */
                if (useOldPOSTRequest) {
                    sendRequest(br.createPostRequest(br.getURL(), this.getPaginationPostDataQuery(page, nextPageToken)));
                } else {
                    sendRequest(br.createPostRequest(br.getURL(), this.getPaginationPostDataJson(page, nextPageToken)));
                }
            }
        } while (true);
        return ret;
    }

    /** Returns String that can be used an unique ID based on given URL. */
    private String getLinkidFromURL(final String url) {
        return this.getHost() + new Regex(url, "(?i)https?://[^/]+/(.+)").getMatch(0);
    }

    private UrlQuery getPaginationPostDataQuery(final int index, final String pageToken) {
        final UrlQuery query = new UrlQuery();
        query.add("password", "");
        query.add("page_index", Integer.toString(index));
        query.appendEncoded("page_token", pageToken);
        return query;
    }

    private String getPaginationPostDataJson(final int index, final String pageToken) {
        final Map<String, Object> postData = new HashMap<String, Object>();
        postData.put("q", "");
        postData.put("password", null);
        postData.put("page_token", pageToken);
        postData.put("page_index", index);
        return JSonStorage.serializeToJson(postData);
    }
}
