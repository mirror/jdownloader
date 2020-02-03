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
import java.util.LinkedHashMap;

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
import jd.plugins.PluginException;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hitomi.la" }, urls = { "https?://(www\\.)?hitomi\\.la/(?:galleries/\\d+\\.html|reader/\\d+\\.html|[^/]+/.*?-\\d+\\.html)" })
public class HitomiLa extends antiDDoSForDecrypt {
    public HitomiLa(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String guid = new Regex(parameter, "/(?:galleries|reader)/(\\d+)").getMatch(0);
        if (guid == null) {
            guid = new Regex(parameter, "/[^/]+/.*?-(\\d+)\\.html").getMatch(0);
        }
        if (guid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        String fpName = null;
        int i = 0;
        String imghost = getImageHost(guid) + "a";
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
            /* Avoid https, prefer http */
            getPage("https://hitomi.la/reader/" + guid + ".html");
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            fpName = br.getRegex("<title>([^<>\"]*?) \\| Hitomi\\.la</title>").getMatch(0);
            // get the image host.
            // retval = subdomain_from_galleryid(g) + retval;
            final String js = br.getRegex("src\\s*=\\s*\"([^\"]+" + guid + "\\.js)\"").getMatch(0);
            if (js == null) {
                return null;
            }
            final Browser brc = br.cloneBrowser();
            getPage(brc, js);
            LinkedHashMap<String, Object> entries = null;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(brc.toString().replace("var galleryinfo = ", ""));
            numberOfPages = ressourcelist.size();
            final DecimalFormat df = numberOfPages > 999 ? new DecimalFormat("0000") : numberOfPages > 99 ? new DecimalFormat("000") : new DecimalFormat("00");
            // boolean checked = false;
            for (final Object picO : ressourcelist) {
                ++i;
                entries = (LinkedHashMap<String, Object>) picO;
                final String hash = (String) entries.get("hash");
                final long haswebp = JavaScriptEngineFactory.toLong(entries.get("haswebp"), 1);
                final String type;
                final String ext;
                if (haswebp == 1) {
                    type = "webp";
                    ext = "webp";
                } else {
                    type = "images";
                    ext = "jpg";
                    imghost = "ba";
                }
                final String last_char_two = hash.substring(hash.length() - 3, hash.length() - 1);
                final String last_char = hash.substring(hash.length() - 1);
                String url = String.format("https://%s.hitomi.la/%s/%s/%s/%s.%s", imghost, type, last_char, last_char_two, hash, ext);
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
                dl.setFinalFileName(df.format(i) + getFileNameExtensionFromString(hash, "." + ext));
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
}
