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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SrfChCrawler extends PluginForDecrypt {
    public SrfChCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "srf.ch" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/PLUGIN_UNDER_DEVELOPMENT_.+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(new int[] { 410 });
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex(">\\s*window\\.__SSR_VIDEO_DATA__ = (\\{.*?\\})</script>").getMatch(0);
        if (json != null) {
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> initialData = (Map<String, Object>) entries.get("initialData");
            final Map<String, Object> show = (Map<String, Object>) initialData.get("show");
            final Map<String, Object> videoDetail = (Map<String, Object>) entries.get("videoDetail");
            if (videoDetail != null) {
                return this.crawlVideo(videoDetail.get("urn").toString());
            } else if (show != null) {
                final Map<String, Object> latestMedia = (Map<String, Object>) show.get("latestMedia");
                // if (!latestMedia.get("mediaType").toString().equals("VIDEO")) {
                // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                // }
                return this.crawlVideo(latestMedia.get("urn").toString());
            } else {
                /* Unsupported content */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            /*
             * TODO: Add crawler for embedded content such as:
             * https://www.srf.ch/news/panorama/gefaelschte-unterschrift-immobilienfirma-betreibt-ahnungslose-frau
             */
        }
        String fpName = br.getRegex("").getMatch(0);
        final String[] links = br.getRegex("").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            ret.add(createDownloadlink(singleLink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlVideo(final String urn) throws Exception {
        if (StringUtils.isEmpty(urn)) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* xml also possible: http://il.srgssr.ch/integrationlayer/1.0/<channelname>/srf/video/play/<videoid>.xml */
        this.br.getPage("https://il.srgssr.ch/integrationlayer/2.0/mediaComposition/byUrn/" + urn + ".json?onlyChapters=false&vector=portalplay");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // TODO
        final Map<String, Object> root = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final List<Object> chapterList = (List<Object>) root.get("chapterList");
        if (chapterList.size() != 1) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> mediaInfo = (Map<String, Object>) chapterList.get(0);
        final String title = (String) mediaInfo.get("title");
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String dateFormatted = new Regex(mediaInfo.get("date"), "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String ext;
        final String mediaType = (String) mediaInfo.get("mediaType");
        if (mediaType.equalsIgnoreCase("AUDIO")) {
            ext = ".mp3";
        } else if (mediaType.equalsIgnoreCase("VIDEO")) {
            ext = ".mp4";
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported mediaType:" + mediaType);
        }
        final DownloadLink link = this.createDownloadlink("TODO");
        link.setFinalFileName(dateFormatted + "_" + mediaInfo.get("vendor") + "_" + title + ext);
        String description = (String) mediaInfo.get("description");
        if (description == null) {
            description = (String) JavaScriptEngineFactory.walkJson(root, "show/description");
        }
        if (StringUtils.isEmpty(link.getComment()) && !StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        final String officialDownloadurl = (String) mediaInfo.get("podcastHdUrl");
        return ret;
    }
}
