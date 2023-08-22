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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
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

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String subfolderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(2);
        Map<String, Object> entries = null;
        String cachedToken = jd.plugins.hoster.FexNet.getAuthToken(this, br);
        br.setAllowedResponseCodes(new int[] { 400, 401 });
        br.getHeaders().put("Authorization", "Bearer " + cachedToken);
        br.getHeaders().put("Content-Type", "application/json");
        br.setCookie(this.getHost(), "G_ENABLED_IDPS", "google");
        // /* Access root folder first time -> Get name of it, then continue below */
        // br.getPage(API_BASE + "/v2/file/share/" + folderID);
        // if (br.getHttpConnection().getResponseCode() == 404) {
        // decryptedLinks.add(this.createOfflinelink(parameter));
        // return decryptedLinks;
        // }
        // entries = restoreFromString(br.toString(), TypeRef.MAP);
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
        /* Subfolder of previously crawler password protected subfolder? Then we already know the password! */
        if (param.getDownloadLink() != null) {
            passCode = param.getDecrypterPassword();
        }
        do {
            logger.info("Crawling page: " + page);
            final UrlQuery query = new UrlQuery();
            query.add("page", page + "");
            query.add("sort_by", "name");
            /* 2021-01-14 */
            query.add("per_page", "500");
            query.add("is_desc", "1");
            String urlpart_share;
            if (passCode != null) {
                urlpart_share = "share/s";
            } else {
                urlpart_share = "share";
            }
            String url = jd.plugins.hoster.FexNet.API_BASE + "/v2/file/" + urlpart_share + "/children/" + folderID;
            if (subfolderID != null) {
                url += "/" + subfolderID;
            }
            br.getPage(url + "?" + query.toString());
            /* 2021-01-14: E.g. {"code":2414,"status":400} */
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 401) {
                /* 2021-01-14: E.g. {"code":2426,"status":401} */
                logger.info("Folder is password protected");
                boolean success = false;
                int counter = 0;
                do {
                    /* Ask user for password if none is given. */
                    if (passCode == null) {
                        passCode = getUserInput("Password?", param);
                    }
                    br.postPageRaw(jd.plugins.hoster.FexNet.API_BASE + "/v2/file/share/" + folderID + "/auth", "{\"password\":\"" + passCode + "\"}");
                    if (br.getHttpConnection().getResponseCode() == 400) {
                        /* 2021-01-14: {"code":1056,"form":{"password":[1054]},"status":400} */
                        passCode = null;
                        counter++;
                        continue;
                    } else {
                        /* 2021-01-14: {"refresh_token":"b64String", "token": "b64String"} */
                        entries = restoreFromString(br.toString(), TypeRef.MAP);
                        final String tokenNew = (String) entries.get("token");
                        if (!StringUtils.isEmpty(tokenNew)) {
                            jd.plugins.hoster.FexNet.setAuthToken(tokenNew);
                            cachedToken = tokenNew;
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
                } while (!this.isAbort() && counter <= 2);
                if (!success) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                urlpart_share = "share/s";
                url = jd.plugins.hoster.FexNet.API_BASE + "/v2/file/" + urlpart_share + "/children/" + folderID;
                if (subfolderID != null) {
                    url += "/" + subfolderID;
                }
                br.getPage(url + "?" + query.toString());
            }
            entries = restoreFromString(br.toString(), TypeRef.MAP);
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
                        final DownloadLink link = this.createDownloadlink("https://" + this.getHost() + "/s/" + folderID + "#" + id);
                        link.setRelativeDownloadFolderPath(subfolderPath + "/" + name);
                        if (passCode != null) {
                            link.setDownloadPassword(passCode);
                        }
                        ret.add(link);
                        distribute(link);
                    } else {
                        /* Single file */
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
                            link.setRelativeDownloadFolderPath(subfolderPath);
                            link._setFilePackage(fp);
                        }
                        link.setProperty(jd.plugins.hoster.FexNet.PROPERTY_token, cachedToken);
                        if (passCode != null) {
                            link.setDownloadPassword(passCode);
                        }
                        ret.add(link);
                        distribute(link);
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
        if (ret.isEmpty()) {
            if (!StringUtils.isEmpty(subfolderPath)) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderID + "_" + subfolderPath);
            } else {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderID);
            }
        }
        return ret;
    }
}
