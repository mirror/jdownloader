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

import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.CivitaiCom;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { CivitaiCom.class })
public class CivitaiComCrawler extends PluginForDecrypt {
    public CivitaiComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        return CivitaiCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(models/.+|posts/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        final Regex urlregex = new Regex(param.getCryptedUrl(), "(?i)/(posts|models)/(\\d+)");
        final UrlQuery query = UrlQuery.parse(contenturl);
        final String modelVersionId = query.get("modelVersionId");
        final String itemType = urlregex.getMatch(0);
        final String itemID = urlregex.getMatch(1);
        /* Using API: https://github.com/civitai/civitai/wiki/REST-API-Reference */
        final String apiBase = "https://civitai.com/api/v1";
        final List<Map<String, Object>> modelVersions = new ArrayList<Map<String, Object>>();
        if (modelVersionId != null) {
            /* https://github.com/civitai/civitai/wiki/REST-API-Reference#get-apiv1models-versionsmodelversionid */
            br.getPage(apiBase + "/model-versions/" + modelVersionId);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            modelVersions.add(entries);
        } else if (itemType.equals("models")) {
            /* Crawl all versions of a model */
            /* https://github.com/civitai/civitai/wiki/REST-API-Reference#get-apiv1modelsmodelid */
            br.getPage(apiBase + "/models/" + itemID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> _modelVersions_ = (List<Map<String, Object>>) entries.get("modelVersions");
            modelVersions.addAll(_modelVersions_);
        } else if (itemType.equals("posts")) {
            /* Handles such links: https://civitai.com/posts/1234567 */
            /* https://github.com/civitai/civitai/wiki/REST-API-Reference#get-apiv1images */
            br.getPage(apiBase + "/images?postId=" + itemID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> images = (List<Map<String, Object>>) entries.get("items");
            if (images == null || images.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(itemID);
            fp.setPackageKey("civitai://post/" + itemID);
            for (final Map<String, Object> image : images) {
                final String imageurl = image.get("url").toString();
                final String tempName = Plugin.getFileNameFromURL(imageurl);
                final DownloadLink link = this.createDownloadlink(br.getURL("/images/" + image.get("id")).toExternalForm());
                link._setFilePackage(fp);
                if (tempName != null) {
                    link.setName(tempName);
                }
                link.setAvailable(true);
                ret.add(link);
            }
        } else {
            /* Unsupported link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Process collected ModelVersions */
        for (final Map<String, Object> entries : modelVersions) {
            final ArrayList<DownloadLink> thisRet = new ArrayList<DownloadLink>();
            final String modelName = entries.get("name").toString();
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(modelName);
            fp.setPackageKey("civitai://model/modelVersion/" + modelVersionId);
            final List<Map<String, Object>> files = (List<Map<String, Object>>) entries.get("files");
            for (final Map<String, Object> file : files) {
                final Map<String, Object> hashes = (Map<String, Object>) file.get("hashes");
                final DownloadLink link = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(file.get("downloadUrl").toString()));
                link.setFinalFileName(file.get("name").toString());
                link.setDownloadSize(((Number) file.get("sizeKB")).longValue() * 1024);
                link.setAvailable(true);
                final String sha256 = (String) hashes.get("SHA256");
                // final String crc32 = (String) hashes.get("CRC32");
                if (sha256 != null) {
                    link.setSha256Hash(sha256);
                }
                // if (crc32 != null) {
                // link.setHashInfo(HashInfo.newInstanceSafe(crc32, HashInfo.TYPE.CRC32C));
                // }
                thisRet.add(link);
            }
            final List<Map<String, Object>> images = (List<Map<String, Object>>) entries.get("images");
            for (final Map<String, Object> image : images) {
                final DownloadLink link = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(image.get("url").toString()));
                link.setAvailable(true);
                thisRet.add(link);
            }
            for (final DownloadLink result : thisRet) {
                result._setFilePackage(fp);
                ret.add(result);
            }
        }
        // if(ret.isEmpty()) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, Account acc) {
        return false;
    }
}
