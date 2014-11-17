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
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloud.mail.ru" }, urls = { "https?://(www\\.)?cloud\\.mail\\.ru((/|%2F)public(/|%2F)[a-z0-9]+(/|%2F)[^<>\"]+|/[A-Z0-9]{32})" }, flags = { 0 })
public class CloudMailRuDecrypter extends PluginForDecrypt {

    public CloudMailRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String  BUILD            = "hotfix-26-5.201411142035";
    /* Max .zip filesize = 4 GB */
    private static final double MAX_ZIP_FILESIZE = 4194304;
    private static String       DOWNLOAD_ZIP     = "DOWNLOAD_ZIP_2";

    private static final String TYPE_APIV2       = "https?://(www\\.)?cloud\\.mail\\.ru/[A-Z0-9]{32}";

    private String              json;
    private String              PARAMETER        = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        prepBR();
        PARAMETER = Encoding.htmlDecode(param.toString()).replace("http://", "https://");
        if (PARAMETER.endsWith("/")) {
            PARAMETER = PARAMETER.substring(0, PARAMETER.lastIndexOf("/"));
        }
        String id;
        final DownloadLink main = createDownloadlink("http://clouddecrypted.mail.ru/" + System.currentTimeMillis() + new Random().nextInt(100000));
        if (PARAMETER.matches(TYPE_APIV2)) {
            id = new Regex(PARAMETER, "([A-Z0-9]{32})$").getMatch(0);
            main.setName(PARAMETER);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            try {
                br.postPage("https://cloud.mail.ru/api/v2/batch", "files=" + id + "&batch=%5B%7B%22method%22%3A%22folder%2Ftree%22%7D%2C%7B%22method%22%3A%22folder%22%7D%5D&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&api=2&build=" + BUILD);
            } catch (final BrowserException e) {
                main.setFinalFileName(id);
                main.setAvailable(false);
                main.setProperty("offline", true);
                decryptedLinks.add(main);
                return decryptedLinks;
            }
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
            id = new Regex(PARAMETER, "cloud\\.mail\\.ru/public/(.+)").getMatch(0);
            main.setName(new Regex(PARAMETER, "public/[a-z0-9]+/(.+)").getMatch(0));
            final String id_url_encoded = Encoding.urlEncode(id);
            try {
                br.getPage("https://cloud.mail.ru/api/v2/folder?weblink=" + id_url_encoded + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&offset=0&limit=500&api=2&build=" + BUILD);
            } catch (final BrowserException e) {
                main.setFinalFileName(id);
                main.setAvailable(false);
                main.setProperty("offline", true);
                decryptedLinks.add(main);
                return decryptedLinks;
            }
            json = br.toString();
            if (br.containsHTML("\"status\":(400|404)") || br.getHttpConnection().getResponseCode() == 404) {
                main.setAvailable(false);
                main.setProperty("offline", true);
                decryptedLinks.add(main);
                return decryptedLinks;
            }
        }
        main.setProperty("plain_request_id", id);
        main.setProperty("mainlink", PARAMETER);

        String fpName = null;
        String mainName = new Regex(json, "\"body\":\\{\"count\":\\{\"folders\":\\d+,\"files\":\\d+\\},\"name\":\"([^<>\"]*?)\"").getMatch(0);
        if (mainName == null) {
            mainName = new Regex(PARAMETER, "public/([a-z0-9]+)/").getMatch(0);
        }
        if (mainName == null) {
            mainName = id;
        }
        mainName = Encoding.htmlDecode(mainName.trim());
        fpName = mainName;
        final String[] links = getList(id);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            return null;
        }
        br.getPage("https://cloud.mail.ru/api/v2/dispatcher?api=2&build=" + BUILD + "&_=" + System.currentTimeMillis());
        final String dataserver = br.getRegex("\"url\":\"(https?://[a-z0-9]+\\.datacloudmail\\.ru/weblink/)view/\"").getMatch(0);
        long totalSize = 0;
        for (final String singleinfo : links) {
            if ("folder".equals(getJson(singleinfo, "kind"))) {
                String folder_url = getJson(singleinfo, "weblink");
                if (folder_url == null) {
                    logger.warning("Decrypter broken for link: " + PARAMETER);
                    return null;
                }
                folder_url = Encoding.htmlDecode(folder_url);
                folder_url = "https://cloud.mail.ru/public/" + folder_url;
                decryptedLinks.add(createDownloadlink(folder_url));
            } else {
                String browserurl;
                final DownloadLink dl = createDownloadlink("http://clouddecrypted.mail.ru/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filesize = getJson(singleinfo, "size");
                String filename = getJson(singleinfo, "name");
                if (filesize == null || filename == null) {
                    logger.warning("Decrypter broken for link: " + PARAMETER);
                    return null;
                }
                final String unique_id = id + "/" + filename;
                if (dataserver != null) {
                    final String directlink = "https://cloclo20.datacloudmail.ru/weblink/get/" + unique_id + "?x-email=undefined";
                    dl.setProperty("plain_directlink", directlink);
                }
                filename = Encoding.htmlDecode(filename.trim());
                final long cursize = Long.parseLong(filesize);
                dl.setDownloadSize(cursize);
                totalSize += cursize;
                dl.setFinalFileName(filename);
                dl.setProperty("plain_name", filename);
                dl.setProperty("plain_size", filesize);
                dl.setProperty("mainlink", PARAMETER);
                dl.setProperty("plain_request_id", id);
                dl.setProperty("unique_id", unique_id);
                /** TODO: Remove this */
                if (PARAMETER.matches(TYPE_APIV2)) {
                    dl.setProperty("noapi", true);
                }
                browserurl = "https://cloud.mail.ru/public/" + id + "/" + filename;
                dl.setProperty("browser_url", browserurl);
                try {
                    dl.setContentUrl(browserurl);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                    dl.setBrowserUrl(browserurl);
                }
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        if (decryptedLinks.size() > 1 && totalSize <= MAX_ZIP_FILESIZE * 1024 && SubConfiguration.getConfig("cloud.mail.ru").getBooleanProperty(DOWNLOAD_ZIP, false)) {
            /* = all files (links) of the folder as .zip archive */
            final String main_name = fpName + ".zip";
            main.setFinalFileName(fpName);
            main.setProperty("plain_name", main_name);
            main.setProperty("plain_size", Long.toString(totalSize));
            main.setProperty("complete_folder", true);
            decryptedLinks.add(main);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String[] getList(String id) {
        /* id is not (yet) needed in the RegEx below */
        if (id.endsWith("/")) {
            id = id.substring(0, id.length() - 1);
        }
        String[] lists;
        String[] links;
        if (PARAMETER.matches(TYPE_APIV2)) {
            lists = new Regex(json, "\"list\":([\t\n\r ]+)?\\[(.*?)\\]").getColumn(1);
            links = lists[lists.length - 1].split("\\},\\{");
        } else {
            lists = new Regex(json, "\"list\":\\[\\{(.*?\\})\\]").getColumn(0);
            links = lists[lists.length - 1].split("\\},\\{");
        }
        if (links == null || links.length == 0) {
            return null;
        }
        return links;
    }

    private String getJson(final String source, final String PARAMETER) {
        String result = new Regex(source, "\"" + PARAMETER + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + PARAMETER + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

}
