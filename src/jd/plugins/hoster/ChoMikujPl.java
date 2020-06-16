//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "chomikuj.pl" }, urls = { "https?://chomikujdecrypted\\.pl/.*?,\\d+$" })
public class ChoMikujPl extends antiDDoSForHost {
    private String               dllink                      = null;
    private static final String  PREMIUMONLY                 = "(Aby pobrać ten plik, musisz być zalogowany lub wysłać jeden SMS\\.|Właściciel tego chomika udostępnia swój transfer, ale nie ma go już w wystarczającej|wymaga opłacenia kosztów transferu z serwerów Chomikuj\\.pl)";
    private static final String  ACCESSDENIED                = "Nie masz w tej chwili uprawnień do tego pliku lub dostęp do niego nie jest w tej chwili możliwy z innych powodów\\.";
    private final String         VIDEOENDINGS                = "\\.(avi|flv|mp4|mpg|rmvb|divx|wmv|mkv)";
    private static final String  MAINPAGE                    = "https://chomikuj.pl/";
    /* Pluging settings */
    public static final String   DECRYPTFOLDERS              = "DECRYPTFOLDERS";
    private static final String  AVOIDPREMIUMMP3TRAFFICUSAGE = "AVOIDPREMIUMMP3TRAFFICUSAGE";
    private Browser              cbr                         = null;
    private static final int     free_maxchunks              = 1;
    private static final boolean free_resume                 = false;
    private static final int     free_maxdls                 = -1;
    private static final int     account_maxchunks           = 0;
    /* TODO: Verify if premium users really can resume */
    private static final boolean account_resume              = true;
    private static final int     account_maxdls              = -1;
    private boolean              serverIssue                 = false;
    private boolean              premiumonly                 = false;
    private boolean              plus18                      = false;

    public ChoMikujPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://chomikuj.pl/Create.aspx");
        setConfigElements();
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("chomikujdecrypted.pl/", "chomikuj.pl/"));
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setAllowedResponseCodes(new int[] { 500 });
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        serverIssue = false;
        premiumonly = false;
        plus18 = false;
        br.setFollowRedirects(true);
        final String fid = getFID(link);
        String mainlink = link.getStringProperty("mainlink", null);
        if (mainlink == null) {
            /* 2020-02-27 */
            mainlink = link.getContentUrl();
        }
        if (fid == null) {
            /* This should never happen! */
            logger.info("Failed to find fileid");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * Workaround: Always try to login even if this is not called from handlePremium as some content is only available when user is
         * logged in which he usually isn't during availablecheck and we do not want to have that content displayed as offline!
         */
        final Account accWorkaround = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            this.login(account, false);
        } else if (accWorkaround != null) {
            this.login(accWorkaround, false);
        }
        if (mainlink != null) {
            /* Try to find better filename - usually only needed for single links. */
            getPage(mainlink);
            if (isDownload) {
                if (this.passwordHandling(link, account)) {
                    /* We need to access URL again in case there way a password prompt. */
                    getPage(mainlink);
                }
            }
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!br.containsHTML(fid)) {
                /* html must contain fileid - if not, content should be offline (e.g. redirect to upper folder or errorpage) */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String filesize = br.getRegex("<p class=\"fileSize\">([^<>\"]*?)</p>").getMatch(0);
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
            }
            String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (filename != null) {
                logger.info("Found html filename for single link");
                filename = Encoding.htmlDecode(filename).trim();
                link.setFinalFileName(filename);
            } else {
                logger.info("Failed to find html filename for single link");
            }
            plus18 = this.br.containsHTML("\"FormAdultViewAccepted\"");
        }
        /*
         * 2016-11-03: Removed filesize check [for free download attempts] because free downloads are only rarely possible and usually
         * require a captcha.
         */
        if (dllink != null && !isDownload) {
            // In case the link redirects to the finallink
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (con.isOK() && (con.isContentDisposition() || !con.getContentType().contains("html"))) {
                    link.setDownloadSize(con.getLongContentLength());
                    /* Only set final filename if it wasn't set before as video and */
                    /* audio streams can have bad filenames */
                    if (link.getFinalFileName() == null) {
                        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                    }
                } else {
                    /* Just because we get html here that doesn't mean that the file is offline ... */
                    serverIssue = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            throw e;
        }
        final String remainingTraffic = br.getRegex("<strong>([^<>\"]*?)</strong>[\t\n\r ]+transferu").getMatch(0);
        if (remainingTraffic != null) {
            if (this.getPluginConfig().getBooleanProperty("IGNORE_TRAFFIC_LIMIT", false) || this.getPluginConfig().getBooleanProperty(ChoMikujPl.AVOIDPREMIUMMP3TRAFFICUSAGE, false)) {
                /*
                 * Uploaders can always download their OWN files no matter how much traffic they have left and downloading streams does not
                 * use up any traffic.
                 */
                ai.setSpecialTraffic(true);
            } else {
                ai.setSpecialTraffic(false);
            }
            final long hardcodedDailyFreeLimit = SizeFormatter.getSize("50MB");
            ai.setTrafficLeft(SizeFormatter.getSize(remainingTraffic.replace(",", ".")));
            /*
             * Most users will use free accounts with a daily limit of max 50 MB so let's just display that as max. traffic for all users
             * who have less- exactly 50MB left.
             */
            if (ai.getTrafficLeft() <= hardcodedDailyFreeLimit) {
                ai.setTrafficMax(hardcodedDailyFreeLimit);
            }
            ai.setStatus("Account with traffic limitation");
        } else {
            /*
             * 2019-07-16: Not sure if that is a good idea but at the moment we're handling all accounts as premium and set unlimited
             * traffic if we don't find any ...
             */
            ai.setStatus("Account without traffic limitation");
            ai.setUnlimitedTraffic();
        }
        account.setType(AccountType.PREMIUM);
        /* 2019-07-16: Points can be converted to traffic but for us they're not important */
        final String collectedPointsStr = br.getRegex("title=\"Punkty\"[^<>]*?><strong>\\s*(\\d+)\\s*</strong>").getMatch(0);
        if (collectedPointsStr != null) {
            ai.setPremiumPoints(collectedPointsStr);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://chomikuj.pl/Regulamin.aspx";
    }

    public boolean getDllink(final DownloadLink theLink, final Browser br, final boolean premium) throws Exception {
        final boolean redirectsSetting = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        String unescapedBR;
        final String fid = getFID(theLink);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* Premium users can always download the original file */
        if (isVideo(theLink) && !premium) {
            /* Download video stream (free download) */
            br.setFollowRedirects(true);
            getPageWithCleanup(br, "https://" + this.getHost() + "/ShowVideo.aspx?id=" + fid);
            if (br.getURL().contains("chomikuj.pl/Error404.aspx") || cbr.containsHTML("(Nie znaleziono|Strona, której szukasz nie została odnaleziona w portalu\\.<|>Sprawdź czy na pewno posługujesz się dobrym adresem)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.setFollowRedirects(false);
            getPage(br, "https://" + this.getHost() + "/Video.ashx?id=" + fid + "&type=1&ts=" + new Random().nextInt(1000000000) + "&file=video&start=0");
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                /* Probably not free downloadable! */
                return false;
            }
            theLink.setFinalFileName(theLink.getName());
        } else if (theLink.getName().toLowerCase().endsWith(".mp3") && !premium) {
            /* Download mp3 stream */
            dllink = getDllinkMP3(theLink);
            theLink.setFinalFileName(theLink.getName());
        } else {
            /* 2016-11-30: That page does not exist anymore. */
            // getPageWithCleanup(br, "http://chomikuj.pl/action/fileDetails/Index/" + fid);
            // final String filesize = br.getRegex("<p class=\"fileSize\">([^<>\"]*?)</p>").getMatch(0);
            // if (filesize != null) {
            // theLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
            // }
            // if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("fileDetails/Unavailable")) {
            // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // }
            final String requestVerificationToken = br.getRegex("<div id=\"content\">\\s*?<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (requestVerificationToken == null) {
                logger.warning("Failed to find requestVerificationToken");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setCookie(this.getHost(), "cookiesAccepted", "1");
            postPageWithCleanup(br, "https://" + this.getHost() + "/action/License/DownloadContext", "FileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
            unescapedBR = Encoding.unicodeDecode(br.toString());
            String serializedUserSelection = new Regex(unescapedBR, "name=\"SerializedUserSelection\" type=\"hidden\" value=\"([^<>\"]+)\"").getMatch(0);
            if (serializedUserSelection == null) {
                serializedUserSelection = new Regex(unescapedBR, "name=\\\\\"SerializedUserSelection\\\\\" type=\\\\\"hidden\\\\\" value=\\\\\"([^<>\"]+)\\\\\"").getMatch(0);
            }
            String serializedOrgFile = new Regex(unescapedBR, "name=\"SerializedOrgFile\" type=\"hidden\" value=\"([^<>\"]+)\"").getMatch(0);
            if (serializedOrgFile == null) {
                serializedOrgFile = new Regex(unescapedBR, "name=\\\\\"SerializedOrgFile\\\\\" type=\\\\\"hidden\\\\\" value=\\\\\"([^<>\"]+)\\\\\"").getMatch(0);
            }
            if (br.containsHTML("downloadWarningForm")) {
                if (serializedUserSelection == null || serializedOrgFile == null) {
                    /* Plugin broken */
                    return false;
                }
                postPageWithCleanup(br, "/action/License/DownloadWarningAccept", "FileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken) + "&SerializedUserSelection=" + Encoding.urlEncode(serializedUserSelection) + "&SerializedOrgFile=" + Encoding.urlEncode(serializedOrgFile));
                unescapedBR = Encoding.unicodeDecode(br.toString());
            }
            if (br.containsHTML("g\\-recaptcha")) {
                final String rcSiteKey = PluginJSonUtils.getJson(unescapedBR, "sitekey");
                if (rcSiteKey == null || serializedUserSelection == null || serializedOrgFile == null) {
                    /* Plugin broken */
                    return false;
                }
                /* Handle captcha */
                logger.info("Handling captcha");
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcSiteKey).getToken();
                final String postData = "FileId=" + fid + "&SerializedUserSelection=" + Encoding.urlEncode(serializedUserSelection) + "&SerializedOrgFile=" + Encoding.urlEncode(serializedOrgFile) + "&FileName=" + Encoding.urlEncode(theLink.getName()) + "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response) + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken);
                postPageWithCleanup(br, "/action/License/DownloadNotLoggedCaptchaEntered", postData);
            } else {
                postPageWithCleanup(br, "/action/License/Download", "FileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
            }
            if (cbr.containsHTML(PREMIUMONLY)) {
                return false;
            }
            if (cbr.containsHTML(ACCESSDENIED)) {
                return false;
            }
            dllink = PluginJSonUtils.getJson(br, "redirectUrl");
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("\\\\u003ca href=\\\\\"([^\"]*?)\\\\\" title").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("\"(https?://[A-Za-z0-9\\-_\\.]+\\.chomikuj\\.pl/File\\.aspx[^<>\"]*?)\\\\\"").getMatch(0);
            }
            if (!StringUtils.isEmpty(dllink)) {
                dllink = Encoding.htmlDecode(dllink);
            }
        }
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.unicodeDecode(dllink);
        }
        br.setFollowRedirects(redirectsSetting);
        return true;
    }

    private static Object PWLOCK = new Object();

    private boolean passwordHandling(final DownloadLink link, final Account account) throws Exception {
        final Object thislock;
        if (account != null) {
            thislock = account;
        } else {
            thislock = PWLOCK;
        }
        synchronized (thislock) {
            final Form passForm = br.getFormbyProperty("id", "LoginToFolder");
            if (passForm == null) {
                logger.info("Content is NOT password protected or there was no password prompt because we still got valid password cookies");
                return false;
            }
            logger.info("Content is password protected");
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
            }
            passForm.put("Password", passCode);
            passForm.remove("Remember");
            if (account != null) {
                passForm.put("Remember", "true");
            } else {
                passForm.put("Remember", "false");
            }
            this.submitForm(passForm);
            /* Important! The other parts of this plugin cannot handle escaped results! */
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
            final String success = PluginJSonUtils.getJson(br, "IsSuccess");
            if (!"true".equals(success)) {
                logger.info("User entered wrong password");
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            logger.info("User entered correct password");
            link.setDownloadPassword(passCode);
            if (account != null) {
                /* Save cookies and hope that all other links will not require us to enter password again --> Downloadstart is faster */
                account.saveCookies(br.getCookies(br.getURL()), "");
            }
            return true;
        }
    }

    private boolean isVideo(final DownloadLink dl) {
        String filename = dl.getFinalFileName();
        if (filename == null) {
            filename = dl.getName();
        }
        if (filename.contains(".")) {
            final String ext = filename.substring(filename.lastIndexOf("."));
            if (ext.matches(VIDEOENDINGS)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public boolean getDllink_premium(final DownloadLink link, final Account account, final Browser br, final boolean premium) throws Exception {
        final boolean redirectsSetting = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        final String fid = this.getFID(link);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (this.getPluginConfig().getBooleanProperty(AVOIDPREMIUMMP3TRAFFICUSAGE, false) && link.getName().toLowerCase().endsWith(".mp3")) {
            /* User wants to force stream download for .mp3 files --> Does not use up any premium traffic. */
            dllink = getDllinkMP3(link);
        } else {
            /* Premium users can always download the original file */
            final boolean accountHasLessTrafficThanRequiredForThisFile = account.getAccountInfo().getTrafficLeft() < link.getView().getBytesTotal();
            try {
                final String requestVerificationToken = br.getRegex("<div id=\"content\">\\s*?<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (requestVerificationToken == null) {
                    logger.warning("Failed to find requestVerificationToken");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* First, kind of a linkcheck - if we haven't found the filesize before we'll probably find it now. */
                getPageWithCleanup(br, "https://chomikuj.pl/action/fileDetails/Index/" + fid);
                final String filesize = br.getRegex("<p class=\"fileSize\">([^<>\"]*?)</p>").getMatch(0);
                if (filesize != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
                }
                if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("fileDetails/Unavailable")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* Users can override traffic check. For this case we'll check if we have enough traffic for this file here. */
                if (!account.getAccountInfo().isSpecialTraffic() && accountHasLessTrafficThanRequiredForThisFile) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not enough traffic to download this file", 30 * 60 * 1000l);
                }
                br.setCookie("http://chomikuj.pl/", "__RequestVerificationToken_Lw__", requestVerificationToken);
                br.getHeaders().put("Referer", link.getDownloadURL());
                postPageWithCleanup(br, "https://chomikuj.pl/action/License/DownloadContext", "fileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
                if (cbr.containsHTML(ACCESSDENIED)) {
                    return false;
                }
                /* Low traffic warning(?) */
                if (cbr.containsHTML("action=\"/action/License/DownloadWarningAccept\"")) {
                    final String serializedUserSelection = cbr.getRegex("name=\"SerializedUserSelection\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                    final String serializedOrgFile = cbr.getRegex("name=\"SerializedOrgFile\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (serializedUserSelection == null || serializedOrgFile == null) {
                        logger.warning("Failed to pass low traffic warning!");
                        return false;
                    }
                    postPageWithCleanup(br, "https://chomikuj.pl/action/License/DownloadWarningAccept", "FileId=" + fid + "&SerializedUserSelection=" + Encoding.urlEncode(serializedUserSelection) + "&SerializedOrgFile=" + Encoding.urlEncode(serializedOrgFile) + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
                }
                if (cbr.containsHTML("dontShowBoxInSession")) {
                    /* 2020-04-29: https://www.picflash.org/viewer.php?img=chomikuj_disable_box_download2X2RRQ.png */
                    postPageWithCleanup(br, "/action/chomikbox/DontDownloadWithBox", "__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
                    postPageWithCleanup(br, "/action/License/Download", "FileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
                }
                if (cbr.containsHTML("/action/License/acceptLargeTransfer")) {
                    // this can happen also
                    // problem is.. general cleanup is wrong, response is = Content-Type: application/json; charset=utf-8
                    cleanupBrowser(br, PluginJSonUtils.unescape(br.toString()));
                    // so we can get output in logger for debug purposes.
                    logger.info(cbr.toString());
                    final Form f = cbr.getFormbyAction("/action/License/acceptLargeTransfer");
                    if (f == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    submitFormWithCleanup(br, f);
                } else if (cbr.containsHTML("/action/License/AcceptOwnTransfer")) {
                    /*
                     * Some files on chomikuj hoster are available to download using transfer from file owner. When there's no owner
                     * transfer left then transfer is reduced from downloader account (downloader is asked if he wants to use his own
                     * transfer). We have to confirm this here.
                     */
                    // problem is.. general cleanup is wrong, response is = Content-Type: application/json; charset=utf-8
                    cleanupBrowser(cbr, PluginJSonUtils.unescape(br.toString()));
                    // so we can get output in logger for debug purposes.
                    logger.info(cbr.toString());
                    final Form f = cbr.getFormbyAction("/action/License/AcceptOwnTransfer");
                    if (f == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    f.remove(null);
                    f.remove(null);
                    f.put("__RequestVerificationToken", Encoding.urlEncode(requestVerificationToken));
                    submitFormWithCleanup(br, f);
                }
                dllink = br.getRegex("redirectUrl\":\"(https?://.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\\\\u003ca href=\\\\\"([^\"]*?)\\\\\" title").getMatch(0);
                }
                if (dllink == null) {
                    dllink = br.getRegex("\"(https?://[A-Za-z0-9\\-_\\.]+\\.chomikuj\\.pl/File\\.aspx[^<>\"]*?)\\\\\"").getMatch(0);
                }
                if (dllink == null) {
                    if (cbr.containsHTML("\"BuyAdditionalTransfer")) {
                        logger.info("Disabling chomikuj.pl account: Not enough traffic available");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    logger.warning("Failed to find final downloadurl");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = Encoding.unicodeDecode(dllink);
                dllink = Encoding.htmlDecode(dllink);
                if (dllink.contains("#SliderTransfer")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Traffic limit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                br.setFollowRedirects(redirectsSetting);
            } catch (final PluginException e) {
                String msg = null;
                try {
                    final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    msg = (String) entries.get("Content");
                    msg = msg.trim();
                } catch (final Throwable e2) {
                }
                if (e.getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT && accountHasLessTrafficThanRequiredForThisFile) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not enough traffic to download this file", 30 * 60 * 1000l);
                } else if (!StringUtils.isEmpty(msg)) {
                    /* Try to display more precise errormessage, avoid plugin defect. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg);
                } else {
                    throw e;
                }
            }
        }
        return true;
    }

    private String getDllinkMP3(final DownloadLink dl) throws Exception {
        final String fid = getFID(dl);
        getPageWithCleanup(br, "https://" + this.getHost() + "/Audio.ashx?id=" + fid + "&type=2&tp=mp3");
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            logger.warning("Failed to find stream-URL");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.unicodeDecode(dllink);
        return dllink;
    }

    private String getFID(final DownloadLink dl) {
        return dl.getStringProperty("fileid");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdls;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return account_maxdls;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        dllink = checkDirectLink(link, null);
        if (dllink == null) {
            requestFileInformation(link, null, true);
            final boolean is_premiumonly = cbr != null && cbr.containsHTML(PREMIUMONLY) || this.premiumonly;
            if (plus18) {
                logger.info("Adult content only downloadable when logged in");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else if (serverIssue) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            }
            if (!getDllink(link, br, false) && is_premiumonly) {
                throw new AccountRequiredException();
            } else if (this.dllink == null) {
                /* 2020-04-20: Lazy handling because most files are premiumonly: Final downloadlink not found = premiumonly */
                throw new AccountRequiredException();
            }
        }
        handleDownload(link, null, free_resume, free_maxchunks);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        dllink = checkDirectLink(link, account);
        if (dllink == null) {
            /* Login will happen inside requestFileInformation */
            requestFileInformation(link, account, true);
            getDllink_premium(link, account, br, true);
        }
        handleDownload(link, account, account_resume, account_maxchunks);
    }

    public void handleDownload(final DownloadLink link, final Account account, boolean resume, int maxChunks) throws Exception, PluginException {
        if (dllink == null) {
            logger.warning("dllink is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean isAudioStreamDownload = StringUtils.containsIgnoreCase(dllink, "/Audio.ashx");
        final boolean isVideoStreamPreviewDownload = StringUtils.containsIgnoreCase(dllink, "/Preview.ashx");
        if (isAudioStreamDownload || isVideoStreamPreviewDownload) {
            resume = true;
            maxChunks = 0;
        }
        if (!resume) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
        dl.setFilenameFix(true);
        final URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition() && ((StringUtils.containsIgnoreCase(con.getContentType(), "text") && con.getResponseCode() == 200) || !con.isOK())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (account != null) {
            link.setProperty("dllink_account", dllink);
        } else {
            link.setProperty("dllink_free", dllink);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final Account account) {
        final String directlinkproperty;
        if (account != null) {
            directlinkproperty = "dllink_account";
        } else {
            directlinkproperty = "dllink_free";
        }
        String dllink = downloadLink.getStringProperty(directlinkproperty);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(directlinkproperty, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(directlinkproperty, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    private void handleServerErrors() throws PluginException {
        if (br.getURL().contains("Error.aspx")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 15 * 60 * 1000l);
        }
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                if (setLoginCookies(this.br, account)) {
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                        logger.info("Trust cookies without checking");
                        return;
                    }
                    getPageWithCleanup(this.br, MAINPAGE);
                    if (this.isLoggedIn(this.br)) {
                        logger.info("Successfully loggedin via cookies");
                        /* Save new cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Failed to login via cookies");
                    }
                }
                logger.info("Performing full login");
                br.clearCookies(account.getHoster());
                prepBrowser(br, account.getHoster());
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                getPageWithCleanup(this.br, MAINPAGE);
                String postData = "ReturnUrl=&Login=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass());
                final String[] requestVerificationTokens = br.getRegex("<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"\\']+)\"").getColumn(0);
                if (requestVerificationTokens.length > 0) {
                    logger.info("Found " + requestVerificationTokens.length + "x '__RequestVerificationToken' values");
                    /*
                     * 2019-10-17: Strange - website contains this value twice (well different values, same key) and uses them in login POST
                     * data. According to my tests, login works even without these tokens or with them set to "".
                     */
                    for (final String requestVerificationToken : requestVerificationTokens) {
                        postData += "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken);
                    }
                } else {
                    logger.info("Failed to find any '__RequestVerificationToken' - trying to login without it");
                }
                PostRequest postRequest = br.createPostRequest("/action/Login/TopBarLogin", postData);
                postRequest.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                // postPageRawWithCleanup(this.br, "/action/Login/TopBarLogin",
                // "rememberLogin=true&rememberLogin=false&ReturnUrl=&Login=" + Encoding.urlEncode(account.getUser()) + "&Password=" +
                // Encoding.urlEncode(account.getPass()) + "&__RequestVerificationToken=" +
                // Encoding.urlEncode(requestVerificationToken));
                postRequestWithCleanup(this.br, postRequest);
                if (!isLoggedIn(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.setCookie(br.getHost(), "cookiesAccepted", "1");
                br.setCookie(br.getHost(), "spt", "0");
                br.setCookie(br.getHost(), "rcid", "1");
                getPageWithCleanup(this.br, "/" + Encoding.urlEncode(account.getUser()));
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public static boolean setLoginCookies(final Browser brlogin, final Account account) {
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            brlogin.setCookies(account.getHoster(), cookies);
            return true;
        } else {
            return false;
        }
    }

    private boolean isLoggedIn(final Browser br) {
        return br.getCookie(MAINPAGE, "RememberMe", Cookies.NOTDELETEDPATTERN) != null;
    }

    /** Performs request and then puts cleaned up html into cbr browser instance. */
    private void getPageWithCleanup(final Browser br, final String url) throws Exception {
        getPage(br, url);
        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, correctBR(br.toString()));
    }

    /** Performs request and then puts cleaned up html into cbr browser instance. */
    private void postPageWithCleanup(final Browser br, final String url, final String postData) throws Exception {
        postPage(br, url, postData);
        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, correctBR(br.toString()));
    }

    /** Performs request and then puts cleaned up html into cbr browser instance. */
    private void postPageRawWithCleanup(final Browser br, final String url, final String postData) throws Exception {
        postPageRaw(br, url, postData);
        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, correctBR(br.toString()));
    }

    /** Performs request and then puts cleaned up html into cbr browser instance. */
    private void postRequestWithCleanup(final Browser br, Request request) throws Exception {
        sendRequest(br, request);
        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, correctBR(br.toString()));
    }

    /** Performs request and then puts cleaned up html into cbr browser instance. */
    private void submitFormWithCleanup(final Browser br, final Form form) throws Exception {
        submitForm(br, form);
        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, correctBR(br.toString()));
    }

    private String correctBR(final String input) {
        return input.replace("\\", "");
    }

    /**
     * This allows backward compatibility for design flaw in setHtmlCode(), It injects updated html into all browsers that share the same
     * request id. This is needed as request.cloneRequest() was never fully implemented like browser.cloneBrowser().
     *
     * @param ibr
     *            Import Browser
     * @param t
     *            Provided replacement string output browser
     * @author raztoki
     */
    private void cleanupBrowser(final Browser ibr, final String t) throws Exception {
        String dMD5 = JDHash.getMD5(ibr.toString());
        // preserve valuable original request components.
        final String oURL = ibr.getURL();
        final URLConnectionAdapter con = ibr.getRequest().getHttpConnection();
        Request req = new Request(oURL) {
            {
                boolean okay = false;
                try {
                    final Field field = this.getClass().getSuperclass().getDeclaredField("requested");
                    field.setAccessible(true);
                    field.setBoolean(this, true);
                    okay = true;
                } catch (final Throwable e2) {
                    e2.printStackTrace();
                }
                if (okay == false) {
                    try {
                        requested = true;
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                }
                httpConnection = con;
                setHtmlCode(t);
            }

            public long postRequest() throws IOException {
                return 0;
            }

            public void preRequest() throws IOException {
            }
        };
        ibr.setRequest(req);
        if (ibr.isDebug()) {
            logger.info("\r\ndirtyMD5sum = " + dMD5 + "\r\ncleanMD5sum = " + JDHash.getMD5(ibr.toString()) + "\r\n");
            System.out.println(ibr.toString());
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.ChomikujPlScript;
    }

    // for the decrypter, so we have only one session of antiddos
    public void getPage(final String url) throws Exception {
        super.getPage(url);
    }

    // for the decrypter, so we have only one session of antiddos
    public void postPage(final String url, final String parameter) throws Exception {
        super.postPage(url, parameter);
    }

    // for the decrypter, so we have only one session of antiddos
    public void submitForm(final Form form) throws Exception {
        super.submitForm(form);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ChoMikujPl.AVOIDPREMIUMMP3TRAFFICUSAGE, "Force download of the stream versions of .mp3 files in account mode?\r\n<html><b>Avoids premium traffic usage for .mp3 files!</b></html>").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ChoMikujPl.DECRYPTFOLDERS, "Decrypt subfolders in folders").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "IGNORE_TRAFFIC_LIMIT", "Ignore trafficlimit in account (e.g. useful to download self uploaded files or stream download)?").setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}