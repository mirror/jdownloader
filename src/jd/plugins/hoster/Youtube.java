//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youtube.com" }, urls = { "(httpJDYoutube://[\\w\\.\\-]*?youtube\\.com/(videoplayback\\?.+|get_video\\?.*?video_id=.+&.+(&fmt=\\d+)?))|(httpJDYoutube://video\\.google\\.com/timedtext\\?type=track&name=.*?\\&lang=[a-z\\-]{2,}\\&v=[a-z\\-_A-Z0-9]+)|(httpJDYoutube://img\\.youtube.com/vi/[a-z\\-_A-Z0-9]+/(hqdefault|mqdefault|default|maxresdefault).jpg)" }, flags = { 2 })
public class Youtube extends PluginForHost {

    private static Object       lock                    = new Object();
    private boolean             prem                    = false;
    private static final String ISVIDEOANDPLAYLIST      = "ISVIDEOANDPLAYLIST";
    private static final String IDASFILENAME            = "ISASFILENAME";
    private static final String IDINFILENAME            = "IDINFILENAME_V2";
    private static final String FORMATINNAME            = "FORMATINNAME";
    private static final String PLAYLISTNUMBERINNAME    = "PLAYLISTNUMBERINNAME";
    private static final String USEUPLOADERINNAME       = "USEUPLOADERINNAME";
    private static final String ALLOW_MP3               = "ALLOW_MP3_V2";
    private static final String ALLOW_MP4               = "ALLOW_MP4_V2";
    private static final String ALLOW_WEBM              = "ALLOW_WEBM_V2";
    private static final String ALLOW_FLV               = "ALLOW_FLV_V2";
    private static final String ALLOW_3GP               = "ALLOW_3GP_V2";
    private static final String ALLOW_3D                = "ALLOW_3D_V2";
    private static final String ALLOW_144P              = "ALLOW_144P_V2";
    private static final String ALLOW_240P              = "ALLOW_240P_V2";
    private static final String ALLOW_360P              = "ALLOW_360P_V2";
    private static final String ALLOW_480P              = "ALLOW_480P_V2";
    private static final String ALLOW_520P              = "ALLOW_520P_V2";
    private static final String ALLOW_720P              = "ALLOW_720P_V2";
    private static final String ALLOW_1080P             = "ALLOW_1080P_V2";
    private static final String ALLOW_ORIGINAL          = "ALLOW_ORIGINAL_V2";
    private static final String ALLOW_BEST              = "ALLOW_BEST2";
    private static final String ALLOW_SUBTITLES         = "ALLOW_SUBTITLES_V2";
    private static final String GROUP_FORMAT            = "GROUP_FORMAT";
    private static final String ALLOW_THUMBNAIL_MAX     = "ALLOW_THUMBNAIL_MAX";
    private static final String ALLOW_THUMBNAIL_HQ      = "ALLOW_THUMBNAIL_HQ";
    private static final String ALLOW_THUMBNAIL_MQ      = "ALLOW_THUMBNAIL_MQ";
    private static final String ALLOW_THUMBNAIL_DEFAULT = "ALLOW_THUMBNAIL_DEFAULT";
    private static final String FAST_CHECK              = "FAST_CHECK2";
    private static final String PROXY_ACTIVE            = "PROXY_ACTIVE";
    private static final String PROXY_ADDRESS           = "PROXY_ADDRESS";
    private static final String PROXY_PORT              = "PROXY_PORT";

    @Override
    public String getAGBLink() {
        return "http://youtube.com/t/terms";
    }

    public static String unescape(String s) {
        if (s == null) return null;
        if (true) {
            // convert any html based unicode as a pre correction
            String test = s;
            String regex = "(&#x([0-9a-f]{4});)";
            String[] rmHtml = new Regex(s, regex).getColumn(0);
            if (rmHtml != null && rmHtml.length != 0) {
                // lets prevent wasteful cycles
                HashSet<String> dupe = new HashSet<String>();
                for (String htmlrm : rmHtml) {
                    if (dupe.add(htmlrm) == true) {
                        String[] rm = new Regex(htmlrm, regex).getRow(0);
                        if (rm[1] != null) {
                            test = test.replaceAll(rm[0], "\\\\u" + rm[1]);
                        }
                    }
                }
                s = test;
            }
        }
        char ch;
        char ch2;
        final StringBuilder sb = new StringBuilder();
        int ii;
        int i;
        for (i = 0; i < s.length(); i++) {
            ch = s.charAt(i);
            // prevents StringIndexOutOfBoundsException with ending char equals case trigger
            if (s.length() != i + 1) {
                switch (ch) {
                case '%':
                case '\\':
                    ch2 = ch;
                    ch = s.charAt(++i);
                    StringBuilder sb2 = null;
                    switch (ch) {
                    case 'u':
                        /* unicode */
                        sb2 = new StringBuilder();
                        i++;
                        ii = i + 4;
                        for (; i < ii; i++) {
                            ch = s.charAt(i);
                            if (sb2.length() > 0 || ch != '0') {
                                sb2.append(ch);
                            }
                        }
                        i--;
                        sb.append((char) Long.parseLong(sb2.toString(), 16));
                        continue;
                    case 'x':
                        /* normal hex coding */
                        sb2 = new StringBuilder();
                        i++;
                        ii = i + 2;
                        for (; i < ii; i++) {
                            ch = s.charAt(i);
                            sb2.append(ch);
                        }
                        i--;
                        sb.append((char) Long.parseLong(sb2.toString(), 16));
                        continue;
                    default:
                        if (ch2 == '%') {
                            sb.append(ch2);
                        }
                        sb.append(ch);
                        continue;
                    }
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    public Youtube(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.youtube.com/login?next=/index");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("httpJDYoutube", "http"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account, this.br, true, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus(JDL.L("plugins.hoster.youtube.accountok", "Account is OK."));
        ai.setValidUntil(-1);
        account.setValid(true);
        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        this.prem = false;

        /* we now have to get fresh links */

        downloadLink.setProperty("valid", false);
        this.requestFileInformation(downloadLink);
        this.br.setDebug(true);
        int maxChunks = 0;
        boolean resume = true;
        if (downloadLink.getBooleanProperty("subtitle", false) || downloadLink.getBooleanProperty("thumbnail", false)) {
            maxChunks = 1;
            resume = false;
        }
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), resume, maxChunks);
        if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video") && !downloadLink.getBooleanProperty("subtitle", false) && !downloadLink.getBooleanProperty("thumbnail", false)) {
            downloadLink.setProperty("valid", false);
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (this.dl.startDownload()) {
            this.postprocess(downloadLink);
        } else {
            try {
                final long lastProgress = downloadLink.getLongProperty("lastprogress", -1);
                if (lastProgress > 0) {
                    logger.info("LastProgress is: " + lastProgress);
                }
                final long progress = downloadLink.getPluginProgress().getTotal();
                logger.info("Stopped at current process: " + downloadLink.getPluginProgress().getTotal());
                downloadLink.setProperty("lastprogress", progress);
            } catch (final Exception e) {
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.login(account, this.br, false, false);
        this.prem = true;
        /* we now have to get fresh links */
        this.requestFileInformation(downloadLink);
        this.br.setDebug(true);
        int maxChunks = 0;
        boolean resume = true;
        if (downloadLink.getBooleanProperty("subtitle", false) || downloadLink.getBooleanProperty("thumbnail", false)) {
            maxChunks = 1;
            resume = false;
        }
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), resume, maxChunks);
        if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video")) {
            downloadLink.setProperty("valid", false);
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (this.dl.startDownload()) {
            this.postprocess(downloadLink);
        } else {
            try {
                final long lastProgress = downloadLink.getLongProperty("lastprogress", -1);
                if (lastProgress > 0) {
                    logger.info("LastProgress is: " + lastProgress);
                }
                final long progress = downloadLink.getPluginProgress().getTotal();
                logger.info("Stopped at current process: " + downloadLink.getPluginProgress().getTotal());
                downloadLink.setProperty("lastprogress", progress);
            } catch (final Exception e) {
            }
        }
    }

    public void login(final Account account, Browser br, boolean refresh, boolean showDialog) throws Exception {
        synchronized (Youtube.lock) {
            if (br == null) {
                br = this.br;
            }
            try {
                br.setDebug(true);
                this.setBrowserExclusive();
                if (account.getProperty("cookies") != null) {
                    @SuppressWarnings("unchecked")
                    HashMap<String, String> cookies = (HashMap<String, String>) account.getProperty("cookies");
                    if (cookies != null) {
                        if (cookies.containsKey("LOGIN_INFO")) {
                            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                                final String key = cookieEntry.getKey();
                                final String value = cookieEntry.getValue();
                                br.setCookie("youtube.com", key, value);
                            }

                            if (refresh == false)
                                return;
                            else {
                                br.getPage("http://www.youtube.com");
                                if (!br.containsHTML("<span class=\"yt-uix-button-content\">Sign In </span></button></div>")) { return; }
                            }
                        }
                    }
                }

                br.setFollowRedirects(true);
                br.getPage("http://www.youtube.com/");
                /* first call to google */
                br.getPage("https://www.google.com/accounts/ServiceLogin?uilel=3&service=youtube&passive=true&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252Findex&hl=en_US&ltmpl=sso");
                String checkConnection = br.getRegex("iframeUri: \\'(https.*?)\\'").getMatch(0);
                if (checkConnection != null) {
                    /*
                     * don't know if this is important but seems to set pstMsg to 1 ;)
                     */
                    checkConnection = unescape(checkConnection);
                    try {
                        br.cloneBrowser().getPage(checkConnection);
                    } catch (final Exception e) {
                        logger.info("checkConnection failed, continuing anyways...");
                    }
                }
                final Form form = br.getForm(0);
                form.put("pstMsg", "1");
                form.put("dnConn", "https%3A%2F%2Faccounts.youtube.com&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252F");
                form.put("Email", Encoding.urlEncode(account.getUser()));
                form.put("Passwd", Encoding.urlEncode(account.getPass()));
                form.put("GALX", br.getCookie("http://www.google.com", "GALX"));
                form.put("timeStmp", "");
                form.put("secTok", "");
                form.put("rmShown", "1");
                form.put("signIn", "Anmelden");
                form.put("asts", "");
                br.setFollowRedirects(false);
                final String cook = br.getCookie("http://www.google.com", "GALX");
                if (cook == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                br.submitForm(form);
                if (br.getRedirectLocation() == null) {
                    final String page = Encoding.htmlDecode(br.toString());
                    final String red = new Regex(page, "url='(http://.*?)'").getMatch(0);
                    if (red == null) {
                        account.setValid(false);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.getPage(red);
                }
                /* second call to google */
                br.getPage(br.getRedirectLocation());
                if (br.containsHTML("Google will check if this")) {
                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Youtube Login Error", "Please logout and login again at youtube.com, account check needed!");
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

                // 2-step verification
                if (br.containsHTML("2-step verification")) {
                    String step = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L("plugins.hoster.youtube.2step.title", "2-Step verification required"), JDL.L("plugins.hoster.youtube.2step.message", "Youtube.com requires Google's 2-Step verification. Please input the code from your phone or the backup list."), "", null, null, null);
                    Form stepform = br.getForm(0);
                    stepform.put("smsUserPin", step);
                    stepform.remove("exp");
                    stepform.remove("ltmpl");
                    br.setFollowRedirects(true);
                    br.submitForm(stepform);

                    if (br.containsHTML("The code you entered didn&#39;t verify")) {
                        account.setValid(false);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.2step.failed", "2-Step verification code couldn't be verified!"));
                    }

                    stepform = br.getForm(0);
                    stepform.remove("nojssubmit");
                    br.submitForm(stepform);
                    br.getPage("http://www.youtube.com/signin?action_handle_signin=true");
                } else if (br.containsHTML("class=\"gaia captchahtml desc\"")) {
                    if (true) {
                        account.setValid(false);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.logincaptcha.failed", "The captcha login verification is broken. Please contact our support."));
                    }
                    final String captchaLink = br.getRegex("<img src=\\'(https?://accounts\\.google\\.com/Captcha\\?[^<>\"]*?)\\'").getMatch(0);
                    if (captchaLink == null) {
                        account.setValid(false);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.logincaptcha.failed", "The captcha login verification is broken. Please contact our support."));
                    }
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "youtube.com", "http://youtube.com", true);
                    final String c = getCaptchaCode(captchaLink, dummyLink);
                    // Lots of stuff needed here
                    br.postPage("https://accounts.google.com/LoginVerification", "");

                } else {
                    br.setFollowRedirects(true);
                    br.getPage(br.getRedirectLocation());

                    String location = unescape(br.getRegex("location\\.replace\\(\"(.*?)\"").getMatch(0));
                    br.getPage(location);
                }
                if (br.getCookie("http://www.youtube.com", "LOGIN_INFO") == null) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies cYT = br.getCookies("youtube.com");
                for (final Cookie c : cYT.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                // set login cookie of the account.
                account.setProperty("cookies", cookies);
            } catch (PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    private void postprocess(final DownloadLink downloadLink) {
        if (downloadLink.getProperty("convertto") != null) {
            JDUtilities.getPluginForDecrypt("youtube.com");
            final jd.plugins.decrypter.TbCm.DestinationFormat convertto = jd.plugins.decrypter.TbCm.DestinationFormat.valueOf(downloadLink.getProperty("convertto").toString());
            jd.plugins.decrypter.TbCm.DestinationFormat InType = jd.plugins.decrypter.TbCm.DestinationFormat.VIDEOFLV;
            if (convertto.equals(jd.plugins.decrypter.TbCm.DestinationFormat.VIDEOWEBM) || convertto.equals(jd.plugins.decrypter.TbCm.DestinationFormat.VIDEOMP4) || convertto.equals(jd.plugins.decrypter.TbCm.DestinationFormat.VIDEO3GP)) {
                InType = convertto;
            }
            if (!jd.plugins.decrypter.TbCm.ConvertFile(downloadLink, InType, convertto)) {
                logger.severe("Video-Convert failed!");
            }
        }

        if (downloadLink.getBooleanProperty("subtitle", false)) {
            if (!jd.plugins.decrypter.TbCm.convertSubtitle(downloadLink)) {
                logger.severe("Subtitle conversion failed!");
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        // For streaming extension to tell her that these links can be streamed without account
        // System.out.println("Youtube: " + downloadLink);

        if (downloadLink.getBooleanProperty("subtitle", false) || downloadLink.getBooleanProperty("thumbnail", false)) {
            URLConnectionAdapter urlConnection = null;
            try {
                urlConnection = br.openGetConnection(downloadLink.getDownloadURL());

                if (urlConnection.getResponseCode() == 404) return AvailableStatus.FALSE;

                String size = urlConnection.getHeaderField("Content-Length");
                downloadLink.setDownloadSize(Long.parseLong(size));
                return AvailableStatus.TRUE;
            } finally {
                try {
                    urlConnection.disconnect();
                } catch (final Throwable e) {
                }
            }
        }

        downloadLink.setProperty("STREAMING", true);
        for (int i = 0; i < 4; i++) {
            if (downloadLink.getBooleanProperty("valid", true)) {
                downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
                downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", 0l));
                return AvailableStatus.TRUE;
            } else {
                downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
                downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", 0l));
                final PluginForDecrypt plugin = JDUtilities.getPluginForDecrypt("youtube.com");
                if (plugin == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "cannot decrypt videolink"); }
                if (downloadLink.getStringProperty("fmtNew", null) == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "You have to add link again"); }
                if (downloadLink.getStringProperty("videolink", null) == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "You have to add link again"); }

                final HashMap<Integer, String[]> LinksFound = ((jd.plugins.decrypter.TbCm) plugin).getLinks(downloadLink.getStringProperty("videolink", null), this.prem, this.br, 0);

                if (LinksFound == null || LinksFound.isEmpty()) {
                    if (this.br.containsHTML("<div\\s+id=\"verify-age-actions\">")) { throw new PluginException(LinkStatus.ERROR_FATAL, "The entered account couldn't pass the age verification!"); }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (LinksFound.get(downloadLink.getIntegerProperty("fmtNew", 0)) == null) {
                    // too fast connections??
                    Thread.sleep(5000);
                    continue;

                }
                downloadLink.setUrlDownload(LinksFound.get(downloadLink.getIntegerProperty("fmtNew", 0))[0]);
                return AvailableStatus.TRUE;
            }
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink downloadLink) {
        downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
        downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", 0l));
        downloadLink.setProperty("valid", false);
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getDescription() {
        return "JDownloader's YouTube Plugin helps downloading videoclips from youtube.com. YouTube provides different video formats and qualities. JDownloader is able to extract audio after download, and save it as mp3 file. \r\n - Hear your favourite YouTube Clips on your MP3 Player.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), ISVIDEOANDPLAYLIST, new String[] { JDL.L("plugins.host.youtube.isvideoandplaylist.video", "Only add video"), JDL.L("plugins.host.youtube.isvideoandplaylist.playlist", "Add playlist and video"), JDL.L("plugins.host.youtube.isvideoandplaylist.ask", "Ask everytime") }, JDL.L("plugins.host.youtube.isvideoandplaylist", "If a video also contains a playlist?")).setDefaultValue(2));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_CHECK, JDL.L("plugins.hoster.youtube.fast", "Fast LinkCheck (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        ConfigEntry id = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), IDASFILENAME, JDL.L("plugins.hoster.youtube.idasfilename", "Use Video-ID as filename?")).setDefaultValue(false);
        getConfig().addEntry(id);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), IDINFILENAME, JDL.L("plugins.hoster.youtube.idinfilename", "Use Video-ID additionally in filename?")).setDefaultValue(false).setEnabledCondidtion(id, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USEUPLOADERINNAME, JDL.L("plugins.hoster.youtube.useuploaderinname", "Use uploader name in filename?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FORMATINNAME, JDL.L("plugins.hoster.youtube.formatinname", "Use format in filename?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PLAYLISTNUMBERINNAME, JDL.L("plugins.hoster.youtube.playlistvideonumberinname", "Use video numbers in filename (works for playlist,user and course links)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_SUBTITLES, JDL.L("plugins.hoster.youtube.grabsubtitles", "Grab subtitles?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GROUP_FORMAT, JDL.L("plugins.hoster.youtube.groupbyformat", "Group by format?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MP3, JDL.L("plugins.hoster.youtube.checkmp3", "Grab MP3?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_3GP, JDL.L("plugins.hoster.youtube.check3gp", "Grab 3GP?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_FLV, JDL.L("plugins.hoster.youtube.checkflv", "Grab FLV?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MP4, JDL.L("plugins.hoster.youtube.checkmp4", "Grab MP4?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_WEBM, JDL.L("plugins.hoster.youtube.checkwebm", "Grab WEBM?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_144P, JDL.L("plugins.hoster.youtube.check144p", "Grab 144p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_240P, JDL.L("plugins.hoster.youtube.check240p", "Grab 240p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_360P, JDL.L("plugins.hoster.youtube.check360p", "Grab 360p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480P, JDL.L("plugins.hoster.youtube.check480p", "Grab 480p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_520P, JDL.L("plugins.hoster.youtube.check520p", "Grab 520p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720P, JDL.L("plugins.hoster.youtube.check720p", "Grab 720p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080P, JDL.L("plugins.hoster.youtube.check1080p", "Grab 1080p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_ORIGINAL, JDL.L("plugins.hoster.youtube.checkOrginal", "Grab Original (better than 1080p)?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.youtube.checkbest", "Returns the best videos within your speficied Resolution and Format selections above!")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_3D, JDL.L("plugins.hoster.youtube.preferd", "Allow 3D Videos?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_THUMBNAIL_MAX, JDL.L("plugins.hoster.youtube.grabrhumbnailmax", "Grab max. resulution thumbnail?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_THUMBNAIL_HQ, JDL.L("plugins.hoster.youtube.grabrhumbnailhq", "Grab HQ (480x360) thumbnail?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_THUMBNAIL_MQ, JDL.L("plugins.hoster.youtube.grabrhumbnailmq", "Grab MQ (320x180) thumbnail?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_THUMBNAIL_DEFAULT, JDL.L("plugins.hoster.youtube.grabrhumbnaildefault", "Grab default (120x90) thumbnail?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROXY_ACTIVE, JDL.L("plugins.hoster.youtube.proxyactive", "Use HTTP Proxy?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), PROXY_ADDRESS, JDL.L("plugins.hoster.youtube.proxyaddress", "Proxy Address")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), PROXY_PORT, JDL.L("plugins.hoster.youtube.proxyport", "Proxy Port")));
    }
}