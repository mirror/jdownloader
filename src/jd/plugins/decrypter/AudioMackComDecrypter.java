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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
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
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AudioMackComDecrypter extends PluginForDecrypt {
    public AudioMackComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "audiomack.com" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(";
            /* Older RegExes */
            regex += "((?:embed\\d-)?album|(?:embed/)?(?:album|playlist))/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+";
            /* 2021-04-20: Album new */
            regex += "|[a-z0-9\\-_]+/album/[A-Za-z0-9\\-_]+";
            /* 2021-04-20: Playlist new */
            regex += "|[a-z0-9\\-_]+/playlist/[A-Za-z0-9\\-_]+";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    /* 2019-01-24: API support is broken */
    private static final boolean USE_OAUTH_API = true;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String packageidprefix = "audiomack://";
        if (USE_OAUTH_API) {
            final String contenturl = param.getCryptedUrl();
            br.getPage(contenturl);
            String ogurl = br.getRegex("\"og:url\" content=\"([^\"]+)\"").getMatch(0);
            final String musicType = new Regex(ogurl, "(?i).*(album|playlist)/.*").getMatch(0);
            if (musicType == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(AudioMa.getOAuthQueryString(br));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> results = (Map<String, Object>) entries.get("results");
            final String itemID = results.get("id").toString();
            final String status = (String) results.get("status");
            if (status != null && status.equalsIgnoreCase("suspended")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String description = (String) results.get("description");
            if (!StringUtils.isEmpty(description)) {
                description = Encoding.htmlDecode(description);
            }
            final boolean isPlaylist = "playlist".equalsIgnoreCase(musicType);
            final List<Map<String, Object>> tracks = (List<Map<String, Object>>) results.get("tracks");
            int index = 0;
            for (final Map<String, Object> track : tracks) {
                final String uploader_url_slug = track.get("uploader_url_slug").toString();
                final String url_slug = track.get("url_slug").toString();
                final DownloadLink dl = createDownloadlink("https://" + this.getHost() + "/" + uploader_url_slug + "/song/" + url_slug);
                if (isPlaylist) {
                    dl.setProperty(AudioMa.PROPERTY_PLAYLIST_POSITION, index + 1);
                    dl.setProperty(AudioMa.PROPERTY_PLAYLIST_NUMBEROF_ITEMS, tracks.size());
                }
                AudioMa.parseSingleSongData(dl, track);
                dl.setAvailable(true);
                dl.setContentUrl(ogurl);
                ret.add(dl);
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
            fp.setPackageKey(packageidprefix + "/item/" + itemID);
            fp.setName(Encoding.htmlDecode(fpName).trim());
            if (!StringUtils.isEmpty(description)) {
                fp.setComment(description);
            }
            fp.addLinks(ret);
            return ret;
        } else {
            final String contenturl = param.getCryptedUrl().replaceFirst("(?i)/embed\\d-album/", "/album/");
            br.getPage(contenturl);
            /* Offline or not yet released */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("class=\"countdown\\-clock\"|This song has been removed due to a DMCA Complaint")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String fpName = br.getRegex("name=\"twitter:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) {
                final Regex paraminfo = new Regex(contenturl, "/([A-Za-z0-9\\-_]+)/([A-Za-z0-9\\-_]+)$");
                fpName = paraminfo.getMatch(0) + " - " + paraminfo.getMatch(1).replace("-", " ");
            }
            final String plaintable = br.getRegex("<div id=\"playlist\" class=\"plwrapper\" for=\"audiomack\\-embed\">(.*?</div>[\t\n\r ]+</div>[\t\n\r ]+</div>(<\\!\\-\\-/\\.song\\-wrap\\-\\->)?)[\t\n\r ]+</div>[\t\n\r ]+</div>").getMatch(0);
            final String[] links = plaintable.split("<div class=\"song\"");
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                    if (description != null) {
                        fina.setComment(Encoding.htmlDecode(description));
                    }
                    fina.setContentUrl(contenturl);
                    ret.add(fina);
                }
            }
            fpName = Encoding.htmlDecode(fpName).trim();
            final String ziplink = br.getRegex("\"(https?://music\\.audiomack\\.com/albums/[^<>\"]+\\.zip?[^<>\"]*?)\"").getMatch(0);
            if (ziplink != null) {
                final DownloadLink fina = createDownloadlink(DirectHTTP.createURLForThisPlugin(ziplink));
                fina.setFinalFileName(fpName + ".zip");
                fina.setAvailable(true);
                fina.setContentUrl(ziplink);
                ret.add(fina);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
            return ret;
        }
    }
}
