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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hitomi.la" }, urls = { "https?://(?:www\\.)?hitomi\\.la/(?:galleries/\\d+\\.html|reader/\\d+\\.html|[^/]+/.*?-\\d+\\.html)" })
public class HitomiLa extends antiDDoSForDecrypt {
    public HitomiLa(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String gallery_id = new Regex(parameter, "/(?:galleries|reader)/(\\d+)").getMatch(0);
        if (gallery_id == null) {
            gallery_id = new Regex(parameter, "/[^/]+/.*?-(\\d+)\\.html").getMatch(0);
        }
        final String url_name = new Regex(parameter, "https?://(?:www\\.)?hitomi\\.la/(?:(?:galleries|reader)/)?(.*?)(?:-\\d+)?\\.html$").getMatch(0);
        if (gallery_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        String fpName = null;
        int i = 0;
        String imghost = getImageHost(gallery_id) + "a";
        int numberOfPages;
        final boolean use_Thumbnails = false;
        if (use_Thumbnails) {
            this.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String[] thumbnails = br.getRegex("<img src=\"//tn\\.hitomi\\.la/bigtn/(./\\d+/[a-f0-9]+)\\.jpg\">").getColumn(0);
            numberOfPages = thumbnails.length;
            final DecimalFormat df = numberOfPages > 999 ? new DecimalFormat("0000") : numberOfPages > 99 ? new DecimalFormat("000") : new DecimalFormat("00");
            for (final String thumbnail : thumbnails) {
                final DownloadLink dl = createDownloadlink("directhttp://https://" + imghost + ".hitomi.la/webp/" + thumbnail + ".webp");
                dl.setProperty("Referer", br.getURL());
                dl.setProperty("requestType", "GET");
                dl.setAvailable(true);
                dl.setFinalFileName(df.format(i) + ".webp");
                decryptedLinks.add(dl);
            }
        } else {
            getPage("https://hitomi.la/galleries/" + gallery_id + ".html");
            // this.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String extra_redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"\\d+;url=(http[^\"]+)\">").getMatch(0);
            if (extra_redirect != null) {
                this.getPage(extra_redirect);
            }
            fpName = br.getRegex("<title>([^<>\"]*?) \\| Hitomi\\.la</title>").getMatch(0);
            if (fpName == null) {
                /* Fallback */
                fpName = url_name;
            }
            // get the image host.
            // retval = subdomain_from_galleryid(g) + retval;
            final String js = br.getRegex("src\\s*=\\s*\"([^\"]+" + gallery_id + "\\.js)\"").getMatch(0);
            if (js == null) {
                return null;
            }
            final Browser brc = br.cloneBrowser();
            getPage(brc, js);
            LinkedHashMap<String, Object> entries = null;
            final Object picsO = JavaScriptEngineFactory.jsonToJavaObject(brc.toString().replace("var galleryinfo = ", ""));
            final ArrayList<Object> ressourcelist;
            if (picsO instanceof ArrayList) {
                ressourcelist = (ArrayList<Object>) picsO;
            } else {
                entries = (LinkedHashMap<String, Object>) picsO;
                ressourcelist = (ArrayList<Object>) entries.get("files");
            }
            numberOfPages = ressourcelist.size();
            final DecimalFormat df = numberOfPages > 999 ? new DecimalFormat("0000") : numberOfPages > 99 ? new DecimalFormat("000") : new DecimalFormat("00");
            // boolean checked = false;
            for (final Object picO : ressourcelist) {
                ++i;
                final Map<String, String> picInfo = (HashMap<String, String>) picO;
                boolean use_new_way = true;
                String url = null;
                String ext = null;
                if (use_new_way) {
                    url = "https:" + url_from_url_from_hash(gallery_id, picInfo, null, null, null);
                    ext = Plugin.getFileNameExtensionFromURL(url);
                } else {
                    entries = (LinkedHashMap<String, Object>) picO;
                    final String hash = (String) entries.get("hash");
                    final long haswebp = JavaScriptEngineFactory.toLong(entries.get("haswebp"), 1);
                    final String type;
                    if (haswebp == 1) {
                        type = "webp";
                        ext = ".webp";
                    } else {
                        type = "images";
                        ext = ".jpg";
                        imghost = "ba";
                    }
                    final String last_char_two = hash.substring(hash.length() - 3, hash.length() - 1);
                    final String last_char = hash.substring(hash.length() - 1);
                    url = String.format("https://%s.hitomi.la/%s/%s/%s/%s.%s", imghost, type, last_char, last_char_two, hash, ext);
                }
                // if (!checked) {
                // HeadRequest head = br.createHeadRequest(url);
                // URLConnectionAdapter con = br.cloneBrowser().openRequestConnection(head);
                // try {
                // if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "image")) {
                // checked = true;
                // } else {
                // con.disconnect();
                // head = br.createHeadRequest("https://0a.hitomi.la/galleries" + singleLink);
                // con = br.cloneBrowser().openRequestConnection(head);
                // if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "image")) {
                // checked = true;
                // imghost = "0a";
                // } else {
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                // }
                // }
                // } finally {
                // con.disconnect();
                // }
                // }
                final DownloadLink dl = createDownloadlink("directhttp://" + url);
                dl.setProperty("Referer", br.getURL());
                dl.setProperty("requestType", "GET");
                dl.setAvailable(true);
                dl.setFinalFileName(df.format(i) + ext);
                decryptedLinks.add(dl);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /**
     * they do some javascript trickery (check reader.js,common.js). rewritten in java.
     *
     * @param guid
     * @return
     * @throws DecrypterException
     */
    private String getImageHost(final String guid) throws DecrypterException {
        // number of subdmains, var number_of_frontends = 3;
        final int i = 3;
        // guid is always present, so not sure why they have failover. That said you don't need subdomain either base domain works also!
        String g = new Regex(guid, "^\\d*(\\d)$").getMatch(0);
        if (false && "1".equals(g)) {
            g = "0";
        }
        final String subdomain = Character.toString((char) (97 + (Integer.parseInt(g) % i)));
        return subdomain;
    }

    /* 2020-02-10: See also: https://board.jdownloader.org/showpost.php?p=457258&postcount=16 */
    /* ####################################################################################################################### */
    public static final Pattern SUBDOMAIN_FROM_URL_PATTERN  = Pattern.compile("/[0-9a-f]/([0-9a-f]{2})/");
    public static final Pattern URL_FROM_URL_PATTERN        = Pattern.compile("//..?\\.hitomi\\.la/");
    public static final Pattern FULL_PATH_FROM_HASH_PATTERN = Pattern.compile("^.*(..)(.)$");

    String subdomain_from_galleryid(int g, int number_of_frontends) {
        int o = g % number_of_frontends;
        return String.valueOf((char) (97 + o));
    }

    String subdomain_from_url(String url, String base) {
        String retval = "a";
        if (base != null) {
            retval = base;
        }
        int number_of_frontends = 3;
        Matcher m = SUBDOMAIN_FROM_URL_PATTERN.matcher(url);
        if (!m.find()) {
            return retval;
        }
        try {
            int g = Integer.parseInt(m.group(1), 16);
            if (g < 0x30) {
                number_of_frontends = 2;
            }
            if (g < 0x09) {
                g = 1;
            }
            retval = subdomain_from_galleryid(g, number_of_frontends) + retval;
        } catch (NumberFormatException ignore) {
        }
        return retval;
    }

    String url_from_url(String url, String base) {
        return URL_FROM_URL_PATTERN.matcher(url).replaceAll("//" + subdomain_from_url(url, base) + ".hitomi.la/");
    }

    String full_path_from_hash(String hash) {
        if (hash.length() < 3) {
            return hash;
        }
        return FULL_PATH_FROM_HASH_PATTERN.matcher(hash).replaceAll("$2/$1/" + hash);
    }

    String url_from_hash(String galleryid, Map<String, String> image, String dir, String ext) {
        ext = isNotBlank(ext) ? ext : (isNotBlank(dir) ? dir : image.get("name").split("\\.")[1]);
        dir = isNotBlank(dir) ? dir : "images";
        return "//a.hitomi.la/" + dir + '/' + full_path_from_hash(image.get("hash")) + '.' + ext;
    }

    String url_from_url_from_hash(String galleryid, Map<String, String> image, String dir, String ext, String base) {
        return url_from_url(url_from_hash(galleryid, image, dir, ext), base);
    }

    boolean isNotBlank(String str) {
        return str != null && !str.isEmpty();
    }
}
