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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "samepage.io" }, urls = { "https://samepage\\.io/([a-z0-9]+/share/[a-z0-9]+|app/#(!|%21)/[a-z0-9]+/page\\-\\d+)" })
public class SamePageIoDecrypter extends PluginForDecrypt {

    public SamePageIoDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     DOWNLOAD_ZIP   = "DOWNLOAD_ZIP";

    private static final String     TYPE_APP       = "https://samepage\\.io/app/#\\!/[a-z0-9]+/page\\-[a-z0-9]+";
    private static final String     TYPE_SHARE     = "https://samepage\\.io/[a-z0-9]+/share/[a-z0-9]+";

    private String                  id_1           = null;
    private String                  id_2           = null;
    private long                    totalSize      = 0;

    private String                  parameter      = null;
    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        parameter = param.toString();
        prepBR();
        if (parameter.matches(TYPE_APP)) {
            id_1 = new Regex(parameter, "samepage\\.io/app/#\\!/([a-z0-9]+)/").getMatch(0);
            id_2 = new Regex(parameter, "samepage\\.io/app/#\\!/[a-z0-9]+/page\\-(\\d+)").getMatch(0);
        } else {
            id_1 = new Regex(parameter, "samepage\\.io/([a-z0-9]+)/share/([a-z0-9]+)").getMatch(0);
            if (id_1 == null) {
                /* We NEED that ID! */
                logger.info("Unsupported or offline url");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            br.getPage(parameter);
            id_2 = new Regex(br.getURL(), "samepage\\.io/app/#\\!/[a-z0-9]+/page\\-(\\d+)[A-Za-z0-9\\-_]+").getMatch(0);
            if (id_2 == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        parameter = "https://samepage.io/app/#!/" + id_1 + "/page-" + id_2;

        br.getPage("https://samepage.io/app/");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");

        br.postPageRaw("https://samepage.io/api/app/jsonrpc?method=Bootstrap.bootstrap", String.format("{\"id\":0,\"jsonrpc\":\"2.0\",\"method\":\"Bootstrap.bootstrap\",\"params\":{\"referral\":\"\",\"timezoneOffsetData\":{\"current\":\"2017-02-14T17:30:34+0100\",\"currentOffset\":3600,\"summer\":\"2017-08-01T17:30:34+0200\",\"summerOffset\":7200,\"winter\":\"2017-02-01T17:30:34+0100\",\"winterOffset\":3600},\"tenantId\":\"%s\",\"itemId\":\"%s\",\"branch\":\"production\"}}", id_1, id_2));
        final String bootstrapAction = PluginJSonUtils.getJsonValue(this.br, "bootstrapAction");
        final String token = br.getCookie("http://samepage.io", "TOKEN_WORKSPACE");
        if ("login".equalsIgnoreCase(bootstrapAction)) {
            logger.info("Login required");
            return decryptedLinks;
        } else if (token == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        // br.getHeaders().put("Referer", "https://samepage.io/app/");
        // br.getHeaders().put("X-Token", token);
        // br.getHeaders().put("Content-Type", "application/json; charset=UTF-8");
        /* Item not found */
        if (br.containsHTML("\"code\":6002")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        String folderName = null;

        processFilesKamikaze(JavaScriptEngineFactory.jsonToJavaMap(this.br.toString()));

        // if (decryptedLinks.size() > 1 && SubConfiguration.getConfig(this.getHost()).getBooleanProperty(DOWNLOAD_ZIP, false)) {
        // /* = all files (links) of the folder as .zip archive */
        // final DownloadLink main = createDownloadlink("http://samepagedecrypted.io/" + System.currentTimeMillis() + new
        // Random().nextInt(100000));
        // final String main_name = "all_files_" + folderName + ".zip";
        //
        // main.setProperty("mainlink", parameter);
        // main.setFinalFileName(folderName);
        // main.setProperty("plain_name", main_name);
        // main.setProperty("plain_size", Long.toString(totalSize));
        // /* 2017-02-14: What is this for?? */
        // main.setProperty("plain_fileid", null);
        // main.setProperty("plain_id_1", id_1);
        // main.setProperty("plain_id_2", id_2);
        // main.setProperty("complete_folder", true);
        // decryptedLinks.add(main);
        // }

        if (folderName != null) {
            folderName = Encoding.htmlDecode(folderName.trim());
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(folderName);
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private boolean processFile(final LinkedHashMap<String, Object> entries) {
        final String type = (String) entries.get("type");
        final long filesize = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "file/size"), 0);
        final String filename = (String) entries.get("name");
        final Object fileido = entries.get("id");
        final String fileid = fileido != null && fileido instanceof String ? (String) fileido : null;
        final String hash = (String) entries.get("hash");
        if (filename == null || fileid == null || !"file".equalsIgnoreCase(type)) {
            return false;
        }
        final DownloadLink dl = createDownloadlink("http://samepagedecrypted.io/" + System.currentTimeMillis() + new Random().nextInt(100000));
        dl.setDownloadSize(filesize);
        totalSize += filesize;
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
        dl.setContentUrl(parameter);
        decryptedLinks.add(dl);
        return true;
    }

    /**
     * Recursive function to crawl all files --> Easiest way as they have different jsons and they are always huge.
     *
     */
    @SuppressWarnings("unchecked")
    private void processFilesKamikaze(final Object jsono) throws DecrypterException {
        LinkedHashMap<String, Object> test;
        if (jsono instanceof LinkedHashMap) {
            test = (LinkedHashMap<String, Object>) jsono;
            if (!processFile(test)) {
                final Iterator<Entry<String, Object>> it = test.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Object> thisentry = it.next();
                    final Object mapObject = thisentry.getValue();
                    processFilesKamikaze(mapObject);
                }
            }
        } else if (jsono instanceof ArrayList) {
            ArrayList<Object> ressourcelist = (ArrayList<Object>) jsono;
            for (final Object listo : ressourcelist) {
                processFilesKamikaze(listo);
            }
        }
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
