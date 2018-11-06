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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.VariantInfoMassengeschmackTv;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "massengeschmack.tv" }, urls = { "https?://massengeschmack\\.tv/dl.+|https?://[^/]+\\.massengeschmack\\.tv/deliver.+" })
public class MassengeschmackTv extends PluginForHost {
    public static final long trust_cookie_age = 300000l;

    /* 2016-12-31: Officially their (API) cookie is only valid for max 30 minutes! */
    // public static final long max_cookie_age = 100800l;
    public MassengeschmackTv(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://massengeschmack.tv/register/");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://massengeschmack.tv/AGB";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public static final String  TYPE_MASSENGESCHMACK_GENERAL              = ".+massengeschmack\\.tv/play/[a-z0-9\\-]+";
    public static final String  TYPE_MASSENGESCHMACK_LIVE                 = ".+massengeschmack\\.tv/live/[a-z0-9\\-]+";
    public static final String  TYPE_MASSENGESCHMACK_DIRECT               = ".+massengeschmack\\.tv/dl/.+";
    public static final String  HTML_MASSENGESCHMACK_OFFLINE              = ">Clip nicht gefunden|>Error 404<|>Die angeforderte Seite wurde nicht gefunden";
    private static final String HTML_MASSENGESCHMACK_CLIP_PREMIUMONLY     = ">Clip nicht kostenlos verfügbar";
    /* API stuff */
    public static final boolean VIDEOS_ENABLE_API                         = true;
    public static final String  API_BASE_URL                              = "https://massengeschmack.tv/api/v1/";
    public static final String  API_LIST_SUBSCRIPTIONS                    = "?action=listSubscriptions";
    public static final String  API_GET_CLIP                              = "?action=getClip&identifier=%s";
    public static final int     API_RESPONSECODE_ERROR_CONTENT_OFFLINE    = 400;
    public static final int     API_RESPONSECODE_ERROR_LOGIN_WRONG        = 401;
    public static final int     API_RESPONSECODE_ERROR_RATE_LIMIT_REACHED = 500;
    private static final String HOST_MASSENGESCHMACK                      = "massengeschmack.tv";
    private static final String PREFER_MP4                                = "PREFER_MP4";
    private static final String CUSTOM_DATE                               = "CUSTOM_DATE_2";
    private static final String CUSTOM_FILENAME                           = "CUSTOM_FILENAME_2018_10";
    private static final String CUSTOM_PACKAGENAME                        = "CUSTOM_PACKAGENAME";
    private static final String FASTLINKCHECK                             = "FASTLINKCHECK";
    private static final String EMPTY_FILENAME_INFORMATION                = "-";
    public static final String  default_EXT                               = ".mp4";
    private static Object       LOCK                                      = new Object();
    private String              dllink                                    = null;
    private boolean             is_premiumonly_content                    = false;

    /**
     * Using API wherever possible:<br />
     * <a href="https://massengeschmack.tv/api/doc.php">massengeschmack.tv API documentation</a><br />
     * Another way to easily get the BEST(WORST) file/rendition of a video: <br />
     * https://massengeschmack.tv/dlr/<ClipID>/(best|mobile).mp4<br />
     * Example: https://massengeschmack.tv/dlr/sakura-1/best.mp4<br />
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /* TODO: Improve this case because at the moment, such URLs are not downloadable at all! */
        this.is_premiumonly_content = downloadLink.getBooleanProperty("premiumonly", false);
        /* URLs added until revision 39587 are not compatible with the current revision anymore */
        if (!new Regex(downloadLink.getPluginPatternMatcher(), this.getSupportedLinks()).matches()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* TODO: Check for expiring direct-urls! */
        dllink = downloadLink.getPluginPatternMatcher();
        if (dllink.contains(".m3u8") || dllink.contains(".mp4")) {
            downloadLink.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        }
        this.br.setFollowRedirects(true);
        if (!dllink.contains(".m3u8") && !this.is_premiumonly_content) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    if (downloadLink.getFinalFileName() == null) {
                        /* E.g. user added direct-URLs directly, without decrypter. */
                        downloadLink.setFinalFileName(getFileNameFromHeader(con));
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    // /* Variant stuff */
    // public static void setVariant(final DownloadLink link, final DailyMotionVariant v) {
    // link.setVariant(v);
    // link.setLinkID(link.getPluginPatternMatcher() + "." + v._getUniqueId());
    // }
    //
    // /* Test */
    // public static void setVariant(final DownloadLink link, final VariantInfoMassengeschmackTv v) {
    // final String filesize = v.getFilesize();
    // link.setVariant(v);
    // link.setLinkID(link.getPluginPatternMatcher() + "." + v._getUniqueId());
    // if (filesize != null) {
    // link.setDownloadSize(SizeFormatter.getSize(filesize));
    // }
    // final String fileName = getMassengeschmack_other_FormattedFilename(link, v);
    // link.setFinalFileName(fileName);
    // }
    //
    // @Override
    // public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
    // if (variant != null && variant instanceof VariantInfoMassengeschmackTv) {
    // MassengeschmackTv.setVariant(downloadLink, (VariantInfoMassengeschmackTv) variant);
    // } else if (variant != null) {
    // super.setActiveVariantByLink(downloadLink, variant);
    // }
    // }
    //
    // @Override
    // public List<? extends LinkVariant> getVariantsByLink(DownloadLink downloadLink) {
    // if (downloadLink.isGenericVariantSupport()) {
    // return super.getVariantsByLink(downloadLink);
    // }
    // return downloadLink.getVariants(VariantInfoMassengeschmackTv.class);
    // }
    //
    // @Override
    // public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
    // if (downloadLink.isGenericVariantSupport()) {
    // return super.getActiveVariantByLink(downloadLink);
    // }
    // return downloadLink.getVariant(VariantInfoMassengeschmackTv.class);
    // }
    //
    // /**
    // * @param variantInfos
    // */
    // public static void sortVariants(final List<VariantInfoMassengeschmackTv> variantInfos) {
    // if (variantInfos != null) {
    // for (final VariantInfoMassengeschmackTv v : variantInfos) {
    // v.setUrl(null);
    // }
    // if (variantInfos.size() > 1) {
    // Collections.sort(variantInfos, new Comparator<VariantInfoMassengeschmackTv>() {
    // public int compare(long x, long y) {
    // return (x < y) ? -1 : ((x == y) ? 0 : 1);
    // }
    //
    // @Override
    // public int compare(VariantInfoMassengeschmackTv o1, VariantInfoMassengeschmackTv o2) {
    // return compare(o2.getFilesizeLong(), o1.getFilesizeLong());
    // }
    // });
    // }
    // }
    // }
    public static String getFilenameLastChance(final String dllink, final String url_videoid) {
        String ext = getFileNameExtensionFromString(dllink);
        if (ext == null) {
            /* This should never happen */
            ext = ".mp4";
        }
        String filename = getFilenameFromFinalDownloadlink(dllink);
        if (StringUtils.isEmpty(filename)) {
            /* Okay finally fallback to url-filename */
            filename = url_videoid + ext;
        }
        return filename;
    }

    public static String getEpisodenumberFromVideoid(final String url_videoid) {
        if (StringUtils.isEmpty(url_videoid)) {
            return null;
        }
        final String episodenumber = new Regex(url_videoid, "(\\d+)$").getMatch(0);
        return episodenumber;
    }

    public static String getFilenameFromFinalDownloadlink(final String dllink) {
        String filename_directlink = null;
        if (!StringUtils.isEmpty(dllink)) {
            filename_directlink = new Regex(dllink, "/([^/]+\\.(?:flv|mp4|mkv|webm|mov))$").getMatch(0);
        }
        /* Make sure that we actually get a filename ... */
        if (!StringUtils.isEmpty(filename_directlink) && filename_directlink.equals("best.mp4")) {
            filename_directlink = null;
        }
        return filename_directlink;
    }

    @SuppressWarnings({ "unchecked" })
    public static String getStreamDllinkMassengeschmackWebsite(final Browser br) {
        /* First try hls/mp4. */
        String dllink = br.getRegex("type=\"application/x\\-mpegurl\" src=\"(http://[^<>\"]*?\\.m3u8)\"").getMatch(0);
        /*
         * 2016-04-14: This must be their new way of storing streams in HTML - works for massengeschmack.tv- and fernsehkritik.tv episodes.
         */
        final String stream_json = br.getRegex("MEDIA[\t\n\r ]*?=[\t\n\r ]*?(\\[.*?\\])").getMatch(0);
        if (StringUtils.isEmpty(dllink)) {
            String dllink_mp4 = null;
            String dllink_webm = null;
            if (!StringUtils.isEmpty(stream_json)) {
                try {
                    String dllink_temp = null;
                    HashMap<String, Object> entries = null;
                    final ArrayList<Object> videoressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(stream_json);
                    for (final Object videoo : videoressourcelist) {
                        entries = (HashMap<String, Object>) videoo;
                        dllink_temp = (String) entries.get("src");
                        if (!StringUtils.isEmpty(dllink_temp) && StringUtils.isEmpty(dllink_webm) && dllink_temp.contains(".webm")) {
                            dllink_webm = dllink_temp;
                        } else if (!StringUtils.isEmpty(dllink_temp) && StringUtils.isEmpty(dllink_mp4) && dllink_temp.contains(".mp4")) {
                            dllink_mp4 = dllink_temp;
                        } else {
                            // logger.info("Skipping unknown quality or we already have that one: " + dllink_temp);
                        }
                    }
                } catch (final Throwable e) {
                }
            }
            /*
             * 2016-04-14: This is probably old: Sometimes different http qualities are available - prefer webm, highest quality, slightly
             * better than mp4.
             */
            if (StringUtils.isEmpty(dllink_webm)) {
                dllink_webm = br.getRegex("type=\"video/webm\" src=\"(http://[^<>\"]*?\\.webm)\"").getMatch(0);
            }
            /* No luck? Try mp4. */
            if (StringUtils.isEmpty(dllink_mp4)) {
                dllink_mp4 = br.getRegex("type=\"video/mp4\" src=\"(http://[^<>\"]*?\\.mp4)\"").getMatch(0);
            }
            /* No luck? Try wider RegEx. */
            if (StringUtils.isEmpty(dllink_mp4)) {
                dllink_mp4 = br.getRegex("(/dl/[^<>\"]*?\\.mp4[^<>\"/]*?)\"").getMatch(0);
            }
            /* Nothing there? Try to download stream! */
            if (StringUtils.isEmpty(dllink_mp4)) {
                final String base = br.getRegex("var base = \\'(http://[^<>\"]*?)\\';").getMatch(0);
                final String link = br.getRegex("playlist = \\[\\{url: base \\+ \\'([^<>\"]*?)\\'").getMatch(0);
                if (base != null && link != null) {
                    dllink_mp4 = base + link;
                }
            }
            dllink = getSelectedFormat(dllink_mp4, dllink_webm);
            /* Fix DLLINK if needed */
            if (!StringUtils.isEmpty(dllink) && !dllink.startsWith("http")) {
                dllink = "http://" + HOST_MASSENGESCHMACK + dllink;
            }
        }
        return dllink;
    }

    public static String getSelectedFormat(final String dllink_mp4, final String dllink_webm) {
        String dllink = null;
        /* Finally decide which format we want to have in case we have a choice. */
        if (preferMP4()) {
            dllink = dllink_mp4;
        }
        /* No luck? We don't care which format the user selected! */
        if (StringUtils.isEmpty(dllink)) {
            dllink = dllink_webm;
        }
        if (StringUtils.isEmpty(dllink)) {
            /* Last chance */
            dllink = dllink_mp4;
        }
        return dllink;
    }

    /** https://massengeschmack.tv/dlr/[url_videoid]/[best|mobile|hd].mp4 */
    public static String getBESTDllinkSpecialAPICall(final String url_videoid) {
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
            ai.setStatus("Bitte gib deine E-Mail Adresse ins 'Benutzername' Feld ein!");
            account.setValid(false);
            return ai;
        }
        long expirelong = -1;
        login(this.br, account, true);
        /* API does not tell us account type, expire date or anything else account related so we have to go via website ... */
        br.getPage("https://" + this.br.getHost() + "/u/");
        boolean hasActiveSubscription = this.br.containsHTML(">Abonnement aktiv");
        boolean isPremium = this.br.containsHTML("Zugang bis \\d{1,2}\\. (?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember) 20\\d{2}");
        boolean isExpired = false;
        br.getPage("/account/");
        if (!hasActiveSubscription) {
            /* Fallback */
            hasActiveSubscription = this.br.containsHTML("<h5 [^>]*?><span [^>]*?>Automatische Verlängerung:</span>\\s*?AKTIV\\s*?</h5>");
        }
        /* First try - this RegEx will only work if the users' subscription is still active and he pays after this date. */
        String expire = br.getRegex("Zahlung am (\\d{1,2}\\. (?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember) 20\\d{2})").getMatch(0);
        if (expire != null) {
            hasActiveSubscription = true;
        } else {
            /* Second try - this RegEx will work if the user does still have premium right now but his subscription ends after that date. */
            expire = br.getRegex("Zugang bis (\\d{1,2}\\. (?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember) 20\\d{2})").getMatch(0);
        }
        if (expire == null) {
            /* Let's go full risc */
            expire = br.getRegex("(\\d{1,2}\\. (?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember) 20\\d{2})").getMatch(0);
        }
        if (expire != null) {
            /* Expiredate available --> We should have a premium account! */
            isPremium = true;
            expirelong = TimeFormatter.getMilliSeconds(expire, "dd. MMMM yyyy", Locale.GERMANY);
            ai.setValidUntil(expirelong);
            if (ai.isExpired()) {
                isExpired = true;
            }
        }
        if (!isExpired && (isPremium || hasActiveSubscription)) {
            /* Expiredate has already been set via code above. */
            account.setType(AccountType.PREMIUM);
            if (hasActiveSubscription) {
                ai.setStatus("Premium Account (Automatische Verlängerung aktiv)");
            } else {
                ai.setStatus("Premium Account (Keine automatische Verlängerung)");
            }
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Kostenloser Account (Kein Abonnement aktiv)");
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (inValidate(dllink)) {
            /* 2018-07-19: Keep it simple: No downloadlink --> Probably premiumonly content! */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (dllink.contains(".m3u8")) {
            downloadHLS(link);
        } else {
            /* More chunks work but download will stop at random point! */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                handleKnownServerResponsecodes(dl.getConnection().getResponseCode());
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unbekannter Serverfehler", 30 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        /*
         * This may not necessarily mean that it is only for premium. Sometimes a free account might be enough to download such content e.g.
         * https://massengeschmack.tv/play/fktv148
         */
        if (this.is_premiumonly_content) {
            /*
             * Different accounts have different shows - so a premium account must not necessarily have the rights to view/download
             * everything!
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (inValidate(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.setFollowRedirects(true);
        if (dllink.contains(".m3u8")) {
            downloadHLS(link);
        } else {
            /* http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            handleKnownServerResponsecodes(dl.getConnection().getResponseCode());
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unbekannter Serverfehler", 30 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    private void handleKnownServerResponsecodes(final long responsecode) throws PluginException {
        switch ((int) responsecode) {
        case 403:
            /* Premiumonly */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        case 404:
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        default:
            break;
        }
    }

    public void downloadHLS(final DownloadLink link) throws Exception {
        /* hls download */
        this.br.getPage(dllink);
        if (br.getHttpConnection().getResponseCode() == 403) {
            /* Premiumonly */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        final String url_base = this.br.getBaseURL();
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String url_hls = hlsbest.getDownloadurl();
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

    public static boolean preferMP4() {
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("massengeschmack.tv");
        return hosterPlugin.getPluginConfig().getBooleanProperty(PREFER_MP4, defaultPREFER_MP4);
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
            final String retry_seconds_str = PluginJSonUtils.getJsonValue(br, "retryAfter");
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

    /** Important: Keep this for legacy reasons! */
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
        br.setAllowedResponseCodes(new int[] { MassengeschmackTv.API_RESPONSECODE_ERROR_CONTENT_OFFLINE, MassengeschmackTv.API_RESPONSECODE_ERROR_LOGIN_WRONG, MassengeschmackTv.API_RESPONSECODE_ERROR_RATE_LIMIT_REACHED });
        return br;
    }

    public static String getUrlNameForMassengeschmackGeneral(final String addedURL) {
        return new Regex(addedURL, "([A-Za-z0-9\\-_]+)$").getMatch(0);
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

    /* In case the channel (url_videoid_without_episodenumber) is different from what the website shows we have to manually fix this. */
    public static String getVideoidWithoutEpisodenumber(String url_videoid) {
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
            if (!StringUtils.isEmpty(url_episodenumber)) {
                url_videoid = url_videoid.substring(0, url_videoid.length() - url_episodenumber.length());
            }
        }
        if (url_videoid.length() > 1) {
            /* Should start uppercase */
            final String first_char_uppercase = url_videoid.substring(0, 1).toUpperCase();
            url_videoid = first_char_uppercase + url_videoid.substring(1);
        }
        return url_videoid;
    }

    @SuppressWarnings("deprecation")
    public static String getMassengeschmack_other_FormattedFilename(final DownloadLink downloadLink, final VariantInfoMassengeschmackTv variant) {
        final SubConfiguration cfg = SubConfiguration.getConfig(downloadLink.getHost());
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if ((!formattedFilename.contains("*episodenumber*") && !formattedFilename.contains("*episodename*")) || !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename;
        }
        final String date = downloadLink.getStringProperty("directdate", null);
        final String channel = downloadLink.getStringProperty("directchannel", EMPTY_FILENAME_INFORMATION);
        final String episodename = downloadLink.getStringProperty("directepisodename", EMPTY_FILENAME_INFORMATION);
        final String episodenumber = downloadLink.getStringProperty("directepisodenumber", EMPTY_FILENAME_INFORMATION);
        String qualityName;
        String ext;
        if (variant != null) {
            qualityName = variant.getVariantName();
            if (qualityName == null) {
                /* This should never happen */
                qualityName = EMPTY_FILENAME_INFORMATION;
            }
            ext = getFileNameExtensionFromString(variant.getUrl(), default_EXT);
        } else {
            qualityName = EMPTY_FILENAME_INFORMATION;
            ext = downloadLink.getStringProperty("directtype", default_EXT);
        }
        if (StringUtils.isEmpty(ext)) {
            ext = default_EXT;
        }
        String formattedDate = EMPTY_FILENAME_INFORMATION;
        if (date != null) {
            try {
                final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
                SimpleDateFormat formatter = new SimpleDateFormat(inputDateformat, new Locale("de", "DE"));
                final Date dateStr = new Date(getTimestampFromDate(date));
                formattedDate = formatter.format(dateStr);
                final Date theDate = formatter.parse(formattedDate);
                if (userDefinedDateFormat != null) {
                    formatter = new SimpleDateFormat(userDefinedDateFormat, new Locale("de", "DE"));
                    formattedDate = formatter.format(theDate);
                }
                if (formattedDate != null) {
                    formattedFilename = formattedFilename.replace("*date*", formattedDate);
                } else {
                    formattedFilename = formattedFilename.replace("*date*", EMPTY_FILENAME_INFORMATION);
                }
            } catch (Exception e) {
                // prevent user error killing plugin.
                formattedDate = default_empty_tag_separation_mark;
            }
        }
        if (episodenumber.equals(EMPTY_FILENAME_INFORMATION)) {
            formattedFilename = formattedFilename.replace("Folge *episodenumber* -", EMPTY_FILENAME_INFORMATION);
        } else if (formattedFilename.contains("*episodenumber*")) {
            formattedFilename = formattedFilename.replace("*episodenumber*", episodenumber);
        }
        if (formattedDate != null) {
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
        }
        formattedFilename = formattedFilename.replace("*quality*", qualityName);
        if (channel != null) {
            formattedFilename = formattedFilename.replace("*channel*", channel);
        }
        if (episodename != null) {
            formattedFilename = formattedFilename.replace("*episodename*", episodename);
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);
        return formattedFilename;
    }

    private static long getTimestampFromDate(final String date) {
        final long timestamp;
        if (date.matches("\\d{2}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}:{2}")) {
            /* E.g. TYPE_MASSENGESCHMACK_LIVE */
            timestamp = TimeFormatter.getMilliSeconds(date, "dd.MM.yy HH:mm", Locale.GERMANY);
        } else if (date.matches("\\d{2}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}")) {
            /* E.g. TYPE_MASSENGESCHMACK_LIVE */
            timestamp = TimeFormatter.getMilliSeconds(date, "dd.MM.yy HH:mm", Locale.GERMANY);
        } else if (date.matches("\\d+")) {
            timestamp = Long.parseLong(date) * 1000;
        } else {
            timestamp = TimeFormatter.getMilliSeconds(date, inputDateformat, Locale.GERMANY);
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

    @Override
    public String getDescription() {
        return "JDownloader's Massengeschmack Plugin kann Videos von massengeschmack.tv herunterladen. Hier kann man eigene Dateinamen definieren und (als Massengeschmack Abonnent) die herunterzuladenden Videoformate wählen.";
    }

    private final static String  defaultCustomFilename             = "*date*_*quality*_*channel*_Folge *episodenumber* - *episodename**ext*";
    private final static String  defaultCustomPackagename          = "Fernsehkritik.tv Folge *episodenumber* vom *date*";
    private final static String  defaultCustomDate                 = "yyyy-MM-dd";
    private static final String  default_empty_tag_separation_mark = "-";
    private static final String  inputDateformat                   = "dd. MMMMM yyyy";
    private static final boolean defaultPREFER_MP4                 = false;
    private static final boolean defaultFASTLINKCHECK              = true;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Allgemeine Einstellungen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, _JDT.T.lit_enable_fast_linkcheck()).setDefaultValue(defaultFASTLINKCHECK));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Format-Einstellungen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_MP4, "Für den kostenlosen massengeschmack.tv Download:\r\nBevorzuge .mp4 Download statt .webm?\r\nBedenke, dass mp4 eine minimal bessere Audio-Bitrate- aber dafür eine niedrigere Video-Bitrate hat!?").setDefaultValue(defaultPREFER_MP4));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "LOAD_BEST", _JDT.T.lit_add_only_the_best_video_quality()).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "LOAD_BEST_OF_SELECTION", _JDT.T.lit_add_only_the_best_video_quality_within_user_selected_formats()).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "LOAD_UNKNOWN", _JDT.T.lit_add_unknown_formats()).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "LOAD_1080p", "Lade 1080p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "LOAD_720p", "Lade 720p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "LOAD_432p", "Lade 432p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "LOAD_AUDIO_m4a", "Lade m4a Audio?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "LOAD_AUDIO_mp3", "Lade mp3 Audio?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Lege eigene Datei-/Paketnamen fest:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, "Definiere das Datumsformat:").setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Lege eigene Dateinamen fest!\r\nBeispiel: '*channel* *quality* Episode *episodenumber* vom *date**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, "Definiere das Muster der eigenen Dateinamen:").setDefaultValue(defaultCustomFilename));
        final StringBuilder sb_other = new StringBuilder();
        sb_other.append("Erklärung der verfügbaren Tags:\r\n");
        sb_other.append("*quality* = Qualität z.B. 'SD432p 768x432'\r\n");
        sb_other.append("*channel* = Name der Serie/Channel\r\n");
        sb_other.append("*episodename* = Name der Episode\r\n");
        sb_other.append("*episodenumber* = Nummer der Episode\r\n");
        sb_other.append("*date* = Erscheinungsdatum der Episode - Erscheint im oben festgelegten Format\r\n");
        sb_other.append("*ext* = Dateiendung - meistens '.flv'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb_other.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Lege eigene Paketnamen fest!\r\nBeispiel: 'Fernsehkritik.tv Folge *episodenumber* vom *date*':"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME, "Lege das Muster der eigenen Paketnamen fest:").setDefaultValue(defaultCustomPackagename));
        final StringBuilder sbpack = new StringBuilder();
        sbpack.append("Erklärung der verfügbaren Tags:\r\n");
        sbpack.append("*episodenumber* = Nummer der Episode\r\n");
        sbpack.append("*date* = Erscheinungsdatum der Episode - Erscheint im oben festgelegten Format");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbpack.toString()));
    }
}