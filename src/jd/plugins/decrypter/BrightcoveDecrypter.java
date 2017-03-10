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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

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
import jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveEdgeContainer.Protocol;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
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

    /**
     * Find the Brightcove accountID - relevant to access the js- and json later on. <br />
     *
     * @param br
     *            :Browser containing the html code of the source page- and the required Brightcove parameters.
     *
     * */
    public static String brightcoveEdgeRegexIDAccount(final Browser br) {
        return br.getRegex("data\\-account\\s*=\\s*(\"|')(\\d+)\\1").getMatch(1);
    }

    /**
     * Find the Brightcove referenceID - relevant to access the json later on. <br />
     *
     * @param br
     *            :Browser containing the html code of the source page- and the required Brightcove parameters.
     *
     * */
    public static String brightcoveEdgeRegexIDReference(final Browser br) {
        return br.getRegex("data\\-video\\-id\\s*=\\s*(\"|')([^<>\"\\']+)\\1").getMatch(1);
    }

    /**
     * Find the Brightcove policyKey - relevant to access the json later on as this key is required in the 'Access' header. <br />
     *
     * @param br
     *            :Browser containing the html code of the source page- and the required Brightcove parameters.
     * @param accountID
     *            : Brightcove accountID
     *
     * */
    public static String getPolicyKey(final Browser br, final String accountID) throws IOException {
        if (br == null || StringUtils.isEmpty(accountID)) {
            return null;
        }
        final Browser js = br.cloneBrowser();
        js.getHeaders().put("Accept", "*/*");
        /* E.g. working fine for stern.de */
        String bcJSURL = br.getRegex("<script src=(\"|')(//players\\.brightcove\\.net/" + accountID + "/.*?)\\1></script>").getMatch(1);
        if (StringUtils.isEmpty(bcJSURL)) {
            /* E.g. required for about.com */
            final String dataPlayer = br.getRegex("data\\-player=\"([^<>\"]+)\"").getMatch(0);
            if (dataPlayer == null) {
                return null;
            }
            bcJSURL = String.format("http://players.brightcove.net/%s/%s_default/index.min.js", accountID, dataPlayer);
        }
        js.getPage(bcJSURL);
        final String policyKey = PluginJSonUtils.getJson(js, "policyKey");
        return policyKey;
    }

    /**
     * Sets the previously parsed policyKey as 'Accept' header. <br />
     * Without this header we'll get a 403!
     *
     * @param br
     *            :Browser containing the html code of the source page- and the required Brightcove parameters.
     * @param policyKey
     *            : policyKey obtained via getPolicyKey
     *
     * */
    public static void setAPIHeaders(final Browser br, final String policyKey) {
        br.getHeaders().put("Accept", "application/json;pk=" + policyKey);
    }

    /**
     * Returns the Brightcove API URL which finally leads to the json data of our video. <br />
     *
     * @param accountID
     *            :Brightcove accountID
     * @param referenceID
     *            : Brightcove referenceID
     *
     * */
    public static String getAPIPlaybackUrl(final String accountID, final String referenceID) {
        if (accountID == null || referenceID == null) {
            return null;
        }
        return String.format("https://edge.api.brightcove.com/playback/v1/accounts/%s/videos/%s", accountID, referenceID);
    }

    /**
     * Returns the Brightcove HLS master (mobile) URL via unencrypted http url. <br />
     *
     * @param videoID
     *            :Brightcove videoID (usually [but not always]) this can be found inside the json of the playbackUrl.
     *
     * */
    public static String getHlsMasterHttp(final String videoID) {
        if (videoID == null) {
            return null;
        }
        /* Possible additional (not required) parameter: 'pubId' e.g. '&pubId=4013' */
        return String.format("http://c.brightcove.com/services/mobile/streaming/index/master.m3u8?videoId=%s", videoID);
    }

    /**
     * Returns the Brightcove HLS master (mobile) URL via encrypted https url. <br />
     *
     * @param videoID
     *            :Brightcove videoID (usually [but not always]) this can be found inside the json of the playbackUrl.
     *
     * */
    public static String getHlsMasterHttps(final String videoID) {
        if (videoID == null) {
            return null;
        }
        /* Possible additional (not required) parameter: 'pubId' e.g. '&pubId=4013' */
        return String.format("https://secure.brightcove.com/services/mobile/streaming/index/master.m3u8?videoId=%s&secure=true", videoID);
    }

    /**
     * Finds ALL BrightcoveEdgeContainer Objects. <br />
     *
     * @param br
     *            : Browser containing the html code of the source page- and the required Brightcove parameters.
     */
    public static HashMap<String, BrightcoveEdgeContainer> findAllQualitiesAuto(final Browser br) throws DecrypterException, Exception {
        return findAllQualities(br, null);
    }

    /**
     * Finds all BrightcoveEdgeContainer Objects that have the allowed Protocol type. <br />
     *
     * @param br
     *            : Browser containing the html code of the source page- and the required Brightcove parameters.
     * @param allowedProtocolsList
     *            : List containing all allowed protocols - usually (2017-03-10) we only want HTTP and HLS which ensures that we avoid
     *            complicated download procedures and/or issues with crypted streams. <br />
     *            null = Allow ALL protocols!
     */
    public static HashMap<String, BrightcoveEdgeContainer> findAllQualitiesAuto(final Browser br, final List<Protocol> allowedProtocolsList) throws DecrypterException, Exception {
        return findAllQualities(br, allowedProtocolsList);
    }

    /**
     * Finds the best BrightcoveEdgeContainer of all the qualities it finds before. <br />
     * Calls findBESTBrightcoveEdgeContainerAuto(br, null). <br />
     *
     * @param br
     *            : Browser containing the html code of the source page- and the required Brightcove parameters.
     */
    public static BrightcoveEdgeContainer findBESTBrightcoveEdgeContainerAuto(final Browser br) throws DecrypterException, Exception {
        return findBESTBrightcoveEdgeContainerAuto(br, null);
    }

    /**
     * Finds the best BrightcoveEdgeContainer of all the qualities it finds before. <br />
     *
     * @param br
     *            : Browser containing the html code of the source page- and the required Brightcove parameters.
     * @param allowedProtocolsList
     *            : List containing all allowed protocols - usually (2017-03-10) we only want HTTP and HLS which ensures that we avoid
     *            complicated download procedures and/or issues with crypted streams. <br />
     *            null = Allow ALL protocols!
     */
    public static BrightcoveEdgeContainer findBESTBrightcoveEdgeContainerAuto(final Browser br, final List<Protocol> allowedProtocolsList) throws DecrypterException, Exception {
        final HashMap<String, BrightcoveEdgeContainer> allQualities = findAllQualitiesAuto(br, allowedProtocolsList);
        return findBESTBrightcoveEdgeContainer(allQualities);
    }

    /**
     * TODO: add support for thumbnails via quality selection, make sure to only grab hls one time if it is available multiple times (via
     * https and without)!, Add parameter for encrypted stuff and/or block encrypted hls- and RTMPE by default.
     */
    /**
     * Finds all (video) qualities via all allowed protocols. <br />
     *
     * @param br
     *            : Browser containing the html code of the source page- and the required Brightcove parameters.
     * @param allowedProtocolsList
     *            : List containing all allowed protocols - usually (2017-03-10) we only want HTTP and HLS which ensures that we avoid
     *            complicated download procedures and/or issues with crypted streams.
     */
    @SuppressWarnings("unchecked")
    public static HashMap<String, BrightcoveEdgeContainer> findAllQualities(final Browser br, final List<Protocol> allowedProtocolsList) throws IOException {
        final String accountID = brightcoveEdgeRegexIDAccount(br);
        final String referenceID = brightcoveEdgeRegexIDReference(br);
        /*
         * Check this here already because the next step requires a http request - if one of these values is missing we cannot continue
         * anyways!
         */
        if (StringUtils.isEmpty(accountID) || StringUtils.isEmpty(referenceID)) {
            return null;
        }
        final String policyKey = getPolicyKey(br, accountID);
        if (br == null || StringUtils.isEmpty(policyKey)) {
            return null;
        }
        final String source_host = br.getHost();

        final HashMap<String, BrightcoveEdgeContainer> all_found_qualities = new HashMap<String, BrightcoveEdgeContainer>();
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
            /* The videoID which can be used to build hls-master-urls --> This is NOT == referenceID! */
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
            /*
             * Actually there is only one single clip in different formats so every format should have the same duration but some of them do
             * have their own 'duration' data which is why we call this 'duration_general'.
             */
            final long duration_general = JavaScriptEngineFactory.toLong(entries.get("duration"), 0);

            if (StringUtils.isEmpty(publisherInfo)) {
                publisherInfo = source_host;
            }

            /* We're gonna use this in our filenames later so let's remove the TLD and the dots. */
            String publisher_name = new Regex(publisherInfo, "(?:https?://(?:www\\.)?)?([^/]+)").getMatch(0);
            publisher_name = publisher_name.substring(0, publisher_name.lastIndexOf(".")).replace(".", "_");

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

                if (src == null || codec == null) {
                    /* Skip invalid items */
                    continue;
                }

                BrightcoveEdgeContainer bcont = null;
                if (type != null && type.equalsIgnoreCase("application/x-mpegURL") || src.contains(".m3u8")) {
                    /* HLS */
                    if (allowedProtocolsList != null && !allowedProtocolsList.contains(Protocol.HLS)) {
                        /* Skip HLS if it's not wanted. */
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
                            bcont.setBandwidth(hlsCont.getBandwidth());
                            bcont.setVideoCodec(codec);
                            bcont.setDownloadURL(hlsCont.getDownloadurl());
                            bcont.setPublishedAT(published_at);
                            bcont.setPublisherName(publisher_name);
                            bcont.setCreatedAT(created_at);
                            bcont.setDescription(description);
                            bcont.setDescriptionLong(description_long);
                            bcont.setProtocol(BrightcoveEdgeContainer.Protocol.HLS);
                            all_found_qualities.put(bcont.getQualityKey(), bcont);
                        }
                    }

                } else if (src.startsWith("rtmp") || src.startsWith("http")) {
                    if (width == 0 || height == 0) {
                        continue;
                    }
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
                    if (src.startsWith("rtmpe")) {
                        bcont.setAppName(app_name);
                        bcont.setProtocol(BrightcoveEdgeContainer.Protocol.RTMPE);
                    } else if (src.startsWith("rtmp")) {
                        bcont.setAppName(app_name);
                        bcont.setProtocol(BrightcoveEdgeContainer.Protocol.RTMP);
                    } else {
                        bcont.setProtocol(BrightcoveEdgeContainer.Protocol.HTTP);
                    }
                    if (allowedProtocolsList != null && !allowedProtocolsList.contains(bcont.getProtocol())) {
                        /* Skip unwanted protocols. */
                        continue;
                    }
                    all_found_qualities.put(bcont.getQualityKey(), bcont);
                } else {
                    /* Skip unknown formats/protocols */
                    continue;
                }
            }

        } catch (final Throwable e) {
        }
        return all_found_qualities;
    }

    /**
     * Finds the best BrightcoveEdgeContainer Object within the previously found Objects. <br />
     * 'Best' = Object with the highest filesize (for HLS, highest bandwidth)<br />
     *
     * @param inputmap
     *            : HashMap with previously found BrightcoveEdgeContainer objects
     */
    public static BrightcoveEdgeContainer findBESTBrightcoveEdgeContainer(final HashMap<String, BrightcoveEdgeContainer> inputmap) {
        if (inputmap == null) {
            return null;
        }
        long bandwidth_max = 0;
        long filesize_max = 0;

        /* Usually HTTP */
        BrightcoveEdgeContainer bestByFilesize = null;
        /* Usually HDS/HLS */
        BrightcoveEdgeContainer bestByBandwidth = null;
        BrightcoveEdgeContainer best = null;

        final Iterator<Entry<String, BrightcoveEdgeContainer>> inputmap_iterator = inputmap.entrySet().iterator();
        while (inputmap_iterator.hasNext()) {
            final Entry<String, BrightcoveEdgeContainer> brightcoveEdgeContainerEntryTemp = inputmap_iterator.next();
            final BrightcoveEdgeContainer brightcoveEdgeContainerTemp = brightcoveEdgeContainerEntryTemp.getValue();
            if (brightcoveEdgeContainerTemp.getFilesize() > filesize_max) {
                bestByFilesize = brightcoveEdgeContainerTemp;
                filesize_max = brightcoveEdgeContainerTemp.getFilesize();
            } else if (brightcoveEdgeContainerTemp.getBandwidth() > bandwidth_max) {
                bestByBandwidth = brightcoveEdgeContainerTemp;
                bandwidth_max = brightcoveEdgeContainerTemp.getBandwidth();
            }

        }

        if (bestByFilesize != null && bestByBandwidth != null && bestByFilesize.getProtocol() == Protocol.HTTP && bestByBandwidth.getProtocol() == Protocol.HLS && bestByBandwidth.getFilesize() == 0) {
            /* Find out if hls has higher quality than http. */
            try {
                final Browser br = new Browser();
                final DownloadLink dummyLink = new DownloadLink(null, "Dummy", "brightcove.com", "http://brightcove.com/", true);
                final HLSDownloader downloader = new HLSDownloader(dummyLink, br, bestByBandwidth.getDownloadURL());
                final StreamInfo streamInfo = downloader.getProbe();
                if (streamInfo != null) {
                    final long estimatedSize = downloader.getEstimatedSize();
                    if (estimatedSize > 0) {
                        bestByBandwidth.setFilesize(estimatedSize);
                    }
                }
            } catch (final Throwable e) {
            }
            if (bestByBandwidth.getFilesize() > bestByFilesize.getFilesize()) {
                best = bestByBandwidth;
            } else {
                best = bestByFilesize;
            }
        }
        if (best == null && bestByFilesize != null) {
            /* Usually http */
            best = bestByFilesize;
        } else if (best == null && bestByBandwidth != null) {
            /* Usually hls */
            best = bestByBandwidth;
        }

        return best;
    }

    /* Handles quality selection of given HashMap 'inputmap' with errorhandling for bad user selection! */
    /**
     * Handles quality selection of given HashMap 'inputmap' with errorhandling for bad user selection! <br />
     * No matter what the user does - if his selection is bad, this function will simply return all the contents of the inputmap as result.<br />
     *
     * @param inputmap
     *            : HashMap with previously found BrightcoveEdgeContainer objects
     * @param grabBEST
     *            : true = return only the best BrightcoveEdgeContainer object
     * @param grabBESTWithinUserSelection
     *            : true = Find the selected qualities, then grab the BEST object within these results - this way the user can define his
     *            own BEST quality.
     * @param grabUnknownQualities
     *            : true = grab qualities which are not present in the allPossibleQualities List.<br />
     *            This is good to keep the mechanism working if the content provider introduces new formats which are not yet built into the
     *            plugin settings.
     * @param allPossibleQualities
     *            : List of all possible/known quality strings. <br />
     *            Important to identify unknown qualities! <br />
     *            Although BEST selection is based on filesize/bandwidth, this list should usually be in order highest quality --> lowest
     *            quality!
     * @param allSelectedQualities
     *            : List of all user-selected quality strings.<br />
     *            Format of quality strings: 'Protocol_Bandwidth_Height'<br />
     *            Example of an http quality string: 'http_0_720' (Bandwidth is usually not given for http Protocol qualities) <br />
     *            Example of an hls quality string: 'hls_314000_720' (Bandwidth is usually given for hls Protocol qualities - also the same
     *            height can exist with multiple Bandwidth values --> Important for quality selection and filenames!)
     */
    public static HashMap<String, BrightcoveEdgeContainer> handleQualitySelection(final HashMap<String, BrightcoveEdgeContainer> inputmap, final boolean grabBEST, final boolean grabBESTWithinUserSelection, final boolean grabUnknownQualities, final List<String> allPossibleQualities, final List<String> allSelectedQualities) {
        /* If BEST, grab BEST */
        HashMap<String, BrightcoveEdgeContainer> final_selected_qualities_map = new HashMap<String, BrightcoveEdgeContainer>();
        if (grabBEST) {
            final BrightcoveEdgeContainer bestQuality = findBESTBrightcoveEdgeContainer(inputmap);
            if (bestQuality != null) {
                final_selected_qualities_map.put(bestQuality.getQualityKey(), bestQuality);
            }
        }

        /* Either BEST setting not active or it failed --> Proceed here */
        if (final_selected_qualities_map.isEmpty()) {
            boolean atLeastOneSelectedQualityExists = false;
            if (allSelectedQualities != null) {
                for (final String selectedQuality : allSelectedQualities) {
                    if (inputmap.containsKey(selectedQuality)) {
                        atLeastOneSelectedQualityExists = true;
                        break;
                    }
                }
            }

            /* Return selected qualities */
            final Iterator<Entry<String, BrightcoveEdgeContainer>> inputmap_iterator = inputmap.entrySet().iterator();
            while (inputmap_iterator.hasNext()) {
                final Entry<String, BrightcoveEdgeContainer> brightcoveEdgeContainerEntryTemp = inputmap_iterator.next();
                final String qualityKey = brightcoveEdgeContainerEntryTemp.getKey();
                final BrightcoveEdgeContainer brightcoveEdgeContainerTemp = brightcoveEdgeContainerEntryTemp.getValue();
                final boolean isUnknownQuality = allPossibleQualities != null && !allPossibleQualities.contains(qualityKey);
                if (allSelectedQualities.contains(qualityKey)) {
                    /* Grab quality because user selected it */
                    final_selected_qualities_map.put(qualityKey, brightcoveEdgeContainerTemp);
                } else if (!atLeastOneSelectedQualityExists || (grabUnknownQualities && isUnknownQuality)) {
                    /*
                     * Grab quality because of bad user plugin settings OR because is not known to the format selection AND user wants to
                     * have unknown qualities!
                     */
                    final_selected_qualities_map.put(qualityKey, brightcoveEdgeContainerTemp);
                } else {
                    /* Skip unselected qualities */
                }
            }
        }

        return final_selected_qualities_map;
    }

    public static class BrightcoveEdgeContainer {

        public static enum Protocol {
            DASH,
            HTTP,
            HDS,
            HLS,
            RTMP,
            RTMPE
        }

        private Protocol protocol;
        private String   ext;
        private String   account_id;
        private String   reference_id;

        private String   name;
        private String   description;
        private String   descriptionLong;
        private String   published_at;
        private String   created_at;

        private String   publisherName;
        private String   videoCodec;
        private String   downloadurl;
        private String   app_name;

        private long     bandwidth;
        private long     width;
        private long     height;
        private long     length;
        private long     size;

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
            this.setAccountID(accountID);
            this.setReferenceID(referenceID);
            this.setName(name);
            this.setHeight(height);
            this.setWidth(width);
        }

        public Protocol getProtocol() {
            return this.protocol;
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

        public long getBandwidth() {
            return this.bandwidth;
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

        public void setProtocol(final Protocol protocol) {
            this.protocol = protocol;
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
            if (!StringUtils.isEmpty(description)) {
                this.description = description;
            }
        }

        public void setDescriptionLong(final String descriptionLong) {
            if (!StringUtils.isEmpty(descriptionLong)) {
                this.descriptionLong = descriptionLong;
            }
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

        public void setBandwidth(final long bandwidth) {
            this.bandwidth = bandwidth;
        }

        public void setFilesize(final long filesize) {
            if (filesize > 0) {
                this.size = filesize;
            }
        }

        public String getResolution() {
            return this.getWidth() + "x" + this.getHeight();
        }

        /* TODO: Maybe improve this */
        @Override
        public String toString() {
            return this.getName() + "_" + this.getResolution();
        }

        public boolean equals(final BrightcoveEdgeContainer compare) {
            return this.getLinkID().equals(compare.getLinkID());
        }

        public String getLinkID() {
            return this.getName() + "_" + this.getBandwidth() + "_" + this.getWidth() + "_" + this.getHeight();
        }

        public String getQualityKey() {
            return this.getProtocol() + "_" + this.getBandwidth() + "_" + this.getHeight();
        }

        /**
         * Returns a basic filename for current object.<br />
         * It contains the date of release, name of the source (publisher), title and information about the format (dimensions, videoCodec).
         */
        public String getStandardFilename() {
            final String filename;
            if (this.getProtocol() == Protocol.HLS) {
                /*
                 * For hls, we have the bandwidth which is relevant for quality selection and required in the filenames for the user to
                 * differ between formats later on.
                 */
                filename = formatDate(this.getPublishedAT()) + "_" + this.getPublisherName() + "_" + this.getName() + "_" + this.getBandwidth() + "_" + this.getResolution() + "_" + this.getVideoCodec() + this.getFileExtension();
            } else {
                filename = formatDate(this.getPublishedAT()) + "_" + this.getPublisherName() + "_" + this.getName() + "_" + this.getResolution() + "_" + this.getVideoCodec() + this.getFileExtension();
            }
            return filename;
        }

        /**
         * Formats input to yyyy-MM-dd.
         *
         * @param input
         *            : Date in the format of yyyy-MM-ddBLABLA_DONT_CARE
         */
        private static String formatDate(final String input) {
            if (input == null) {
                return null;
            }
            /* No need to parse the date as the format we want is already available in the input date String :) */
            String output = new Regex(input, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
            if (output == null) {
                output = input;
            }
            return output;
        }

        /**
         * Sets filename, filesize (if available), linkid, comment (if available) and availibility (=true) on given DownloadLink object.
         *
         * @param dl
         *            : Given DownloadLink on which the information will be set.
         */
        public DownloadLink setInformationOnDownloadLink(final DownloadLink dl) {
            dl.setFinalFileName(this.getStandardFilename());
            if (this.getFilesize() > 0) {
                dl.setDownloadSize(this.getFilesize());
            }
            dl.setLinkID(this.getLinkID());
            if (dl.getComment() == null && this.getDescriptionLong() != null) {
                dl.setComment(this.getDescriptionLong());
            }
            dl.setAvailable(true);
            return dl;
        }

    }

}
