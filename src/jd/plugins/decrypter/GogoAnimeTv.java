//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision: 37494 $", interfaceVersion = 2, names = { "gogoanime.tv" }, urls = { "https?://(\\w+\\.)?(?:gogoanime\\.(?:tv|io)|vidstreaming\\.io|gogodramaonline\\.com|gogodrama\\.us|gogodrama\\.to|k-vid\\.net)/(?:(?:watch/)?[-A-Za-z0-9]+-episode-\\d+|(?:embed|streaming)\\.php\\?id=[a-zA-Z0-9_/\\+=\\-%]+)" })
@SuppressWarnings("deprecation")
public class GogoAnimeTv extends antiDDoSForDecrypt {

    public GogoAnimeTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private final String embed = "/(?:embed|streaming)\\.php\\?id=[a-zA-Z0-9_/\\+=\\-%]+";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // couple domains down?
        final String parameter = param.toString().replaceFirst("gogodrame\\.[a-z]+", "gogodramaonline.com");
        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // two formats
        if (new Regex(parameter, embed).matches()) {
            handleEmbed(decryptedLinks);
        } else {
            handleEp(decryptedLinks);
            final String fpName = br.getRegex("div class=\"title_name\">\\s*<h2>(.*?)</h2>").getMatch(0);
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private void handleEp(ArrayList<DownloadLink> decryptedLinks) {
        // download link which is the usually variant of iframe. but is also present within data-video
        final String[] hrefs = br.getRegex("<a [^>]*data-video=('|\")(.*?)\\1").getColumn(1);
        if (hrefs != null) {
            for (final String href : hrefs) {
                decryptedLinks.add(createDownloadlink(Request.getLocation(href, br.getRequest())));
            }
        }
    }

    // id reference is base64encoded (number)
    private void handleEmbed(final ArrayList<DownloadLink> decryptedLinks) {
        // simple iframe
        final String link = br.getRegex("<iframe[^>]*\\s+src=\"((?:https?:)?//[^/]+/[^<>\"]+)\"").getMatch(0);
        if (!inValidate(link)) {
            decryptedLinks.add(createDownloadlink(link));
        }
        return;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}