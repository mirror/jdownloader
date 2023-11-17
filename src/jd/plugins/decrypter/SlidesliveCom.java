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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.GenericM3u8;
import jd.plugins.hoster.YoutubeDashV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SlidesliveCom extends PluginForDecrypt {
    public SlidesliveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "slideslive.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(\\d+)/([\\w\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String contentID = urlinfo.getMatch(0);
        final String playerToken = br.getRegex("data-player-token=\"([^\"]+)").getMatch(0);
        final String slidesHost = br.getRegex("slideslive_on_the_fly_resized_slides_host\":\"([^\"]+)").getMatch(0);
        String title = HTMLSearch.searchMetaTag(br, "twitter:title");
        if (title == null) {
            /* Fallback */
            title = urlinfo.getMatch(1).replace("-", " ").trim();
        }
        title = Encoding.htmlDecode(title).trim();
        if (playerToken == null || slidesHost == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("https://ben.slideslive.com/player/" + contentID + "?player_token=" + Encoding.urlEncode(playerToken));
        final String externalVideoServiceID = br.getRegex("#EXT-SL-VOD-VIDEO-SERVICE-NAME:(.*?)\\s").getMatch(0);
        final String videoID = br.getRegex("EXT-SL-VOD-VIDEO-ID:(.*?)\\s").getMatch(0);
        if (videoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (externalVideoServiceID.equalsIgnoreCase("youtube")) {
            /* E.g. https://slideslive.com/38893320/lhani-detekce-lzi-a-emoce-z-pohledu-forenzni-psychologie */
            ret.add(this.createDownloadlink(YoutubeDashV2.generateContentURL(videoID)));
        } else if (externalVideoServiceID.equalsIgnoreCase("yoda")) {
            /* E.g. https://slideslive.com/38955218/diffusion-models-and-lossy-generative-modeling */
            final String serversArray = br.getRegex("#EXT-SL-VOD-VIDEO-SERVERS:(\\[.*?)\\s").getMatch(0);
            final List<String> servers = restoreFromString(serversArray, TypeRef.STRING_LIST);
            final String hlsMaster = "https://" + servers.get(0) + "/" + videoID + "/master.m3u8";
            final DownloadLink video = this.createDownloadlink(hlsMaster);
            video.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, title);
            ret.add(video);
        }
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Origin", "https://" + br.getHost());
        brc.getHeaders().put("Accept", "application/json");
        final boolean preferJsonAPI = false;
        final String slidesjsonurl = br.getRegex("EXT-SL-VOD-SLIDES-JSON-URL:(https?://.*?)\\s").getMatch(0);
        final String slidesxmlurl = br.getRegex("EXT-SL-VOD-SLIDES-XML-URL:(https?://.*?)\\s").getMatch(0);
        if (slidesjsonurl == null && slidesxmlurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String imagesExt = ".png";
        boolean useJsonAPI = false;
        if ((preferJsonAPI && slidesjsonurl != null) || slidesxmlurl == null) {
            /* 2023-11-16 */
            useJsonAPI = true;
        } else {
            // brc.getPage("https://slides.slideslive.com/" + contentID + "/" + contentID + ".xml");
            brc.getPage(slidesxmlurl);
            final String[] items = brc.getRegex("<slideName>([^<]+)</slideName>").getColumn(0);
            if (items != null && items.length > 0) {
                for (final String slideName : items) {
                    final String directurl = "https://" + slidesHost + "/" + contentID + "/slides/" + slideName + imagesExt + "?h=432&f=webp&s=lambda&accelerate_s3=1";
                    final DownloadLink image = createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
                    image.setAvailable(true);
                    ret.add(image);
                }
            } else {
                logger.info("XML handling failed -> Use json as fallback");
                useJsonAPI = true;
            }
        }
        if (useJsonAPI) {
            if (slidesjsonurl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // brc.getPage("https://s.slideslive.com/" + contentID + "/v5/slides.json?" + System.currentTimeMillis() / 1000);
            brc.getPage(slidesjsonurl);
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> slides = (List<Map<String, Object>>) entries.get("slides");
            int index = 0;
            for (final Map<String, Object> slide : slides) {
                final Map<String, Object> imagemap = (Map<String, Object>) slide.get("image");
                final Map<String, Object> videomap = (Map<String, Object>) slide.get("video");
                if (imagemap == null && videomap != null) {
                    /* Not supported */
                    logger.info("Skipping slide in index: " + index);
                    continue;
                }
                final String slideName = imagemap.get("name").toString();
                final String directurl = "https://" + slidesHost + "/" + contentID + "/slides/" + slideName + imagesExt + "?h=432&f=webp&s=lambda&accelerate_s3=1";
                final DownloadLink image = createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
                image.setAvailable(true);
                ret.add(image);
                index++;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
