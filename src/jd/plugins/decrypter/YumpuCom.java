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
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "yumpu.com" }, urls = { "https?://(?:www\\.)?yumpu\\.com/[a-z]{2}/document/read/(\\d+)/([a-z0-9\\-]+)" })
public class YumpuCom extends PluginForDecrypt {
    public YumpuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String url_name = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1);
        final String[] possibleQualityIdentifiersSorted = new String[] { "big", "large", "medium", "small" };
        /* Old way */
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("https://www." + this.getHost() + "/en/document/json2/" + fid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
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
        final String title = (String) images.get("title");
        final Map<String, Object> dimensions = (Map<String, Object>) images.get("dimensions");
        String best_resolution = null;
        String best_resolution_label = null;
        for (final String quality : possibleQualityIdentifiersSorted) {
            best_resolution = (String) dimensions.get(quality);
            if (best_resolution != null) {
                best_resolution_label = quality;
                break;
            }
        }
        if (StringUtils.isEmpty(base_path) || StringUtils.isEmpty(title) || StringUtils.isEmpty(best_resolution) || ressourcelist == null || ressourcelist.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final boolean setComment = !StringUtils.isEmpty(description) && !description.equalsIgnoreCase(fpName);
        for (int i = 0; i < ressourcelist.size(); i++) {
            /* 2019-05-21: 'quality' parameter = percentage of quality */
            final Map<String, Object> ressource = ressourcelist.get(i);
            final Map<String, Object> this_images = (Map<String, Object>) ressource.get("images");
            final Map<String, Object> this_qss = (Map<String, Object>) ressource.get("qss");
            final String this_image_best_resolution = this_images.get(best_resolution_label).toString();
            final String this_image_best_resolution_aws_auth = this_qss.get(best_resolution_label).toString();
            final String directurl = base_path + this_image_best_resolution + "?" + this_image_best_resolution_aws_auth;
            final String filename = i + "_" + title;
            final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
            if (setComment) {
                dl.setComment(description);
            }
            ret.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(ret);
        }
        return ret;
    }
}
