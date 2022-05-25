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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
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
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 4, names = {}, urls = {})
public class ArteMediathekV3 extends PluginForDecrypt {
    public ArteMediathekV3(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "arte.tv", "arte.fr" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z]{2})/videos/(\\d+-\\d+-[ADF]+)/([a-z0-9\\-]+)/?");
        }
        return ret.toArray(new String[0]);
    }

    private static final String API_BASE = "https://api.arte.tv/api/opa/v3";

    private static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("Authorization", "Bearer Nzc1Yjc1ZjJkYjk1NWFhN2I2MWEwMmRlMzAzNjI5NmU3NWU3ODg4ODJjOWMxNTMxYzEzZGRjYjg2ZGE4MmIwOA");
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        return crawlPrograms(param);
    }

    private ArrayList<DownloadLink> crawlPrograms(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String urlLanguage = urlinfo.getMatch(0);
        final String contentID = urlinfo.getMatch(1);
        prepBRAPI(br);
        br.getPage(API_BASE + "/programs/" + urlLanguage + "/" + contentID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // TODO: Add pagination
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object errorsO = entries.get("errors");
        if (errorsO != null) {
            /* TODO: Maybe display more detailed errormessage to user. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Map<String, Object>> programs = (List<Map<String, Object>>) entries.get("programs");
        for (final Map<String, Object> program : programs) {
            ret.addAll(this.crawlProgram(param, program));
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlProgram(final CryptedLink param, final Map<String, Object> program) throws IOException {
        // TODO: Add support for multiple videos(?), implement plugin settings
        final Map<String, Object> vid = (Map<String, Object>) program.get("mainVideo");
        final String title = vid.get("title").toString();
        final String subtitle = (String) vid.get("subtitle");
        final String dateFormatted = new Regex(vid.get("firstBroadcastDate").toString(), "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String fullDescription = (String) vid.get("fullDescription");
        final String videoStreamsAPIURL = JavaScriptEngineFactory.walkJson(vid, "links/videoStreams/web/href").toString();
        prepBRAPI(br);
        // br.getPage(API_BASE + "/videoStreams?programId=" + Encoding.urlEncode(programId) +
        // "&reassembly=A&platform=ARTE_NEXT&channel=DE&kind=SHOW&protocol=%24in:HTTPS,HLS&quality=%24in:EQ,HQ,MQ,SQ,XQ&profileAmm=%24in:AMM-PTWEB,AMM-PTHLS,AMM-OPERA,AMM-CONCERT-NEXT,AMM-Tvguide&limit=100");
        br.getPage(videoStreamsAPIURL);
        String titleBase = dateFormatted + "_" + vid.get("platform") + "_" + title;
        if (!StringUtils.isEmpty(titleBase)) {
            titleBase += " - " + subtitle;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(titleBase);
        if (!StringUtils.isEmpty(fullDescription)) {
            fp.setComment(fullDescription);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Crawl thumbnail */
        final Map<String, Object> mainImage = (Map<String, Object>) vid.get("mainImage");
        final String imageCaption = (String) mainImage.get("caption");
        final DownloadLink thumbnail = this.createDownloadlink("directhttp://" + mainImage.get("url"));
        thumbnail.setFinalFileName(titleBase + "." + mainImage.get("extension"));
        if (imageCaption != null) {
            thumbnail.setComment(imageCaption);
        }
        thumbnail.setAvailable(true);
        ret.add(thumbnail);
        distribute(thumbnail);
        /* Crawl video streams */
        // TODO: Implement pagination
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final List<Map<String, Object>> videoStreams = (List<Map<String, Object>>) entries.get("videoStreams");
        for (final Map<String, Object> videoStream : videoStreams) {
            final String protocol = videoStream.get("protocol").toString();
            if (!protocol.equalsIgnoreCase("https")) {
                /* 2022-05-25: Only grab HTTP streams for now */
                continue;
            }
            final long durationSeconds = ((Number) videoStream.get("durationSeconds")).longValue();
            final long bitrate = ((Number) videoStream.get("bitrate")).longValue();
            final DownloadLink link = this.createDownloadlink(videoStream.get("url").toString());
            final String finalFilename = titleBase + videoStream.get("filename");
            link.setFinalFileName(finalFilename);
            link.setProperty(DirectHTTP.FIXNAME, finalFilename);
            // TODO: Maybe add filesize estimation based on duration in seconds + bitrate?
            link.setAvailable(true);
            link.setDownloadSize(bitrate / 8 * 1024 * durationSeconds);
            link._setFilePackage(fp);
            ret.add(link);
            distribute(link);
        }
        return ret;
    }
}
