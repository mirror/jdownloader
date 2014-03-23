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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloud.mail.ru" }, urls = { "https?://(www\\.)?cloud\\.mail\\.ru(/|%2F)public(/|%2F)[a-z0-9]+(/|%2F)[^<>\"/]+(/|%2F)([^<>\"]+)?" }, flags = { 0 })
public class CloudMailRuDecrypter extends PluginForDecrypt {

    public CloudMailRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String  BUILD            = "hotfix-17-7.201403131547";
    /* Max .zip filesize = 4 GB */
    private static final double MAX_ZIP_FILESIZE = 4194304;
    private static final String DOWNLOAD_ZIP     = "DOWNLOAD_ZIP";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = Encoding.htmlDecode(param.toString()).replace("http://", "https://");
        final String id = new Regex(parameter, "cloud\\.mail\\.ru/public/(.+)").getMatch(0);
        final DownloadLink main = createDownloadlink("http://clouddecrypted.mail.ru/" + System.currentTimeMillis() + new Random().nextInt(100000));
        main.setProperty("plain_request_id", id);
        main.setProperty("mainlink", parameter);
        main.setName(new Regex(parameter, "public/[a-z0-9]+/(.+)").getMatch(0));

        prepBR();
        br.getPage("https://cloud.mail.ru/api/v1/folder/recursive?storage=public&id=" + Encoding.urlEncode(id) + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&api=1&htmlencoded=false&build=" + BUILD);
        if (br.containsHTML("\"status\":400")) {
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        // br.getPage(parameter);

        String fpName = null;
        String mainName = br.getRegex("\"url\":.*?\\},\"name\":\"([^<>\"]*?)\",\"id").getMatch(0);
        if (mainName == null) mainName = new Regex(parameter, "public/([a-z0-9]+)/").getMatch(0);
        final String detailedName = new Regex(parameter, "([^<>\"/]+)/?$").getMatch(0);
        mainName = Encoding.htmlDecode(mainName.trim());
        if (detailedName != null) {
            fpName = mainName + " - " + detailedName;
        } else {
            fpName = mainName;
        }
        final String[] links = getList();
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        long totalSize = 0;
        for (final String singleinfo : links) {
            if ("folder".equals(getJson("kind", singleinfo))) {
                String folder_url = getJson("web", singleinfo);
                if (folder_url == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                folder_url = "https://cloud.mail.ru" + Encoding.htmlDecode(folder_url);
                decryptedLinks.add(createDownloadlink(folder_url));
            } else {
                final DownloadLink dl = createDownloadlink("http://clouddecrypted.mail.ru/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filesize = getJson("size", singleinfo);
                String filename = getJson("name", singleinfo);
                final String directlink = getJson("get", singleinfo);
                final String view = getJson("view", singleinfo);
                if (filesize == null || filename == null || directlink == null || view == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                filename = Encoding.htmlDecode(filename.trim());
                final long cursize = Long.parseLong(filesize);
                dl.setDownloadSize(cursize);
                totalSize += cursize;
                dl.setFinalFileName(filename);
                dl.setProperty("plain_name", filename);
                dl.setProperty("plain_size", filesize);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("plain_view", view);
                dl.setProperty("plain_directlink", directlink);
                dl.setProperty("plain_request_id", id);
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

    private String[] getList() {
        String[] lists = br.getRegex("\"list\":\\[(.*?)\\]").getColumn(0);
        if (lists == null) return null;
        final String linktext = lists[lists.length - 1];

        final String[] links = linktext.split("\\},\\{");
        if (links == null || links.length == 0) { return null; }
        return links;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        return result;
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

}
