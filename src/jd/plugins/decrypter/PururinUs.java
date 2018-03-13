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
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 * @author raztoki, psp
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pururin.us" }, urls = { "https?://(?:www\\.)?pururin\\.us/gallery/\\d+/[a-z0-9\\-]+\\.html" })
public class PururinUs extends antiDDoSForDecrypt {
    public PururinUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        // the uid can be determined by redirect after first page get. http://svn.jdownloader.org/issues/45635
        // http://pururin.com/gallery/55/alice-in-sexland.html -> http://pururin.com/gallery/12159/alice-first.html
        final String uid = new Regex(parameter, "/gallery/(\\d+)/").getMatch(0);
        final String url_name = new Regex(parameter, "([^/]+)\\.html").getMatch(0);
        if (uid == null) {
            logger.warning("Plugin Defect 'uid' == null");
            return null;
        }
        // correct cryptedUrl
        param.setCryptedUrl(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Page not found|This gallery was deleted\\.")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("<h1>Pururin is under maintenance</h1>")) {
            logger.info("Pururin is under maintenance");
            return decryptedLinks;
        }
        /* Sometimes a gallery is downloadable as a single file && this is on the gallery page */
        String downloadlink = br.getRegex("class=\"btn btn\\-download\" href=\"([^\"]+)\"").getMatch(0);
        if (downloadlink != null) {
            if (downloadlink.matches("/download/\\d+/.+")) {
                downloadlink = new Regex(br.getURL(), "https?://[^/]+").getMatch(-1) + downloadlink;
            } // self hosted content (goes to its own plugin), or hoster links
            decryptedLinks.add(createDownloadlink(downloadlink));
        }
        String fpName = br.getRegex("class=\"otitle\"[^>]*?>([^<>]+)<").getMatch(0);
        if (fpName == null) {
            fpName = url_name;
        }
        // they only seem to show 12 max on this page. we need to either open reader to get max page or parse total pages count and
        // construct url.
        String pages_string = br.getRegex("<li>(\\d+) Pages</li>").getMatch(0);
        if (pages_string == null) {
            pages_string = br.getRegex("<td>(\\d+)</td>\\s*</tr>\\s*</tbody>").getMatch(0);
            if (pages_string == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final int pages_int = Integer.parseInt(pages_string);
        // /view/chapternumber/pagenumber/.*?\.html ?
        String link = br.getRegex("\"(/view/\\d+/\\d+/[a-z0-9\\-_]+\\.html)\"").getMatch(0);
        if (link == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        link = Request.getLocation(link, br.getRequest()).replaceFirst("(/\\d+)(/[a-z0-9\\-_]+\\.html)", "/" + "*CHANGEME*" + "$2");
        DecimalFormat df_links = new DecimalFormat("00");
        if (pages_int > 999) {
            df_links = new DecimalFormat("0000");
        } else if (pages_int > 99) {
            df_links = new DecimalFormat("000");
        }
        // for rename
        for (int i = 1; i <= pages_int; i++) {
            final DownloadLink dl = createDownloadlink(link.replaceFirst("\\*CHANGEME\\*", i + ""));
            dl.setAvailable(true);
            dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
            final String[] fn = new Regex(dl.getDownloadURL(), "/(\\d+)/(\\d+)/([^/]+)\\.html$").getRow(0);
            // not final as this hasn't been confirmed.
            dl.setName(fn[0] + "_" + df_links.format(Integer.parseInt(fn[1])) + "_" + fn[2]);
            dl.setProperty("links_length", pages_string);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    protected void getPage(final String parameter) throws Exception {
        super.getPage(parameter);
    }
}
