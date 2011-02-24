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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.Youtube;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import de.savemytube.flv.FLV;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youtube.com" }, urls = { "http://[\\w\\.]*?youtube\\.com/(watch.*?v=[a-z-_A-Z0-9]+|view_play_list\\?p=[a-z-_A-Z0-9]+(.*?page=\\d+)?)" }, flags = { 0 })
public class TbCm extends PluginForDecrypt {

    public static enum DestinationFormat {
        AUDIOMP3("Audio (MP3)", new String[] { ".mp3" }), VIDEOFLV("Video (FLV)", new String[] { ".flv" }), VIDEOMP4("Video (MP4)", new String[] { ".mp4" }), VIDEOWEBM("Video (Webm)", new String[] { ".webm" }), VIDEO3GP("Video (3GP)", new String[] { ".3gp" }), UNKNOWN("Unknown (unk)", new String[] { ".unk" }),

        VIDEOIPHONE("Video (IPhone)", new String[] { ".mp4" });

        private String text;
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
        public long size;
        public int fmt;
        public String desc;
    }

    private final Pattern StreamingShareLink = Pattern.compile("\\< streamingshare=\"youtube\\.com\" name=\"(.*?)\" dlurl=\"(.*?)\" brurl=\"(.*?)\" convertto=\"(.*?)\" comment=\"(.*?)\" \\>", Pattern.CASE_INSENSITIVE);

    static public final Pattern YT_FILENAME_PATTERN = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);

    HashMap<DestinationFormat, ArrayList<Info>> possibleconverts = null;

    private static final Logger LOG = JDLogger.getLogger();

    private static final String TEMP_EXT = ".tmp$";

    public static boolean ConvertFile(final DownloadLink downloadlink, final DestinationFormat InType, final DestinationFormat OutType) {
        TbCm.LOG.info("Convert " + downloadlink.getName() + " - " + InType.getText() + " - " + OutType.getText());
        if (InType.equals(OutType)) {
            TbCm.LOG.info("No Conversion needed, renaming...");
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
                TbCm.LOG.info("Convert FLV to mp3...");
                new FLV(downloadlink.getFileOutput(), true, true);

                // FLV löschen
                if (!new File(downloadlink.getFileOutput()).delete()) {
                    new File(downloadlink.getFileOutput()).deleteOnExit();
                }
                // AVI löschen
                if (!new File(downloadlink.getFileOutput().replaceAll(TbCm.TEMP_EXT, ".avi")).delete()) {
                    new File(downloadlink.getFileOutput().replaceAll(TbCm.TEMP_EXT, ".avi")).deleteOnExit();
                }

                return true;
            default:
                TbCm.LOG.warning("Don't know how to convert " + InType.getText() + " to " + OutType.getText());
                downloadlink.getLinkStatus().setErrorMessage(JDL.L("convert.progress.unknownintype", "Unknown format"));
                return false;
            }
        default:
            TbCm.LOG.warning("Don't know how to convert " + InType.getText() + " to " + OutType.getText());
            downloadlink.getLinkStatus().setErrorMessage(JDL.L("convert.progress.unknownintype", "Unknown format"));
            return false;
        }
    }

    public TbCm(final PluginWrapper wrapper) {
        super(wrapper);
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

    private void addVideosCurrentPage(final ArrayList<DownloadLink> links) {
        final String[] videos = this.br.getRegex("id=\"video-long-.*?href=\"(/watch\\?v=.*?)&").getColumn(0);
        for (final String video : videos) {
            links.add(this.createDownloadlink("http://www.youtube.com" + video));
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.possibleconverts = new HashMap<DestinationFormat, ArrayList<Info>>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("watch#!v", "watch?v");
        this.br.setFollowRedirects(true);
        this.br.setCookiesExclusive(true);
        this.br.clearCookies("youtube.com");
        if (parameter.contains("watch#")) {
            parameter = parameter.replace("watch#", "watch?");
        }
        if (parameter.contains("view_play_list")) {
            this.br.getPage(parameter);
            this.addVideosCurrentPage(decryptedLinks);
            if (!parameter.contains("page=")) {
                final String[] pages = this.br.getRegex("<a href=(\"|')(http://www.youtube.com/view_play_list\\?p=.*?page=\\d+)(\"|')").getColumn(1);
                for (int i = 0; i < pages.length - 1; i++) {
                    this.br.getPage(pages[i]);
                    this.addVideosCurrentPage(decryptedLinks);
                }
            }
        } else {
            boolean prem = false;
            final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
            if (accounts != null && accounts.size() != 0) {
                prem = this.login(accounts.get(0));
            }

            try {
                if (this.StreamingShareLink.matcher(parameter).matches()) {
                    // StreamingShareLink

                    final String[] info = new Regex(parameter, this.StreamingShareLink).getMatches()[0];

                    for (final String debug : info) {
                        logger.info(debug);
                    }
                    final DownloadLink thislink = this.createDownloadlink(info[1]);
                    thislink.setProperty("ALLOW_DUPE", true);
                    thislink.setBrowserUrl(info[2]);
                    thislink.setFinalFileName(info[0]);
                    thislink.setSourcePluginComment("Convert to " + DestinationFormat.valueOf(info[3]).getText());
                    thislink.setProperty("convertto", info[3]);

                    decryptedLinks.add(thislink);
                    return decryptedLinks;
                }

                final HashMap<Integer, String[]> LinksFound = this.getLinks(parameter, prem, this.br);
                if (LinksFound == null || LinksFound.isEmpty()) {
                    if (this.br.getURL().toLowerCase().indexOf("youtube.com/verify_age?next_url=") != -1 || this.br.getURL().toLowerCase().indexOf("youtube.com/get_video_info?") != -1 && !prem) { throw new DecrypterException(DecrypterException.ACCOUNT); }
                    throw new DecrypterException("Video no longer available");
                }

                /* First get the filename */
                String YT_FILENAME = "";
                if (LinksFound.containsKey(-1)) {
                    YT_FILENAME = LinksFound.get(-1)[0];
                    LinksFound.remove(-1);
                }
                /* prefer videoID als filename? */
                SubConfiguration cfg = SubConfiguration.getConfig("youtube.com");
                if (cfg.getBooleanProperty("ISASFILENAME", false)) {
                    String id = new Regex(parameter, "v=([a-z-_A-Z0-9]+)").getMatch(0);
                    if (id != null) YT_FILENAME = id.toUpperCase(Locale.ENGLISH);
                }
                final boolean mp3 = cfg.getBooleanProperty("ALLOW_MP3", true);
                final boolean mp4 = cfg.getBooleanProperty("ALLOW_MP4", true);
                final boolean webm = cfg.getBooleanProperty("ALLOW_WEBM", true);
                final boolean flv = cfg.getBooleanProperty("ALLOW_FLV", true);
                final boolean threegp = cfg.getBooleanProperty("ALLOW_3GP", true);
                /* http://en.wikipedia.org/wiki/YouTube */
                final HashMap<Integer, Object[]> ytVideo = new HashMap<Integer, Object[]>() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -3028718522449785181L;

                    {
                        // **** FLV *****
                        if (mp3) {
                            this.put(0, new Object[] { DestinationFormat.VIDEOFLV, "H.263", "MP3", "Mono" });
                            this.put(5, new Object[] { DestinationFormat.VIDEOFLV, "H.263", "MP3", "Stereo" });
                            this.put(6, new Object[] { DestinationFormat.VIDEOFLV, "H.263", "MP3", "Mono" });
                        }
                        if (flv) {
                            this.put(34, new Object[] { DestinationFormat.VIDEOFLV, "H.264", "AAC", "Stereo" });
                            this.put(35, new Object[] { DestinationFormat.VIDEOFLV, "H.264", "AAC", "Stereo" });
                        }

                        // **** 3GP *****
                        if (threegp) {
                            this.put(13, new Object[] { DestinationFormat.VIDEO3GP, "H.263", "AMR", "Mono" });
                            this.put(17, new Object[] { DestinationFormat.VIDEO3GP, "H.264", "AAC", "Stereo" });
                        }
                        // **** MP4 *****
                        if (mp4) {
                            this.put(18, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo" });
                            this.put(22, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo" });
                            this.put(37, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo" });
                            this.put(38, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo" });
                        }
                        // **** WebM *****
                        if (webm) {
                            this.put(43, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo" });
                            this.put(45, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo" });
                        }
                    }
                };

                /* check for wished formats first */
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
                         * we do not want to download unknown formats at the
                         * moment
                         */
                        continue;
                    }
                    dlLink = LinksFound.get(format)[0];
                    System.out.println(dlLink);
                    try {
                        if (this.br.openGetConnection(dlLink).getResponseCode() == 200) {
                            this.addtopos(cMode, dlLink, this.br.getHttpConnection().getLongContentLength(), vQuality, format);
                        }
                    } finally {
                        try {
                            this.br.getHttpConnection().disconnect();
                        } catch (final Throwable e) {
                        }
                    }
                    if (format == 0 || format == 5 || format == 6) {
                        try {
                            if (this.br.openGetConnection(dlLink).getResponseCode() == 200) {
                                this.addtopos(DestinationFormat.AUDIOMP3, dlLink, this.br.getHttpConnection().getLongContentLength(), "", format);
                            }
                        } finally {
                            try {
                                this.br.getHttpConnection().disconnect();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                }

                for (final Iterator<Entry<jd.plugins.decrypter.TbCm.DestinationFormat, ArrayList<Info>>> it = this.possibleconverts.entrySet().iterator(); it.hasNext();) {
                    final Entry<jd.plugins.decrypter.TbCm.DestinationFormat, ArrayList<Info>> next = it.next();
                    final jd.plugins.decrypter.TbCm.DestinationFormat convertTo = next.getKey();
                    // create a package, for each quality.
                    final FilePackage filePackage = FilePackage.getInstance();
                    filePackage.setName("YouTube " + convertTo.getText() + "(" + convertTo.getExtFirst() + ")");
                    for (final Info info : next.getValue()) {
                        final DownloadLink thislink = this.createDownloadlink(info.link.replaceFirst("http", "httpJDYoutube"));
                        thislink.setProperty("ALLOW_DUPE", true);
                        filePackage.add(thislink);
                        thislink.setBrowserUrl(parameter);
                        thislink.setFinalFileName(YT_FILENAME + info.desc + convertTo.getExtFirst());
                        thislink.setSourcePluginComment("Convert to " + convertTo.getText());
                        thislink.setProperty("size", Long.valueOf(info.size));
                        if (convertTo != DestinationFormat.AUDIOMP3) {
                            thislink.setProperty("name", YT_FILENAME + info.desc + convertTo.getExtFirst());
                        } else {
                            /*
                             * because demuxer will fail when mp3 file already
                             * exists
                             */
                            thislink.setProperty("name", YT_FILENAME + info.desc + ".tmp");
                        }
                        thislink.setProperty("convertto", convertTo.name());
                        thislink.setProperty("videolink", parameter);
                        thislink.setProperty("valid", true);
                        thislink.setProperty("fmtNew", info.fmt);
                        decryptedLinks.add(thislink);
                    }
                }
            } catch (final IOException e) {
                this.br.getHttpConnection().disconnect();
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                return null;
            }
        }
        return decryptedLinks;
    }

    public HashMap<Integer, String[]> getLinks(final String video, final boolean prem, Browser br) throws Exception {
        if (br == null) {
            br = this.br;
        }
        br.setFollowRedirects(true);
        /* the cookie and the user-agent makes html5 available */
        br.setCookie("youtube.com", "PREF", "f2=40000000");
        br.getHeaders().put("User-Agent", "Wget/1.12");
        br.getPage(video);
        final String VIDEOID = new Regex(video, "watch\\?v=([\\w_-]+)").getMatch(0);
        String YT_FILENAME = br.containsHTML("&title=") ? Encoding.htmlDecode(br.getRegex("&title=([^&$]+)").getMatch(0).replaceAll("\\+", " ").trim()) : VIDEOID;
        final String url = br.getURL();
        boolean ythack = false;
        if (url != null && !url.equals(video)) {
            /* age verify with activated premium? */
            if (url.toLowerCase().indexOf("youtube.com/verify_age?next_url=") != -1 && prem) {
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
            } else if (url.toLowerCase().indexOf("youtube.com/index?ytsession=") != -1 || url.toLowerCase().indexOf("youtube.com/verify_age?next_url=") != -1 && !prem) {
                ythack = true;
                br.getPage("http://www.youtube.com/get_video_info?video_id=" + VIDEOID);
                YT_FILENAME = br.containsHTML("&title=") ? Encoding.htmlDecode(br.getRegex("&title=([^&$]+)").getMatch(0).replaceAll("\\+", " ").trim()) : VIDEOID;
            } else if (url.toLowerCase().indexOf("google.com/accounts/servicelogin?") != -1) {
                // private videos
                return null;
            }
        }
        /* html5_fmt_map */
        YT_FILENAME = br.getRegex(TbCm.YT_FILENAME_PATTERN).count() != 0 ? Encoding.htmlDecode(br.getRegex(TbCm.YT_FILENAME_PATTERN).getMatch(0).trim()) : VIDEOID;
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
                        hitUrl = hitUrl.replaceAll("\\\\/", "/");
                        links.put(Integer.parseInt(hitFmt), new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, true)), hitQ });
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
        String fmt_stream_map_str = "";
        if (ythack) {
            fmt_stream_map_str = (br.getMatch("&fmt_stream_map=(.+?)&") + "|").replaceAll("%7C", "|");
        } else {
            fmt_stream_map_str = (br.getMatch("\"fmt_stream_map\":\\s+\"(.+?)\",") + "|").replaceAll("\\\\/", "/");
        }
        final String fmt_stream_map[][] = new Regex(fmt_stream_map_str, "(\\d+)\\|(http.*?)\\|").getMatches();
        String Videoq = "";
        for (final String fmt_str[] : fmt_stream_map) {
            if (fmt_list.containsKey(fmt_str[0])) {
                final Integer q = Integer.parseInt(fmt_list.get(fmt_str[0]).split("x")[1]);
                if (fmt_str[0].equals("40")) {
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
            } else {
                Videoq = "unk";
            }
            links.put(Integer.parseInt(fmt_str[0]), new String[] { Encoding.htmlDecode(Encoding.urlDecode(fmt_str[1], true)), Videoq });
        }
        if (YT_FILENAME != null) {
            links.put(-1, new String[] { YT_FILENAME });
        }
        return links;
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
                ((Youtube) plugin).login(account, this.br);
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

}
