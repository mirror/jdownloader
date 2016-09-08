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

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "webtoons.com" }, urls = { "https?://(?:www\\.)?webtoons\\.com/[a-z]{2}/[^/]+/[^/]+/(?:[^/]+/viewer\\?title_no=\\d+\\&episode_no=\\d+|list\\?title_no=\\d+)" })
public class WebtoonsCom extends PluginForDecrypt {

    public WebtoonsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setAllowedResponseCodes(400);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String titlenumber = new Regex(parameter, "title_no=(\\d+)").getMatch(0);
        final String episodenumber = new Regex(parameter, "episode_no=(\\d+)").getMatch(0);
        String fpName = br.getRegex("<title>([^<>]+)</title>").getMatch(0);
        String[] links;
        if (episodenumber != null) {
            /* Decrypt single episode */
            links = br.getRegex("class=\"_images\" data\\-url=\"(http[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DecimalFormat df = new DecimalFormat("0000");
            int counter = 0;
            for (final String singleLink : links) {
                counter++;
                final DownloadLink dl = createDownloadlink("directhttp://" + singleLink);
                String name = getFileNameFromURL(new URL(singleLink));
                if (name == null) {
                    name = ".jpg";
                }
                name = df.format(counter) + "_" + name;
                dl.setAvailable(true);
                dl.setFinalFileName(name);
                decryptedLinks.add(dl);
            }
        } else {
            int maxpage = 1;
            int pagetemp = 0;
            final String[] pages = this.br.getRegex(titlenumber + "\\&page=(\\d+)").getColumn(0);
            for (final String page_str : pages) {
                pagetemp = Integer.parseInt(page_str);
                if (pagetemp > maxpage) {
                    maxpage = pagetemp;
                }
            }
            for (int currpage = 1; currpage <= maxpage; currpage++) {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                if (currpage > 1) {
                    this.br.getPage(parameter + "&page=" + currpage);
                }
                /* Find urls of all episode of a title --> Re-Add these single episodes to the crawler. */
                links = br.getRegex("<li id=\"episode_\\d+\">[^<>]*?<a href=\"(https?://[^<>\"]+title_no=" + titlenumber + "\\&episode_no=\\d+)\"").getColumn(0);
                if (links == null || links.length == 0) {
                    /* Maybe we already found everything or there simply ism't anything. */
                    break;
                }
                for (final String singleLink : links) {
                    decryptedLinks.add(this.createDownloadlink(singleLink));
                }
            }
            if (decryptedLinks.size() == 0) {
                return null;
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }
}
