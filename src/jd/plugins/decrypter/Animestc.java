package jd.plugins.decrypter;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.plugins.*;
import jd.plugins.components.PluginJSonUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {"animestc.net"}, urls = {"https?://(?:www\\.)?animestc\\.net/animes/(.*)"})
public class Animestc extends PluginForDecrypt {
    public Animestc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String url = param.getCryptedUrl();
        String seriesName = parseSerieName(url);
        String seriesId = requestSerieId(seriesName);
        ArrayList<String> episodesLinks = requestEpisodeLinks(seriesId);
        for (String episodeLink : episodesLinks) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(episodeLink)));
        }
        return decryptedLinks;
    }

    String parseSerieName(String url) throws PluginException {
        Pattern pattern = Pattern.compile("animes/(.*)");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find() || matcher.group(1).isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to get serie name");
        }
        return matcher.group(1);
    }

    String requestSerieId(String serieName) throws Exception {
        br.getPage("https://api2.animestc.com/series?slug=" + serieName);
        String serieId = PluginJSonUtils.getJson(br, "id");
        if (serieId.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to get serie id");
        }
        return serieId;
    }

    ArrayList<String> requestEpisodeLinks(String serieId) throws Exception {
        br.getPage("/episodes?order=id&direction=desc&page=1&seriesId=" + serieId + "&specialOrder=true");
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        ArrayList<String> episodes = new ArrayList<String>();
        ArrayList<LinkedHashMap<String, Object>> episodesResponse = (ArrayList<LinkedHashMap<String, Object>>) entries.get("data");
        for (LinkedHashMap<String, Object> episodeResponse : episodesResponse) {
            LinkedHashMap<String, Object> linksResponse = (LinkedHashMap<String, Object>) episodeResponse.get("links");
            linksResponse.remove("online");
            Set<String> keys = linksResponse.keySet();
            ArrayList<String> keysArrayList = new ArrayList<String>(keys);
            Collections.reverse(keysArrayList);
            boolean qualityWithLinks = false;
            for (String quality: keysArrayList) {
                ArrayList<LinkedHashMap<String, Object>> links = (ArrayList<LinkedHashMap<String, Object>>) linksResponse.get(quality);
                for (LinkedHashMap<String, Object> link : links) {
                    Integer linkId = (Integer) episodeResponse.get("id");
                    String finalUrl = "https://www.animestelecine.top/link/" + Base64.encode(linkId + "/high/" + link.get("index"));
                    episodes.add(finalUrl);
                    qualityWithLinks = true;
                }
                if (qualityWithLinks){
                    break;
                }
            }
        }
        return episodes;
    }
}
