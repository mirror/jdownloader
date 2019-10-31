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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.HeadRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

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
        /* Avoid https, prefer http */
        getPage("https://hitomi.la/reader/" + guid + ".html");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>([^<>\"]*?) \\| Hitomi\\.la</title>").getMatch(0);
        // get the image host.
        // retval = subdomain_from_galleryid(g) + retval;
        String[] links = br.getRegex("(/" + guid + "/(?:[^<>\"]*?\\.[a-z]+)+)").getColumn(0);
        if (links == null || links.length == 0) {
            final String js = br.getRegex("src\\s*=\\s*\"([^\"]+" + guid + "\\.js)\"").getMatch(0);
            if (js != null) {
                final Browser brc = br.cloneBrowser();
                getPage(brc, js);
                final String names[] = brc.getRegex("\"name\"\\s*:\\s*\"(.*?)\"").getColumn(0);
                if (names != null && names.length > 0) {
                    final ArrayList<String> urls = new ArrayList<String>();
                    for (String name : names) {
                        urls.add("/" + guid + "/" + name);
                    }
                    links = urls.toArray(new String[0]);
                }
            }
        }
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final int numberOfPages = links.length;
        final DecimalFormat df = numberOfPages > 999 ? new DecimalFormat("0000") : numberOfPages > 99 ? new DecimalFormat("000") : new DecimalFormat("00");
        int i = 0;
        String imghost = getImageHost(guid) + "a";
        boolean checked = false;
        for (final String singleLink : links) {
            ++i;
            if (!checked) {
                HeadRequest head = br.createHeadRequest("https://" + imghost + ".hitomi.la/galleries" + singleLink);
                URLConnectionAdapter con = br.cloneBrowser().openRequestConnection(head);
                try {
                    if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "image")) {
                        checked = true;
                    } else {
                        con.disconnect();
                        head = br.createHeadRequest("https://0a.hitomi.la/galleries" + singleLink);
                        con = br.cloneBrowser().openRequestConnection(head);
                        if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "image")) {
                            checked = true;
                            imghost = "0a";
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                } finally {
                    con.disconnect();
                }
            }
            final DownloadLink dl = createDownloadlink("directhttp://https://" + imghost + ".hitomi.la/galleries" + singleLink);
            dl.setProperty("Referer", br.getURL());
            dl.setProperty("requestType", "GET");
            dl.setAvailable(true);
            dl.setFinalFileName(df.format(i) + getFileNameExtensionFromString(singleLink, ".jpg"));
            decryptedLinks.add(dl);
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
