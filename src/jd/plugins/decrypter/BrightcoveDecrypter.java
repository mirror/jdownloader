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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
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

        private final String ext = ".mp4";
        private String       displayName;
        private String       shortDescription;
        private String       publisherName;
        private String       videoCodec;
        private String       downloadurl;

        private int          width;
        private int          height;
        private int          length;

        private long         creationDate;
        private long         encodingRate;
        private long         size;
        private long         mediaDeliveryType;

        public BrightcoveClipData() {
            //
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
            return formatDate(creationDate) + "_" + publisherName + "_" + displayName + "_" + width + "x" + height + "_" + videoCodec + ext;
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

    public static ArrayList<BrightcoveEdgeContainer> findAllQualitiesAuto(final Browser br) throws DecrypterException, Exception {
        final String accountID = brightcoveEdgeRegexIDAccount(br);
        final String videoID = brightcoveEdgeRegexIDAccount(br);
        final String policyKey = getPolicyKey(br, accountID);
        return findAllQualities(br, accountID, videoID, policyKey);
    }

    public static ArrayList<BrightcoveEdgeContainer> findAllQualities(final Browser br, final String accountID, final String videoID, final String policyKey) throws IOException {
        if (br == null || StringUtils.isEmpty(accountID) || StringUtils.isEmpty(videoID) || StringUtils.isEmpty(policyKey)) {
            return null;
        }
        /* TODO: Brightcove edge parser goes here */
        setAPIHeaders(br, policyKey);
        br.getPage(String.format("https://edge.api.brightcove.com/playback/v1/accounts/%s/videos/%s", accountID, videoID));
        /* TODO: Add functionality */
        return null;
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

        private long   width;
        private long   height;
        private long   length;
        private long   size;

        public BrightcoveEdgeContainer() {
            ext = ".mp4";
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

        public void setWidth(final long width) {
            this.width = width;
        }

        public void setHeight(long height) {
            this.height = height;
        }

        public void setDuration(final long duration) {
            this.length = duration;
        }

        public void setFilesize(final long filesize) {
            this.size = filesize;
        }

        @Override
        public String toString() {
            return name + "_" + width + "x" + height;
        }

        public String getStandardFilename() {
            return "null_date_formatted" + "_" + publisherName + "_" + name + "_" + width + "x" + height + "_" + videoCodec + ext;
        }

        // private String formatDate(final long date) {
        // String formattedDate = null;
        // final String targetFormat = "yyyy-MM-dd";
        // Date theDate = new Date(date);
        // try {
        // final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
        // formattedDate = formatter.format(theDate);
        // } catch (Exception e) {
        // /* prevent input error killing plugin */
        // formattedDate = Long.toString(date);
        // }
        // return formattedDate;
        // }

    }

}
