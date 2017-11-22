//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "video2brain.com" }, urls = { "https?://(?:www\\.)?video2brain\\.com/(?:de/tutorial/[a-z0-9\\-]+|en/lessons/[a-z0-9\\-]+|fr/tuto/[a-z0-9\\-]+|es/tutorial/[a-z0-9\\-]+|[a-z]{2}/videos\\-\\d+\\.htm)" })
public class Video2brainCom extends PluginForHost {
    public Video2brainCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.video2brain.com/en/support/faq");
        setConfigElements();
    }

    /* 2016-08-02: video2brain.com/en redirects to lynda.com */
    private static final String[] languages = { "de", "fr", "es" };

    @Override
    public String getAGBLink() {
        return "https://www.video2brain.com/en/imprint";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    /* Connection stuff */
    // private final boolean FREE_RESUME = false;
    // private final int FREE_MAXCHUNKS = 0;
    private final int           FREE_MAXDOWNLOADS    = 20;
    private final boolean       RESUME_RTMP          = false;
    private final boolean       RESUME_HTTP          = true;
    private final int           MAXCHUNKS_HTTP       = 0;
    private final int           ACCOUNT_MAXDOWNLOADS = 20;
    private boolean             premiumonly          = false;
    private boolean             inPremiumMode        = false;
    public static final String  domain               = "video2brain.com";
    // public static final String domain_dummy_education = "video2brain.com_EDUCATION";
    private final String        TYPE_OLD             = "https?://(?:www\\.)?video2brain\\.com/[a-z]{2}/videos\\-\\d+\\.htm";
    public static final String  ADD_ORDERID          = "ADD_ORDERID";
    public static final long    trust_cookie_age     = 300000l;
    public static final boolean defaultADD_ORDERID   = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        premiumonly = false;
        this.setBrowserExclusive();
        this.br = newBrowser(new Browser());
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(this.br, aa);
            } catch (final PluginException e) {
                /* Dont throw if user is in free mode anyways. */
                if (inPremiumMode) {
                    throw e;
                }
            }
        }
        /*
         * 2nd way to get videoinfo if videoid is given:
         * https://www.video2brain.com/de/custom/modules/feedback/feedback_ajax.cfc?method=renderFeedbackFormJSON&type=video&id=19752
         */
        /* 2rd way to get videoinfo if videoid is given: https://www.video2brain.com/de/video-info-19752.xml */
        getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* 404 - standard offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.getURL().matches("https?://(?:www\\.)?video2brain\\.com/[^/]+/[^/]+/[^/]+") && !this.br.getURL().matches(TYPE_OLD)) {
            /*
             * Current url is wrong --> Probably we were redirected because the added URL does not lead us to any downloadable content -->
             * Offline
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        boolean set_final_filename = true;
        final long order_id = link.getLongProperty("order_id", 0);
        final String url_language = getUrlLanguage(link);
        final String productid = getProductID(this.br);
        String videoid = getActiveVideoID(this.br);
        final String url_name;
        if (this.br.getURL().matches(TYPE_OLD)) {
            url_name = new Regex(this.br.getURL(), "(\\d+)\\.htm$").getMatch(0);
            if (videoid == null) {
                /* Fallback to find the videoid but actually this should not be needed but ... just in case! */
                videoid = url_name;
            }
        } else {
            url_name = new Regex(this.br.getURL(), "([A-Za-z0-9\\-]+)$").getMatch(0);
        }
        String title = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        if (title == null) {
            /* Fallback to url filename */
            title = url_name;
        }
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = Encoding.htmlDecode(title.trim());
        String filename = "video2brain";
        if (order_id > 0 && this.getPluginConfig().getBooleanProperty(ADD_ORDERID, defaultADD_ORDERID)) {
            filename += "_" + getFormattedVideoPositionNumber(order_id);
        }
        if (productid != null && videoid != null) {
            filename += "_" + productid + "_" + videoid;
        }
        filename += "_" + url_language;
        if (this.br.containsHTML("class=\"only\\-register\\-player\"") || this.br.containsHTML("class=\"video\\-detail\\-player no\\-access\\-player standard\"")) {
            filename += "_paid_content";
            set_final_filename = false;
            premiumonly = true;
        } else if (this.br.containsHTML("var contentVideo_trailer")) {
            filename += "_trailer";
        } else {
            filename += "_tutorial";
        }
        filename += "_" + title + ".mp4";
        filename = encodeUnicode(filename);
        if (set_final_filename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        inPremiumMode = false;
        if (true) {
            /* Free- and Free Account download not allowed. */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        requestFileInformation(downloadLink);
        handleDownload(downloadLink);
    }

    @SuppressWarnings("deprecation")
    public void handleDownload(final DownloadLink downloadLink) throws Exception {
        if (premiumonly) {
            /* This can even happen to paid users! */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (this.inPremiumMode && !isLoggedIn(this.br)) {
            /* This might happen if user changes password while our login function still trusts the cookies without checking on them. */
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String videoid = getActiveVideoID(this.br);
        final String url_language = getUrlLanguage(downloadLink);
        Browser br2 = this.br.cloneBrowser();
        /* User-Agent is not necessarily needed! */
        br2.getHeaders().put("User-Agent", "iPad");
        boolean http_url_is_okay = false;
        final String html5_http_url_plain = this.br.getRegex("<video src=\\'(http[^<>\"\\']+)\\'").getMatch(0);
        String html5_http_url_full = null;
        final String access_exp = this.br.getRegex("Video\\.access_exp[\t\n\r ]*?=[\t\n\r ]*?(\\d+);").getMatch(0);
        final String access_hash = this.br.getRegex("Video\\.access_hash[\t\n\r ]*?=[\t\n\r ]*?\"([a-f0-9]+)\";").getMatch(0);
        final String token = jd.plugins.hoster.Video2brainCom.getToken(this.br);
        String postData = "";
        if (token != null) {
            postData = "token=" + Encoding.urlEncode(token) + "&";
        }
        try {
            prepareAjaxRequest(br2);
            /* Not necessarily needed */
            getPage(br2, "https://www." + this.getHost() + "/" + url_language + "/custom/modules/feedback/feedback_ajax.cfc?method=renderFeedbackFormJSON&type=video&id=" + videoid);
            postPage(br2, "https://www." + this.getHost() + "/" + url_language + "/custom/modules/cdn/cdn.cfc?method=getSecureTokenJSON", postData + "video_id=" + videoid);
        } catch (final Throwable e) {
        }
        if (html5_http_url_plain != null && access_exp != null && access_hash != null) {
            /* 2016-02-28: TODO: Fix this handling! */
            /* Let's try to build our http url first - we can still fallback to rtmp if this fails! */
            /* They usually only use these urls for Apple devices. */
            try {
                postData += "expire=1&path=" + Encoding.urlEncode(html5_http_url_plain) + "&access_exp=" + access_exp + "&access_hash=" + access_hash;
                /*
                 * E.g. call for officially available downloads:
                 * https://www.video2brain.com/de/custom/modules/product/product_ajax.cfc?method
                 * =renderProductDetailDownloads&product_ids=2853&t=
                 */
                prepareAjaxRequest(br2);
                /* Works fine via GET request too */
                postPage(br2, "https://www." + this.getHost() + "/" + url_language + "/custom/modules/cdn/cdn.cfc?method=getSecureTokenJSON", postData);
                final String final_http_url_token = br2.getRegex("\"([^<>\"\\'\\\\]+)").getMatch(0);
                if (final_http_url_token != null) {
                    html5_http_url_full = html5_http_url_plain + "?" + final_http_url_token;
                    URLConnectionAdapter con = null;
                    try {
                        /* Remove old headers/cookies - not needed! */
                        br2 = newBrowser(new Browser());
                        br2.getHeaders().put("Accept", "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5");
                        br2.getHeaders().put("Accept-Language", "de,en-US;q=0.7,en;q=0.3");
                        try {
                            /* Quickly access url without token (as website does) */
                            con = br2.openGetConnection(html5_http_url_plain);
                        } catch (final Throwable e) {
                        }
                        con = br2.openHeadConnection(html5_http_url_full);
                        if (con.isOK() && !con.getContentType().contains("html")) {
                            http_url_is_okay = true;
                        }
                    } finally {
                        con.disconnect();
                    }
                }
            } catch (final Throwable e) {
            }
        }
        if (http_url_is_okay) {
            /* Prefer http - quality-wise rtmp and http are the same! */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, html5_http_url_full, RESUME_HTTP, MAXCHUNKS_HTTP);
            if (dl.getConnection().getContentType().contains("html")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            }
            dl.startDownload();
        } else {
            /* E.g. https://www.video2brain.com/en/video-info-8581.xml */
            String config_url = this.br.getRegex("configuration:[\t\n\r ]*?\"([^<>\"]*?)\"").getMatch(0);
            if (config_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (config_url.startsWith("trailer")) {
                /* Fix trailer xml url as it is escaped and incomplete. */
                config_url = "/" + url_language + "/" + Encoding.unicodeDecode(config_url);
            }
            getPage(config_url);
            final String rtmpurl = this.br.getRegex("<src>(rtmp[^\n]+)").getMatch(0);
            if (rtmpurl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // final String playpath = new Regex(rtmpurl, "((?:flv|mp4):.+)").getMatch(0);
            try {
                dl = new RTMPDownload(this, downloadLink, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setUrl(rtmpurl);
            // if (playpath != null) {
            // rtmp.setPlayPath(playpath);
            // }
            // rtmp.setApp("");
            rtmp.setFlashVer("WIN 20,0,0,306");
            // rtmp.setSwfUrl("https://www.video2brain.com/en/swf/Video2brainPlayer.swf?v=59");
            rtmp.setResume(RESUME_RTMP);
            ((RTMPDownload) dl).startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    private String getUrlLanguage(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "video2brain\\.com/([^/]+)").getMatch(0);
    }

    private static Object LOCK = new Object();

    public static void login(Browser br, final Account account) throws Exception {
        synchronized (LOCK) {
            try {
                br = newBrowser(br);
                br.setCookiesExclusive(true);
                String account_language = getAccountLanguage(account);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && account_language != null) {
                    br.setCookies(domain, cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    /**
                     * Education users need to access this page first otherwise they will NOT be logged in! <br />
                     * So what do we do? Simply access that url regarding of the user account type.<br />
                     * If the user uses a normal account with E-Mail and password he will get an errormessage similar to this:<br />
                     * "Sie befinden sich außerhalb einer gültigen IP-Range für einen IP-Login. Ihre IP: 1.1.1.1".<br />
                     * This does not matter as he is still loggedin with his logindata via cookies so html code will contain the "logout"
                     * button --> Login should work fine!<br />
                     */
                    getPage(br, "https://www." + domain + "/" + account_language + "/education");
                    if (isLoggedIn(br)) {
                        /* Save new cookie timestamp */
                        account.saveCookies(br.getCookies(domain), "");
                        return;
                    }
                    br = newBrowser(new Browser());
                }
                boolean success = false;
                /**
                 * Each country/language may have individual logins - we could request this in the login mask or we simply go through all of
                 * them.
                 */
                for (final String language : languages) {
                    /* Ignore previously saved given language - if it worked fine we would not have to re-login anyways! */
                    account_language = language;
                    final String lang_uppercase = language.toUpperCase();
                    /* First lets check if maybe the user has VPN education access --> No need to login with logindata! */
                    getPage(br, "https://www." + domain + "/" + language + "/education");
                    /* E.g. errormessage: Sie befinden sich außerhalb einer gültigen IP-Range für einen IP-Login. Ihre IP: 91.49.11.2 */
                    if (br.containsHTML("class=\"notice\\-page\\-msg\"")) {
                        /*
                         * Either EDU user is not connected to his VPN or our user is a normal user who needs username & password to login
                         */
                        /* First thing we can do is to do is make sure user entered a valid E-Mail address in the username field! */
                        if (!account.getUser().matches(".+@.+\\..+")) {
                            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                            }
                        }
                        /* Set this property on account - it might be useful later */
                        account.setProperty("education", false);
                        getPage(br, "https://www." + domain + "/" + language + "/login");
                        Form loginform = br.getFormbyProperty("id", "login_form");
                        if (loginform == null) {
                            loginform = new Form();
                            loginform.setMethod(MethodType.POST);
                        }
                        /* Fix action */
                        if (loginform.getAction() == null || loginform.getAction().isEmpty()) {
                            loginform.setAction("/" + language + "/custom/modules/user/user_ajax.cfc?method=login");
                        }
                        /* Remove trash fields */
                        loginform.remove("v2b_userpassword");
                        final String token = getToken(br);
                        /* Fix token */
                        final InputField tokenfield = loginform.getInputFieldByName("sectoken");
                        if (tokenfield != null) {
                            loginform.remove("sectoken");
                        }
                        if (token != null) {
                            loginform.put("token", Encoding.urlEncode(token));
                        }
                        /* Add logindata */
                        loginform.put("email", Encoding.urlEncode(account.getUser()));
                        loginform.put("password", Encoding.urlEncode(account.getPass()));
                        loginform.put("set_cookie", "true");
                        /* Prepare Headers */
                        prepAjaxHeaders(br);
                        br.submitForm(loginform);
                        /* TODO: Maybe make sure/check if this also works for users of other countries! */
                        final String language_based_cookie = br.getCookie(domain, "V2B_USER_" + lang_uppercase);
                        if (language_based_cookie == null) {
                            continue;
                        }
                        String continue_url = PluginJSonUtils.getJsonValue(br, "url");
                        if (continue_url == null) {
                            /* Small fallback. */
                            continue_url = "/" + language + "/login";
                        }
                        getPage(br, continue_url);
                        success = true;
                        break;
                    } else {
                        /* Set this property on account - it might be useful later */
                        account.setProperty("education", true);
                        success = true;
                        break;
                    }
                }
                if (!success) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(domain), "");
                /* Save used language String to avoid unnecessary server requests for future login requests. */
                account.setProperty("language", account_language);
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private static String getAccountLanguage(final Account acc) {
        return acc.getStringProperty("language", null);
    }

    public static boolean isLoggedIn(final Browser br) {
        return br.containsHTML("user\\-logout\\.htm\"");
    }

    public static void prepAjaxHeaders(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    }

    public static String getToken(final Browser br) {
        /* Usually needed for logín */
        String token = br.getRegex("var[\t\n\r ]*?ajftok[\t\n\r ]*?=[\t\n\r ]*?\"([^<>\"]+)\";").getMatch(0);
        if (token == null) {
            /* Usually needed for all other json requests */
            token = br.getRegex("var[\t\n\r ]*?ajtok[\t\n\r ]*?=[\t\n\r ]*?\"([^<>\"]+)\";").getMatch(0);
        }
        return token;
    }

    private static Browser newBrowser(final Browser br) {
        br.setFollowRedirects(true);
        /* Some unnecessary cookies */
        br.setCookie(domain, "v2babde", "A");
        br.setCookie(domain, "v2babes", "A");
        br.setCookie(domain, "v2babfr", "B");
        br.setCookie(domain, "GA_INIT", "1");
        /* Set higher timeout value as servers are often slow/overloaded */
        br.setReadTimeout(1 * 60 * 1000);
        br.setConnectTimeout(1 * 60 * 1000);
        return br;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        final String accountlanguage = getAccountLanguage(account);
        this.br.getPage("https://www." + this.getHost() + "/" + accountlanguage + "/");
        /* Different url for every language/country */
        final String abonements_url = this.br.getRegex("id=\"myv2b_dd_usertype\">[^<>]*?<a href=\"(https?://(?:www\\.)?video2brain\\.com/[^/]+/my-video2brain/(options|settings)/[^/]+)\"").getMatch(0);
        String statustext = "";
        String abonnement = null;
        String accessGroup = null;
        boolean isPremium = false;
        long validUntil = -1;
        if (abonements_url != null) {
            /* This url should only be available for premium accounts --> Then we can RegEx the expire date. */
            this.br.getPage(abonements_url);
            if (StringUtils.containsIgnoreCase(abonements_url, "mein-abonnement")) {
                abonnement = br.getRegex("<div><b>Abonnement:</b>\\s*(.*?)\\s*</div>").getMatch(0);
                accessGroup = br.getRegex("<div><b>Zugangsgruppe:</b>\\s*(.*?)\\s*</div>").getMatch(0);
                isPremium = StringUtils.containsIgnoreCase(abonnement, "Premium");
                final String expiredate = this.br.getRegex("<div><b>Gültig bis:</b>\\s*([0-9\\. ]+)\\s*</div>").getMatch(0);
                validUntil = TimeFormatter.getMilliSeconds(expiredate, "dd'.'MM'.'yyyy", Locale.GERMAN);
                if (isPremium) {
                    if (accessGroup != null && br.containsHTML("Ihr Benutzerkonto ist mit obigen Konto verbunden")) {
                        // Account ist an eine Gruppe gebunden
                        if (validUntil < 0) {
                            validUntil = 0;
                        }
                    } else if (StringUtils.containsIgnoreCase(abonnement, "test") || StringUtils.containsIgnoreCase(abonnement, "probe")) {
                        if ((validUntil - (10 * 24 * 60 * 60 * 1000l)) < System.currentTimeMillis()) {
                            // we make sure account is valid for more than 10 days
                            isPremium = false;
                        }
                    }
                }
            } else if (StringUtils.containsIgnoreCase(abonements_url, "mes-abonnements")) {
                abonnement = br.getRegex("<div><b>Abonnement:</b>\\s*(.*?)\\s*</div>").getMatch(0);
                isPremium = !StringUtils.containsIgnoreCase(abonnement, "essai");
                final String expiredate = this.br.getRegex("<div><b>Valable jusqu'au:</b>\\s*(.*?)\\s*</div>").getMatch(0);
                validUntil = TimeFormatter.getMilliSeconds(expiredate, "dd MMMM yyyy", Locale.FRENCH);
            }
        }
        if (abonnement != null) {
            if (accessGroup != null) {
                if ("de".equalsIgnoreCase(accountlanguage)) {
                    statustext = abonnement + "(Zugangsgruppe:" + accessGroup + ")";
                } else {
                    statustext = abonnement + "(Group:" + accessGroup + ")";
                }
            } else {
                statustext = abonnement;
            }
        } else {
            statustext = "Free Account";
        }
        /* 2016-11-30: Removed check for "Upgrade" button as it seems to be always available! */
        if (validUntil == -1 || !isPremium) {
            account.setType(AccountType.FREE);
            /*
             * Downloads via JDownloader only possible for real paid accounts --> Show this in account status to prevent users from thinking
             * that this is a bug thus creating useless support tickets.
             */
            if ("de".equalsIgnoreCase(accountlanguage)) {
                statustext += " !Downloads nur mit bezahlten Accounts möglich!";
            } else {
                statustext += " !Downloads require a paid account!";
            }
        } else {
            account.setType(AccountType.PREMIUM);
        }
        ai.setStatus(statustext);
        if (validUntil > 0) {
            // add 24 hours as the day seems to be valid on last day
            ai.setValidUntil(validUntil + (24 * 60 * 60 * 1000l));
        }
        account.setMaxSimultanDownloads(ACCOUNT_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        account.setValid(true);
        account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 60 * 60 * 1000l);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        inPremiumMode = true;
        requestFileInformation(link);
        if (AccountType.FREE.equals(account.getType())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        /* No need to log in here - we're already logged in! */
        handleDownload(link);
    }

    private void getPage(final String url) throws IOException, PluginException {
        this.br.getPage(url);
        getPage(this.br, url);
    }

    private void postPage(final String url, final String data) throws IOException, PluginException {
        postPage(this.br, url, data);
    }

    public static void getPage(final Browser br, final String url) throws IOException, PluginException {
        br.getPage(url);
        generalErrorhandling(br);
    }

    @SuppressWarnings("deprecation")
    public static void postPage(final Browser br, final String url, final String data) throws IOException, PluginException {
        br.postPage(url, data);
        generalErrorhandling(br);
    }

    public static void generalErrorhandling(final Browser br) throws PluginException {
        if (br.containsHTML("Sie sind wegen zu großer Serverbelastung ausgeloggt worden")) {
            /**
             * <div class="notice-page-msg"> Sie sind wegen zu großer Serverbelastung ausgeloggt worden. Sie können sich wieder in 86
             * Minuten einloggen. Vielen Dank für Ihre Geduld. </div>
             */
            /* TODO: Add this errorhandling for other languages too. */
            /*
             * That means that our account is valid but at the same time cookies will be invalid/missing --> We can accept such accounts but
             * are not yet able to download using them!
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nServer says: 'Sie sind wegen zu großer Serverbelastung ausgeloggt worden'!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
    }

    public static void prepareAjaxRequest(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }

    public static String getProductID(final Browser br) {
        String productid = br.getRegex("Video\\.product_id[\t\n\r ]*?=[\t\n\r ]*?(\\d+);").getMatch(0);
        if (productid == null) {
            productid = br.getRegex("var support_product_id[\t\n\r ]*?=[\t\n\r ]*?(\\d+);").getMatch(0);
        }
        return productid;
    }

    public static String getActiveVideoID(final Browser br) {
        String productid = br.getRegex("Video\\.active_video_id[\t\n\r ]*?=[\t\n\r ]*?(\\d+);").getMatch(0);
        if (productid == null) {
            productid = br.getRegex("Video\\.initVideoDetails\\((\\d+)").getMatch(0);
        }
        return productid;
    }

    public static String getFormattedVideoPositionNumber(final long videoposition) {
        final DecimalFormat df = new DecimalFormat("000");
        return df.format(videoposition);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_MAXDOWNLOADS;
    }

    @Override
    public String getDescription() {
        return "JDownloader's video2brain Plugin helps downloading videoclips from video2brain.com.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ADD_ORDERID, JDL.L("plugins.hoster.orf.video2braincom", "Add position number to filename?\r\nE.g. '001_somevideoname.mp4', '002_somevideoname.mp4'\r\nKeep in mind that this will only work for courses that were added via course-urls and NOT for single videos!")).setDefaultValue(defaultADD_ORDERID));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}