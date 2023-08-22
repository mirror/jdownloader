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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "redbull.com" }, urls = { "https?://(?:www\\.)?redbull\\.com/(de-de)/(episodes|films)/([\\w\\-]+)" })
public class RedbullTv extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public RedbullTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final Regex urlRegex = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String urlMediaType = urlRegex.getMatch(1);
        final String urlSlug = urlRegex.getMatch(2);
        final String internalMediaType;
        if (urlMediaType.equals("episodes")) {
            internalMediaType = "episode-videos";
        } else {
            internalMediaType = "films";
        }
        /* Use API to find internal videoID. */
        br.getPage("https://www.redbull.com/v3/api/graphql/v1/v3/feed/de-DE%3Ede-INT?filter[type]=" + internalMediaType + "&filter[uriSlug]=" + urlSlug + "&disableUsageRestrictions=true&rb3Schema=v1:urlLookup&rb3CountryCode=de");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> urlLookupRoot = restoreFromString(br.toString(), TypeRef.MAP);
        /* Continue with internal videoID. */
        final String videoID = (String) JavaScriptEngineFactory.walkJson(urlLookupRoot, "data/translations/{0}/id");
        if (StringUtils.isEmpty(videoID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Different API request for series-episodes and films but similar json response. */
        if (urlMediaType.equalsIgnoreCase("episodes")) {
            br.getPage("https://api-player.redbull.com/rbcom/episodewithplaylist?videoId=" + Encoding.urlEncode(videoID));
        } else {
            br.getPage("https://api-player.redbull.com/rbcom/vodwithplaylist?videoId=" + Encoding.urlEncode(videoID) + "&scoring=featuredFresh&disableUsageRestrictions=false&playlistItems=20&localeMixing=de-DE%3Ede-INT&relatedTo=" + Encoding.urlEncode(videoID) + "&spaces=redbull_com%2Crbtv%2Credbullmusic");
        }
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final String title = (String) entries.get("title");
        final String subtitle = (String) entries.get("subHeading");
        final String description = (String) entries.get("description");
        String main_title = title;
        long season = -1;
        long episode = -1;
        final Number seasonNumber = ((Number) entries.get("seasonNumber"));
        final Number episodeNumberO = ((Number) entries.get("episodeNumber"));
        if (seasonNumber != null) {
            season = seasonNumber.longValue();
        }
        if (episodeNumberO != null) {
            episode = episodeNumberO.longValue();
        }
        if (season > -1 && episode > -1) {
            final DecimalFormat df = new DecimalFormat("00");
            main_title += " - S" + df.format(season) + "E" + df.format(episode);
        }
        main_title = main_title + " - " + subtitle;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(main_title);
        fp.setComment(description);
        final String hlsMaster = (String) entries.get("videoUrl");
        if (hlsMaster == null) {
            return null;
        }
        /* TODO: Implement quality selection and improve filenames. */
        br.getPage(hlsMaster);
        final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br);
        for (final HlsContainer quality : qualities) {
            final DownloadLink video = this.createDownloadlink(quality.getDownloadurl().replaceFirst("https?://", "m3u8s://"));
            video.setFinalFileName(main_title + "_" + quality.getHeight() + "p.mp4");
            video.setAvailable(true);
            video._setFilePackage(fp);
            decryptedLinks.add(video);
        }
        return decryptedLinks;
    }

    private String getFormatString(final String[] formatinfo) {
        String formatString = "";
        final String videoCodec = formatinfo[0];
        final String videoBitrate = formatinfo[1];
        final String videoResolution = formatinfo[2];
        final String audioCodec = formatinfo[3];
        final String audioBitrate = formatinfo[4];
        if (videoCodec != null) {
            formatString += videoCodec + "_";
        }
        if (videoResolution != null) {
            formatString += videoResolution + "_";
        }
        if (videoBitrate != null) {
            formatString += videoBitrate + "_";
        }
        if (audioCodec != null) {
            formatString += audioCodec + "_";
        }
        if (audioBitrate != null) {
            formatString += audioBitrate;
        }
        if (formatString.endsWith("_")) {
            formatString = formatString.substring(0, formatString.lastIndexOf("_"));
        }
        return formatString;
    }
}
