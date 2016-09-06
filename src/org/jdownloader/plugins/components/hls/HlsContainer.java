package org.jdownloader.plugins.components.hls;

import java.util.ArrayList;
import java.util.List;

import jd.http.Browser;

import org.appwork.utils.Regex;

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

    public static List<HlsContainer> getHlsQualities(final Browser br) throws Exception {
        final ArrayList<HlsContainer> hlsqualities = new ArrayList<HlsContainer>();
        final String[] medias = br.getRegex("#EXT-X-STREAM-INF([^\r\n]+[\r\n]+[^\r\n]+)").getColumn(-1);
        if (medias == null) {
            return null;
        }
        for (final String media : medias) {
            // name = quality
            // final String quality = new Regex(media, "NAME=\"(.*?)\"").getMatch(0);
            String hlsurl = null;
            final String bandwidth = new Regex(media, "BANDWIDTH=(\\d+)").getMatch(0);
            final String resolution = new Regex(media, "RESOLUTION=(\\d+x\\d+)").getMatch(0);
            final String codecs = new Regex(media, "CODECS=\"([^<>\"]+)\"").getMatch(0);
            final String[] lines = Regex.getLines(media);
            for (final String line : lines) {
                if (line.contains(".m3u8")) {
                    hlsurl = line;
                    break;
                }
            }
            if (bandwidth == null || hlsurl == null) {
                continue;
            }
            hlsurl = hlsurl.trim();
            if (!hlsurl.startsWith("http")) {
                hlsurl = br.getBaseURL() + hlsurl;
            }
            final HlsContainer hls = new HlsContainer();
            hls.bandwidth = Long.parseLong(bandwidth);
            hls.codecs = codecs;
            hls.downloadurl = hlsurl;
            /* TODO: Add audio */
            hls.type = "video";

            if (resolution != null) {
                final String[] resolution_info = resolution.split("x");
                final String width = resolution_info[0];
                final String height = resolution_info[1];
                hls.width = Integer.parseInt(width);
                hls.height = Integer.parseInt(height);
            }

            hlsqualities.add(hls);
        }
        return hlsqualities;
    }

    public static final String ext = ".mp4";
    public String              codecs;
    public String              downloadurl;
    public String              type;
    public int                 width;
    public int                 height;
    public long                bandwidth;

    public String getResolution() {
        return width + "x" + height;
    }

    public HlsContainer() {
    }

    @Override
    public String toString() {
        return codecs + "_" + width + "x" + height;
    }

    public String getStandardFilename() {
        String filename = width + "x" + height;
        if (codecs != null) {
            filename += "_" + codecs;
        }
        filename += ext;
        return filename;
    }

}