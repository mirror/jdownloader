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
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloud.mail.ru" }, urls = { "https?://(www\\.)?cloud\\.mail\\.ru/public/[a-z0-9]+/[^<>\"/]+(/[^<>\"/]+)?" }, flags = { 0 })
public class CloudMailRuDecrypter extends PluginForDecrypt {

    public CloudMailRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String  BUILD            = "hotfix-17-7.201403131547";
    private static final String TYPE_SINGLE_FILE = "https?://(www\\.)?cloud\\.mail\\.ru/public/[a-z0-9]+/[^<>\"/]+/[^<>\"/]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        final String id = new Regex(parameter, "cloud\\.mail\\.ru/public/([a-z0-9]+/[^<>\"/]+)").getMatch(0);
        final DownloadLink main = createDownloadlink("http://clouddecrypted.mail.ru/" + System.currentTimeMillis() + new Random().nextInt(100000));
        main.setProperty("plain_request_id", id);
        main.setProperty("mainlink", parameter);
        main.setName(new Regex(parameter, "public/[a-z0-9]+/(.+)").getMatch(0));

        prepBR();
        br.getPage("https://cloud.mail.ru/api/v1/folder/recursive?storage=public&id=" + id + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&api=1&htmlencoded=false&build=" + BUILD);
        if (br.containsHTML("\"status\":400")) {
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        // br.getPage(parameter);

        String folderName = br.getRegex("\"url\":.*?\\},\"name\":\"([^<>\"]*?)\",\"id").getMatch(0);
        if (folderName == null) folderName = new Regex(parameter, "public/([a-z0-9]+)/").getMatch(0);
        folderName = Encoding.htmlDecode(folderName.trim());

        final String[] links = getList();
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (parameter.matches(TYPE_SINGLE_FILE)) {
            final String targetName = Encoding.htmlDecode(new Regex(parameter, "cloud\\.mail\\.ru/public/[a-z0-9]+/[^<>\"/]+/(.+)").getMatch(0));
            for (final String singleinfo : links) {
                final String filesize = getJson("size", singleinfo);
                String filename = getJson("name", singleinfo);
                final String directlink = getJson("get", singleinfo);
                final String view = getJson("view", singleinfo);
                final String mimetype = getJson("mimetype", singleinfo);
                final String kind = getJson("kind", singleinfo);
                if ("folder".equals(kind)) continue;
                if (filesize == null || filename == null || directlink == null || view == null || mimetype == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                filename = Encoding.htmlDecode(filename.trim());
                String ext = null;
                if (directlink.contains(".")) ext = directlink.substring(directlink.lastIndexOf("."));
                if (ext == null || ext.length() > 5) {
                    if (mimetype.equals("image/jpeg")) ext = ".jpg";
                }
                if (ext != null && ext.length() <= 5 && !filename.endsWith(ext)) filename += ext;
                if (filename.equals(targetName)) {
                    final DownloadLink dl = createDownloadlink("http://clouddecrypted.mail.ru/" + System.currentTimeMillis() + new Random().nextInt(100000));
                    final long cursize = Long.parseLong(filesize);
                    dl.setDownloadSize(cursize);
                    dl.setFinalFileName(filename);
                    dl.setProperty("plain_name", filename);
                    dl.setProperty("plain_size", filesize);
                    dl.setProperty("mainlink", parameter);
                    dl.setProperty("plain_view", view);
                    dl.setProperty("plain_directlink", directlink);
                    dl.setProperty("plain_request_id", id);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    break;
                }
            }
            if (decryptedLinks.size() == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        } else {
            long totalSize = 0;
            for (final String singleinfo : links) {
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

            if (decryptedLinks.size() > 1) {
                /* = all files (links) of the folder as .zip archive */
                final String main_name = folderName + ".zip";
                main.setFinalFileName(folderName);
                main.setProperty("plain_name", main_name);
                main.setProperty("plain_size", Long.toString(totalSize));
                main.setProperty("complete_folder", true);
                decryptedLinks.add(main);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderName);
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
