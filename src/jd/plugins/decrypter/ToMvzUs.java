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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
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
@DecrypterPlugin(revision = "$Revision: 30086 $", interfaceVersion = 3, names = { "twomovies.us" }, urls = { "http://(?:www\\.)?twomovies\\.us/(?:watch_movie/[a-zA-z0-9_]+|watch_episode/[a-zA-Z0-9_]+/\\d+/\\d+|full_movie/\\d+/\\d+/\\d+/(?:episode/\\d+/\\d+/|movie/))" }, flags = { 0 })
public class ToMvzUs extends PluginForDecrypt {

    public ToMvzUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    final String host = "http://(?:www\\.)?twomovies\\.us/";
    final String fm   = host + "full_movie/\\d+/\\d+/\\d+/(?:episode/\\d+/\\d+/|movie/)";
    final String wt   = host + "(?:watch_episode/[a-zA-Z0-9_]+/\\d+/\\d+|watch_movie/[a-zA-z0-9_]+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "links_tos", "1");
        br.getPage(parameter);
        if (br.getHttpConnection() == null || !br.getHttpConnection().isOK()) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        } else if (parameter.matches(fm) && br.getURL().matches(host + "watch_[^/]+/.+")) {
            // redirect happened back to the main subgroup, this happens when the parameter doesn't end with /
            System.out.println(parameter);
            return decryptedLinks;
        }
        // tv ep each mirror, movie each mirror.
        if (parameter.matches(wt)) {
            decryptWatch(decryptedLinks);
            final String fpName = new Regex(parameter, "(?:watch_episode|watch_movie)/(.*?)/").getMatch(0);
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.replace("_", " "));
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
        if (parameter.matches(fm)) {
            decryptIframe(decryptedLinks);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void decryptIframe(ArrayList<DownloadLink> decryptedLinks) {
        // they are always held in iframe src. page seems to only have one.
        final String[] iframes = br.getRegex("<iframe .*?</iframe>").getColumn(-1);
        if (iframes != null) {
            for (final String iframe : iframes) {
                final String src = new Regex(iframe, "src=(\"|')(.*?)\\1").getMatch(1);
                if (src != null) {
                    decryptedLinks.add(createDownloadlink(src));
                }
            }
        }
    }

    private void decryptWatch(ArrayList<DownloadLink> decryptedLinks) {
        // scan for each ep
        final String[] eps = br.getRegex(fm).getColumn(-1);
        if (eps != null) {
            for (final String ep : eps) {
                decryptedLinks.add(createDownloadlink(ep));
            }
        } else {
            System.out.println(1);
        }
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

}