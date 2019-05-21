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
import java.util.LinkedHashMap;

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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "yumpu.com" }, urls = { "https?://(?:www\\.)?yumpu\\.com/[a-z]{2}/document/read/(\\d+)/([a-z0-9\\-]+)" })
public class YumpuCom extends PluginForDecrypt {
    public YumpuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String url_name = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        br.getPage("https://www." + this.getHost() + "/en/document/json/" + fid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Object errorO = entries.get("error");
        if (errorO != null) {
            /* E.g. {"error":{"reason":"deleted","message":"Document deleted"}} */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        entries = (LinkedHashMap<String, Object>) entries.get("document");
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("pages");
        final String description = (String) entries.get("description");
        String fpName = (String) entries.get("title");
        if (StringUtils.isEmpty(fpName)) {
            /* Fallback */
            fpName = url_name;
        }
        final String base_path = (String) entries.get("base_path");
        entries = (LinkedHashMap<String, Object>) entries.get("images");
        final String base_title = (String) entries.get("title");
        final LinkedHashMap<String, Object> dimensions = (LinkedHashMap<String, Object>) entries.get("dimensions");
        final String best_resolution = (String) dimensions.get("big");
        if (StringUtils.isEmpty(base_path) || StringUtils.isEmpty(base_title) || StringUtils.isEmpty(best_resolution) || ressourcelist == null || ressourcelist.size() == 0) {
            return null;
        }
        final boolean setComment = !StringUtils.isEmpty(description) && !description.equalsIgnoreCase(fpName);
        for (int i = 1; i <= ressourcelist.size(); i++) {
            /* 2019-05-21: 'quality' = percentage of quality */
            final String directurl = String.format("directhttp://%s/%d/%s/%s?quality=100", base_path, i, best_resolution, base_title);
            final String filename = i + "_" + base_title;
            final DownloadLink dl = createDownloadlink(directurl);
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
            if (setComment) {
                dl.setComment(description);
            }
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
