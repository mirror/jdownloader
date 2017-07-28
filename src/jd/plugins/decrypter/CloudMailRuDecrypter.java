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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cloud.mail.ru" }, urls = { "https?://(?:www\\.)?cloud\\.mail\\.ru((?:/|%2F)public(?:/|%2F)[a-z0-9]+(?:/|%2F)[^<>\"]+|(?:/|%2F)(?:files(?:/|%2F))?[A-Z0-9]{32})" })
public class CloudMailRuDecrypter extends PluginForDecrypt {
    public CloudMailRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String  BUILD            = "release-33-1.201603012259";
    /* Max .zip filesize = 4 GB */
    private static final double MAX_ZIP_FILESIZE = 4194304;
    private static String       DOWNLOAD_ZIP     = "DOWNLOAD_ZIP_2";
    private static final String TYPE_APIV2       = "https?://(www\\.)?cloud\\.mail\\.ru/(?:files/)?[A-Z0-9]{32}";
    private String              json;
    private String              parameter        = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        prepBR();
        parameter = Encoding.htmlDecode(param.toString()).replace("http://", "https://");
        if (parameter.endsWith("/")) {
            parameter = parameter.substring(0, parameter.lastIndexOf("/"));
        }
        String id;
        final DownloadLink main = createDownloadlink("http://clouddecrypted.mail.ru/" + System.currentTimeMillis() + new Random().nextInt(100000));
        if (parameter.matches(TYPE_APIV2)) {
            id = new Regex(parameter, "([A-Z0-9]{32})$").getMatch(0);
            main.setName(parameter);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("https://cloud.mail.ru/api/v2/batch", "files=" + id + "&batch=%5B%7B%22method%22%3A%22folder%2Ftree%22%7D%2C%7B%22method%22%3A%22folder%22%7D%5D&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&api=2&build=" + BUILD);
            /* Offline|Empty folder */
            if (br.containsHTML("\"status\":400|\"count\":\\{\"folders\":0,\"files\":0\\}")) {
                main.setFinalFileName(id);
                main.setAvailable(false);
                main.setProperty("offline", true);
                decryptedLinks.add(main);
                return decryptedLinks;
            }
            json = br.toString();
        } else {
            id = new Regex(parameter, "cloud\\.mail\\.ru/public/(.+)").getMatch(0);
            main.setName(new Regex(parameter, "public/[a-z0-9]+/(.+)").getMatch(0));
            final String id_url_encoded = Encoding.urlEncode(id);
            br.getPage("https://cloud.mail.ru/api/v2/folder?weblink=" + id_url_encoded + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&offset=0&limit=500&api=2&build=" + BUILD);
            json = br.toString();
            if (br.containsHTML("\"status\":(400|404)") || br.getHttpConnection().getResponseCode() == 404) {
                main.setAvailable(false);
                main.setProperty("offline", true);
                decryptedLinks.add(main);
                return decryptedLinks;
            }
        }
        main.setProperty("plain_request_id", id);
        main.setProperty("mainlink", parameter);
        String mainName = new Regex(json, "\"body\":\\{\"count\":\\{\"folders\":\\d+,\"files\":\\d+\\},\"name\":\"([^<>\"]*?)\"").getMatch(0);
        if (mainName == null) {
            mainName = new Regex(parameter, "public/([a-z0-9]+)/").getMatch(0);
        }
        if (mainName == null) {
            mainName = id;
        }
        mainName = Encoding.htmlDecode(mainName.trim());
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("body");
        // final String title_json = (String)entries.get("name");
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("list");
        if (ressourcelist == null || ressourcelist.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage("https://cloud.mail.ru/api/v2/dispatcher?api=2&build=" + BUILD + "&_=" + System.currentTimeMillis());
        final String dataserver = br.getRegex("\"url\":\"(https?://[a-z0-9]+\\.datacloudmail\\.ru/weblink/)view/\"").getMatch(0);
        long totalSize = 0;
        boolean folderContainsSubfolder = false;
        final HashMap<String, List<DownloadLink>> results = new HashMap<String, List<DownloadLink>>();
        for (final Object fileo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) fileo;
            final String type = (String) entries.get("kind");
            String weblink = (String) entries.get("weblink");
            final String item_name = (String) entries.get("name");
            if (StringUtils.isEmpty(weblink)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if ("folder".equals(type)) {
                folderContainsSubfolder = true;
                weblink = Encoding.htmlDecode(weblink);
                weblink = "https://cloud.mail.ru/public/" + weblink;
                decryptedLinks.add(createDownloadlink(weblink));
            } else {
                final DownloadLink dl = createDownloadlink("http://clouddecrypted.mail.ru/" + System.currentTimeMillis() + new Random().nextInt(100000));
                /* Cut that */
                weblink = new Regex(weblink, "([^<>\"]*?)/[^<>\"/]+").getMatch(0);
                final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                if (filesize == 0 || item_name == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                String unique_id;
                if (id.contains(item_name)) {
                    unique_id = id;
                } else {
                    unique_id = id + "/" + item_name;
                }
                if (dataserver != null) {
                    final String directlink = dataserver + "get/" + unique_id + "?x-email=undefined";
                    dl.setProperty("plain_directlink", directlink);
                }
                dl.setDownloadSize(filesize);
                totalSize += filesize;
                dl.setFinalFileName(item_name);
                dl.setProperty("plain_name", item_name);
                dl.setProperty("plain_size", filesize);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("plain_request_id", id);
                dl.setProperty("unique_id", unique_id);
                /** TODO: Remove this */
                if (parameter.matches(TYPE_APIV2)) {
                    dl.setProperty("noapi", true);
                }
                final String browserurl = "https://cloud.mail.ru/public/" + unique_id;
                dl.setProperty("browser_url", browserurl);
                List<DownloadLink> result = results.get(weblink);
                if (result == null) {
                    result = new ArrayList<DownloadLink>();
                    results.put(weblink, result);
                }
                dl.setContentUrl(browserurl);
                dl.setAvailable(true);
                result.add(dl);
            }
        }
        if (folderContainsSubfolder) {
            for (final Entry<String, List<DownloadLink>> entry : results.entrySet()) {
                final List<DownloadLink> downloadLinks = entry.getValue();
                if (downloadLinks.size() > 1 && entry.getKey() != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(entry.getKey());
                    fp.setProperty("ALLOW_MERGE", true);
                    fp.addLinks(new ArrayList<DownloadLink>(downloadLinks));
                }
                decryptedLinks.addAll(downloadLinks);
            }
        } else {
            /* No subfolders --> Use mainName for all items so they all go into one package. */
            for (final Entry<String, List<DownloadLink>> entry : results.entrySet()) {
                final List<DownloadLink> downloadLinks = entry.getValue();
                if (downloadLinks.size() > 1 && entry.getKey() != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(mainName);
                    fp.setProperty("ALLOW_MERGE", true);
                    fp.addLinks(new ArrayList<DownloadLink>(downloadLinks));
                }
                decryptedLinks.addAll(downloadLinks);
            }
        }
        if (decryptedLinks.size() > 1 && totalSize <= MAX_ZIP_FILESIZE * 1024 && SubConfiguration.getConfig("cloud.mail.ru").getBooleanProperty(DOWNLOAD_ZIP, false)) {
            /* = all files (links) of the folder as .zip archive */
            final String main_name = mainName + ".zip";
            main.setProperty("plain_name", main_name);
            main.setProperty("plain_size", Long.toString(totalSize));
            main.setProperty("complete_folder", true);
            decryptedLinks.add(main);
        }
        return decryptedLinks;
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }
}
