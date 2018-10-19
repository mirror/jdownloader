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
//along with this program.  If not, see <https?://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 39988 $", interfaceVersion = 3, names = { "masterani.me" }, urls = { "https?://(?:www\\.)?masterani\\.me/anime/.+" })
public class MasterAniMe extends PluginForDecrypt {
    public MasterAniMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String page = br.getPage(parameter);
        String showID = new Regex(parameter, "anime/(?:[a-z]+)/(\\d+)").getMatch(0);
        String showTitle = new Regex(parameter, "anime/(?:[a-z]+)/\\d+-([^/#?&]+)").getMatch(0);
        if (showID != null && showTitle != null) {
            String fpName = br.getRegex("<title>([^<>]+) - Masterani</title>").getMatch(0);
            if (StringUtils.containsIgnoreCase(parameter, "/anime/info/")) {
                final ArrayList<HashMap> episodes = getEpisodeList(br, showID);
                for (HashMap episode : episodes) {
                    final String episodeURL = "https://www.masterani.me/anime/watch/" + showID + "-" + showTitle + "/" + ((HashMap) episode.get("info")).get("episode").toString();
                    decryptedLinks.add(createDownloadlink(episodeURL));
                }
            } else if (StringUtils.containsIgnoreCase(parameter, "/anime/watch/")) {
                if (br.containsHTML("<video-mirrors :mirrors=")) {
                    String videoJSON = br.getRegex("<video-mirrors :mirrors='([^'>]+)'>").getMatch(0);
                    if (videoJSON != null) {
                        videoJSON = videoJSON.replaceAll("\\\\/", "/");
                        ArrayList<String> mediaURLs = getURLsFromEpisodeJSON(videoJSON);
                        for (String mediaURL : mediaURLs) {
                            decryptedLinks.add(createDownloadlink(mediaURL));
                        }
                    }
                }
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
                fp.setProperty("ALLOW_MERGE", true);
            }
        }
        //
        return decryptedLinks;
    }

    private ArrayList<HashMap> getEpisodeList(Browser br, String showID) throws IOException {
        final Browser brJSON = br.cloneBrowser();
        String json = brJSON.getPage("/api/anime/" + showID + "/detailed");
        HashMap<String, Object> jsonObject = new ObjectMapper().readValue(json, HashMap.class);
        ArrayList<HashMap> result = (ArrayList<HashMap>) jsonObject.get("episodes");
        return result;
    }

    private ArrayList<String> getURLsFromEpisodeJSON(String json) throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        ArrayList<HashMap<String, Object>> videoDetails = new ObjectMapper().readValue(json, ArrayList.class);
        for (HashMap<String, Object> videoDetail : videoDetails) {
            HashMap<String, String> host = (HashMap<String, String>) videoDetail.get("host");
            StringBuilder sb = new StringBuilder();
            if (host.get("embed_prefix") != null) {
                sb.append(host.get("embed_prefix"));
            }
            if (videoDetail.get("embed_id") != null) {
                sb.append((String) videoDetail.get("embed_id"));
            }
            if (host.get("embed_suffix") != null) {
                sb.append(host.get("embed_suffix"));
            }
            if (sb.length() > 0) {
                result.add(sb.toString());
            }
        }
        return result;
    }
}
