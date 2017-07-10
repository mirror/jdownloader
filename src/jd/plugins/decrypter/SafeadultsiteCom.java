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
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "safeadultsite.com", "eroticretina.com" }, urls = { "https?://(?:www\\.)?safeadultsite\\.com/(?:pic|video)/.*?\\.html", "https?://(?:www\\.)?eroticretina\\.com/pic/.*?\\.html" })
public class SafeadultsiteCom extends PornEmbedParser {

    public SafeadultsiteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (parameter.contains("/video/")) {
            cleanup();
            // lets try and find a nice filename
            String title = br.getRegex("<title>(.*?)\\s+\\|\\s+").getMatch(0);
            if (title == null) {
                title = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            }
            decryptedLinks.addAll(findEmbedUrls(title));
        } else {
            String fpName = br.getRegex("<H1>(.*?)</H1>").getMatch(0);
            final String html_gallery = br.getRegex("<div class=\"gallery\">(.*?)</div>").getMatch(0);
            final String[] thumbnails = new Regex(html_gallery, "decodeURIComponent\\(\\'([^<>\"\\']+)\\'").getColumn(0);
            if (thumbnails == null || thumbnails.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String full_url_host;
            if (br.getHost().equalsIgnoreCase("safeadultsite.com")) {
                full_url_host = "saspic";
            } else {
                full_url_host = "erpic";
            }
            for (String singleThumbnail : thumbnails) {
                singleThumbnail = Encoding.htmlDecode(singleThumbnail);
                final String urlpart = new Regex(singleThumbnail, "/\\d+x\\d+/(.+)").getMatch(0);
                if (urlpart == null) {
                    continue;
                }
                final String finallink = String.format("https://%s.xxxssl.com/pics/%s", full_url_host, urlpart);
                final DownloadLink dl = createDownloadlink(finallink);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private void cleanup() {
        String notsoclean = br.toString();
        final HashSet<String> dupe = new HashSet<String>();
        final String[] u = br.getRegex("decodeURIComponent\\('(.*?)'\\)").getColumn(0);
        if (u != null && u.length > 0) {
            for (final String b : u) {
                if (dupe.add(b)) {
                    final String suck = Encoding.urlDecode(b, false);
                    notsoclean = notsoclean.replace("decodeURIComponent('" + b + "')", suck);
                }
            }
        }
        br.getRequest().setHtmlCode(notsoclean);
    }
}
