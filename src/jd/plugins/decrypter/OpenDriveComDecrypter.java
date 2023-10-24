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
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "opendrive.com" }, urls = { "https?://(?:www\\.)?opendrive\\.com/folders\\?[A-Za-z0-9]+|https?://od\\.lk/(?:fl|s)/[A-Za-z0-9]+(?:\\?folderpath=[a-zA-Z0-9_/\\+\\=\\-%]+)?" })
public class OpenDriveComDecrypter extends PluginForDecrypt {
    public OpenDriveComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String folderurl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String folderid = new Regex(folderurl, "([A-Za-z0-9\\-_]+)(\\?folderpath=.+)?$").getMatch(0);
        this.br.setFollowRedirects(true);
        br.getPage("https://od.lk/fl/" + folderid);
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String csrftoken = br.getRegex("data\\-csrftoken=\"([^<>\"]+)\"").getMatch(0);
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        jd.plugins.hoster.OpenDriveCom.prepBRAjax(this.br);
        br.getHeaders().put("Origin", "https://od.lk");
        br.getHeaders().put("X-Ajax-CSRF-Token", csrftoken);
        br.postPage("/ajax", "action=files.load-folder-content&folder_id=" + folderid + "&with_breadcrumbs=1&last_request_time=0&public=1&offset=0&order_by=name&order_type=asc");
        final Map<String, Object> root = restoreFromString(br.toString(), TypeRef.MAP);
        final Number errorcode = (Number) JavaScriptEngineFactory.walkJson(root, "error/code");
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404 || errorcode != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Map<String, Object>> subfolders = (List<Map<String, Object>>) root.get("Folders");
        final List<Map<String, Object>> files = (List<Map<String, Object>>) root.get("Files");
        if (subfolders.isEmpty() && files.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        String fpName = (String) root.get("Name");
        // final String name_of_previous_folder = (String) JavaScriptEngineFactory.walkJson(entries, "Breadcrumbs/{0}/Name");
        if (fpName == null) {
            fpName = folderid;
        }
        String path = new Regex(folderurl, "folderpath=(.+)").getMatch(0);
        if (path != null) {
            path = Encoding.Base64Decode(path);
            path += "/" + fpName;
        } else {
            path = fpName;
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        for (final Map<String, Object> file : files) {
            /* Do not use this as linkid as id inside our url is the one we prefer (also unique). */
            // final String fileid = (String)entries.get("FileId");
            final String url = file.get("Link").toString();
            final String directurl = file.get("DownloadLink").toString();
            final String filename = file.get("Name").toString();
            final Object filesize = file.get("Size");
            final DownloadLink dl = this.createDownloadlink(url);
            dl.setRelativeDownloadFolderPath(path);
            dl.setName(filename);
            if (filesize != null) {
                if (filesize instanceof Number) {
                    dl.setVerifiedFileSize(((Number) filesize).longValue());
                } else {
                    dl.setVerifiedFileSize(Long.parseLong(filesize.toString()));
                }
            }
            if (!StringUtils.isEmpty(directurl)) {
                dl.setProperty("directurl", directurl);
            }
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            ret.add(dl);
        }
        for (final Map<String, Object> folder : subfolders) {
            String url = folder.get("Link").toString();
            url += "?folderpath=" + Encoding.Base64Encode(path);
            final DownloadLink dl = this.createDownloadlink(url);
            ret.add(dl);
        }
        return ret;
    }
}
