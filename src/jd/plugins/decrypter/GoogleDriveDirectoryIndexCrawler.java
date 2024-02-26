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

import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.DispositionHeader;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionObject;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
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
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.GoogleDriveDirectoryIndex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { jd.plugins.hoster.GoogleDriveDirectoryIndex.class })
public class GoogleDriveDirectoryIndexCrawler extends PluginForDecrypt {
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
            /* Special regex is used here to allow URLs containing username and/or password information. */
            ret.add("https?://[\\w\\d:#@%/;$()~_?\\+-=\\.&\\\\]*" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Crawler plugin that can handle instances of this project:
     * https://gitlab.com/ParveenBhadooOfficial/Google-Drive-Index/-/blob/master/README.md or:</br>
     * https://github.com/alx-xlx/goindex </br>
     * Be sure to add all domains to host plugin GoogleDriveDirectoryIndex.java too!
     */
    public GoogleDriveDirectoryIndexCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        return br;
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

    private DownloadLink getDirectDownload(final URLConnectionAdapter con) throws IOException {
        if (con != null && looksLikeDownloadableContent(con)) {
            con.disconnect();
            final DownloadLink direct = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(con.getURL().toExternalForm()));
            final DispositionHeader dispositionHeader = Plugin.parseDispositionHeader(con);
            String dispositionHeaderFilename = null;
            if (dispositionHeader != null && StringUtils.isNotEmpty(dispositionHeaderFilename = dispositionHeader.getFilename())) {
                direct.setFinalFileName(dispositionHeaderFilename);
                if (dispositionHeader.getEncoding() == null) {
                    try {
                        direct.setFinalFileName(URLEncode.decodeURIComponent(dispositionHeaderFilename, "UTF-8", true));
                    } catch (final IllegalArgumentException ignore) {
                    } catch (final UnsupportedEncodingException ignore) {
                    }
                }
            }
            if (con.getCompleteContentLength() > 0) {
                direct.setVerifiedFileSize(con.getCompleteContentLength());
            }
            direct.setAvailable(true);
            return direct;
        } else {
            return null;
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl();
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final GoogleDriveDirectoryIndex plg = (GoogleDriveDirectoryIndex) this.getNewPluginForHostInstance(this.getHost());
            plg.login(account);
        }
        boolean useOldPostRequest;
        /* Check if we maybe already know which request type is the right one so we need less http requests. */
        if (param.getDownloadLink() != null && param.getDownloadLink().hasProperty(PROPERTY_FOLDER_USE_OLD_POST_REQUEST)) {
            useOldPostRequest = true;
        } else {
            useOldPostRequest = false;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* Older versions required urlquery, newer expect json POST body */
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(contenturl);
            /* Check if we got a single direct downloadable file */
            final DownloadLink direct = getDirectDownload(con);
            if (direct != null) {
                logger.info("Result is a directurl");
                final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                ret.add(direct);
                return ret;
            } else {
                // initial get request (same as in browser) can set cookies/referer that might avoid 401
                br.followConnection(true);
            }
            if (useOldPostRequest) {
                con = br.openPostConnection(contenturl, this.getPaginationPostDataQuery(0, ""));
            } else {
                con = br.openRequestConnection(br.createPostRequest(contenturl, this.getPaginationPostDataJson(0, "")));
                if (con.getResponseCode() == 500) {
                    br.followConnection(true);
                    logger.info("Error 500 -> Trying again via old POST request method");
                    con = br.openPostConnection(contenturl, this.getPaginationPostDataQuery(0, ""));
                    /* Make sure that subsequent requests will be sent in the correct form right away. */
                    useOldPostRequest = true;
                }
            }
            br.followConnection(true);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 401) {
            exceptionAccountRequiredOrInvalid(account);
        } else if (br.containsHTML("\"rateLimitExceeded\"")) {
            // TODO: 2023-10-30: Check if this can still happen and implement it into parsed json handling
            throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT);
        }
        /* Looks like folder should be available -> Crawl it */
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        final boolean isParameterFile = !contenturl.endsWith("/");
        String subFolderPath = getAdoptedCloudFolderStructure();
        /*
         * If the user imports a link just by itself should it also be placed into the correct package. We can determine this via url
         * structure, else base folder with files wont be packaged together just based on filename....
         */
        if (subFolderPath == null) {
            final Regex typicalUrlStructure = new Regex(contenturl, "(?i)https?://[^/]+/0:(/.*)");
            if (typicalUrlStructure.patternFind()) {
                /*
                 * Set correct (root) folder structure e.g. https://subdomain.example.site/0:/subfolder1/subfolder2 --> Path:
                 * /subfolder1/subfolder2 /subfolder1/subfolder2
                 */
                subFolderPath = Encoding.urlDecode(typicalUrlStructure.getMatch(0), false);
            } else {
                /* Use path of URL as file-path */
                final String[] split = contenturl.split("/");
                subFolderPath = Encoding.urlDecode(split[split.length - (isParameterFile ? 2 : 1)], false);
            }
            fp.setName(subFolderPath.replaceAll("(^/)|(/$)", ""));
        } else {
            final String fpName = subFolderPath.substring(subFolderPath.lastIndexOf("/") + 1);
            fp.setName(fpName.replaceAll("(^/)|(/$)", ""));
        }
        final String baseUrl;
        /* urls can already be encoded which breaks stuff, only encode non-encoded content */
        if (!new Regex(contenturl, "%[a-z0-9]{2}").patternFind()) {
            baseUrl = URLEncode.encodeURIComponent(contenturl);
        } else {
            baseUrl = contenturl;
        }
        GoogleDriveDirectoryIndex hosterplugin = null;
        int page = 0;
        do {
            Map<String, Object> entries = null;
            try {
                entries = JavaScriptEngineFactory.jsonToJavaMap(decodeJSON(br.getRequest().getHtmlCode()));
            } catch (final Throwable e) {
                /* Json parsing failed -> Assume that account is required or given account is invalid */
                exceptionAccountRequiredOrInvalid(account);
            }
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
                if (StringUtils.isEmpty(name) || StringUtils.isEmpty(type)) {
                    /* Skip invalid objects */
                    continue;
                }
                String url = baseUrl;
                if (StringUtils.endsWithCaseInsensitive(type, ".folder")) {
                    // folder urls have to END in "/" this is how it works in browser no need for workarounds
                    url += URLEncode.encodeURIComponent(name) + "/";
                } else if (!isParameterFile) {
                    // do not this if base is a file!
                    url += URLEncode.encodeURIComponent(name);
                }
                final DownloadLink dl;
                if (type.endsWith(".folder")) {
                    /* Folder */
                    dl = this.createDownloadlink(url);
                    final String thisfolder = subFolderPath + "/" + name;
                    dl.setRelativeDownloadFolderPath(thisfolder);
                    /* Save this so we need less requests for the next subfolder levels... */
                    if (useOldPostRequest) {
                        dl.setProperty(PROPERTY_FOLDER_USE_OLD_POST_REQUEST, true);
                    }
                } else {
                    /* File */
                    if (hosterplugin == null) {
                        hosterplugin = (GoogleDriveDirectoryIndex) this.getNewPluginForHostInstance(this.getHost());
                    }
                    final long filesize = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
                    dl = new DownloadLink(hosterplugin, name, this.getHost(), url, true);
                    dl.setAvailable(true);
                    dl.setFinalFileName(name);
                    if (filesize > 0) {
                        dl.setVerifiedFileSize(filesize);
                    }
                    if (!StringUtils.isEmpty(subFolderPath)) {
                        dl.setRelativeDownloadFolderPath(subFolderPath);
                    }
                    dl._setFilePackage(fp);
                }
                distribute(dl);
                ret.add(dl);
            }
            logger.info("Crawled page " + (page + 1) + " | Found items so far: " + ret.size() + " | nextPageToken = " + nextPageToken + " | useOldPostRequest = " + useOldPostRequest);
            if (this.isAbort()) {
                break;
            } else if (StringUtils.isEmpty(nextPageToken)) {
                logger.info("Stopping because: Reached end");
                break;
            } else {
                page += 1;
                /* Older versions required urlquery, newer expect json POST body */
                if (useOldPostRequest) {
                    br.postPage(br.getURL(), this.getPaginationPostDataQuery(page, nextPageToken));
                } else {
                    br.postPageRaw(br.getURL(), this.getPaginationPostDataJson(page, nextPageToken));
                }
            }
        } while (true);
        return ret;
    }

    private void exceptionAccountRequiredOrInvalid(final Account account) throws AccountRequiredException {
        if (account != null) {
            /* We cannot check accounts so the only way we can find issues is by just trying with the login credentials here ... */
            logger.info("Existing account is invalid (?)");
            account.setError(AccountError.INVALID, 5 * 60, null);
        } else {
            throw new AccountRequiredException();
        }
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
