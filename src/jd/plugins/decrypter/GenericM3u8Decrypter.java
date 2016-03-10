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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

//Decrypts embedded videos from dailymotion
@DecrypterPlugin(revision = "$Revision: 26321 $", interfaceVersion = 3, names = { "m3u8" }, urls = { "https?://.+\\.m3u8[^\\s<>\"']*" }, flags = { 0 })
public class GenericM3u8Decrypter extends PluginForDecrypt {

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public GenericM3u8Decrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            // invalid link
            return ret;
        }
        if (br.containsHTML("#EXT-X-STREAM-INF")) {
            for (String line : Regex.getLines(br.toString())) {
                if (!line.startsWith("#")) {
                    if (line.startsWith("http")) {
                        DownloadLink link = createDownloadlink(line);
                        ret.add(link);
                    } else {
                        DownloadLink link = createDownloadlink(br.getBaseURL() + line);
                        ret.add(link);
                    }
                }
            }
        } else {
            //
            DownloadLink link = createDownloadlink("m3u8" + param.getCryptedUrl().substring(4));
            if (br.containsHTML("EXT-X-KEY")) {
                link.setProperty("ENCRYPTED", true);
            }
            ret.add(link);
        }
        return ret;
    }

    /** Finds the highest video quality based on the max filesize. */
    public static HlsContainer findBestVideoByBandwidth(final ArrayList<HlsContainer> media) {
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

    public static ArrayList<HlsContainer> getHlsQualities(final Browser br) throws Exception {
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

    public static class HlsContainer {

        public static final String ext = ".mp4";
        public String              codecs;
        public String              downloadurl;
        public String              type;
        public int                 width;
        public int                 height;
        public long                bandwidth;

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

}