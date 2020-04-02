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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bt.com" }, urls = { "https?://(?:www\\.)?btcloud\\.bt\\.com/web/app/share/invite/([A-Za-z0-9]+)|https://cloud\\.bt\\.comdecrypted/\\?.+" })
public class BtCom extends PluginForDecrypt {
    public BtCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String apikey;
        String main_uri;
        String cookie_NWBCS;
        br.setFollowRedirects(true);
        if (parameter.matches(".+web/app/share/invite/.+")) {
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("errorCode")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            apikey = PluginJSonUtils.getJson(br, "nab.api.cs.key");
            main_uri = PluginJSonUtils.getJson(PluginJSonUtils.unescape(br.toString()), "location");
            cookie_NWBCS = br.getCookie(br.getURL(), "NWBCS", Cookies.NOTDELETEDPATTERN);
            if (StringUtils.isEmpty(apikey) || StringUtils.isEmpty(main_uri) || StringUtils.isEmpty(cookie_NWBCS)) {
                return null;
            }
        } else {
            final UrlQuery query = new UrlQuery().parse(parameter);
            apikey = query.get("apikey");
            main_uri = query.get("uri");
            cookie_NWBCS = query.get("cookie_NWBCS");
        }
        br.getHeaders().put("Authorization", String.format("NWB token=\"%s\"; authVersion=\"1.0\"", cookie_NWBCS));
        br.getHeaders().put("Accept", "application/vnd.newbay.dv-1.10+json");
        br.getHeaders().put("Authorization-Domain", "sncr.shared.token");
        br.getHeaders().put("Sec-Fetch-Dest", "empty");
        br.getHeaders().put("Sec-Fetch-Mode", "cors");
        br.getHeaders().put("Sec-Fetch-Site", "same-origin");
        br.getHeaders().put("X-Client-Identifier", "WhiteLabelWebApp");
        br.getHeaders().put("X-Client-Platform", "WEB");
        br.getHeaders().put("X-F1-Client-Authorization", "Basic " + apikey);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-Response-Encoding", "html");
        // br.setAllowedResponseCodes(new int[] { 406 });
        /* Extra/First auth request seems not to be required */
        // br.postPage("https://btcloud.bt.com/share/api/auth/refresh", "shareToken=" + NWBCS);
        // final String shareToken = PluginJSonUtils.getJson(br, "shareToken");
        // if (StringUtils.isEmpty(shareToken)) {
        // return null;
        // }
        // br.getHeaders().put("Authorization", String.format("NWB token=\"%s\"; authVersion=\"1.0\"", NWBCS));
        br.getPage("https://btcloud.bt.com/dv/api/folder/list?uri=" + Encoding.urlEncode(main_uri) + "&start=1&count=60&_=" + System.currentTimeMillis());
        /* 2nd offline check */
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        Map<String, Object> folderInfo;
        entries = (Map<String, Object>) entries.get("nodeCollection");
        final Object foldersO = entries.get("folder");
        if (foldersO != null) {
            final ArrayList<Object> folders = (ArrayList<Object>) foldersO;
            for (final Object folderO : folders) {
                folderInfo = (Map<String, Object>) folderO;
                final String uri = (String) JavaScriptEngineFactory.walkJson(folderInfo, "uri/$");
                // final String link = (String) JavaScriptEngineFactory.walkJson(folderInfo, "link/$");
                if (StringUtils.isEmpty(uri)) {
                    /* Skip invalid items */
                    continue;
                }
                final UrlQuery query = new UrlQuery();
                query.append("apikey", apikey, false);
                query.append("uri", uri, false);
                query.append("cookie_NWBCS", cookie_NWBCS, false);
                /* Goes back into decrypter */
                decryptedLinks.add(this.createDownloadlink("https://cloud.bt.comdecrypted/?" + query.toString()));
            }
        }
        final ArrayList<Object> files;
        final Object filesO = entries.get("file");
        if (filesO instanceof ArrayList) {
            files = (ArrayList<Object>) entries.get("file");
        } else {
            /* Single file in folder */
            files = new ArrayList<Object>();
            files.add(filesO);
        }
        for (final Object fileO : files) {
            entries = (Map<String, Object>) fileO;
            final String uri = (String) JavaScriptEngineFactory.walkJson(entries, "uri/$");
            final String contentToken = (String) JavaScriptEngineFactory.walkJson(entries, "contentToken/$");
            final String filename = (String) JavaScriptEngineFactory.walkJson(entries, "name/$");
            final String parentPath = (String) JavaScriptEngineFactory.walkJson(entries, "parentPath/$");
            final long filesize = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "size/$"), 0);
            if (StringUtils.isEmpty(uri) || StringUtils.isEmpty(filename)) {
                /* Skip invalid items */
                continue;
            }
            String uri_encoded = URLEncode.encodeURIComponent(uri);
            // uri_encoded = uri_encoded.replace("dv-file-share://", "dv-file-share%3A%2F%2F");
            final String dllink = String.format("https://common.btpc.prod.cloud.synchronoss.net/dv/api/file/sharedContent?uri=%s&X-Client-Platform=WEB&X-Client-Identifier=WhiteLabelWebApp&NWB=A%s&cachebuster=%d&browser=true", uri_encoded, contentToken, System.currentTimeMillis());
            final DownloadLink dl = this.createDownloadlink("directhttp://" + dllink);
            dl.setFinalFileName(filename);
            if (filesize > 0) {
                dl.setDownloadSize(filesize);
            }
            dl.setAvailable(true);
            if (!StringUtils.isEmpty(parentPath)) {
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, parentPath);
            }
            dl.setLinkID(this.getHost() + "://" + uri);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
