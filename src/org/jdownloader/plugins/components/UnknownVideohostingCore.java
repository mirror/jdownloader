package org.jdownloader.plugins.components;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
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
import jd.plugins.PluginProgress;
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
    private static final String        default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean       free_resume       = true;
    private static final int           free_maxchunks    = 0;
    private static final int           free_maxdownloads = -1;
    protected String                   dllink            = null;
    protected boolean                  server_issues     = false;
    private static final Object        LOCK              = new Object();
    private static final AtomicBoolean isPairing         = new AtomicBoolean(true);

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/home";
    }

    @Override
    public void init() {
        isPairing.set(true);
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
        return "/(?:embed/)?([a-z0-9]{12})";
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + UnknownVideohostingCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (allowPairingAPILinkcheck()) {
            /* Without this, we will often not get the online/offline state until we actually attempt to download! */
            prepBrAPI(this.br);
            br.getPage("https://" + this.getHost() + "/api/pair/" + this.getFID(link));
            handleAPIIErrors(link, null, false);
        }
        final Browser brc = new Browser();
        brc.getPage(link.getPluginPatternMatcher());
        /* 1st offlinecheck */
        if (brc.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // if (brc.containsHTML("<title> EMBED</title>")) {
        // /* TODO: Check if this is still needed */
        // final String redirectLink = link.getStringProperty("redirect_link");
        // if (StringUtils.isEmpty(redirectLink)) {
        // logger.info("page expired and no redirect_link found");
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        // brc.setFollowRedirects(false);
        // brc.getPage(redirectLink);
        // if (brc.getRedirectLocation() == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // link.setPluginPatternMatcher(brc.getRedirectLocation());
        // return requestFileInformation(link);
        // }
        /* 2019-05-08: TODO: Unsure about that */
        boolean requiresCaptcha = true;
        String filename = null;
        try {
            final String json = brc.getRegex("window\\.__INITIAL_STATE__=(\\{.*?\\});\\(function\\(\\)").getMatch(0);
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
                    if (con.getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (con.getCompleteContentLength() < 1000) {
                        /* 2020-05-24 */
                        logger.info("File is very small --> Must be offline");
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (!con.getContentType().contains("html")) {
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

    /*
     * 2020-05-24: Pairing API will return offline status for dead files even if user did not yet execute pairing. Via website, solving a
     * reCaptchaV2 is required to find the real online/offline status! This works for all supported sites except thevideos.ga. See also
     * vev.io/pair
     */
    protected boolean allowPairingAPILinkcheck() {
        return true;
    }

    protected int getPairingTimeoutSeconds() {
        return 120;
    }

    private Browser prepBrAPI(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    protected String getDllink(final DownloadLink link, final boolean isDownload) throws Exception {
        if (usePairingMode()) {
            /* https://vev.io/api#pair_access */
            prepBrAPI(br);
            br.getHeaders().put("Referer", "https://" + this.getHost() + "/pair");
            // while (pairingActive.get()) {
            // logger.info("Waiting for pairing to finish");
            // this.sleep(1000, link);
            // }
            synchronized (LOCK) {
                if (allowPairingAPILinkcheck() && !isPaired()) {
                    /*
                     * We could have waited several minutes because of synchronization so we need to access the page again to refresh the
                     * status! If we don't do that, user may receive two pairing-dialogs if he e.g. tries to start a lof of vev.io downloads
                     * at the same time but never went through the pairing process before!
                     */
                    logger.info("Accessing pairing page again to avoid multiple pairing dialogs at the same time");
                    br.getPage("https://" + this.getHost() + "/api/pair/" + this.getFID(link));
                } else {
                    br.getPage("https://" + this.getHost() + "/api/pair/" + this.getFID(link));
                    /* Check for offline status */
                    handleAPIIErrors(link, null, false);
                }
                /*
                 * Returns current pairing session - we do not need this at this moment. Example response with no active sessions:
                 * {"sessions":[]}
                 */
                // br.getPage("https://" + this.getHost() + "/api/pair");
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
                        final Thread dialog = displayPairingDialog();
                        try {
                            waitForPairing(link);
                        } finally {
                            /* Close dialog */
                            dialog.interrupt();
                        }
                    }
                    if (!isPaired()) {
                        logger.info("Pairing failed");
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Pairing failure", 15 * 60 * 1000l);
                    }
                    displayPairingSuccessDialog();
                }
                isPairing.set(false);
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
        if (StringUtils.isEmpty(dllink)) {
            this.handleAPIIErrors(link, null, true);
        }
        return dllink;
    }

    private void waitForPairing(final DownloadLink link) throws Exception {
        final PluginProgress waitProgress = new PluginProgress(0, 100, null) {
            // protected long lastCurrent = -1;
            // protected long lastTotal = -1;
            // protected long startTimeStamp = -1;
            @Override
            public PluginTaskID getID() {
                return PluginTaskID.WAIT;
            }

            @Override
            public String getMessage(Object requestor) {
                if (requestor instanceof ETAColumn) {
                    final long eta = getETA();
                    if (eta >= 0) {
                        return TimeFormatter.formatMilliSeconds(eta, 0);
                    }
                    return "";
                }
                return "Waiting for user to complete pairing process in browser ...";
            }
            // @Override
            // public void updateValues(long current, long total) {
            // super.updateValues(current, total);
            // if (startTimeStamp == -1 || lastTotal == -1 || lastTotal != total || lastCurrent == -1 || lastCurrent > current) {
            // lastTotal = total;
            // lastCurrent = current;
            // startTimeStamp = System.currentTimeMillis();
            // // this.setETA(-1);
            // return;
            // }
            // long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
            // if (currentTimeDifference <= 0) {
            // return;
            // }
            // long speed = (current * 10000) / currentTimeDifference;
            // if (speed == 0) {
            // return;
            // }
            // long eta = ((total - current) * 10000) / speed;
            // // this.setETA(eta);
            // }
        };
        waitProgress.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
        waitProgress.setProgressSource(this);
        try {
            final int maxwait = getPairingTimeoutSeconds();
            final int waitStepsSeconds = 5;
            int remainingWaitSeconds = maxwait;
            do {
                logger.info("Remaining pairing seconds: " + remainingWaitSeconds);
                // this.sleep(waitStepsSeconds * 1000l, this.getDownloadLink());
                br.getPage("https://" + this.getHost() + "/api/pair");
                if (isPaired()) {
                    logger.info("Pairing successful");
                    break;
                } else {
                    logger.info("Pairing failed / not yet done by user");
                    remainingWaitSeconds -= waitStepsSeconds;
                    link.addPluginProgress(waitProgress);
                    final int progress = (int) (100 - ((float) remainingWaitSeconds / (float) maxwait) * 100);
                    logger.info("Remaining wait seconds: " + remainingWaitSeconds);
                    logger.info("WaitProgress = " + progress);
                    waitProgress.updateValues(progress, 100);
                    waitProgress.setETA(remainingWaitSeconds * 1000);
                    for (int sleepRound = 0; sleepRound < waitStepsSeconds; sleepRound++) {
                        if (isAbort()) {
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        } else {
                            Thread.sleep(1000);
                        }
                    }
                }
            } while (remainingWaitSeconds > 0);
        } finally {
            this.getDownloadLink().removePluginProgress(waitProgress);
        }
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
                        message += "1. Öffne " + host + "/pair sofern das nicht bereits automatisch passiert ist.\r\n";
                        message += "2. Folge den Anweisungen im Browser.\r\n";
                        message += "Falls du einen headless JD/myjd verwendest, gehe sicher, dass du das Pairing mit derselben IP bestätigst, die auch der andere JDownloader hat!\r\n";
                        message += "Falls du einen Proxy in JDownloader verwendest, musst du denselben auch im Browser verwenden ansonsten wird das Pairing nicht funktionieren!\r\n";
                        message += "Dieses Fenster wird sich nach erfolgreichem Pairing automatisch schließen.\r\n";
                    } else {
                        title = host + " - New download method";
                        message += "Hello dear user\r\n";
                        message += "Please follow the instructions to be able to download from " + host + ":\r\n";
                        message += "1. Open " + host + "/pair in your browser if this did not happen automatically already.\r\n";
                        message += "2. Follow the instructions given in browser.\r\n";
                        message += "If you are on headless/myjdownloader, make sure to confirm pairing with the SAME IP, your JDownloader is using!.\r\n";
                        message += "If you are using a proxy in JD, you will have to use it in your browser too otherwise pairing will fail!.\r\n";
                        message += "Once the pairing process is completed, this dialog will auto-close!\r\n";
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

    public Thread displayPairingSuccessDialog() throws InterruptedException {
        logger.info("Displaying Pairing success information message");
        final int max_wait_seconds = 60;
        final String pairingDurationStr = PluginJSonUtils.getJson(br, "expire");
        final String howLongLastsPairing;
        if (pairingDurationStr == null || !pairingDurationStr.matches("\\d+")) {
            /* This should never happen */
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                howLongLastsPairing = "unbekannt";
            } else {
                howLongLastsPairing = "unknown";
            }
        } else {
            final int pairingValidity = Integer.parseInt(pairingDurationStr) * 1000;
            /* Save this timestamp as it might be useful in the future! */
            this.getDownloadLink().setProperty("pairing_validity", System.currentTimeMillis() + pairingValidity);
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                final SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                final String formattedDate = formatter.format(new Date(System.currentTimeMillis() + pairingValidity));
                howLongLastsPairing = String.format("Deine Pairing Session ist gültig für %s also bis zum %s Uhr.", TimeFormatter.formatMilliSeconds(pairingValidity, 0), formattedDate);
            } else {
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm");
                final String formattedDate = formatter.format(new Date(System.currentTimeMillis() + pairingValidity));
                howLongLastsPairing = String.format("Your pairing session is valid for %s (until %s)", TimeFormatter.formatMilliSeconds(pairingValidity, 0), formattedDate);
            }
        }
        final String host = this.getHost();
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = host + " - Pairing erfolgreich";
                        message += "Pairing erfolgreich!\r\n";
                        message += howLongLastsPairing + "\r\n";
                        message += "Danach wirst du das Pairing erneut durchführen müssen.\r\n";
                        message += "Bedenke, dass die Session nur für die IP gültig ist, unter der sie erstellt wurde!\r\n";
                        message += "Eine Änderung deiner IP/Proxy (oder Reconnect) würde die pairing Session sofort ungültig machen!\r\n";
                    } else {
                        title = host + " - Pairing successful";
                        message += "Pairing successful!\r\n";
                        message += howLongLastsPairing + "\r\n";
                        message += "After this time you will have to do the pairing process again.\r\n";
                        message += "Keep in mind that this pairing session is bound to the IP which you've used during the pairing process!\r\n";
                        message += "Changing your IP/Proxy (or doing a reconnect) will invalidate this session immediately!\r\n";
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
            } else if (errormessage.equalsIgnoreCase("invalid request")) {
                /*
                 * 2020-05-23: They seem to shadow-ban VPNs. When using one, they may return the following no matter which request you do:
                 * {"code":400,"message":"invalid request","errors":[]}
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage + " (Possibly VPN/proxy shadowban - change IT/disable proxy and retry)", 5 * 60 * 1000l);
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
        /* Debug-Test */
        // isPairing.set(false);
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
        if (isPairing.get() && usePairingMode()) {
            return 1;
        } else {
            return free_maxdownloads;
        }
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
