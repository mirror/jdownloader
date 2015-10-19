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
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "disk.yandex.net", "docviewer.yandex.com" }, urls = { "https?://(?:www\\.)?((((mail|disk)\\.)?yandex\\.(?:net|com|com\\.tr|ru|ua)/(disk/)?public/(\\?hash=.+|#.+))|(?:yadi\\.sk|yadisk\\.cc)/(?:d|i)/[A-Za-z0-9\\-_]+|yadi\\.sk/mail/\\?hash=.+)", "https?://docviewer\\.yandex\\.(?:net|com|com\\.tr|ru|ua)/\\?url=ya\\-disk\\-public%3A%2F%2F.+" }, flags = { 0, 0 })
public class DiskYandexNetFolder extends PluginForDecrypt {

    public DiskYandexNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_docviewer    = "https?://docviewer\\.yandex\\.[^/]+/\\?url=ya\\-disk\\-public%3A%2F%2F([^/\"\\&]+).*?";
    private final String        type_primaryURLs  = "https?://(?:www\\.)?((mail|disk)\\.)?yandex\\.(net|com|com\\.tr|ru|ua)/(disk/)?public/(\\?hash=.+|#.+)";
    private final String        type_shortURLs_d  = "https?://(?:www\\.)?(yadi\\.sk|yadisk\\.cc)/d/[A-Za-z0-9\\-_]+";
    private final String        type_shortURLs_i  = "https?://(?:www\\.)?(yadi\\.sk|yadisk\\.cc)/i/[A-Za-z0-9\\-_]+";

    private final String        type_yadi_sk_mail = "https?://(www\\.)?yadi\\.sk/mail/\\?hash=.+";

    private final String        DOWNLOAD_ZIP      = "DOWNLOAD_ZIP_2";

    private static final String OFFLINE_TEXT      = "<title>The file you are looking for could not be found\\.|class=\"nb\\-panel__warning\"|>Nothing found</span>|<title>Nothing found \\— Yandex\\.Disk</title>|_file\\-blocked\"|/_not\\-found.css\"";
    private static final String JSON_TYPE_DIR     = "dir";

    /** Using API: https://tech.yandex.ru/disk/api/reference/public-docpage/ */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        jd.plugins.hoster.DiskYandexNet.prepbrAPI(this.br);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String hash_temp = null;
        String hash_complete = null;
        String fname_url = null;
        if (parameter.matches(type_docviewer)) {
            /* TODO: Change that --> FILE-URLs --> Should work fine then with the fixed decrypter! */
            /* Documents in web view mode --> File-URLs! */
            hash_temp = new Regex(parameter, type_docviewer).getMatch(0);
            hash_complete = new Regex(parameter, "url=ya\\-disk\\-public%3A%2F%2F(.+)").getMatch(0);
            String hash_temp_decoded = Encoding.htmlDecode(hash_temp);
            fname_url = new Regex(parameter, "\\&name=([^/\\&]+)").getMatch(0);
            if (fname_url == null) {
                fname_url = new Regex(hash_temp_decoded, ":/([^/]+)$").getMatch(0);
            }
            fname_url = Encoding.htmlDecode(fname_url);
        } else if (parameter.matches(type_yadi_sk_mail)) {
            hash_temp = new Regex(parameter, "hash=(.+)").getMatch(0);
            parameter = "https://disk.yandex.com/public/?hash=" + hash_temp;
        } else {
            parameter = parameter.replace("mail.yandex.ru/", "disk.yandex.net/").replace("#", "?hash=");
        }
        final DownloadLink main = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
        String fpName = null;
        String mainhashID = null;
        if (parameter.matches(type_shortURLs_d) || parameter.matches(type_shortURLs_i)) {
            br.getPage(parameter);
            if (br.containsHTML(OFFLINE_TEXT)) {
                main.setAvailable(false);
                main.setProperty("offline", true);
                main.setFinalFileName(new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0));
                decryptedLinks.add(main);
                return decryptedLinks;
            }
            mainhashID = getJson("hash");
            if (mainhashID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            parameter = "https://disk.yandex.com/public/?hash=" + Encoding.urlEncode(mainhashID);
        }
        mainhashID = new Regex(parameter, "hash=(.+)$").getMatch(0);
        mainhashID = fixHash(mainhashID);
        parameter = "https://disk.yandex.com/public/?hash=" + mainhashID;

        this.br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");

        main.setProperty("mainlink", parameter);
        main.setProperty("LINKDUPEID", "copydiskyandexcom" + mainhashID);
        main.setName(mainhashID);

        short offset = 0;
        final short entries_per_request = 200;
        long numberof_entries = 0;
        long filesize_total = 0;

        final FilePackage fp = FilePackage.getInstance();

        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break;
            }
            this.br.getPage("https://cloud-api.yandex.net/v1/disk/public/resources?limit=" + entries_per_request + "&offset=" + offset + "&public_key=" + Encoding.urlEncode(mainhashID));
            if (this.getJson("error") != null) {
                main.setAvailable(false);
                main.setProperty("offline", true);
                main.setFinalFileName(mainhashID);
                decryptedLinks.add(main);
                return decryptedLinks;
            }

            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(this.br.toString());
            final String type_main = (String) entries.get("type");

            if (!type_main.equals(JSON_TYPE_DIR)) {
                /* We only have a single URL */
                final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                if (jd.plugins.hoster.DiskYandexNet.apiAvailablecheckIsOffline(this.br)) {
                    dl.setAvailable(false);
                    dl.setProperty("offline", true);
                    dl.setFinalFileName("");
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
                decryptSingleFile(dl, entries);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }

            final String walk_string = "_embedded/items";
            final ArrayList<Object> resource_data_list = (ArrayList) DummyScriptEnginePlugin.walkJson(entries, walk_string);

            if (offset == 0) {
                /* Set total number of entries on first loop. */
                numberof_entries = DummyScriptEnginePlugin.toLong(DummyScriptEnginePlugin.walkJson(entries, "_embedded/total"), 0);
                fpName = (String) entries.get("name");
                if (fpName == null || fpName.equals("")) {
                    fpName = mainhashID;
                }
                fp.setName(fpName);
            }

            main.setProperty("hash_main", mainhashID);
            mainhashID = Encoding.htmlDecode(mainhashID);

            for (final Object list_object : resource_data_list) {
                entries = (LinkedHashMap<String, Object>) list_object;

                // final boolean hasPreview = entries.get("preview") != null;
                final String type = (String) entries.get("type");
                final String hash = (String) entries.get("public_key");
                final String path = (String) entries.get("path");
                final String md5 = (String) entries.get("md5");
                if (type == null || path == null) {
                    return null;
                }
                String name = (String) entries.get("name");
                if (type.equals(JSON_TYPE_DIR)) {
                    final String folderlink = "https://disk.yandex.com/public/?hash=" + hash + Encoding.urlEncode(path);
                    /** TODO: Implement decryption of subfolders ! */
                    // decryptedLinks.add(createDownloadlink(folderlink));
                } else {
                    // if (url_short == null && hasPreview) {
                    // /* Probably a document so we need to build the view-url */
                    // url_short = "https://docviewer.yandex.com/?url=ya-disk-public%3A%2F%2F" + Encoding.urlEncode(hash);
                    // }
                    if (name == null || hash == null) {
                        return null;
                    }
                    final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                    decryptSingleFile(dl, entries);
                    final String url_content = "https://disk.yandex.com/public/?hash=" + Encoding.urlEncode(hash) + "%3A" + Encoding.urlEncode(path);

                    dl.setProperty("hash_main", hash);
                    dl.setProperty("mainlink", url_content);

                    if (md5 != null) {
                        dl.setMD5Hash(md5);
                    }
                    dl.setContentUrl(url_content);
                    dl.setLinkID(hash + path);
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    filesize_total += dl.getDownloadSize();
                }
                offset++;
            }

            if (resource_data_list.size() < entries_per_request) {
                /* Fail safe */
                break;
            }

        } while (offset < numberof_entries);
        /* Only add main .zip link if the user added the ROOT link, otherwise we get the ROOT as .zip with a wrong filename */
        final boolean is_root_plus_zip = (!mainhashID.contains("/") && decryptedLinks.size() > 0);
        /* If we did not find any other links it's probably a single file. */
        final boolean is_single = (decryptedLinks.size() == 0);
        /* Empty folder --> Offline */
        if (is_single && numberof_entries == 0) {
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        } else if (is_single) {
            main.setFinalFileName(mainhashID);
            main.setProperty("plain_filename", mainhashID);
            String filesize = br.getRegex(">Size: ([^<>\"]*?)<br/").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex(">Size:</span>([^<>\"]*?)</div>").getMatch(0);
            }
            if (filesize != null) {
                filesize = fixFilesize(filesize);
                main.setDownloadSize(filesize_total);
                main.setProperty("plain_size", filesize);
            }
            main.setAvailable(true);
            decryptedLinks.add(main);
        } else if (is_root_plus_zip && SubConfiguration.getConfig("disk.yandex.net").getBooleanProperty(DOWNLOAD_ZIP, false)) {
            main.setFinalFileName(mainhashID + ".zip");
            main.setProperty("plain_filename", mainhashID + ".zip");
            main.setProperty("path", "/");
            main.setProperty("is_zipped_folder", true);
            main.setDownloadSize(filesize_total);
            main.setAvailable(true);
            decryptedLinks.add(main);
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        return decryptedLinks;
    }

    private void decryptSingleFile(final DownloadLink dl, final LinkedHashMap<String, Object> entries) throws Exception {
        final AvailableStatus status = jd.plugins.hoster.DiskYandexNet.parseInformationAPIAvailablecheck(dl, entries);
        dl.setAvailableStatus(status);
    }

    private static String fixHash(final String input) {
        /* First fully decode it */
        String hash = input;
        if (hash.contains("+") || hash.contains(" ")) {
            hash = Encoding.htmlDecode(hash);
            hash = hash.replace("+", "%2B");
            hash = hash.replace("=", "%3D");
            hash = hash.replace(" ", "-");
            hash = hash.replace("/", "%2F");
        } else {
            // hash = hash.replace("%20", "+");
            // hash = hash.replace("%2F", "_");
            // hash = hash.replace("/", "_");
        }
        return hash;
    }

    /* For multiple files in folders */
    private static String fixHashFileFolder(final String input) {
        /* First fully decode it */
        String hash = input;
        hash = hash.replace("=", "%3D");
        hash = hash.replace(":", "%3A");
        hash = hash.replace("/", "%2F");
        hash = hash.replace(" ", "+");
        return hash;
    }

    private String fixFilesize(String filesize) {
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replaceAll("(к|К)", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize + "b";
        return filesize;
    }

    private static AtomicBoolean yt_loaded = new AtomicBoolean(false);

    private String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (!yt_loaded.getAndSet(true)) {
            JDUtilities.getPluginForHost("youtube.com");
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

}
