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
import java.util.Random;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "chomikuj.pl" }, urls = { "http://chomikujdecrypted\\.pl/.*?,\\d+$" })
public class ChoMikujPl extends antiDDoSForHost {
    private String              dllink                      = null;
    private static final String PREMIUMONLY                 = "(Aby pobrać ten plik, musisz być zalogowany lub wysłać jeden SMS\\.|Właściciel tego chomika udostępnia swój transfer, ale nie ma go już w wystarczającej|wymaga opłacenia kosztów transferu z serwerów Chomikuj\\.pl)";
    private static final String PREMIUMONLYUSERTEXT         = "Download is only available for registered/premium users!";
    private static final String ACCESSDENIED                = "Nie masz w tej chwili uprawnień do tego pliku lub dostęp do niego nie jest w tej chwili możliwy z innych powodów\\.";
    private final String        VIDEOENDINGS                = "\\.(avi|flv|mp4|mpg|rmvb|divx|wmv|mkv)";
    private static final String MAINPAGE                    = "http://chomikuj.pl/";
    private static Object       LOCK                        = new Object();
    /* Pluging settings */
    public static final String  DECRYPTFOLDERS              = "DECRYPTFOLDERS";
    private static final String AVOIDPREMIUMMP3TRAFFICUSAGE = "AVOIDPREMIUMMP3TRAFFICUSAGE";
    private static boolean      pluginloaded                = false;
    private Browser             cbr                         = null;
    private int                 free_maxchunks              = 1;
    private boolean             free_resume                 = false;
    private int                 free_maxdls                 = -1;
    private int                 account_maxchunks           = 0;
    /* TODO: Verify if premium users really can resume */
    private boolean             account_resume              = true;
    private int                 account_maxdls              = -1;
    private boolean             serverIssue                 = false;
    private boolean             premiumonly                 = false;
    private boolean             plus18                      = false;

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
            prepBr.setAllowedResponseCodes(500);
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        serverIssue = false;
        premiumonly = false;
        plus18 = false;
        this.setBrowserExclusive();
        final String mainlink = link.getStringProperty("mainlink", null);
        // Offline from decrypter
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (mainlink != null) {
            /* Try to find better filename - usually only needed for single links. */
            getPage(mainlink);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                /* Additional offline check */
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
        }
        plus18 = this.br.containsHTML("\"FormAdultViewAccepted\"");
        /*
         * 2016-11-03: Removed filesize check [for free download attempts] because free downloads are only rarely possible and usually
         * require a captcha.
         */
        if (dllink != null) {
            // In case the link redirects to the finallink
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        account.setValid(true);
        final String remainingTraffic = br.getRegex("<strong>([^<>\"]*?)</strong>[\t\n\r ]+transferu").getMatch(0);
        if (remainingTraffic != null) {
            /* Basically uploaders can always download their own files no matter how much traffic they have left ... */
            ai.setSpecialTraffic(true);
            ai.setTrafficLeft(SizeFormatter.getSize(remainingTraffic.replace(",", ".")));
        } else {
            ai.setUnlimitedTraffic();
        }
        ai.setStatus("Premium Account");
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://chomikuj.pl/Regulamin.aspx";
    }

    @SuppressWarnings("deprecation")
    public boolean getDllink(final DownloadLink theLink, final Browser br, final boolean premium) throws Exception {
        final boolean redirectsSetting = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        String unescapedBR;
        final String fid = getFID(theLink);
        // Set by the decrypter if the link is password protected
        String savedLink = theLink.getStringProperty("savedlink");
        String savedPost = theLink.getStringProperty("savedpost");
        if (savedLink != null && savedPost != null) {
            postPage(br, savedLink, savedPost);
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* Premium users can always download the original file */
        if (isVideo(theLink) && !premium) {
            /* Download video stream (free download) */
            br.setFollowRedirects(true);
            getPageWithCleanup(br, "http://" + this.getHost() + "/ShowVideo.aspx?id=" + fid);
            if (br.getURL().contains("chomikuj.pl/Error404.aspx") || cbr.containsHTML("(Nie znaleziono|Strona, której szukasz nie została odnaleziona w portalu\\.<|>Sprawdź czy na pewno posługujesz się dobrym adresem)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.setFollowRedirects(false);
            getPage(br, "http://" + this.getHost() + "/Video.ashx?id=" + fid + "&type=1&ts=" + new Random().nextInt(1000000000) + "&file=video&start=0");
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
            final String chomikID = theLink.getStringProperty("chomikID");
            String requestVerificationToken = theLink.getStringProperty("requestverificationtoken");
            if (requestVerificationToken == null || true) {
                /* 2016-12-02: Debug-test: Always get a new requestverificationtoken */
                getPage(br, theLink.getDownloadURL());
                br.followRedirect();
                requestVerificationToken = br.getRegex("<div id=\"content\">\\s*?<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (requestVerificationToken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (chomikID != null) {
                final String folderPassword = theLink.getStringProperty("password");
                if (folderPassword != null) {
                    br.setCookie("http://chomikuj.pl/", "FoldersAccess", String.format("%s=%s", chomikID, folderPassword));
                } else {
                    logger.warning("Failed to set FoldersAccess cookie inside getDllink");
                    /* this url won't work without password. */
                    return false;
                }
            }
            br.setCookie(this.getHost(), "cookiesAccepted", "1");
            postPageWithCleanup(br, "http://" + this.getHost() + "/action/License/DownloadContext", "FileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
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
    public boolean getDllink_premium(final DownloadLink theLink, final Browser br, final boolean premium) throws Exception {
        final boolean redirectsSetting = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        final String fid = this.getFID(theLink);
        /* Set by the decrypter if the link is password protected */
        String savedLink = theLink.getStringProperty("savedlink");
        String savedPost = theLink.getStringProperty("savedpost");
        if (savedLink != null && savedPost != null) {
            postPage(br, savedLink, savedPost);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (this.getPluginConfig().getBooleanProperty(AVOIDPREMIUMMP3TRAFFICUSAGE, false) && theLink.getName().toLowerCase().endsWith(".mp3")) {
            /* User wants to force stream download for .mp3 files --> Does not use up any premium traffic. */
            dllink = getDllinkMP3(theLink);
        } else {
            /* Premium users can always download the original file */
            getPageWithCleanup(br, "http://chomikuj.pl/action/fileDetails/Index/" + fid);
            final String filesize = br.getRegex("<p class=\"fileSize\">([^<>\"]*?)</p>").getMatch(0);
            if (filesize != null) {
                theLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
            }
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("fileDetails/Unavailable")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String requestVerificationToken = theLink.getStringProperty("requestverificationtoken");
            if (requestVerificationToken == null) {
                br.setFollowRedirects(true);
                getPageWithCleanup(br, theLink.getDownloadURL());
                br.setFollowRedirects(false);
                requestVerificationToken = cbr.getRegex("<div id=\"content\">[\t\n\r ]+<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (requestVerificationToken == null) {
                requestVerificationToken = theLink.getStringProperty("__RequestVerificationToken_Lw__", null);
            }
            if (requestVerificationToken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setCookie("http://chomikuj.pl/", "__RequestVerificationToken_Lw__", requestVerificationToken);
            final String chomikID = theLink.getStringProperty("chomikID");
            if (chomikID != null) {
                final String folderPassword = theLink.getStringProperty("password");
                if (folderPassword != null) {
                    br.setCookie("http://chomikuj.pl/", "FoldersAccess", String.format("%s=%s", chomikID, folderPassword));
                } else {
                    logger.warning("Failed to set FoldersAccess cookie inside getDllink");
                    // this link won't work without password
                    return false;
                }
            }
            br.getHeaders().put("Referer", theLink.getDownloadURL());
            postPageWithCleanup(br, "http://chomikuj.pl/action/License/DownloadContext", "fileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
            if (cbr.containsHTML(ACCESSDENIED)) {
                return false;
            }
            /* Low traffic warning */
            if (cbr.containsHTML("action=\"/action/License/DownloadWarningAccept\"")) {
                final String serializedUserSelection = cbr.getRegex("name=\"SerializedUserSelection\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                final String serializedOrgFile = cbr.getRegex("name=\"SerializedOrgFile\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (serializedUserSelection == null || serializedOrgFile == null) {
                    logger.warning("Failed to pass low traffic warning!");
                    return false;
                }
                postPageWithCleanup(br, "http://chomikuj.pl/action/License/DownloadWarningAccept", "FileId=" + fid + "&SerializedUserSelection=" + Encoding.urlEncode(serializedUserSelection) + "&SerializedOrgFile=" + Encoding.urlEncode(serializedOrgFile) + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
            }
            if (cbr.containsHTML("/action/License/acceptLargeTransfer")) {
                // this can happen also
                // problem is.. general cleanup is wrong, response is = Content-Type: application/json; charset=utf-8
                cleanupBrowser(cbr, PluginJSonUtils.unescape(br.toString()));
                // so we can get output in logger for debug purposes.
                logger.info(cbr.toString());
                final Form f = cbr.getFormbyAction("/action/License/acceptLargeTransfer");
                if (f == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                submitFormWithCleanup(br, f);
            } else if (cbr.containsHTML("/action/License/AcceptOwnTransfer")) {
                /*
                 * Some files on chomikuj hoster are available to download using transfer from file owner. When there's no owner transfer
                 * left then transfer is reduced from downloader account (downloader is asked if he wants to use his own transfer). We have
                 * to confirm this here.
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
            dllink = br.getRegex("redirectUrl\":\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\\\\u003ca href=\\\\\"([^\"]*?)\\\\\" title").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("\"(http://[A-Za-z0-9\\-_\\.]+\\.chomikuj\\.pl/File\\.aspx[^<>\"]*?)\\\\\"").getMatch(0);
            }
            if (dllink != null) {
                dllink = Encoding.unicodeDecode(dllink);
                dllink = Encoding.htmlDecode(dllink);
                if (dllink.contains("#SliderTransfer")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Traffic limit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            }
            br.setFollowRedirects(redirectsSetting);
        }
        return true;
    }

    private String getDllinkMP3(final DownloadLink dl) throws Exception {
        final String fid = getFID(dl);
        getPageWithCleanup(br, "http://chomikuj.pl/Audio.ashx?id=" + fid + "&type=2&tp=mp3");
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.unicodeDecode(dllink);
        free_resume = false;
        account_resume = false;
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (plus18) {
            logger.info("Adult content only downloadable when logged in");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (serverIssue) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        } else if (cbr != null && cbr.containsHTML(PREMIUMONLY) || premiumonly) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (!isVideo(downloadLink)) {
            if (!getDllink(downloadLink, br, false)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!free_resume) {
            free_maxchunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        dl.setFilenameFix(true);
        final URLConnectionAdapter con = dl.getConnection();
        if ((StringUtils.containsIgnoreCase(con.getContentType(), "text") && con.getResponseCode() == 200) || !con.isOK()) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        dllink = null;
        getDllink_premium(link, br, true);
        if (cbr.containsHTML("\"BuyAdditionalTransfer")) {
            logger.info("Disabling chomikuj.pl account: Not enough traffic available");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (dllink == null) {
            String argh1 = br.getRegex("orgFile\\\\\" value=\\\\\"(.*?)\\\\\"").getMatch(0);
            String argh2 = br.getRegex("userSelection\\\\\" value=\\\\\"(.*?)\\\\\"").getMatch(0);
            if (argh1 == null || argh2 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // For some files they ask
            // "Do you really want to download this file", so we have to confirm
            // it with "YES" here ;)
            if (cbr.containsHTML("Właściciel tego chomika udostępnia darmowy transfer, ale jego ilość jest obecnie zbyt mała, aby można było pobrać plik")) {
                postPage("http://chomikuj.pl/action/License/AcceptOwnTransfer?fileId=" + getFID(link), "orgFile=" + Encoding.urlEncode(argh1) + "&userSelection=" + Encoding.urlEncode(argh2) + "&__RequestVerificationToken=" + Encoding.urlEncode(link.getStringProperty("requestverificationtoken")));
            } else {
                postPage("http://chomikuj.pl/action/License/acceptLargeTransfer?fileId=" + getFID(link), "orgFile=" + Encoding.urlEncode(argh1) + "&userSelection=" + Encoding.urlEncode(argh2) + "&__RequestVerificationToken=" + Encoding.urlEncode(link.getStringProperty("requestverificationtoken")));
            }
            dllink = br.getRegex("redirectUrl\":\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\\\\u003ca href=\\\\\"([^\"]*?)\\\\\" title").getMatch(0);
            }
            if (dllink != null) {
                dllink = Encoding.htmlDecode(dllink);
            }
            if (dllink == null) {
                getDllink(link, br, true);
            }
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        sleep(2 * 1000l, link);
        if (!account_resume) {
            account_maxchunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, account_resume, account_maxchunks);
        dl.setFilenameFix(true);
        final URLConnectionAdapter con = dl.getConnection();
        if ((StringUtils.containsIgnoreCase(con.getContentType(), "text") && con.getResponseCode() == 200) || !con.isOK()) {
            // 206 Partitial Content might have text/html content-type
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        if (br.getURL().contains("Error.aspx")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 15 * 60 * 1000l);
        }
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                getPageWithCleanup(this.br, MAINPAGE);
                final String lang = System.getProperty("user.language");
                final String requestVerificationToken = br.getRegex("<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"\\']+)\"").getMatch(0);
                if (requestVerificationToken == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                postPageRawWithCleanup(this.br, "http://chomikuj.pl/action/Login/TopBarLogin", "rememberLogin=true&rememberLogin=false&topBar_LoginBtn=Zaloguj&ReturnUrl=%2F" + Encoding.urlEncode(account.getUser()) + "&Login=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()) + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
                if (br.getCookie(MAINPAGE, "RememberMe") == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.setCookie(MAINPAGE, "cookiesAccepted", "1");
                br.setCookie(MAINPAGE, "spt", "0");
                br.setCookie(MAINPAGE, "rcid", "1");
                postPageRawWithCleanup(this.br, "http://chomikuj.pl/" + Encoding.urlEncode(account.getUser()), "ReturnUrl=%2F" + Encoding.urlEncode(account.getUser()) + "&Login=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()) + "&rememberLogin=true&rememberLogin=false&topBar_LoginBtn=Zaloguj");
                getPageWithCleanup(this.br, "http://chomikuj.pl/" + Encoding.urlEncode(account.getUser()));
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void getPageWithCleanup(final Browser br, final String url) throws Exception {
        getPage(br, url);
        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, correctBR(br.toString()));
    }

    private void postPageWithCleanup(final Browser br, final String url, final String postData) throws Exception {
        postPage(br, url, postData);
        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, correctBR(br.toString()));
    }

    private void postPageRawWithCleanup(final Browser br, final String url, final String postData) throws Exception {
        postPageRaw(br, url, postData);
        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, correctBR(br.toString()));
    }

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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ChoMikujPl.AVOIDPREMIUMMP3TRAFFICUSAGE, JDL.L("plugins.hoster.chomikujpl.avoidPremiumMp3TrafficUsage", "Force download of the stream versions of .mp3 files in account mode?\r\n<html><b>Avoids premium traffic usage for .mp3 files!</b></html>")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ChoMikujPl.DECRYPTFOLDERS, JDL.L("plugins.hoster.chomikujpl.decryptfolders", "Decrypt subfolders in folders")).setDefaultValue(true));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}