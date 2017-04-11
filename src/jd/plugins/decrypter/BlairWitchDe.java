//    jDownloader - Downloadmanager
//    Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blairwitch.de" }, urls = { "http://(?:www\\.)?blairwitch\\.de/news/[a-z-]+-\\d+/" }) 
public class BlairWitchDe extends PluginForDecrypt {
    public BlairWitchDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        HashSet<String> dupe = new HashSet<String>();

        String param = parameter.toString();
        br.getPage(param);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(param));
            return decryptedLinks;
        }
        String filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            // this may or may not be an error. to prevent frivolous guarado tickets
            return decryptedLinks;
        }
        filename = Encoding.htmlDecode(filename);

        // images
        final String[][] images = br.getRegex("(http://(?:www\\.)?blairwitch\\.de/wp-content/(?:uploads/\\d+/\\d+/|bwdatabase/gallery/full/\\d+.*?)(\\.(?:je?pg|png|gif)))").getMatches();
        if (images != null) {
            for (final String[] finallink : images) {
                if (dupe.add(finallink[0])) {
                    final DownloadLink dl = createDownloadlink("directhttp://" + finallink[0]);
                    dl.setFinalFileName(filename + finallink[1]);
                    decryptedLinks.add(dl);
                }
            }
        }

        // some are hosted themselves
        // some are youtube
        // some are at brightcove and these are rtmp
        String[] finallinks = br.getRegex("https?://(?:www\\.)?blairwitch\\.de/wp-content/bwdatabase/trailer/\\d+\\.mp4").getColumn(-1);
        if (finallinks != null) {
            for (final String finallink : finallinks) {
                if (dupe.add(finallink)) {
                    final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                    dl.setFinalFileName(filename + ".mp4");
                    decryptedLinks.add(dl);
                }
            }
        }
        // youtube
        finallinks = br.getRegex("<iframe[^>]*src=('|\")(.*?youtube.com/.*?)\\1").getColumn(1);
        if (finallinks != null) {
            for (final String finallink : finallinks) {
                if (finallink != null && dupe.add(finallink)) {
                    final DownloadLink dl = createDownloadlink(finallink);
                    decryptedLinks.add(dl);
                }
            }
        }
        // brightcove (not supported at this time)

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filename);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}