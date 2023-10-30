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
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
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
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AparatCom extends PluginForDecrypt {
    public AparatCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "aparat.com" });
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

    private static final String PATTERN_RELATIVE_VIDEO    = "/v/([A-Za-z0-9]+).*";
    private static final String PATTERN_RELATIVE_PLAYLIST = "/playlist/(\\d+)";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_VIDEO + "|" + PATTERN_RELATIVE_PLAYLIST + ")");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.setAllowedResponseCodes(400);
        final Regex singlevideo = new Regex(param.getCryptedUrl(), PATTERN_RELATIVE_VIDEO);
        final Regex playlist = new Regex(param.getCryptedUrl(), PATTERN_RELATIVE_PLAYLIST);
        if (singlevideo.patternFind()) {
            final String videoid = singlevideo.getMatch(0);
            br.getPage("https://www." + this.getHost() + "/api/fa/v1/video/video/show/videohash/" + videoid + "?pr=1&mf=1");
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Number status = (Number) JavaScriptEngineFactory.walkJson(entries, "meta/status");
            if (status != null && (status.intValue() == 404 || status.intValue() == 410)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String type = (String) JavaScriptEngineFactory.walkJson(entries, "data/type");
            if (!StringUtils.equalsIgnoreCase(type, "VideoShow")) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported type:" + type);
            }
            final Map<String, Object> videoShow = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/attributes");
            final String title = (String) videoShow.get("title");
            final List<Map<String, Object>> file_link_all = (List<Map<String, Object>>) videoShow.get("file_link_all");
            if (file_link_all != null) {
                // last one is best quality
                final Map<String, Object> lastEntry = file_link_all.get(file_link_all.size() - 1);
                final List<String> urls = (List<String>) lastEntry.get("urls");
                final DownloadLink dl = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(urls.get(0)));
                final String profile = (String) lastEntry.get("profile");
                String fileName;
                if (!StringUtils.isEmpty(title)) {
                    fileName = title;
                } else {
                    fileName = videoid;
                }
                if (profile != null) {
                    fileName += "_" + profile;
                }
                dl.setFinalFileName(fileName + ".mp4");
                dl.setAvailable(true);
                dl.setContentUrl(contenturl);
                ret.add(dl);
            }
            /* Put all results into one package */
            final FilePackage filePackage = FilePackage.getInstance();
            if (!title.isEmpty()) {
                filePackage.setName(Encoding.htmlDecode(title));
            }
            filePackage.setComment(title);
            filePackage.setPackageKey("aparat_com://video/" + videoid);
            filePackage.addLinks(ret);
            return ret;
        } else if (playlist.patternFind()) {
            final String playlistid = playlist.getMatch(0);
            br.getPage("https://www." + this.getHost() + "/api/fa/v1/video/playlist/one/playlist_id/" + playlistid);
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> playlistmap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/attributes");
            final int numberofVideos = ((Number) playlistmap.get("cnt")).intValue();
            logger.info("Crawling playlist " + playlistid + " | Number of videos: " + numberofVideos);
            if (numberofVideos == 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Empty playlist");
            }
            final List<Map<String, Object>> items = (List<Map<String, Object>>) entries.get("included");
            for (final Map<String, Object> item : items) {
                final String type = item.get("type").toString();
                if (!StringUtils.equalsIgnoreCase(type, "Video")) {
                    /* Skip invalid items */
                    continue;
                }
                final Map<String, Object> videomap = (Map<String, Object>) item.get("attributes");
                final String videoid = videomap.get("uid").toString();
                final DownloadLink videoresult = this.createDownloadlink("https://www." + this.getHost() + "/v/" + videoid + "?playlist=" + playlistid);
                ret.add(videoresult);
            }
            // /* Put all results into one package */
            // final FilePackage filePackage = FilePackage.getInstance();
            // if (!title.isEmpty()) {
            // filePackage.setName(Encoding.htmlDecode(title));
            // }
            // filePackage.setComment(title);
            // filePackage.setPackageKey("aparat_com://playlist/" + playlistid);
            // filePackage.addLinks(ret);
        } else {
            /* Developer mistake -> This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}