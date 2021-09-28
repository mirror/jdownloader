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
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9\\-_]+)");
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

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://fromsmash.com"));
        br.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://fromsmash.com"));
        /* 2021-09-28: Without these headers they will respond with XML. */
        br.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT, "application/json, text/plain, */*"));
        br.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "application/json"));
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.getHeaders().put("Authorization", "Bearer " + getToken(this, this.br));
        prepBR(br);
        br.getPage("https://transfer.eu-central-1.fromsmash.co/transfer/" + folderID + "/preview?version=07-2020");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /*
             * E.g. {"code":404,"error":"Transfer <folderID> not found","requestId":"<someHash>","details":{"name":"Transfer","primary":
             * "<folderID>"}}
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> transfer = (Map<String, Object>) entries.get("transfer");
        String fpName = (String) transfer.get("title");
        if (StringUtils.isEmpty(fpName)) {
            fpName = folderID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        /* TODO: Add pagination if needed */
        br.getPage("/transfer/" + folderID + "/files/preview?version=07-2020&limit=9");
        entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Map<String, Object>> files = (List<Map<String, Object>>) entries.get("files");
        for (final Map<String, Object> file : files) {
            final DownloadLink link = this.createDownloadlink("https://" + this.getHost() + "/" + folderID + "#fileid=" + file.get("id").toString());
            link.setFinalFileName(file.get("name").toString());
            link.setVerifiedFileSize(((Number) file.get("size")).longValue());
            link.setAvailable(true);
            link.setProperty(FromsmashCom.PROPERTY_DIRECTURL, file.get("download").toString());
            link._setFilePackage(fp);
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }
}
