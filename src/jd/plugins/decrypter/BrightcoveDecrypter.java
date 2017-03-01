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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "brightcove.com" }, urls = { "https?://c\\.brightcove\\.com/services/viewer/htmlFederated\\?.+" })
public class BrightcoveDecrypter extends PluginForDecrypt {

    public BrightcoveDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * General plugin to decrypt html versions of videos hosted via https://www.brightcove.com/de/ SLASH AKAMAI.
     *
     * TODO: Maybe add thumbnail support, maybe add HLS/HDS support
     * */
    @SuppressWarnings("unchecked")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // Keep this in mind for future implementations: http://c.brightcove.com/services/mobile/streaming/index/master.m3u8?videoId= +
        // videoid
        // final String brightcove_URL = "http://c.brightcove.com/services/viewer/htmlFederated?&width=340&height=192&flashID=" + flashID +
        // "&includeAPI=true&templateLoadHandler=templateLoaded&templateReadyHandler=playerReady&bgcolor=%23FFFFFF&htmlFallback=true&playerID="
        // + playerID + "&publisherID=" + publisherID + "&playerKey=" + Encoding.urlEncode(playerKey) +
        // "&isVid=true&isUI=true&dynamicStreaming=true&optimizedContentLoad=true&wmode=transparent&%40videoPlayer=" + videoID +
        // "&allowScriptAccess=always";
        // this.br.getPage(brightcove_URL);

        final String json = getSourceJson(this.br);
        if (json == null) {
            return null;
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final String publisherName = getPublisherName(entries);
        final String title = getTitle(entries);
        final long creationDate = getCreationDate(entries);
        final String date_formatted = formatDate(creationDate);

        final ArrayList<BrightcoveClipData> media = findAllQualities(this, entries);
        for (final BrightcoveClipData clip : media) {
            final String final_filename = date_formatted + "_" + publisherName + "_" + title + "_" + clip.width + "x" + clip.height + "_" + clip.videoCodec + clip.ext;
            final DownloadLink dl = this.createDownloadlink("directhttp://" + clip.downloadurl);
            dl.setDownloadSize(clip.size);
            dl.setAvailable(true);
            dl.setFinalFileName(final_filename);
            dl.setContentUrl(clip.downloadurl);
            decryptedLinks.add(dl);
        }

        String fpName = date_formatted + "_" + publisherName + "_" + title;

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static ArrayList<BrightcoveClipData> findAllQualities(final Plugin plugin, final LinkedHashMap<String, Object> map) {
        final ArrayList<BrightcoveClipData> media = new ArrayList<BrightcoveClipData>();
        final String publisherName = plugin.encodeUnicode(getPublisherName(map));
        final String title = plugin.encodeUnicode(getTitle(map));
        final long creationDate = getCreationDate(map);
        LinkedHashMap<String, Object> entries = null;
        final ArrayList<Object> resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(map, "data/programmedContent/videoPlayer/mediaDTO/renditions");
        for (final Object o : resource_data_list) {
            entries = (LinkedHashMap<String, Object>) o;
            /* audioOnly == true = untested case */
            // final boolean audioOnly = ((Boolean) entries.get("audioOnly")).booleanValue();
            // if (audioOnly) {
            // continue;
            // }
            final String videoCodec = (String) entries.get("videoCodec");
            final String downloadurl = (String) entries.get("defaultURL");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
            final long encodingRate = JavaScriptEngineFactory.toLong(entries.get("encodingRate"), -1);
            final long frameWidth = JavaScriptEngineFactory.toLong(entries.get("frameWidth"), -1);
            final long frameHeight = JavaScriptEngineFactory.toLong(entries.get("frameHeight"), -1);
            final long mediaDeliveryType = JavaScriptEngineFactory.toLong(entries.get("mediaDeliveryType"), -1);

            if (videoCodec == null || downloadurl == null || filesize == -1 || encodingRate == -1 || frameWidth == -1 || frameHeight == -1 || mediaDeliveryType == -1) {
                return null;
            }

            final BrightcoveClipData clip = new BrightcoveClipData();
            clip.setCreationDate(creationDate);
            clip.setPublisherName(publisherName);
            clip.setDisplayname(title);
            clip.setFilesize(filesize);
            clip.setEncodingRate(encodingRate);
            clip.setWidth((int) frameWidth);
            clip.setHeight((int) frameHeight);
            clip.setVideoCodec(videoCodec);
            clip.setDownloadURL(downloadurl);
            clip.setMediaDeliveryType(mediaDeliveryType);
            media.add(clip);
        }
        return media;
    }

    public static String getBrightcoveMobileHLSUrl() {
        return "http://c.brightcove.com/services/mobile/streaming/index/master.m3u8?videoId=";
    }

    /** Finds the highest video quality based on the max filesize. */
    @SuppressWarnings("unchecked")
    public static BrightcoveClipData findBestVideoHttpByFilesize(final Plugin plugin, final Browser br) {
        final String json = getSourceJson(br);
        if (json == null) {
            return null;
        }
        LinkedHashMap<String, Object> map = null;
        try {
            map = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        } catch (final Throwable e) {
            return null;
        }
        return findBestVideoByFilesize(plugin, map);
    }

    /** Finds the highest video quality based on the max filesize. */
    public static BrightcoveClipData findBestVideoByFilesize(final Plugin plugin, final LinkedHashMap<String, Object> map) {
        final ArrayList<BrightcoveClipData> media = findAllQualities(plugin, map);
        return findBestVideoByFilesize(media);
    }

    /** Finds the highest video quality based on the max filesize. */
    public static BrightcoveClipData findBestVideoByFilesize(final ArrayList<BrightcoveClipData> media) {
        if (media == null) {
            return null;
        }
        BrightcoveClipData best = null;
        long filesize_highest = 0;
        for (final BrightcoveClipData clip : media) {
            final long filesize_temp = clip.size;
            if (filesize_temp > filesize_highest) {
                filesize_highest = filesize_temp;
                best = clip;
            }
        }
        return best;
    }

    public static String getSourceJson(final Browser br) {
        return br.getRegex("var experienceJSON = (\\{.*?);\r").getMatch(0);
    }

    public static String getPublisherName(final LinkedHashMap<String, Object> map) {
        return (String) JavaScriptEngineFactory.walkJson(map, "data/programmedContent/videoPlayer/mediaDTO/publisherName");
    }

    public static String getTitle(final LinkedHashMap<String, Object> map) {
        return (String) JavaScriptEngineFactory.walkJson(map, "data/programmedContent/videoPlayer/mediaDTO/displayName");
    }

    public static long getCreationDate(final LinkedHashMap<String, Object> map) {
        return JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(map, "data/programmedContent/videoPlayer/mediaDTO/creationDate"), -1);
    }

    private String formatDate(final long date) {
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date);
        }
        return formattedDate;
    }

    public static class BrightcoveClipData {

        private String ext;
        private String displayName;
        private String shortDescription;
        private String publisherName;
        private String videoCodec;
        private String downloadurl;

        private int    width;
        private int    height;
        private int    length;

        private long   creationDate;
        private long   encodingRate;
        private long   size;
        private long   mediaDeliveryType;

        public BrightcoveClipData() {
            ext = ".mp4";
        }

        public String getFileExtension() {
            return this.ext;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getShortDescription() {
            return this.shortDescription;
        }

        public String getPublisherName() {
            return this.publisherName;
        }

        public String getVideoCodec() {
            return this.videoCodec;
        }

        public String getDownloadURL() {
            return this.downloadurl;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public int getLength() {
            return this.length;
        }

        public long getFilesize() {
            return this.size;
        }

        public long getCreationDate() {
            return this.creationDate;
        }

        public long getEncodingRate() {
            return this.encodingRate;
        }

        public long getMediaDeliveryType() {
            return this.mediaDeliveryType;
        }

        public void setFileExtension(final String fileExtension) {
            this.ext = fileExtension;
        }

        public void setDisplayname(final String displayName) {
            this.displayName = displayName;
        }

        public void setShortDescription(final String shortDescription) {
            this.shortDescription = shortDescription;
        }

        public void setPublisherName(final String publisherName) {
            this.publisherName = publisherName;
        }

        public void setVideoCodec(final String videoCodec) {
            this.videoCodec = videoCodec;
        }

        public void setDownloadURL(final String downloadURL) {
            this.downloadurl = downloadURL;
        }

        public void setWidth(final int width) {
            this.width = width;
        }

        public void setHeight(final int height) {
            this.height = height;
        }

        public void setLength(final int length) {
            this.length = length;
        }

        public void setFilesize(final long filesize) {
            this.size = filesize;
        }

        public void setCreationDate(final long creationDate) {
            this.creationDate = creationDate;
        }

        public void setEncodingRate(final long encodingRate) {
            this.encodingRate = encodingRate;
        }

        public void setMediaDeliveryType(final long mediaDeliveryType) {
            this.mediaDeliveryType = mediaDeliveryType;
        }

        @Override
        public String toString() {
            return displayName + "_" + width + "x" + height;
        }

        public String getStandardFilename() {
            return formatDate(this.getCreationDate()) + "_" + this.getPublisherName() + "_" + this.getDisplayName() + "_" + this.getWidth() + "x" + this.getHeight() + "_" + this.getVideoCodec() + this.getFileExtension();
        }

        private String formatDate(final long date) {
            String formattedDate = null;
            final String targetFormat = "yyyy-MM-dd";
            Date theDate = new Date(date);
            try {
                final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
                formattedDate = formatter.format(theDate);
            } catch (Exception e) {
                /* prevent input error killing plugin */
                formattedDate = Long.toString(date);
            }
            return formattedDate;
        }

    }

    public static String brightcoveEdgeRegexIDAccount(final Browser br) {
        return br.getRegex("data\\-account\\s*=\\s*(\"|')(\\d+)\\1").getMatch(1);
    }

    public static String brightcoveEdgeRegexIDVideo(final Browser br) {
        return br.getRegex("data\\-video\\-id\\s*=\\s*(\"|')(\\d+)\\1").getMatch(1);
    }

    public static String getPolicyKey(final Browser br, final String accountID) throws DecrypterException, Exception {
        if (br == null || StringUtils.isEmpty(accountID)) {
            return null;
        }
        final String bcJS = br.getRegex("<script src=(\"|')(//players\\.brightcove\\.net/" + accountID + "/.*?)\\1></script>").getMatch(1);
        if (StringUtils.isEmpty(bcJS)) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        final Browser js = br.cloneBrowser();
        js.getHeaders().put("Accept", "*/*");
        js.getPage(bcJS);
        final String policyKey = PluginJSonUtils.getJson(js, "policyKey");
        return policyKey;
    }

    public static void setAPIHeaders(final Browser br, final String policyKey) {
        br.getHeaders().put("Accept", "application/json;pk=" + policyKey);
    }

    public static String getAPIPlaybackUrl(final String accountID, final String videoID) {
        return String.format("https://edge.api.brightcove.com/playback/v1/accounts/%s/videos/%s", accountID, videoID);
    }

    public static String getHlsMasterHttp(final String videoID) {
        /* Possible additional (not required) parameter: 'pubId' e.g. '&pubId=4013' */
        return String.format("http://c.brightcove.com/services/mobile/streaming/index/master.m3u8?videoId=%s", videoID);
    }

    public static String getHlsMasterHttps(final String videoID) {
        /* Possible additional (not required) parameter: 'pubId' e.g. '&pubId=4013' */
        return String.format("https://secure.brightcove.com/services/mobile/streaming/index/master.m3u8?videoId=%s&secure=true", videoID);
    }

    public static HashMap<String, BrightcoveEdgeContainer> findAllQualitiesAuto(final Browser br) throws DecrypterException, Exception {
        final String accountID = brightcoveEdgeRegexIDAccount(br);
        final String videoID = brightcoveEdgeRegexIDAccount(br);
        final String policyKey = getPolicyKey(br, accountID);
        return findAllQualities(br, accountID, videoID, policyKey, true);
    }

    public static HashMap<String, BrightcoveEdgeContainer> findAllQualitiesAuto(final Browser br, final boolean crawlHLS) throws DecrypterException, Exception {
        final String accountID = brightcoveEdgeRegexIDAccount(br);
        final String videoID = brightcoveEdgeRegexIDAccount(br);
        final String policyKey = getPolicyKey(br, accountID);
        return findAllQualities(br, accountID, videoID, policyKey, crawlHLS);
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, BrightcoveEdgeContainer> findAllQualities(final Browser br, final String accountID, final String referenceID, final String policyKey, final boolean crawlHLS) throws IOException {
        if (br == null || StringUtils.isEmpty(accountID) || StringUtils.isEmpty(referenceID) || StringUtils.isEmpty(policyKey)) {
            return null;
        }
        final String source_host = br.getHost();

        final HashMap<String, BrightcoveEdgeContainer> all_found_qualities = new HashMap<String, BrightcoveEdgeContainer>();
        /* TODO: Brightcove edge parser goes here */
        setAPIHeaders(br, policyKey);
        br.getPage(getAPIPlaybackUrl(accountID, referenceID));
        try {
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> sources = (ArrayList<Object>) entries.get("sources");
            final String name = (String) entries.get("name");
            final String published_at = (String) entries.get("published_at");
            final String created_at = (String) entries.get("created_at");
            String description = null;
            String description_long = null;
            /* The videoID which can be used to build hls-master-urls. */
            String videoID = null;
            String publisherInfo = null;
            try {
                /* Rather unimportant things */
                description = (String) entries.get("description");
                description_long = (String) entries.get("long_description");
                videoID = (String) entries.get("id");
                publisherInfo = (String) JavaScriptEngineFactory.walkJson(entries, "link/url");
            } catch (final Throwable e) {
            }
            final long duration_general = JavaScriptEngineFactory.toLong(entries.get("duration"), 0);

            if (publisherInfo == null || publisherInfo.equals("")) {
                publisherInfo = source_host;
            }
            final String publisher_name = new Regex(publisherInfo, "https?://(?:www\\.)?([^/]+)").getMatch(0).replace(".", "_");

            for (final Object sourceo : sources) {
                entries = (LinkedHashMap<String, Object>) sourceo;
                final long duration = JavaScriptEngineFactory.toLong(entries.get("duration"), 0);
                final long width = JavaScriptEngineFactory.toLong(entries.get("width"), 0);
                final long height = JavaScriptEngineFactory.toLong(entries.get("height"), 0);
                final long size = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                final String codec = (String) entries.get("codec");
                final String src = (String) entries.get("src");
                final String app_name = (String) entries.get("app_name");
                final String type = (String) entries.get("type");

                if (src == null || codec == null || width == 0 || height == 0) {
                    /* Skip invalid items */
                    continue;
                }

                BrightcoveEdgeContainer bcont = null;
                if (type != null && type.equalsIgnoreCase("application/x-mpegURL") || src.contains(".m3u8")) {
                    /* HLS */
                    if (!crawlHLS) {
                        /* Skip HLS if it's not required. */
                        continue;
                    }
                    final Browser brc = br.cloneBrowser();
                    /* Access hls master. */
                    br.getPage(src);
                    final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(brc);
                    if (hlsContainers != null) {
                        for (final HlsContainer hlsCont : hlsContainers) {
                            bcont = new BrightcoveEdgeContainer(accountID, referenceID, name, hlsCont.getHeight(), hlsCont.getWidth());
                            bcont.setDuration(duration_general);
                            bcont.setVideoCodec(codec);
                            bcont.setDownloadURL(hlsCont.getDownloadurl());
                            bcont.setPublishedAT(published_at);
                            bcont.setPublisherName(publisher_name);
                            bcont.setCreatedAT(created_at);
                            bcont.setDescription(description);
                            bcont.setDescriptionLong(description_long);
                            /* TODO: Set protocol!, What to do with multiple same widths but different/higher "BANDWIDTH" values?? */
                            all_found_qualities.put(Integer.toString(hlsCont.getWidth()), bcont);
                        }
                    }

                } else if (src.startsWith("rtmp") || src.startsWith("http")) {
                    bcont = new BrightcoveEdgeContainer(accountID, referenceID, name, (int) height, (int) width);
                    bcont.setFilesize(size);
                    bcont.setDuration(duration);
                    bcont.setVideoCodec(codec);
                    bcont.setDownloadURL(src);
                    bcont.setPublishedAT(published_at);
                    bcont.setPublisherName(publisher_name);
                    bcont.setCreatedAT(created_at);
                    bcont.setDescription(description);
                    bcont.setDescriptionLong(description_long);
                    /* TODO: Set protocol! */
                    if (src.startsWith("rtmp")) {
                        bcont.setAppName(app_name);
                    } else {
                    }
                    all_found_qualities.put(Long.toString(width), bcont);
                } else {
                    /* Skip unknown formats/protocols */
                    continue;
                }
            }

        } catch (final Throwable e) {
        }
        return all_found_qualities;
    }

    public static class BrightcoveEdgeContainer {

        private String ext;
        private String account_id;
        private String reference_id;

        private String name;
        private String description;
        private String descriptionLong;
        private String published_at;
        private String created_at;

        private String publisherName;
        private String videoCodec;
        private String downloadurl;
        private String app_name;

        private long   width;
        private long   height;
        private long   length;
        private long   size;

        public void init() {
            ext = ".mp4";
        }

        public BrightcoveEdgeContainer() {
            init();
        }

        public BrightcoveEdgeContainer(final String accountID, final String referenceID) {
            init();
            this.setAccountID(accountID);
            this.setReferenceID(referenceID);
        }

        public BrightcoveEdgeContainer(final String accountID, final String referenceID, final String name, final int height, final int width) {
            init();
            this.setHeight(height);
            this.setWidth(width);
        }

        public String getFileExtension() {
            return this.ext;
        }

        public String getAccountID() {
            return this.account_id;
        }

        public String getReferenceID() {
            return this.reference_id;
        }

        public String getName() {
            return this.name;
        }

        public String getDescription() {
            return this.description;
        }

        public String getDescriptionLong() {
            return this.descriptionLong;
        }

        public String getPublishedAT() {
            return this.published_at;
        }

        public String getCreatedAT() {
            return this.created_at;
        }

        public String getPublisherName() {
            return this.publisherName;
        }

        public String getVideoCodec() {
            return this.videoCodec;
        }

        public String getDownloadURL() {
            return this.downloadurl;
        }

        public String getAppName() {
            return this.app_name;
        }

        public long getWidth() {
            return this.width;
        }

        public long getHeight() {
            return this.height;
        }

        public long getDuration() {
            return this.length;
        }

        public long getFilesize() {
            return this.size;
        }

        public void setFileExtension(final String fileExtension) {
            this.ext = fileExtension;
        }

        public void setAccountID(final String accountID) {
            this.account_id = accountID;
        }

        public void setReferenceID(final String referenceID) {
            this.reference_id = referenceID;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public void setDescriptionLong(final String descriptionLong) {
            this.descriptionLong = descriptionLong;
        }

        public void setPublishedAT(final String publishedAT) {
            this.published_at = publishedAT;
        }

        public void setCreatedAT(final String createdAT) {
            this.created_at = createdAT;
        }

        public void setPublisherName(final String publisherName) {
            this.publisherName = publisherName;
        }

        public void setVideoCodec(final String videoCodec) {
            this.videoCodec = videoCodec;
        }

        public void setDownloadURL(final String downloadURL) {
            this.downloadurl = downloadURL;
        }

        public void setAppName(final String appName) {
            this.app_name = appName;
        }

        public void setWidth(final long width) {
            this.width = width;
        }

        public void setHeight(long height) {
            this.height = height;
        }

        public void setDuration(final long duration) {
            if (duration > 0) {
                this.length = duration;
            }
        }

        public void setFilesize(final long filesize) {
            if (filesize > 0) {
                this.size = filesize;
            }
        }

        @Override
        public String toString() {
            return name + "_" + width + "x" + height;
        }

        public String getStandardFilename() {
            return formatDate(this.getPublishedAT()) + "_" + this.getPublisherName() + "_" + this.getName() + "_" + this.getWidth() + "x" + this.getHeight() + "_" + this.getVideoCodec() + this.getFileExtension();
        }

        /** Formats input to yyyy-MM-dd */
        private static String formatDate(final String input) {
            if (input == null) {
                return null;
            }
            String output = new Regex(input, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
            if (output == null) {
                output = input;
            }
            return output;
        }

    }

}
