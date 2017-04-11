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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * they host and link to other content like youtube.
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cracked.com" }, urls = { "https?://(?:www\\.)?cracked\\.com/video_\\d+.*?\\.html" }) 
public class CrackedCom extends PluginForDecrypt {

    public CrackedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(parameter);

        // they have youtube sometimes
        String finallink = br.getRegex("id=youtubePlayer src=\"//(?:www\\.)?(youtube\\.com/embed/[^<>\"]+)\"").getMatch(0);
        if (finallink != null) {
            finallink = "http://www." + finallink;
            decryptedLinks.add(createDownloadlink(finallink));
            /* A check for future embedded content */
            if (finallink.contains("youtube.com/")) {
                return decryptedLinks;
            }
        }
        // when nothing is found or result was not a youtube video lets just throw back to hoster plugin, error handling there can pick it
        // up!
        decryptedLinks.add(createDownloadlink(parameter.replace("cracked.com/", "crackeddecrypted.com/")));

        return decryptedLinks;
    }
}
