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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.AbstractDialog;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.TbCm.ConvertDialog.ConversionMode;
import jd.plugins.hoster.Youtube;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youtube.com" }, urls = { "http://[\\w\\.]*?youtube\\.com/(watch.*?v=[a-z-_A-Z0-9]+|view_play_list\\?p=[a-z-_A-Z0-9]+(.*?page=\\d+)?)" }, flags = { 0 })
public class TbCm extends PluginForDecrypt {

    private Pattern StreamingShareLink = Pattern.compile("\\< streamingshare=\"youtube\\.com\" name=\"(.*?)\" dlurl=\"(.*?)\" brurl=\"(.*?)\" convertto=\"(.*?)\" comment=\"(.*?)\" \\>", Pattern.CASE_INSENSITIVE);
    static public final Pattern YT_FILENAME_PATTERN = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);
    HashMap<ConversionMode, ArrayList<Info>> possibleconverts = null;

    public TbCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 100);
    }

    private boolean login(Account account) throws Exception {
        this.setBrowserExclusive();
        PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
        try {
            if (plugin != null) {
                ((Youtube) plugin).login(account, br);
            } else
                return false;
        } catch (PluginException e) {
            account.setEnabled(false);
            account.setValid(false);
            return false;
        }
        return true;
    }

    private void addVideosCurrentPage(ArrayList<DownloadLink> links) {
        String[] videos = br.getRegex("id=\"video-long-.*?href=\"(/watch\\?v=.*?)&").getColumn(0);
        for (String video : videos) {
            links.add(this.createDownloadlink("http://www.youtube.com" + video));
        }
    }

    static class Info {
        public String link;
        public long size;
        public int fmt;
        public String desc;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        possibleconverts = new HashMap<ConversionMode, ArrayList<Info>>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("watch#!v", "watch?v");
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("youtube.com");
        if (parameter.contains("watch#")) {
            parameter = parameter.replace("watch#", "watch?");
        }
        if (parameter.contains("view_play_list")) {
            br.getPage(parameter);
            addVideosCurrentPage(decryptedLinks);
            if (!parameter.contains("page=")) {
                String[] pages = br.getRegex("<a href=(\"|')(http://www.youtube.com/view_play_list\\?p=.*?page=\\d+)(\"|')").getColumn(1);
                for (int i = 0; i < pages.length - 1; i++) {
                    br.getPage(pages[i]);
                    addVideosCurrentPage(decryptedLinks);
                }
            }
        } else {
            boolean prem = false;
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
            if (accounts != null && accounts.size() != 0) prem = login(accounts.get(0));

            try {
                if (StreamingShareLink.matcher(parameter).matches()) {
                    // StreamingShareLink

                    String[] info = new Regex(parameter, StreamingShareLink).getMatches()[0];

                    for (String debug : info) {
                        logger.info(debug);
                    }
                    DownloadLink thislink = createDownloadlink(info[1]);
                    thislink.setBrowserUrl(info[2]);
                    thislink.setFinalFileName(info[0]);
                    thislink.setSourcePluginComment("Convert to " + (ConversionMode.valueOf(info[3])).getText());
                    thislink.setProperty("convertto", info[3]);

                    decryptedLinks.add(thislink);
                    return decryptedLinks;
                }

                HashMap<Integer, String[]> LinksFound = getLinks(parameter, prem, this.br);
                if (LinksFound == null || LinksFound.isEmpty()) {
                    if ((br.getURL().toLowerCase().indexOf("youtube.com/verify_age?next_url=") != -1) || ((br.getURL().toLowerCase().indexOf("youtube.com/get_video_info?") != -1) && !prem)) throw new DecrypterException(DecrypterException.ACCOUNT);
                    throw new DecrypterException("Video no longer available");
                }

                /* First get the filename */
                String YT_FILENAME = "";
                if (LinksFound.containsKey(-1)) {
                    YT_FILENAME = LinksFound.get(-1)[0];
                    LinksFound.remove(-1);
                }

                /* http://en.wikipedia.org/wiki/YouTube */
                final HashMap<Integer, Object[]> ytVideo = new HashMap<Integer, Object[]>() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -3028718522449785181L;

                    {
                        // **** FLV *****
                        put(0, new Object[] { ConversionMode.VIDEOFLV, "H.263", "MP3", "Mono" });
                        put(5, new Object[] { ConversionMode.VIDEOFLV, "H.263", "MP3", "Stereo" });
                        put(6, new Object[] { ConversionMode.VIDEOFLV, "H.263", "MP3", "Mono" });
                        put(34, new Object[] { ConversionMode.VIDEOFLV, "H.264", "AAC", "Stereo" });
                        put(35, new Object[] { ConversionMode.VIDEOFLV, "H.264", "AAC", "Stereo" });

                        // **** 3GP *****
                        put(13, new Object[] { ConversionMode.VIDEO3GP, "H.263", "AMR", "Mono" });
                        put(17, new Object[] { ConversionMode.VIDEO3GP, "H.264", "AAC", "Stereo" });

                        // **** MP4 *****
                        put(18, new Object[] { ConversionMode.VIDEOMP4, "H.264", "AAC", "Stereo" });
                        put(22, new Object[] { ConversionMode.VIDEOMP4, "H.264", "AAC", "Stereo" });
                        put(37, new Object[] { ConversionMode.VIDEOMP4, "H.264", "AAC", "Stereo" });
                        put(38, new Object[] { ConversionMode.VIDEOMP4, "H.264", "AAC", "Stereo" });

                        // **** WebM *****
                        put(43, new Object[] { ConversionMode.VIDEOWEBM, "VP8", "Vorbis", "Stereo" });
                        put(45, new Object[] { ConversionMode.VIDEOWEBM, "VP8", "Vorbis", "Stereo" });
                    }
                };

                /* check for wished formats first */
                String dlLink = "";
                String vQuality = "";
                ConversionMode cMode = null;
                boolean wishedFound = false;
                for (Integer format : LinksFound.keySet()) {
                    if (ytVideo.containsKey(format)) {
                        cMode = (ConversionMode) ytVideo.get(format)[0];
                        vQuality = "(" + LinksFound.get(format)[1] + "_" + ytVideo.get(format)[1] + "-" + ytVideo.get(format)[2] + ")";
                    } else {
                        cMode = ConversionMode.UNKNOWN;
                        vQuality = "(" + LinksFound.get(format)[1] + "_" + format + ")";
                    }
                    cMode = (ConversionMode) (ytVideo.containsKey(format) ? ytVideo.get(format)[0] : ConversionMode.UNKNOWN);
                    dlLink = LinksFound.get(format)[0];
                    if (ConvertDialog.getKeeped().contains(cMode)) {
                        try {
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(cMode, dlLink, br.getHttpConnection().getLongContentLength(), vQuality, format);
                                LinksFound.remove(format);
                                wishedFound = true;
                            }
                        } finally {
                            try {
                                br.getHttpConnection().disconnect();
                            } catch (Throwable e) {
                            }
                        }
                    }
                    if (ConvertDialog.getKeeped().contains(ConversionMode.AUDIOMP3) && (format == 0 || format == 5 || format == 6)) {
                        try {
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.AUDIOMP3, dlLink, br.getHttpConnection().getLongContentLength(), "", format);
                                LinksFound.remove(format);
                                wishedFound = true;
                            }
                        } finally {
                            try {
                                br.getHttpConnection().disconnect();
                            } catch (Throwable e) {
                            }
                        }
                    }
                }

                if (!wishedFound) {
                    for (Integer format : LinksFound.keySet()) {
                        if (ytVideo.containsKey(format)) {
                            cMode = (ConversionMode) ytVideo.get(format)[0];
                            vQuality = "(" + LinksFound.get(format)[1] + "_" + ytVideo.get(format)[1] + "-" + ytVideo.get(format)[2] + ")";
                        } else {
                            cMode = ConversionMode.UNKNOWN;
                            vQuality = "(" + LinksFound.get(format)[1] + "_" + format + ")";
                        }
                        dlLink = LinksFound.get(format)[0];
                        System.out.println(dlLink);
                        try {
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(cMode, dlLink, br.getHttpConnection().getLongContentLength(), vQuality, format);
                            }
                        } finally {
                            try {
                                br.getHttpConnection().disconnect();
                            } catch (Throwable e) {
                            }
                        }
                        if (format == 0 || format == 5 || format == 6) {
                            try {
                                if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                    addtopos(ConversionMode.AUDIOMP3, dlLink, br.getHttpConnection().getLongContentLength(), "", format);
                                }
                            } finally {
                                try {
                                    br.getHttpConnection().disconnect();
                                } catch (Throwable e) {
                                }
                            }
                        }
                    }
                }

                ConversionMode convertTo = showDisplayDialog(new ArrayList<ConversionMode>(possibleconverts.keySet()), YT_FILENAME, param);

                for (Info info : possibleconverts.get(convertTo)) {
                    DownloadLink thislink = createDownloadlink(info.link.replaceFirst("http", "httpJDYoutube"));
                    thislink.setBrowserUrl(parameter);
                    thislink.setFinalFileName(YT_FILENAME + info.desc + convertTo.getExtFirst());
                    thislink.setSourcePluginComment("Convert to " + convertTo.getText());
                    thislink.setProperty("size", Long.valueOf(info.size));
                    if (convertTo != ConversionMode.AUDIOMP3) {
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
            } catch (IOException e) {
                br.getHttpConnection().disconnect();
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                return null;
            }
        }
        return decryptedLinks;
    }

    private ConversionMode showDisplayDialog(ArrayList<ConversionMode> displaymodes, String name, CryptedLink link) throws DecrypterException {
        link.getProgressController().setStatusText(JDL.L("gui.linkgrabber.waitinguserio", "Waiting for user input"));
        synchronized (JDUtilities.USERIO_LOCK) {
            ConversionMode temp = ConvertDialog.displayDialog(displaymodes, name);
            link.getProgressController().setStatusText(null);
            if (temp == null) throw new DecrypterException(JDL.L("jd.plugins.Plugin.aborted", "Decryption aborted!"));
            return temp;
        }
    }

    public HashMap<Integer, String[]> getLinks(String video, boolean prem, Browser br) throws Exception {
        if (br == null) br = this.br;
        br.setFollowRedirects(true);
        /* the cookie and the user-agent makes html5 available */
        br.setCookie("youtube.com", "PREF", "f2=40000000");
        br.getHeaders().put("User-Agent", "Wget/1.12");
        br.getPage(video);
        String VIDEOID = new Regex(video, "watch\\?v=([\\w_-]+)").getMatch(0);
        String YT_FILENAME = null;
        String url = br.getURL();
        boolean ythack = false;
        if (url != null && !url.equals(video)) {
            /* age verify with activated premium? */
            if ((url.toLowerCase().indexOf("youtube.com/verify_age?next_url=") != -1) && prem) {
                String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
                LinkedHashMap<String, String> p = Request.parseQuery(url);
                String next = p.get("next_url");
                Form form = new Form();
                form.setAction(url);
                form.setMethod(MethodType.POST);
                form.put("next_url", "%2F" + next.substring(1));
                form.put("action_confirm", "Confirm+Birth+Date");
                form.put("session_token", Encoding.urlEncode(session_token));
                br.submitForm(form);
                if (br.getCookie("http://www.youtube.com", "is_adult") == null) return null;
            } else if ((url.toLowerCase().indexOf("youtube.com/index?ytsession=") != -1) || ((url.toLowerCase().indexOf("youtube.com/verify_age?next_url=") != -1) && !prem)) {
                ythack = true;
                br.getPage("http://www.youtube.com/get_video_info?video_id=" + VIDEOID);
                YT_FILENAME = br.containsHTML("&title=") ? Encoding.htmlDecode(br.getRegex("&title=([^&$]+)").getMatch(0).replaceAll("\\+", " ").trim()) : VIDEOID;
            } else if ((url.toLowerCase().indexOf("google.com/accounts/servicelogin?") != -1)) {
                // private videos
                return null;
            }
        }

        HashMap<Integer, String[]> links = new HashMap<Integer, String[]>();

        /* html5 links */
        if (!ythack) {
            final HashMap<String, String> VideoQ_map = new HashMap<String, String>() {
                /**
                 * 
                 */
                private static final long serialVersionUID = -2038768002287207875L;

                {
                    put("highres", "Original");
                    put("hd1080", "1080p");
                    put("hd720", "720p");
                    put("large", "480p");
                    put("medium", "360p");
                    put("small", "240p");
                    put("light", "240p Light");
                }
            };
            YT_FILENAME = (br.getRegex(YT_FILENAME_PATTERN).count() != 0) ? Encoding.htmlDecode(br.getRegex(YT_FILENAME_PATTERN).getMatch(0).trim()) : VIDEOID;
            String linksDataHTML5[][] = br.getRegex("videoPlayer\\.setAvailableFormat\\(\\s*\"(.+?)\"\\s*,\\s*\".+?\"\\s*,\\s*\"(.+?)\"\\s*,\\s*\"(\\d+)\"\\s*\\)\\s*;").getMatches();
            String VideoQ = "";
            for (String linkData[] : linksDataHTML5) {
                VideoQ = (VideoQ_map.containsKey(linkData[1])) ? VideoQ_map.get(linkData[1]) : linkData[1];
                links.put(Integer.parseInt(linkData[2]), new String[] { Encoding.htmlDecode(Encoding.urlDecode(linkData[0], true)), VideoQ });
            }
        }

        /* normal links */
        HashMap<String, String> fmt_list = new HashMap<String, String>();
        String fmt_list_str = "";
        if (ythack) {
            fmt_list_str = (br.getMatch("&fmt_list=(.+?)&") + ",").replaceAll("%2F", "/").replaceAll("%2C", ",");
        } else {
            fmt_list_str = (br.getMatch("\"fmt_list\":\\s+\"(.+?)\",") + ",").replaceAll("\\\\/", "/");
        }
        String fmt_list_map[][] = new Regex(fmt_list_str, "(\\d+)/(\\d+x\\d+)/\\d+/\\d+/\\d+,").getMatches();
        for (String[] fmt : fmt_list_map) {
            fmt_list.put(fmt[0], fmt[1]);
        }
        String fmt_stream_map_str = "";
        if (ythack) {
            fmt_stream_map_str = (br.getMatch("&fmt_stream_map=(.+?)&") + "|").replaceAll("%7C", "|");
        } else {
            fmt_stream_map_str = (br.getMatch("\"fmt_stream_map\":\\s+\"(.+?)\",") + "|").replaceAll("\\\\/", "/");
        }
        String fmt_stream_map[][] = new Regex(fmt_stream_map_str, "(\\d+)\\|(http.*?)\\|").getMatches();
        String Videoq = "";
        for (String fmt_str[] : fmt_stream_map) {
            if (fmt_list.containsKey(fmt_str[0])) {
                Integer q = Integer.parseInt(fmt_list.get(fmt_str[0]).split("x")[1]);
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
        if (YT_FILENAME != null) links.put(-1, new String[] { YT_FILENAME });
        return links;
    }

    private void addtopos(ConversionMode mode, String link, long size, String desc, int fmt) {
        ArrayList<Info> info = possibleconverts.get(mode);
        if (info == null) {
            info = new ArrayList<Info>();
            possibleconverts.put(mode, info);
        }
        Info tmp = new Info();
        tmp.link = link;
        tmp.size = size;
        tmp.desc = desc;
        tmp.fmt = fmt;
        info.add(tmp);
    }

    static class ConvertDialog extends AbstractDialog {

        /**
         * Zeigt Auswahldialog an. wenn keepformat aktiviert ist wird
         * gespeichtertes Input zurückgegeben, es sei denn, andere Formate
         * stehen zur Auswahl
         */
        public static ConversionMode displayDialog(final ArrayList<ConversionMode> modes, final String name) {
            if (modes.size() == 0) return null;
            if (modes.size() == 1) return modes.get(0);

            if (ConvertDialog.keepformat) {
                boolean newFormatChoosable = false;
                if (!ConvertDialog.forcekeep) {
                    for (int i = 0; i < modes.size(); i++) {
                        if (!keeped_availablemodes.contains(modes.get(i))) {
                            newFormatChoosable = true;
                            break;
                        }
                    }
                }
                if (!newFormatChoosable) {
                    for (int i = 0; i < keeped.size(); i++) {
                        if (modes.contains(keeped.get(i))) return keeped.get(i);
                    }
                }
            }

            GuiRunnable<ConversionMode> run = new GuiRunnable<ConversionMode>() {
                @Override
                public ConversionMode runSave() {
                    return new ConvertDialog(modes, name).getReturnMode();
                }
            };
            return run.getReturnValue();
        }

        private static final long serialVersionUID = -9146764850581039090L;

        public static enum ConversionMode {
            AUDIOMP3("Audio (MP3)", new String[] { ".mp3" }), VIDEOFLV("Video (FLV)", new String[] { ".flv" }), VIDEOMP4("Video (MP4)", new String[] { ".mp4" }), VIDEOWEBM("Video (Webm)", new String[] { ".webm" }), VIDEO3GP("Video (3GP)", new String[] { ".3gp" }), UNKNOWN("Unknown (unk)", new String[] { ".unk" });

            private String text;
            private String[] ext;

            ConversionMode(String text, String[] ext) {
                this.text = text;
                this.ext = ext;
            }

            @Override
            public String toString() {
                return text;
            }

            public String getText() {
                return text;
            }

            public String getExtFirst() {
                return ext[0];
            }

        }

        private static boolean forcekeep = false;
        private static ArrayList<ConversionMode> keeped = new ArrayList<ConversionMode>();
        private static ArrayList<ConversionMode> keeped_availablemodes = new ArrayList<ConversionMode>();
        private static boolean keepformat = false;

        private static void setKeepformat(boolean newkeepformat) {
            if (newkeepformat == false) {
                ConvertDialog.keepformat = false;
                ConvertDialog.setForceKeep(false);
                ConvertDialog.keeped_availablemodes = new ArrayList<ConversionMode>();
                ConvertDialog.keeped = new ArrayList<ConversionMode>();
            } else {
                ConvertDialog.keepformat = true;
            }
        }

        private static void setForceKeep(boolean newforcekeep) {
            ConvertDialog.forcekeep = newforcekeep;
        }

        private static boolean hasKeeped() {
            if (keepformat) return !keeped.isEmpty();
            return false;
        }

        private static void addKeeped(ConversionMode FormatToAdd, ArrayList<ConversionMode> FormatsInList, boolean TopPriority) {
            ConvertDialog.keepformat = true;
            if (TopPriority) {
                ConvertDialog.keeped.add(0, FormatToAdd);
            } else {
                ConvertDialog.keeped.add(FormatToAdd);
            }

            for (int i = 0; i < FormatsInList.size(); i++) {
                if (!ConvertDialog.keeped_availablemodes.contains(FormatsInList.get(i))) {
                    ConvertDialog.keeped_availablemodes.add(FormatsInList.get(i));
                }
            }
        }

        public static ArrayList<ConversionMode> getKeeped() {
            return ConvertDialog.keeped;
        }

        private ArrayList<ConversionMode> modes;

        private JCheckBox chkKeepFormat;
        private JCheckBox chkForceKeep;
        private JCheckBox chkTopPriority;
        private JComboBox cmbModes;

        private ConvertDialog(ArrayList<ConversionMode> modes, String name) {
            super(UserIO.NO_COUNTDOWN, JDL.L("convert.dialog.chooseformat", "Select a format:") + " [" + name + "]", UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), null, null);

            this.modes = modes;

            init();
        }

        public JComponent contentInit() {
            chkKeepFormat = new JCheckBox(ConvertDialog.hasKeeped() ? JDL.L("convert.dialog.staykeepingformat", "Try to get preferred format") : JDL.L("convert.dialog.keepformat", "Use this format for this session"));
            chkKeepFormat.setSelected(ConvertDialog.hasKeeped());

            chkForceKeep = new JCheckBox(JDL.L("convert.dialog.forcekeep", "Keep force"));
            chkForceKeep.setSelected(ConvertDialog.forcekeep);

            chkTopPriority = new JCheckBox(JDL.L("convert.dialog.toppriority", "This format has highest priority"));

            cmbModes = new JComboBox(modes.toArray(new ConversionMode[modes.size()]));
            cmbModes.setSelectedIndex(0);

            JPanel panel = new JPanel(new MigLayout("ins 0"));
            if (ConvertDialog.hasKeeped()) panel.add(chkTopPriority);
            panel.add(chkKeepFormat);
            panel.add(chkForceKeep, "wrap");
            panel.add(cmbModes, "spanx");
            return panel;
        }

        private ConversionMode getReturnMode() {
            if (!UserIO.isOK(getReturnValue())) return null;

            ConversionMode selectedValue = (ConversionMode) cmbModes.getSelectedItem();
            if ((chkKeepFormat.isSelected()) || (chkForceKeep.isSelected())) {
                ConvertDialog.setKeepformat(true);
                ConvertDialog.addKeeped(selectedValue, modes, chkTopPriority.isSelected());
                ConvertDialog.setForceKeep(chkForceKeep.isSelected());
            } else {
                ConvertDialog.setKeepformat(false);
            }
            return selectedValue;
        }

    }

}
