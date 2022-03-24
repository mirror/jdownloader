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
import java.util.List;
import java.util.Map;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AnimeFrenzy extends antiDDoSForDecrypt {
    public AnimeFrenzy(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "animefrenzy.vip", "animefrenzy.net", "animefrenzy.org" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:anime|cartoon|watch|stream)/[^/]+|[\\w\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>(?:Watch\\s+)?([^<]+)\\s+(?:- Watch Anime Online|English\\s+[SD]ub\\s+)").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("<li[^>]*>\\s*<a[^>]+href\\s*=\\s*[\"']([^\"']+/watch/[^\"']+)[\"']").getColumn(0));
        Collections.addAll(links, br.getRegex("<a[^>]+class\\s*=\\s*[\"']noepia[\"'][^>]+href\\s*=\\s*[\"']([^\"']+)[\"']").getColumn(0));
        String[][] hostLinks = br.getRegex("\\\"(?:host|id)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^\\}]+\\\"(?:host|id)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^\\}]+\\\"type\\\"\\s*:\\s*\\\"(?:subbed|cartoon)\\\"").getMatches();
        for (String[] hostLink : hostLinks) {
            links.add(buildEmbedURL(hostLink[0], hostLink[1]));
            links.add(buildEmbedURL(hostLink[1], hostLink[0]));
        }
        String showSlug = new Regex(parameter, "(?:watch|anime)/([\\w-]+)").getMatch(0);
        if (StringUtils.isNotEmpty(showSlug)) {
            Browser br2 = br.cloneBrowser();
            getPage(br2, "https://ani.api-web.site/anime/slug/" + showSlug + "?token=Yopgjtcustomer7NX1Oe");
            Map<String, Object> jsonEntries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br2.toString());
            if (jsonEntries.get("data") != null) {
                Map<String, Object> jsonData = (Map<String, Object>) jsonEntries.get("data");
                if (jsonData.get("episodes") != null) {
                    List<Map<String, Object>> jsonEpisodes = (List<Map<String, Object>>) jsonData.get("episodes");
                    if (jsonEpisodes.size() > 0) {
                        for (Map<String, Object> jsonEpisode : jsonEpisodes) {
                            if (StringUtils.isNotEmpty((String) jsonEpisode.get("slug"))) {
                                links.add(br.getURL("/anime/" + (String) jsonEpisode.get("slug")).toString());
                            }
                        }
                    }
                }
            }
        }
        showSlug = new Regex(parameter, "(?:stream|anime)/([\\w-]+)").getMatch(0);
        if (StringUtils.isNotEmpty(showSlug)) {
            Browser br2 = br.cloneBrowser();
            getPage(br2, "https://ani.api-web.site/anime-episode/slug/" + showSlug + "?token=Yopgjtcustomer7NX1Oe");
            Map<String, Object> jsonEntries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br2.toString());
            if (jsonEntries.get("data") != null) {
                Map<String, Object> jsonData = (Map<String, Object>) jsonEntries.get("data");
                if (jsonData.get("videos") != null) {
                    List<Map<String, Object>> jsonEpisodes = (List<Map<String, Object>>) jsonData.get("videos");
                    if (jsonEpisodes.size() > 0) {
                        for (Map<String, Object> jsonEpisode : jsonEpisodes) {
                            if (StringUtils.isNotEmpty((String) jsonEpisode.get("host")) && StringUtils.isNotEmpty((String) jsonEpisode.get("video_id"))) {
                                String host = (String) jsonEpisode.get("host");
                                String video_id = (String) jsonEpisode.get("video_id");
                                links.add(buildEmbedURL(host, video_id));
                            }
                        }
                    }
                }
            }
        }
        for (String link : links) {
            if (link != null) {
                link = processPrefixSlashes(Encoding.htmlDecode(link));
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        /* 2022-03-24 */
        final PluginForDecrypt plg = this.getNewPluginForDecryptInstance("gogoplay4.com");
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        for (final String url : urls) {
            if (plg.canHandle(url)) {
                decryptedLinks.add(this.createDownloadlink(url));
            }
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
        String result = null;
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
        } else if (host.equalsIgnoreCase("vidstream") || host.equalsIgnoreCase("vidstream")) {
            result = "https://vidstreaming.io/download?id=" + id;
        } else if (host.equals("yare.wtf")) {
            result = "https://yare.wtf/vidstreaming/download/" + id;
        } else if (host.equals("facebook")) {
            result = "https://www.facebook.com/plugins/video.php?href=https%3A%2F%2Fwww.facebook.com%2Flayfon.alseif.16%2Fvideos%2F" + id + "%2F";
        } else if (host.equals("upload2")) {
            result = "https//upload2.com/embed/" + id;
        }
        return result;
    }
}