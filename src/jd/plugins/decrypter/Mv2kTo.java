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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "movie4k.to", "m2k.to" }, urls = { "https?://(www\\.)?movie4k\\.to/{1,2}(?!movies\\-(all|genre)|tvshows\\-season)(tvshows\\-\\d+\\-[^<>\"/]*?\\.html|[^<>\"/]*\\-\\d+(?:.*?\\.html)?|\\d+\\-[^<>\"/]*?)(\\.html)?", "https?://(?:www\\.)?(?:m2k\\.to|movie2k\\.com|movie2k\\.com|movie2k\\.me|movie2k\\.ws)/[a-zA-Z0-9\\-]+\\d+[a-zA-Z0-9\\-]+\\.html" })
public class Mv2kTo extends PluginForDecrypt {

    // note: movie2k.to no dns record raztoki20160308

    public Mv2kTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS  = "https?://(www\\.)?movie4k\\.to//?[a-z0-9\\-_]+\\-all\\-\\d+\\.html";
    private static final String INVALIDLINKS2 = "https?://(www\\.)?movie4k\\.to//?tvshows\\-episode[a-z0-9\\-]+\\.html";

    /**
     * Description of regex array: 1= nowvideo.co, streamcloud.com 2=flashx.tv, vidbux.com, xvidstage.com, vidstream.in, hostingbulk.com,
     * uploadc.com, allmyvideos.net, firedrive.com, and many others 3=zalaa.com, 4=stream2k.com 5=flashx.tv, yesload.net
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // m2k is back, you can not rename url back to movie4k.to, the additional domains are not online either...
        final String parameter = param.toString().replaceFirst("(?:movie2k\\.com|movie2k\\.com|movie2k\\.me|movie2k\\.ws)/", "m2k.to/");
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String initalMirror = parameter.substring(parameter.lastIndexOf("/") + 1);
        br.setFollowRedirects(true);
        if (parameter.matches(INVALIDLINKS) || parameter.matches(INVALIDLINKS2) || parameter.contains("/index.php")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("/images/404\\.jpg")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String continuelink = br.getRegex("<SCRIPT>window\\.location='([^<>\"]*?)';</SCRIPT>").getMatch(0);
        if (continuelink != null) {
            br.getPage(continuelink);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().length() < 30) {
            logger.info("Invalid URL, or the URL doesn't exist any longer: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>Watch ([^<>\"]*?) online - Watch Movies Online, Full Movies, Download</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>(.*?) online").getMatch(0);
        }

        /* 2016-11-14: Removed a lot of code and replaced it with this (Rev which contains the old code == 35530) */
        final String[] htmls = this.br.getRegex("<tr id=\"link_\\d+\" >.*?</tr>").getColumn(-1);
        for (final String html : htmls) {
            final String url = new Regex(html, " <a[^>]*?href=\"(http[^<>\"]+)\">").getMatch(0);
            if (url == null) {
                continue;
            }
            decryptedLinks.add(this.createDownloadlink(url));
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}