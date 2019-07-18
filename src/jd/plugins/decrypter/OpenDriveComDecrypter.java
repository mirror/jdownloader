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
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "opendrive.com" }, urls = { "https?://(?:www\\.)?opendrive\\.com/folders\\?[A-Za-z0-9]+|https?://od\\.lk/(?:fl|s)/[A-Za-z0-9]+(?:\\?folderpath=[a-zA-Z0-9_/\\+\\=\\-%]+)?" })
public class OpenDriveComDecrypter extends PluginForDecrypt {
    public OpenDriveComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        final String folderid = new Regex(parameter, "([A-Za-z0-9\\-_]+)(\\?folderpath=.+)?$").getMatch(0);
        this.br.setFollowRedirects(true);
        br.getPage("https://od.lk/fl/" + folderid);
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName("folder_offline_" + folderid);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String csrftoken = br.getRegex("data\\-csrftoken=\"([^<>\"]+)\"").getMatch(0);
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        jd.plugins.hoster.OpenDriveCom.prepBRAjax(this.br);
        br.getHeaders().put("Origin", "https://od.lk");
        br.getHeaders().put("X-Ajax-CSRF-Token", csrftoken);
        br.postPage("https://od.lk/ajax", "action=files.load-folder-content&folder_id=" + folderid + "&with_breadcrumbs=1&last_request_time=0&public=1&offset=0&order_by=name&order_type=asc");
        final String error = PluginJSonUtils.getJson(br, "error");
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404 || error != null) {
            logger.info("Error:" + error);
            final DownloadLink offline = createOfflinelink(parameter);
            offline.setFinalFileName("folder_offline_" + folderid);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<Object> folders = (ArrayList<Object>) entries.get("Folders");
        final ArrayList<Object> files = (ArrayList<Object>) entries.get("Files");
        String fpName = (String) entries.get("Name");
        // final String name_of_previous_folder = (String) JavaScriptEngineFactory.walkJson(entries, "Breadcrumbs/{0}/Name");
        if (fpName == null) {
            fpName = folderid;
        }
        String path = new Regex(parameter, "folderpath=(.+)").getMatch(0);
        if (path != null) {
            path = Encoding.Base64Decode(path);
            path += "/" + fpName;
        } else {
            path = fpName;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        for (final Object fileo : files) {
            entries = (LinkedHashMap<String, Object>) fileo;
            /* Do not use this as linkid as id inside our url is the one we prefer (also unique). */
            // final String fileid = (String)entries.get("FileId");
            final String url = (String) entries.get("Link");
            final String directurl = (String) entries.get("DownloadLink");
            final String filename = (String) entries.get("Name");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("Size"), -1);
            if (StringUtils.isEmpty(url) || StringUtils.isEmpty(filename) || filesize == -1) {
                continue;
            }
            final DownloadLink dl = this.createDownloadlink(url);
            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path);
            dl.setName(filename);
            dl.setDownloadSize(filesize);
            if (!StringUtils.isEmpty(directurl)) {
                dl.setProperty("directurl", directurl);
            }
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            decryptedLinks.add(dl);
        }
        for (final Object foldero : folders) {
            entries = (LinkedHashMap<String, Object>) foldero;
            String url = (String) entries.get("Link");
            if (StringUtils.isEmpty(url)) {
                continue;
            }
            url += "?folderpath=" + Encoding.Base64Encode(path);
            final DownloadLink dl = this.createDownloadlink(url);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
