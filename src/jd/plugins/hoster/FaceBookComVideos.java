//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
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
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "https?://(www\\.)?facebook\\.com/(video/video\\.php\\?v=|video/embed\\?video_id=|profile\\.php\\?id=\\d+\\&ref=ts#\\!/video/video\\.php\\?v=|(photo/)?photo\\.php\\?v=|photo\\.php\\?fbid=)\\d+" }, flags = { 2 })
public class FaceBookComVideos extends PluginForHost {

    private String              FACEBOOKMAINPAGE           = "http://www.facebook.com";
    private String              PREFERHD                   = "PREFERHD";
    private static Object       LOCK                       = new Object();
    public static String        Agent                      = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2";
    private boolean             pluginloaded               = false;
    private static final String PHOTOLINK                  = "https?://(www\\.)?facebook\\.com/photo\\.php\\?fbid=\\d+";

    private String              DLLINK                     = null;
    private static final String FASTLINKCHECK_PICTURES     = "FASTLINKCHECK_PICTURES";
    private static final String USE_ALBUM_NAME_IN_FILENAME = "USE_ALBUM_NAME_IN_FILENAME";

    public FaceBookComVideos(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.facebook.com/r.php");
        setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) {
        String thislink = link.getDownloadURL().replace("https://", "http://");
        String videoID = new Regex(thislink, "facebook\\.com/(ts#\\!/video/video\\.php\\?v=|photo\\.php\\?v=|video/embed\\?video_id=)(\\d+)").getMatch(1);
        if (videoID != null) thislink = "http://facebook.com/video/video.php?v=" + videoID;
        link.setUrlDownload(thislink);
    }

    public String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.setCookie("http://www.facebook.com", "locale", "en_GB");
        br.setFollowRedirects(true);
        final boolean noAccountNeeded = link.getBooleanProperty("nologin", false);
        if (!noAccountNeeded) {
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null || !aa.isValid()) {
                link.setName(new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.facebookvideos.only4registered", "Links can only be checked if a valid account is entered"));
                return AvailableStatus.UNCHECKABLE;
            }
            login(aa, false, br);
        }
        br.getPage(link.getDownloadURL());
        String getThisPage = br.getRegex("window\\.location\\.replace\\(\"(http:.*?)\"").getMatch(0);
        if (getThisPage != null) br.getPage(getThisPage.replace("\\", ""));
        if (br.containsHTML("<h2 class=\"accessible_elem\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"mtm mbs mrs fsm fwn fcg\">[A-Za-z0-9:]+</span>([^<>\"]*?)</div>").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = Encoding.htmlDecode(filename.trim());

        if (link.getDownloadURL().matches(PHOTOLINK)) {
            // Try if a downloadlink is available
            DLLINK = br.getRegex("class=\"fbPhotosPhotoActionsItem\" href=\"(https?://[^<>\"]*?\\?dl=1)\"").getMatch(0);
            // Try to find original quality link
            // final String setID = br.getRegex("\"set\":\"([^<>\"]*?)\"").getMatch(0);
            // final String user = br.getRegex("\"user\":\"([^<>\"]*?)\"").getMatch(0);
            // if (setID != null && user != null && DLLINK == null) {
            // final String fbid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
            // final Browser br2 = br.cloneBrowser();
            // String theaterView = FACEBOOKMAINPAGE + "/photo.php?fbid=" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) +
            // "&set=" + setID +
            // "&type=3&src=http%3A%2F%2Fsphotos-a.ak.fbcdn.net%2Fhphotos-ak-prn1%2F621123_4356832729269_1617797914_n.jpg&smallsrc=";
            // theaterView = FACEBOOKMAINPAGE + "/ajax/pagelet/generic.php/PhotoViewerInitPagelet?no_script_path=1&data={%22fbid%22%3A%22" +
            // fbid + "%22%2C%22set%22%3A%22" + setID +
            // "%22%2C%22type%22%3A%221%22%2C%22firstLoad%22%3Atrue%2C%22ssid%22%3A1374160988631%7D&__user=" + user +
            // "&__a=1&__dyn=7n8ahyj35CFUlgDxqihXzAu&__req=jsonp_3&__adt=3";
            // br2.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            // br2.getPage(theaterView);
            // DLLINK = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            // }
            // If no downloadlink is there, simply try to find the directlink to the picture
            if (DLLINK == null) DLLINK = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) DLLINK = br.getRegex("id=\"fbPhotoImage\" src=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = DLLINK.replace("\\", "");

            // Try to change it to HD
            final Regex urlSplit = new Regex(DLLINK, "(https?://[a-z0-9\\-\\.]+/hphotos\\-ak\\-[a-z0-9]+)/(q\\d+/)?s\\d+x\\d+(/.+)");
            final String partOne = urlSplit.getMatch(0);
            final String partTwo = urlSplit.getMatch(2);
            if (partOne != null && partTwo != null) {
                // Usual part
                DLLINK = partOne + partTwo;
            }

            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html"))
                    link.setDownloadSize(con.getLongContentLength());
                else
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                if (this.getPluginConfig().getBooleanProperty(USE_ALBUM_NAME_IN_FILENAME, false)) {
                    link.setFinalFileName(filename + "_" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + ".jpg");
                } else {
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)).trim());
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            link.setFinalFileName(filename + ".mp4");
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true, br);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setStatus("Valid Facebook account is active");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.facebook.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (DLLINK != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            if (downloadLink.getBooleanProperty("nologin", false)) {
                handleVideo(downloadLink);
            } else {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered users");
            }
        }
    }

    private String getHigh() {
        return br.getRegex("%22hd_src%22%3A%22(http[^<>\"\\']*?)%22").getMatch(0);
    }

    private String getLow() {
        return br.getRegex("%22sd_src%22%3A%22(http[^<>\"\\']*?)%22").getMatch(0);
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            handleVideo(downloadLink);
        }
    }

    public void handleVideo(final DownloadLink downloadLink) throws Exception {
        if (DLLINK == null) {
            boolean preferHD = getPluginConfig().getBooleanProperty(PREFERHD);
            br.getRequest().setHtmlCode(unescape(br.toString()));
            if (preferHD) {
                DLLINK = getHigh();
                if (DLLINK == null) getLow();
            } else {
                DLLINK = getLow();
                if (DLLINK == null) getHigh();
            }
            if (DLLINK == null) {
                logger.warning("Final downloadlink (dllink) is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            DLLINK = Encoding.urlDecode(decodeUnicode(DLLINK), true);
            DLLINK = Encoding.htmlDecode(DLLINK);
            DLLINK = DLLINK.replace("\\", "");
            if (DLLINK == null) {
                logger.warning("Final downloadlink (dllink) is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        logger.info("Final downloadlink = " + DLLINK + " starting download...");
        final String Vollkornkeks = downloadLink.getDownloadURL().replace(FACEBOOKMAINPAGE, "");
        br.setCookie(FACEBOOKMAINPAGE, "x-referer", Encoding.urlEncode(FACEBOOKMAINPAGE + Vollkornkeks + "#" + Vollkornkeks));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String LOGINFAIL_GERMAN  = "\r\nEvtl. ungültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!\r\nBedenke, dass die Facebook Anmeldung per JD nur funktioniert, wenn Facebook\r\nkeine zusätzlichen Sicherheitsabfragen beim Login deines Accounts verlangt.\r\nPrüfe das und versuchs erneut!";
    private static final String LOGINFAIL_ENGLISH = "\r\nMaybe invalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!\r\nNote that the Facebook login via JD will only work if there are no additional\r\nsecurity questions when logging in your account.\r\nCheck that and try again!";

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force, Browser br) throws Exception {
        br.getHeaders().put("User-Agent", Agent);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("c_user") && cookies.containsKey("xs") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        br.setCookie(FACEBOOKMAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(true);
            br.getPage(FACEBOOKMAINPAGE);
            final String lang = System.getProperty("user.language");
            final Form loginForm = br.getForm(0);
            if (loginForm == null) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            loginForm.remove("persistent");
            loginForm.put("persistent", "1");
            loginForm.remove(null);
            loginForm.put("email", Encoding.urlEncode(account.getUser()));
            loginForm.put("pass", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginForm);
            /**
             * Facebook thinks we're an unknown device, now we prove we're not ;)
             */
            if (br.containsHTML(">Your account is temporarily locked")) {
                final String nh = br.getRegex("name=\"nh\" value=\"([a-z0-9]+)\"").getMatch(0);
                final String dstc = br.getRegex("name=\"fb_dtsg\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (nh == null || dstc == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_GERMAN, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_ENGLISH, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&submit%5BContinue%5D=Continue");

                final DownloadLink dummyLink = new DownloadLink(this, "Account", "facebook.com", "http://facebook.com", true);
                String achal = br.getRegex("name=\"achal\" value=\"([a-z0-9]+)\"").getMatch(0);
                final String captchaPersistData = br.getRegex("name=\"captcha_persist_data\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (captchaPersistData == null || achal == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_GERMAN, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_ENGLISH, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Normal captcha handling
                for (int i = 1; i <= 3; i++) {
                    String captchaLink = br.getRegex("\"(https?://(www\\.)?facebook\\.com/captcha/tfbimage\\.php\\?captcha_challenge_code=[^<>\"]*?)\"").getMatch(0);
                    if (captchaLink == null) break;
                    captchaLink = Encoding.htmlDecode(captchaLink);

                    String code;
                    try {
                        code = getCaptchaCode(captchaLink, dummyLink);
                    } catch (final Exception e) {
                        continue;
                    }
                    br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&captcha_response=" + Encoding.urlEncode(code) + "&achal=" + achal + "&submit%5BSubmit%5D=Submit");
                }

                // reCaptcha handling
                for (int i = 1; i <= 3; i++) {
                    final String rcID = br.getRegex("\"recaptchaPublicKey\":\"([^<>\"]*?)\"").getMatch(0);
                    if (rcID == null) break;

                    final String extraChallengeParams = br.getRegex("name=\"extra_challenge_params\" value=\"([^<>\"]*?)\"").getMatch(0);
                    final String captchaSession = br.getRegex("name=\"captcha_session\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (extraChallengeParams == null || captchaSession == null) break;

                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId(rcID);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c;
                    try {
                        c = getCaptchaCode(cf, dummyLink);
                    } catch (final Exception e) {
                        continue;
                    }
                    br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&captcha_session=" + Encoding.urlEncode(captchaSession) + "&extra_challenge_params=" + Encoding.urlEncode(extraChallengeParams) + "&recaptcha_type=password&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&captcha_response=" + Encoding.urlEncode(c) + "&achal=1&submit%5BSubmit%5D=Submit");
                }

                for (int i = 1; i <= 3; i++) {
                    if (br.containsHTML(">To confirm your identity, please enter your birthday")) {
                        achal = br.getRegex("name=\"achal\" value=\"([a-z0-9]+)\"").getMatch(0);
                        if (achal == null) break;
                        String birthdayVerificationAnswer;
                        try {
                            birthdayVerificationAnswer = getUserInput("Enter your birthday (dd:MM:yyyy)", dummyLink);
                        } catch (final Exception e) {
                            continue;
                        }
                        final String[] bdSplit = birthdayVerificationAnswer.split(":");
                        if (bdSplit == null || bdSplit.length != 3) continue;
                        int bdDay = 0, bdMonth = 0, bdYear = 0;
                        try {
                            bdDay = Integer.parseInt(bdSplit[0]);
                            bdMonth = Integer.parseInt(bdSplit[1]);
                            bdYear = Integer.parseInt(bdSplit[2]);
                        } catch (final Exception e) {
                            continue;
                        }
                        br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&birthday_captcha_month=" + bdMonth + "&birthday_captcha_day=" + bdDay + "&birthday_captcha_year=" + bdYear + "&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&achal=" + achal + "&submit%5BSubmit%5D=Submit");
                    } else {
                        break;
                    }
                }
            } else if (br.containsHTML("/checkpoint/")) {
                br.getPage("https://www.facebook.com/checkpoint/");
                final String postFormID = br.getRegex("name=\"post_form_id\" value=\"(.*?)\"").getMatch(0);
                final String nh = br.getRegex("name=\"nh\" value=\"(.*?)\"").getMatch(0);
                if (nh == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_GERMAN, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_ENGLISH, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&submit%5BContinue%5D=Weiter&nh=" + nh);
                br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&submit%5BThis+is+Okay%5D=Das+ist+OK&nh=" + nh);
                br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&machine_name=&submit%5BDon%27t+Save%5D=Nicht+speichern&nh=" + nh);
                br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&machine_name=&submit%5BDon%27t+Save%5D=Nicht+speichern&nh=" + nh);
            }
            if (br.getCookie(FACEBOOKMAINPAGE, "c_user") == null || br.getCookie(FACEBOOKMAINPAGE, "xs") == null) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_GERMAN, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGINFAIL_ENGLISH, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(FACEBOOKMAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
            account.setValid(true);
        }
    }

    @Override
    public String getDescription() {
        return "JDownloader's Facebook Plugin helps downloading videoclips and photo galleries. Facebook provides two different video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFERHD, JDL.L("plugins.hoster.facebookcomvideos.preferhd", "Videos: Prefer HD quality")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK_PICTURES, JDL.L("plugins.hoster.facebookcomvideos.fastlinkcheckpictures", "Photos: Enable fast linkcheck (filesize won't be shown in linkgrabber)?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_ALBUM_NAME_IN_FILENAME, JDL.L("plugins.hoster.facebookcomvideos.usealbumnameinfilename", "Photos: Use album name in filename [note that filenames change once the download starts]?")).setDefaultValue(true));
    }

    private String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}