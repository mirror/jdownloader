//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.HashMap;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abc.net.au" }, urls = { "https?://(?:www\\.)?abc\\.net\\.au/news/\\d{4}-\\d{2}-\\d{2}/[^/]+/\\d+" }) 
public class AbcNtAu extends antiDDoSForDecrypt {

    public AbcNtAu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String[][] results = br.getRegex("inline(Video|Audio)Data\\.push\\((.*?)\\);").getMatches();
        if (results != null) {
            for (final String[] result : results) {
                final HashMap<Integer, DownloadLink> abc = new HashMap<Integer, DownloadLink>();
                String urlPattern = null;
                final ArrayList<Integer> qual = new ArrayList<Integer>();
                String[] result_array = PluginJSonUtils.getJsonResultsFromArray(result[1]);
                if (result_array != null) {
                    for (final String results_a : result_array) {
                        final String url = PluginJSonUtils.getJsonValue(results_a, "url");
                        // if video
                        final boolean isVideo = "Video".equalsIgnoreCase(result[0]);
                        final String q = new Regex(url, "(\\d+)k\\.mp4").getMatch(0);
                        if (isVideo && q != null) {
                            // get qual
                            qual.add(Integer.parseInt(q));
                            if (urlPattern == null) {
                                urlPattern = url;
                            }
                        }
                        final String size = PluginJSonUtils.getJsonValue(results_a, "fileSize");
                        if (!inValidate(url)) {
                            final DownloadLink dl = createDownloadlink(url);
                            if (!inValidate(size)) {
                                dl.setVerifiedFileSize(Long.parseLong(size));
                            }
                            dl.setAvailableStatus(AvailableStatus.TRUE);
                            if (isVideo && q != null) {
                                abc.put(Integer.parseInt(q), dl);
                            } else {
                                decryptedLinks.add(dl);
                            }
                        }
                    }
                }
                // analyse the results, because they don't include all qualities at times.. yet they're available.
                if (urlPattern != null && !qual.isEmpty()) {
                    // 1000k, 512k, 256k,
                    final int[] k = new int[] { 1000, 512, 256 };
                    for (final int kk : k) {
                        if (!qual.contains(kk)) {
                            decryptedLinks.add(createDownloadlink(urlPattern.replaceFirst("\\d+(k\\.mp4)", kk + "$1")));
                        }
                    }
                }
                // only add best
                if (!abc.isEmpty()) {
                    final int[] k = new int[] { 1000, 512, 256 };
                    for (final int kk : k) {
                        if (abc.containsKey(kk)) {
                            decryptedLinks.add(abc.get(kk));
                            break;
                        }
                    }
                }

            }
        }
        // lets look for external links? youtube etc.
        final String[] externlinks = br.getRegex("<div class=\"inline-content (?:interactive )?(?:full|left|right)\">(.*?)</div>").getColumn(0);
        if (externlinks != null) {
            for (final String externlink : externlinks) {
                final String[] links = HTMLParser.getHttpLinks(externlink, null);
                for (final String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        }
        final String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (!inValidate(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}