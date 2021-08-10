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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.HidriveCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hidrive.com" }, urls = { "https?://(?:my\\.hidrive\\.com/share/|(?:www\\.)?hidrive\\.strato\\.com/share)([^/#]+)" })
public class HidriveComCrawler extends PluginForDecrypt {
    public HidriveComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        HidriveCom.prepBRAPI(this.br);
        br.getPage("https://my.hidrive.com/api/share/info?id=" + Encoding.urlEncode(contentID));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* {"viewmode":"c","is_encrypted":false,"writable":false,"has_password":false,"readable":true} */
        final Map<String, Object> folderInfo = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final UrlQuery folderQuery = new UrlQuery();
        folderQuery.add("id", contentID);
        String passCode = param.getDecrypterPassword();
        if ((Boolean) folderInfo.get("has_password")) {
            /* TODO: Check if this password handling is working and add check for wrong password! */
            if (passCode == null) {
                passCode = getUserInput("Password?", param);
            }
            folderQuery.add("password", Encoding.urlEncode(passCode));
        }
        br.postPage("/api/share/token", folderQuery);
        /* {"expires_in":14400,"access_token":"XXXXYYYY","root_name":"Galerie-Ansicht","token_type":"Bearer"} */
        final Map<String, Object> token = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String root_name = token.get("root_name").toString();
        final String access_token = token.get("access_token").toString();
        final long access_token_valid_until = System.currentTimeMillis() + ((Number) token.get("expires_in")).longValue() * 1000;
        if (StringUtils.isEmpty(access_token)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Authorization", "Bearer " + access_token);
        br.getPage("/api/dir?path=/&fields=id%2Cpath%2Creadable%2Cwritable%2Cmembers.id%2Cmembers.parent_id%2Cmembers.name%2Cmembers.mtime%2Cmembers.mime_type%2Cmembers.path%2Cmembers.readable%2Cmembers.writable%2Cmembers.type%2Cmembers.image.width%2Cmembers.image.height%2Cmembers.image.exif.Orientation%2Cmembers.size&members=all&limit=0%2C5000&sort=none");
        final Map<String, Object> folder = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) folder.get("members");
        for (final Map<String, Object> ressource : ressourcelist) {
            /* 2021-08-10: Ignore folders/subfolders for now */
            if (ressource.get("type").toString().equals("file")) {
                final DownloadLink link = this.createDownloadlink("https://my.hidrive.com/share/" + contentID + "#file_id=" + ressource.get("id"));
                /* There is no file-specific URL available --> Set url added by the user */
                link.setContentUrl(param.getCryptedUrl());
                link.setFinalFileName(ressource.get("name").toString());
                link.setVerifiedFileSize(((Number) ressource.get("size")).longValue());
                link.setAvailable(true);
                link.setProperty(HidriveCom.PROPERTY_ACCESS_TOKEN, access_token);
                link.setProperty(HidriveCom.PROPERTY_ACCESS_TOKEN_VALID_UNTIL, access_token_valid_until);
                if (passCode != null) {
                    link.setDownloadPassword(passCode);
                }
                decryptedLinks.add(link);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(root_name);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
