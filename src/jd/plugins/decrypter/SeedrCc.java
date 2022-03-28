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

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "seedr.cc" }, urls = { "https?://(?:www\\.)?seedr\\.cc/files/(\\d+)" })
public class SeedrCc extends PluginForDecrypt {
    public SeedrCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        if (aa == null) {
            logger.info("Account needed to use this crawler");
            throw new AccountRequiredException();
        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        ((jd.plugins.hoster.SeedrCc) plg).login(aa, false);
        jd.plugins.hoster.SeedrCc.prepAjaxBr(this.br);
        br.getPage("https://www." + this.getHost() + "/fs/folder/" + folderID + "/items");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> response = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Map<String, Object>> ressourcelist_files = (List<Map<String, Object>>) response.get("files");
        final List<Map<String, Object>> ressourcelist_folders = (List<Map<String, Object>>) response.get("folders");
        final String full_path = (String) response.get("path");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(full_path);
        if (ressourcelist_files != null) {
            /* Crawl files --> Urls go into host plugin */
            for (final Map<String, Object> fileInfo : ressourcelist_files) {
                final String filename = fileInfo.get("name").toString();
                final String fileID = fileInfo.get("id").toString();
                final String hash = (String) fileInfo.get("hash");
                if (filename == null || fileID == null) {
                    /* This should never happen! */
                    continue;
                }
                final DownloadLink file = createDownloadlink("http://seedrdecrypted.cc/" + fileID);
                file.setFinalFileName(filename);
                file.setVerifiedFileSize(((Long) fileInfo.get("size")).longValue());
                if (hash != null) {
                    file.setMD5Hash(hash);
                }
                file.setAvailable(true);
                file.setContentUrl(param.getCryptedUrl());
                file.setRelativeDownloadFolderPath(full_path);
                file._setFilePackage(fp);
                decryptedLinks.add(file);
            }
        }
        if (ressourcelist_folders != null) {
            /* Crawl folders --> These urls go back into decrypter */
            for (final Map<String, Object> folderInfo : ressourcelist_folders) {
                final String id = folderInfo.get("id").toString();
                if (id.equals("0")) {
                    /* This should never happen! */
                    continue;
                }
                decryptedLinks.add(createDownloadlink("https://www." + this.getHost() + "/files/" + id));
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Found nothing - probably only empty folder(s)");
        }
        return decryptedLinks;
    }
}
