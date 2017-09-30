package org.jdownloader.plugins.components.hls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.M3U8Playlist;

import jd.http.Browser;

public class HlsContainer {
    public static HlsContainer findBestVideoByBandwidth(final List<HlsContainer> media) {
        if (media == null) {
            return null;
        }
        HlsContainer best = null;
        long bandwidth_highest = 0;
        for (final HlsContainer hls : media) {
            final long bandwidth_temp = hls.bandwidth;
            if (bandwidth_temp > bandwidth_highest) {
                bandwidth_highest = bandwidth_temp;
                best = hls;
            }
        }
        return best;
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
        if (streams == null) {
            return null;
        }
        for (final String stream[] : streams) {
            if (StringUtils.isNotEmpty(stream[1])) {
                final String streamInfo = stream[0];
                // final String quality = new Regex(media, "(?:,|^)\\s*NAME\\s*=\\s*\"(.*?)\"").getMatch(0);
                final String programID = new Regex(streamInfo, "(?:,|^)\\s*PROGRAM-ID\\s*=\\s*(\\d+)").getMatch(0);
                final String bandwidth = new Regex(streamInfo, "(?:,|^)\\s*BANDWIDTH\\s*=\\s*(\\d+)").getMatch(0);
                final String average_bandwidth = new Regex(streamInfo, "(?:,|^)\\s*AVERAGE-BANDWIDTH\\s*=\\s*(\\d+)").getMatch(0);
                final String resolution = new Regex(streamInfo, "(?:,|^)\\s*RESOLUTION\\s*=\\s*(\\d+x\\d+)").getMatch(0);
                final String framerate = new Regex(streamInfo, "(?:,|^)\\s*FRAME-RATE\\s*=\\s*(\\d+)").getMatch(0);
                final String codecs = new Regex(streamInfo, "(?:,|^)\\s*CODECS\\s*=\\s*\"([^<>\"]+)\"").getMatch(0);
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
                if (average_bandwidth != null) {
                    hls.average_bandwidth = Integer.parseInt(average_bandwidth);
                } else {
                    hls.average_bandwidth = -1;
                }
                if (codecs != null) {
                    hls.codecs = codecs.trim();
                }
                hls.downloadurl = url;
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
        return hlsqualities;
    }

    private String             codecs;
    private String             downloadurl;
    private List<M3U8Playlist> m3u8List          = null;
    private int                width             = -1;
    private int                height            = -1;
    private int                bandwidth         = -1;
    private int                average_bandwidth = -1;
    private int                programID         = -1;
    private int                framerate         = -1;

    protected List<M3U8Playlist> loadM3U8(Browser br) throws IOException {
        final Browser br2 = br.cloneBrowser();
        return M3U8Playlist.loadM3U8(getDownloadurl(), br2);
    }

    public void setM3U8(List<M3U8Playlist> m3u8List) {
        this.m3u8List = m3u8List;
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

    public void setProgramID(int programID) {
        this.programID = programID;
    }

    private boolean isAudioMp3() {
        return StringUtils.equalsIgnoreCase(codecs, "mp4a.40.34");
    }

    private boolean isAudioAac() {
        return StringUtils.equalsIgnoreCase(codecs, "mp4a.40.5") || StringUtils.equalsIgnoreCase(codecs, "mp4a.40.2");
    }

    public boolean isAudio() {
        return isAudioMp3() || isAudioAac();
    }

    public String getCodecs() {
        return this.codecs;
    }

    public String getDownloadurl() {
        return downloadurl;
    }

    public boolean isVideo() {
        if (isAudio()) {
            return false;
        } else if (this.width == -1 && this.height == -1) {
            /* wtf case */
            return false;
        } else {
            return true;
        }
    }

    public int getWidth() {
        final int width;
        if (this.isAudio()) {
            width = 0;
        } else {
            width = this.width;
        }
        return width;
    }

    public int getHeight() {
        final int height;
        if (this.isAudio()) {
            height = 0;
        } else {
            height = this.height;
        }
        return height;
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
        return getStandardFilename();
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
        if (isAudioMp3()) {
            return ".mp3";
        } else if (isAudioAac()) {
            return ".aac";
        } else {
            return ".mp4";
        }
    }
}