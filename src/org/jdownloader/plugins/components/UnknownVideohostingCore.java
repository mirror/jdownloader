package org.jdownloader.plugins.components;

//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class UnknownVideohostingCore extends PluginForHost {
    public UnknownVideohostingCore(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    // public static List<String[]> getPluginDomains() {
    // final List<String[]> ret = new ArrayList<String[]>();
    // // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
    // ret.add(new String[] { "imgdew.com" });
    // return ret;
    // }
    //
    // public static String[] getAnnotationNames() {
    // return buildAnnotationNames(getPluginDomains());
    // }
    //
    // @Override
    // public String[] siteSupportedNames() {
    // return buildSupportedNames(getPluginDomains());
    // }
    //
    // public static String[] getAnnotationUrls() {
    // return UnknownVideohostingCore.buildAnnotationUrls(getPluginDomains());
    // }
    // @Override
    // public String rewriteHost(String host) {
    // if (host == null) {
    // return null;
    // } else {
    // final String mapping = this.getMappedHost(ImgmazeCom.getPluginDomains(), host);
    // if (mapping != null) {
    // return mapping;
    // }
    // }
    // return super.rewriteHost(host);
    // }
    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;
    private final Object         LOCK              = new Object();

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/home";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        return this.getHost() + "://" + getFID(link);
    }

    public String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "([a-z0-9]{12}$|\\p{XDigit}++(?=\\.html$))").getMatch(0);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fuid = getFID(link);
        link.setPluginPatternMatcher(String.format("https://%s/%s", this.getHost(), fuid));
    }

    public static final String getDefaultAnnotationPatternPart() {
        return "/(?:(?:embed/)?[a-z0-9]{12}|embed-\\p{XDigit}++\\.html)";
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + UnknownVideohostingCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, InterruptedException {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        /* 1st offlinecheck */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("<title> EMBED</title>")) {
            final String redirectLink = link.getStringProperty("redirect_link");
            if (StringUtils.isEmpty(redirectLink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "page expired and no redirect_link found");
            }
            br.setFollowRedirects(false);
            br.getPage(redirectLink);
            if (br.getRedirectLocation() != null) {
                link.setPluginPatternMatcher(br.getRedirectLocation());
                return requestFileInformation(link);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* 2019-05-08: TODO: Unsure about that */
        boolean requiresCaptcha = true;
        String filename = null;
        try {
            final String json = br.getRegex("window\\.__INITIAL_STATE__=(\\{.*?\\});\\(function\\(\\)").getMatch(0);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
            entries = (LinkedHashMap<String, Object>) entries.get("videoplayer");
            final Object errorO = entries.get("error");
            if (errorO != null && errorO instanceof LinkedHashMap) {
                final LinkedHashMap<String, Object> error = (LinkedHashMap<String, Object>) errorO;
                final String message = (String) error.get("message");
                if ("invalid video code".equalsIgnoreCase(message)) {
                    return AvailableStatus.FALSE;
                }
            }
            requiresCaptcha = ((Boolean) entries.get("captcha")).booleanValue();
            entries = (LinkedHashMap<String, Object>) entries.get("video");
            filename = (String) entries.get("title");
        } catch (final Throwable e) {
        }
        String fallbackFilename = link.getStringProperty("fallback_filename");
        boolean fallbackFilenameEmpty = StringUtils.isEmpty(fallbackFilename);
        if (StringUtils.isEmpty(filename) || (new Regex(filename, "^(?:[a-z0-9]+|file title unknown)$").matches() && !fallbackFilenameEmpty)) {
            filename = br.getRegex("<title>Watch ([^<>\"]+) \\- Vidup</title>").getMatch(0);
            if (StringUtils.isEmpty(filename) || (new Regex(filename, "^(?:[a-z0-9]+|file title unknown)$").matches() && !fallbackFilenameEmpty)) {
                filename = br.getRegex("<title>(.*?)(?i: EMBED)?</title>").getMatch(0);
                if (StringUtils.isEmpty(filename) || (new Regex(filename, "^(?:[a-z0-9]+|file title unknown)$").matches() && !fallbackFilenameEmpty)) {
                    if (!fallbackFilenameEmpty) {
                        filename = fallbackFilename;
                    } else {
                        /* Last chance fallback */
                        filename = this.getFID(link);
                    }
                }
            }
        }
        String ext;
        if (!StringUtils.isEmpty(dllink)) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
            if (ext != null && !ext.matches("\\.(?:flv|mp4)")) {
                ext = default_extension;
            }
        } else {
            ext = default_extension;
        }
        filename = Encoding.htmlDecode(filename).trim();
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (check_filesize_via_directurl() || isDownload) {
            dllink = this.getDllink(link, isDownload);
            if (!StringUtils.isEmpty(dllink)) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        link.setDownloadSize(con.getLongContentLength());
                    } else {
                        server_issues = true;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br, final String rcKey) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, rcKey) {
            @Override
            public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                return TYPE.INVISIBLE;
            }
        };
    }

    /** If enabled, pairing API will be used for downloading e.g. see vev.io/pair */
    protected boolean usePairingMode() {
        return true;
    }

    protected int getPairingTimeoutSeconds() {
        return 120;
    }

    private String getDllink(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, InterruptedException {
        if (usePairingMode()) {
            /* https://vev.io/api#pair_access */
            br.getHeaders().put("Referer", "https://" + this.getHost() + "/pair");
            br.setAllowedResponseCodes(new int[] { 400 });
            /*
             * 2020-05-22: Although the API is pretty much unusable as long as the user did not enable pairing via browser, it does return
             * an offline status for invalid items --> By enabling this, we will check for this right away!
             */
            final boolean linkcheckOnFirstRequest = true;
            synchronized (LOCK) {
                if (linkcheckOnFirstRequest) {
                    br.getPage("https://" + this.getHost() + "/api/pair/" + this.getFID(link));
                    /* Check for offline status */
                    handleAPIIErrors(link, null, false);
                } else {
                    br.getPage("https://" + this.getHost() + "/api/pair");
                }
                /*
                 * 2020-05-22: E.g. failure:
                 * {"code":400,"message":"IP is not currently paired. Please visit https://vidup.io/pair for more information.","errors":[]}
                 */
                if (!isPaired()) {
                    logger.info("New pairing session required");
                    if (!isDownload) {
                        /* Avoid captchas during linkcheck */
                        return null;
                    }
                    final boolean doAutoPairing = false;
                    if (doAutoPairing) {
                        /* 2020-05-22: This is not yet working! */
                        br.getPage("https://" + this.getHost() + "/pair");
                        // final Form continueForm = br.getForm(0);
                        // br.submitForm(continueForm);
                        final String recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2(this, br, this.getReCaptchaKeyPairing()).getToken();
                        final Map<String, Object> postData = new HashMap<String, Object>();
                        postData.put("g-recaptcha-response", recaptchaV2Response);
                        postData.put("ihash", "TODO");
                        /* 2020-05-22: Example good response: {"session":{"ip":["12.12.12.12"],"expire":14400}} */
                        br.postPageRaw("/api/pair", JSonStorage.serializeToJson(postData));
                    } else {
                        /* TODO */
                        final Thread dialog = displayPairingDialog();
                        try {
                            final int maxwait = getPairingTimeoutSeconds();
                            final int waitStepsSeconds = 5;
                            int remainingWaitSeconds = maxwait;
                            do {
                                logger.info("Remaining pairing seconds: " + remainingWaitSeconds);
                                this.sleep(waitStepsSeconds * 1000l, link);
                                br.getPage("https://" + this.getHost() + "/api/pair");
                                if (isPaired()) {
                                    logger.info("Pairing successful");
                                    break;
                                } else {
                                    logger.info("Pairing failed");
                                    remainingWaitSeconds -= waitStepsSeconds;
                                }
                            } while (remainingWaitSeconds > 0);
                        } finally {
                            /* Close dialog */
                            dialog.interrupt();
                        }
                    }
                    if (!isPaired()) {
                        logger.info("Pairing failed");
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Pairing failure", 30 * 60 * 1000l);
                    }
                    handleAPIIErrors(link, null, false);
                }
            }
            if (br.getURL() == null || !br.getURL().contains(this.getFID(link))) {
                /* Only perform this API call if it hasn't been done already! */
                br.getPage("https://" + this.getHost() + "/api/pair/" + this.getFID(link));
                handleAPIIErrors(link, null, false);
            }
        } else {
            br.getHeaders().put("Origin", "https://" + this.getHost());
            br.getHeaders().put("Referer", "https://" + this.getHost() + "/" + this.getFID(link));
            br.getHeaders().put("Accept", "application/json;charset=UTF-8");
            br.getHeaders().put("sec-fetch-dest", "empty");
            br.getHeaders().put("sec-fetch-mode", "cors");
            br.getHeaders().put("sec-fetch-site", "same-origin");
            /* According to website, this way we'll get a higher downloadspeed */
            br.getHeaders().put("x-adblock", "0");
            br.setAllowedResponseCodes(new int[] { 400 });
            int loop = 0;
            boolean captchaFailed = false;
            String recaptchaV2Response = null;
            do {
                String postData = "";
                // br.getPage(link.getPluginPatternMatcher());
                // recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, this.getReCaptchaKey()).getToken();
                if (loop > 0 || recaptchaV2Response != null) {
                    postData = "{\"g-recaptcha-verify\":\"" + recaptchaV2Response + "\"}";
                }
                br.setCurrentURL("https://" + this.getHost() + "/" + this.getFID(link));
                br.postPageRaw("https://" + this.getHost() + "/api/serve/video/" + this.getFID(link), postData);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    br.setCurrentURL("https://" + this.getHost() + "/" + this.getFID(link));
                    final String url = "https://" + this.getHost() + "/stream" + this.getFID(link) + ".mp4";
                    br.setFollowRedirects(false);
                    br.getPage(url);
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    return br.getRedirectLocation();
                }
                /* 2nd offlinecheck */
                final String errormessage = PluginJSonUtils.getJson(br, "message");
                if (errormessage != null && (errormessage.equalsIgnoreCase("captcha required") || errormessage.equalsIgnoreCase("invalid captcha verification"))) {
                    if (!isDownload) {
                        logger.info("Failed to find downloadlink because we don't want to ask for captchas during availablecheck");
                        return null;
                    }
                    captchaFailed = true;
                    recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2(this, br, this.getReCaptchaKey()).getToken();
                }
                loop++;
            } while (loop <= 1);
            if (captchaFailed) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            this.handleAPIIErrors(link, null, false);
        }
        String dllink = null;
        try {
            HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> quals;
            final Object qualitiesO = entries.get("qualities");
            if (qualitiesO instanceof ArrayList) {
                quals = (ArrayList<Object>) qualitiesO;
            } else {
                quals = new ArrayList<Object>();
                entries = (HashMap<String, Object>) entries.get("qualities");
                final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
                while (it.hasNext()) {
                    quals.add(it.next().getValue());
                }
            }
            long quality_temp = 0;
            long quality_best = 0;
            for (final Object qualO : quals) {
                entries = (HashMap<String, Object>) qualO;
                quality_temp = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "size/{0}"), 0);
                if (quality_temp > quality_best) {
                    quality_best = quality_temp;
                    dllink = (String) entries.get("src");
                }
            }
            if (!StringUtils.isEmpty(dllink)) {
                logger.info("BEST handling for multiple video source succeeded");
            }
        } catch (final Throwable e) {
            logger.info("BEST handling for multiple video source failed");
        }
        return dllink;
    }

    private boolean isPaired() {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        try {
            /* Return result depending on which API call was performed before */
            if (br.getURL().matches(".+api/pair/[a-z0-9]+")) {
                /*
                 * 2020-05-22: E.g.
                 * {"code":400,"message":"IP is not currently paired. Please visit https://vidup.io/pair for more information.","errors":[]}
                 */
                final String message = (String) entries.get("message");
                if (message != null && message.contains("IP is not currently paired")) {
                    return false;
                } else {
                    return true;
                }
            } else {
                final ArrayList<Object> sessions = (ArrayList<Object>) entries.get("sessions");
                if (sessions != null && !sessions.isEmpty()) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        }
    }

    /** Displays information regarding pairing to allow API downloads. */
    public Thread displayPairingDialog() throws InterruptedException {
        logger.info("Displaying Pairing information message");
        final int max_wait_seconds = getPairingTimeoutSeconds();
        final String host = this.getHost();
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = host + " - neue Download Methode";
                        message += "Hallo liebe(r) NutzerIn\r\n";
                        message += "Bitte folge den Anweisungen um von " + host + " herunterladen zu können:\r\n";
                        message += "1. Öffne " + host + "/pair sofern das nicht automatisch passiert.\r\n";
                        message += "2. Folge den Anweisungen im Browser.\r\n";
                        message += "Falls du einen headless JD/myjd verwendest, gehe sicher, dass du das Pairing mit derselben IP bestätigst, die auch der andere JDownloader hat!\r\n";
                        message += "Falls du einen Proxy in JDownloader verwendest, musst du denselben auch im Browser verwenden ansonsten wird das Pairing nicht funktionieren!\r\n";
                        message += "Dieses Fenster wird sich automatisch schließen.\r\n";
                    } else {
                        title = host + " - New download method";
                        message += "Hello dear user\r\n";
                        message += "Please follow the instructions to be able to download from " + host + ":\r\n";
                        message += "1. Open " + host + "/pair in your browser if this did not happen automatically already.\r\n";
                        message += "2. Follow the instructions given in browser.\r\n";
                        message += "If you are on headless/myjdownloader, make sure to confirm pairing with the SAME IP, your JDownloader is using!.\r\n";
                        message += "If you are using a proxy in JD, you will have to use it in your browser too otherwise pairing will fail!.\r\n";
                        message += "Once completed, this dialog will auto-close!\r\n";
                    }
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL("https://" + host + "/pair");
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(max_wait_seconds * 1000);
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void handleAPIIErrors(final DownloadLink link, final Account account, final boolean handleUnknownErrors) throws PluginException {
        final String errormessage = PluginJSonUtils.getJson(br, "message");
        if (!StringUtils.isEmpty(errormessage)) {
            if (errormessage.equalsIgnoreCase("invalid video code") || errormessage.equalsIgnoreCase("invalid video specified")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (errormessage.equalsIgnoreCase("captcha required") || errormessage.equalsIgnoreCase("invalid captcha verification")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            logger.info("Unknown error has happened: " + errormessage);
            if (handleUnknownErrors) {
                logger.info("Handling unknown error");
                if (link == null) {
                    throw new AccountUnavailableException(errormessage, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, 5 * 60 * 1000l);
                }
            } else {
                logger.info("NOT handling unknown error");
            }
        }
    }

    /**
     * Useful if direct-urls are available without captcha --> We can display the filesize in linkgrabber. <br/>
     * default: false
     */
    public boolean check_filesize_via_directurl() {
        return false;
    }

    /** Can be overridden to set a hardcoded reCaptcha key */
    protected String getReCaptchaKey() {
        return null;
    }

    /** Returns reCaptchaV2 key for "/pair" page. */
    protected String getReCaptchaKeyPairing() {
        return null;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            dllink = this.getDllink(link, true);
        }
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_Videohosting;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
