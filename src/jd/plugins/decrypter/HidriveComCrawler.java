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
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.hoster.HidriveCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hidrive.com" }, urls = { "https?://(?:my\\.hidrive\\.com/share/|(?:www\\.)?hidrive\\.strato\\.com/share/)(.+)" })
public class HidriveComCrawler extends PluginForDecrypt {
    public HidriveComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String URLREGEX = "https?://[^/]+/share/([^/#]+)(/.+)?";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        return crawlFolder(param, false);
    }

    /** Set returnDummyResult to true if you only want this to return a dummy DownloadLink object to obtain a fresh token. */
    public ArrayList<DownloadLink> crawlFolder(final CryptedLink param, final boolean returnDummyResult) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlregex = new Regex(param.getCryptedUrl(), URLREGEX);
        /* Remove "#$": Required in browser to display paths properly but it is not part of the id we need! */
        final String baseFolderID = urlregex.getMatch(0).replace("#$", "");
        String internalPath = urlregex.getMatch(1);
        if (internalPath == null) {
            internalPath = "/";
        }
        HidriveCom.prepBRAPI(this.br);
        /* Check if this folder is available. */
        br.getPage("https://my.hidrive.com/api/share/info?id=" + Encoding.urlEncode(baseFolderID));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* {"viewmode":"c","is_encrypted":false,"writable":false,"has_password":false,"readable":true} */
        final Map<String, Object> folderInfo = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final UrlQuery folderQuery = new UrlQuery();
        folderQuery.add("id", baseFolderID);
        String passCode = param.getDecrypterPassword();
        if ((Boolean) folderInfo.get("has_password") || passCode != null) {
            /* TODO: Check if this password handling is working and add check for wrong password! */
            if (passCode == null) {
                passCode = getUserInput("Password?", param);
            }
            folderQuery.add("password", Encoding.urlEncode(passCode));
        }
        br.postPage("/api/share/token", folderQuery);
        if (br.getHttpConnection().getResponseCode() == 403) {
            /* User must have entered wrong password: {"msg":"Forbidden","code":"403"} */
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        /* {"expires_in":14400,"access_token":"XXXXYYYY","root_name":"Galerie-Ansicht","token_type":"Bearer"} */
        final Map<String, Object> token = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String access_token = token.get("access_token").toString();
        final long access_token_valid_until = System.currentTimeMillis() + ((Number) token.get("expires_in")).longValue() * 1000;
        if (StringUtils.isEmpty(access_token)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Authorization", "Bearer " + access_token);
        if (returnDummyResult) {
            final DownloadLink dummy = this.createDownloadlink(param.getCryptedUrl());
            dummy.setProperty(HidriveCom.PROPERTY_ACCESS_TOKEN, access_token);
            dummy.setProperty(HidriveCom.PROPERTY_ACCESS_TOKEN_VALID_UNTIL, access_token_valid_until);
            ret.add(dummy);
            return ret;
        }
        final UrlQuery folderOverviewQuery = new UrlQuery();
        folderOverviewQuery.add("path", internalPath);
        folderOverviewQuery.add("fields", Encoding.urlEncode("id,path,readable,writable,members.id,members.parent_id,members.name,members.mtime,members.mime_type,members.path,members.readable,members.writable,members.type,members.image.width,members.image.height,members.image.exif.Orientation,members.size"));
        folderOverviewQuery.add("members", "all");
        folderOverviewQuery.add("limit", Encoding.urlEncode("0,5000"));
        folderOverviewQuery.add("sort", "none");
        br.getPage("/api/dir?" + folderOverviewQuery.toString());
        final Map<String, Object> folderOverview = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) folderOverview.get("members");
        String subfolderPath = this.getAdoptedCloudFolderStructure();
        if (subfolderPath == null) {
            /* Root folder */
            subfolderPath = Encoding.htmlDecode(token.get("root_name").toString());
        }
        if (ressourcelist.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, subfolderPath);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(subfolderPath);
        for (final Map<String, Object> ressource : ressourcelist) {
            if (ressource.get("type").toString().equals("file")) {
                final DownloadLink file = this.createDownloadlink("https://my.hidrive.com/share/" + baseFolderID + "#file_id=" + ressource.get("id"));
                /* There is no file-specific URL available --> Set url added by the user */
                file.setContentUrl(param.getCryptedUrl());
                file.setFinalFileName(URLEncode.decodeURIComponent(ressource.get("name").toString()));
                file.setVerifiedFileSize(((Number) ressource.get("size")).longValue());
                file.setAvailable(true);
                file.setProperty(HidriveCom.PROPERTY_ACCESS_TOKEN, access_token);
                file.setProperty(HidriveCom.PROPERTY_ACCESS_TOKEN_VALID_UNTIL, access_token_valid_until);
                if (passCode != null) {
                    file.setDownloadPassword(passCode);
                }
                file.setRelativeDownloadFolderPath(subfolderPath);
                file._setFilePackage(fp);
                ret.add(file);
            } else if (ressource.get("type").toString().equals("dir")) {
                /* Subfolder */
                final String relativeFolderPathUrlEncoded = (String) ressource.get("path");
                final DownloadLink folder = this.createDownloadlink("https://my.hidrive.com/share/" + baseFolderID + "#$" + relativeFolderPathUrlEncoded);
                folder.setRelativeDownloadFolderPath(subfolderPath + Encoding.htmlDecode(relativeFolderPathUrlEncoded));
                ret.add(folder);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }
}
