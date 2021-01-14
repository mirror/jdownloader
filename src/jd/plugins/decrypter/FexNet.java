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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FexNet extends PluginForDecrypt {
    public FexNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String API_BASE = "https://api.fex.net/api";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fex.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z]{2}/)?s/([a-z0-9]{3,})(#(\\d+))?");
        }
        return ret.toArray(new String[0]);
    }

    private static String cachedToken          = null;
    private static long   cachedTokenTimestamp = 0;
    private static Object LOCK                 = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String subfolderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(2);
        Map<String, Object> entries = null;
        synchronized (LOCK) {
            if (cachedToken == null || System.currentTimeMillis() - cachedTokenTimestamp > 5 * 60 * 1000l) {
                cachedToken = getFreshAuthToken(this.br);
                cachedTokenTimestamp = System.currentTimeMillis();
            }
        }
        br.setAllowedResponseCodes(new int[] { 400, 401 });
        br.getHeaders().put("Authorization", "Bearer " + cachedToken);
        br.getHeaders().put("Content-Type", "application/json");
        // /* Access root folder first time -> Get name of it, then continue below */
        // br.getPage(API_BASE + "/v2/file/share/" + folderID);
        // if (br.getHttpConnection().getResponseCode() == 404) {
        // decryptedLinks.add(this.createOfflinelink(parameter));
        // return decryptedLinks;
        // }
        // entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        // /* Set root folder name */
        // subfolderPath = (String) JavaScriptEngineFactory.walkJson(entries, "shared_link/title");
        String subfolderPath = this.getAdoptedCloudFolderStructure();
        if (subfolderPath == null) {
            subfolderPath = "";
        }
        FilePackage fp = null;
        if (!StringUtils.isEmpty(subfolderPath)) {
            fp = FilePackage.getInstance();
            fp.setName(subfolderPath);
        }
        /* Theirs start with 1 too */
        int page = 1;
        String passCode = null;
        do {
            logger.info("Crawling page: " + page);
            final UrlQuery query = new UrlQuery();
            query.add("page", page + "");
            query.add("sort_by", "name");
            /* 2021-01-14 */
            query.add("per_page", "500");
            query.add("is_desc", "1");
            String url = API_BASE + "/v2/file/share/children/" + folderID;
            if (subfolderID != null) {
                url += "/" + subfolderID;
            }
            br.getPage(url + "?" + query.toString());
            /* 2021-01-14: E.g. {"code":2414,"status":400} */
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            } else if (br.getHttpConnection().getResponseCode() == 401) {
                /* TODO: Fix this */
                /* 2021-01-14: E.g. {"code":2426,"status":401} */
                logger.info("Folder is password protected");
                boolean success = false;
                int counter = 0;
                do {
                    /* Ask user for password if none is given. */
                    if (passCode == null) {
                        passCode = getUserInput("Password?", param);
                    }
                    br.postPageRaw(API_BASE + "/v2/file/share/" + folderID + "/auth", "{\"password\":\"" + passCode + "\"}");
                    if (br.getHttpConnection().getResponseCode() == 400) {
                        /* 2021-01-14: {"code":1056,"form":{"password":[1054]},"status":400} */
                        passCode = null;
                    } else {
                        /* 2021-01-14: {"refresh_token":"b64String", "token": "b64String"} */
                        entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                        final String tokenNew = (String) entries.get("token");
                        if (!StringUtils.isEmpty(tokenNew)) {
                            cachedToken = tokenNew;
                            cachedTokenTimestamp = System.currentTimeMillis();
                            br.getHeaders().put("Authorization", "Bearer " + cachedToken);
                            /* Gets auto-set */
                            // br.setCookie(br.getHost(), "token", cachedToken);
                        } else {
                            /* This will probably lead to a failure! */
                            logger.warning("Failed to get new token after password");
                        }
                        success = true;
                        break;
                    }
                    counter++;
                } while (!this.isAbort() && counter <= 2);
                if (!success) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                br.getPage(url + "?" + query.toString());
            }
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> pagination = (Map<String, Object>) entries.get("pagination");
            if (entries.containsKey("children")) {
                final List<Object> ressourcelist = (List<Object>) entries.get("children");
                for (final Object fileO : ressourcelist) {
                    entries = (Map<String, Object>) fileO;
                    final long id = ((Number) entries.get("id")).longValue();
                    final String name = (String) entries.get("name");
                    if (StringUtils.isEmpty(name)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final boolean is_dir = ((Boolean) entries.get("is_dir")).booleanValue();
                    if (is_dir) {
                        /* Subfolder -> Goes back into crawler */
                        final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/s/" + folderID + "#" + id);
                        dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderPath + "/" + name);
                        decryptedLinks.add(dl);
                    } else {
                        final String download_url = (String) entries.get("download_url");
                        if (StringUtils.isEmpty(download_url)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final long filesize = ((Number) entries.get("size")).longValue();
                        final DownloadLink link = this.createDownloadlink("https://" + this.getHost() + "/folder/" + folderID + "/file/" + id);
                        /*
                         * Set meaningful URL for the user to copy - this filehost does not have URLs pointing to single files within a
                         * folder structure!
                         */
                        link.setContentUrl("https://" + this.getHost() + "/s/" + folderID + "#" + subfolderPath);
                        link.setFinalFileName(name);
                        if (filesize > 0) {
                            link.setDownloadSize(filesize);
                        }
                        link.setAvailable(true);
                        link.setProperty(jd.plugins.hoster.FexNet.PROPERTY_directurl, download_url);
                        if (fp != null) {
                            link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderPath);
                            link._setFilePackage(fp);
                        }
                        link.setProperty(jd.plugins.hoster.FexNet.PROPERTY_token, cachedToken);
                        if (passCode != null) {
                            link.setDownloadPassword(passCode);
                        }
                        decryptedLinks.add(link);
                    }
                }
            }
            final int pageMax = ((Number) pagination.get("pages")).intValue();
            if (page >= pageMax) {
                logger.info("Stopping because reached last page: " + pageMax);
                break;
            }
            page++;
        } while (!this.isAbort());
        if (decryptedLinks.isEmpty()) {
            decryptedLinks.add(this.createOfflinelink(parameter, "Empty folder: " + folderID, "Empty folder: " + folderID));
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    public static final String getFreshAuthToken(final Browser br) throws PluginException, IOException {
        br.getPage(API_BASE + "/v1/config/anonymous");
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String token = (String) JavaScriptEngineFactory.walkJson(entries, "anonymous/anonym_token");
        if (StringUtils.isEmpty(token)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return token;
    }
}
