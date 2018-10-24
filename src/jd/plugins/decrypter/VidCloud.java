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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40000 $", interfaceVersion = 2, names = { "vidcloud.xyz" }, urls = { "https?://(play\\.)?vidcloud\\.xyz/(watch|stream).+" })
public class VidCloud extends PluginForDecrypt {
    public VidCloud(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        String page = br.getPage(parameter);
        String title = extractTitleFromURL(parameter);
        String[][] playlistEmbeds = br.getRegex("playcdn\\.vidcloud\\.xyz/stream/[^\"]+").getMatches();
        for (String[] playlistEmbed : playlistEmbeds) {
            final Browser brPlaylist = br.cloneBrowser();
            brPlaylist.setFollowRedirects(true);
            playlistEmbed[0] = "https://" + playlistEmbed[0];
            brPlaylist.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brPlaylist.getHeaders().put("Referer", parameter);
            String playlist = brPlaylist.getPage(playlistEmbed[0]);
            String[][] playlistItems = new Regex(playlist, "NAME=\"([0-9]+)\"[\\r\\n\\t ]+(https?://(?:[^\\.]+\\.)?vidcloud\\.xyz/[a-zA-Z0-9/\\.?=]+)").getMatches();
            for (String[] playlistItem : playlistItems) {
                String url = Encoding.htmlOnlyDecode(playlistItem[1]);
                DownloadLink dl = createDownloadlink(url);
                if (title != null) {
                    String resolution = playlistItem[0];
                    dl.setForcedFileName(title + " (" + resolution + "p).mp4");
                }
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    private String extractTitleFromURL(String url) {
        String result = new Regex(url, "[&?]jdTitle=([^&$]+)").getMatch(0);
        System.out.print(result);
        if (result != null) {
            result = Encoding.urlDecode(result, false);
        }
        return result;
    }
}