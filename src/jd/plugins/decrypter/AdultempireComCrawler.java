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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.GenericM3u8;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AdultempireComCrawler extends PluginForDecrypt {
    public AdultempireComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "adultempire.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(\\d+/[a-z0-9\\-]+\\.html|gw/player/[^/]*item_id=\\d+.*)");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_EMBED = "https://www.adultempire.com/gw/player/[^/]*item_id=(\\d+).*";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String internalID;
        if (param.getCryptedUrl().matches(TYPE_EMBED)) {
            /* Internal ID given inside URL. */
            internalID = new Regex(param.getCryptedUrl(), TYPE_EMBED).getMatch(0);
        } else {
            /* Internal ID needs to be parsed via HTML code. */
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            internalID = br.getRegex("item:\\s*'(\\d+)'").getMatch(0);
            final String[] scenesSnapshotURLs = br.getRegex("a rel=\"scenescreenshots\"\\s*href=\"(https?://[^\"]+)\"").getColumn(0);
            if (scenesSnapshotURLs.length > 0) {
                for (final String scenesSnapshotURL : scenesSnapshotURLs) {
                    final DownloadLink image = this.createDownloadlink(scenesSnapshotURL);
                    image.setAvailable(true);
                    ret.add(image);
                }
            }
        }
        if (internalID == null) {
            /* Assume that content is offline or no trailer is available. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Website will include more parameters but really only "item_id" is required! */
        br.getPage("https://www." + this.getHost() + "/gw/player/?type=trailer&item_id=" + internalID);
        final String thumbnailUrl = PluginJSonUtils.getJson(br, "thumbnailUrl");
        final String httpStreamingURL = PluginJSonUtils.getJson(br, "contentUrl");
        final Browser brc = new Browser();
        brc.getHeaders().put("Accept", "application/json, text/plain, */*");
        brc.getHeaders().put("Content-Type", "application/json");
        brc.getHeaders().put("Origin", "https://www." + this.getHost());
        brc.postPageRaw("https://player.digiflix.video/verify", "{\"item_id\":" + internalID + ",\"encrypted_customer_id\":null,\"signature\":null,\"timestamp\":null,\"stream_type\":\"trailer\",\"initiate_tracking\":false,\"forcehd\":false}");
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(brc.getRequest().getHtmlCode());
        final Map<String, Object> item_detail = (Map<String, Object>) entries.get("item_detail");
        final String title = (String) item_detail.get("title");
        final String back_cover = (String) item_detail.get("back_cover");
        final String front_cover = (String) item_detail.get("front_cover");
        final String posterURL = (String) item_detail.get("poster");
        if (!StringUtils.isEmpty(thumbnailUrl)) {
            final DownloadLink thumbnail = this.createDownloadlink(thumbnailUrl);
            thumbnail.setAvailable(true);
            ret.add(thumbnail);
        }
        if (!StringUtils.isEmpty(httpStreamingURL)) {
            final DownloadLink httpStream = this.createDownloadlink(httpStreamingURL);
            httpStream.setFinalFileName(title + "_http.mp4");
            httpStream.setAvailable(true);
            ret.add(httpStream);
        }
        if (!StringUtils.isEmpty(posterURL) && !posterURL.contains("/nophoto_")) {
            final DownloadLink poster = this.createDownloadlink(posterURL);
            poster.setAvailable(true);
            ret.add(poster);
        }
        if (!StringUtils.isEmpty(back_cover)) {
            final DownloadLink backcover = this.createDownloadlink(back_cover);
            backcover.setAvailable(true);
            ret.add(backcover);
        }
        if (!StringUtils.isEmpty(front_cover)) {
            final DownloadLink frontcover = this.createDownloadlink(front_cover);
            frontcover.setAvailable(true);
            ret.add(frontcover);
        }
        final String hlsMaster = (String) entries.get("playlist_url");
        br.getPage(hlsMaster);
        final List<HlsContainer> containers = HlsContainer.getHlsQualities(br);
        for (final HlsContainer container : containers) {
            final DownloadLink video = this.createDownloadlink(GenericM3u8.createURLForThisPlugin(container.getDownloadurl()));
            video.setFinalFileName(title + "_hls_" + container.getHeight() + "p.mp4");
            video.setAvailable(true);
            ret.add(video);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
