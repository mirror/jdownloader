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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gogoanime.pro" }, urls = { "https?://(www\\d*\\.)?gogoanime\\.pro/anime/[^/]+\\d+.+" })
public class GoGoAnimePro extends antiDDoSForDecrypt {
    public GoGoAnimePro(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>(?:Gogoanime - Watch\\s*)([^<]+)\\s+in\\s+HD\\s+-\\s+GogoAnime").getMatch(0);
        String[] details = br.getRegex("<div[^>]+id\\s*=\\s*\"watch\"[^>]+data-id\\s*=\\s*\"([^\"]*)\"[^>]+data-ep-base-name\\s*=\\s*\"([^\"]*)\"[^>]*>").getRow(0);
        String titleID = details[0];
        String episodeID = details[1];
        /* 2021-02-05: episodeID is allowed to be an empty string */
        if (StringUtils.isEmpty(titleID) || episodeID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Can be "anime" or "film". So far I've only seen "anime". */
        final String type = "anime";
        final String reCaptchaKey = br.getRegex("var\\s*recaptcha_key=\\s*'([^<>\"\\']+)").getMatch(0);
        final UrlQuery query = new UrlQuery();
        query.add("id", titleID);
        query.add("ep", "");
        query.add("episode", episodeID);
        if (reCaptchaKey != null) {
            /* 2021-02-05: Captcha */
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, reCaptchaKey) {
                @Override
                public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                    return TYPE.INVISIBLE;
                }
            }.getToken();
            query.appendEncoded("token", recaptchaV2Response);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("/ajax/" + type + "/servers" + "?" + query.toString());
        final String jsonSource = this.br.toString();
        String videoDetails = (String) JavaScriptEngineFactory.walkJson(JSonStorage.restoreFromString(jsonSource, TypeRef.HASHMAP), "html");
        if (StringUtils.isEmpty(videoDetails)) {
            getLogger().warning("Could not retrieve video Detail JSON from webservice.");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final GetRequest getKey = new GetRequest("https://mcloud.to/key");
            getKey.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            Browser br2 = br.cloneBrowser();
            String mcloud = new Regex(br2.getPage(getKey), "window\\.mcloudKey\\s*=\\s*['\"]\\s*([^'\"]+)\\s*['\"]").getMatch(0);
            String[][] episodeDetails = new Regex(videoDetails, "data-name\\s*=\\s*[\"']([^\"']+)[\"'][^>]+data-name-normalized\\s*=\\s*[\"']([^\"']+)[\"'][^>]+href\\s*=\\s*[\"']([^\"']+)[\"'][^>]+data-sources\\s*=\\s*'(\\{[^']+\\})'").getMatches();
            if (episodeDetails != null) {
                for (String[] episodeDetail : episodeDetails) {
                    String episodeName = episodeDetail[0];
                    String episodeTitle = episodeDetail[1];
                    String serverIDList = episodeDetail[3];
                    if (StringUtils.isNotEmpty(serverIDList)) {
                        for (String serverID : serverIDList.replaceAll("[\\{\\}]", "").split(",")) {
                            /*
                             * 2021-02-05: Webservice change: Instead of the old method below it now uses some encoded episode ID like
                             * /ajax/anime/episode?id=52f3e81e6aa0e4e61cabc0677f1a9a24d4d6c7ae2c9b48b67ae5e2af5800b733 No idea where that's
                             * generated.
                             */
                            final GetRequest getEpisode = new GetRequest(br.getURL("/ajax/episode/info?filmId=" + titleID + "&server=" + serverID + "&episode=" + Encoding.urlEncode(episodeName) + "&mcloud=" + mcloud).toString());
                            getEpisode.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                            String getEpisodeResponse = br.getPage(getEpisode);
                            if (br.containsHTML("503 Service Temporarily Unavailable")) {
                                Thread.sleep(1000);
                                getEpisodeResponse = br.getPage(getEpisode);
                            }
                            String target = (String) JavaScriptEngineFactory.walkJson(JSonStorage.restoreFromString(getEpisodeResponse, TypeRef.HASHMAP), "target");
                            if (StringUtils.isNotEmpty(target)) {
                                decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(target)));
                            }
                            Thread.sleep(100);
                        }
                    }
                }
            }
        }
        //
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}