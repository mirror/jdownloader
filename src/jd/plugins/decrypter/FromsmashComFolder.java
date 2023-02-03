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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.FromsmashCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FromsmashComFolder extends PluginForDecrypt {
    public FromsmashComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fromsmash.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([^/]+)");
        }
        return ret.toArray(new String[0]);
    }

    protected static AtomicReference<String> TOKEN                       = new AtomicReference<String>();
    protected static AtomicLong              TOKEN_TIMESTAMP_VALID_UNTIL = new AtomicLong(-1);
    protected final static long              TOKEN_EXPIRE                = 60 * 60 * 1000l;

    public static String getToken(final Plugin plugin, final Browser br) throws IOException, PluginException {
        synchronized (TOKEN) {
            String token = TOKEN.get();
            if (!StringUtils.isEmpty(token) && Time.systemIndependentCurrentJVMTimeMillis() < TOKEN_TIMESTAMP_VALID_UNTIL.get()) {
                return token;
            } else {
                final Browser brc = br.cloneBrowser();
                prepBR(brc);
                brc.postPageRaw("https://iam.eu-central-1.fromsmash.co/account", "{}");
                final Map<String, Object> response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                final Map<String, Object> account = (Map<String, Object>) response.get("account");
                final Map<String, Object> tokenMap = (Map<String, Object>) account.get("token");
                token = (String) tokenMap.get("token");
                /* 2021-09-28: Their tokens are valid for 24H. */
                final String tokenExpireDate = (String) tokenMap.get("expiration");
                final long tokenTimestamp = TimeFormatter.getMilliSeconds(tokenExpireDate, "yyyy-MM-dd'T'HH:mm:ss'.000Z'", Locale.ENGLISH);
                if (StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    TOKEN.set(token);
                    TOKEN_TIMESTAMP_VALID_UNTIL.set(tokenTimestamp);
                    return token;
                }
            }
        }
    }

    private String[] getDetails(final CryptedLink param, final Browser br) throws Exception {
        final String folderName = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Accept", "application/json, text/plain, */*");
        brc.getPage("https://link.fromsmash.co/target/fromsmash.com%2F" + URLEncode.encodeURIComponent(folderName) + "?version=10-2019");
        Map<String, Object> json = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
        final String target = (String) JavaScriptEngineFactory.walkJson(json, "target/target");
        final String url = (String) JavaScriptEngineFactory.walkJson(json, "target/url");
        if (target == null && url == null) {
            if (brc.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (target != null) {
            return new String[] { target, url };
        } else {
            return new String[] { folderName, url };
        }
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://fromsmash.com"));
        br.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://fromsmash.com"));
        /* 2021-09-28: Without these headers they will respond with XML. */
        br.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT, "application/json, text/plain, */*"));
        br.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "application/json"));
        return br;
    }

    /** Puts download password to header. */
    public static void setPasswordHeader(final Browser br, final String passCode) {
        br.getHeaders().put("Smash-Authorization", Encoding.Base64Encode(passCode));
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getHeaders().put("Authorization", "Bearer " + getToken(this, this.br));
        final String details[] = getDetails(param, br);
        final String folderID = details[0];
        final String region = new URL(details[1]).getHost();
        prepBR(br);
        String passCode = null;
        int passwordAttempt = 0;
        do {
            passwordAttempt += 1;
            br.getPage("https://" + region + "/transfer/" + folderID + "/preview?version=07-2020");
            if (br.getHttpConnection().getResponseCode() == 404) {
                /*
                 * E.g. {"code":404,"error":"Transfer <folderID> not found","requestId":"<someHash>","details":{"name":"Transfer","primary":
                 * "<folderID>"}}
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 403) {
                /* {"code":403,"error":"Password does not match for <folderID>","requestId":"<someHash>"} */
                logger.info("Password attempt " + passwordAttempt + " failed");
                if (passwordAttempt > 3) {
                    /* Too many wrong password attempts */
                    throw new DecrypterException(DecrypterException.PASSWORD);
                } else {
                    passCode = getUserInput("Password?", param);
                    setPasswordHeader(br, passCode);
                    continue;
                }
            } else {
                /* Correct password was entered or no password needed */
                break;
            }
        } while (true);
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> transfer = (Map<String, Object>) entries.get("transfer");
        final String status = transfer.get("status").toString();
        if (!status.equalsIgnoreCase("Uploaded")) {
            /* E.g. "Expired" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = (String) transfer.get("title");
        if (StringUtils.isEmpty(fpName)) {
            fpName = folderID;
        }
        final int numberofItems = ((Number) transfer.get("filesNumber")).intValue();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        /* 2021-09-29: Website is using max. 9 items --> Probably because of their 3-pair grid layout. */
        final int maxItemsPerPage = 9;
        final UrlQuery query = new UrlQuery();
        query.add("version", "07-2020");
        query.add("limit", Integer.toString(maxItemsPerPage));
        int page = 0;
        String next = null;
        do {
            page += 1;
            br.getPage("/transfer/" + folderID + "/files/preview?" + query.toString());
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final List<Map<String, Object>> files = (List<Map<String, Object>>) entries.get("files");
            for (final Map<String, Object> file : files) {
                final String fileid = file.get("id").toString();
                final DownloadLink link = this.createDownloadlink("https://" + this.getHost() + "/" + folderID + "#fileid=" + fileid);
                link.setFinalFileName(file.get("name").toString());
                link.setVerifiedFileSize(((Number) file.get("size")).longValue());
                link.setAvailable(true);
                link.setProperty(FromsmashCom.PROPERTY_DIRECTURL, file.get("download").toString());
                link.setProperty("fileid", fileid);
                link.setProperty("folderid", folderID);
                link.setProperty("region", region);
                if (passCode != null) {
                    link.setPasswordProtected(true);
                    link.setDownloadPassword(passCode);
                    /*
                     * User can modify the other property but we know that the given download password will never change so let's make sure
                     * we can re-use exactly the password which we know is correct!
                     */
                    link.setProperty(FromsmashCom.PROPERTY_STATIC_DOWNLOAD_PASSWORD, passCode);
                }
                link._setFilePackage(fp);
                distribute(link);
                ret.add(link);
            }
            logger.info("Progress: Page: " + page + " | Found items " + ret.size() + " / " + numberofItems);
            next = (String) entries.get("next");
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                return ret;
            } else if (StringUtils.isEmpty(next)) {
                logger.info("Stopping because: Reached end (no 'next' token given)");
                break;
            } else if (files.size() < maxItemsPerPage) {
                logger.info("Stopping because: Page contains less items than " + maxItemsPerPage);
                break;
            } else {
                /* Continue to next page */
                query.addAndReplace("start", Encoding.urlEncode(next));
                continue;
            }
        } while (true);
        return ret;
    }
}
