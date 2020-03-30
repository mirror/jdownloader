//jDownloader - Downloadmanager
//Copyright (C) 2020  JD-Team support@jdownloader.org
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
//along with this program.  If not, see <https?://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "animestc.com" }, urls = { "https?://(?:www\\.)?animestc\\.com/animes/[^/]+/?" })
public class Animestc extends PluginForDecrypt {
    public Animestc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String html = br.toString();

        ArrayList<String> episodesContentFullHdList = parseEpisodeContents(html, "1080p");
        ArrayList<String> episodesContentHdList = parseEpisodeContents(html, "720p");

        if (episodesContentFullHdList.isEmpty() && episodesContentHdList.isEmpty()) {
            logger.warning("Unable to parse episodes container");
            return decryptedLinks;
        }

        for (int i = 0; i < episodesContentFullHdList.size(); ++i) {
            ArrayList<String> episodeLinkList = parseEpisodeLinks(episodesContentFullHdList.get(i));

            if (episodeLinkList.isEmpty()){
                episodeLinkList = parseEpisodeLinks(episodesContentHdList.get(i));
            }
            for (String episodeLink : episodeLinkList) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(episodeLink)));
            }
        }

        return decryptedLinks;
    }

    ArrayList<String> parseEpisodeLinks(String episodeContent){
        Pattern pattern = Pattern.compile("href=\"(.*)\"");
        return getListOfMatches(pattern, episodeContent);
    }

    ArrayList<String> parseEpisodeContents(String html, String quality) {
        Pattern pattern = Pattern.compile("<div class=\"links_" + quality + "\">(.*?)<div class=\"clearfix\">", Pattern.DOTALL);
        return getListOfMatches(pattern, html);
    }

    ArrayList<String> getListOfMatches(Pattern pattern, String text){
        Matcher m = pattern.matcher(text);
        ArrayList<String> list = new ArrayList<>();
        while (m.find()) {
            for (int i = 0; i < m.groupCount(); i++) {
                list.add(m.group(i + 1));
            }
        }
        return list;
    }
}
