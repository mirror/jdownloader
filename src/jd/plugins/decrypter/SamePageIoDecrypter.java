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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "samepage.io" }, urls = { "https://samepage\\.io/([a-z0-9]+/share/[a-z0-9]+|app/#\\!/[a-z0-9]+/page\\-\\d+)" }, flags = { 0 })
public class SamePageIoDecrypter extends PluginForDecrypt {

    public SamePageIoDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DOWNLOAD_ZIP = "DOWNLOAD_ZIP";

    private static final String TYPE_APP     = "https://samepage\\.io/app/#\\!/[a-z0-9]+/page\\-[a-z0-9]+";
    private static final String TYPE_SHARE   = "https://samepage\\.io/[a-z0-9]+/share/[a-z0-9]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final DownloadLink main = createDownloadlink("http://samepagedecrypted.io/" + System.currentTimeMillis() + new Random().nextInt(100000));
        main.setProperty("mainlink", parameter);
        prepBR();
        String id_1;
        String id_2;
        if (parameter.matches(TYPE_APP)) {
            id_1 = new Regex(parameter, "samepage\\.io/app/#\\!/([a-z0-9]+)/").getMatch(0);
            id_2 = new Regex(parameter, "samepage\\.io/app/#\\!/[a-z0-9]+/page\\-(\\d+)").getMatch(0);
        } else {
            id_1 = new Regex(parameter, "samepage\\.io/([a-z0-9]+)/share/([a-z0-9]+)").getMatch(0);
            br.getPage(parameter);
            id_2 = new Regex(br.getURL(), "samepage\\.io/app/#\\!/[a-z0-9]+/page\\-(\\d+)[A-Za-z0-9\\-_]+").getMatch(0);
            if (id_2 == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }

        br.getPage("https://samepage.io/app/");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");

        br.postPageRaw("https://samepage.io/api/app/jsonrpc", "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\":\"Bootstrap.bootstrap\",\"params\":{\"tenantId\":\"" + id_1 + "\",\"itemId\":\"" + id_2 + "\"}}");
        final String apiVersion = getJson("apiVersion", br.toString());
        final String token = br.getCookie("http://samepage.io", "TOKEN_WORKSPACE");
        if (token == null || apiVersion == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        br.getHeaders().put("Referer", "https://samepage.io/app/");
        br.getHeaders().put("X-Token", token);
        br.getHeaders().put("Content-Type", "application/json; charset=UTF-8");
        br.postPageRaw("https://samepage.io/" + id_1 + "/server/data?method=Items.get", "{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"Items.get\",\"params\":{\"includeChildren\":-1,\"includeIamFollowing\":true,\"includeHasSubtree\":true,\"includeChain\":true,\"id\":\"" + id_2 + "\"},\"apiVersion\":\"" + apiVersion + "\"}");
        /* Item not found */
        if (br.containsHTML("\"code\":6002")) {
            main.setFinalFileName(new Regex(parameter, "samepage\\.io/(.+)").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        String folderName = br.getRegex("\"name\":\"([^<>\"]*?)\",\"layout\":").getMatch(0);
        String linktext = br.getRegex("\"value\":\\{\\},\"children\":\\[(.*?)\\],\"role\":\"Reader\",\"type\":\"LinkList\"").getMatch(0);
        final String user_id = br.getRegex("\"children\":\\[\\{\"id\":\"([a-z0-9]+)\"").getMatch(0);
        if (linktext == null || folderName == null || user_id == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        folderName = Encoding.htmlDecode(folderName.trim());

        final String[] links = linktext.split("\\},\\{");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        long totalSize = 0;
        for (final String singleinfo : links) {
            if (!singleinfo.contains("\"file\"")) {
                continue;
            }
            final DownloadLink dl = createDownloadlink("http://samepagedecrypted.io/" + System.currentTimeMillis() + new Random().nextInt(100000));
            final String filesize = getJson("size", singleinfo);
            String filename = getJson("name", singleinfo);
            final String fileid = getJson("id", singleinfo);
            final String hash = getJson("hash", singleinfo);
            if (filesize == null || filename == null || fileid == null) {
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
            if (hash != null) {
                dl.setProperty("plain_hash", hash);
                dl.setMD5Hash(hash);
            }
            dl.setProperty("mainlink", parameter);
            dl.setProperty("plain_fileid", fileid);
            dl.setProperty("plain_id_1", id_1);
            dl.setProperty("plain_id_2", id_2);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        if (decryptedLinks.size() > 1 && SubConfiguration.getConfig("samepage.io").getBooleanProperty(DOWNLOAD_ZIP, false)) {
            /* = all files (links) of the folder as .zip archive */
            final String main_name = "all_files_" + folderName + ".zip";
            main.setFinalFileName(folderName);
            main.setProperty("plain_name", main_name);
            main.setProperty("plain_size", Long.toString(totalSize));
            main.setProperty("plain_fileid", user_id);
            main.setProperty("plain_id_1", id_1);
            main.setProperty("plain_id_2", id_2);
            main.setProperty("complete_folder", true);
            decryptedLinks.add(main);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.getHeaders().put("Accept-Language", "de,en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

}
