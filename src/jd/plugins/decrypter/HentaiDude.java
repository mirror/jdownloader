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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40413 $", interfaceVersion = 3, names = { "hentaidude.com" }, urls = { "https?://(?:www\\.)?hentaidude\\.com/.*" })
public class HentaiDude extends antiDDoSForDecrypt {
    public HentaiDude(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<meta (?:name|property)=\"og:(?:title|description)\" content=[\"']([^<>\"]*?)(?: ?\\| Hentaidude.com)").getMatch(0);
        String[][] source = br.getRegex("action:[\r\n\t ]+'msv-get-sources',[\r\n\t ]+id:[\r\n\t ]+'([0-9]+)',[\r\n\t ]+nonce:[\r\n\t ]+'([0-9a-fA-F]+)'").getMatches();
        final PostRequest post = new PostRequest(br.getURL("/wp-admin/admin-ajax.php"));
        post.addVariable("action", "msv-get-sources");
        post.addVariable("id", source[0][0]);
        post.addVariable("nonce", source[0][1]);
        post.getHeaders().put("Origin", "https://hentaidude.com");
        post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
        String postResult = br.getPage(post);
        final String[] results = HTMLParser.getHttpLinks(postResult, null);
        for (String result : results) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(result)));
            if (result.matches("https?://cdn[0-9]+.hentaidude\\.com/index.*")) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(result);
                    final String contentType = con.getContentType();
                    if (!StringUtils.containsIgnoreCase(contentType, "video/mp4")) {
                        getPage(result);
                        String[][] detailResults = br.getRegex("(?:suburl|name=\"og:url\" content) ?= ?\"([^\"]+)\"").getMatches();
                        for (String[] detailResult : detailResults) {
                            decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(detailResult[0])));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
