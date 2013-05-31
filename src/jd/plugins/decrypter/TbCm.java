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

package jd.plugins.decrypter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import de.savemytube.flv.FLV;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youtube.com" }, urls = { "https?://[\\w\\.]*?youtube\\.com/(embed/|.*?watch.*?v(%3D|=)|view_play_list\\?p=|playlist\\?(p|list)=|.*?g/c/|.*?grid/user/|v/|user/)[a-z\\-_A-Z0-9]+(.*?page=\\d+)?(.*?list=[a-z\\-_A-Z0-9]+)?" }, flags = { 0 })
public class TbCm extends PluginForDecrypt {
    private static AtomicBoolean PLUGIN_CHECKED  = new AtomicBoolean(false);
    private static AtomicBoolean PLUGIN_DISABLED = new AtomicBoolean(false);

    public TbCm(PluginWrapper wrapper) {
        super(wrapper);
    };

    private void canHandle() {
        if (PLUGIN_CHECKED.get()) return;
        String installerSource = null;
        try {
            installerSource = JDIO.readFileToString(JDUtilities.getResourceFile("src.dat"));
            PLUGIN_DISABLED.set(installerSource.contains("\"PS\""));
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            PLUGIN_CHECKED.set(true);
        }
    }

    public static enum DestinationFormat {
        AUDIOMP3("Audio (MP3)", new String[] { ".mp3" }),
        VIDEOFLV("Video (FLV)", new String[] { ".flv" }),
        VIDEOMP4("Video (MP4)", new String[] { ".mp4" }),
        VIDEOWEBM("Video (Webm)", new String[] { ".webm" }),
        VIDEO3GP("Video (3GP)", new String[] { ".3gp" }),
        UNKNOWN("Unknown (unk)", new String[] { ".unk" }),
        VIDEOIPHONE("Video (IPhone)", new String[] { ".mp4" });

        private String   text;
        private String[] ext;

        DestinationFormat(final String text, final String[] ext) {
            this.text = text;
            this.ext = ext;
        }

        public String getExtFirst() {
            return this.ext[0];
        }

        public String getText() {
            return this.text;
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

    static class Info {
        public String link;
        public long   size;
        public int    fmt;
        public String desc;
    }

    private final Pattern                       StreamingShareLink  = Pattern.compile("\\< streamingshare=\"youtube\\.com\" name=\"(.*?)\" dlurl=\"(.*?)\" brurl=\"(.*?)\" convertto=\"(.*?)\" comment=\"(.*?)\" \\>", Pattern.CASE_INSENSITIVE);

    static public final Pattern                 YT_FILENAME_PATTERN = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);
    private static final String                 UNSUPPORTEDRTMP     = "itag%2Crtmpe%2";

    HashMap<DestinationFormat, ArrayList<Info>> possibleconverts    = null;

    private static final String                 TEMP_EXT            = ".tmp$";
    private boolean                             pluginloaded        = false;
    private boolean                             verifyAge           = false;

    private HashMap<String, FilePackage>        filepackages        = new HashMap<String, FilePackage>();

    private static final String                 NAME_SUBTITLES      = "subtitles";
    private static final String                 NAME_THUMBNAILS     = "thumbnails";

    public static boolean ConvertFile(final DownloadLink downloadlink, final DestinationFormat InType, final DestinationFormat OutType) {
        System.out.println("Convert " + downloadlink.getName() + " - " + InType.getText() + " - " + OutType.getText());
        if (InType.equals(OutType)) {
            System.out.println("No Conversion needed, renaming...");
            final File oldone = new File(downloadlink.getFileOutput());
            final File newone = new File(downloadlink.getFileOutput().replaceAll(TbCm.TEMP_EXT, OutType.getExtFirst()));
            downloadlink.setFinalFileName(downloadlink.getName().replaceAll(TbCm.TEMP_EXT, OutType.getExtFirst()));
            oldone.renameTo(newone);
            return true;
        }

        downloadlink.getLinkStatus().setStatusText(JDL.L("convert.progress.convertingto", "convert to") + " " + OutType.toString());

        switch (InType) {
        case VIDEOFLV:
            // Inputformat FLV
            switch (OutType) {
            case AUDIOMP3:
                System.out.println("Convert FLV to mp3...");
                new FLV(downloadlink.getFileOutput(), true, true);

                // FLV löschen
                new File(downloadlink.getFileOutput()).delete();
                // AVI löschen
                new File(downloadlink.getFileOutput().replaceAll(TbCm.TEMP_EXT, ".avi")).delete();

                return true;
            default:
                System.out.println("Don't know how to convert " + InType.getText() + " to " + OutType.getText());
                downloadlink.getLinkStatus().setErrorMessage(JDL.L("convert.progress.unknownintype", "Unknown format"));
                return false;
            }
        default:
            System.out.println("Don't know how to convert " + InType.getText() + " to " + OutType.getText());
            downloadlink.getLinkStatus().setErrorMessage(JDL.L("convert.progress.unknownintype", "Unknown format"));
            return false;
        }
    }

    /**
     * Converts the Google Closed Captions subtitles to SRT subtitles. It runs after the completed download.
     * 
     * @param downloadlink
     *            . The finished link to the Google CC subtitle file.
     * @return The success of the conversion.
     */
    public static boolean convertSubtitle(final DownloadLink downloadlink) {
        final File source = new File(downloadlink.getFileOutput());

        BufferedWriter dest;
        try {
            dest = new BufferedWriter(new FileWriter(new File(source.getAbsolutePath().replace(".xml", ".srt"))));
        } catch (IOException e1) {
            return false;
        }

        final StringBuilder xml = new StringBuilder();
        int counter = 1;
        final String lineseparator = System.getProperty("line.separator");

        Scanner in = null;
        try {
            in = new Scanner(new FileReader(source));
            while (in.hasNext()) {
                xml.append(in.nextLine() + lineseparator);
            }
        } catch (Exception e) {
            return false;
        } finally {
            in.close();
        }

        String[][] matches = new Regex(xml.toString(), "<text start=\"(.*?)\" dur=\"(.*?)\">(.*?)</text>").getMatches();

        try {
            for (String[] match : matches) {
                dest.write(counter++ + lineseparator);

                Double start = Double.valueOf(match[0]);
                Double end = start + Double.valueOf(match[1]);
                dest.write(convertSubtitleTime(start) + " --> " + convertSubtitleTime(end) + lineseparator);

                String text = match[2].trim();
                text = text.replaceAll(lineseparator, " ");
                text = text.replaceAll("&amp;", "&");
                text = text.replaceAll("&quot;", "\"");
                text = text.replaceAll("&#39;", "'");
                dest.write(text + lineseparator + lineseparator);
            }
        } catch (Exception e) {
            return false;
        } finally {
            try {
                dest.close();
            } catch (IOException e) {
            }
        }

        source.delete();

        return true;
    }

    /**
     * Converts the the time of the Google format to the SRT format.
     * 
     * @param time
     *            . The time from the Google XML.
     * @return The converted time as String.
     */
    private static String convertSubtitleTime(Double time) {
        String hour = "00";
        String minute = "00";
        String second = "00";
        String millisecond = "0";

        Integer itime = Integer.valueOf(time.intValue());

        // Hour
        Integer timeHour = Integer.valueOf(itime.intValue() / 3600);
        if (timeHour < 10) {
            hour = "0" + timeHour.toString();
        } else {
            hour = timeHour.toString();
        }

        // Minute
        Integer timeMinute = Integer.valueOf((itime.intValue() % 3600) / 60);
        if (timeMinute < 10) {
            minute = "0" + timeMinute.toString();
        } else {
            minute = timeMinute.toString();
        }

        // Second
        Integer timeSecond = Integer.valueOf(itime.intValue() % 60);
        if (timeSecond < 10) {
            second = "0" + timeSecond.toString();
        } else {
            second = timeSecond.toString();
        }

        // Millisecond
        millisecond = String.valueOf(time - itime).split("\\.")[1];
        if (millisecond.length() == 1) millisecond = millisecond + "00";
        if (millisecond.length() == 2) millisecond = millisecond + "0";
        if (millisecond.length() > 2) millisecond = millisecond.substring(0, 3);

        // Result
        String result = hour + ":" + minute + ":" + second + "," + millisecond;

        return result;
    }

    private void gsProxy(boolean b) {
        SubConfiguration cfg = SubConfiguration.getConfig("youtube.com");
        if (cfg != null && cfg.getBooleanProperty("PROXY_ACTIVE")) {
            String PROXY_ADDRESS = cfg.getStringProperty("PROXY_ADDRESS");
            int PROXY_PORT = cfg.getIntegerProperty("PROXY_PORT");
            if (isEmpty(PROXY_ADDRESS) || PROXY_PORT < 0) return;
            PROXY_ADDRESS = new Regex(PROXY_ADDRESS, "^[0-9a-zA-Z]+://").matches() ? PROXY_ADDRESS : "http://" + PROXY_ADDRESS;
            org.appwork.utils.net.httpconnection.HTTPProxy proxy = org.appwork.utils.net.httpconnection.HTTPProxy.parseHTTPProxy(PROXY_ADDRESS + ":" + PROXY_PORT);
            if (b && proxy != null && proxy.getHost() != null) {
                br.setProxy(proxy);
                return;
            }
        }
        br.setProxy(br.getThreadProxy());
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private void addtopos(final DestinationFormat mode, final String link, final long size, final String desc, final int fmt) {
        ArrayList<Info> info = this.possibleconverts.get(mode);
        if (info == null) {
            info = new ArrayList<Info>();
            this.possibleconverts.put(mode, info);
        }
        final Info tmp = new Info();
        tmp.link = link;
        tmp.size = size;
        tmp.desc = desc;
        tmp.fmt = fmt;
        info.add(tmp);
    }

    public boolean canHandle(final String data) {
        canHandle();
        if (PLUGIN_DISABLED.get() == true) return false;
        return super.canHandle(data);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        canHandle();
        this.possibleconverts = new HashMap<DestinationFormat, ArrayList<Info>>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (PLUGIN_DISABLED.get() == true) return decryptedLinks;
        String parameter = param.toString().replace("watch#!v", "watch?v");
        parameter = parameter.replaceFirst("(verify_age\\?next_url=\\/?)", "");
        parameter = parameter.replaceFirst("(%3Fv%3D)", "?v=");
        parameter = parameter.replaceFirst("(watch\\?.*?v)", "watch?v");
        parameter = parameter.replaceFirst("/embed/", "/watch?v=");
        parameter = parameter.replaceFirst("https", "http");

        this.br.setFollowRedirects(true);
        this.br.setCookiesExclusive(true);
        this.br.clearCookies("youtube.com");
        br.setCookie("http://youtube.com", "PREF", "hl=en-GB");
        if (parameter.contains("watch#")) {
            parameter = parameter.replace("watch#", "watch?");
        }
        if (parameter.contains("v/")) {
            String id = new Regex(parameter, "v/([a-z\\-_A-Z0-9]+)").getMatch(0);
            if (id != null) parameter = "http://www.youtube.com/watch?v=" + id;
        }

        ArrayList<String> linkstodecrypt = new ArrayList<String>();

        boolean prem = false;
        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    prem = this.login(n);
                    break;
                }
            }
        }

        boolean multiple_videos = true;

        // Check if link contains a viedo and a playlist
        int choice = -1;
        if (parameter.contains("list=") && parameter.contains("watch?v=")) {
            choice = getPluginConfig().getIntegerProperty("ISVIDEOANDPLAYLIST", 2);
            // choice == 0 -> Only video
            // choice == 1 -> Playlist
            // choice == 2 -> Ask

            if (choice == 2) {
                int ret = UserIO.getInstance().requestConfirmDialog(0, parameter, JDL.L("plugins.host.youtube.isvideoandplaylist.question.message", "The Youtube link contains a video an a playlist. What do you want do download?"), null, JDL.L("plugins.host.youtube.isvideoandplaylist.question.onlyvideo", "Only video"), JDL.L("plugins.host.youtube.isvideoandplaylist.question.playlist", "Complete playlist"));
                // Video selected
                if (ret == 2) choice = 0;
                // Playlist selected
                if (ret == 4) choice = 1;
            }

            if (choice == 0) {
                parameter = new Regex(parameter, "(http://www\\.youtube\\.com/watch\\?v=[a-z\\-_A-Z0-9]+).*?").getMatch(0);
            }
        }

        // Parse playlist
        if (parameter.contains("view_play_list") || parameter.contains("playlist") || parameter.contains("g/c/") || parameter.contains("grid/user/") || choice == 1) {
            if (parameter.contains("g/c/") || parameter.contains("grid/user/")) {
                String id = new Regex(parameter, "g/c/([a-z\\-_A-Z0-9]+)").getMatch(0);
                if (id == null) {
                    id = new Regex(parameter, "grid/user/([a-z\\-_A-Z0-9]+)").getMatch(0);
                    if (id == null) {
                        id = new Regex(parameter, "youtube\\.com/playlist\\?list=(.+)").getMatch(0);
                    }
                }
                if (id != null) parameter = "http://www.youtube.com/view_play_list?p=" + id;
            }

            String page = parameter;

            // Parse every page to get all videos
            do {
                this.br.getPage(page);
                String[] videos = this.br.getRegex("<a href=\"(/watch\\?v=[a-z\\-_A-Z0-9]+)\\&amp;list=[a-z\\-_A-Z0-9]+\\&amp;index=\\d+").getColumn(0);
                if (parameter.contains("list=") && parameter.contains("watch?v=")) videos = this.br.getRegex("<a class=\"yt\\-uix\\-contextlink \" href=\"(/watch\\?v=[a-z\\-_A-Z0-9]+)\\&amp;list=[a-z\\-_A-Z0-9]+").getColumn(0);
                if (videos.length == 0) videos = this.br.getRegex("<a href=\"(/watch\\?v=[a-z\\-_A-Z0-9]+)\\&amp;list=[a-z\\-_A-Z0-9]+").getColumn(0);
                for (String video : videos) {
                    video = Encoding.htmlDecode(video);
                    linkstodecrypt.add("http://www.youtube.com" + video);
                }

                // Get all page links
                String[][] next = br.getRegex("<a href=\"(/playlist\\?list=[a-z\\-_A-Z0-9]+\\&amp;page=\\d+)\" class=\"yt\\-uix\\-button  yt\\-uix\\-pager\\-button yt\\-uix\\-sessionlink yt\\-uix\\-button\\-hh\\-default\" data-sessionlink=\"[A-Za-z0-9=%\\-_]+\" data-page=\"\\d+\"><span class=\"yt\\-uix\\-button\\-content\">[^0-9]+</span></a>").getMatches();

                if (next.length == 2 || (next.length == 1 && linkstodecrypt.size() == 100)) {
                    page = "http://www.youtube.com" + unescape(next[next.length - 1][0]);
                } else {
                    page = "";
                }
            } while (page.length() != 0);
        } else if (parameter.contains("/user/")) {
            // Handle user links
            parameter = "http://www.youtube.com/user/" + new Regex(parameter, "youtube\\.com/user/([a-z\\-_A-Z0-9]+)($|\\?.*?)").getMatch(0);
            br.getPage(parameter + "/videos?view=0");
            if (br.containsHTML(">404 Not Found<")) {
                logger.info("The following link is offline: " + parameter);
                return decryptedLinks;
            }

            String next = null;

            do {
                String content = unescape(br.toString());

                if (content.contains("iframe style=\"display:block;border:0;\" src=\"/error")) throw new PluginException(LinkStatus.ERROR_RETRY, "An unkown error occured");

                String[][] links = new Regex(content, "a href=\"(/watch\\?v=[a-z\\-_A-Z0-9]+)\" class=\"ux\\-thumb\\-wrap yt\\-uix\\-sessionlink yt\\-uix\\-contextlink contains\\-addto spf\\-link\"").getMatches();

                for (String[] url : links) {
                    String video = Encoding.htmlDecode(url[0]);
                    if (!video.startsWith("http://www.youtube.com")) video = "http://www.youtube.com" + video;
                    linkstodecrypt.add(video);
                }

                next = new Regex(content, "button class=\"yt\\-uix\\-load\\-more load\\-more\\-button yt\\-uix\\-button yt\\-uix\\-button\\-hh\\-default\" type=\"button\" onclick=\";return false;\" data\\-uix\\-load\\-more\\-href=\"(.*?)\"").getMatch(0);
                if (next == null) next = new Regex(content, "button onclick=\";return false;\" class=\"yt\\-uix\\-load\\-more load\\-more\\-button yt\\-uix\\-button yt\\-uix\\-button\\-hh\\-default\" type=\"button\" data\\-uix\\-load\\-more\\-href=\"(.*?)\"").getMatch(0);
                if (next == null) next = new Regex(content, "button type=\"button\" class=\"yt\\-uix\\-load\\-more load\\-more\\-button yt\\-uix\\-button yt\\-uix\\-button\\-hh\\-default\" onclick=\";return false;\" data\\-uix\\-load\\-more\\-href=\"(.*?)\"").getMatch(0);
                if (next == null) next = new Regex(content, "button onclick=\";return false;\" type=\"button\" class=\"yt\\-uix\\-load\\-more load\\-more\\-button yt\\-uix\\-button yt\\-uix\\-button\\-hh\\-default\" data\\-uix\\-load\\-more\\-href=\"(.*?)\"").getMatch(0);

                if (next != null && next.length() > 0) {
                    br.getPage("http://www.youtube.com" + next);
                }
            } while (next != null && next.length() > 0);
        } else {
            // Handle single video
            linkstodecrypt.add(parameter);
            multiple_videos = false;
        }

        final SubConfiguration cfg = SubConfiguration.getConfig("youtube.com");
        boolean fast = cfg.getBooleanProperty("FAST_CHECK2", false);
        final boolean best = cfg.getBooleanProperty("ALLOW_BEST2", false);
        final AtomicBoolean mp3 = new AtomicBoolean(cfg.getBooleanProperty("ALLOW_MP3_V2", true));
        final AtomicBoolean flv = new AtomicBoolean(cfg.getBooleanProperty("ALLOW_FLV_V2", true));
        /* http://en.wikipedia.org/wiki/YouTube */
        final HashMap<Integer, Object[]> ytVideo = new HashMap<Integer, Object[]>() {
            private static final long serialVersionUID = -3028718522449785181L;

            {
                boolean threeD = cfg.getBooleanProperty("ALLOW_3D_V2", true);
                boolean mp4 = cfg.getBooleanProperty("ALLOW_MP4_V2", true);
                boolean webm = cfg.getBooleanProperty("ALLOW_WEBM_V2", true);
                boolean threegp = cfg.getBooleanProperty("ALLOW_3GP_V2", true);
                if (mp3.get() == false && mp4 == false && webm == false && flv.get() == false && threegp == false) {
                    /* if no container is selected, then everything is enabled */
                    mp3.set(true);
                    mp4 = true;
                    webm = true;
                    flv.set(true);
                    threegp = true;
                }

                /** User selected nothing -> Decrypt everything */
                boolean q144p = cfg.getBooleanProperty("ALLOW_144P_V2", true);
                boolean q240p = cfg.getBooleanProperty("ALLOW_240P_V2", true);
                boolean q360p = cfg.getBooleanProperty("ALLOW_360P_V2", true);
                boolean q480p = cfg.getBooleanProperty("ALLOW_480P_V2", true);
                boolean q520p = cfg.getBooleanProperty("ALLOW_520P_V2", true);
                boolean q720p = cfg.getBooleanProperty("ALLOW_720P_V2", true);
                boolean q1080p = cfg.getBooleanProperty("ALLOW_1080P_V2", true);
                boolean q3072p = cfg.getBooleanProperty("ALLOW_ORIGINAL_V2", true);
                if (!q240p && !q360p && !q480p && !q520p && !q720p && !q1080p && !q3072p && !threeD) {
                    q240p = true;
                    q360p = true;
                    q480p = true;
                    q520p = true;
                    q720p = true;
                    q1080p = true;
                    q3072p = true;
                    threeD = true;
                }

                // **** FLV *****
                if (mp3.get()) {
                    this.put(0, new Object[] { DestinationFormat.AUDIOMP3, "H.263", "MP3", "Mono" });
                    this.put(5, new Object[] { DestinationFormat.AUDIOMP3, "H.263", "MP3", "Stereo" });
                    this.put(6, new Object[] { DestinationFormat.AUDIOMP3, "H.263", "MP3", "Mono" });
                }
                if (flv.get()) {
                    if (q240p) {
                        // video bit rate @ 0.25Mbit/second
                        this.put(5, new Object[] { DestinationFormat.VIDEOFLV, "H.263", "MP3", "Stereo", "240p" });
                    }
                    if (q240p) {
                        // video bit rate @ 0.8Mbit/second
                        this.put(6, new Object[] { DestinationFormat.VIDEOFLV, "H.263", "MP3", "Stereo", "240p" });
                    }
                    if (q360p) {
                        this.put(34, new Object[] { DestinationFormat.VIDEOFLV, "H.264", "AAC", "Stereo", "360p" });
                    }
                    if (q480p) {
                        this.put(35, new Object[] { DestinationFormat.VIDEOFLV, "H.264", "AAC", "Stereo", "480p" });
                    }
                }

                // **** 3GP *****
                if (threegp) {
                    // according to wiki the video format is unknown. we rate this as mono! need to look into 3gp more to confirm.
                    if (q144p) {
                        this.put(17, new Object[] { DestinationFormat.VIDEO3GP, "H.264", "AAC", "Stereo", "144p" });
                    }
                    if (q240p) {
                        this.put(13, new Object[] { DestinationFormat.VIDEO3GP, "H.263", "AAC", "Mono", "240p" });
                    }
                    if (q240p) {
                        this.put(36, new Object[] { DestinationFormat.VIDEO3GP, "H.264", "AAC", "Stereo", "240p" });
                    }
                }

                // **** MP4 *****
                if (mp4) {
                    if (q360p) {
                        // 270p / 360p
                        this.put(18, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo", "360p" });
                    }
                    if (q720p) {
                        this.put(22, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo", "720p" });
                    }
                    if (q1080p) {
                        this.put(37, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo", "1080p" });
                    }
                    if (q3072p) {
                        this.put(38, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo", "3072p" });
                    }
                }

                // **** WebM *****
                if (webm) {
                    if (q360p) {
                        this.put(43, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo", "360p" });
                    }
                    if (q480p) {
                        this.put(44, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo", "480p" });
                    }
                    if (q720p) {
                        this.put(45, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo", "720p" });
                    }
                    if (q1080p) {
                        this.put(46, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo", "1080p" });
                    }
                }

                // **** 3D *****/
                if (threeD) {
                    if (webm) {
                        if (q360p) {
                            this.put(100, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo", "360p" });
                        }
                        if (q360p) {
                            this.put(101, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo", "360p" });
                        }
                        if (q720p) {
                            this.put(102, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo", "720p" });
                        }
                    }
                    if (mp4) {
                        if (q240p) {
                            this.put(83, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo", "240p" });
                        }
                        if (q360p) {
                            this.put(82, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo", "360p" });
                        }
                        if (q520p) {
                            this.put(85, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo", "520p" });
                        }
                        if (q720p) {
                            this.put(84, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo", "720p" });
                        }
                    }
                }
            }
        };

        // Force fast linkcheck if there are more then 20 videos in queue.
        if (linkstodecrypt.size() > 20) fast = true;

        for (String url : linkstodecrypt) {
            // Make an little sleep to prevent DDoS
            Thread.sleep(25);

            try {
                this.possibleconverts.clear();

                if (this.StreamingShareLink.matcher(url).matches()) {
                    // StreamingShareLink

                    final String[] info = new Regex(url, this.StreamingShareLink).getMatches()[0];

                    for (final String debug : info) {
                        logger.info(debug);
                    }
                    final DownloadLink thislink = this.createDownloadlink(info[1]);
                    thislink.setProperty("ALLOW_DUPE", true);
                    thislink.setBrowserUrl(info[2]);
                    thislink.setFinalFileName(info[0]);
                    thislink.setProperty("convertto", info[3]);

                    decryptedLinks.add(thislink);
                    // return decryptedLinks;
                    continue;
                }
                verifyAge = false;
                HashMap<Integer, String[]> LinksFound = this.getLinks(url, prem, this.br, 0);
                String error = br.getRegex("<div id=\"unavailable\\-message\" class=\"\">[\t\n\r ]+<span class=\"yt\\-alert\\-vertical\\-trick\"></span>[\t\n\r ]+<div class=\"yt\\-alert\\-message\">([^<>\"]*?)</div>").getMatch(0);
                // Removed due wrong offline detection
                // if (error == null) error = br.getRegex("<div class=\"yt\\-alert\\-message\">(.*?)</div>").getMatch(0);
                if (error == null) error = br.getRegex("\\&reason=([^<>\"/]*?)\\&").getMatch(0);
                if (br.containsHTML(UNSUPPORTEDRTMP)) error = "RTMP video download isn't supported yet!";
                if ((LinksFound == null || LinksFound.isEmpty()) && error != null) {
                    error = Encoding.urlDecode(error, false);
                    logger.info("Video unavailable: " + url);
                    logger.info("Reason: " + error.trim());
                    continue;
                }
                if (LinksFound == null || LinksFound.isEmpty()) {
                    if (linkstodecrypt.size() == 1) {
                        if (verifyAge || this.br.getURL().toLowerCase().indexOf("youtube.com/get_video_info?") != -1 && !prem) { throw new DecrypterException(DecrypterException.ACCOUNT); }
                        logger.info("Video unavailable: " + url);
                        continue;
                    } else {
                        continue;
                    }
                }

                /* First get the filename */
                String YT_FILENAME = "";
                if (LinksFound.containsKey(-1)) {
                    YT_FILENAME = LinksFound.get(-1)[0];
                    LinksFound.remove(-1);
                }

                // Use uploader name in filename
                if (cfg.getBooleanProperty("USEUPLOADERINNAME", false)) {
                    String uploadername = br.getRegex("feature=watch\" dir=\"ltr\">(.*?)</a><span class=\"yt\\-user\\-separator\">").getMatch(0);
                    if (uploadername != null) YT_FILENAME = uploadername + " - " + YT_FILENAME;
                }

                if (cfg.getBooleanProperty("IDINFILENAME_V2", false) && !cfg.getBooleanProperty("ISASFILENAME", false)) {
                    String id = new Regex(url, "v=([a-z\\-_A-Z0-9]+)").getMatch(0);
                    if (id != null) YT_FILENAME = YT_FILENAME + " - " + id;
                }

                /* prefer videoID as filename? */
                if (cfg.getBooleanProperty("ISASFILENAME", false)) {
                    String id = new Regex(url, "v=([a-z\\-_A-Z0-9]+)").getMatch(0);
                    if (id != null) YT_FILENAME = id;
                }

                /*
                 * We match against users resolution and file encoding type. This allows us to use their upper and lower limits. It will
                 * return multiple results if they are in the same quality rating
                 */
                if (best) {
                    final HashMap<Integer, String[]> bestFound = new HashMap<Integer, String[]>();
                    if (LinksFound.get(38) != null && ytVideo.containsKey(38)) {
                        // 3072p mp4
                        if (LinksFound.containsKey(38) && ytVideo.containsKey(38)) bestFound.put(38, LinksFound.get(38));
                    } else if ((LinksFound.containsKey(37) && ytVideo.containsKey(37)) || (LinksFound.containsKey(46) && ytVideo.containsKey(46))) {
                        // 1080p mp4
                        if (LinksFound.containsKey(37) && ytVideo.containsKey(37)) bestFound.put(37, LinksFound.get(37));
                        // 1080p webm
                        if (LinksFound.containsKey(46) && ytVideo.containsKey(46)) bestFound.put(46, LinksFound.get(46));
                    } else if ((LinksFound.containsKey(22) && ytVideo.containsKey(22)) || (LinksFound.containsKey(45) && ytVideo.containsKey(45)) || (LinksFound.containsKey(84) && ytVideo.containsKey(84)) || (LinksFound.containsKey(102) && ytVideo.containsKey(102))) {
                        // 720p mp4
                        if (LinksFound.containsKey(22) && ytVideo.containsKey(22)) bestFound.put(22, LinksFound.get(22));
                        // 720p webm
                        if (LinksFound.containsKey(45) && ytVideo.containsKey(45)) bestFound.put(45, LinksFound.get(45));
                        // 720p(3d) mp4
                        if (LinksFound.containsKey(84) && ytVideo.containsKey(84)) bestFound.put(84, LinksFound.get(84));
                        // 720p(3d) webm
                        if (LinksFound.containsKey(102) && ytVideo.containsKey(102)) bestFound.put(102, LinksFound.get(102));
                    } else if (LinksFound.containsKey(85) && ytVideo.containsKey(85)) {
                        // 520p(3d) mp4
                        if (LinksFound.containsKey(85) && ytVideo.containsKey(85)) bestFound.put(85, LinksFound.get(85));
                    } else if ((LinksFound.containsKey(35) && ytVideo.containsKey(35)) || (LinksFound.containsKey(44) && ytVideo.containsKey(44))) {
                        // 480p flv
                        if (LinksFound.containsKey(35) && ytVideo.containsKey(35)) bestFound.put(35, LinksFound.get(35));
                        // 480p webm
                        if (LinksFound.containsKey(44) && ytVideo.containsKey(44)) bestFound.put(44, LinksFound.get(44));
                    } else if ((LinksFound.containsKey(18) && ytVideo.containsKey(18)) || (LinksFound.containsKey(34) && ytVideo.containsKey(34)) || (LinksFound.containsKey(43) && ytVideo.containsKey(43)) || (LinksFound.containsKey(82) && ytVideo.containsKey(82) || (LinksFound.containsKey(100) && ytVideo.containsKey(100)) || (LinksFound.containsKey(101) && ytVideo.containsKey(101)))) {
                        // 360p mp4
                        if (LinksFound.containsKey(18) && ytVideo.containsKey(18)) bestFound.put(18, LinksFound.get(18));
                        // 360p flv
                        if (LinksFound.containsKey(34) && ytVideo.containsKey(34)) bestFound.put(34, LinksFound.get(34));
                        // 360p webm
                        if (LinksFound.containsKey(43) && ytVideo.containsKey(43)) bestFound.put(43, LinksFound.get(43));
                        // 360p(3d) mp4
                        if (LinksFound.containsKey(82) && ytVideo.containsKey(82)) bestFound.put(82, LinksFound.get(82));
                        // 360p(3d) webm ** need to figure out which is what, could create a dupe when saving .
                        if (LinksFound.containsKey(100) && ytVideo.containsKey(100)) bestFound.put(100, LinksFound.get(100));
                        // 360p(3d) webm ** need to figure out which is what, could create a dupe when saving.
                        if (LinksFound.containsKey(101) && ytVideo.containsKey(101)) bestFound.put(101, LinksFound.get(101));
                    } else if ((LinksFound.containsKey(5) && ytVideo.containsKey(5)) || (LinksFound.containsKey(6) && ytVideo.containsKey(6)) || (LinksFound.containsKey(13) && ytVideo.containsKey(13)) || (LinksFound.containsKey(36) && ytVideo.containsKey(36)) || (LinksFound.containsKey(83) && ytVideo.containsKey(83))) {
                        // 240p flv @ video bit rate @ 0.25Mbit/second
                        if (LinksFound.containsKey(5) && ytVideo.containsKey(5) && !LinksFound.containsKey(6))
                            bestFound.put(5, LinksFound.get(5));
                        // 240p flv @ video bit rate @ 0.8Mbit/second
                        else if (LinksFound.containsKey(6) && ytVideo.containsKey(6)) bestFound.put(6, LinksFound.get(6));
                        // 240p 3gp mono ** according to wiki this has the highest video rate. but we rate this as mono! need to look into
                        // 3gp more to confirm
                        if (LinksFound.containsKey(13) && ytVideo.containsKey(13) && !LinksFound.containsKey(36))
                            bestFound.put(13, LinksFound.get(13));
                        // 240p 3gp stereo
                        else if (LinksFound.containsKey(36) && ytVideo.containsKey(36)) bestFound.put(36, LinksFound.get(36));
                        // 240p(3d) mp4
                        if (LinksFound.containsKey(83) && ytVideo.containsKey(83)) bestFound.put(83, LinksFound.get(83));
                    } else if ((LinksFound.containsKey(17) && ytVideo.containsKey(17))) {
                        // 144p 3gp
                        if (LinksFound.containsKey(17) && ytVideo.containsKey(17)) bestFound.put(17, LinksFound.get(17));
                    }
                    if (bestFound.isEmpty()) {
                        logger.warning("ERROR: in best code");
                        return null;
                    } else {
                        LinksFound = bestFound;
                    }
                }

                String dlLink = "";
                String vQuality = "";
                DestinationFormat cMode = null;

                for (final Integer format : LinksFound.keySet()) {
                    if (ytVideo.containsKey(format)) {
                        cMode = (DestinationFormat) ytVideo.get(format)[0];
                        vQuality = "(" + LinksFound.get(format)[1] + "_" + ytVideo.get(format)[1] + "-" + ytVideo.get(format)[2] + ")";
                    } else {
                        cMode = DestinationFormat.UNKNOWN;
                        vQuality = "(" + LinksFound.get(format)[1] + "_" + format + ")";
                        /*
                         * we do not want to download unknown formats at the moment
                         */
                        continue;
                    }
                    dlLink = LinksFound.get(format)[0];
                    // Skip MP3 but handle 240p flv
                    if (!(format == 5 && mp3.get() && !flv.get())) {
                        try {
                            if (fast) {
                                this.addtopos(cMode, dlLink, 0, vQuality, format);
                            } else if (this.br.openGetConnection(dlLink).getResponseCode() == 200) {
                                Thread.sleep(200);
                                this.addtopos(cMode, dlLink, this.br.getHttpConnection().getLongContentLength(), vQuality, format);
                            }
                        } catch (final Throwable e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                this.br.getHttpConnection().disconnect();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                    // Handle MP3
                    if ((format == 0 || format == 5 || format == 6) && mp3.get()) {
                        try {
                            if (fast) {
                                this.addtopos(DestinationFormat.AUDIOMP3, dlLink, 0, "", format);
                            } else if (this.br.openGetConnection(dlLink).getResponseCode() == 200) {
                                Thread.sleep(200);
                                this.addtopos(DestinationFormat.AUDIOMP3, dlLink, this.br.getHttpConnection().getLongContentLength(), "", format);
                            }
                        } catch (final Throwable e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                this.br.getHttpConnection().disconnect();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                }

                for (final Entry<DestinationFormat, ArrayList<Info>> next : this.possibleconverts.entrySet()) {
                    final DestinationFormat convertTo = next.getKey();

                    // create a package, for each quality.
                    FilePackage filePackage = filepackages.get(convertTo.getText());
                    if (filePackage == null) {
                        filePackage = FilePackage.getInstance();
                        filePackage.setProperty("ALLOW_MERGE", true);
                        if (multiple_videos || cfg.getBooleanProperty("GROUP_FORMAT", false))
                            filePackage.setName("YouTube " + convertTo.getText());
                        else
                            filePackage.setName(YT_FILENAME + " " + convertTo.getText());
                        filepackages.put(convertTo.getText(), filePackage);
                    }

                    for (final Info info : next.getValue()) {
                        final DownloadLink thislink = this.createDownloadlink(info.link.replaceFirst("http", "httpJDYoutube"));
                        thislink.setProperty("ALLOW_DUPE", true);
                        filePackage.add(thislink);
                        thislink.setBrowserUrl(url);
                        String desc = info.desc;
                        if (cfg.getBooleanProperty("FORMATINNAME", true) == false) {
                            desc = "";
                        }
                        switch (info.fmt) {
                        case 82:
                        case 83:
                        case 84:
                        case 85:
                        case 100:
                        case 101:
                        case 102:
                            desc = "(3D)" + desc;
                            break;
                        }
                        thislink.setFinalFileName(YT_FILENAME + desc + convertTo.getExtFirst());
                        thislink.setProperty("size", info.size);
                        String name = null;
                        if (convertTo != DestinationFormat.AUDIOMP3) {
                            name = YT_FILENAME + desc + convertTo.getExtFirst();
                            thislink.setProperty("name", name);
                        } else {
                            // because demuxer will fail when mp3 file already exists
                            name = YT_FILENAME + desc + ".tmp";
                            thislink.setProperty("name", name);
                        }
                        thislink.setProperty("convertto", convertTo.name());
                        thislink.setProperty("videolink", url);
                        thislink.setProperty("valid", true);
                        thislink.setProperty("fmtNew", info.fmt);
                        thislink.setProperty("LINKDUPEID", name);
                        decryptedLinks.add(thislink);
                    }
                }

                final String VIDEOID = new Regex(url, "watch\\?v=([\\w_\\-]+)").getMatch(0);

                // Grab Subtitles
                if (cfg.getBooleanProperty("ALLOW_SUBTITLES", true)) {
                    br.getPage("http://video.google.com/timedtext?type=list&v=" + VIDEOID);

                    FilePackage filePackage = filepackages.get(NAME_SUBTITLES);

                    if (filePackage == null) {
                        filePackage = FilePackage.getInstance();
                        filePackage.setProperty("ALLOW_MERGE", true);
                        if (multiple_videos)
                            filePackage.setName("Youtube (Subtitles)");
                        else
                            filePackage.setName(YT_FILENAME + " (Subtitles)");
                        filepackages.put(NAME_SUBTITLES, filePackage);
                    }

                    String[][] matches = br.getRegex("<track id=\"(.*?)\" name=\"(.*?)\" lang_code=\"(.*?)\" lang_original=\"(.*?)\".*?/>").getMatches();

                    for (String[] track : matches) {
                        String link = "http://video.google.com/timedtext?type=track&name=" + URLEncoder.encode(track[1], "UTF-8") + "&lang=" + URLEncoder.encode(track[2], "UTF-8") + "&v=" + URLEncoder.encode(VIDEOID, "UTF-8");

                        DownloadLink dlink = this.createDownloadlink(link.replaceFirst("http", "httpJDYoutube"));
                        dlink.setProperty("ALLOW_DUPE", true);
                        dlink.setBrowserUrl(url);

                        String name = YT_FILENAME + " (" + track[3] + ").xml";
                        dlink.setFinalFileName(name);
                        dlink.setProperty("name", name);
                        dlink.setProperty("subtitle", true);

                        decryptedLinks.add(dlink);
                        filePackage.add(dlink);
                    }
                }

                // Grab thumbnails
                FilePackage filePackage = filepackages.get(NAME_THUMBNAILS);
                if ((cfg.getBooleanProperty("ALLOW_THUMBNAIL_HQ", false) || cfg.getBooleanProperty("ALLOW_THUMBNAIL_MQ", false) || cfg.getBooleanProperty("ALLOW_THUMBNAIL_DEFAULT", false) || cfg.getBooleanProperty("ALLOW_THUMBNAIL_MAX", false)) && filePackage == null) {
                    filePackage = FilePackage.getInstance();
                    filePackage.setProperty("ALLOW_MERGE", true);
                    if (multiple_videos)
                        filePackage.setName("Youtube (Thumbnails)");
                    else
                        filePackage.setName(YT_FILENAME + " (Thumbnails)");
                    filepackages.put(NAME_THUMBNAILS, filePackage);
                }

                if (cfg.getBooleanProperty("ALLOW_THUMBNAIL_MAX", false)) {
                    decryptedLinks.add(createThumbnailDownloadLink(YT_FILENAME + " (MAX).jpg", "http://img.youtube.com/vi/" + VIDEOID + "/maxresdefault.jpg", url, filePackage));
                }

                if (cfg.getBooleanProperty("ALLOW_THUMBNAIL_HQ", false)) {
                    decryptedLinks.add(createThumbnailDownloadLink(YT_FILENAME + " (HQ).jpg", "http://img.youtube.com/vi/" + VIDEOID + "/hqdefault.jpg", url, filePackage));
                }

                if (cfg.getBooleanProperty("ALLOW_THUMBNAIL_MQ", false)) {
                    decryptedLinks.add(createThumbnailDownloadLink(YT_FILENAME + " (MQ).jpg", "http://img.youtube.com/vi/" + VIDEOID + "/mqdefault.jpg", url, filePackage));
                }

                if (cfg.getBooleanProperty("ALLOW_THUMBNAIL_DEFAULT", false)) {
                    decryptedLinks.add(createThumbnailDownloadLink(YT_FILENAME + ".jpg", "http://img.youtube.com/vi/" + VIDEOID + "/default.jpg", url, filePackage));
                }
            } catch (final IOException e) {
                this.br.getHttpConnection().disconnect();
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                // return null;
            }
        }

        return decryptedLinks;
    }

    private DownloadLink createThumbnailDownloadLink(String name, String link, String browserurl, FilePackage filePackage) {
        DownloadLink dlink = this.createDownloadlink(link.replaceFirst("http", "httpJDYoutube"));
        dlink.setProperty("ALLOW_DUPE", true);
        filePackage.add(dlink);
        dlink.setBrowserUrl(browserurl);

        dlink.setFinalFileName(name);
        dlink.setProperty("name", name);
        dlink.setProperty("thumbnail", true);

        return dlink;
    }

    public HashMap<Integer, String[]> getLinks(final String video, final boolean prem, Browser br, int retrycount) throws Exception {
        if (retrycount > 2) {
            // do not retry more often than 2 time
            return null;
        }
        if (br == null) {
            br = this.br;
        }

        try {
            gsProxy(true);
        } catch (Throwable e) {
            /* does not exist in 09581 */
        }
        br.setFollowRedirects(true);
        /* this cookie makes html5 available and skip controversy check */
        br.setCookie("youtube.com", "PREF", "f2=40100000&hl=en-GB");
        br.getHeaders().put("User-Agent", "Wget/1.12");
        br.getPage(video);
        if (br.containsHTML("id=\"unavailable-submessage\" class=\"watch-unavailable-submessage\"")) { return null; }
        final String VIDEOID = new Regex(video, "watch\\?v=([\\w_\\-]+)").getMatch(0);
        boolean fileNameFound = false;
        String YT_FILENAME = VIDEOID;
        if (br.containsHTML("&title=")) {
            YT_FILENAME = Encoding.htmlDecode(br.getRegex("&title=([^&$]+)").getMatch(0).replaceAll("\\+", " ").trim());
            fileNameFound = true;
        }
        final String url = br.getURL();
        boolean ythack = false;
        if (url != null && !url.equals(video)) {
            /* age verify with activated premium? */
            if (url.toLowerCase(Locale.ENGLISH).indexOf("youtube.com/verify_age?next_url=") != -1) {
                verifyAge = true;
            }
            if (url.toLowerCase(Locale.ENGLISH).indexOf("youtube.com/verify_age?next_url=") != -1 && prem) {
                final String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
                final LinkedHashMap<String, String> p = Request.parseQuery(url);
                final String next = p.get("next_url");
                final Form form = new Form();
                form.setAction(url);
                form.setMethod(MethodType.POST);
                form.put("next_url", "%2F" + next.substring(1));
                form.put("action_confirm", "Confirm+Birth+Date");
                form.put("session_token", Encoding.urlEncode(session_token));
                br.submitForm(form);
                if (br.getCookie("http://www.youtube.com", "is_adult") == null) { return null; }
            } else if (url.toLowerCase(Locale.ENGLISH).indexOf("youtube.com/index?ytsession=") != -1 || url.toLowerCase(Locale.ENGLISH).indexOf("youtube.com/verify_age?next_url=") != -1 && !prem) {
                ythack = true;
                br.getPage("http://www.youtube.com/get_video_info?video_id=" + VIDEOID);
                if (br.containsHTML("&title=") && fileNameFound == false) {
                    YT_FILENAME = Encoding.htmlDecode(br.getRegex("&title=([^&$]+)").getMatch(0).replaceAll("\\+", " ").trim());
                    fileNameFound = true;
                }
            } else if (url.toLowerCase(Locale.ENGLISH).indexOf("google.com/accounts/servicelogin?") != -1) {
                // private videos
                return null;
            }
        }
        Form forms[] = br.getForms();
        if (forms != null) {
            for (Form form : forms) {
                if (form.getAction() != null && form.getAction().contains("verify_age")) {
                    logger.info("Verify Age");
                    br.submitForm(form);
                    break;
                }
            }
        }
        /* html5_fmt_map */
        if (br.getRegex(TbCm.YT_FILENAME_PATTERN).count() != 0 && fileNameFound == false) {
            YT_FILENAME = Encoding.htmlDecode(br.getRegex(TbCm.YT_FILENAME_PATTERN).getMatch(0).trim());
            fileNameFound = true;
        }
        final HashMap<Integer, String[]> links = new HashMap<Integer, String[]>();
        String html5_fmt_map = br.getRegex("\"html5_fmt_map\": \\[(.*?)\\]").getMatch(0);

        if (html5_fmt_map != null) {
            String[] html5_hits = new Regex(html5_fmt_map, "\\{(.*?)\\}").getColumn(0);
            if (html5_hits != null) {
                for (String hit : html5_hits) {
                    String hitUrl = new Regex(hit, "url\": \"(http:.*?)\"").getMatch(0);
                    String hitFmt = new Regex(hit, "itag\": (\\d+)").getMatch(0);
                    String hitQ = new Regex(hit, "quality\": \"(.*?)\"").getMatch(0);
                    if (hitUrl != null && hitFmt != null && hitQ != null) {
                        hitUrl = unescape(hitUrl.replaceAll("\\\\/", "/"));
                        links.put(Integer.parseInt(hitFmt), new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, true)), hitQ });
                    }
                }
            }
        } else {
            /* new format since ca. 1.8.2011 */
            html5_fmt_map = br.getRegex("\"url_encoded_fmt_stream_map\": \"(.*?)\"").getMatch(0);
            if (html5_fmt_map == null) {
                html5_fmt_map = br.getRegex("url_encoded_fmt_stream_map=(.*?)(&|$)").getMatch(0);
                if (html5_fmt_map != null) {
                    html5_fmt_map = html5_fmt_map.replaceAll("%2C", ",");
                    if (!html5_fmt_map.contains("url=")) {
                        html5_fmt_map = html5_fmt_map.replaceAll("%3D", "=");
                        html5_fmt_map = html5_fmt_map.replaceAll("%26", "&");
                    }
                }
            }
            if (html5_fmt_map != null && !html5_fmt_map.contains("signature") && !html5_fmt_map.contains("sig")) {
                Thread.sleep(5000);
                br.clearCookies(getHost());
                return getLinks(video, prem, br, retrycount + 1);
            }
            if (html5_fmt_map != null) {
                if (html5_fmt_map.contains(UNSUPPORTEDRTMP)) { return null; }
                String[] html5_hits = new Regex(html5_fmt_map, "(.*?)(,|$)").getColumn(0);
                if (html5_hits != null) {
                    for (String hit : html5_hits) {
                        hit = unescape(hit);
                        String hitUrl = new Regex(hit, "url=(http.*?)(\\&|$)").getMatch(0);
                        String sig = new Regex(hit, "url=http.*?(\\&|$)(sig|signature)=(.*?)(\\&|$)").getMatch(2);
                        if (sig == null) sig = new Regex(hit, "(sig|signature)=(.*?)(\\&|$)").getMatch(1);
                        String hitFmt = new Regex(hit, "itag=(\\d+)").getMatch(0);
                        String hitQ = new Regex(hit, "quality=(.*?)(\\&|$)").getMatch(0);
                        if (hitUrl != null && hitFmt != null && hitQ != null) {
                            hitUrl = unescape(hitUrl.replaceAll("\\\\/", "/"));
                            if (hitUrl.startsWith("http%253A")) {
                                hitUrl = Encoding.htmlDecode(hitUrl);
                            }
                            String[] inst = null;
                            if (hitUrl.contains("sig")) {
                                inst = new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, true)), hitQ };
                            } else {
                                inst = new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, true) + "&signature=" + sig), hitQ };
                            }
                            links.put(Integer.parseInt(hitFmt), inst);
                        }
                    }
                }
            }
        }

        /* normal links */
        final HashMap<String, String> fmt_list = new HashMap<String, String>();
        String fmt_list_str = "";
        if (ythack) {
            fmt_list_str = (br.getMatch("&fmt_list=(.+?)&") + ",").replaceAll("%2F", "/").replaceAll("%2C", ",");
        } else {
            fmt_list_str = (br.getMatch("\"fmt_list\":\\s+\"(.+?)\",") + ",").replaceAll("\\\\/", "/");
        }
        final String fmt_list_map[][] = new Regex(fmt_list_str, "(\\d+)/(\\d+x\\d+)/\\d+/\\d+/\\d+,").getMatches();
        for (final String[] fmt : fmt_list_map) {
            fmt_list.put(fmt[0], fmt[1]);
        }
        if (links.size() == 0 && ythack) {
            /* try to find fallback links */
            String urls[] = br.getRegex("url%3D(.*?)($|%2C)").getColumn(0);
            int index = 0;
            for (String vurl : urls) {
                String hitUrl = new Regex(vurl, "(.*?)%26").getMatch(0);
                String hitQ = new Regex(vurl, "%26quality%3D(.*?)%").getMatch(0);
                if (hitUrl != null && hitQ != null) {
                    hitUrl = unescape(hitUrl.replaceAll("\\\\/", "/"));
                    if (fmt_list_map.length >= index) {
                        links.put(Integer.parseInt(fmt_list_map[index][0]), new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, false)), hitQ });
                        index++;
                    }
                }
            }
        }
        for (Integer fmt : links.keySet()) {
            String fmt2 = fmt + "";
            if (fmt_list.containsKey(fmt2)) {
                String Videoq = links.get(fmt)[1];
                final Integer q = Integer.parseInt(fmt_list.get(fmt2).split("x")[1]);
                if (fmt == 17) {
                    Videoq = "144p";
                } else if (fmt == 40) {
                    Videoq = "240p Light";
                } else if (q > 1080) {
                    Videoq = "Original";
                } else if (q > 720) {
                    Videoq = "1080p";
                } else if (q > 576) {
                    Videoq = "720p";
                } else if (q > 360) {
                    Videoq = "480p";
                } else if (q > 240) {
                    Videoq = "360p";
                } else {
                    Videoq = "240p";
                }
                links.get(fmt)[1] = Videoq;
            }
        }
        if (YT_FILENAME != null && links != null && !links.isEmpty()) {
            links.put(-1, new String[] { YT_FILENAME });
        }
        return links;
    }

    private synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 100);
    }

    private boolean login(final Account account) throws Exception {
        this.setBrowserExclusive();
        final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
        try {
            if (plugin != null) {
                ((jd.plugins.hoster.Youtube) plugin).login(account, this.br, false, false);
            } else {
                return false;
            }
        } catch (final PluginException e) {
            account.setEnabled(false);
            account.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}