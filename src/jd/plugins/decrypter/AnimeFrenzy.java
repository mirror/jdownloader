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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animefrenzy.eu" }, urls = { "https?://(www\\.)?animefrenzy\\.eu/(?:anime|watch)/\\d+-[\\w-]+" })
public class AnimeFrenzy extends antiDDoSForDecrypt {
    public AnimeFrenzy(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>(?:Watch\\s+)?([^<]+)\\s+(?:- Watch Anime Online|English\\s+[SD]ub\\s+)").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("<li[^>]*>\\s*<a[^>]+href\\s*=\\s*[\"']([^\"']+/watch/[^\"']+)[\"']").getColumn(0));
        String[][] hostLinks = br.getRegex("\\{\\s*\"host\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"id\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"type\"\\s*:\\s*\"subbed\"").getMatches();
        for (String[] hostLink : hostLinks) {
            links.add(buildEmbedURL(hostLink[0], hostLink[1]));
        }
        for (String link : links) {
            link = processPrefixSlashes(Encoding.htmlDecode(link));
            decryptedLinks.add(createDownloadlink(link));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String processPrefixSlashes(String link) throws IOException {
        link = link.trim().replaceAll("^//", "https://");
        if (link.startsWith("/")) {
            link = this.br.getURL(link).toString();
        }
        return link;
    }

    private String buildEmbedURL(String host, String id) {
        String result = "";
        if (host.equals("trollvid")) {
            result = "https//trollvid.net/embed/" + id;
        } else if (host.equals("mp4.sh")) {
            result = "https://trollvid.net/embedc/" + id;
        } else if (host.equals("mp4upload")) {
            result = "//www.mp4upload.com/embed-" + id + ".html";
        } else if (host.equals("xstreamcdn")) {
            result = "https://www.xstreamcdn.com/v/" + id;
        } else if (host.equals("vidstreaming")) {
            result = "https://vidstreaming.io/streaming.php?id=" + id;
        } else if (host.equals("facebook")) {
            result = "https://www.facebook.com/plugins/video.php?href=https%3A%2F%2Fwww.facebook.com%2Flayfon.alseif.16%2Fvideos%2F" + id + "%2F";
        } else if (host.equals("upload2")) {
            result = "https//upload2.com/embed/" + id;
        }
        return result;
    }
}