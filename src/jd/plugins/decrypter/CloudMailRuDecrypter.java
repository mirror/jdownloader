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
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.CloudMailRu;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cloud.mail.ru" }, urls = { "https?://(?:www\\.)?cloud\\.mail\\.ru((?:/|%2F)public(?:/|%2F)[a-z0-9]+(?:/|%2F)[^<>\"]+|(?:/|%2F)(?:files(?:/|%2F))?[A-Z0-9]{32})" })
public class CloudMailRuDecrypter extends PluginForDecrypt {
    public CloudMailRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Last updated: 2020-12-16 */
    public static final String  BUILD            = "cloudweb-11674-72-8-0.202012151755";
    /* Max .zip filesize = 4 GB // 2018: 10 MB // 20180223 back to 4194304 (4 MB) * 1024 for testing by user */
    private static final double MAX_ZIP_FILESIZE = 4194304;
    private static String       DOWNLOAD_ZIP     = "DOWNLOAD_ZIP_2";
    private static final String TYPE_APIV2       = "https?://(www\\.)?cloud\\.mail\\.ru/(?:files/)?[A-Z0-9]{32}";
    private String              json;
    private String              parameter        = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        prepBR();
        parameter = Encoding.htmlDecode(param.toString()).replace("http://", "https://");
        if (parameter.endsWith("/")) {
            parameter = parameter.substring(0, parameter.lastIndexOf("/"));
        }
        String subfolder = this.getAdoptedCloudFolderStructure();
        if (subfolder == null) {
            subfolder = "";
        }
        String id;
        String dummyFilenameForEmptyFolders;
        if (parameter.matches(TYPE_APIV2)) {
            id = new Regex(parameter, "([A-Z0-9]{32})$").getMatch(0);
            if (StringUtils.isEmpty(subfolder)) {
                dummyFilenameForEmptyFolders = id;
            } else {
                dummyFilenameForEmptyFolders = id + "_" + subfolder;
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage(CloudMailRu.API_BASE + "/batch", "files=" + id + "&batch=%5B%7B%22method%22%3A%22folder%2Ftree%22%7D%2C%7B%22method%22%3A%22folder%22%7D%5D&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&api=2&build=" + BUILD);
            /* Offline|Empty folder */
            if (br.containsHTML("\"status\":400")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("\"count\":\\{\"folders\":0,\"files\":0\\}")) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, dummyFilenameForEmptyFolders);
            }
        } else {
            id = new Regex(parameter, "cloud\\.mail\\.ru/public/(.+)").getMatch(0);
            if (StringUtils.isEmpty(subfolder)) {
                dummyFilenameForEmptyFolders = id;
            } else {
                dummyFilenameForEmptyFolders = id + "_" + subfolder;
            }
            br.getPage(CloudMailRu.API_BASE + "/folder?weblink=" + URLEncode.encodeURIComponent(id) + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&offset=0&limit=0&api=2&build=" + BUILD);
            final String nfolders = PluginJSonUtils.getJsonValue(br.toString(), "folders");
            final String nfiles = PluginJSonUtils.getJsonValue(br.toString(), "files");
            final String limit = nfolders + nfiles;
            br.getPage(CloudMailRu.API_BASE + "/folder?weblink=" + URLEncode.encodeURIComponent(id) + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&offset=0&limit=" + limit + "&api=2&build=" + BUILD);
            if (br.containsHTML("\"status\":(400|404)") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        // main.setProperty("plain_request_id", id);
        json = br.toString();
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
        entries = (Map<String, Object>) entries.get("body");
        long completeFolderSize = 0;
        if (entries.containsKey("size")) {
            completeFolderSize = ((Number) entries.get("size")).longValue();
        }
        final String title_of_current_folder = (String) entries.get("name");
        if (!StringUtils.isEmpty(title_of_current_folder) && StringUtils.isEmpty(subfolder)) {
            subfolder = title_of_current_folder;
            dummyFilenameForEmptyFolders = id + "_" + subfolder;
        }
        final List<Object> ressourcelist = (List<Object>) entries.get("list");
        if (ressourcelist.size() == 0) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, dummyFilenameForEmptyFolders);
        }
        FilePackage fp = null;
        /* "/" is most likely a single file inside a (theoretical) folder [root] -> Do not assign packagenames in this case! */
        if (!StringUtils.isEmpty(subfolder) && !subfolder.equals("/")) {
            fp = FilePackage.getInstance();
            fp.setName(subfolder);
            fp.setAllowMerge(true);
        }
        for (final Object fileo : ressourcelist) {
            entries = (Map<String, Object>) fileo;
            final String type = (String) entries.get("kind");
            String weblink = (String) entries.get("weblink");
            final String itemTitle = (String) entries.get("name");
            if (StringUtils.isEmpty(weblink) || StringUtils.isEmpty(itemTitle)) {
                /* Skip invalid objects */
                continue;
            }
            if ("folder".equals(type)) {
                weblink = Encoding.htmlOnlyDecode(weblink, false);
                String encoded_weblink = URLEncode.encodeURIComponent(weblink);
                /* We need the "/" so let's encode them back. */
                encoded_weblink = encoded_weblink.replace("%2F", "/");
                encoded_weblink = encoded_weblink.replace("+", "%20");
                if (!encoded_weblink.endsWith("/")) {
                    // spaces at the end without / will be removed
                    encoded_weblink = encoded_weblink + "/";
                }
                weblink = "https://cloud.mail.ru/public/" + encoded_weblink;
                final DownloadLink folderLink = createDownloadlink(weblink);
                folderLink.setRelativeDownloadFolderPath(subfolder + "/" + itemTitle);
                decryptedLinks.add(folderLink);
            } else {
                if ("illegal".equals(entries.get("uflr"))) {
                    // flagged as illegal, no longer available, doesn't show up in browser either
                    continue;
                }
                String encoded_weblink = URLEncode.encodeURIComponent(weblink);
                /* We need the "/" so let's encode them back. */
                encoded_weblink = encoded_weblink.replace("%2F", "/");
                encoded_weblink = encoded_weblink.replace("+", "%20");
                final String contenturl = "https://cloud.mail.ru/public/" + encoded_weblink;
                final DownloadLink dl = createDownloadlink(contenturl);
                final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                if (itemTitle == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.setDownloadSize(filesize);
                dl.setFinalFileName(itemTitle);
                dl.setProperty("mainlink", parameter);
                // PROPERTY_WEBLINK in raw format!
                dl.setProperty(CloudMailRu.PROPERTY_WEBLINK, weblink);
                /** TODO: Remove this */
                if (parameter.matches(TYPE_APIV2)) {
                    dl.setProperty("noapi", true);
                }
                dl.setAvailable(true);
                // String incompletePathWithoutRootFolder = new Regex(weblink, "^[A-Za-z0-9]+/[A-Za-z0-9]+/(.+)").getMatch(0);
                // if (incompletePathWithoutRootFolder.contains("/" + itemTitle)) {
                // incompletePathWithoutRootFolder = incompletePathWithoutRootFolder.replace(itemTitle, "");
                // }
                // if (!StringUtils.isEmpty(incompletePathWithoutRootFolder)) {
                // dl.setRelativeDownloadFolderPath(incompletePathWithoutRootFolder);
                // }
                if (!StringUtils.isEmpty(subfolder)) {
                    dl.setRelativeDownloadFolderPath(subfolder);
                }
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() > 1 && completeFolderSize <= MAX_ZIP_FILESIZE * 1024 && SubConfiguration.getConfig("cloud.mail.ru").getBooleanProperty(DOWNLOAD_ZIP, false)) {
            /* = all files (links) of the folder as .zip archive */
            final DownloadLink main = createDownloadlink(parameter);
            if (!StringUtils.isEmpty(title_of_current_folder)) {
                main.setFinalFileName(title_of_current_folder + ".zip");
            } else {
                /* Fallback */
                main.setFinalFileName(id + ".zip");
            }
            // main.setProperty("plain_size", Long.toString(totalSize));
            main.setProperty(CloudMailRu.PROPERTY_COMPLETE_FOLDER, true);
            main.setProperty(CloudMailRu.PROPERTY_WEBLINK, id);
            main.setDownloadSize(completeFolderSize);
            if (!StringUtils.isEmpty(subfolder)) {
                main.setRelativeDownloadFolderPath(subfolder);
            }
            if (fp != null) {
                main._setFilePackage(fp);
            }
            decryptedLinks.add(main);
        }
        return decryptedLinks;
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
        br.setAllowedResponseCodes(new int[] { 400 });
    }
}
