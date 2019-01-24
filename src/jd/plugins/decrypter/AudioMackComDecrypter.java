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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.AudioMa;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "audiomack.com" }, urls = { "https?://(www\\.)?audiomack\\.com/((?:embed\\d-)?album|(?:embed/)?(?:album|playlist))/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+" })
public class AudioMackComDecrypter extends PluginForDecrypt {
    public AudioMackComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* 2019-01-24: API support is broken */
    private static final boolean USE_OAUTH_API = true;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (USE_OAUTH_API) {
            final String parameter = param.toString();
            br.setFollowRedirects(true);
            br.getPage(parameter);
            String ogurl = br.getRegex("\"og:url\" content=\"([^\"]+)\"").getMatch(0);
            final String musicType = new Regex(ogurl, ".+?/(?:embed/)?(album|playlist)/.+?/.+$").getMatch(0);
            if (musicType == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean embedMode = new Regex(ogurl, ".+?/embed/(?:album|playlist)/.+?/.+$").matches();
            br.getPage(AudioMa.getOAuthQueryString(br));
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0), null));
                return decryptedLinks;
            }
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            String description = (String) JavaScriptEngineFactory.walkJson(entries, "results/description");
            if (StringUtils.isNotEmpty(description)) {
                description = Encoding.htmlDecode(description);
            }
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> tracks = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "results/tracks");
            int index = 0;
            for (Map<String, Object> track : tracks) {
                String url = (String) track.get("download_url");
                if (StringUtils.isEmpty(url)) {
                    url = (String) track.get("streaming_url");
                }
                final DownloadLink dl = createDownloadlink(url);
                String title = (String) track.get("title");
                String feat = (String) track.get("featuring");
                String fileName;
                if ("playlist".equals(musicType)) {
                    String artist = (String) track.get("artist");
                    fileName = String.format("%d %s - %s", index + 1, artist, title);
                } else {
                    fileName = String.format("%d %s", index + 1, title);
                }
                if (!embedMode && StringUtils.isNotEmpty(feat)) {
                    fileName = String.format("%s (feat. %s)", fileName, feat.replaceFirst(", ([^,]+)$", " & $1"));
                }
                fileName += ".mp3";
                dl.setFinalFileName(Encoding.htmlDecode(fileName));
                dl.setAvailable(true);
                dl.setProperty("mainlink", ogurl);
                if (description != null) {
                    dl.setComment(description);
                }
                dl.setContentUrl(ogurl);
                decryptedLinks.add(dl);
                index++;
            }
            String fpName;
            if ("playlist".equals(musicType)) {
                fpName = (String) JavaScriptEngineFactory.walkJson(entries, "results/title");
            } else {
                String artist = (String) JavaScriptEngineFactory.walkJson(entries, "results/artist");
                String title = (String) JavaScriptEngineFactory.walkJson(entries, "results/title");
                fpName = String.format("%s-%s", artist, title);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        } else {
            br = new Browser();
            final String parameter = param.toString().replaceFirst("/embed\\d-album/", "/album/");
            br.setFollowRedirects(true);
            br.getPage(parameter);
            /* Offline or not yet released */
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"countdown\\-clock\"|This song has been removed due to a DMCA Complaint")) {
                decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0), null));
                return decryptedLinks;
            }
            String fpName = br.getRegex("name=\"twitter:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) {
                final Regex paraminfo = new Regex(parameter, "/([A-Za-z0-9\\-_]+)/([A-Za-z0-9\\-_]+)$");
                fpName = paraminfo.getMatch(0) + " - " + paraminfo.getMatch(1).replace("-", " ");
            }
            final String plaintable = br.getRegex("<div id=\"playlist\" class=\"plwrapper\" for=\"audiomack\\-embed\">(.*?</div>[\t\n\r ]+</div>[\t\n\r ]+</div>(<\\!\\-\\-/\\.song\\-wrap\\-\\->)?)[\t\n\r ]+</div>[\t\n\r ]+</div>").getMatch(0);
            final String[] links = plaintable.split("<div class=\"song\"");
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String description = br.getRegex("<meta name=\"description\" content=\"(.*?)\" >").getMatch(0);
            for (final String singleinfo : links) {
                final Regex url_name = new Regex(singleinfo, "<a href=\"#\" data\\-url=\"(http://(www\\.)?audiomack\\.com/api/music/url/album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+)\">([^<>\"]*?)<");
                final String url = url_name.getMatch(0);
                String name = url_name.getMatch(2);
                final String titlenumber = new Regex(singleinfo, "<div class=\"index\">(\\d+\\.)</div>").getMatch(0);
                if (url != null && name != null && titlenumber != null) {
                    name = Encoding.htmlDecode(name).trim();
                    final DownloadLink fina = createDownloadlink(url);
                    final String finalname = titlenumber + name + ".mp3";
                    fina.setFinalFileName(finalname);
                    fina.setAvailable(true);
                    fina.setProperty("plain_filename", finalname);
                    fina.setProperty("mainlink", parameter);
                    if (description != null) {
                        fina.setComment(Encoding.htmlDecode(description));
                    }
                    fina.setContentUrl(parameter);
                    decryptedLinks.add(fina);
                }
            }
            fpName = Encoding.htmlDecode(fpName.trim());
            final String ziplink = br.getRegex("\"(https?://music\\.audiomack\\.com/albums/[^<>\"]+\\.zip?[^<>\"]*?)\"").getMatch(0);
            if (ziplink != null) {
                final DownloadLink fina = createDownloadlink("directhttp://" + ziplink);
                fina.setFinalFileName(fpName + ".zip");
                fina.setAvailable(true);
                fina.setContentUrl(ziplink);
                decryptedLinks.add(fina);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        }
    }
}
