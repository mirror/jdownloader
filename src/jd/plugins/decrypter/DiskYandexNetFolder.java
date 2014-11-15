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
import java.util.concurrent.atomic.AtomicBoolean;

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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "disk.yandex.net" }, urls = { "https?://(www\\.)?((((mail|disk)\\.)?yandex\\.(net|com|com\\.tr|ru|ua)/(disk/)?public/(\\?hash=[A-Za-z0-9%/\\+=\\-]+|#[A-Za-z0-9%\\/+=\\-]+))|(yadi\\.sk|yadisk\\.cc)/d/[A-Za-z0-9\\-_]+|yadi\\.sk/mail/\\?hash=[A-Za-z0-9%/\\+=]+)" }, flags = { 0 })
public class DiskYandexNetFolder extends PluginForDecrypt {

    public DiskYandexNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String        primaryURLs       = "https?://(www\\.)?((mail|disk)\\.)?yandex\\.(net|com|com\\.tr|ru|ua)/(disk/)?public/(\\?hash=[A-Za-z0-9%/\\+=\\-]+|#[A-Za-z0-9%\\/+=\\-]+)";
    private final String        shortURLs         = "https?://(www\\.)?(yadi\\.sk|yadisk\\.cc)/d/[A-Za-z0-9\\-_]+";

    private final String        type_yadi_sk_mail = "https?://(www\\.)?yadi\\.sk/mail/\\?hash=[A-Za-z0-9%/\\+=]+";

    private final String        DOWNLOAD_ZIP      = "DOWNLOAD_ZIP_2";

    private static final String OFFLINE_TEXT      = "<title>The file you are looking for could not be found\\.|class=\"nb\\-panel__warning\"|>Nothing found</span>|<title>Nothing found \\— Yandex\\.Disk</title>|_file\\-blocked\"|/_not\\-found.css\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(type_yadi_sk_mail)) {
            final String hash = new Regex(parameter, "hash=(.+)").getMatch(0);
            parameter = "https://disk.yandex.com/public/?hash=" + hash;
        } else {
            parameter = parameter.replace("mail.yandex.ru/", "disk.yandex.net/").replace("#", "?hash=");
        }
        final DownloadLink main = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
        String hashID = null;
        if (parameter.matches(shortURLs)) {
            br.getPage(parameter);
            if (br.containsHTML(OFFLINE_TEXT)) {
                main.setAvailable(false);
                main.setProperty("offline", true);
                main.setFinalFileName(new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0));
                decryptedLinks.add(main);
                return decryptedLinks;
            }
            String newUrl = Encoding.htmlDecode(br.getURL()).replace("&locale=ru", "");
            if (newUrl.matches(primaryURLs)) {
                parameter = new Regex(newUrl, primaryURLs).getMatch(-1);
            } else {
                /* URL has not changed - try to manually change it to our basic url format */
                hashID = br.getRegex("\"hash\":\"([^<>\"]*?)\"").getMatch(0);
                if (hashID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                hashID = fixHash(hashID);
                parameter = "https://disk.yandex.com/public/?hash=" + hashID;
            }
        }
        if (parameter.matches(primaryURLs)) {
            String protocol = new Regex(parameter, "(https?)://").getMatch(0);
            hashID = new Regex(parameter, "hash=(.+)$").getMatch(0);
            if (protocol == null || hashID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            hashID = fixHash(hashID);
            parameter = protocol + "://disk.yandex.com/public/?hash=" + hashID;
            br.getPage(parameter);
        }

        main.setProperty("mainlink", parameter);
        main.setName(hashID);

        if (br.containsHTML(OFFLINE_TEXT)) {
            main.setAvailable(false);
            main.setProperty("offline", true);
            main.setFinalFileName(hashID);
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("class=\"nb\\-panel__title\" title=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        // /* First try to get files of current folder */
        // final String hashID_special_encoded = hashID.replace("%2F", "/").replace("%28", "(").replace("%29", ")");
        // String linktext = br.getRegex("\"idEncoded\":\"/public/" + hashID_special_encoded + "parents\":\\[(.*?)\\]").getMatch(0);
        String linktext = br.getRegex("\"resources\":\\[(.*?)\\]\\}\\}").getMatch(0);
        if (linktext != null) {
            linktext = Encoding.htmlDecode(linktext);
        }
        String[] data = null;
        if (linktext != null) {
            data = linktext.split("\\},\\{");
        }

        hashID = Encoding.htmlDecode(hashID);
        // stored hash should not be urldecoded as this is what we need in host plugin
        main.setProperty("hash_plain", hashID);

        if (data != null && data.length != 0) {
            for (final String singleData : data) {
                final String type = getJson("type", singleData);
                String hash = getJson("idEncoded", singleData);
                String name = getJson("name", singleData);
                if (singleData.length() > 30 && hash != null) {
                    hash = unescape(hash);
                    if ("dir".equals(type)) {
                        hash = hash.replace("/public/", "");
                        hash = Encoding.urlEncode(hash);
                        /* Correct encoding, space issues */
                        hash = hash.replace("+", "%20");
                        final String folderlink = "https://disk.yandex.com/public/?hash=" + hash;
                        decryptedLinks.add(createDownloadlink(folderlink));
                    } else {
                        if (name == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        name = Encoding.htmlDecode(name.trim());
                        name = unescape(name);
                        final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                        dl.setFinalFileName(name);
                        final String filesize = new Regex(singleData, "class=\"file__size\">([^<>\"]*?)</span>").getMatch(0);
                        if (filesize != null) {
                            dl.setDownloadSize(SizeFormatter.getSize(filesize));
                            dl.setProperty("plain_size", filesize);
                        }
                        dl.setProperty("plain_filename", name);
                        dl.setProperty("hash_plain", hash);
                        dl.setProperty("mainlink", parameter);
                        dl.setAvailable(true);
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        /* Only add main .zip link if the user added the ROOT link, otherwise we get the ROOT as .zip with a wrong filename */
        final boolean is_root_plus_zip = (!hashID.contains("/") && decryptedLinks.size() > 0);
        /* If we did not find any other links it's probably a single link */
        final boolean is_single = (decryptedLinks.size() == 0);
        /* Empty folder --> Offline */
        if (is_single && br.containsHTML("class=\"file file_empty\">Empty folder")) {
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        } else if (is_single) {
            main.setFinalFileName(fpName);
            main.setProperty("plain_filename", fpName);
            String filesize = br.getRegex(">Size: ([^<>\"]*?)<br/").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex(">Size:</span>([^<>\"]*?)</div>").getMatch(0);
            }
            if (filesize != null) {
                filesize = fixFilesize(filesize);
                main.setDownloadSize(SizeFormatter.getSize(filesize));
                main.setProperty("plain_size", filesize);
            }
            main.setAvailable(true);
            decryptedLinks.add(main);
        } else if (is_root_plus_zip && SubConfiguration.getConfig("disk.yandex.net").getBooleanProperty(DOWNLOAD_ZIP, false)) {
            main.setFinalFileName(fpName + ".zip");
            main.setProperty("plain_filename", fpName + ".zip");
            main.setAvailable(true);
            decryptedLinks.add(main);
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String fixHash(final String input) {
        /* First fully decode it */
        String hashID = Encoding.htmlDecode(input);
        if (hashID.contains("+") || hashID.contains("/") || hashID.contains("/")) {
            hashID = Encoding.urlEncode(hashID);
        }
        /* Fix remaining encoding issues */
        hashID = hashID.replace("+", "%20");
        return hashID;
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

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([0-9\\.]+)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
        return result;
    }

}
