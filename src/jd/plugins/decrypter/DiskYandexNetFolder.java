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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

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
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "disk.yandex.net", "docviewer.yandex.com" }, urls = { "https?://(?:www\\.)?(((((mail|disk)\\.)?yandex\\.(?:net|com|com\\.tr|ru|ua)|yadi\\.sk)/(disk/)?public/(\\?hash=.+|#.+))|(?:yadi\\.sk|yadisk\\.cc)/(?:d|i)/[A-Za-z0-9\\-_]+(/[^/]+){0,}|yadi\\.sk/mail/\\?hash=.+)", "https?://docviewer\\.yandex\\.(?:net|com|com\\.tr|ru|ua)/\\?url=ya\\-disk\\-public%3A%2F%2F.+" })
public class DiskYandexNetFolder extends PluginForDecrypt {

    public DiskYandexNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_docviewer    = "https?://docviewer\\.yandex\\.[^/]+/\\?url=ya\\-disk\\-public%3A%2F%2F([^/\"\\&]+).*?";
    private final String        type_primaryURLs  = "https?://(?:www\\.)?(((mail|disk)\\.)?yandex\\.(net|com|com\\.tr|ru|ua)|yadi\\.sk)/(disk/)?public/(\\?hash=.+|#.+)";
    private final String        type_shortURLs_d  = "https?://(?:www\\.)?(yadi\\.sk|yadisk\\.cc)/d/[A-Za-z0-9\\-_]+(/[^/]+){0,}";
    private final String        type_shortURLs_i  = "https?://(?:www\\.)?(yadi\\.sk|yadisk\\.cc)/i/[A-Za-z0-9\\-_]+";

    private final String        type_yadi_sk_mail = "https?://(www\\.)?yadi\\.sk/mail/\\?hash=.+";

    private final String        DOWNLOAD_ZIP      = "DOWNLOAD_ZIP_2";

    private static final String OFFLINE_TEXT      = "class=\"not\\-found\\-public__caption\"|_file\\-blocked\"|A complaint was received regarding this file|>File blocked<";
    private static final String JSON_TYPE_DIR     = "dir";

    /** Using API: https://tech.yandex.ru/disk/api/reference/public-docpage/ */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        jd.plugins.hoster.DiskYandexNet.prepbrAPI(this.br);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String hash_decoded = null;
        String fname_url = null;
        String fpName = null;
        String mainhashID = null;
        String path_main = new Regex(parameter, type_shortURLs_d).getMatch(1);
        if (path_main == null) {
            path_main = "/";
        }
        if (parameter.matches(type_docviewer)) {
            /* TODO: Change that --> FILE-URLs --> Should work fine then with the fixed decrypter! */
            /* Documents in web view mode --> File-URLs! */
            /* First lets fix broken URLs by removing unneeded parameters ... */
            final String remove = new Regex(parameter, "(\\&[a-z0-9]+=.+)").getMatch(0);
            if (remove != null) {
                parameter = parameter.replace(remove, "");
            }
            mainhashID = new Regex(parameter, type_docviewer).getMatch(0);
            mainhashID = new Regex(parameter, "url=ya\\-disk\\-public%3A%2F%2F(.+)").getMatch(0);
            String hash_temp_decoded = Encoding.htmlDecode(mainhashID);
            fname_url = new Regex(parameter, "\\&name=([^/\\&]+)").getMatch(0);
            if (fname_url == null) {
                fname_url = new Regex(hash_temp_decoded, ":/([^/]+)$").getMatch(0);
            }
            fname_url = Encoding.htmlDecode(fname_url);
        } else if (parameter.matches(type_yadi_sk_mail)) {
            mainhashID = regexHashFromURL(parameter);
            parameter = "https://disk.yandex.com/public/?hash=" + mainhashID;
        } else {
            parameter = parameter.replace("#", "?hash=");
            mainhashID = regexHashFromURL(parameter);
        }
        boolean parameter_correct = false;
        final DownloadLink main = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
        if (parameter.matches(type_shortURLs_d) || parameter.matches(type_shortURLs_i)) {
            getPage(parameter);
            if (br.containsHTML(OFFLINE_TEXT)) {
                main.setAvailable(false);
                main.setProperty("offline", true);
                main.setFinalFileName(new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0));
                decryptedLinks.add(main);
                return decryptedLinks;
            }
            mainhashID = PluginJSonUtils.getJsonValue(br, "hash");
            if (mainhashID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            parameter = "https://disk.yandex.com/public/?hash=" + Encoding.urlEncode(mainhashID);
            parameter_correct = true;
        }
        hash_decoded = Encoding.htmlDecode(mainhashID);
        if (hash_decoded.contains(":/")) {
            final Regex hashregex = new Regex(hash_decoded, "(.*?):(/.+)");
            mainhashID = hashregex.getMatch(0);
            path_main = hashregex.getMatch(1);
        }
        if (!parameter_correct) {
            parameter = "https://disk.yandex.com/public/?hash=" + mainhashID;
        }

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
                return decryptedLinks;
            }
            getPage("https://cloud-api.yandex.net/v1/disk/public/resources?limit=" + entries_per_request + "&offset=" + offset + "&public_key=" + Encoding.urlEncode(mainhashID) + "&path=" + Encoding.urlEncode(path_main));
            if (PluginJSonUtils.getJsonValue(br, "error") != null) {
                main.setAvailable(false);
                main.setProperty("offline", true);
                main.setFinalFileName(mainhashID);
                decryptedLinks.add(main);
                return decryptedLinks;
            }

            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            final String type_main = (String) entries.get("type");

            if (!type_main.equals(JSON_TYPE_DIR)) {
                /* We only have a single file --> Add to downloadliste / host plugin */
                final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                if (jd.plugins.hoster.DiskYandexNet.apiAvailablecheckIsOffline(this.br)) {
                    dl.setAvailable(false);
                    dl.setProperty("offline", true);
                    dl.setFinalFileName(mainhashID);
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
                decryptSingleFile(dl, entries);
                dl.setProperty("mainlink", parameter);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }

            final String walk_string = "_embedded/items";
            final ArrayList<Object> resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(entries, walk_string);

            if (offset == 0) {
                /* Set total number of entries on first loop. */
                numberof_entries = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "_embedded/total"), 0);
                fpName = (String) entries.get("name");
                if (inValidate(fpName)) {
                    /* Maybe our folder has no name. */
                    fpName = mainhashID;
                }
                fp.setName(fpName);
            }

            main.setProperty("hash_main", mainhashID);
            mainhashID = Encoding.htmlDecode(mainhashID);

            for (final Object list_object : resource_data_list) {
                entries = (LinkedHashMap<String, Object>) list_object;

                final String type = (String) entries.get("type");
                final String hash = (String) entries.get("public_key");
                final String path = (String) entries.get("path");
                final String md5 = (String) entries.get("md5");
                final String url_preview = (String) entries.get("preview");
                if (type == null || path == null) {
                    return null;
                }
                String name = (String) entries.get("name");
                if (type.equals(JSON_TYPE_DIR)) {
                    /* Subfolders go back into our decrypter! */
                    final String folderlink = "https://disk.yandex.com/public/?hash=" + Encoding.urlEncode(hash) + "%3A" + Encoding.urlEncode(path);
                    decryptedLinks.add(createDownloadlink(folderlink));
                } else {
                    if (name == null || hash == null) {
                        return null;
                    }
                    final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                    decryptSingleFile(dl, entries);
                    final String url_content;
                    if (url_preview != null) {
                        /*
                         * Given preview URLs are bullshit as trhey only e.g. link to a thumbnail of a .pdf file - but we know how to build
                         * good "open in browser" content URLs ...
                         */
                        url_content = "https://docviewer.yandex.com/?url=ya-disk-public%3A%2F%2F" + Encoding.urlEncode(hash) + "%3A" + Encoding.urlEncode(path);
                    } else {
                        /*
                         * We do not have any URL - set main URL.
                         */
                        url_content = "https://disk.yandex.com/public/?hash=" + Encoding.urlEncode(hash);
                    }

                    dl.setProperty("hash_main", hash);
                    dl.setProperty("mainlink", url_content);

                    if (md5 != null) {
                        /* md5 hash is usually given */
                        dl.setMD5Hash(md5);
                    }
                    dl.setProperty("is_part_of_a_folder", true);
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
        if (decryptedLinks.size() == 0) {
            /* Should never happen! */
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        /* Only add main .zip link if the user added the ROOT link, otherwise we get the ROOT as .zip anyways which makes no sense. */
        final boolean is_root_folder = path_main.equals("/");
        if (is_root_folder && SubConfiguration.getConfig("disk.yandex.net").getBooleanProperty(DOWNLOAD_ZIP, false)) {
            /* User wants a .zip file of the complete (sub) folder --> Do that */
            main.setFinalFileName(fpName + ".zip");
            main.setProperty("plain_filename", fpName + ".zip");
            main.setProperty("path", path_main);
            main.setProperty("is_zipped_folder", true);
            main.setContentUrl(parameter);
            main.setLinkID(mainhashID + path_main);
            main.setDownloadSize(filesize_total);
            main.setAvailable(true);
            decryptedLinks.add(main);
        }

        return decryptedLinks;
    }

    private String regexHashFromURL(final String url) {
        return new Regex(url, "hash=([A-Za-z0-9=%\\+]+)").getMatch(0);
    }

    private void decryptSingleFile(final DownloadLink dl, final LinkedHashMap<String, Object> entries) throws Exception {
        final AvailableStatus status = jd.plugins.hoster.DiskYandexNet.parseInformationAPIAvailablecheck(this, dl, entries);
        dl.setAvailableStatus(status);
    }

    private void getPage(final String url) throws IOException {
        jd.plugins.hoster.DiskYandexNet.getPage(this.br, url);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    private boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

}
