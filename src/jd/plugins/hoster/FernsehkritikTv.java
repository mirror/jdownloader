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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.decrypter.GenericM3u8Decrypter.HlsContainer;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fernsehkritik.tv", "massengeschmack.tv" }, urls = { "https?://(?:www\\.)?fernsehkritik\\.tv/jdownloaderfolgeneu?\\d+", "https?://(?:www\\.)?massengeschmack\\.tv/(?:play|clip)/[a-z0-9\\-]+|https?://(?:www\\.)?massengeschmack\\.tv/live/[a-z0-9\\-]+|https?://massengeschmack\\.tv/dl/.+" }, flags = { 2, 2 })
public class FernsehkritikTv extends PluginForHost {

    public static final long trust_cookie_age = 300000l;

    /* Officially their (API) cookie is only valid for max 30 minutes! */
    // public static final long max_cookie_age = 100800l;

    public FernsehkritikTv(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://massengeschmack.tv/register/");
        this.setConfigElements();
    }

    @Override
    public String rewriteHost(String host) {
        if ("fernsehkritik.tv".equals(getHost())) {
            if (host == null || "fernsehkritik.tv".equals(host)) {
                return "massengeschmack.tv";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://fernsehkritik.tv/datenschutzbestimmungen/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://").replace("/clip/", "/play/"));
    }

    private static final String  TYPE_FOLGE_NEW                            = "http://fernsehkritik\\.tv/jdownloaderfolgeneu\\d+";
    private static final String  TYPE_MASSENGESCHMACK_GENERAL              = "https?://(?:www\\.)?massengeschmack\\.tv/play/[a-z0-9\\-]+";
    private static final String  TYPE_MASSENGESCHMACK_LIVE                 = "https?://(?:www\\.)?massengeschmack\\.tv/live/[a-z0-9\\-]+";
    private static final String  TYPE_MASSENGESCHMACK_DIRECT               = "https?://massengeschmack\\.tv/dl/.+";

    public static final String   HTML_MASSENGESCHMACK_OFFLINE              = ">Clip nicht gefunden|>Error 404<|>Die angeforderte Seite wurde nicht gefunden";
    private static final String  HTML_MASSENGESCHMACK_CLIP_PREMIUMONLY     = ">Clip nicht kostenlos verfügbar";

    /* API stuff */
    private static final boolean VIDEOS_ENABLE_API                         = true;
    public static final String   API_BASE_URL                              = "https://massengeschmack.tv/api/v1/";
    public static final String   API_LIST_SUBSCRIPTIONS                    = "?action=listSubscriptions";
    public static final String   API_GET_CLIP                              = "?action=getClip&identifier=%s";
    public static final int      API_RESPONSECODE_ERROR_CONTENT_OFFLINE    = 400;
    public static final int      API_RESPONSECODE_ERROR_LOGIN_WRONG        = 401;
    public static final int      API_RESPONSECODE_ERROR_RATE_LIMIT_REACHED = 500;

    private static final String  HOST_MASSENGESCHMACK                      = "massengeschmack.tv";
    private static final String  GRAB_POSTECKE                             = "GRAB_POSTECKE";
    private static final String  CUSTOM_DATE                               = "CUSTOM_DATE_2";
    private static final String  CUSTOM_FILENAME_FKTV                      = "CUSTOM_FILENAME_FKTV_2";
    private static final String  CUSTOM_FILENAME_MASSENGESCHMACK_OTHER     = "CUSTOM_FILENAME_MASSENGESCHMACK_OTHER_5";
    private static final String  CUSTOM_PACKAGENAME                        = "CUSTOM_PACKAGENAME";
    private static final String  FASTLINKCHECK                             = "FASTLINKCHECK";

    private static final String  EMPTY_FILENAME_INFORMATION                = "-";
    private static final String  default_EXT                               = ".mp4";

    private static Object        LOCK                                      = new Object();
    private String               DLLINK                                    = null;
    private boolean              loggedin                                  = false;
    private boolean              is_premiumonly_content                    = false;
    private String               url_videoid                               = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink, AccountController.getInstance().getValidAccount(this));
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    /**
     * Using API wherever possible:<br />
     * <a href="https://massengeschmack.tv/api/doc.php">massengeschmack.tv API documentation</a><br />
     * Another way to easily get the BEST(WORST) file/rendition of a video: <br />
     * https://massengeschmack.tv/dlr/<ClipID>/(best|mobile).mp4<br />
     * Example: https://massengeschmack.tv/dlr/sakura-1/best.mp4<br />
     */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.is_premiumonly_content = false;
        this.DLLINK = null;
        this.url_videoid = getUrlNameForMassengeschmackGeneral(link);
        this.br.setFollowRedirects(true);
        long filesize = -1;
        long filesize_max = 0;
        long filesize_temp = 0;

        final String api_best_url = getBESTDllinkSpecialAPICall();
        String dllink_temp = null;
        String filesize_string = null;
        String url_videoid_without_episodenumber = null;
        String final_filename = null;
        String episodenumber = null;
        String channel = null;
        String episodename = null;
        String date = null;
        String ext = null;
        String description = null;
        URLConnectionAdapter con = null;

        if (account != null) {
            try {
                login(this.br, account, false);
                loggedin = true;
            } catch (final Throwable e) {
            }
        }

        try {

            if (link.getDownloadURL().matches(TYPE_FOLGE_NEW)) {
                this.DLLINK = link.getStringProperty("directlink", null);
                if (this.DLLINK == null) {
                    br.getPage(link.getStringProperty("mainlink", null));
                    /* This case is nearly impossible */
                    if (br.containsHTML(HTML_MASSENGESCHMACK_OFFLINE)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (br.containsHTML(HTML_MASSENGESCHMACK_CLIP_PREMIUMONLY)) {
                        link.getLinkStatus().setStatusText("Zurzeit nur für Massengeschmack Abonenten herunterladbar");
                        this.is_premiumonly_content = true;
                        return AvailableStatus.TRUE;
                    }
                    /* Prefer webm as the video bitrate is higher than mp4. Audio bitrate is a bit lower but that should be fine. */
                    this.DLLINK = br.getRegex("(?:type=\"video/webm\" src=|type: \"video/webm\",src:  )\"(https?://[a-z0-9]+\\.massengeschmack\\.tv/deliver/t/[^<>\"]+\\.webm)\"").getMatch(0);
                    if (this.DLLINK == null) {
                        this.DLLINK = br.getRegex("(?:type=\"video/mp4\" src=|type: \"video/mp4\",src:  )\"(https?://[a-z0-9]+\\.massengeschmack\\.tv/deliver/t/[^<>\"]+\\.mp4)\"").getMatch(0);
                    }
                    if (this.DLLINK == null) {
                        this.DLLINK = br.getRegex("\"(https?://[a-z0-9]+\\.massengeschmack\\.tv/deliver/t/[^<>\"]+\\.webm)\"").getMatch(0);
                    }
                    if (this.DLLINK == null) {
                        this.DLLINK = br.getRegex("\"(https?://[a-z0-9]+\\.massengeschmack\\.tv/deliver/t/[^<>\"]+\\.mp4)\"").getMatch(0);
                    }
                }
                if (inValidate(this.DLLINK)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                ext = this.DLLINK.substring(this.DLLINK.lastIndexOf("."));
                link.setProperty("directtype", ext);
                final_filename = getFKTVFormattedFilename(link);
            } else if (link.getDownloadURL().matches(TYPE_MASSENGESCHMACK_GENERAL) && VIDEOS_ENABLE_API && loggedin) {
                url_videoid_without_episodenumber = getVideoidWithoutEpisodenumber(url_videoid);

                apiGetPage(this.br, String.format(API_BASE_URL + API_GET_CLIP, url_videoid));
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(this.br.toString());
                final ArrayList<Object> files = (ArrayList) entries.get("files");
                channel = (String) entries.get("pdesc");
                episodename = (String) entries.get("title");
                description = (String) entries.get("desc");
                date = Long.toString(DummyScriptEnginePlugin.toLong(entries.get("date"), -1));
                for (final Object fileo : files) {
                    entries = (LinkedHashMap<String, Object>) fileo;
                    filesize_temp = DummyScriptEnginePlugin.toLong(entries.get("size"), -1);
                    dllink_temp = (String) entries.get("url");
                    if (inValidate(dllink_temp) && filesize_temp != -1) {
                        continue;
                    }
                    if (dllink_temp.startsWith("//")) {
                        dllink_temp = "https:" + dllink_temp;
                    }
                    if (filesize_temp > filesize_max) {
                        filesize_max = filesize_temp;
                        this.DLLINK = dllink_temp;
                    }
                }

                if (!inValidate(this.DLLINK)) {
                    /* Okay we already have a final downloadlink but let's try to get an even higher quality via api_best_url. */
                    /*
                     * This might sometimes get us the HD version even if it is not officially available for registered-/free users:
                     * http://forum.massengeschmack.tv/showthread.php?17604-Api&p=439951#post439951
                     */
                    con = this.br.openHeadConnection(api_best_url);
                    if (con.isOK() && !con.getContentType().contains("html")) {
                        filesize = con.getLongContentLength();
                        this.DLLINK = api_best_url;
                    }
                }

                if (inValidate(channel) || inValidate(episodename) || date.equals("-1")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                episodenumber = new Regex(episodename, "Folge (\\d+)").getMatch(0);
                /* Fix episodename */
                if (episodenumber != null) {
                    /* Clean episodename! */
                    final String realepisodename = new Regex(episodename, "Folge \\d+: \"([^<>\"]*?)\"").getMatch(0);
                    if (realepisodename != null) {
                        episodename = realepisodename;
                    } else {
                        episodename = episodename.replace("Folge " + episodenumber, "");
                    }
                }
                /* Fix channel */
                if (!channel.toLowerCase().contains(url_videoid_without_episodenumber.toLowerCase())) {
                    channel = url_videoid_without_episodenumber;
                }

                if (inValidate(this.DLLINK) && account.getType() == AccountType.FREE) {
                    /*
                     * Rare special case: User has a free account, video is NOT downloadable for freeusers but is watchable. In this case we
                     * cannot get any downloadlinks via API --> Website handling needed! This could also be used as a fallback for API
                     * failures but it is not intended to be that ;)
                     */
                    try {
                        this.br.getPage("http://massengeschmack.tv/play/" + this.url_videoid);
                        getStreamDllinkMassengeschmackWebsite();
                    } catch (final Throwable e) {
                    }
                }

                if (!inValidate(DLLINK)) {
                    ext = DLLINK.substring(DLLINK.lastIndexOf("."));
                } else {
                    is_premiumonly_content = true;
                    ext = default_EXT;
                }

                if (!inValidate(episodename)) {
                    episodename = Encoding.htmlDecode(episodename).trim();
                    link.setProperty("directepisodename", episodename);
                }
                link.setProperty("directdate", date);
                if (!inValidate(channel)) {
                    channel = Encoding.htmlDecode(channel).trim();
                    link.setProperty("directchannel", channel);
                }
                link.setProperty("directepisodenumber", episodenumber);
                link.setProperty("directtype", ext);

                final_filename = getMassengeschmack_other_FormattedFilename(link);

            } else if (link.getDownloadURL().matches(TYPE_MASSENGESCHMACK_GENERAL) || link.getDownloadURL().matches(TYPE_MASSENGESCHMACK_LIVE)) {
                br.getPage(link.getDownloadURL());
                if (br.containsHTML(HTML_MASSENGESCHMACK_OFFLINE) || this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (this.br.getHttpConnection().getResponseCode() == 403) {
                    link.getLinkStatus().setStatusText("No free downloadable version available");
                    link.setName(new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)$").getMatch(0));
                    this.is_premiumonly_content = true;
                    return AvailableStatus.TRUE;
                }
                if (link.getDownloadURL().matches(TYPE_MASSENGESCHMACK_LIVE)) {
                    /* Get m3u8 main playlist */
                    DLLINK = br.getRegex("\"(https?://dl\\.massengeschmack\\.tv/live/[^<>\"]*?adaptive\\.m3u8)\"").getMatch(0);
                    episodename = this.br.getRegex("class=\"active\"><span>([^<>\"]*?)</span></li>").getMatch(0);
                    if (episodename == null) {
                        /* Fallback to url */
                        episodename = new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)$").getMatch(0);
                    }
                    episodenumber = new Regex(episodename, "(\\d+)$").getMatch(0);
                    if (episodenumber != null) {
                        /* Remove episodenumber from episodename! */
                        episodename = episodename.substring(0, episodename.length() - episodenumber.length());
                    }
                    /* Get date without time */
                    date = br.getRegex("<p class=\"muted\">(\\d{2}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2})[^<>\"]+<").getMatch(0);
                } else {
                    /* Try to get official download url (usually there is none available for free users) */
                    final String[] downloadlink_info = this.br.getRegex("(massengeschmack\\.tv/dl/.*?</small></em></a></li>)").getColumn(0);
                    if (downloadlink_info != null) {
                        for (final String dlinfo : downloadlink_info) {
                            dllink_temp = new Regex(dlinfo, "(/dl/[^<>\"]*?\\.mp4[^<>\"/]*?)\"").getMatch(0);
                            filesize_string = new Regex(dlinfo, "\\((\\d+(?:,\\d+)? (?:MiB|GiB))\\)").getMatch(0);
                            if (filesize_string != null) {
                                filesize_string = filesize_string.replace(",", ".");
                                filesize_temp = SizeFormatter.getSize(filesize_string);
                            }
                            if (filesize_temp > filesize_max) {
                                filesize_max = filesize_temp;
                                this.DLLINK = dllink_temp;
                            }
                        }
                        if (!inValidate(DLLINK)) {
                            filesize = filesize_max;
                            if (!DLLINK.startsWith("http")) {
                                DLLINK = "http://" + HOST_MASSENGESCHMACK + DLLINK;
                            }
                        }
                    }
                    if (inValidate(DLLINK)) {
                        /* Last chance fallback! */
                        getStreamDllinkMassengeschmackWebsite();
                    }

                    if (!inValidate(this.DLLINK)) {
                        /* Okay we already have a final downloadlink but let's try to get an even higher quality via api_best_url. */
                        /*
                         * This might sometimes get us the HD version even if it is not officially available for registered-/free users:
                         * http://forum.massengeschmack.tv/showthread.php?17604-Api&p=439951#post439951
                         */
                        con = this.br.openHeadConnection(api_best_url);
                        if (con.isOK() && !con.getContentType().contains("html")) {
                            filesize = con.getLongContentLength();
                            this.DLLINK = api_best_url;
                        }
                    }

                    channel = br.getRegex("<li><a href=\"/mag\\-cover\\.php\\?pid=\\d+\">([^<<\"]*?)</a>").getMatch(0);
                    episodename = br.getRegex("<h3>([^<>\"]*?)</h3>").getMatch(0);
                    date = br.getRegex("<p class=\"muted\">([^<>\"]*?) /[^<]+</p>").getMatch(0);
                    episodenumber = this.br.getRegex("class=\"active\"><span>Folge (\\d+)</span></li>").getMatch(0);

                    if (episodenumber == null && !inValidate(episodename)) {
                        episodenumber = new Regex(episodename, "Folge (\\d+)").getMatch(0);
                    }
                    if (!inValidate(episodename) && !inValidate(episodenumber)) {
                        final String episodetext = "Folge " + episodenumber;
                        final String episodetext2 = episodetext + ": ";
                        episodename = episodename.replace(episodetext2, "");
                        episodename = episodename.replace(episodetext, "");
                    }
                }

                if (ext == null) {
                    ext = DLLINK.substring(DLLINK.lastIndexOf("."));
                }
                if (ext.equals(".m3u8")) {
                    ext = default_EXT;
                }

                if (!inValidate(episodename)) {
                    episodename = Encoding.htmlDecode(episodename).trim();
                    link.setProperty("directepisodename", episodename);
                }
                if (!inValidate(date)) {
                    link.setProperty("directdate", date);
                }
                if (!inValidate(channel)) {
                    channel = Encoding.htmlDecode(channel).trim();
                    link.setProperty("directchannel", channel);
                }
                link.setProperty("directepisodenumber", episodenumber);
                link.setProperty("directtype", ext);

                final_filename = getMassengeschmack_other_FormattedFilename(link);

            } else if (link.getDownloadURL().matches(TYPE_MASSENGESCHMACK_DIRECT)) {
                /* Most times such links will be premium only but there are also free downloadable direct urls! */
                final_filename = new Regex(link.getDownloadURL(), "/([^/]+)$").getMatch(0);
                if (final_filename == null) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                DLLINK = link.getDownloadURL();
            } else {
                link.getLinkStatus().setStatusText("Unknown linkformat");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            link.setFinalFileName(final_filename);

            if (filesize < 0 && filesize_string != null) {
                filesize = SizeFormatter.getSize(filesize_string);
            }

            if (filesize < 0 && !inValidate(DLLINK) && !DLLINK.contains(".m3u8")) {
                con = br.openHeadConnection(DLLINK);
                final long responsecode = con.getResponseCode();
                if (con.isOK() && !con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
                } else if (responsecode == API_RESPONSECODE_ERROR_LOGIN_WRONG || responsecode == 403) {
                    this.is_premiumonly_content = true;
                } else {
                    /* 404 and/or html --> Probably offline */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }

        if (filesize > -1) {
            link.setDownloadSize(filesize);
        }

        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }

        return AvailableStatus.TRUE;
    }

    private void getStreamDllinkMassengeschmackWebsite() {
        /* Next try hls. */
        if (inValidate(DLLINK)) {
            DLLINK = br.getRegex("type=\"application/x\\-mpegurl\" src=\"(http://[^<>\"]*?\\.m3u8)\"").getMatch(0);
        }
        /* Sometimes different hls qualities are available - prefer webm, highest quality */
        if (inValidate(DLLINK)) {
            DLLINK = br.getRegex("type=\"video/webm\" src=\"(http://[^<>\"]*?\\.webm)\"").getMatch(0);
        }
        if (inValidate(DLLINK)) {
            DLLINK = br.getRegex("type=\"video/mp4\" src=\"(http://[^<>\"]*?\\.mp4)\"").getMatch(0);
        }
        if (inValidate(DLLINK)) {
            DLLINK = br.getRegex("(/dl/[^<>\"]*?\\.mp4[^<>\"/]*?)\"").getMatch(0);
        }
        /* Nothing there? Try to download stream! */
        if (inValidate(DLLINK)) {
            final String base = br.getRegex("var base = \\'(http://[^<>\"]*?)\\';").getMatch(0);
            final String link = br.getRegex("playlist = \\[\\{url: base \\+ \\'([^<>\"]*?)\\'").getMatch(0);
            if (base != null && link != null) {
                DLLINK = base + link;
            }
        }

        if (!inValidate(DLLINK) && !DLLINK.startsWith("http")) {
            DLLINK = "http://" + HOST_MASSENGESCHMACK + DLLINK;
        }
    }

    /** https://massengeschmack.tv/dlr/[url_videoid]/[best|mobile|hd].mp4 */
    private String getBESTDllinkSpecialAPICall() {
        /*
         * Function/API Call of which we definitly know that we will get the highest quality (kind of like an API).
         */
        return "https://massengeschmack.tv/dlr/" + url_videoid + "/best.mp4";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!(account.getUser().matches(".+@.+"))) {
            ai.setStatus("Bitte trage deine E-Mail Adresse ins 'Benutzername' Feld ein!");
            account.setValid(false);
            return ai;
        }
        long expirelong = -1;
        login(this.br, account, true);
        br.getPage("/u/");
        String expire = br.getRegex("bis (\\d{1,2}\\. [A-Za-z]+ \\d{4})").getMatch(0);
        if (expire == null) {
            expire = br.getRegex("(\\d{1,2}\\. (?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember) 20\\d{2})").getMatch(0);
        }
        if (expire != null) {
            expirelong = TimeFormatter.getMilliSeconds(expire, "dd. MMMM yyyy", Locale.GERMANY);
        }
        if (System.currentTimeMillis() - expirelong > 0 || br.containsHTML(">Kein Abonnement aktiv")) {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd. MMMM yyyy", Locale.GERMANY));
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, null);
        if (link.getDownloadURL().matches(TYPE_FOLGE_NEW) && br.containsHTML(HTML_MASSENGESCHMACK_CLIP_PREMIUMONLY)) {
            /* User added a current fernsehkritik episode which is not yet available for free. */
            final String date = link.getStringProperty("directdate", null);
            final long timestamp_released = getTimestampFromDate(date);
            final long timePassed = System.currentTimeMillis() - timestamp_released;
            if (timePassed > 14 * 24 * 60 * 60 * 1000l) {
                /*
                 * This should never happen - even if the Fernsehkritiker is VERY late but in case the current episode is not available for
                 * free after 14 days we have to assume that it is only available for Massengeschmack members.!
                 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                /* Less than 14 days after the release of the episode --> Wait for free release */
                final long waitUntilFreeRelease;
                if (timePassed < 7 * 24 * 60 * 60 * 1000l) {
                    /*
                     * The Fernsehkritiker usually releases new episodes for free 7 days after the release for Massengeschmack members.
                     */
                    waitUntilFreeRelease = (timestamp_released + 7 * 24 * 60 * 60 * 1000l) - System.currentTimeMillis();
                } else {
                    /*
                     * It's more than 7 days but still less than 14...okay let's ait 3 hours and try again - the new episode should be out
                     * soon and if we pass 14 days without the release, users will see the PREMIUMONLY message (actually this should never
                     * happen for Fernsehkritik episodes as all of them get released free after some time.).
                     */
                    waitUntilFreeRelease = 1 * 60 * 60 * 1000l;
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Die kostenlose Version dieser Episode wurde noch nicht freigegeben", waitUntilFreeRelease);
            }
        }
        if (this.is_premiumonly_content) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (inValidate(DLLINK)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (DLLINK.contains(".m3u8")) {
            downloadHLS(link);
        } else {
            /* More chunks work but download will stop at random point! */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unbekannter Serverfehler", 30 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        if (this.is_premiumonly_content) {
            /*
             * Different accounts have different shows - so a premium account must not necessarily have the rights to view/download
             * everything!
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (inValidate(DLLINK)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.setFollowRedirects(true);
        if (DLLINK.contains(".m3u8")) {
            downloadHLS(link);
        } else {
            /* http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    public void downloadHLS(final DownloadLink link) throws Exception {
        /* hls download */
        this.br.getPage(DLLINK);
        final String url_base = this.br.getBaseURL();
        final HlsContainer hlsbest = jd.plugins.decrypter.GenericM3u8Decrypter.findBestVideoByBandwidth(jd.plugins.decrypter.GenericM3u8Decrypter.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String url_hls = hlsbest.downloadurl;
        if (!url_hls.startsWith("http")) {
            url_hls = url_base + url_hls;
        }
        URLConnectionAdapter con = null;
        try {
            con = this.br.openGetConnection(url_hls);
            con.disconnect();
        } catch (final Throwable e) {
        }
        if (con != null && !con.getContentType().equals("application/x-mpegURL")) {
            /* TODO: Check whether this errorhandling does still work / is still needed. */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - stream is not available");
        }
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, url_hls);
        dl.startDownload();
    }

    /* Important login cookie = "_mgtvSession"* */
    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBRAPI(br);
                br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(HOST_MASSENGESCHMACK, cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    /* Check cookies */
                    br.setFollowRedirects(true);
                    apiGetPage(br, API_BASE_URL + API_LIST_SUBSCRIPTIONS);
                    if (isLoggedIn(br)) {
                        /* Refresh timestamp */
                        account.saveCookies(br.getCookies(HOST_MASSENGESCHMACK), "");
                        return;
                    }
                }
                br.setFollowRedirects(true);
                apiGetPage(br, API_BASE_URL + API_LIST_SUBSCRIPTIONS);
                if (!isLoggedIn(br)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(HOST_MASSENGESCHMACK), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    public static void apiGetPage(final Browser br, final String url) throws PluginException, IOException {
        br.getPage(url);
        apiHandleErrors(br);
    }

    public static void apiHandleErrors(final Browser br) throws PluginException {
        final int responsecode = br.getHttpConnection().getResponseCode();
        switch (responsecode) {
        case 200:
            /* Everything okay */
            break;
        case 301:
            /* Everything okay */
            break;
        case 302:
            /* Everything okay */
            break;
        case API_RESPONSECODE_ERROR_CONTENT_OFFLINE:
            // {"api_error":"Clip not found."}
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        case API_RESPONSECODE_ERROR_LOGIN_WRONG:
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        case API_RESPONSECODE_ERROR_RATE_LIMIT_REACHED:
            // {"api_error":"API Rate Limited.", "retryAfter":30}
            long waittime;
            final String retry_seconds_str = getJson(br.toString(), "retryAfter");
            if (retry_seconds_str != null) {
                waittime = Long.parseLong(retry_seconds_str) * 1001l;
            } else {
                waittime = 30 * 1001l;
            }
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API rate limit reached", waittime);
        default:
            /* Unknown response */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        }
    }

    // public static void loginWebsite(final Account account, final Browser br) throws IOException, PluginException {
    // synchronized (LOCK) {
    // br.getPage("https://massengeschmack.tv/index_login.php");
    // br.postPage(br.getURL(), "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
    // if (!hasLoginCookie(br)) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort
    // stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es
    // erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your
    // password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without
    // copy & paste.",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // }
    // }

    public static boolean isLoggedIn(final Browser br) {
        /* Responsecode should always be 200 for API requests */
        final boolean isloggedin = br.getHttpConnection().getResponseCode() != API_RESPONSECODE_ERROR_LOGIN_WRONG && hasLoginCookie(br) && br.getHttpConnection().getContentType().equals("application/json");
        return isloggedin;
    }

    public static boolean hasLoginCookie(final Browser br) {
        final boolean haslogincookie = br.getCookie(HOST_MASSENGESCHMACK, "_mgtvSession") != null;
        return haslogincookie;
    }

    public static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.setAllowedResponseCodes(new int[] { FernsehkritikTv.API_RESPONSECODE_ERROR_CONTENT_OFFLINE, FernsehkritikTv.API_RESPONSECODE_ERROR_LOGIN_WRONG, FernsehkritikTv.API_RESPONSECODE_ERROR_RATE_LIMIT_REACHED });
        return br;
    }

    @SuppressWarnings("deprecation")
    public static String getUrlNameForMassengeschmackGeneral(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9\\-_]+)$").getMatch(0);
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

    @SuppressWarnings("deprecation")
    public String getFKTVFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("fernsehkritik.tv");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_FKTV, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = defaultCustomFilename;
        }
        if (!formattedFilename.contains("*episodenumber*") || !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename;
        }

        final String ext = downloadLink.getStringProperty("directtype", default_EXT);
        final String date = downloadLink.getStringProperty("directdate", null);
        final String episodenumber = downloadLink.getStringProperty("directepisodenumber", null);

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            SimpleDateFormat formatter = new SimpleDateFormat(inputDateformat_1, new Locale("de", "DE"));
            final Date theDate = new Date(getTimestampFromDate(date));

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = default_empty_tag_separation_mark;
                }
            }
            if (formattedDate != null) {
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            } else {
                formattedFilename = formattedFilename.replace("*date*", "");
            }
        }
        if (formattedFilename.contains("*episodenumber*") && episodenumber != null) {
            formattedFilename = formattedFilename.replace("*episodenumber*", episodenumber);
        }
        if (formattedFilename.contains("*date*") && formattedDate != null) {
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);

        return encodeUnicode(formattedFilename);
    }

    /* In case the channel (url_videoid_without_episodenumber) is different from what the website shows we have to manually fix this. */
    private String getVideoidWithoutEpisodenumber(String url_videoid) {
        if (url_videoid == null) {
            return null;
        }
        final String url_episodenumber = new Regex(url_videoid, "(\\d+)$").getMatch(0);
        if (url_videoid.matches("fktvplus\\d+")) {
            url_videoid = "Fernsehkritik-TV Plus";
        } else if (url_videoid.matches("fktv.*?")) {
            url_videoid = "Fernsehkritik-TV";
        } else if (url_videoid.matches("ptv.*?")) {
            url_videoid = "Pantoffel-TV";
        } else if (url_videoid.matches("ps\\d+")) {
            url_videoid = "Pressesch(l)au";
        } else if (url_videoid.matches("studio(?:\\-)?\\d+")) {
            /* 'Das Studio'-Pattern #1 */
            url_videoid = "Das Studio";
        } else if (url_videoid.matches("studio(?:\\-)?p\\d+")) {
            /* 'Das Studio'-Pattern #2 */
            url_videoid = "Das Studio";
        } else {
            /* url_name_without_episodenumber should already be okay - simply remove '-' and that's it. */
            url_videoid = url_videoid.replace("-", "");
            if (!inValidate(url_episodenumber)) {
                url_videoid = url_videoid.substring(0, url_videoid.length() - url_episodenumber.length());
            }
        }
        /* Should start uppercase */
        final String first_char_uppercase = url_videoid.substring(0, 1).toUpperCase();
        url_videoid = first_char_uppercase + url_videoid.substring(1);
        return url_videoid;
    }

    @SuppressWarnings("deprecation")
    public String getMassengeschmack_other_FormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("fernsehkritik.tv");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_MASSENGESCHMACK_OTHER, defaultCustomFilename_massengeschmack_other);
        if ((!formattedFilename.contains("*episodenumber*") && !formattedFilename.contains("*episodename*")) || !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename_massengeschmack_other;
        }

        final String ext = downloadLink.getStringProperty("directtype", default_EXT);
        final String date = downloadLink.getStringProperty("directdate", null);
        final String channel = downloadLink.getStringProperty("directchannel", EMPTY_FILENAME_INFORMATION);
        final String episodename = downloadLink.getStringProperty("directepisodename", EMPTY_FILENAME_INFORMATION);
        final String episodenumber = downloadLink.getStringProperty("directepisodenumber", EMPTY_FILENAME_INFORMATION);

        String formattedDate = EMPTY_FILENAME_INFORMATION;
        if (date != null && formattedFilename.contains("*date*")) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            SimpleDateFormat formatter = new SimpleDateFormat(inputDateformat_1, new Locale("de", "DE"));
            Date dateStr = new Date(getTimestampFromDate(date));

            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat, new Locale("de", "DE"));
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = default_empty_tag_separation_mark;
                }
            }
            if (formattedDate != null) {
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            } else {
                formattedFilename = formattedFilename.replace("*date*", EMPTY_FILENAME_INFORMATION);
            }
        }
        if (episodenumber.equals(EMPTY_FILENAME_INFORMATION)) {
            formattedFilename = formattedFilename.replace("Folge *episodenumber* -", EMPTY_FILENAME_INFORMATION);
        } else if (formattedFilename.contains("*episodenumber*")) {
            formattedFilename = formattedFilename.replace("*episodenumber*", episodenumber);
        }
        if (formattedFilename.contains("*date*") && formattedDate != null) {
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
        }
        if (formattedFilename.contains("*channel*") && channel != null) {
            formattedFilename = formattedFilename.replace("*channel*", channel);
        }
        if (formattedFilename.contains("*episodename*") && channel != null) {
            formattedFilename = formattedFilename.replace("*episodename*", episodename);
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);

        return encodeUnicode(formattedFilename);
    }

    private long getTimestampFromDate(final String date) {
        final long timestamp;
        if (date.matches("\\d{2}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}")) {
            /* E.g. TYPE_MASSENGESCHMACK_LIVE */
            timestamp = TimeFormatter.getMilliSeconds(date, "dd.MM.yy HH:mm", Locale.GERMANY);
        } else if (date.matches("\\d+")) {
            timestamp = Long.parseLong(date) * 1000;
        } else {
            timestamp = TimeFormatter.getMilliSeconds(date, inputDateformat_1, Locale.GERMANY);
        }
        return timestamp;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     */
    public static String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        // output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public String getDescription() {
        return "JDownloader's Fernsehkritik Plugin kann Videos von fernsehkritik.tv und massengeschmack.tv herunterladen. Hier kann man eigene Dateinamen definieren und (als Massengeschmack Abonnent) die herunterzuladenden Videoformate wählen.";
    }

    private final static String defaultCustomFilename                       = "Fernsehkritik-TV Folge *episodenumber* vom *date**ext*";
    private final static String defaultCustomFilename_massengeschmack_other = "*date*_*channel*_Folge *episodenumber* - *episodename**ext*";
    private final static String defaultCustomPackagename                    = "Fernsehkritik.tv Folge *episodenumber* vom *date*";
    private final static String defaultCustomDate                           = "yyyy-MM-dd";
    private static final String default_empty_tag_separation_mark           = "-";
    private static final String inputDateformat_1                           = "dd. MMMMM yyyy";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Allgemeine Einstellungen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_POSTECKE, JDL.L("plugins.hoster.fernsehkritik.grabpostecke", "Beim Hinzufügen von Fktv Episoden:\r\nFüge 'Postecke'/'Massengeschmack Direkt' zu Fktv Episoden ein, falls verfügbar?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, JDL.L("plugins.hoster.fernsehkritik.fastLinkcheck", "Aktiviere schnellen Linkcheck (Dateigröße erst bei Download sichtbar)?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Lege eigene Datei-/Paketnamen fest:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.fernsehkritiktv.customdate", "Definiere das Datumsformat:")).setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Setze eigene Dateinamen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Eigene Dateinamen für Fktv Episoden:!\r\nBeispiel: 'Fernsehkritik-TV Folge *episodenumber* vom *date**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_FKTV, JDL.L("plugins.hoster.fernsehkritiktv.customfilename", "Definiere das Muster der eigenen Dateinamen:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Erklärung der verfügbaren Tags:\r\n");
        sb.append("*episodenumber* = Nummer der Fktv Episode\r\n");
        sb.append("*date* = Erscheinungsdatum der Fktv Episode - Erscheint im oben festgelegten Format\r\n");
        sb.append("*ext* = Dateiendung - meistens '.flv'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Lege eigene Dateinamen für alle anderen Massengeschmack Links fest!\r\nBeispiel: '*channel* Episode *episodenumber* vom *date**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_MASSENGESCHMACK_OTHER, JDL.L("plugins.hoster.fernsehkritiktv.customfilename_massengeschmack", "Definiere das Muster der eigenen Dateinamen:")).setDefaultValue(defaultCustomFilename_massengeschmack_other));
        final StringBuilder sb_other = new StringBuilder();
        sb_other.append("Erklärung der verfügbaren Tags:\r\n");
        sb_other.append("*channel* = Name der Serie/Channel\r\n");
        sb_other.append("*episodename* = Name der Episode\r\n");
        sb_other.append("*episodenumber* = Nummer der Episode\r\n");
        sb_other.append("*date* = Erscheinungsdatum der Episode - Erscheint im oben festgelegten Format\r\n");
        sb_other.append("*ext* = Dateiendung - meistens '.flv'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb_other.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Lege eigene Paketnamen fest!\r\nBeispiel: 'Fernsehkritik.tv Folge *episodenumber* vom *date*':"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME, JDL.L("plugins.hoster.fernsehkritiktv.custompackagename", "Lege das Muster der eigenen Paketnamen fest:")).setDefaultValue(defaultCustomPackagename));
        final StringBuilder sbpack = new StringBuilder();
        sbpack.append("Erklärung der verfügbaren Tags:\r\n");
        sbpack.append("*episodenumber* = Nummer der Episode\r\n");
        sbpack.append("*date* = Erscheinungsdatum der Episode - Erscheint im oben festgelegten Format");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbpack.toString()));
    }
}