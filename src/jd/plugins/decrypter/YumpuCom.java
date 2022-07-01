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

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "yumpu.com" }, urls = { "https?://(?:www\\.)?yumpu\\.com/[a-z]{2}/document/read/(\\d+)/([a-z0-9\\-]+)" })
public class YumpuCom extends PluginForDecrypt {
    public YumpuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String jsonURL = br.getRegex("var jsonUrl = \"([^\"]+)\"").getMatch(0);
        final String fid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String url_name = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1);
        final String[] possibleQualityIdentifiersSorted = new String[] { "big", "large", "medium", "small" };
        if (jsonURL != null) {
            /* New way */
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // br.getPage("https://www." + this.getHost() + "/en/document/json2/" + fid);
            br.getPage(jsonURL);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Object errorO = entries.get("error");
            if (errorO != null) {
                /* E.g. {"error":{"reason":"deleted","message":"Document deleted"}} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> document = (Map<String, Object>) entries.get("document");
            final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) document.get("pages");
            final String description = (String) document.get("description");
            String fpName = (String) document.get("title");
            if (StringUtils.isEmpty(fpName)) {
                /* Fallback */
                fpName = url_name;
            }
            final String base_path = (String) document.get("base_path");
            final Map<String, Object> images = (Map<String, Object>) document.get("images");
            final String basetitle = (String) images.get("title");
            if (StringUtils.isEmpty(base_path) || StringUtils.isEmpty(basetitle) || ressourcelist == null || ressourcelist.size() == 0) {
                return null;
            }
            final boolean setComment = !StringUtils.isEmpty(description) && !description.equalsIgnoreCase(fpName);
            for (int i = 0; i <= ressourcelist.size() - 1; i++) {
                /* 2019-05-21: 'quality' = percentage of quality */
                final Map<String, Object> page = ressourcelist.get(i);
                final Map<String, Object> imagesInfo1 = (Map<String, Object>) page.get("images");
                final Map<String, Object> imagesInfo2 = (Map<String, Object>) page.get("qss");
                String urlPath = null, query = null;
                for (final String possibleQualityIdentifierSorted : possibleQualityIdentifiersSorted) {
                    if (imagesInfo1.containsKey(possibleQualityIdentifierSorted) && imagesInfo2.containsKey(possibleQualityIdentifierSorted)) {
                        // bestResolution = (String) dimensions.get(possibleQualityIdentifierSorted);
                        urlPath = imagesInfo1.get(possibleQualityIdentifierSorted).toString();
                        query = imagesInfo2.get(possibleQualityIdentifierSorted).toString();
                        break;
                    }
                }
                final String directurl = base_path + urlPath + "?" + query;
                final String filename = i + "_" + basetitle;
                final DownloadLink dl = createDownloadlink(directurl + "");
                dl.setFinalFileName(filename);
                dl.setAvailable(true);
                if (setComment) {
                    dl.setComment(description);
                }
                ret.add(dl);
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                fp.addLinks(ret);
            }
        } else {
            /* Old way */
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("https://www." + this.getHost() + "/en/document/json/" + fid);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Object errorO = entries.get("error");
            if (errorO != null) {
                /* E.g. {"error":{"reason":"deleted","message":"Document deleted"}} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> document = (Map<String, Object>) entries.get("document");
            final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) document.get("pages");
            final String description = (String) document.get("description");
            String fpName = (String) document.get("title");
            if (StringUtils.isEmpty(fpName)) {
                /* Fallback */
                fpName = url_name;
            }
            final String base_path = (String) document.get("base_path");
            final Map<String, Object> images = (Map<String, Object>) document.get("images");
            final String basetitle = (String) images.get("title");
            final Map<String, Object> dimensions = (Map<String, Object>) images.get("dimensions");
            String bestResolution = null;
            String bestResolutionIdentifier = null;
            for (final String possibleQualityIdentifierSorted : possibleQualityIdentifiersSorted) {
                if (dimensions.containsKey(possibleQualityIdentifierSorted)) {
                    bestResolution = (String) dimensions.get(possibleQualityIdentifierSorted);
                    bestResolutionIdentifier = possibleQualityIdentifierSorted;
                    break;
                }
            }
            if (StringUtils.isEmpty(base_path) || StringUtils.isEmpty(basetitle) || StringUtils.isEmpty(bestResolution) || ressourcelist == null || ressourcelist.size() == 0) {
                return null;
            }
            final boolean setComment = !StringUtils.isEmpty(description) && !description.equalsIgnoreCase(fpName);
            for (int i = 0; i <= ressourcelist.size() - 1; i++) {
                /* 2019-05-21: 'quality' = percentage of quality */
                final Map<String, Object> page = ressourcelist.get(i);
                final Map<String, Object> imagesInfo = (Map<String, Object>) page.get("images");
                final String bestResolutionBaseURL = (String) imagesInfo.get(bestResolutionIdentifier);
                final String directurl = base_path + bestResolutionBaseURL;
                final String filename = i + "_" + basetitle;
                final DownloadLink dl = createDownloadlink(directurl + "");
                dl.setFinalFileName(filename);
                dl.setAvailable(true);
                if (setComment) {
                    dl.setComment(description);
                }
                ret.add(dl);
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                fp.addLinks(ret);
            }
        }
        return ret;
    }
}
