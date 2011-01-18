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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

/**
 * @author typek_pb
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, urls = { "http://[\\w\\.]*?surfmovies\\.org/(surf-movies|surf-music|online-surf-movies|bodyboard-movies)/.+html" }, flags = { 0 }, names = { "www.surfmovies.org" })
public class SurfMvsOrg extends PluginForDecrypt {

    public SurfMvsOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.clearCookies(getHost());
        br.getPage(cryptedLink.getCryptedUrl());

        // find all links between
        // src="http://surfmovies.org/icon/download.png"
        // and
        // <a name="fb_share" type="button_count"
        // that are not referring to surfmovies.org site
        String[][] links = null;
        // anything between and <a name="fb_share" type="button_count"
        String section = br.getRegex("src=\"http://surfmovies[.]org/icon/download[.]png\"(.*?)<a name=\"fb_share\" type=\"button_count\"").getMatch(0);
        if (null != section && 0 < section.trim().length()) {
            // extract licks from section
            links = new Regex(section, "(http://(?![www\\.]*?surfmovies[.]org)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])").getMatches();
            if (null != links && 0 < links.length) {
                for (String[] link : links) {
                    if (null != link && 0 < link.length && null != link[0] && 0 < link[0].length()) {
                        decryptedLinks.add(createDownloadlink(link[0]));
                    }
                }
            }
        }
        return decryptedLinks;
    }
}
