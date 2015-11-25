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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Cookie;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fernsehkritik.tv", "massengeschmack.tv" }, urls = { "http://(?:www\\.)?fernsehkritik\\.tv/jdownloaderfolgeneu?\\d+", "https?://(?:www\\.)?massengeschmack\\.tv/play/\\d+/[a-z0-9\\-]+|https?://(?:www\\.)?massengeschmack\\.tv/live/play/[a-z0-9\\-]+|https?://massengeschmack\\.tv/dl/.+" }, flags = { 2, 2 })
public class FernsehkritikTv extends PluginForHost {

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
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    private static final String TYPE_FOLGE_NEW                        = "http://fernsehkritik\\.tv/jdownloaderfolgeneu\\d+";
    private static final String TYPE_MASSENGESCHMACK_GENERAL          = "https?://(?:www\\.)?massengeschmack\\.tv/play/\\d+/[a-z0-9\\-]+";
    private static final String TYPE_MASSENGESCHMACK_LIVE             = "https?://(?:www\\.)?massengeschmack\\.tv/live/play/[a-z0-9\\-]+";
    private static final String TYPE_MASSENGESCHMACK_DIRECT_PREMIUM   = "https?://massengeschmack\\.tv/dl/.+";

    public static final String  HTML_MASSENGESCHMACK_OFFLINE          = ">Clip nicht gefunden";
    private static final String HTML_MASSENGESCHMACK_PREMIUMONLY      = ">Clip nicht kostenlos verfügbar|Dieser Livestream benötigt ein Abo";

    private static final String MSG_PREMIUMONLY                       = "Nur für Massengeschmack Abonenten herunterladbar";

    private static final String HOST_MASSENGESCHMACK                  = "http://massengeschmack.tv";
    private static final String HTML_LOGIN_ERROR                      = "class=\"alert alert\\-error\"";
    private static final String HTML_NO_FREE_VERSION_FOUND            = ">Keine kostenlose Version gefunden";
    private static final String GRAB_POSTECKE                         = "GRAB_POSTECKE";
    private static final String CUSTOM_DATE                           = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME_FKTV                  = "CUSTOM_FILENAME_FKTV_2";
    private static final String CUSTOM_FILENAME_MASSENGESCHMACK_OTHER = "CUSTOM_FILENAME_MASSENGESCHMACK_OTHER_3";
    private static final String CUSTOM_PACKAGENAME                    = "CUSTOM_PACKAGENAME";
    private static final String FASTLINKCHECK                         = "FASTLINKCHECK";

    private static Object       LOCK                                  = new Object();
    private String              DLLINK                                = null;
    private boolean             LOGGEDIN                              = false;

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

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink, final Account account) throws Exception {
        DLLINK = null;
        String filesize_string = null;
        long filesize = -1;
        String final_filename = null;
        if (account != null) {
            try {
                login(account, false);
                LOGGEDIN = true;
            } catch (final Throwable e) {
            }
        }
        if (downloadLink.getDownloadURL().matches(TYPE_FOLGE_NEW)) {
            DLLINK = downloadLink.getStringProperty("directlink", null);
            if (DLLINK == null) {
                br.getPage(downloadLink.getStringProperty("mainlink", null));
                /* This case is nearly impossible */
                if (br.containsHTML(HTML_MASSENGESCHMACK_OFFLINE)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.containsHTML(HTML_MASSENGESCHMACK_PREMIUMONLY)) {
                    downloadLink.getLinkStatus().setStatusText("Zurzeit nur für Massengeschmack Abonenten herunterladbar");
                    return AvailableStatus.TRUE;
                }
                DLLINK = br.getRegex("type=\"video/mp4\" src=\"(http://[^<>\"]*?\\.mp4)\"").getMatch(0);
            }
            final_filename = getFKTVFormattedFilename(downloadLink);
        } else if (downloadLink.getDownloadURL().matches(TYPE_MASSENGESCHMACK_GENERAL) || downloadLink.getDownloadURL().matches(TYPE_MASSENGESCHMACK_LIVE)) {
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML(HTML_MASSENGESCHMACK_OFFLINE) || this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(HTML_NO_FREE_VERSION_FOUND) || this.br.containsHTML(HTML_MASSENGESCHMACK_PREMIUMONLY)) {
                downloadLink.getLinkStatus().setStatusText("No free downloadable version available");
                downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "([a-z0-9\\-]+)$").getMatch(0));
                return AvailableStatus.TRUE;
            }
            final String episodenumber;
            String channel;
            String episodename;
            final String date;
            if (downloadLink.getDownloadURL().matches(TYPE_MASSENGESCHMACK_LIVE)) {
                /* Get m3u8 main playlist */
                DLLINK = br.getRegex("\"(https?://dl\\.massengeschmack\\.tv/live/[^<>\"]*?adaptive\\.m3u8)\"").getMatch(0);
                final Regex info = this.br.getRegex("<h3>([^<>\"]*?) ?<small>([^<>\"]*?)</small>");
                channel = info.getMatch(0);
                episodename = info.getMatch(1);
                if (episodename == null) {
                    /* Fallback to url */
                    episodename = new Regex(downloadLink.getDownloadURL(), "([a-z0-9\\-]+)$").getMatch(0);
                }
                episodenumber = new Regex(episodename, "(\\d+)$").getMatch(0);
                if (episodenumber != null) {
                    /* Remove episodenumber from episodename! */
                    episodename = episodename.substring(0, episodename.length() - episodenumber.length());
                }
                /* Get date without time */
                date = br.getRegex("<p class=\"muted\">(\\d{2}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2})[^<>\"]+<").getMatch(0);
            } else {
                /* Prefer download, highest quality (usually not available for free users) */
                DLLINK = br.getRegex("\"(/dl/[^<>\"]*?\\.mp4[^<>\"/]*?)\"").getMatch(0);
                if (DLLINK != null) {
                    filesize_string = this.br.getRegex(DLLINK + ".*?class=\"label\">~(\\d+(?:\\.\\d+)? (?:MB|GB))<").getMatch(0);
                    DLLINK = "http://massengeschmack.tv" + DLLINK;
                }
                /* Sometimes different qualities are available - prefer webm, highest quality */
                if (DLLINK == null) {
                    DLLINK = br.getRegex("type=\"video/webm\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
                }
                /* Nothing there? Download stream! */
                if (DLLINK == null) {
                    getMassengeschmackDLLINK();
                }
                channel = br.getRegex("<li><a href=\"/u/\\d+\">([^<>\"]*?)</a> <span class=\"divider\"").getMatch(0);
                episodename = br.getRegex("<li class=\"active\">([^<>]*?)<").getMatch(0);
                /* Get date without time */
                date = br.getRegex("<p class=\"muted\">([^<>\"]*?) /[^<]+</p>").getMatch(0);
                episodenumber = new Regex(episodename, "Folge (\\d+)").getMatch(0);
            }
            if (channel == null || episodename == null || date == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext.equals(".m3u8")) {
                ext = ".mp4";
            }

            channel = Encoding.htmlDecode(channel).trim();
            episodename = Encoding.htmlDecode(episodename).trim();
            downloadLink.setProperty("directchannel", channel);
            downloadLink.setProperty("directdate", date);
            downloadLink.setProperty("directepisodename", episodename);
            downloadLink.setProperty("directepisodenumber", episodenumber);
            downloadLink.setProperty("directtype", ext);

            final_filename = getMassengeschmack_other_FormattedFilename(downloadLink);
        } else if (downloadLink.getDownloadURL().matches(TYPE_MASSENGESCHMACK_DIRECT_PREMIUM)) {
            final_filename = new Regex(downloadLink.getDownloadURL(), "/([^/]+)$").getMatch(0);
            if (final_filename == null) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DLLINK = downloadLink.getDownloadURL();
            if (!this.LOGGEDIN) {
                /* We know that these urls should be online! */
                downloadLink.getLinkStatus().setStatusText("Accound benötigt um Verfügbarkeit dieses Links zu überprüfen");
                return AvailableStatus.TRUE;
            }
        } else {
            downloadLink.getLinkStatus().setStatusText("Unknown linkformat");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        downloadLink.setFinalFileName(final_filename);

        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (filesize_string != null) {
            filesize = SizeFormatter.getSize(filesize_string);
        }

        if (filesize < 0 && !DLLINK.contains(".m3u8")) {
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }

        downloadLink.setDownloadSize(filesize);

        return AvailableStatus.TRUE;
    }

    private void getMassengeschmackDLLINK() throws PluginException {
        final String base = br.getRegex("var base = \\'(http://[^<>\"]*?)\\';").getMatch(0);
        final String link = br.getRegex("playlist = \\[\\{url: base \\+ \\'([^<>\"]*?)\\'").getMatch(0);
        if (base == null || link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = base + link;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (!(account.getUser().matches(".+@.+"))) {
            ai.setStatus("Bitte trage deine E-Mail Adresse ins 'Benutzername' Feld ein!");
            account.setValid(false);
            return ai;
        }
        login(account, true);
        br.getPage("/u/");
        if (br.containsHTML("<td>\\(0 Tage\\)</td>|Keine gebuchten Magazine")) {
            /* Free accounts nearly no advantages */
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        } else {
            this.br.getPage("/u/renew.php");
            String expire = br.getRegex("bis zum: ([^<>\"]*?)<").getMatch(0);
            if (expire == null) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MMMM yyyy", Locale.GERMANY));
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        final AvailableStatus availStatus = requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(TYPE_FOLGE_NEW) && br.containsHTML(HTML_MASSENGESCHMACK_PREMIUMONLY)) {
            /* User added a current fernsehkritik episode which is not yet available for free. */
            final String date = downloadLink.getStringProperty("directdate", null);
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
        if (AvailableStatus.UNCHECKABLE.equals(availStatus)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (DLLINK == null && (br.containsHTML(HTML_NO_FREE_VERSION_FOUND) || this.br.containsHTML(HTML_MASSENGESCHMACK_PREMIUMONLY)) || DLLINK.contains(".m3u8")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        br.setFollowRedirects(false);
        /* More chunks work but download will stop at random point then */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unbekannter Serverfehler", 30 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        if (br.containsHTML(HTML_NO_FREE_VERSION_FOUND)) {
            /*
             * Different accounts have different shows - so a premium account must not necessarily have the rights to view/download
             * everything!
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.setFollowRedirects(true);
        if (DLLINK.contains(".m3u8")) {
            /* hls download */
            this.br.getPage(DLLINK);
            DLLINK = null;
            final String[] medias = this.br.getRegex("#EXT-X-STREAM-INF([^\r\n]+[\r\n]+[^\r\n]+)").getColumn(-1);
            if (medias == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String url_hls = null;
            long bandwidth_highest = 0;
            for (final String media : medias) {
                // name = quality
                // final String quality = new Regex(media, "NAME=\"(.*?)\"").getMatch(0);
                final String bw = new Regex(media, "BANDWIDTH=(\\d+)").getMatch(0);
                final long bandwidth_temp = Long.parseLong(bw);
                if (bandwidth_temp > bandwidth_highest) {
                    bandwidth_highest = bandwidth_temp;
                    url_hls = new Regex(media, "([^/\n\t\r]+\\.m3u8)").getMatch(0);
                }
            }
            if (url_hls == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            url_hls = this.br.getBaseURL() + url_hls;
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, url_hls);
            dl.startDownload();
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

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.getHeaders().put("Accept-Language", "de-de,de;q=0.8");
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(HOST_MASSENGESCHMACK, key, value);
                        }
                        return;
                    }
                }
                br.getHeaders().put("Accept-Encoding", "gzip");
                br.setFollowRedirects(true);
                br.getPage(HOST_MASSENGESCHMACK + "/login/");
                br.postPage("/login/", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML(HTML_LOGIN_ERROR)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(HOST_MASSENGESCHMACK);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            } finally {
                /* E.g. needed to download direct urls */
                br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
            }
        }
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

        final String ext = downloadLink.getStringProperty("directtype", null);
        final String date = downloadLink.getStringProperty("directdate", null);
        final String episodenumber = downloadLink.getStringProperty("directepisodenumber", null);

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            SimpleDateFormat formatter = new SimpleDateFormat(inputDateformat_1, new Locale("de", "DE"));
            // Date dateStr = formatter.parse(date);
            //
            // formattedDate = formatter.format(dateStr);
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

    @SuppressWarnings("deprecation")
    public String getMassengeschmack_other_FormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("fernsehkritik.tv");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_MASSENGESCHMACK_OTHER, defaultCustomFilename_massengeschmack_other);
        if ((!formattedFilename.contains("*episodenumber*") && !formattedFilename.contains("*episodename*")) || !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename_massengeschmack_other;
        }

        final String ext = downloadLink.getStringProperty("directtype", null);
        final String date = downloadLink.getStringProperty("directdate", null);
        final String channel = downloadLink.getStringProperty("directchannel", null);
        final String episodename = downloadLink.getStringProperty("directepisodename", null);
        final String episodenumber = downloadLink.getStringProperty("directepisodenumber", "-");

        String formattedDate = null;
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
                formattedFilename = formattedFilename.replace("*date*", "");
            }
        }
        if (formattedFilename.contains("*episodenumber*") && episodenumber != null) {
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
        } else {
            timestamp = TimeFormatter.getMilliSeconds(date, inputDateformat_1, Locale.GERMANY);
        }
        return timestamp;
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
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public String getDescription() {
        return "JDownloader's Fernsehkritik Plugin kann Videos von fernsehkritik.tv und massengeschmack.tv herunterladen. Hier kann man eigene Dateinamen definieren und (als Massengeschmack Abonnent) die herunterzuladenden Videoformate wählen.";
    }

    private final static String defaultCustomFilename                       = "Fernsehkritik-TV Folge *episodenumber* vom *date**ext*";
    private final static String defaultCustomFilename_massengeschmack_other = "*channel* *episodename* vom *date**ext*";
    private final static String defaultCustomPackagename                    = "Fernsehkritik.tv Folge *episodenumber* vom *date*";
    private final static String defaultCustomDate                           = "dd MMMMM yyyy";
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