package org.jdownloader.plugins.components.hls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jd.http.Browser;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.logging.LogController;

public class HlsContainer {
    public static List<HlsContainer> findBestVideosByBandwidth(final List<HlsContainer> media) {
        if (media == null || media.size() == 0) {
            return null;
        }
        final Map<String, List<HlsContainer>> hlsContainer = new HashMap<String, List<HlsContainer>>();
        List<HlsContainer> ret = null;
        long bandwidth_highest = 0;
        for (HlsContainer item : media) {
            final String id = item.getExtXStreamInf();
            List<HlsContainer> list = hlsContainer.get(id);
            if (list == null) {
                list = new ArrayList<HlsContainer>();
                hlsContainer.put(id, list);
            }
            list.add(item);
            long bandwidth_temp = item.getBandwidth();
            if (bandwidth_temp == -1) {
                bandwidth_temp = item.getAverageBandwidth();
            }
            if (bandwidth_temp > bandwidth_highest) {
                bandwidth_highest = bandwidth_temp;
                ret = list;
            }
        }
        return ret;
    }

    public static HlsContainer findBestVideoByBandwidth(final List<HlsContainer> media) {
        final List<HlsContainer> ret = findBestVideosByBandwidth(media);
        if (ret != null && ret.size() > 0) {
            return ret.get(0);
        } else {
            return null;
        }
    }

    public static List<HlsContainer> getHlsQualities(final Browser br, final String m3u8) throws Exception {
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("Accept", "*/*");
        br2.getPage(m3u8);
        return getHlsQualities(br2);
    }

    public static List<HlsContainer> getHlsQualities(final Browser br) throws Exception {
        final ArrayList<HlsContainer> hlsqualities = new ArrayList<HlsContainer>();
        final String[][] streams = br.getRegex("#EXT-X-STREAM-INF:?([^\r\n]+)[\r\n]+([^\r\n]+)").getMatches();
        if (streams != null) {
            for (final String stream[] : streams) {
                if (StringUtils.isNotEmpty(stream[1])) {
                    final String streamInfo = stream[0];
                    if (false && new Regex(streamInfo, "(?:,|^)\\s*AUDIO\\s*=").matches()) {
                        LogInterface logger = br.getLogger();
                        if (logger == null) {
                            logger = LogController.CL();
                        }
                        logger.info("Unsupported M3U8! Split Audio/Video, see  https://svn.jdownloader.org/issues/87898|" + streamInfo);
                        continue;
                    }
                    // final String quality = new Regex(media, "(?:,|^)\\s*NAME\\s*=\\s*\"(.*?)\"").getMatch(0);
                    final String programID = new Regex(streamInfo, "(?:,|^)\\s*PROGRAM-ID\\s*=\\s*(\\d+)").getMatch(0);
                    final String bandwidth = new Regex(streamInfo, "(?:,|^)\\s*BANDWIDTH\\s*=\\s*(\\d+)").getMatch(0);
                    final String average_bandwidth = new Regex(streamInfo, "(?:,|^)\\s*AVERAGE-BANDWIDTH\\s*=\\s*(\\d+)").getMatch(0);
                    final String resolution = new Regex(streamInfo, "(?:,|^)\\s*RESOLUTION\\s*=\\s*(\\d+x\\d+)").getMatch(0);
                    final String framerate = new Regex(streamInfo, "(?:,|^)\\s*FRAME-RATE\\s*=\\s*(\\d+)").getMatch(0);
                    final String codecs = new Regex(streamInfo, "(?:,|^)\\s*CODECS\\s*=\\s*\"([^<>\"]+)\"").getMatch(0);
                    final String name = new Regex(streamInfo, "(?:,|^)\\s*NAME\\s*=\\s*\"([^<>\"]+)\"").getMatch(0);
                    final String url = br.getURL(stream[1]).toString();
                    final HlsContainer hls = new HlsContainer();
                    if (programID != null) {
                        hls.programID = Integer.parseInt(programID);
                    } else {
                        hls.programID = -1;
                    }
                    if (bandwidth != null) {
                        hls.bandwidth = Integer.parseInt(bandwidth);
                    } else {
                        hls.bandwidth = -1;
                    }
                    if (name != null) {
                        hls.name = name.trim();
                    }
                    if (average_bandwidth != null) {
                        hls.average_bandwidth = Integer.parseInt(average_bandwidth);
                    } else {
                        hls.average_bandwidth = -1;
                    }
                    if (codecs != null) {
                        hls.codecs = codecs.trim();
                    }
                    hls.streamURL = url;
                    hls.m3u8URL = br.getURL();
                    if (resolution != null) {
                        final String[] resolution_info = resolution.split("x");
                        final String width = resolution_info[0];
                        final String height = resolution_info[1];
                        hls.width = Integer.parseInt(width);
                        hls.height = Integer.parseInt(height);
                    }
                    if (framerate != null) {
                        hls.framerate = Integer.parseInt(framerate);
                    }
                    hlsqualities.add(hls);
                }
            }
        }
        return hlsqualities;
    }

    private String codecs;
    private String streamURL;
    private String m3u8URL;

    public String getM3U8URL() {
        return m3u8URL;
    }

    private List<M3U8Playlist> m3u8List          = null;
    private int                width             = -1;
    private int                height            = -1;
    private int                bandwidth         = -1;
    private int                average_bandwidth = -1;
    private int                programID         = -1;
    private int                framerate         = -1;
    private String             name              = null;

    public String getName() {
        return name;
    }

    protected List<M3U8Playlist> loadM3U8(Browser br) throws IOException {
        final Browser br2 = br.cloneBrowser();
        return M3U8Playlist.loadM3U8(getStreamURL(), br2);
    }

    public void setM3U8(List<M3U8Playlist> m3u8List) {
        this.m3u8List = m3u8List;
    }

    public String getExtXStreamInf() {
        final StringBuilder sb = new StringBuilder();
        sb.append("#EXT-X-STREAM-INF:");
        boolean sep = false;
        if (getProgramID() != -1) {
            sb.append("PROGRAM-ID=" + getProgramID());
            sep = true;
        }
        if (getBandwidth() != -1) {
            if (sep) {
                sb.append(",");
            }
            sb.append("BANDWIDTH=" + getBandwidth());
            sep = true;
        }
        if (getAverageBandwidth() != -1) {
            if (sep) {
                sb.append(",");
            }
            sb.append("AVERAGE-BANDWIDTH=" + getAverageBandwidth());
            sep = true;
        }
        if (getCodecs() != null) {
            if (sep) {
                sb.append(",");
            }
            sb.append("CODECS=\"" + getCodecs() + "\"");
            sep = true;
        }
        if (getResolution() != null) {
            if (sep) {
                sb.append(",");
            }
            sb.append("RESOLUTION=" + getResolution());
            sep = true;
        }
        if (getFramerate() != -1) {
            if (sep) {
                sb.append(",");
            }
            sb.append("FRAME-RATE=" + getFramerate());
            sep = true;
        }
        if (getName() != null) {
            if (sep) {
                sb.append(",");
            }
            sb.append("NAME=\"" + getName() + "\"");
            sep = true;
        }
        return sb.toString();
    }

    public List<M3U8Playlist> getM3U8(Browser br) throws IOException {
        if (m3u8List == null) {
            setM3U8(loadM3U8(br));
            final int bandwidth;
            if (getAverageBandwidth() > 0) {
                bandwidth = getAverageBandwidth();
            } else {
                bandwidth = getBandwidth();
            }
            if (m3u8List != null && bandwidth > 0) {
                for (final M3U8Playlist m3u8 : m3u8List) {
                    m3u8.setAverageBandwidth(bandwidth);
                }
            }
        }
        return m3u8List;
    }

    public int getProgramID() {
        return programID;
    }

    public static enum CODEC_TYPE {
        VIDEO,
        AUDIO,
        UNKNOWN
    }

    public static enum CODEC {
        // http://mp4ra.org/#/codecs
        // https://wiki.multimedia.cx/index.php/MPEG-4_Audio#Audio_Object_Types
        MP3(CODEC_TYPE.AUDIO, "mp3,", "mp3", "mp4a\\.40\\.34"),
        AAC(CODEC_TYPE.AUDIO, "aac", "m4a", "mp4a\\.40\\.(1|2|3|4|5|6)"),
        AVC(CODEC_TYPE.VIDEO, "avc", "mp4", "avc\\d+"),
        HEVC(CODEC_TYPE.VIDEO, "hevc", "mp4", "(hev|hvc)\\d+"),
        UNKNOWN(CODEC_TYPE.UNKNOWN, null, null, null);
        private final CODEC_TYPE type;

        public CODEC_TYPE getType() {
            return type;
        }

        public String getDefaultExtension() {
            return defaultExtension;
        }

        public Pattern getPattern() {
            return pattern;
        }

        private final String  defaultExtension;
        private final Pattern pattern;
        private final String  codecName;

        public String getCodecName() {
            return codecName;
        }

        private CODEC(CODEC_TYPE type, final String codecName, final String defaultExtension, final String pattern) {
            this.type = type;
            this.codecName = codecName;
            this.defaultExtension = defaultExtension;
            this.pattern = pattern != null ? Pattern.compile(pattern) : null;
        }

        public static CODEC parse(final String raw) {
            if (StringUtils.isNotEmpty(raw)) {
                for (CODEC codec : values()) {
                    if (codec.getPattern() != null && new Regex(raw, codec.getPattern()).matches()) {
                        return codec;
                    }
                }
            }
            return UNKNOWN;
        }
    }

    public static class StreamCodec {
        private final CODEC codec;

        public CODEC getCodec() {
            return codec;
        }

        public String getRaw() {
            return raw;
        }

        private final String raw;

        private StreamCodec(final String raw) {
            this.raw = raw;
            this.codec = CODEC.parse(raw);
        }
    }

    public List<StreamCodec> getStreamCodecs() {
        final String[] codecs = this.codecs != null ? this.codecs.split(",") : null;
        if (codecs != null) {
            final List<StreamCodec> ret = new ArrayList<StreamCodec>();
            for (final String codec : codecs) {
                ret.add(new StreamCodec(codec));
            }
            return ret;
        } else {
            return null;
        }
    }

    public StreamCodec getCodecType(CODEC_TYPE type) {
        final List<StreamCodec> ret = getStreamCodecs();
        if (ret != null) {
            for (final StreamCodec streamCodec : ret) {
                if (streamCodec.getCodec().getType().equals(type)) {
                    return streamCodec;
                }
            }
        }
        return null;
    }

    public StreamCodec getCodec(CODEC codec) {
        final List<StreamCodec> ret = getStreamCodecs();
        if (ret != null) {
            for (final StreamCodec streamCodec : ret) {
                if (streamCodec.getCodec().equals(codec)) {
                    return streamCodec;
                }
            }
        }
        return null;
    }

    public String getCodecs() {
        return this.codecs;
    }

    @Deprecated
    public String getDownloadurl() {
        return getStreamURL();
    }

    public String getStreamURL() {
        return streamURL;
    }

    public boolean isVideo() {
        if (getCodecType(CODEC_TYPE.VIDEO) != null) {
            return true;
        } else if (this.width == -1 && this.height == -1) {
            /* wtf case */
            return false;
        } else {
            return true;
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getFramerate() {
        return framerate;
    }

    /**
     * @param fallback
     *            : Value to be returned if framerate is unknown - usually this will be 25.
     */
    public int getFramerate(final int fallback) {
        if (framerate == -1) {
            return fallback;
        } else {
            return framerate;
        }
    }

    public String getResolution() {
        return this.getWidth() + "x" + this.getHeight();
    }

    public int getBandwidth() {
        return this.bandwidth;
    }

    public int getAverageBandwidth() {
        return this.average_bandwidth;
    }

    public HlsContainer() {
    }

    @Override
    public String toString() {
        return getExtXStreamInf();
    }

    public String getStandardFilename() {
        String filename = "";
        if (width != -1 && height != -1) {
            filename += getResolution();
        }
        if (codecs != null) {
            filename += "_" + codecs;
        }
        filename += getFileExtension();
        return filename;
    }

    public String getFileExtension() {
        final StreamCodec video = getCodecType(CODEC_TYPE.VIDEO);
        final StreamCodec audio = getCodecType(CODEC_TYPE.AUDIO);
        if (video != null) {
            return "." + video.getCodec().getDefaultExtension();
        } else if (audio != null) {
            return "." + audio.getCodec().getDefaultExtension();
        } else {
            // fallback
            return ".mp4";
        }
    }
}