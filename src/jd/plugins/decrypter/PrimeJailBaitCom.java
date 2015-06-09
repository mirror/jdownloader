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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "primejailbait.com" }, urls = { "https?://(www\\.)?primejailbait\\.com/(id/\\d+|profile/[A-Za-z0-9\\-_]+/fav/\\d+)/$" }, flags = { 0 })
public class PrimeJailBaitCom extends PluginForDecrypt {

    public PrimeJailBaitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String TYPE_SINGLE       = "^https?://(www\\.)?primejailbait\\.com/id/\\d+/$";
    private final String TYPE_PROFILE_FAVS = "^https?://(www\\.)?primejailbait\\.com/profile/[A-Za-z0-9\\-_]+/fav/\\d+/$";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("images/404\\.png\"") || br.getURL().equals("http://primejailbait.com/404/") || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_PROFILE_FAVS)) {
            final Regex urlinfo = new Regex(parameter, "primejailbait.com/profile/([^/]+)/fav/([^/]+)/$");
            final String username = urlinfo.getMatch(0);
            final String lid = urlinfo.getMatch(1);

            String next = null;
            final int pics_per_page = 30;
            int currentPage = 1;
            String[] thumbinfo = null;
            do {
                logger.info("Decrypting page: " + currentPage);
                if (this.isAbort()) {
                    logger.info("User aborted decryption process");
                    break;
                }
                if (currentPage > 1) {
                    br.getPage("/profile_inf.php?page=" + currentPage + "&user=" + username + "&list=" + lid);
                }
                thumbinfo = br.getRegex("<div class=\\'thumb\\' id=\\'\\d+\\'>(.*?)<span>By:").getColumn(0);
                if (thumbinfo == null || thumbinfo.length == 0) {
                    return null;
                }
                for (final String thumb : thumbinfo) {
                    String thumb_url = new Regex(thumb, "(https?://[a-z0-9\\-\\.]+/pics/bigthumbs/[^<>\"]*?)\\'").getMatch(0);
                    if (thumb_url == null && decryptedLinks.size() > 1) {
                        logger.info("Probably reached end of the page");
                        break;
                    }
                    if (thumb_url == null) {
                        return null;
                    }
                    /* Build directlinks */
                    thumb_url = thumb_url.replace("/bigthumbs/", "/original/");
                    final DownloadLink dl = createDownloadlink("directhttp://" + thumb_url);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
                currentPage++;
            } while (thumbinfo.length >= pics_per_page);
        } else {
            String finallink = br.getRegex("<div id=\"bigwall\" class=\"right\">[\t\n\r ]+<img border=0 src=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\\'(https?://pics\\.primejailbait\\.com/pics/original/[^<>\"\\']*?)\\'").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}