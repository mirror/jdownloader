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

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rmz.cr" }, urls = { "https?://(?:www\\.)?(?:rapidmoviez\\.(?:com|eu|cr)|rmz\\.rezavn|rmz\\.cr)/release/[^/]+" })
public class RmzCr extends antiDDoSForDecrypt {
    public RmzCr(PluginWrapper wrapper) {
        super(wrapper);
    }

    // 2020-02-25: Old RegEx: https?://(?:www\\.)?rmz\\.cr/(?:release/)?(?!l/)[^/]+
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<div id=\"title_release_before_title\"></div>\\s*<h2>([^<>\"]+)<").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>RapidMoviez\\s+-\\s+([^<]+)</title>").getMatch(0);
        }
        final String[] covers = br.getRegex("(https?://[^/]+/data/images/movies/[^<>\"]+)\"").getColumn(0);
        if (covers.length > 0) {
            logger.info("Found covers");
            for (final String coverURL : covers) {
                final DownloadLink dl = createDownloadlink("directhttp://" + coverURL);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            logger.info("Failed to find any covers");
        }
        final boolean grabScreencaps = false;
        if (grabScreencaps) {
            final String screencapsHTML = br.getRegex("<div class=\"fullsize\">Click on the image to see full size</div>(.*?)</div>\\s+</div>").getMatch(0);
            final String screencaps[] = HTMLParser.getHttpLinks(screencapsHTML, "");
            if (screencaps != null) {
                logger.info("Found screencaps");
                for (final String link : screencaps) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            } else {
                logger.info("Failed to find screencaps");
            }
        }
        String linkBlock = br.getRegex("(<div id=\"(?:title_release_after_download_title|title_release_after_imdb)\">[^$]+<div id=\"title_release_after_links\">)").getMatch(0);
        if (linkBlock != null) {
            /* 2020-01-22 */
            String[] links = new Regex(linkBlock, "id=\"l\\d+\">([^<>\"]+)</pre>").getColumn(0);
            for (String link : links) {
                if (link.startsWith("/")) {
                    // link = br.getURL(link).toString();
                    /* 2020-01-22: Skip URLs which would lead to this crawler again */
                    continue;
                }
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty("ALLOW_MERGE", true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}