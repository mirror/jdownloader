//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.BrDeConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "br-online.de" }, urls = { "https?://(?:www\\.)?br\\.de/.+" })
public class BrDeDecrypter extends PluginForDecrypt {
    /* only keep best quality , do not change the ORDER */
    private static final String[] all_possible_qualities = { "X", "C", "E", "B", "A", "0" };

    public BrDeDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        boolean offline = false;
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        final String videoID = new Regex(parameter, "av:([a-f0-9]{24})").getMatch(0);
        if (videoID == null) {
            /*
             * 2019-12-13: Legacy handling for old XML way e.g.
             * https://www.br.de/telekolleg/faecher/englisch/telekolleg-englisch-out-about100.html
             */
            return crawlOldContent(parameter, decryptedLinks);
        }
        br.getHeaders().put("Content-Type", "application/json");
        /* This is important! */
        br.getHeaders().put("Referer", String.format("https://www.%s/mediathek//video/av:%s", this.getHost(), videoID));
        br.postPageRaw("https://api.mediathek.br.de/graphql/relayBatch",
                "[{\"id\":\"DetailPageRendererQuery\",\"query\":\"query DetailPageRendererQuery(  $clipId: ID!) {  video: node(id: $clipId) {    __typename    ...DetailPage_video    id  }}fragment BookmarkAction_clip on ClipInterface {  id  bookmarked}fragment ChildContentRedirect_creativeWork on CreativeWorkInterface {  categories(first: 100) {    edges {      node {        __typename        id      }    }  }}fragment ClipActions_clip on ClipInterface {  id  bookmarked  downloadable  ...BookmarkAction_clip  ...Rate_clip  ...Share_clip  ...Download_clip}fragment ClipInfo_clip on ClipInterface {  __typename  id  title  kicker  description  shortDescription  availableUntil  versionFrom  ...Subtitles_clip  ...Duration_clip  ...FSKInfo_clip  ...RelatedContent_clip  ...ExternalLinks_clip  ... on ProgrammeInterface {    episodeNumber    initialScreening {      __typename      start      publishedBy {        __typename        name        id      }      id    }    episodeOf {      __typename      description      id      title      scheduleInfo      ...SubscribeAction_series      ... on CreativeWorkInterface {        ...LinkWithSlug_creativeWork      }    }  }  ... on ItemInterface {    itemOf(first: 1) {      edges {        node {          __typename          versionFrom          initialScreening {            __typename            start            publishedBy {              __typename              name              id            }            id          }          episodeOf {            __typename            id            title            scheduleInfo            ...SubscribeAction_series            ... on CreativeWorkInterface {              ...LinkWithSlug_creativeWork            }          }          id        }      }    }  }}fragment DetailPage_video on Node {  ...VideoPlayer_video  ... on ClipInterface {    id    title    kicker    slug    shortDescription    description    status {      __typename      id    }    ...ClipActions_clip    ...ClipInfo_clip    ...ChildContentRedirect_creativeWork  }}fragment Download_clip on ClipInterface {  videoFiles(first: 10) {    edges {      node {        __typename        publicLocation        videoProfile {          __typename          height          id        }        id      }    }  }}fragment Duration_clip on ClipInterface {  duration}fragment Error_clip on ClipInterface {  ageRestriction  ... on ProgrammeInterface {    availableUntil    initialScreening {      __typename      start      end      publishedBy {        __typename        name        id      }      id    }  }}fragment ExternalLinks_clip on ClipInterface {  relatedLinks(first: 20) {    edges {      node {        __typename        id        label        url      }    }  }}fragment FSKInfo_clip on ClipInterface {  ageRestriction}fragment LinkWithSlug_creativeWork on CreativeWorkInterface {  id  slug}fragment Rate_clip on ClipInterface {  id  reactions {    likes    dislikes  }  myInteractions {    __typename    reaction {      __typename      id    }    id  }}fragment RelatedContent_clip on ClipInterface {  __typename  title  kicker  ...Duration_clip  ...TeaserImage_creativeWorkInterface  ... on ProgrammeInterface {    episodeNumber    versionFrom    initialScreening {      __typename      start      id    }    items(first: 30, filter: {essences: {empty: {eq: false}}, status: {id: {eq: \\\"av:http://ard.de/ontologies/lifeCycle#published\\\"}}}) {      edges {        node {          __typename          title          kicker          ...Duration_clip          ...TeaserImage_creativeWorkInterface          ...LinkWithSlug_creativeWork          id        }      }    }    episodeOf {      __typename      title      kicker      ...LinkWithSlug_creativeWork      id    }    moreEpisodes: siblings(next: 2, previous: 1, filter: {essences: {empty: {eq: false}}, status: {id: {eq: \\\"av:http://ard.de/ontologies/lifeCycle#published\\\"}}}) {      current      node {        __typename        title        kicker        episodeNumber        versionFrom        initialScreening {          __typename          start          id        }        ...Duration_clip        ...TeaserImage_creativeWorkInterface        ...LinkWithSlug_creativeWork        id      }    }  }  ... on ItemInterface {    moreItems: siblings(next: 25, previous: 25, filter: {essences: {empty: {eq: false}}, status: {id: {eq: \\\"av:http://ard.de/ontologies/lifeCycle#published\\\"}}}) {      current      node {        __typename        title        kicker        itemOf(first: 1) {          edges {            node {              __typename              versionFrom              initialScreening {                __typename                start                id              }              id            }          }        }        ...Duration_clip        ...TeaserImage_creativeWorkInterface        ...LinkWithSlug_creativeWork        id      }    }    itemOf(first: 1) {      edges {        node {          __typename          title          kicker          versionFrom          initialScreening {            __typename            start            id          }          ...Duration_clip          ...TeaserImage_creativeWorkInterface          ...LinkWithSlug_creativeWork          episodeOf {            __typename            title            kicker            ...LinkWithSlug_creativeWork            id          }          id        }      }    }  }}fragment Settings_clip on ClipInterface {  videoFiles(first: 10) {    edges {      node {        __typename        id        mimetype        publicLocation        videoProfile {          __typename          id          width          height        }      }    }  }}fragment Share_clip on ClipInterface {  title  id  embeddable  embedCode  canonicalUrl}fragment SubscribeAction_series on SeriesInterface {  id  subscribed}fragment Subtitles_clip on ClipInterface {  videoFiles(first: 10) {    edges {      node {        __typename        subtitles {          edges {            node {              __typename              timedTextFiles(filter: {mimetype: {eq: \\\"text/vtt\\\"}}) {                edges {                  node {                    __typename                    publicLocation                    id                  }                }              }              id            }          }        }        id      }    }  }}fragment TeaserImage_creativeWorkInterface on CreativeWorkInterface {  id  defaultTeaserImage {    __typename    shortDescription    copyright    imageFiles(first: 1) {      edges {        node {          __typename          id          publicLocation          crops(first: 1, filter: {format: ASPECT_RATIO_16_9}) {            count            edges {              node {                __typename                publicLocation                width                height                id              }            }          }        }      }    }    id  }}fragment Track_clip on ClipInterface {  videoFiles(first: 10) {    edges {      node {        __typename        publicLocation        subtitles {          edges {            node {              id              language              closed              __typename              timedTextFiles(filter: {mimetype: {eq: \\\"text/vtt\\\"}}) {                edges {                  node {                    __typename                    id                    mimetype                    publicLocation                  }                }              }            }          }        }        id      }    }  }}fragment VideoPlayer_video on Node {  id  type: __typename  ... on ClipInterface {    title    ageRestriction    chromecastEntity    videoFiles(first: 10) {      edges {        node {          __typename          id          mimetype          publicLocation          videoProfile {            __typename            id            width          }        }      }    }    ...Track_clip    ...Error_clip    ...Settings_clip    defaultTeaserImage {      __typename      imageFiles(first: 1) {        edges {          node {            __typename            id            publicLocation          }        }      }      id    }    myInteractions {      __typename      completed      progress      id    }  }  ... on ProgrammeInterface {    liveBroadcasts: broadcasts(filter: {start: {lte: \\\"now\\\"}}, orderBy: START_ASC) {      edges {        node {          __typename          start          end          broadcastedOn(first: 1) {            edges {              node {                __typename                id                type: __typename                streamingUrls(first: 10, filter: {hasEmbeddedSubtitles: {eq: false}}) {                  edges {                    node {                      __typename                      id                      publicLocation                      hasEmbeddedSubtitles                    }                  }                }              }            }          }          id        }      }    }    futureBroadcasts: broadcasts(filter: {end: {gte: \\\"now\\\"}}, orderBy: START_DESC) {      edges {        node {          __typename          start          end          broadcastedOn(first: 1) {            edges {              node {                __typename                id                type: __typename                streamingUrls(first: 10, filter: {hasEmbeddedSubtitles: {eq: false}}) {                  edges {                    node {                      __typename                      id                      publicLocation                      hasEmbeddedSubtitles                    }                  }                }              }            }          }          id        }      }    }  }  ... on LivestreamInterface {    streamingUrls(first: 10, filter: {hasEmbeddedSubtitles: {eq: false}}) {      edges {        node {          __typename          id          publicLocation          hasEmbeddedSubtitles        }      }    }  }}\",\"variables\":{\"clipId\":\"av:"
                        + videoID + "\"}}]");
        if (offline || this.br.getHttpConnection().getResponseCode() == 404) {
            /* Add offline link so user can see it */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = null;
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(ressourcelist.get(0), "data/video");
        final String type = (String) entries.get("__typename");
        /* 2019-12-13: Hm not yet sure about that ... it could fail for valid video items too! */
        final boolean is_video = type != null && (type.equalsIgnoreCase("Item") || type.equalsIgnoreCase("Programme"));
        if (type != null && !is_video) {
            /* E.g. "Series" or other non-video objects */
            logger.info("Failed to find any downloadable content");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String date = (String) JavaScriptEngineFactory.walkJson(entries, "initialScreening/start");
        if (StringUtils.isEmpty(date)) {
            /*
             * 2019-12-13: E.g.
             * https://www.br.de/mediathek/video/basteln-mit-kindern-krippe-aus-naturmaterialien-av:5dea974cf48951001a0b38fa
             */
            date = (String) entries.get("versionFrom");
        }
        String show = (String) JavaScriptEngineFactory.walkJson(entries, "episodeOf/title");
        String title = (String) entries.get("title");
        final ArrayList<Object> qualities = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "videoFiles/edges");
        if (qualities == null || qualities.isEmpty() || title == null) {
            /*
             * Probably not a downloadable video e.g. LIVE-streams:
             * https://www.br.de/mediathek/video/bergwetter-und-alpenblick-panoramabilder-live-av:5da6d8987c69d4001a35a8d1
             */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        HashMap<String, DownloadLink> best_map = new HashMap<String, DownloadLink>();
        HashMap<String, DownloadLink> tmpBestMap = new HashMap<String, DownloadLink>();
        final BrDeConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.BrDeConfigInterface.class);
        final boolean grab_subtitle = cfg.isGrabSubtitleEnabled();
        final boolean grabBEST = cfg.isGrabBESTEnabled();
        final boolean fast_linkcheck = cfg.isFastLinkcheckEnabled();
        String date_formatted = null;
        if (!StringUtils.isEmpty(date)) {
            /* Date is not always given */
            date_formatted = formatDate(date);
        }
        title = encodeUnicode(Encoding.htmlDecode(title).trim());
        if (!StringUtils.isEmpty(show)) {
            /* Show is not always given */
            show = encodeUnicode(Encoding.htmlDecode(show).trim());
        }
        String subtitle_url = null;
        for (final Object videoO : qualities) {
            entries = (LinkedHashMap<String, Object>) videoO;
            entries = (LinkedHashMap<String, Object>) entries.get("node");
            final String final_url = (String) entries.get("publicLocation");
            if (StringUtils.isEmpty(final_url)) {
                /* Skip bad items */
                continue;
            } else if (final_url.contains(".m3u8")) {
                /* 2019-12-13: Skip hls - HTTP URLs are always available! */
                continue;
            }
            /* 2019-12-13: These quality identifiers haven't changed when they changed their website layout the last time! */
            final String q_string = new Regex(final_url, "_(0|A|B|C|D|E|X)\\.mp4").getMatch(0);
            final String width = Long.toString(JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "videoProfile/width"), 0));
            final String height = Long.toString(JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "videoProfile/height"), 0));
            final String resolution = width + "x" + height;
            if (q_string == null || width.equals("0") || width.equals("0")) {
                /* Skip invalid entries */
                continue;
            }
            /*
             * 2019-12-13: Every quality contains an entry for subtitles but of course all lead to the same URL so we'll only grab that
             * once!
             */
            if (subtitle_url == null) {
                try {
                    subtitle_url = (String) JavaScriptEngineFactory.walkJson(entries, "subtitles/edges/{0}/node/timedTextFiles/edges/{0}/node/publicLocation");
                } catch (final Throwable e) {
                }
            }
            String final_video_name = "";
            if (date_formatted != null) {
                final_video_name += date_formatted + "_";
            }
            final_video_name += "br";
            if (show != null) {
                final_video_name += " - " + show;
            }
            final_video_name += " - " + title + "_" + resolution + ".mp4";
            final DownloadLink dl_video = createDownloadlink("http://brdecrypted-online.de/?format=mp4&quality=" + resolution + "&hash=" + videoID);
            dl_video.setLinkID(getHost() + "://" + videoID + "/" + q_string + "/" + resolution);
            dl_video.setProperty("mainlink", parameter);
            dl_video.setProperty("direct_link", final_url);
            dl_video.setProperty("plain_filename", final_video_name);
            dl_video.setProperty("plain_resolution", resolution);
            dl_video.setContentUrl(parameter);
            dl_video.setFinalFileName(final_video_name);
            /* 2019-12-13: Filesize is not given via json anymore - check URLs to find filesize! */
            if (fast_linkcheck) {
                dl_video.setAvailable(true);
            }
            best_map.put(q_string, dl_video);
            newRet.add(dl_video);
        }
        if (newRet.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        boolean atLeastOneSelectedQualityExists = false;
        ArrayList<String> selected_qualities = new ArrayList<String>();
        if (newRet.size() > 1 && grabBEST) {
            tmpBestMap = findBESTInsideGivenMap(best_map);
            if (!tmpBestMap.isEmpty()) {
                atLeastOneSelectedQualityExists = true;
                best_map = tmpBestMap;
            }
        } else {
            boolean grab_0 = cfg.isGrabHTTPMp4XSVideoEnabled();
            boolean grab_A = cfg.isGrabHTTPMp4SVideoEnabled();
            boolean grab_B = cfg.isGrabHTTPMp4MVideoEnabled();
            boolean grab_C = cfg.isGrabHTTPMp4LVideoEnabled();
            boolean grab_E = cfg.isGrabHTTPMp4XLVideoEnabled();
            boolean grab_X = cfg.isGrabHTTPMp4XXLVideoEnabled();
            /* User deselected all --> Add all */
            if (!grab_0 && !grab_A && !grab_B && !grab_C && !grab_E && !grab_X) {
                grab_0 = true;
                grab_A = true;
                grab_B = true;
                grab_C = true;
                grab_E = true;
                grab_X = true;
            }
            if (grab_X) {
                selected_qualities.add("X");
            }
            if (grab_E) {
                selected_qualities.add("E");
            }
            if (grab_C) {
                selected_qualities.add("C");
            }
            if (grab_B) {
                selected_qualities.add("B");
            }
            if (grab_A) {
                selected_qualities.add("A");
            }
            if (grab_0) {
                selected_qualities.add("0");
            }
            for (final String selected_quality : selected_qualities) {
                if (best_map.containsKey(selected_quality)) {
                    if (!atLeastOneSelectedQualityExists) {
                        atLeastOneSelectedQualityExists = true;
                    }
                    tmpBestMap.put(selected_quality, best_map.get(selected_quality));
                }
            }
            if (cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled() && atLeastOneSelectedQualityExists) {
                /* Select highest quality inside user selected qualities. */
                best_map = findBESTInsideGivenMap(tmpBestMap);
            } else {
                best_map = tmpBestMap;
            }
        }
        if (!atLeastOneSelectedQualityExists) {
            selected_qualities = (ArrayList<String>) Arrays.asList(all_possible_qualities);
        }
        final Iterator<Entry<String, DownloadLink>> it = best_map.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> entry = it.next();
            final DownloadLink keep = entry.getValue();
            if (keep != null) {
                /* Add subtitle link for every quality so players will automatically find it */
                if (grab_subtitle && !StringUtils.isEmpty(subtitle_url)) {
                    final String subtitle_filename = date_formatted + "_br_" + show + " - " + title + "_" + keep.getStringProperty("plain_resolution", null) + ".vtt";
                    final String resolution = keep.getStringProperty("plain_resolution", null);
                    final DownloadLink dl_subtitle = createDownloadlink("http://brdecrypted-online.de/?format=xml&quality=" + resolution + "&hash=" + videoID);
                    final String linkID = keep.getSetLinkID();
                    if (linkID != null) {
                        dl_subtitle.setLinkID(linkID + "/subtitle");
                    }
                    dl_subtitle.setProperty("mainlink", parameter);
                    dl_subtitle.setProperty("direct_link", subtitle_url);
                    dl_subtitle.setProperty("plain_filename", subtitle_filename);
                    dl_subtitle.setProperty("streamingType", "subtitle");
                    dl_subtitle.setContentUrl(parameter);
                    /* Do not check for filesize of subtitles as it can usually be downloaded in less than a second! */
                    dl_subtitle.setAvailable(true);
                    dl_subtitle.setFinalFileName(subtitle_filename);
                    decryptedLinks.add(dl_subtitle);
                }
                decryptedLinks.add(keep);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        String packagename = "";
        if (date_formatted != null) {
            packagename = date_formatted + "_";
        }
        packagename += "br";
        if (show != null) {
            packagename += " - " + show;
        }
        packagename += " - " + title;
        fp.setName(packagename);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /**
     * 2019-12-13: This is the nearly unmodified crawl code of revision 40970. Parts of the br.de website are still running on the old
     * system e.g. https://www.br.de/telekolleg/faecher/englisch/telekolleg-englisch-out-about100.html
     *
     * @throws IOException
     */
    private ArrayList<DownloadLink> crawlOldContent(final String parameter, final ArrayList<DownloadLink> decryptedLinks) throws IOException {
        if (parameter.contains("/livestream/")) {
            /* Invalid URLs. */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || parameter.matches("https?://(?:www\\.)?br\\.de/mediathek/video/index\\.html")) {
            /* Add offline link so user can see it */
            final DownloadLink dl = this.createOfflinelink(parameter);
            String offline_name = new Regex(parameter, "br\\.de/(.+)\\.html$").getMatch(0);
            if (offline_name != null) {
                dl.setFinalFileName(offline_name);
            }
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String player_link = br.getRegex("\\{dataURL:\\'(/[^<>\"]*?)\\'\\}").getMatch(0);
        String date = br.getRegex(">(\\d{2}\\.\\d{2}\\.\\d{4}), \\d{2}:\\d{2} Uhr,?</time>").getMatch(0);
        if (player_link == null) {
            logger.info("URL does not lead to any downloadable content");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String playerLinkID = JDHash.getMD5(player_link);
        br.getPage("http://www.br.de" + player_link);
        if (date == null) {
            date = getXML("broadcastDate");
        }
        String show = getXML("broadcast");
        String plain_name = this.getXML("shareTitle");
        final String[] qualities = br.getRegex("<asset type=(.*?)</asset>").getColumn(0);
        if (qualities == null || qualities.length == 0 || plain_name == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        HashMap<String, DownloadLink> best_map = new HashMap<String, DownloadLink>();
        HashMap<String, DownloadLink> tmpBestMap = new HashMap<String, DownloadLink>();
        final BrDeConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.BrDeConfigInterface.class);
        final boolean grab_subtitle = cfg.isGrabSubtitleEnabled();
        final boolean grabBEST = cfg.isGrabBESTEnabled();
        String date_formatted = null;
        if (!StringUtils.isEmpty(date)) {
            /* Date is not always given */
            date_formatted = formatDateOLD(date);
        }
        if (show == null) {
            /* Show is not always given */
            show = "-";
        }
        plain_name = encodeUnicode(Encoding.htmlDecode(plain_name).trim()).replace("\n", "");
        show = encodeUnicode(Encoding.htmlDecode(show).trim());
        String subtitle_url = br.getRegex("<dataTimedText url=\"(/mediathek/video/untertitel[^<>\"/]+\\.xml)\"").getMatch(0);
        if (subtitle_url != null) {
            subtitle_url = "http://www.br.de" + Encoding.htmlDecode(subtitle_url);
        }
        boolean found_supported_format = false;
        for (final String qinfo : qualities) {
            String final_url = this.getXML(qinfo, "downloadUrl");
            /* Avoid HDS */
            if (final_url == null) {
                continue;
            } else if (final_url.contains(".m3u8") || final_url.contains(".f4m")) {
                /* Skip unsupported formats & livestreams */
                continue;
            }
            found_supported_format = true;
            if (final_url.startsWith("//")) {
                /* 2019-12-13: E.g. https://www.br.de/telekolleg/faecher/englisch/telekolleg-englisch-out-about100.html */
                final_url = "http:" + final_url;
            }
            final String q_string = new Regex(final_url, "_(0|A|B|C|D|E|X)\\.mp4").getMatch(0);
            final String width = this.getXML(qinfo, "frameWidth");
            final String height = this.getXML(qinfo, "frameHeight");
            final String fsize = this.getXML(qinfo, "size");
            final String resolution = width + "x" + height;
            String final_video_name = "";
            if (date_formatted != null) {
                final_video_name = date_formatted + "_";
            }
            final_video_name += "br_" + show + " - " + plain_name + "_" + resolution + ".mp4";
            final DownloadLink dl_video = createDownloadlink("http://brdecrypted-online.de/?format=mp4&quality=" + resolution + "&hash=" + playerLinkID);
            dl_video.setLinkID(getHost() + "://" + playerLinkID + "/" + q_string + "/" + resolution);
            dl_video.setProperty("mainlink", parameter);
            dl_video.setProperty("direct_link", final_url);
            dl_video.setProperty("plain_filename", final_video_name);
            dl_video.setProperty("plain_resolution", resolution);
            dl_video.setContentUrl(parameter);
            dl_video.setFinalFileName(final_video_name);
            dl_video.setDownloadSize(Long.parseLong(fsize));
            dl_video.setAvailable(true);
            best_map.put(q_string, dl_video);
            newRet.add(dl_video);
        }
        if (!found_supported_format) {
            /* 2019-12-13: E.g. http://www.br.de/fernsehen/ard-alpha/index.html */
            logger.info("Failed to find any downloadable content");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (newRet.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        boolean atLeastOneSelectedQualityExists = false;
        ArrayList<String> selected_qualities = new ArrayList<String>();
        if (newRet.size() > 1 && grabBEST) {
            tmpBestMap = findBESTInsideGivenMap(best_map);
            if (!tmpBestMap.isEmpty()) {
                atLeastOneSelectedQualityExists = true;
                best_map = tmpBestMap;
            }
        } else {
            boolean grab_0 = cfg.isGrabHTTPMp4XSVideoEnabled();
            boolean grab_A = cfg.isGrabHTTPMp4SVideoEnabled();
            boolean grab_B = cfg.isGrabHTTPMp4MVideoEnabled();
            boolean grab_C = cfg.isGrabHTTPMp4LVideoEnabled();
            boolean grab_E = cfg.isGrabHTTPMp4XLVideoEnabled();
            boolean grab_X = cfg.isGrabHTTPMp4XXLVideoEnabled();
            /* User deselected all --> Add all */
            if (!grab_0 && !grab_A && !grab_B && !grab_C && !grab_E && !grab_X) {
                grab_0 = true;
                grab_A = true;
                grab_B = true;
                grab_C = true;
                grab_E = true;
                grab_X = true;
            }
            if (grab_X) {
                selected_qualities.add("X");
            }
            if (grab_E) {
                selected_qualities.add("E");
            }
            if (grab_C) {
                selected_qualities.add("C");
            }
            if (grab_B) {
                selected_qualities.add("B");
            }
            if (grab_A) {
                selected_qualities.add("A");
            }
            if (grab_0) {
                selected_qualities.add("0");
            }
            for (final String selected_quality : selected_qualities) {
                if (best_map.containsKey(selected_quality)) {
                    if (!atLeastOneSelectedQualityExists) {
                        atLeastOneSelectedQualityExists = true;
                    }
                    tmpBestMap.put(selected_quality, best_map.get(selected_quality));
                }
            }
            if (cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled() && atLeastOneSelectedQualityExists) {
                /* Select highest quality inside user selected qualities. */
                best_map = findBESTInsideGivenMap(tmpBestMap);
            } else {
                best_map = tmpBestMap;
            }
        }
        if (!atLeastOneSelectedQualityExists) {
            selected_qualities = (ArrayList<String>) Arrays.asList(all_possible_qualities);
        }
        final Iterator<Entry<String, DownloadLink>> it = best_map.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> entry = it.next();
            final DownloadLink keep = entry.getValue();
            if (keep != null) {
                /* Add subtitle link for every quality so players will automatically find it */
                if (grab_subtitle && subtitle_url != null) {
                    final String subtitle_filename = date_formatted + "_br_" + show + " - " + plain_name + "_" + keep.getStringProperty("plain_resolution", null) + ".xml";
                    final String resolution = keep.getStringProperty("plain_resolution", null);
                    final DownloadLink dl_subtitle = createDownloadlink("http://brdecrypted-online.de/?format=xml&quality=" + resolution + "&hash=" + playerLinkID);
                    final String linkID = keep.getSetLinkID();
                    if (linkID != null) {
                        dl_subtitle.setLinkID(linkID + "/subtitle");
                    }
                    dl_subtitle.setProperty("mainlink", parameter);
                    dl_subtitle.setProperty("direct_link", subtitle_url);
                    dl_subtitle.setProperty("plain_filename", subtitle_filename);
                    dl_subtitle.setProperty("streamingType", "subtitle");
                    dl_subtitle.setContentUrl(parameter);
                    dl_subtitle.setAvailable(true);
                    dl_subtitle.setFinalFileName(subtitle_filename);
                    decryptedLinks.add(dl_subtitle);
                }
                decryptedLinks.add(keep);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        String packagename = "";
        if (date_formatted != null) {
            packagename = date_formatted + "_";
        }
        packagename += "br_" + show + " - " + plain_name;
        fp.setName(packagename);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /** TODO: Remove this once we do not need the legacy handling anymore! */
    private String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "><\\!\\[CDATA\\[([^<>\"]*?)\\]\\]></" + parameter + ">").getMatch(0);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
        }
        return result;
    }

    /** TODO: Remove this once we do not need the legacy handling anymore! */
    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    /** TODO: Remove this once we do not need the legacy handling anymore! */
    private String formatDateOLD(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> bestMap) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (bestMap.size() > 0) {
            for (final String quality : all_possible_qualities) {
                keep = bestMap.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = bestMap;
        }
        return newMap;
    }

    private String formatDate(final String input) {
        String ret = new Regex(input, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        if (ret == null) {
            /* Fallback */
            ret = input;
        }
        return ret;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}