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
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "aparat.com" }, urls = { "https?://(?:www\\.)?aparat.com/v/([A-Za-z0-9]+)" })
public class AparatCom extends PluginForDecrypt {
    public AparatCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String itemID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.setAllowedResponseCodes(400);
        br.getPage("https://www." + this.getHost() + "/api/fa/v1/video/video/show/videohash/" + itemID + "?pr=1&mf=1");
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Number status = (Number) JavaScriptEngineFactory.walkJson(entries, "meta/status");
        if (status != null && (status.intValue() == 404 || status.intValue() == 410)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String type = (String) JavaScriptEngineFactory.walkJson(entries, "data/type");
        if (StringUtils.equalsIgnoreCase(type, "VideoShow")) {
            final Map<String, Object> videoShow = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/attributes");
            final String title = (String) videoShow.get("title");
            final List<Map<String, Object>> file_link_all = (List<Map<String, Object>>) videoShow.get("file_link_all");
            if (file_link_all != null) {
                // last one is best quality
                final Map<String, Object> lastEntry = file_link_all.get(file_link_all.size() - 1);
                final List<String> urls = (List<String>) lastEntry.get("urls");
                final DownloadLink dl = this.createDownloadlink("directhttp://" + urls.get(0));
                final String profile = (String) lastEntry.get("profile");
                String fileName;
                if (!StringUtils.isEmpty(title)) {
                    fileName = title;
                } else {
                    fileName = itemID;
                }
                if (profile != null) {
                    fileName += "_" + profile;
                }
                dl.setFinalFileName(fileName + ".mp4");
                dl.setAvailable(true);
                dl.setContentUrl(parameter);
                ret.add(dl);
            }
            if (!title.isEmpty()) {
                final FilePackage filePackage = FilePackage.getInstance();
                filePackage.setName(Encoding.htmlDecode(title));
                filePackage.setComment(title);
                filePackage.addLinks(ret);
            }
            return ret;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported type:" + type);
        }
    }
}