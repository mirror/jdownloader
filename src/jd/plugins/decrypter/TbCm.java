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

    static private String host = "youtube.com";
    static private final Pattern patternswfArgs = Pattern.compile("(.*?swf_Args.*)", Pattern.CASE_INSENSITIVE);
    private static final String PLAYER = "get_video";
    private static final String T = "\"t\"";
    private static final String VIDEO_ID = "video_id";
    private Pattern StreamingShareLink = Pattern.compile("\\< streamingshare=\"youtube\\.com\" name=\"(.*?)\" dlurl=\"(.*?)\" brurl=\"(.*?)\" convertto=\"(.*?)\" comment=\"(.*?)\" \\>", Pattern.CASE_INSENSITIVE);

    static public final Pattern YT_FILENAME = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);
    HashMap<ConversionMode, ArrayList<Info>> possibleconverts = null;

    public TbCm(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 100);
    }

    private String clean(String s) {
        s = s.replaceAll("\"", "");
        s = s.replaceAll("YouTube -", "");
        s = s.replaceAll("YouTube", "");
        s = s.trim();
        return s;
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
                boolean oldLayout = false;
                HashMap<Integer, String> html5LinksFound = getLinksHTML5(parameter, prem, this.br);
                HashMap<Integer, String> linksFound = getLinksNew(parameter, prem, this.br);

                if (linksFound != null && linksFound.size() == 0) {
                    linksFound = getLinks(parameter, prem, this.br);
                    if (linksFound != null && linksFound.size() > 0) oldLayout = true;
                }
                if (linksFound == null || linksFound.size() == 0) {
                    linksFound = html5LinksFound;
                } else {
                    if (html5LinksFound != null) {
                        for (Integer format : html5LinksFound.keySet()) {
                            if (!linksFound.containsKey(format)) {
                                linksFound.put(format, html5LinksFound.get(format));
                            }
                        }
                    }
                }
                if ((linksFound == null || linksFound.size() == 0) && br.containsHTML("verify_age")) throw new DecrypterException(DecrypterException.ACCOUNT);
                if (linksFound == null || linksFound.size() == 0) throw new DecrypterException("Video no longer available");
                String name = Encoding.htmlDecode(br.getRegex(YT_FILENAME).getMatch(0).trim());
                /* check for wished formats first */
                HashMap<Integer, String> links = new HashMap<Integer, String>();
                if (ConvertDialog.getKeeped().contains(ConversionMode.VIDEO3GP) && linksFound.keySet().contains(13)) {
                    links.put(13, linksFound.get(13));
                } else if (ConvertDialog.getKeeped().contains(ConversionMode.VIDEOMP4) && (linksFound.keySet().contains(18) || linksFound.keySet().contains(22) || linksFound.keySet().contains(34) || linksFound.keySet().contains(35) || linksFound.keySet().contains(37) || linksFound.keySet().contains(43) || linksFound.keySet().contains(45))) {
                    Integer mp4[] = new Integer[] { 18, 22, 34, 35, 37, 43, 45 };
                    for (Integer f : mp4) {
                        if (linksFound.containsKey(f)) {
                            links.put(f, linksFound.get(f));
                        }
                    }
                } else {
                    links = linksFound;
                }
                br.setCookie("youtube.com", "PREF", "f2=40000000");
                /* Konvertierungsmöglichkeiten adden */
                for (Integer format : links.keySet()) {
                    String link = links.get(format);
                    String dlLink;
                    if (format == 0) {
                        dlLink = link;
                    } else {
                        dlLink = link + (oldLayout == true ? "&fmt=" + format : "");
                    }
                    try {
                        switch (format) {
                        case 18:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.VIDEOMP4, dlLink, br.getHttpConnection().getLongContentLength(), "(18)", format);
                            }
                            break;
                        case 22:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.VIDEOMP4, dlLink, br.getHttpConnection().getLongContentLength(), "(22,720p)", format);
                            }
                            break;
                        case 34:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.VIDEOFLV, dlLink, br.getHttpConnection().getLongContentLength(), "(HQ)", format);
                            }
                            break;
                        case 35:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.VIDEOFLV, dlLink, br.getHttpConnection().getLongContentLength(), "(35,720p/HQ)", format);
                            }
                            break;
                        case 37:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.VIDEOMP4, dlLink, br.getHttpConnection().getLongContentLength(), "(37,1080p)", format);
                            }
                            if (ConvertDialog.getKeeped().contains(ConversionMode.VIDEOMP4)) break;
                            break;
                        case 13:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.VIDEO3GP, dlLink, br.getHttpConnection().getLongContentLength(), "(13)", format);
                            }
                            break;
                        case 43:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.VIDEOWEBM, dlLink, br.getHttpConnection().getLongContentLength(), "(360p)", format);
                            }
                            break;
                        case 45:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConvertDialog.ConversionMode.VIDEOWEBM, dlLink, br.getHttpConnection().getLongContentLength(), "(720p)", format);
                            }
                            break;
                        default:
                            if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                                addtopos(ConversionMode.VIDEOFLV, dlLink, br.getHttpConnection().getLongContentLength(), "", format);
                                addtopos(ConversionMode.AUDIOMP3, dlLink, br.getHttpConnection().getLongContentLength(), "", format);
                                addtopos(ConversionMode.AUDIOMP3_AND_VIDEOFLV, dlLink, br.getHttpConnection().getLongContentLength(), "", format);
                            }
                        }
                    } finally {
                        try {
                            br.getHttpConnection().disconnect();
                        } catch (Throwable e) {
                        }
                    }
                }

                ConversionMode convertTo = showDisplayDialog(new ArrayList<ConversionMode>(possibleconverts.keySet()), name, param);

                for (Info info : possibleconverts.get(convertTo)) {
                    DownloadLink thislink = createDownloadlink(info.link.replaceFirst("http", "httpJDYoutube"));
                    thislink.setBrowserUrl(parameter);
                    thislink.setFinalFileName(name + info.desc + convertTo.getExtFirst());
                    thislink.setSourcePluginComment("Convert to " + convertTo.getText());
                    thislink.setProperty("size", Long.valueOf(info.size));
                    thislink.setProperty("name", name + info.desc + convertTo.getExtFirst());
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

    /* html5 layout */
    public HashMap<Integer, String> getLinksHTML5(String video, boolean prem, Browser br) throws Exception {
        if (br == null) br = this.br;
        br.setFollowRedirects(true);
        /* this cookie makes webm available */
        br.setCookie("youtube.com", "PREF", "f2=40000000");
        br.getHeaders().put("User-Agent", "Wget/1.12");
        br.getPage(video);
        /* age verify with activated premium? */
        if (br.containsHTML("verify_age") && prem) {
            String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
            String url = br.getURL();
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
        } else if (br.containsHTML("verify_age")) {
            /* no account used, cannot decrypt link */
            return null;
        }
        HashMap<Integer, String> links = new HashMap<Integer, String>();

        String linksData[][] = br.getRegex("videoPlayer\\.setAvailableFormat\\(\\s*\"(.+?)\"\\s*,\\s*\".+?\"\\s*,\\s*\".+?\"\\s*,\\s*\"(\\d+)\"\\s*\\)\\s*;").getMatches();
        for (String linkData[] : linksData) {
            if (!links.containsKey(linkData[0])) links.put(Integer.parseInt(linkData[1]), Encoding.htmlDecode(Encoding.urlDecode(linkData[0], true)));
        }
        return links;
    }

    /* current youtube layout */
    public HashMap<Integer, String> getLinksNew(String video, boolean prem, Browser br) throws Exception {
        if (br == null) br = this.br;
        /* reset this cookie to see old video formats */
        br.setCookie("youtube.com", "PREF", null);
        br.setFollowRedirects(true);
        br.getPage(video);
        /* age verify with activated premium? */
        if (br.containsHTML("verify_age") && prem) {
            String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
            String url = br.getURL();
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
        } else if (br.containsHTML("verify_age")) {
            /* no account used, cannot decrypt link */
            return null;
        }
        HashMap<Integer, String> links = new HashMap<Integer, String>();

        /* new site layout */
        String data = br.getRegex("swfHTML.*?\" : \"(.*?)\";").getMatch(0);
        // data = Encoding.urlDecode(data, true);
        String linksData[][] = new Regex(data, "(\\d+)%7C(http.*?)(&|%2C|%7C%7C)").getMatches();
        for (String linkData[] : linksData) {
            if (!links.containsKey(linkData[0])) links.put(Integer.parseInt(linkData[0]), Encoding.htmlDecode(Encoding.urlDecode(linkData[1], true)));
        }
        return links;
    }

    /* very old youtube layout */
    public HashMap<Integer, String> getLinks(String video, boolean prem, Browser br) throws Exception {
        if (br == null) br = this.br;
        br.setFollowRedirects(true);
        /* reset this cookie to see old video formats */
        br.setCookie("youtube.com", "PREF", null);
        br.getPage(video);
        /* age verify with activated premium? */
        if (br.containsHTML("verify_age") && prem) {
            String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
            String url = br.getURL();
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
        } else if (br.containsHTML("verify_age")) {
            /* no account used, cannot decrypt link */
            return null;
        }
        String video_id = "";
        String t = "";
        String match = br.getRegex(patternswfArgs).getMatch(0);
        if (match == null) return null;

        /* DownloadUrl holen */
        String[] lineSub = match.split(",|:");
        for (int i = 0; i < lineSub.length; i++) {
            String s = lineSub[i];
            if (s.indexOf(VIDEO_ID) > -1) {
                video_id = clean(lineSub[i + 1]);
            }
            if (s.indexOf(T) > -1) {
                t = clean(lineSub[i + 1]);
            }
        }
        HashMap<Integer, String> links = new HashMap<Integer, String>();
        String url = "http://" + host + "/" + PLAYER + "?" + VIDEO_ID + "=" + video_id + "&" + "t=" + t;
        links.put(0, url);
        links.put(22, url);
        links.put(34, url);
        links.put(35, url);
        links.put(37, url);
        links.put(13, url);
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
            AUDIOMP3("Audio (MP3)", new String[] { ".mp3" }), VIDEOFLV("Video (FLV)", new String[] { ".flv" }), AUDIOMP3_AND_VIDEOFLV("Audio & Video (MP3 & FLV)", new String[] { ".mp3", ".flv" }), VIDEOMP4("Video (MP4)", new String[] { ".mp4" }), VIDEOWEBM("Video (Webm)", new String[] { ".webm" }), VIDEO3GP("Video (3GP)", new String[] { ".3gp" }), VIDEOPODCAST("Video (MP4-Podcast)", new String[] { ".mp4" }), VIDEOIPHONE("Video (iPhone)", new String[] { ".mp4" });

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
