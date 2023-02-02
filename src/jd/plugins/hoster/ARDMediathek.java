//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.config.MediathekProperties;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ardmediathek.de", "mediathek.daserste.de", "sandmann.de", "wdr.de", "sportschau.de", "wdrmaus.de", "eurovision.de", "sputnik.de", "mdr.de", "ndr.de", "tagesschau.de" }, urls = { "ardmediathek\\.dedecrypted://.+", "(?:mediathek\\.)?daserste\\.dedecrypted://.+", "sandmann\\.dedecrypted://.+", "wdr.dedecrypted://.+", "sportschau\\.dedecrypted://.+", "wdrmaus\\.dedecrypted://.+", "eurovision\\.dedecrypted://.+", "sputnik\\.dedecrypted://.+", "mdr\\.dedecrypted://.+", "ndr\\.dedecrypted://.+", "tagesschau\\.dedecrypted://.+" })
public class ARDMediathek extends PluginForHost {
    private String             dllink                           = null;
    public static final String PROPERTY_CRAWLER_FORCED_FILENAME = "crawler_forced_filename";
    public static final String PROPERTY_ITEM_ID                 = "itemId";

    public ARDMediathek(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "http://www.ardmediathek.de/ard/servlet/content/3606532";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String linkID = link.getSetLinkID();
        final String real_domain = new Regex(link.getDownloadURL(), "^(.+)decrypted://").getMatch(0);
        if (real_domain != null) {
            link.setUrlDownload(link.getDownloadURL().replace(real_domain + "decrypted://", "http://"));
            if (linkID == null) {
                link.setLinkID(linkID);
            }
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final MediathekProperties data = link.bindData(MediathekProperties.class);
        final String itemId = link.getStringProperty(PROPERTY_ITEM_ID);
        final String itemSrc = data.getSourceHost();
        final String itemType = data.getProtocol();
        final String itemRes = data.getResolution();
        if (itemId != null && itemSrc != null && itemType != null && itemRes != null) {
            String ret = itemSrc.concat("://").concat(itemId).concat("/").concat(itemType).concat("/").concat(itemRes);
            if (data.getAudioDescription()) {
                ret += "_AD";
            }
            return ret;
        }
        return super.getLinkID(link);
    }

    public static boolean isVideoContent(final URLConnectionAdapter con) {
        return con != null && con.getResponseCode() == 200 && StringUtils.containsIgnoreCase(con.getContentType(), "video") && con.getCompleteContentLength() > 512 * 1024l;
    }

    private static boolean isAudioContent(final URLConnectionAdapter con) {
        return con != null && con.getResponseCode() == 200 && StringUtils.containsIgnoreCase(con.getContentType(), "audio") && con.getCompleteContentLength() > 512 * 1024l;
    }

    private static boolean isSubtitleContent(final URLConnectionAdapter con) {
        return con != null && con.getResponseCode() == 200 && StringUtils.containsIgnoreCase(con.getContentType(), "text/xml");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = link.getPluginPatternMatcher();
        /* Keep filenames defined by crawler plugin even after user resets this link. */
        if (link.hasProperty(PROPERTY_CRAWLER_FORCED_FILENAME)) {
            link.setFinalFileName(link.getStringProperty(PROPERTY_CRAWLER_FORCED_FILENAME));
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        if (dllink.contains(".m3u8")) {
            checkFFProbe(link, "Check a HLS Stream");
            br.getPage(this.dllink);
            /* Check for offline and GEO-blocked */
            this.connectionErrorhandling(br.getHttpConnection());
            final List<M3U8Playlist> list = M3U8Playlist.parseM3U8(br);
            final HLSDownloader downloader = new HLSDownloader(link, br, br.getURL(), list);
            final StreamInfo streamInfo = downloader.getProbe();
            if (streamInfo == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                final long estimatedSize = downloader.getEstimatedSize();
                if (estimatedSize > 0) {
                    link.setDownloadSize(estimatedSize);
                }
            }
        } else if (isSubtitle(link)) {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Accept-Encoding", "identity");
                con = brc.openHeadConnection(dllink);
                if (!looksLikeDownloadableContent(con, link)) {
                    connectionErrorhandling(con);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                con = brc.openHeadConnection(dllink);
                if (!looksLikeDownloadableContent(con, link)) {
                    /* Content should definitely be offline in this case! */
                    connectionErrorhandling(con);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter con, final DownloadLink link) {
        if (super.looksLikeDownloadableContent(con)) {
            return true;
        } else if (isVideoContent(con) || isAudioContent(con) || con.isContentDisposition()) {
            return true;
        } else if (isSubtitle(link) && isSubtitleContent(con)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        download(link);
    }

    private void connectionErrorhandling(final URLConnectionAdapter con) throws PluginException {
        if (con.getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 403: GEO-blocked");
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private void download(final DownloadLink link) throws Exception {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            br.setFollowRedirects(true);
            // Workaround to avoid DOWNLOAD INCOMPLETE errors
            boolean resume = true;
            int maxChunks = 0;
            final boolean isSubtitle = isSubtitle(link);
            if (isSubtitle) {
                br.getHeaders().put("Accept-Encoding", "identity");
                link.setDownloadSize(0);
                resume = false;
                maxChunks = 1;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            if (!looksLikeDownloadableContent(dl.getConnection(), link)) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                this.connectionErrorhandling(dl.getConnection());
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
            if (this.dl.startDownload()) {
                this.postprocess(link);
            }
        }
    }

    private void postprocess(final DownloadLink link) {
        if (isSubtitle(link)) {
            if (!convertSubtitle(link)) {
                logger.severe("Subtitle conversion failed!");
            } else {
                link.setFinalFileName(this.correctOrApplyFileNameExtension(link.getFinalFileName(), ".srt"));
            }
        }
    }

    private boolean isSubtitle(final DownloadLink dl) {
        final MediathekProperties data_src = dl.bindData(MediathekProperties.class);
        if ("subtitle".equalsIgnoreCase(data_src.getStreamingType())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts the ARD Closed Captions subtitles to SRT subtitles. It is supposed to run after the completed download.
     *
     * @return The success of the conversion.
     */
    private boolean convertSubtitle(final DownloadLink downloadlink) {
        final File source = new File(downloadlink.getFileOutput());
        final StringBuilder xml = new StringBuilder();
        final String lineseparator = System.getProperty("line.separator");
        Scanner in = null;
        try {
            in = new Scanner(new InputStreamReader(new FileInputStream(source), "UTF-8"));
            while (in.hasNext()) {
                xml.append(in.nextLine() + lineseparator);
            }
        } catch (Exception e) {
            return false;
        } finally {
            in.close();
        }
        boolean success;
        final String xmlContent = xml.toString();
        /* They got two different subtitle formats */
        if (xmlContent.contains("<ebuttm:documentEbuttVersion>") || xmlContent.contains("<tts:documentEbuttVersion>")) {
            success = jd.plugins.hoster.BrDe.convertSubtitleBrDe(this, downloadlink, xmlContent, 0);
        } else {
            int counter = 1;
            BufferedWriter dest;
            try {
                dest = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(source.getAbsolutePath().replace(".xml", ".srt")), "UTF-8"));
            } catch (final IOException e1) {
                return false;
            }
            final String[] matches = new Regex(xmlContent, "(<p id=\"subtitle\\d+\".*?</p>)").getColumn(0);
            try {
                /* Find style --> color assignments */
                final HashMap<String, String> styles_color_names = new HashMap<String, String>();
                final String[] styles = new Regex(xmlContent, "(<style id=\"s\\d+\".*?/>)").getColumn(0);
                if (styles != null) {
                    for (final String style_info : styles) {
                        final String style_id = new Regex(style_info, "<style id=\"(s\\d+)\"").getMatch(0);
                        final String style_color = new Regex(style_info, "tts:color=\"([a-z]+)\"").getMatch(0);
                        if (style_id != null && style_color != null) {
                            styles_color_names.put(style_id, style_color);
                        }
                    }
                }
                /* Add static key value pairs. */
                styles_color_names.put("s1", "black");
                for (final String info : matches) {
                    dest.write(counter++ + lineseparator);
                    final DecimalFormat df = new DecimalFormat("00");
                    final Regex startInfo = new Regex(info, "begin=\"(\\d{2}):([^<>\"]*?)\"");
                    final Regex endInfo = new Regex(info, "end=\"(\\d{2}):([^<>\"]*?)\"");
                    int startHour = Integer.parseInt(startInfo.getMatch(0));
                    int endHour = Integer.parseInt(endInfo.getMatch(0));
                    startHour -= 10;
                    endHour -= 10;
                    final String start = df.format(startHour) + ":" + startInfo.getMatch(1).replace(".", ",");
                    final String end = df.format(endHour) + ":" + endInfo.getMatch(1).replace(".", ",");
                    dest.write(start + " --> " + end + lineseparator);
                    final String[][] color_texts = new Regex(info, "style=\"(s\\d+)\">?(.*?)</p>").getMatches();
                    String text = "";
                    for (final String[] style_text : color_texts) {
                        final String style = style_text[0];
                        text = style_text[1];
                        text = text.replaceAll(lineseparator, " ");
                        text = text.replaceAll("&apos;", "\\\\u0027");
                        text = Encoding.unicodeDecode(text);
                        text = HTMLEntities.unhtmlentities(text);
                        text = HTMLEntities.unhtmlAmpersand(text);
                        text = HTMLEntities.unhtmlAngleBrackets(text);
                        text = HTMLEntities.unhtmlSingleQuotes(text);
                        text = HTMLEntities.unhtmlDoubleQuotes(text);
                        text = text.replaceAll("<br />", lineseparator);
                        text = text.replaceAll("</?(p|span)>?", "");
                        text = text.trim();
                        final String remove_color = new Regex(text, "( ?tts:color=\"[a-z0-9]+\">)").getMatch(0);
                        if (remove_color != null) {
                            text = text.replace(remove_color, "");
                        }
                        final String color = styles_color_names.get(style);
                        final String color_code = getColorCode(color);
                        text = "<font color=#" + color_code + ">" + text + "</font>";
                    }
                    dest.write(text + lineseparator + lineseparator);
                }
                success = true;
            } catch (final Exception e) {
                success = false;
            } finally {
                try {
                    dest.close();
                } catch (IOException e) {
                }
            }
            source.delete();
        }
        return success;
    }

    private static String getColorCode(final String colorName) {
        /* Unhandled case/standard = white */
        String colorCode;
        if (colorName == null) {
            /* Use black for missing/not used/given colors */
            colorCode = "FFFFFF";
        } else if (colorName.equals("blue")) {
            colorCode = "0000FF";
        } else if (colorName.equals("yellow")) {
            colorCode = "FFFF00";
        } else if (colorName.equals("aqua")) {
            colorCode = "00FFFF";
        } else if (colorName.equals("lime")) {
            colorCode = "00FF00";
        } else if (colorName.equals("fuchsia")) {
            colorCode = "FF00FF";
        } else if (colorName.equals("green")) {
            colorCode = "008000";
        } else if (colorName.equals("cyan")) {
            colorCode = "00FFFF";
        } else {
            colorCode = "FFFFFF";
        }
        return colorCode;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}