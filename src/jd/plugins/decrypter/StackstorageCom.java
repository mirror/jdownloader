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
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "stackstorage.com" }, urls = { "https?://([a-z0-9]+)\\.stackstorage\\.com/s/([A-Za-z0-9]+)(\\?dir=([^\\&]+)\\&node\\-id=(\\d+))?" })
public class StackstorageCom extends antiDDoSForDecrypt {
    public StackstorageCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String subdomain = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String folderID = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        String subdir = new Regex(parameter, this.getSupportedLinks()).getMatch(3);
        if (subdir == null) {
            /* Start from root */
            subdir = "%2F";
        }
        String passCode = null;
        if (br.getURL().contains("/login")) {
            /* 2019-02-01: TODO: Not yet supported */
            /* Password required */
            logger.info("Password protected URLs are not yet supported");
            decryptedLinks.add(this.createOfflinelink(parameter, "PASSWORD_PROTECTED"));
            return decryptedLinks;
        }
        final String csrftoken = br.getRegex("name=\"csrf-token\" content=\"([^\"]+)\"").getMatch(0);
        if (StringUtils.isEmpty(csrftoken)) {
            return null;
        }
        int offset = 0;
        int page = 0;
        final int maxItemsPerRequest = 50;
        boolean hasNext = false;
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("CSRF-Token", csrftoken);
        br.getHeaders().put("X-CSRF-Token", csrftoken);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Omit-Authentication-Header", "true");
        boolean isSingleFile = false;
        do {
            if (this.isAbort()) {
                break;
            }
            getPage(String.format("https://%s.stackstorage.com/public-share/%s/list?public=true&token=%s&type=folder&offset=%d&limit=%d&sortBy=default&order=asc&query=&dir=%s&_=%s", subdomain, folderID, folderID, offset, maxItemsPerRequest, subdir, System.currentTimeMillis()));
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("nodes");
            if (page == 0 && ressourcelist == null && entries != null && entries.containsKey("fileId")) {
                /* Looks like we got a single file only! */
                ressourcelist = new ArrayList<Object>();
                ressourcelist.add(entries);
                isSingleFile = true;
            } else if (ressourcelist == null) {
                /* Probably end of pagination */
                break;
            }
            for (final Object fileO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) fileO;
                String path_with_filename = (String) entries.get("path");
                final String fileid = Long.toString(JavaScriptEngineFactory.toLong(entries.get("fileId"), 0));
                final String mimetype = (String) entries.get("mimetype");
                final long filesize = JavaScriptEngineFactory.toLong(entries.get("fileSize"), 0);
                if (StringUtils.isEmpty(path_with_filename) || StringUtils.isEmpty(fileid) || "0".equals(fileid)) {
                    /* Skip invalid objectd */
                    continue;
                }
                final DownloadLink dl;
                if ("httpd/unix-directory".equalsIgnoreCase(mimetype)) {
                    /* Folder */
                    dl = createDownloadlink(String.format("https://%s.stackstorage.com/s/%s?dir=%s&node-id=%s", subdomain, folderID, URLEncode.encodeURIComponent(path_with_filename), fileid));
                } else {
                    /* File */
                    dl = createDownloadlink(String.format("https://stackstorage.com/fileid/%s", fileid));
                    /* Path also contains filename but we need to separate that and remove filename from path */
                    final String path_without_filename;
                    final String[] pathParts = path_with_filename.split("/");
                    // path = CrossSystem.alleviatePathParts(path);
                    String filename = pathParts[pathParts.length - 1];
                    if (filename == null) {
                        /* Fallback */
                        filename = fileid;
                    }
                    final String packagename;
                    FilePackage fp = FilePackage.getInstance();
                    if (pathParts.length > 1 && filename != null) {
                        /* Remove filename from path */
                        path_without_filename = path_with_filename.replace("/" + filename, "");
                        /* Files inside same folder go inside same package! */
                        packagename = pathParts[pathParts.length - 2];
                        fp = FilePackage.getInstance();
                    } else {
                        /* Root */
                        path_without_filename = "/root";
                        /* No foldername given --> Create our own packagename */
                        packagename = subdomain + "_" + folderID;
                    }
                    fp.setName(packagename);
                    dl._setFilePackage(fp);
                    dl.setLinkID(fileid);
                    dl.setContainerUrl(parameter);
                    /* There are no URLs for individual files! */
                    dl.setContentUrl(parameter);
                    dl.setDownloadSize(filesize);
                    dl.setName(filename);
                    dl.setAvailable(true);
                    if (passCode != null) {
                        dl.setDownloadPassword(passCode);
                    }
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path_without_filename);
                    if (isSingleFile) {
                        dl.setProperty("download_path", "/");
                    } else {
                        dl.setProperty("download_path", path_with_filename);
                    }
                }
                decryptedLinks.add(dl);
                distribute(dl);
                offset++;
            }
            hasNext = ressourcelist.size() >= maxItemsPerRequest;
            page++;
        } while (hasNext);
        return decryptedLinks;
    }
}
