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
package jd.plugins.hoster;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nicovideo.jp" }, urls = { "http://(www\\.)?nicovideo\\.jp/watch/(sm|so|nm)?\\d+" })
public class NicoVideoJp extends PluginForHost {
    private static final String  MAINPAGE                             = "http://www.nicovideo.jp/";
    private static final String  ONLYREGISTEREDUSERTEXT               = "Only downloadable for registered users";
    private static final String  CUSTOM_DATE                          = "CUSTOM_DATE";
    private static final String  CUSTOM_FILENAME                      = "CUSTOM_FILENAME";
    private static final String  TYPE_NM                              = "http://(www\\.)?nicovideo\\.jp/watch/nm\\d+";
    private static final String  TYPE_SM                              = "http://(www\\.)?nicovideo\\.jp/watch/sm\\d+";
    private static final String  TYPE_SO                              = "http://(www\\.)?nicovideo\\.jp/watch/so\\d+";
    /* Other types may redirect to this type. This is the only type which is also downloadable without account (sometimes?). */
    private static final String  TYPE_WATCH                           = "http://(www\\.)?nicovideo\\.jp/watch/\\d+";
    private static final String  default_extension                    = ".flv";
    private static final String  privatevid                           = "account.nicovideo.jp";
    private static final String  NOCHUNKS                             = "NOCHUNKS";
    private static final String  AVOID_ECONOMY_MODE                   = "AVOID_ECONOMY_MODE";
    private static final boolean FREE_RESUME                          = true;
    private static final int     FREE_MAXCHUNKS                       = 0;
    private static final int     FREE_MAXDOWNLOADS                    = 2;
    private static final boolean ACCOUNT_FREE_RESUME                  = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS               = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS            = 2;
    private static final int     economy_active_wait_minutes          = 30;
    private static final String  html_account_needed                  = "account\\.nicovideo\\.jp/register\\?from=watch\\&mode=landing\\&sec=not_login_watch";
    public static final long     trust_cookie_age                     = 300000l;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger totalMaxSimultanFreeDownload         = new AtomicInteger(FREE_MAXDOWNLOADS);
    private static AtomicInteger totalMaxSimultanFree_AccountDownload = new AtomicInteger(ACCOUNT_FREE_MAXDOWNLOADS);
    /* don't touch the following! */
    private static AtomicInteger maxPremium                           = new AtomicInteger(1);
    private static AtomicInteger maxFree                              = new AtomicInteger(1);
    private static Object        LOCK                                 = new Object();

    public NicoVideoJp(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://secure.nicovideo.jp/secure/register");
        setConfigElements();
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    /**
     * IMPORTANT: The site has a "normal" and "economy" mode. Normal mode = Higher video quality - mp4 streams. Economy mode = lower quality
     * - flv streams. Premium users are ALWAYS in the normal mode.
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String linkid_url = getLID(link);
        link.setProperty("extension", default_extension);
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        boolean loggedin = false;
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            this.login(aa, false);
            loggedin = true;
        }
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>ニコニコ動画　ログインフォーム</title>|>This video is for .*? only") || br.getURL().contains("/secure/") || br.getURL().contains("login_form?")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("this video inappropriate.<")) {
            String watch = br.getRegex("harmful_link\" href=\"([^<>\"]*?)\">Watch this video</a>").getMatch(0);
            br.getPage(watch);
        }
        if (br.getURL().contains(privatevid)) {
            link.getLinkStatus().setStatusText("This is a private video");
            link.setName(linkid_url);
            return AvailableStatus.TRUE;
        }
        final String channel;
        String filename;
        if (loggedin) {
            filename = br.getRegex("class=\"originalVideoTitle\">([^<>\"]+)<").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"videoTitle\">([^<>\"]+)<").getMatch(0);
            }
            channel = br.getRegex("data\\-click\\-target=\"userName\">([^<>\"]+)<").getMatch(0);
        } else {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1 itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
            }
            channel = br.getRegex("Uploader: <strong itemprop=\"name\">([^<>\"]*?)</strong>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        link.setProperty("plainfilename", filename);
        final String date = br.getRegex("property=\"video:release_date\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}\\+\\d{4})\"").getMatch(0);
        if (date != null) {
            link.setProperty("originaldate", date);
        }
        if (channel != null) {
            link.setProperty("channel", channel);
        }
        filename = getFormattedFilename(link);
        link.setName(filename);
        if (br.containsHTML(html_account_needed)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.nicovideojp.only4registered", ONLYREGISTEREDUSERTEXT));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        br = new Browser();
        return login(account, true);
    }

    @Override
    public String getAGBLink() {
        return "http://info.nicovideo.jp/base/rule.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPremium.get();
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        checkWatchableGeneral();
        String accessPOST = null;
        final String linkid_url = new Regex(br.getURL(), "(\\d+)$").getMatch(0);
        /* Most of the times an account is needed to watch/download videos. */
        if (br.containsHTML(html_account_needed) || !br.getURL().matches(TYPE_WATCH)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, ONLYREGISTEREDUSERTEXT, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        br.getPage("http://ext.nicovideo.jp/api/getthreadkey?language_id=1&thread=" + linkid_url);
        br.getPage("http://ext.nicovideo.jp/thumb_watch/" + linkid_url + "?&w=644&h=408&nli=1");
        final String playkey = br.getRegex("thumbPlayKey\\': \\'([^<>\"]*?)\\'").getMatch(0);
        final String accessFromHash = br.getRegex("accessFromHash\\': \\'([^<>\"]*?)\\'").getMatch(0);
        if (playkey == null || accessFromHash == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        accessPOST = "k=" + Encoding.urlEncode(playkey) + "&v=" + linkid_url + "&as3=1&accessFromDomain=&accessFromHash=" + Encoding.urlEncode(accessFromHash) + "&accessFromCount=0";
        br.postPage("http://ext.nicovideo.jp/thumb_watch", accessPOST);
        String dllink = new Regex(Encoding.htmlDecode(br.toString()), "\\&url=(http://.*?)\\&").getMatch(0);
        if (dllink == null) {
            dllink = new Regex(Encoding.htmlDecode(br.toString()), "(http://smile-com\\d+\\.nicovideo\\.jp/smile\\?v=[0-9\\.]+)").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxChunks = FREE_MAXCHUNKS;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        preDownloadHandling(link);
        try {
            /* add a download slot */
            controlFree(+1);
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NicoVideoJp.NOCHUNKS, false) == false) {
                    link.setProperty(NicoVideoJp.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(NicoVideoJp.NOCHUNKS, false) == false) {
                link.setProperty(NicoVideoJp.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        } finally {
            /* remove download slot */
            controlFree(-1);
        }
    }

    private void preDownloadHandling(final DownloadLink link) throws ParseException, PluginException {
        final String contenttype = dl.getConnection().getContentType();
        if (contenttype.equals("video/mp4")) {
            link.setProperty("extension", ".mp4");
        } else {
            /* Check if the user allows lower quality .flv files. */
            if (this.getPluginConfig().getBooleanProperty(AVOID_ECONOMY_MODE, false)) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Site is currently in economy mode", economy_active_wait_minutes * 60 * 1000l);
            }
            link.setProperty("extension", ".flv");
        }
        /* Now that we got the final extension of the file we can set the final filename. */
        final String final_filename = getFormattedFilename(link);
        link.setFinalFileName(final_filename);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        String dllink = null;
        final String linkid_url = getLID(link);
        requestFileInformation(link);
        br = new Browser();
        login(account, false);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        checkWatchableGeneral();
        /* Can happen if its not clear whether the video is private or offline */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (Encoding.htmlDecode(br.toString()).contains("closed=1\\&done=true")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        } else if (br.containsHTML(">This is a private video and not available")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Now downloadable: This is a private video");
        }
        String flashvars = br.getRegex("id=\"watchAPIDataContainer\" style=\"display:none\">(.*?)</div>").getMatch(0);
        if (br.getURL().matches(TYPE_SO)) {
            br.postPage("http://flapi.nicovideo.jp/api/getflv", "v=" + linkid_url);
        } else if (br.getURL().matches(TYPE_NM) || link.getDownloadURL().matches(TYPE_SM)) {
            final String vid = new Regex(br.getURL(), "((sm|nm)\\d+)$").getMatch(0);
            br.postPage("http://flapi.nicovideo.jp/api/getflv", "v=" + vid);
        }
        dllink = getDllink_account(flashvars != null ? flashvars : br.toString());
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxChunks = ACCOUNT_FREE_MAXCHUNKS;
        if (link.getBooleanProperty(NOCHUNKS, false) && getPluginConfig().getBooleanProperty(NOCHUNKS, true)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_FREE_RESUME, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        preDownloadHandling(link);
        try {
            /* add a download slot */
            controlPremium(+1);
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NicoVideoJp.NOCHUNKS, false) == false) {
                    link.setProperty(NicoVideoJp.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(NicoVideoJp.NOCHUNKS, false) == false) {
                link.setProperty(NicoVideoJp.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        } finally {
            /* remove download slot */
            controlPremium(-1);
        }
    }

    /* Checks if a video is watch-/downloadable, works for logged in- and unregistered users. */
    private void checkWatchableGeneral() throws PluginException {
        if (this.br.containsHTML(">Unable to play video|>You can view this video by join")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
    }

    private String getDllink_account(final String flashvars) {
        final String singleDecode = Encoding.htmlDecode(flashvars);
        String dllink = PluginJSonUtils.getJsonValue(singleDecode, "flvInfo");
        dllink = dllink != null ? asdf(dllink) : asdf(singleDecode);
        return dllink;
    }

    private String asdf(String input) {
        String dllink = new Regex(input, "\\&url=(http.*?)\\&").getMatch(0);
        if (dllink == null) {
            dllink = new Regex(input, "(http://smile\\-[a-z]+\\d+\\.nicovideo\\.jp/smile\\?(?:v|m)=[0-9\\.]+[a-z]*)").getMatch(0);
        }
        String decodedInput = input;
        while (dllink == null && new Regex(decodedInput, "%[a-fA-F0-9]{2}").matches()) {
            decodedInput = Encoding.urlDecode(decodedInput, false);
            dllink = new Regex(decodedInput, "\\&url=(http.*?)\\&").getMatch(0);
            if (dllink == null) {
                dllink = new Regex(decodedInput, "(http://smile\\-[a-z]+\\d+\\.nicovideo\\.jp/smile\\?(?:v|m)=[0-9\\.]+[a-z]*)").getMatch(0);
            }
        }
        return Encoding.urlDecode(dllink, false);
    }

    /**
     * orce = check cookies and perform a full login if that fails. !force = Accept cookies without checking if they're not older than
     * trust_cookie_age.
     */
    private AccountInfo login(final Account account, final boolean force) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (LOCK) {
            this.setBrowserExclusive();
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                /* 2016-05-04: Avoid full login whenever possible! */
                br.setCookies(this.getHost(), cookies);
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age && !force) {
                    /* We trust these cookies --> Do not check them */
                    return ai;
                }
                br.getPage("http://www.nicovideo.jp/");
                if (br.containsHTML("/logout\">Log out</a>")) {
                    /* Save new cookie timestamp */
                    br.setCookies(this.getHost(), cookies);
                    return null;
                }
                /* Full login needed */
                br = new Browser();
            }
            /* Try multiple times - it sometimes just doesn't work :( */
            boolean success = false;
            for (int i = 0; i <= 5; i++) {
                br = new Browser();
                br.setFollowRedirects(true);
                br.getPage("http://www.nicovideo.jp/");
                br.getPage("/login");
                // dont want to follow redirect here, as it takes you to homepage..
                br.setFollowRedirects(false);
                // this will redirect with session info.
                br.getHeaders().put("Accept-Encoding", "gzip, deflate, br");
                br.getHeaders().put("Referer", "https://account.nicovideo.jp/login");
                br.postPage("//account.nicovideo.jp/api/v1/login?show_button_twitter=1&site=niconico&show_button_facebook=1", "mail_tel=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "user_session") == null || "deleted".equals(br.getCookie(MAINPAGE, "user_session"))) {
                    continue;
                }
                success = true;
                break;
            }
            if (!success) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            // there are multiple account types (free and paid services)
            br.getPage("//account.nicovideo.jp/my/account");
            if (br.containsHTML("<span class=\"membership--status\">(?:Yearly|Monthly|Weekly|Daily) plan</span>")) {
                account.setType(AccountType.PREMIUM);
                ai.setStatus("Premium Account");
            } else {
                account.setType(AccountType.FREE);
                ai.setStatus("Free Account");
            }
            account.saveCookies(this.br.getCookies(this.getHost()), "");
            ai.setUnlimitedTraffic();
            return ai;
        }
    }

    @SuppressWarnings("deprecation")
    private String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final String extension = downloadLink.getStringProperty("extension", default_extension);
        final String videoid = new Regex(downloadLink.getDownloadURL(), "/watch/(.+)").getMatch(0);
        String videoName = downloadLink.getStringProperty("plainfilename", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("nicovideo.jp");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = defaultCustomFilename;
        }
        if (!formattedFilename.contains("*videoname") || !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename;
        }
        String date = downloadLink.getStringProperty("originaldate", null);
        final String channelName = downloadLink.getStringProperty("channel", null);
        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            date = date.replace("T", ":");
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
            // 2009-08-30T22:49+0900
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm+ssss");
            Date dateStr = formatter.parse(date);
            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);
            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null) {
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            } else {
                formattedFilename = formattedFilename.replace("*date*", "");
            }
        }
        formattedFilename = formattedFilename.replace("*videoid*", videoid);
        if (formattedFilename.contains("*channelname*")) {
            if (channelName != null) {
                formattedFilename = formattedFilename.replace("*channelname*", channelName);
            } else {
                formattedFilename = formattedFilename.replace("*channelname*", "");
            }
        }
        formattedFilename = formattedFilename.replace("*ext*", extension);
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*videoname*", videoName);
        return formattedFilename;
    }

    private String getLID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    public synchronized void controlPremium(final int num) {
        logger.info("maxPremium was = " + maxPremium.get());
        maxPremium.set(Math.min(Math.max(1, maxPremium.addAndGet(num)), totalMaxSimultanFree_AccountDownload.get()));
        logger.info("maxPremium now = " + maxPremium.get());
    }

    public synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    @Override
    public String getDescription() {
        return "JDownloader's nicovideo.jp plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    private final static String defaultCustomFilename = "*videoname**ext*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.nicovideojp.customdate", "Define how the date should look.")).setDefaultValue("dd.MM.yyyy_HH-mm-ss"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channelname*_*date*_*videoname**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.nicovideojp.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*channelname* = name of the channel/uploader\r\n");
        sb.append("*date* = date when the video was posted - appears in the user-defined format above\r\n");
        sb.append("*videoname* = name of the video without extension\r\n");
        sb.append("*videoid* = ID of the video e.g. 'sm12345678'\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.flv'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NicoVideoJp.AVOID_ECONOMY_MODE, JDL.L("plugins.hoster.MicoVideoJp.AvoidEconomymode", "Avoid economy mode - only download higher quality .mp4 videos?\r\n<html><b>Important: The default extension of all filenames is " + default_extension + ". It will be corrected once the downloads start if either this setting is active or the nicovideo site is in normal (NOT economy) mode!\r\nIf this setting is active and nicovideo is in economy mode, JDownloader will wait " + economy_active_wait_minutes + " minutes and try again afterwards.</b></html>")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NicoVideoJp.NOCHUNKS, JDL.L("plugins.hoster.MicoVideoJp.NoChunk", "Enable Chunk Workaround")).setDefaultValue(true));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}