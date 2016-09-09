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
import java.util.HashSet;

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
        final String fpName = br.getRegex("<title>([^<>]+)</title>").getMatch(0);
        final FilePackage fp;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
        } else {
            fp = null;
        }

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
                if (fp != null) {
                    fp.add(dl);
                }
                decryptedLinks.add(dl);
            }
        } else {
            int pageIndex = 1;
            final HashSet<String> singleLinks = new HashSet<String>();
            while (true) {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                if (pageIndex > 1) {
                    this.br.getPage(parameter + "&page=" + pageIndex);
                }
                /* Find urls of all episode of a title --> Re-Add these single episodes to the crawler. */
                links = br.getRegex("<li id=\"episode_\\d+\">[^<>]*?<a href=\"(https?://[^<>\"]+title_no=" + titlenumber + "\\&episode_no=\\d+)\"").getColumn(0);
                if (links == null || links.length == 0) {
                    /* Maybe we already found everything or there simply ism't anything. */
                    break;
                }
                boolean nextPage = false;
                for (final String singleLink : links) {
                    if (singleLinks.add(singleLink)) {
                        nextPage = true;
                        final DownloadLink link = this.createDownloadlink(singleLink);
                        if (fp != null) {
                            fp.add(link);
                        }
                        distribute(link);
                        decryptedLinks.add(link);
                    }
                }
                if (nextPage) {
                    pageIndex++;
                } else {
                    break;
                }
            }
            if (decryptedLinks.size() == 0) {
                return null;
            }
        }

        return decryptedLinks;
    }
}
